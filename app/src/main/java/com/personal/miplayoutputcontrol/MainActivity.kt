package com.personal.miplayoutputcontrol

import android.app.Activity
import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Lanzador invisible: no dibuja UI, solo reenvía a la pantalla de MiPlay de MIUI/HyperOS.
 *
 * Decisiones de diseño:
 * - [Activity] pura en lugar de `AppCompatActivity`: no necesitamos `AppCompatDelegate`
 *   ni el tema de Material, y ahorramos el class-loading de AppCompat en el arranque.
 * - Sin `setContentView`: se instala una vista transparente para que haya una ventana
 *   válida (necesaria para que `startActivity` no nos cierre por "no visible").
 * - Sin `overridePendingTransition` (deprecado en API 34): la ausencia de animación se
 *   consigue con el tema (`windowAnimationStyle = @null`) y con `overrideActivityTransition`.
 * - Todo el trabajo tras el `onCreate` se hace fuera del hilo principal.
 */
class MainActivity : Activity() {

    private lateinit var store: MiPlayOutputControlStore
    private lateinit var resolver: MiPlayOutputControlResolver

    /**
     * Scope propio de la Activity: `android.app.Activity` no es un `LifecycleOwner`,
     * así que no hay `lifecycleScope`. Se cancela en [onDestroy] para no filtrar la Activity.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private companion object {
        /**
         * Si la activity destino tarda en dibujar, cerramos igualmente a los 1500 ms
         * para no dejar un hueco transparente que capture toques del launcher.
         */
        const val FINISH_GUARD_MS = 1_500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ventana transparente e insensible al input: es un "hop", no una pantalla.
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(View(this).apply { isClickable = false })

        // API 34+: transiciones explícitas (sustituye a overridePendingTransition).
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }

        store = MiPlayOutputControlStore(applicationContext)
        resolver = MiPlayOutputControlResolver(this)

        // Red de seguridad: nunca quedarnos en pantalla.
        window.decorView.postDelayed({ finishQuietly() }, FINISH_GUARD_MS)

        scope.launch {
            val cached = runCatching { store.cached() }.getOrNull()
            val result = resolver.resolve(cached)

            when (result) {
                is MiPlayOutputControlResolver.Result.Found -> {
                    val launched = resolver.launch(result.component)
                    if (launched) {
                        // Cachear solo cuando de verdad funcionó.
                        persist(result.component)
                    } else {
                        // La caché pudo quedar obsoleta tras una actualización de sistema.
                        invalidateAndRetryWithout(cached)
                    }
                }

                MiPlayOutputControlResolver.Result.NotFound,
                MiPlayOutputControlResolver.Result.UnsupportedDevice -> {
                    toast(R.string.error_unsupported_device)
                }
            }

            finishQuietly()
        }
    }

    /**
     * Si el componente cacheado falla, se limpia y se reintenta con la lista completa
     * una sola vez. Un dispositivo que se actualizó de HyperOS cae aquí.
     */
    private suspend fun invalidateAndRetryWithout(cached: ComponentName?) {
        if (cached != null) runCatching { store.clear() }
        val retry = resolver.resolve(cached = null)
        if (retry is MiPlayOutputControlResolver.Result.Found && resolver.launch(retry.component)) {
            persist(retry.component)
        } else {
            toast(R.string.error_launch_failed)
        }
    }

    private suspend fun persist(component: ComponentName) {
        runCatching { store.save(component) }
    }

    private fun toast(resId: Int) {
        Toast.makeText(applicationContext, resId, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Cierra sin animación y sin llamar dos veces. */
    private fun finishQuietly() {
        if (!isFinishing && !isDestroyed) {
            finishAndRemoveTask()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            }
        }
    }
}

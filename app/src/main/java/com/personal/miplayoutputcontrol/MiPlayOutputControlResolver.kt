package com.personal.miplayoutputcontrol

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Decide a qué [ComponentName] hay que saltar.
 *
 * Separado de la Activity a propósito: no depende de `Activity` ni de `Context`
 * opcional, así que la política de elección se puede cubrir con un test JVM.
 */
class MiPlayOutputControlResolver(private val context: Context) {

    /** Resultado de la resolución. */
    sealed interface Result {
        data class Found(val component: ComponentName) : Result
        data object NotFound : Result
        data object UnsupportedDevice : Result
    }

    /**
     * Comprueba que el componente existe y es lanzable.
     *
     * Se usa `resolveActivity` en vez de `getActivityInfo` a propósito: cubre el
     * caso de que el paquete exista pero la activity esté deshabilitada o el
     * componente haya sido renombrado.
     */
    private fun isLaunchable(component: ComponentName): Boolean {
        val intent = explicitIntent(component)
        return context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    /**
     * Resuelve el destino, probando primero la caché.
     *
     * @param cached componente guardado de un arranque anterior, si lo hay.
     */
    fun resolve(cached: ComponentName?): Result {
        // 1. Camino rápido: lo que funcionó la última vez.
        if (cached != null && isLaunchable(cached)) {
            return Result.Found(cached)
        }

        // 2. Nada cacheado o la caché quedó obsoleta: probar la lista.
        val candidate = MiPlayOutputControlTarget.all.firstOrNull { isLaunchable(it) }
            ?: return if (MiPlayOutputControlTarget.all.isEmpty()) {
                Result.UnsupportedDevice
            } else {
                Result.NotFound
            }

        return Result.Found(candidate)
    }

    /** Intent explícito, sin extras, con NEW_TASK porque salimos de una activity que muere. */
    fun explicitIntent(component: ComponentName): Intent = Intent().apply {
        this.component = component
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Abre el componente. Devuelve `false` si el sistema lo rechaza. */
    fun launch(component: ComponentName): Boolean = try {
        context.startActivity(explicitIntent(component))
        true
    } catch (_: android.content.ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        // El fabricante endureció el componente para apps de terceros.
        false
    }
}

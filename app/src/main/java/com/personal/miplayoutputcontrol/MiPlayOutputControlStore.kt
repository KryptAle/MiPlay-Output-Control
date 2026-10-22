package com.personal.miplayoutputcontrol

import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** DataStore propiedad del proceso de la app. Un único fichero, sin singletons. */
private val Context.miPlayDataStore by preferencesDataStore(name = "miplay_target")

/**
 * Cachea qué [ComponentName] funcionó la última vez.
 *
 * Motivo: resolver un `Intent` contra un paquete ajeno cuesta ~1-3 ms, y en un
 * lanzador invisible que se abre y cierra constantemente eso se nota. Guardamos
 * el ganador y en arranques posteriores vamos directos.
 *
 * Si el componente cacheado deja de funcionar (actualización de HyperOS), se
 * limpia y se reintenta la lista completa.
 */
class MiPlayOutputControlStore(private val context: Context) {

    private companion object {
        val KEY_PACKAGE = stringPreferencesKey("component_package")
        val KEY_CLASS = stringPreferencesKey("component_class")
    }

    /** Devuelve el componente cacheado, o `null` si no hay. */
    suspend fun cached(): ComponentName? {
        val prefs: Preferences = context.miPlayDataStore.data.first()
        val pkg = prefs[KEY_PACKAGE] ?: return null
        val cls = prefs[KEY_CLASS] ?: return null
        return ComponentName(pkg, cls)
    }

    /** Guarda el componente que ha funcionado. */
    suspend fun save(component: ComponentName) {
        context.miPlayDataStore.edit { prefs ->
            prefs[KEY_PACKAGE] = component.packageName
            prefs[KEY_CLASS] = component.className
        }
    }

    /** Borra la caché (el componente dejó de existir tras una actualización de sistema). */
    suspend fun clear() {
        context.miPlayDataStore.edit { it.clear() }
    }

    /** `true` si hay algún componente cacheado. Útil para el diagnóstico. */
    fun hasCache() = context.miPlayDataStore.data.map { it[KEY_PACKAGE] != null }
}

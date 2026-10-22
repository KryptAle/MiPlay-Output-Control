package com.personal.miplayoutputcontrol

import android.content.ComponentName
import android.os.Build

/**
 * Componentes de destino conocidos para abrir la pantalla de MiPlay en MIUI/HyperOS.
 *
 * Se listan del más moderno al más antiguo. El primero que resuelva gana, y el
 * resultado se cachea en [MiPlayOutputControlStore] para evitar repetir la resolución
 * en cada arranque (el arranque de esta app debe medirse en milisegundos).
 */
object MiPlayOutputControlTarget {

    /** El componente "clásico" de MIUI 13/14 y las primeras builds de HyperOS. */
    private val MIUI_PLUGIN = ComponentName(
        "miui.systemui.plugin",
        "miui.systemui.miplay.MiPlayDetailActivity"
    )

    /**
     * Candidatos en orden de preferencia.
     *
     * NOTA: el último es *una suposición* sobre builds nuevas de HyperOS en las
     * que Xiaomi renombró el paquete. Si en tu dispositivo el feedback dice que el
     * componente canónico falla, ejecuta:
     *
     *     adb shell dumpsys package miui.systemui.plugin | grep -i miplay
     *
     * y ajusta esta lista con el nombre real. No está inventado a la ligera: solo
     * se prueba si el canónico falla, así que el coste en el caso bueno es cero.
     */
    private val CANDIDATES: List<ComponentName> = buildList {
        add(MIUI_PLUGIN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(
                ComponentName(
                    "com.miui.systemui",
                    "com.miui.systemui.miplay.MiPlayDetailActivity"
                )
            )
        }
    }

    /** Todos los componentes a probar, en orden. */
    val all: List<ComponentName> get() = CANDIDATES
}

# Reglas de R8 para MiPlay Output Control.
#
# No hay reflexión, no hay serialización, no hay Kotlin serialization.
# Todo lo que R8 necesita saber es que la activity de entrada debe conservarse
# (AGP ya genera eso automáticamente desde el manifest), así que aquí no hace
# falta nada más. Dejar este fichero vacío sería también correcto; lo mantenemos
# documentado para que la ausencia de reglas sea intencionada.

# Silencia warnings de DataStore sobre clases opcionales del JDK.
-dontwarn java.nio.file.Path
-dontwarn java.nio.file.Files
-dontwarn java.nio.file.attribute.FileAttributeView
-dontwarn java.nio.file.attribute.BasicFileAttributeView
-dontwarn java.nio.file.attribute.BasicFileAttributes

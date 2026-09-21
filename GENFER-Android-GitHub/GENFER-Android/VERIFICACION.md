# Verificación del proyecto entregado

## Comprobado durante la preparación

- 10 condiciones, 50 asociaciones y 4 entradas de riesgo.
- Identificadores de diagnóstico únicos por asociación.
- Todos los NOC propuestos tienen indicadores y un perfil de 5 anclajes.
- Todas las NIC propuestas tienen al menos dos actividades de selección.
- Ninguna entrada se declara validada ni contiene un código NANDA inventado.
- XML del manifiesto, recursos e instrucciones de respaldo bien formado.
- El manifiesto no solicita permisos de Internet ni acceso general al almacenamiento.
- Gradle Wrapper oficial incluido, con SHA-256 verificado; distribución fijada por checksum.
- Código fuente Java y recursos sin marcadores de generación pendientes.

Estas son comprobaciones de archivos y contenido. No constituyen una compilación Android.

## Pendiente de ejecutar en Android Studio

1. Sincronizar Gradle con JDK 17+ y SDK 35; ejecutar `:app:assembleDebug :app:testDebugUnitTest`.
2. Instalar el APK en un emulador o celular de pruebas.
3. Elegir ACV y movilidad; seleccionar factor, momento, fuentes y cuatro hallazgos. Verificar el PES.
4. Seleccionar Movilidad, indicador, inicial 2, meta 5 y plazo. Añadir una NIC con actividad, programación y rol.
5. Registrar reevaluaciones 1, 2, 4 y 5: retroceso, sin cambio, progreso y meta alcanzada. Verificar anclajes y colores.
6. Marcar No evaluable y seleccionar motivo. No debe mostrarse meta alcanzada ni puntuación 0.
7. Añadir un diagnóstico de riesgo. No debe tener síntomas ni «manifestado por».
8. Cambiar la meta: la reevaluación debe quedar pendiente. Cambiar actividades: ejecución pendiente.
9. Guardar, salir, volver a abrir y cargar el borrador. Rotar la pantalla y comprobar las selecciones.
10. Exportar TXT y JSON, abrirlos fuera de la app y cotejar todas las selecciones; cancelar otra exportación sin pérdida del plan.
11. Cancelar la retirada de un NOC o NIC y comprobar que permanece seleccionado.
12. Comprobar navegación Atrás, texto grande, TalkBack, campos largos y pantallas pequeñas.
13. Probar sin conexión y eliminar el borrador local mediante su botón.

Las horas automáticas corresponden al registro de selecciones. El APK de depuración es de pruebas; la versión de distribución requiere firma propia y las validaciones clínicas e institucionales descritas en README.md.

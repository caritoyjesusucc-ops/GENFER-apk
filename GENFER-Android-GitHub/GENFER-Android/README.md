# GENFER Android — código fuente completo en Java

Aplicación **nativa Android**, escrita en Java. No utiliza WebView, HTML, JavaScript, Node, Capacitor ni un servidor para ejecutarse. La interfaz se construye con componentes Android desde `MainActivity.java`; por eso no necesita un archivo de layout XML.

El ZIP contiene **código fuente, no un APK ya compilado**. Esta versión implementa un ejercicio de simulación; no es una historia clínica ni un producto institucional validado.

## 1. Abrir en Android Studio

1. Instala Android Studio desde https://developer.android.com/studio e instala los componentes recomendados por el asistente.
2. Descomprime este proyecto en una ruta corta, por ejemplo `C:\Proyectos\GENFER-Android`.
3. En Android Studio elige **Open** y selecciona esa carpeta, la que contiene `settings.gradle`.
4. Espera la sincronización de Gradle. La primera sincronización necesita Internet para descargar dependencias de compilación; la aplicación instalada funciona sin Internet.
5. En **Settings > Build, Execution, Deployment > Build Tools > Gradle**, selecciona el JDK integrado de Android Studio, compatible con Java 17 o superior. No uses Java 8.
6. En **Tools > SDK Manager**, instala **Android SDK Platform 35** y **Android SDK Build-Tools 35.0.0**. Android Studio crea `local.properties` con la ubicación del SDK.

Versiones fijadas: Android Gradle Plugin 8.13.0, Gradle 8.13, compileSdk/targetSdk 35, minSdk 26 (Android 8.0). El Wrapper oficial está incluido y su JAR fue contrastado con el SHA-256 publicado por Gradle. La distribución de Gradle también tiene un SHA-256 fijado.

## 2. Generar un APK para probar en tu celular

Con la sincronización terminada, utiliza el menú de Android Studio para **Build > Generate App Bundles or APKs > Generate APKs** (el nombre puede aparecer como **Build APK(s)** según la versión).

También puedes abrir una terminal en la carpeta del proyecto y ejecutar, si Java 17+ ya está configurado:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Si Windows sigue utilizando Java 8 pero instalaste Android Studio en su ubicación habitual, puedes usar directamente su Java integrado:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -classpath .\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest
```

El APK de pruebas, firmado automáticamente con la clave de depuración, se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Cópialo al celular Android de pruebas por USB o por el medio que uses para compartir archivos. Ábrelo en el teléfono y, si Android lo solicita, autoriza la instalación desde esa fuente. No uses únicamente el botón Run para producir un archivo destinado a instalación manual.

## 3. Generar una versión firmada para distribución

Usa **Build > Generate Signed App Bundle or APK > APK**, crea o selecciona tu almacén de claves y genera la variante `release`. Conserva la clave y su contraseña: las actualizaciones deben mantener la firma correspondiente. No se incluyen claves de publicación ni contraseñas en este proyecto. El APK de demostración no implica aprobación institucional.

## 4. Archivos de programación

```text
GENFER-Android/
  settings.gradle
  build.gradle
  gradle.properties
  gradlew / gradlew.bat
  gradle/wrapper/gradle-wrapper.jar
  gradle/wrapper/gradle-wrapper.properties
  app/build.gradle
  app/src/main/AndroidManifest.xml
  app/src/main/java/com/genfer/pae/
    MainActivity.java       Interfaz nativa, navegación, guardado y exportación.
    PlanRepository.java    Catálogo, estado del plan, PES, validación y resumen.
    CareEngine.java        Escala, colores y comparación de puntuaciones.
  app/src/main/assets/catalogo.json
  app/src/main/res/values/strings.xml
  app/src/main/res/drawable/ic_genfer.xml
  app/src/main/res/xml/data_extraction_rules.xml
  app/src/test/java/com/genfer/pae/CareEngineTest.java
```

No falta un archivo HTML: esta versión no depende del prototipo web. Tampoco necesita los archivos externos del proyecto original para compilarse.

## 5. Funciones incluidas

- 10 condiciones y 50 asociaciones de diagnóstico propuestas, con selección de varios diagnósticos por caso.
- Factores, fuentes de valoración y signos/síntomas de selección; PES generado automáticamente.
- Diagnósticos de riesgo sin signos/síntomas ni «manifestado por».
- Resultados e intervenciones relacionados con cada diagnóstico.
- Indicadores, actividades, plazos, programación y responsables predeterminados.
- Escalas de cinco niveles con número, color y descripción. Nada se evalúa automáticamente por seleccionar una patología.
- Metas y reevaluación: alcanzada, en progreso, sin cambio, retroceso, pendiente o no evaluable.
- Registro de ejecución de las actividades y continuidad del cuidado.
- Guardado explícito de un borrador en el almacenamiento privado de la aplicación; apertura y eliminación de ese borrador.
- Estado preservado durante la recreación habitual de la pantalla, como una rotación. Para conservarlo entre sesiones usa Guardar; no se garantiza recuperación sin guardar ante cierre forzado o terminación del proceso.
- Exportación TXT y JSON mediante el selector de archivos del sistema. No pide permiso general de almacenamiento ni acceso a Internet.

La edición de una meta deja su reevaluación pendiente; cambiar actividades o programación deja la ejecución pendiente. No hay historial de versiones. Cada NIC registra el estado del conjunto de actividades seleccionadas; si la ejecución es parcial, se requiere individualizar el diseño antes de un uso clínico.

La exportación del resumen se conserva aunque existan pendientes, claramente señalados. No se implementa reimportación de JSON, inicio de sesión, firma, sincronización, informes PDF ni historia de múltiples turnos. Android permite escoger un proveedor remoto al exportar si el usuario lo selecciona; la aplicación no transmite datos por sí misma.

## 6. Datos y personalización

`catalogo.json` contiene condiciones, diagnósticos, propuestas NOC/NIC, indicadores, actividades y escalas. Las etiquetas y asociaciones se conservaron de la matriz aportada; todas están pendientes de validación. Cambia los catálogos en este archivo y vuelve a compilar.

Para cambiar el nombre visible, edita `app_name` en `res/values/strings.xml`. Para cambiar el icono, reemplaza `res/drawable/ic_genfer.xml`. El identificador `com.genfer.pae` es de demostración: definir uno institucional antes de distribuir una versión definitiva. Para actualizaciones incrementa `versionCode` y conserva identificador y firma.

## 7. Escalas y alcance clínico

La escala tiene cinco puntos, siempre en dirección de peor a mejor estado. Ejemplo de movilidad: 1 dependencia total; 2 ayuda amplia; 3 ayuda moderada; 4 ayuda mínima; 5 independiente. Pendiente y no evaluable se muestran en gris, sin puntuación; el valor interno 0 significa ausencia de puntuación, nunca una calificación clínica.

Los anclajes son ilustrativos y varían por función, síntomas, estado fisiológico, conducta o conocimiento. **No constituyen escalas oficiales NOC**, aunque usen cinco puntos. No se convierten automáticamente signos vitales o laboratorios a calificaciones y no se calculan promedios entre indicadores. El color no sirve para triaje y una meta alcanzada no equivale a ausencia de riesgo clínico.

Antes de usar datos reales se requieren aprobación clínica del catálogo, cotejo de las ediciones y permisos NANDA-I/NOC/NIC, validación de anclajes y actividades, gestión de usuarios, cifrado apropiado, trazabilidad, política de retención, firma e integración con la historia clínica. El guardado privado del prototipo no representa por sí solo una solución clínica segura ni una certificación de cumplimiento.

## 8. Comprobaciones y pruebas pendientes

Se incluyen siete pruebas JUnit del motor: rango 1–5, ausencia de puntuación, comparación con meta, no evaluable, formulación de riesgo, PES y colores. Ejecuta `:app:testDebugUnitTest` con el entorno Android configurado.

En el equipo donde se preparó el proyecto no estaban instalados el SDK Android ni un JDK de compilación compatible. Por tanto, **no se ha compilado el APK ni se ha probado esta interfaz nativa en un teléfono o emulador**. Las pruebas del prototipo web no sustituyen esas comprobaciones. Revisa `VERIFICACION.md` para el estado y el recorrido manual.

## Referencias

- Instalación: https://developer.android.com/studio/install
- Compilación: https://developer.android.com/build/building-cmdline
- Firma: https://developer.android.com/studio/publish/app-signing
- Compatibilidad AGP: https://developer.android.com/build/releases/agp-8-13-0-release-notes
- Archivos elegidos por el usuario: https://developer.android.com/training/data-storage/shared/documents-files
- NOC: https://nursing.uiowa.edu/cncce/nursing-outcomes-classification-overview

## Compilar en GitHub sin instalar Android Studio

Consulta `GITHUB-APK.md`. El proyecto incluye `.github/workflows/android.yml`, que prepara Java y Android SDK, ejecuta las pruebas y publica el APK de demostración como artefacto descargable si la compilación es correcta. El flujo se activa al subir el proyecto a `main`/`master` o manualmente. La carpeta `.github` debe quedar en la raíz del repositorio.

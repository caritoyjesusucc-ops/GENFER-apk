# Generar el APK usando GitHub, sin Android Studio local

1. Descomprime el ZIP actualizado. Entra en la carpeta `GENFER-Android`.
2. En tu repositorio vacío de GitHub, pulsa **sube uno existente**.
3. Arrastra **el contenido** de `GENFER-Android` a la página: `.github`, `app`, `gradle`, `gradlew`, `build.gradle`, `settings.gradle` y los demás archivos. No subas el ZIP ni la carpeta contenedora como un nivel extra.
4. Verifica que en la lista aparezca `.github/workflows/android.yml` y que `settings.gradle` esté en la raíz. Al trabajar con GitHub web, arrastrar carpetas conserva su estructura.
5. Escribe un mensaje como `Agregar GENFER Android` y pulsa **Commit changes / Confirmar cambios**, en la rama principal `main` o `master`.
6. Abre la pestaña **Actions / Acciones**. Se debe iniciar **Compilar APK de GENFER**. Si aparece un aviso para habilitar Actions, revisa y habilítalo. También puedes abrir ese flujo y pulsar **Run workflow**.
7. Cuando la ejecución finalice en verde, ábrela y busca **Artifacts / Artefactos**. Descarga **GENFER-APK** mientras estés conectado a GitHub.
8. Extrae el ZIP descargado: dentro está `app-debug.apk`. Pásalo a tu Android de pruebas y ábrelo para instalar.

## Si algo falla

- Si no aparece el flujo en Actions, verifica que `.github/workflows/android.yml` esté en la raíz del repositorio y que se haya subido todo el contenido, no solo un ZIP.
- Si el flujo termina en rojo, abre el paso que falló y comparte el mensaje de error. No hay todavía un APK descargable de esa ejecución.
- Los archivos de salida se conservan 14 días. Se pueden regenerar mediante **Run workflow**.
- El APK de depuración se firma para pruebas. Una nueva ejecución puede utilizar otra clave de depuración; en ese caso Android puede exigir desinstalar la versión anterior, lo que elimina su borrador local. Exporta primero cualquier simulación que quieras conservar. La distribución estable requiere una clave de firma conservada de forma segura.

Esta configuración no ha sido ejecutada aún en tu repositorio. Intenta compilar y ejecutar las pruebas incluidas; solo una ejecución correcta confirma que generó el APK. No realiza validación clínica ni pruebas en un teléfono.

No subas datos de pacientes ni claves de firma. El paquete contiene únicamente código y catálogos de demostración. El repositorio mostrado en la captura es público.

Fuentes: https://docs.github.com/en/repositories/working-with-files/managing-files/adding-a-file-to-a-repository y https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-workflow-runs/downloading-workflow-artifacts

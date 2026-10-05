# Acordes de guitarra · 2.2

## iPhone y web

Versión para Safari: **https://luisca66.github.io/acordes-guitarra/**. En iPhone: Compartir → Añadir a pantalla de inicio. No requiere App Store.

La web está en `docs/` y se publica con GitHub Pages. Conserva las posiciones visuales, la lectura de letras con acordes, los nombres en español y la exportación de posiciones en PNG. La última hoja y los archivos de la app se guardan localmente para uso sin conexión después de la primera visita.

**Diferencia con Android:** en GitHub Pages la búsqueda abre el catálogo en otra pestaña. Hay que copiar una letra con acordes y usar **Pegar letra con acordes**; no hay descarga automática desde Ultimate Guitar. No se adivinan acordes ni su colocación. Las hojas no se envían a GitHub.

Pruebas de la web: `npm test`. Para probar localmente: `node tests/serve.mjs` y abrir http://127.0.0.1:4173. El catálogo se genera desde el motor Android mediante `tests/ExportChords.java`. La versión web incluye 646 posiciones; los bajos de acordes mayores están incluidos, y las digitaciones explícitas de una hoja tienen prioridad.

El APK Android se distribuye desde las publicaciones de GitHub; no se instala en iPhone.

Verificación adicional en navegador: instala Playwright (`npm install --no-save playwright`) y Microsoft Edge, inicia el servidor local y ejecuta `node tests/web-browser.cjs`. Se prueban la vista móvil, diagramas, alineación, almacenamiento y lectura sin conexión. No se ha probado en un iPhone físico.

App Android en español para leer canciones con letra y acordes, y consultar posiciones de guitarra.

## Canciones

1. Escribe un título; puedes agregar el artista.
2. Toca **Buscar canción** y elige una versión.
3. Lee la letra con los acordes colocados por el transcriptor original. Toca un acorde para abrir su diagrama.

La lectura conserva las secciones, saltos de línea y espacios. El panel oscuro permite desplazamiento horizontal cuando una línea no cabe. La lista inicial de posiciones, como `G 320033`, se convierte en diagramas de cuerdas y trastes antes de la letra, conservando exactamente la digitación de esa versión. Los acordes junto a la letra permanecen intactos y se pueden tocar para ampliar su diagrama. Cuando la hoja no incluye digitaciones iniciales, se muestran posiciones del catálogo para los acordes usados.

La fuente es [Ultimate Guitar](https://www.ultimate-guitar.com/). Se consultan las páginas públicas de búsqueda y de versiones de tipo **Chords**. La app muestra el enlace original y atribuye al transcriptor. No importa versiones Pro ni material que requiera acceso privado. La búsqueda requiere internet; el formato de las páginas puede cambiar o el sitio puede denegar una solicitud. En ese caso aparece un mensaje y un enlace para abrir el sitio en el navegador.

Una respuesta 404 que contiene la página explícita «No results» del buscador se trata como una búsqueda sin coincidencias y ofrece buscar con una parte del título. Los errores 404 de otras páginas y de versiones de canciones siguen mostrando un error de disponibilidad. Se verifica el caso reportado `loving spoons`.

**Pegar letra con acordes** permite abrir una hoja propia con los acordes sobre la letra, conservando los espacios. Se reconoce también el marcado `[ch]Am[/ch]` y las secciones `[Verso]`. No se adivina la colocación de los acordes a partir de una letra sin anotaciones. La última hoja abierta queda guardada en el teléfono para leerla sin conexión.

## Mis acordes

Escribe nombres como `C G Am F`, `Do Sol Lam Fa`, `Dm7 G7 Cmaj7` o `D/F#`. No se pide tonalidad ni progresión. Los nombres repetidos se muestran una sola vez.

Se admiten mayor, `m`, `7`, `maj7`, `m7`, `dim`, `dim7`, `aug`, `sus2`, `sus4`, `add9`, `madd9`, `7sus4`, `7sus2`, `6`, `m6`, `m7b5`, `5`, `9`, `m9`, `maj9` y acordes con bajo como `G/B`.

Los diagramas usan afinación estándar E A D G B e. Se conserva la hoja si un acorde no tiene diagrama y se informa cuál falta. Si la fuente indica otra afinación, no se generan posiciones para esa hoja. El número lateral es el primer traste mostrado. Algunas posiciones requieren cejilla; las posiciones calculadas para acordes extendidos son una opción, no necesariamente la digitación del transcriptor.

**Compartir posiciones como imagen** genera un PNG con los diagramas. Esta función comparte posiciones, no la letra. El modo manual admite 24 acordes distintos.

## Compilar

Abre la carpeta en Android Studio. Usa SDK 36, AGP 9.1.1, Gradle 9.3.1 y un JDK compatible con Gradle. Android mínimo: 8.0 (API 26).

```powershell
.\gradlew.bat assembleDebug lintDebug
```

APK de desarrollo: `app/build/outputs/apk/debug/app-debug.apk`. Una copia fácil de encontrar se entrega como `acordes-guitarra-v2.2.apk`.

## Validación

```powershell
javac -encoding UTF-8 -d tests/build app/src/main/java/com/luis/acordes/Music.java app/src/main/java/com/luis/acordes/Chords.java app/src/main/java/com/luis/acordes/SongSheet.java tests/MusicTest.java tests/ChordsTest.java
java -cp tests/build ChordsTest
java -cp tests/build MusicTest
```

Pruebas: notas de 252 combinaciones de raíz y tipo, 144 acordes con bajo, escritura en español, deduplicación, rechazo de grados y conservación de la alineación. Se conservan además las pruebas de los diagramas de la primera versión.

Prueba en dispositivo o emulador, incluyendo búsqueda real y lectura de una versión pública:

```powershell
.\gradlew.bat assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.luis.acordes.test/com.luis.acordes.IntegrationTest
```

La prueba de interfaz usa texto de ejemplo propio, comprueba ambos modos y los acordes interactivos, y guarda una captura dentro de los archivos privados de la app. Solo ejecutar en un dispositivo de pruebas: reemplaza la última hoja guardada por ese ejemplo.

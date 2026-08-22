# Super Mario 64 Android Port

Questo progetto è un port Android del codice sorgente ricostruito di Super Mario 64, basato su SDL2 e OpenGL ES 2.0.

Include controlli touch multipiattaforma, audio funzionante, salvataggio dei progressi nella memoria interna dell'app e supporto per tastiere e controller esterni (testato con controller PS3).

# Branch

* `master`: port vanilla di SM64, con pochissime modifiche.
* `sm64ex`: branch master di sm64ex.
* `sm64ex_nightly`: branch nightly di sm64ex. Usa questo branch per Render96/SGI models.

# Istruzioni di compilazione

## Android

Segui le istruzioni del repository `sm64-port-android`.

> Nota: la compilazione direttamente da Android è attualmente incompleta.

## Linux

### Installare le dipendenze

Le dipendenze variano in base alla distribuzione Linux utilizzata.

In generale, se riesci già a compilare il port PC e disponi di Android SDK/NDK con un ambiente Gradle funzionante per applicazioni Android, dovresti avere tutto il necessario.

### Clonare il repository

```sh
git clone --recursive https://github.com/RonyxDumb/sm64-android-port
cd sm64-port-android-base
```

### Copiare la baserom

```sh
cp /path/to/your/baserom.z64 ./app/jni/src/baserom.us.z64
```

### Scaricare i sorgenti SDL

```sh
./getSDL.sh
```

### Compilare il codice nativo

```sh
# Se hai più core disponibili puoi aumentare il parametro --jobs.
cd app/jni/src
make --jobs 4
cd ../../..
```

### Compilare l'applicazione Android

```sh
./gradlew assembleDebug
```

### APK risultante

```sh
ls -al ./app/build/outputs/apk/debug/app-debug.apk
```

---

## Windows

> **Guida completa consigliata:** per una configurazione dettagliata su Windows, inclusi Java JDK 11, Gradle 7.1, Android SDK/NDK, SDL2 e la risoluzione degli errori più comuni, consulta **[SETUP.md](SETUP.md)**.

### Installare le dipendenze

Sono necessari tutti gli strumenti richiesti per compilare il port Windows, oltre a quelli necessari per compilare applicazioni Android tramite `gradlew.bat`.

Sono quindi richiesti:

- Java JDK, configurato tramite `JAVA_HOME`
- Android SDK
- Android NDK
- MSYS2 MinGW
- Git
- Make
- Unzip

Salvo dove indicato diversamente, tutti i comandi devono essere eseguiti da **MSYS2 MinGW**.

È inoltre necessario installare `unzip`.

Apri MSYS2 MinGW ed esegui:

```sh
pacman -S unzip
```

### Clonare il repository

```sh
git clone --recursive https://github.com/RonyxDumb/sm64-android-port
```

Entra quindi nella directory:

```sh
cd sm64-port-android-base
```

### Copiare la baserom

Puoi usare Esplora File oppure:

```sh
cp /path/to/your/baserom.z64 ./app/jni/src/baserom.us.z64
```

Il file deve essere posizionato dentro:

```text
app/jni/src/
```

e deve avere lo stesso nome utilizzato dal normale port PC.

Per la versione USA:

```text
baserom.us.z64
```

### Scaricare i sorgenti SDL

```sh
./getSDL.sh
```

### Compilare il codice nativo

```sh
# Se hai più core disponibili puoi aumentare --jobs.
cd app/jni/src
make --jobs 4
cd ../../..
```

### Compilare l'applicazione Android

Questa operazione deve essere eseguita da un normale **Prompt dei comandi di Windows**, non da MSYS2.

```cmd
gradlew.bat assembleDebug
```

Se la compilazione termina correttamente, l'APK sarà disponibile in:

```text
app\build\outputs\apk\debug\app-debug.apk
```

---

## Docker

### Clonare il repository

```sh
git clone --recursive https://github.com/VDavid003/sm64-port-android-base
```

### Creare l'immagine Docker

```sh
cd sm64-port-android-base
docker build . -t sm64_android
```

### Copiare la baserom

```sh
cp /path/to/your/baserom.z64 ./app/jni/src/baserom.us.z64
```

### Configurare i collegamenti simbolici per SDL

```sh
docker run --rm -v $(pwd):/sm64 sm64_android sh -c "ln -nsf /SDL2-2.0.12/src /sm64/app/jni/SDL/src"
```

```sh
docker run --rm -v $(pwd):/sm64 sm64_android sh -c "ln -nsf /SDL2-2.0.12/include /sm64/app/jni/SDL/include"
```

### Compilare il codice nativo

```sh
# Se hai più core disponibili puoi aumentare --jobs.
docker run --rm -v $(pwd):/sm64 sm64_android sh -c "cd /sm64/app/jni/src && make --jobs 4"
```

### Compilare l'applicazione Android

```sh
docker run --rm -v $(pwd):/sm64 sm64_android sh -c "./gradlew assembleDebug"
```

### APK risultante

```sh
ls -al ./app/build/outputs/apk/debug/app-debug.apk
```

# Configurazione

Se vuoi personalizzare la build utilizzando opzioni di compilazione specifiche, devi prima compilare il codice nativo passando le opzioni desiderate al comando `make`, esattamente come nei normali repository SM64.

Successivamente, prima di effettuare la compilazione Android, modifica:

```text
app/jni/src/Android.mk
```

e abilita anche lì le opzioni desiderate.

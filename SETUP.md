# SM64 Port Android — Guida completa per Windows

Questa guida spiega come preparare, configurare e compilare `sm64-port-android-base` su Windows evitando gli errori che possono comparire con Gradle, Java, SDL2, Android NDK e toolchain.

## Requisiti

Servono:

* Windows 10/11
* MSYS2 MinGW64
* Git
* Java JDK 11
* Android SDK
* Android NDK 21.4.7075529
* Una ROM originale dumpata legalmente di Super Mario 64
* Il repository `sm64-port-android-base`

Repository:

```text
https://github.com/VDavid003/sm64-port-android-base
```

---

## 1. Clonare il repository

Aprire MSYS2 MinGW64:

```bash
cd /c/Users/pipin/Desktop

git clone --recursive https://github.com/VDavid003/sm64-port-android-base.git

cd sm64-port-android-base
```

È importante usare:

```bash
--recursive
```

per scaricare anche i submodule.

---

## 2. Installare gli strumenti MSYS2

Dentro MSYS2 MinGW64:

```bash
pacman -S unzip git make python
```

Se necessario:

```bash
pacman -S mingw-w64-x86_64-gcc
```

---

## 3. Inserire la ROM

Per una ROM USA:

```text
baserom.us.z64
```

va messa in:

```text
app/jni/src/baserom.us.z64
```

La ROM deve essere un dump compatibile con la versione richiesta dal progetto.

---

## 4. Preparare SDL2

Dalla root:

```bash
./getSDL.sh
```

SDL deve risultare in:

```text
app/jni/SDL/
```

La struttura corretta deve contenere:

```text
SDL/
├── Android.mk
├── include/
│   ├── SDL.h
│   ├── SDL_config.h
│   ├── SDL_config_android.h
│   └── ...
└── src/
    ├── SDL.c
    └── ...
```

---

## 5. Fix SDL2 su Windows

Il file:

```text
app/jni/SDL/Android.mk
```

usa:

```make
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include/SDL2
```

Perciò deve esistere:

```text
SDL/include/SDL2
```

che deve puntare alla stessa cartella:

```text
SDL/include
```

Da CMD:

```cmd
cd /d C:\Users\pipin\Desktop\sm64-port-android-base\app\jni\SDL
```

Creare una junction:

```cmd
mklink /J "include\SDL2" "C:\Users\pipin\Desktop\sm64-port-android-base\app\jni\SDL\include"
```

Verificare:

```cmd
dir include\SDL2\SDL_config.h
```

Deve trovare:

```text
SDL_config.h
```

Questo evita l'errore:

```text
fatal error: 'SDL_config.h' file not found
```

Non creare:

```text
include\include
```

e non usare una junction con target sbagliato.

---

## 6. Fix armips con GCC moderno

Su MSYS2 moderno può comparire:

```text
'int64_t' was not declared in this scope
```

Il file interessato è:

```text
app/jni/src/tools/armips.cpp
```

Dalla cartella:

```bash
cd /c/Users/pipin/Desktop/sm64-port-android-base/app/jni/src
```

aggiungere:

```cpp
#include <cstdint>
```

automaticamente:

```bash
sed -i '1i #include <cstdint>' tools/armips.cpp
```

Verificare:

```bash
head -n 5 tools/armips.cpp
```

---

## 7. Compilare il core

Da MSYS2 MinGW64:

```bash
cd /c/Users/pipin/Desktop/sm64-port-android-base/app/jni/src

make -j$(nproc)
```

Oppure:

```bash
make -j4
```

Questa fase può generare anche un eseguibile PC, ma non è ancora l'APK Android.

L'APK viene generato successivamente da Gradle.

---

## 8. Usare Gradle 7.1

Il progetto usa un Android Gradle Plugin vecchio e non è compatibile con Gradle 8/9.

Aprire:

```text
gradle/wrapper/gradle-wrapper.properties
```

e assicurarsi che contenga:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-7.1-all.zip
```

Controllare:

```cmd
gradlew.bat --version
```

Deve comparire:

```text
Gradle 7.1
```

Se compare Gradle 8.x, il wrapper non è configurato correttamente.

---

## 9. Installare Java JDK 11

Gradle 7.1 di questo progetto non deve essere eseguito con Java 17.

Un errore tipico è:

```text
Unsupported class file major version 61
```

`61` corrisponde a Java 17.

Installare JDK 11, per esempio Eclipse Temurin.

Percorso usato in questo esempio:

```text
C:\Program Files\Eclipse Adoptium\jdk-11.0.32.9-hotspot
```

Da CMD:

```cmd
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-11.0.32.9-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
```

Verificare:

```cmd
java -version
```

Deve mostrare:

```text
11.0.x
```

Poi:

```cmd
gradlew.bat --version
```

Deve risultare qualcosa come:

```text
Gradle 7.1
JVM: 11.0.x
```

---

## 10. Fermare vecchi daemon Gradle

Dopo aver cambiato Java:

```cmd
gradlew.bat --stop
```

Questo impedisce a Gradle di riutilizzare un daemon avviato precedentemente con Java 17.

---

## 11. Android SDK e NDK

Il progetto è stato compilato correttamente usando:

```text
NDK 21.4.7075529
```

Percorso esempio:

```text
C:\Android_Material\ndk\21.4.7075529
```

Il log deve mostrare qualcosa simile:

```text
ndk-build.cmd
APP_ABI=armeabi-v7a
APP_PLATFORM=android-16
```

Se Gradle non trova l'SDK, creare nella root:

```text
local.properties
```

con:

```properties
sdk.dir=C\:\\Users\\pipin\\AppData\\Local\\Android\\Sdk
```

Usare il percorso reale del proprio Android SDK.

---

## 12. Compilare l'APK

Questa fase va eseguita da CMD Windows.

Dalla root:

```cmd
cd /d C:\Users\pipin\Desktop\sm64-port-android-base
```

Pulire:

```cmd
gradlew.bat clean
```

Compilare:

```cmd
gradlew.bat assembleDebug
```

Se tutto va bene:

```text
BUILD SUCCESSFUL
```

L'APK dovrebbe essere generato in:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Trovarlo anche con:

```cmd
dir /s /b *.apk
```

---

## 13. Installare l'APK

Con ADB:

```cmd
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Oppure copiare manualmente l'APK sul telefono e installarlo.

---

# Fullscreen e display cutout

Su telefoni con fotocamera frontale/notch può comparire una barra nera laterale in landscape.

Nel file:

```text
app/src/main/res/values/styles.xml
```

usare:

```xml
<resources>

    <style name="AppTheme" parent="android:style/Theme.Black.NoTitleBar.Fullscreen">
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
        <item name="android:windowActionModeOverlay">true</item>
    </style>

</resources>
```

Poi nel `AndroidManifest.xml` usare:

```xml
android:theme="@style/AppTheme"
```

invece di:

```xml
android:theme="@android:style/Theme.NoTitleBar.Fullscreen"
```

---

# Impostare l'icona dell'app

Nel blocco `<application>` del:

```text
app/src/main/AndroidManifest.xml
```

aggiungere:

```xml
android:icon="@mipmap/ic_launcher"
```

Esempio:

```xml
<application
    android:label="@string/app_name"
    android:icon="@mipmap/ic_launcher"
    android:allowBackup="true"
    android:theme="@style/AppTheme"
    android:hardwareAccelerated="true">
```

Mettere quindi:

```text
app/src/main/res/mipmap-mdpi/ic_launcher.png
app/src/main/res/mipmap-hdpi/ic_launcher.png
app/src/main/res/mipmap-xhdpi/ic_launcher.png
app/src/main/res/mipmap-xxhdpi/ic_launcher.png
app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
```

Dimensioni:

```text
mdpi      48x48
hdpi      72x72
xhdpi     96x96
xxhdpi   144x144
xxxhdpi  192x192
```

---

# Configurazione consigliata finale

```text
Java:
JDK 11

Gradle:
7.1

Android Gradle Plugin:
4.2.1

Android NDK:
21.4.7075529

ABI:
armeabi-v7a

SDL:
SDL2 con include/SDL2 correttamente collegato

Build native:
MSYS2 MinGW64

Build APK:
CMD Windows
```

---

# Errori comuni

## `Unsupported class file major version 61`

Causa:

```text
Java 17
```

Soluzione:

```cmd
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-11.0.32.9-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
gradlew.bat --stop
```

---

## `IncrementalTaskInputs is not a valid parameter`

Causa:

```text
Gradle 8.x + Android Gradle Plugin 4.2.1
```

Soluzione:

```text
Gradle 7.1
```

nel wrapper.

---

## `SDL_config.h file not found`

Causa:

```text
SDL/include/SDL2 non configurato correttamente
```

Soluzione:

```cmd
mklink /J "include\SDL2" "C:\Users\pipin\Desktop\sm64-port-android-base\app\jni\SDL\include"
```

---

## `int64_t was not declared` in `armips.cpp`

Soluzione:

```cpp
#include <cstdint>
```

in:

```text
app/jni/src/tools/armips.cpp
```

---

## `app\build\outputs\apk\debug` non esiste

Significa che:

```text
gradlew.bat assembleDebug
```

non è terminato con:

```text
BUILD SUCCESSFUL
```

Controllare il primo errore della build.

---

# Sequenza rapida

MSYS2 MinGW64:

```bash
cd /c/Users/pipin/Desktop/sm64-port-android-base

./getSDL.sh

cd app/jni/src

sed -i '1i #include <cstdint>' tools/armips.cpp

make -j$(nproc)
```

CMD:

```cmd
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-11.0.32.9-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d C:\Users\pipin\Desktop\sm64-port-android-base

gradlew.bat --stop
gradlew.bat clean
gradlew.bat assembleDebug
```

APK finale:

```text
app\build\outputs\apk\debug\app-debug.apk
```

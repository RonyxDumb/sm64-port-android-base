# Compilazione automatica Android multiplayer

La workflow lavora su `feature/local-multiplayer` e usa il sorgente
`RonyxDumb/sm64-android-port@feature/local-multiplayer`.
Il submodule `app/jni/src` viene aggiornato insieme al runtime.

## Configurazione

In **Settings → Secrets and variables → Actions**:

- `SM64_ROM_DRIVE_URL`: link a un singolo dump ROM US `baserom.us.z64`.
- `SM64_ROM_SHA1`: SHA-1 atteso, facoltativo.
- `SM64_DRIVE_SERVICE_ACCOUNT_JSON`: autenticazione per Drive, facoltativa.
  Senza questo secret il download usa gdown e il file deve consentire accesso anonimo.

Non serve più `SM64_APK_ENCRYPTION_PASSWORD`: la workflow pubblica direttamente
l'APK installabile e non genera file `.enc`.

I push sul branch multiplayer avviano la compilazione completa se è configurata
la ROM. Le PR eseguono soltanto il preflight senza ROM e senza segreti.

## Scaricare e installare

1. Aprire **Actions → SM64 Android multiplayer APK**.
2. Aprire una run riuscita e scaricare l'artifact **sm64-multiplayer-apk**.
3. Estrarre lo ZIP fornito da GitHub Actions.
4. Aprire **sm64-multiplayer.apk** su Android e installarla.

Lo ZIP è il contenitore standard degli artifact GitHub; l'APK al suo interno
non è cifrata e non richiede password o OpenSSL.
Gli artifact sono conservati per 7 giorni.

## Compilazione

- Ubuntu 22.04, JDK 17 per SDK e controllo Java, JDK 11 per Gradle 7.1.
- Android API 28, NDK 21.4.7075529, SDL2 2.0.12.
- Prebuild Linux: `make -C app/jni/src -j2 VERSION=us`.
- Android: `./gradlew --no-daemon --stacktrace assembleDebug`.
- Il prebuild richiede anche gli header desktop SDL2, Xrandr e libusb.
- Il preflight esegue `tests/run_netplay.sh` con due processi UDP e pacchetti invalidi.

La ROM viene verificata, non viene pubblicata come artifact e viene eliminata
dal runner a fine job. L'APK contiene gli asset generati durante la build.
Il superamento della build non certifica il funzionamento del multiplayer
su due telefoni: limiti e procedura di prova sono nel repository del gioco,
in `docs/android-local-multiplayer.md`.

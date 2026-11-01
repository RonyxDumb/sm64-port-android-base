# Compilazione automatica Android multiplayer

Questa workflow vive nel **contenitore Gradle** `sm64-port-android-base` e
utilizza **esclusivamente** `RonyxDumb/sm64-android-port@feature/local-multiplayer`
come sorgente nativo. Non modifica il port single-player su `main`.

## Attivazione

1. Unire la PR relativa alla CI **soltanto** in `feature/local-multiplayer`,
   non su `master`.
2. In `sm64-port-android-base`, aprire
   **Settings → Secrets and variables → Actions → New repository secret**.
3. Configurare:
   - `SM64_ROM_DRIVE_URL` (necessario per compilare): link di download a un
     **singolo file ROM US** `baserom.us.z64`, non a una cartella né a uno ZIP.
   - `SM64_ROM_SHA1` (consigliato): SHA-1 atteso del proprio dump ROM.
   - `SM64_APK_ENCRYPTION_PASSWORD` (facoltativo): password per cifrare l'APK
     prima di caricarlo negli artifact. Conservare la password offline.
4. Un nuovo push su `feature/local-multiplayer` avvia il controllo dei
   sorgenti e, se è configurato il link ROM, la compilazione completa dell'APK.
   Per un push manuale dopo aver impostato i secret, è sufficiente un normale
   commit sul branch multiplayer.

## Comportamento e sicurezza

- Le PR verso `feature/local-multiplayer` eseguono **solo** il controllo
  sorgenti senza ROM e senza segreti.
- La build completa parte **solo** dai push sul branch multiplayer fidato.
  Senza `SM64_ROM_DRIVE_URL` la build APK viene saltata con un avviso.
- Il downloader usa `gdown`; un link Google Drive accessibile a «chiunque
  abbia il link» **non è privato**. Un file Drive con condivisione ristretta
  normalmente non è scaricabile con `gdown` senza autenticazione aggiuntiva.
  Non inserire link ROM in README, issue, PR, commit o chat pubbliche.
- Il workflow verifica che la ROM scaricata sia di 8 MiB e, se fornito,
  controlla lo SHA-1; la verifica di compatibilità degli asset è poi demandata
  agli script del port.
- Il runner è effimero, la ROM non è caricata come artifact e il file viene
  eliminato dalla cartella di lavoro a fine job.
- In un **repository pubblico** l'APK può incorporare asset proprietari:
  per questo **non viene caricato in chiaro**. L'artifact compare soltanto
  se è impostato `SM64_APK_ENCRYPTION_PASSWORD` e contiene un file cifrato.
- Questa workflow non prova la giocabilità del multiplayer; prova soltanto
  il compilatore e, quando eseguita, la generazione dell'APK.

## Scaricare e decifrare l'APK

Dopo una build riuscita vai in **Actions → SM64 Android multiplayer APK**,
apri il run e scarica `sm64-multiplayer-apk-encrypted`. Estrai lo ZIP
dell'artifact. Sul tuo computer esegui (OpenSSL):

```bash
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
  -in sm64-multiplayer.apk.enc -out sm64-multiplayer.apk
```

OpenSSL richiederà la password usata nel secret. Installa e prova l'APK
soltanto dopo averne verificato la provenienza. Non distribuire la ROM o l'APK
con asset protetti senza le necessarie autorizzazioni.

## Ambiente usato

- Ubuntu 22.04 runner
- Java 11 / Gradle wrapper 7.1
- Android SDK 28, NDK 21.4.7075529
- SDL2 preparato con `getSDL.sh`
- Prebuild `make -C app/jni/src -j2 VERSION=us`
- Gradle `assembleDebug`

L'APK atteso è `app/build/outputs/apk/debug/app-debug.apk`.
La prima build cloud rimane **da verificare**; gli eventuali errori NDK,
SDL2 o asset andranno corretti sulla base dei log.

## Integrazione Wi-Fi Direct

Il branch multiplayer comprende ora `LocalDirect.java`, i permessi Wi-Fi/posizione
richiesti dal target SDK 28 e il bridge JNI richiamato dal gioco. Il submodule
`app/jni/src` punta a `sm64-android-port@feature/local-multiplayer`: usare
`git submodule update --init` per il commit fissato, oppure `--remote` solo se si
vuole intenzionalmente aggiornare il sorgente.

Il preflight esegue anche `tests/run_netplay.sh` del gioco (due processi UDP e
pacchetti invalidi, senza ROM). Questo verifica il trasporto; non simula il
framework Wi-Fi Direct Android né certifica una partita completa. I limiti di
sincronizzazione del mondo sono descritti in `docs/android-local-multiplayer.md`
nel repository del gioco. I progressi raccolti nella sessione vengono aggiunti
ai file scelti su entrambi i telefoni.

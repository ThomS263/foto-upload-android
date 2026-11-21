# Foto-Upload für Android

Android-App für einen selbst betriebenen Foto-Upload-Server. Beim ersten Start die eigene Serveradresse eingeben. Alben, Upload, Galerie und Verwaltung werden vom eigenen Server geladen.

## In Android Studio öffnen

Das Repository klonen und in Android Studio als Projekt öffnen. Android SDK Platform 35 und die zugehörigen Build-Tools installieren. Dann auf einem Android-Gerät oder Emulator ausführen. Die App unterstützt Android 8.0 und neuer.

```bash
git clone https://github.com/ThomS263/foto_upload_offiziell_androidapp.git
cd foto_upload_offiziell_androidapp
./gradlew assembleDebug
```

Die Debug-APK liegt unter `app/build/outputs/apk/debug/`. Auf einem echten Gerät muss die eingegebene Serveradresse erreichbar sein. Zum Testen auf dem Android-Emulator verweist `http://10.0.2.2:8080` auf einen lokalen Server des Entwicklungsrechners.

Die App speichert die Serveradresse lokal. HTTPS ist für den Zugriff über das Internet geeignet. HTTP ist für Server im eigenen Netzwerk verfügbar. Die Auswahl mehrerer Fotos erfolgt über die Android-Dateiauswahl.

Die erste Version unterstützt den normalen Foto- und Video-Upload und das Öffnen der Galerie. Die direkte Kameraaufnahme und ZIP-Downloads aus der App sind noch nicht fertig. Dafür kann die Serveradresse im normalen Android-Browser geöffnet werden.

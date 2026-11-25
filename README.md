# Foto-Upload für Android

Android-App für einen selbst betriebenen Foto-Upload-Server. Beim ersten Start die eigene Serveradresse eingeben. Alben, Upload, Galerie und Verwaltung werden vom eigenen Server geladen.

**[Aktuelle APK herunterladen](https://github.com/ThomS263/foto-upload-android/releases/latest/download/foto-upload-android.apk)** · [Foto-Upload-Server und CasaOS-Installation](https://github.com/ThomS263/foto-upload)

Die APK kann bis zur Veröffentlichung im Google Play Store direkt über GitHub Releases installiert werden. Veröffentlicht werden ausschließlich signierte Release-APKs samt SHA-256-Prüfsumme, keine Debug-APKs. Bei Android muss dafür gegebenenfalls einmalig die Installation aus dem verwendeten Browser erlaubt werden.

Die App verlangt keine pauschalen Kamera- oder Mediathek-Rechte: Kamera und Dateiauswahl laufen über Android-Systemdialoge mit temporären Freigaben. HTTPS-Zertifikate werden vom Betriebssystem geprüft; HTTP ist nur für lokale, private und Tailscale-Adressen erlaubt und löst eine deutliche LAN-Warnung aus. Serveradressen sind keine Zugangsdaten, liegen im privaten App-Speicher und werden nicht in Android-Backups übernommen. Details und der vertrauliche Meldeweg stehen in [SECURITY.md](SECURITY.md).

## In Android Studio öffnen

Das Repository klonen und in Android Studio als Projekt öffnen. Android SDK Platform 35 und die zugehörigen Build-Tools installieren. Dann auf einem Android-Gerät oder Emulator ausführen. Die App unterstützt Android 8.0 und neuer.

```bash
git clone https://github.com/ThomS263/foto-upload-android.git
cd foto-upload-android
./gradlew assembleDebug
```

Die Debug-APK liegt unter `app/build/outputs/apk/debug/`. Auf einem echten Gerät muss die eingegebene Serveradresse erreichbar sein. Zum Testen auf dem Android-Emulator verweist `http://10.0.2.2:8080` auf einen lokalen Server des Entwicklungsrechners.

## Lizenz

Die Android-App steht unter der [GNU Affero General Public License v3.0](LICENSE) (`AGPL-3.0-only`).

Die App speichert die Serveradresse lokal. HTTPS ist für den Zugriff über das Internet geeignet. HTTP ist für Server im eigenen Netzwerk verfügbar. Die Auswahl mehrerer Fotos erfolgt über die Android-Dateiauswahl.

Über die untere Leiste stehen **Start**, **Aktualisieren** und **Einstellungen** jederzeit bereit. Server lassen sich als benannte Profile wie „Zuhause“, „Familie“ oder „Verein“ speichern, umbenennen, testen und wieder löschen. Nach dem Speichern öffnet die App automatisch den gewählten Server.

Die App verwendet die Oberfläche des Servers. Kameraaufnahme, Foto- und Videoauswahl, Galerie und Downloads stehen in der App zur Verfügung. Für ZIP-Downloads ist Foto-Upload Server 3.0.3 oder neuer erforderlich.

Ab Version 1.3 besitzt die App ein eigenes adaptives Android-Symbol, das sich an runde und quadratische Launcher-Formen anpasst.

Ab Version 1.4 kann die Serveradresse per QR-Code übernommen werden. Mehrere bereits verwendete Server bleiben auswählbar. Fotos und Videos lassen sich aus anderen Android-Apps über **Teilen → Foto-Upload** vormerken und anschließend im gewünschten Album hochladen.

Ab Version 1.5 prüft die App GitHub Releases auf neue APKs. Beim Teilen zeigt sie die vorhandenen Alben in einer nativen Auswahl. Während laufender Übertragungen hält ein Android-Dienst den Upload mit einer Benachrichtigung aktiv und zeigt den genauen Fortschritt. Ein langer Druck auf **Start** wechselt direkt zwischen gespeicherten Serverprofilen. Die zuletzt geöffnete Seite bleibt bei einer Drehung oder einer Neuerstellung der Activity erhalten.

## Google Play vorbereiten

Datenschutzerklärung und Store-Texte liegen im Repository. Der Workflow **Play-Store-Bundle erstellen** erzeugt nach manueller Freigabe ein signiertes Android App Bundle. Dafür werden die GitHub-Secrets `PLAY_KEYSTORE_BASE64`, `PLAY_STORE_PASSWORD`, `PLAY_KEY_ALIAS` und `PLAY_KEY_PASSWORD` benötigt. Das erzeugte `.aab` kann zunächst in den internen oder geschlossenen Test von Google Play hochgeladen werden. Die Release-Signatur muss dauerhaft gesichert werden; ohne denselben Schlüssel lassen sich spätere Updates nicht veröffentlichen.

[Datenschutzerklärung](PRIVACY.md) · [Play-Store-Checkliste](PLAY_STORE.md)

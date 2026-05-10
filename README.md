# NetPulse

Android-App zur Netzwerk-Diagnose: Latenz, Ports, DNS, WiFi-Signal — auf einen Blick. **Portfolio-Demo** — gebaut, um Jetpack Compose und Vico Charts praktisch durchzuspielen, nicht als produktives Tool gedacht.

## Funktionen

- TCP-Ping mit Latenz-Verlauf (60-Werte-Live-Graph)
- Port-Scan mit Service-Erkennung
- DNS-Lookup (mehrere IP-Adressen pro Hostname)
- WiFi-Signal-Monitoring (SSID + dBm-Verlauf parallel zum Ping)

## Stack

Kotlin · Jetpack Compose (BOM 2024.12, Material 3) · Vico Charts 2.0.1 · `minSdk` 26, `targetSdk` 35

## Build

```
./gradlew assembleDebug
```

Kein Release-APK — wer bauen will, hat den Source.

## Hinweis

Eine von mehreren kleinen Apps in diesem Repo-Bereich, die als Übungsstrecken für Android-Themen entstanden sind. TCP-Ping statt ICMP, weil Android Root-loses ICMP nicht erlaubt — das ist Android-Realität, nicht Limitierung der App. Funktioniert, ist aber kein Versuch, einen weiteren Network-Tool-Konkurrenten zu sein.

## Lizenz

MIT.

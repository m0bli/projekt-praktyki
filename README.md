# projekt praktyki

---

# Spis Dokumentacji

**__[Sprawdź szczegółową dokumentacje](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/DOCUMENTATION.md)__**

1. **[Architekturę Systemu](DOCUMENTATION.md#1-architektura-systemu)** – Warstwy UI, Repozytoria, Klient Retrofit.
2. **[Specyfikację REST API](DOCUMENTATION.md#2-specyfikacja-interfejsów-rest-api)** – Punkt końcowy `http://127.0.0.1:13308/`, opis klas `UserAPI`, `AuthAPI`, `KontoAPI`.
3. **[Zarządzanie Sesją i Bezpieczeństwo](DOCUMENTATION.md#3-zarządzanie-sesją-i-bezpieczeństwo)** – Re-authentication dla operacji wrażliwych.
4. **[Silnik Motywów](DOCUMENTATION.md#4-silnik-motywów-thememanager)** – Wzorzec obserwatora (`ThemeChangeListener`) i dynamiczne nakładanie kolorów.
5. **[Katalog Okien Dialogowych](DOCUMENTATION.md#6-katalog-okien-dialogowych-dialogs)** – Opis widoków `dialog_*.xml`.
6. **[Instrukcję Deweloperską](DOCUMENTATION.md#8-instrukcja-deweloperska-i-wdrożenie)** – Konfiguracja ADB reverse, kompilacja i budowanie projektu.

---

# Uruchomienie

## 1. Przekierowanie Portu ADB dla Lokalnego Backend
```powershell
$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe reverse tcp:13308 tcp:13308
```

## 2. Kompilacja i Instalacja
```bash
./gradlew installDebug
```

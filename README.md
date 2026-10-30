# Dokumentacja

## jak uruchomić

### 1. Wymagania
* **Android Studio** (Jellyfish / Ladybug / 2024.2 lub nowsze)
* **Android SDK 37**
* **Telefon** (wersja Android 7.0 / API 24 lub nowsza) z włączonym debugowaniem USB w Ustawieniach Deweloperskich.

---

### 2. Połączenie z serwerem (ADB Reverse)
Aplikacja wysyła zapytania do lokalnego backendu REST API na porcie `13308`. Jeśli serwer działa na Twoim komputerze, musisz przekierować port do telefonu..


#### 💻 Windows (PowerShell):
```powershell
$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe reverse tcp:13308 tcp:13308
```

#### 🐧 Linux / macOS:
```bash
adb reverse tcp:13308 tcp:13308
```

---

### 3. Uruchomienie aplikacji
1. Otwórz projekt w **Android Studio**.
2. Wybierz połączone urządzenie / emulator na górnym pasku narzędzi.
3. Kliknij zielony przycisk **Run `app`** (▶️) lub wpisz w terminalu:

```bash
./gradlew installDebug
```

---

### 4. Pierwsze kroki w aplikacji
1. Po uruchomieniu kliknij **"Nie masz konta? Zarejestruj się"**.
2. Wypełnij formularz rejestracji (imię, nazwisko, login, e-mail, hasło).
3. Zaloguj się swoimi nowymi danymi.
4. Gotowe! Możesz teraz korzystać z pulpitu, karty debetowej, historii oraz przelewów.

---

#  DOKUMENTACJA

---

## 📑 Spis Treści Dokumentacji

1. [Architektura Systemu](#1-architektura-systemu)
2. [Specyfikacja Interfejsów REST API](#2-specyfikacja-interfejsów-rest-api)
3. [Zarządzanie Sesją i Bezpieczeństwo](#3-zarządzanie-sesją-i-bezpieczeństwo)
4. [Silnik Motywów (ThemeManager)](#4-silnik-motywów-thememanager)
5. [Przegląd Modułów i Ekranów](#5-przegląd-modułów-i-ekranów)
6. [Katalog Okien Dialogowych (Dialogs)](#6-katalog-okien-dialogowych-dialogs)
7. [Struktura Projektu i Klas](#7-struktura-projektu-i-klas)

---

## 1. Architektura Systemu


* **Warstwa Prezentacji (UI)**:
  * Klasy `Activity` oraz `Fragment` odpowiedzialne za cykl życia interfejsu użytkownika.
  * Komponenty `ViewBinding` oraz układy `ConstraintLayout`, `ScrollView` i `MaterialCardView`.
* **Warstwa Danych i Sieci (Data / Network)**:
  * Klient HTTP oparty na bibliotece **Retrofit 2** z konwerterem **Gson**.
  * Interfejsy repozytoriów (`AuthRepository`, `KontoRepository`) oraz interfejsy API (`UserAPI`, `AuthAPI`, `KontoAPI`).
* **Warstwa Dostosowywania (Theme Engine)**:
  * Centralny menedżer motywów (`ThemeManager`), operujący w oparciu o wzorzec obserwatora (`ThemeChangeListener`) oraz pamięć lokalną `SharedPreferences`.

```
[ UI (Activity/Fragment) ] ◄──► [ ThemeManager / SharedPreferences ]
          │
          ▼
[ Repositories / Services ]
          │
          ▼
[ RetrofitClient (HTTP/Gson) ] ──► [ REST API Backend (http://127.0.0.1:13308/) ]
```

---

## 2. Specyfikacja Interfejsów REST API

Aplikacja komunikuje się z serwerem poprzez protokół HTTP REST API. Domyślny punkt końcowy (Base URL) skonfigurowany w [RetrofitClient.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/network/RetrofitClient.java) to:

`http://127.0.0.1:13308/`

### 2.1. Użytkownicy i Uwierzytelnianie (`AuthAPI` / `UserAPI`)

| Metoda | Ścieżka (Endpoint) | Model Żądania | Model Odpowiedzi | Opis |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | `LoginRequest` | `AuthResponse` | Logowanie użytkownika na podstawie loginu/e-maila i hasła |
| `POST` | `/api/uzytkownicy` | `UserRequest` | `UserResponse` | Rejestracja nowego konta użytkownika |
| `GET` | `/api/uzytkownicy` | — | `List<UserResponse>` | Pobranie listy wszystkich użytkowników |
| `GET` | `/api/uzytkownicy/{id}` | — | `UserResponse` | Pobranie danych profilowych użytkownika wg ID |
| `PUT` | `/api/uzytkownicy/{id}` | `UserRequest` | `UserResponse` | Aktualizacja danych użytkownika (np. zmiana hasła) |

#### Przykład obiektu `UserRequest`:
```json
{
  "imie": "Jan",
  "nazwisko": "Kowalski",
  "login": "jkowalski",
  "email": "jan.kowalski@example.com",
  "haslo": "Haslo123!"
}
```

#### Przykład obiektu `AuthResponse`:
```json
{
  "success": true,
  "message": "Zalogowano pomyślnie",
  "uzytkownikId": 102,
  "nick": "jkowalski"
}
```

### 2.2. Konta i Salda (`KontoAPI`)

| Metoda | Ścieżka (Endpoint) | Parametry | Odpowiedź | Opis |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/konta/uzytkownik/{uzytkownikId}/saldo` | `uzytkownikId` | `SaldoResponse` | Pobranie bieżącego salda konta użytkownika |
| `GET` | `/api/konta/uzytkownik/{uzytkownikId}` | `uzytkownikId` | `List<Map<String, Object>>` | Pobranie pełnej listy rachunków bankowych użytkownika |

---

## 3. Zarządzanie Sesją i Bezpieczeństwo

### 3.1. Przekazywanie Kontekstu Użytkownika
Po pomyślnym zalogowaniu, ID użytkownika (`uzytkownikId`) oraz nick (`nick`) są przekazywane pomiędzy aktywnościami i fragmentami jako parametry `Intent Extra`:

```java
Intent intent = new Intent(context, MainMenuActivity.class);
intent.putExtra("uzytkownikId", uzytkownikId);
intent.putExtra("nick", nick);
intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
startActivity(intent);
```

### 3.2. Weryfikacja Działań Wrażliwych (Re-Authentication)
W celu zagwarantowania bezpieczeństwa bankowego, kluczowe operacje wymagają ponownej weryfikacji hasła użytkownika przed ich wykonaniem:

* **Odsłonięcie pełnych danych karty debetowej** (numer karty, data ważności).
* **Odsłonięcie pełnego numeru rachunku IBAN**.
* **Zmiana limitów płatności dziennych**.
* **Zmiana hasła logowania**.

Wykorzystywany jest do tego dedykowany ekran weryfikacji [dialog_verify_password.xml](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/res/layout/dialog_verify_password.xml), który wysyła żądanie weryfikacyjne do metody `authAPI.login(...)`.

---

## 4. Silnik Motywów (ThemeManager)

Aplikacja posiada dynamiczny system motywów zlokalizowany w klasie [ThemeManager.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/theme/ThemeManager.java).

### 4.1. Oferowane Tryby Kolorystyczne
1. **Tryb Jasny (Light)**: Czysta biel, wysoki kontrast, jasne tła kart.
2. **Tryb Ciemny / Czarny (Dark / Black)**: Głęboka czerń (`#121212` / `#000000`), redukująca zmęczenie wzroku i zużycie baterii na ekranach OLED.
3. **Dostosowanie Koloru Akcentu**: Niebieski, Zielony, Fioletowy, Czerwony, Pomarańczowy.

### 4.2. Wzorzec Obserwatora (Observer Pattern)
Komponenty UI rejestrują się w `ThemeManager`, co pozwala na natychmiastowe odświeżenie kolorystyki interfejsu bez konieczności restartowania aplikacji:

```java
public interface ThemeChangeListener {
    void onThemeChanged();
}

// Rejestracja w cyklu życia
@Override
public void onStart() {
    super.onStart();
    ThemeManager.registerThemeChangeListener(this);
}

@Override
public void onStop() {
    ThemeManager.unregisterThemeChangeListener(this);
    super.onStop();
}
```

---

## 5. Przegląd Modułów i Ekranów

### 5.1. Moduł Autoryzacji (`ui/auth/`)
* **[MainActivity.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/auth/MainActivity.java)**: Ekran logowania z walidacją pól, obsługą błędów HTTP (401, 404, 500) oraz przejściem do rejestracji.
* **[RegisterActivity.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/auth/RegisterActivity.java)**: Formularz zakładania konta sprawdzający unikalność loginu (kod HTTP 409 Conflict).

### 5.2. Moduł Główny (`ui/home/`)
* **[MainMenuActivity.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/home/MainMenuActivity.java)**: Centralny pulpit aplikacji. Obsługuje podgląd salda, pasek nawigacyjny, wysuwany panel (Drawer) oraz listę ostatnich transakcji.
* **[TransferActivity.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/home/TransferActivity.java) / [TransferFragment.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/home/TransferFragment.java)**: Moduł przelewów z potwierdzeniem odbiorcy, wyborem kwoty oraz tytułu.
* **[CardsActivity.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/home/CardsActivity.java) / [CardsFragment.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/home/CardsFragment.java)**: Zarządzanie wirtualną kartą płatniczą, blokowanie operacji oraz zmiana dziennych limitów.
* **[ProfileActivity.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/home/ProfileActivity.java)**: Szczegółowe dane osobowe, numer IBAN, identyfikator użytkownika oraz zmiana hasła logowania.
* **[SettingsActivity.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/home/SettingsActivity.java) / [SettingsFragment.java](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/java/com/example/bank3/ui/home/SettingsFragment.java)**: Ustawienia aplikacji, powiadomienia push, wybór motywu oraz bezpieczne wylogowanie.

---

## 6. Katalog Okien Dialogowych (Dialogs)

Wszystkie okna dialogowe w aplikacji posiadają spójne, zaokrąglone tło oraz przezroczysty bufor okna systemowego (`android.R.color.transparent`):

| Plik Layout XML | Nazwa Okna | Opis / Zastosowanie |
| :--- | :--- | :--- |
| [dialog_logout.xml](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/res/layout/dialog_logout.xml) | Okno Wylogowania | Potwierdzenie wyjścia z konta z bezpiecznym wyczyszczeniem stosu aktywności |
| [dialog_verify_password.xml](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/res/layout/dialog_verify_password.xml) | Weryfikacja Tożsamości | Monit o hasło przed odkryciem danych karty lub numeru IBAN |
| [dialog_confirm_transfer.xml](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/res/layout/dialog_confirm_transfer.xml) | Potwierdzenie Przelewu | Podsumowanie kwoty, odbiorcy oraz tytułu przed realizacją transakcji |
| [dialog_transfer_success.xml](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/res/layout/dialog_transfer_success.xml) | Sukces Przelewu | Komunikat pomyślnego wysłania środków |
| [dialog_change_password.xml](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/res/layout/dialog_change_password.xml) | Zmiana Hasła | Formularz wprowadzania aktualnego oraz nowego hasła |
| [dialog_change_limit.xml](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/res/layout/dialog_change_limit.xml) | Zmiana Limitu | Ustawianie maksymalnego dziennego limitu przelewów |
| [dialog_full_history.xml](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/res/layout/dialog_full_history.xml) | Pełna Historia | Wysuwany panel (BottomSheet) z pełną listą transakcji |
| [dialog_notifications.xml](file:///C:/Users/praktyk1/AndroidStudioProjects/projekt-praktyki/app/src/main/res/layout/dialog_notifications.xml) | Powiadomienia | Wysuwany panel powiadomień systemowych |

---

## 7. Struktura Projektu i Klas

```
app/src/main/java/com/example/bank3/
├── data/
│   ├── api/
│   │   ├── AuthAPI.java
│   │   ├── KontoAPI.java
│   │   └── UserAPI.java
│   └── repository/
│       ├── AuthRepository.java
│       └── KontoRepository.java
├── model/
│   ├── AuthResponse.java
│   ├── LoginRequest.java
│   ├── SaldoResponse.java
│   ├── UserRequest.java
│   └── UserResponse.java
├── network/
│   └── RetrofitClient.java
├── ui/
│   ├── auth/
│   │   ├── MainActivity.java
│   │   └── RegisterActivity.java
│   ├── home/
│   │   ├── CardsActivity.java
│   │   ├── CardsFragment.java
│   │   ├── MainMenuActivity.java
│   │   ├── NotificationHelper.java
│   │   ├── ProfileActivity.java
│   │   ├── SettingsActivity.java
│   │   ├── SettingsFragment.java
│   │   ├── ThemesActivity.java
│   │   ├── TransferActivity.java
│   │   └── TransferFragment.java
│   └── theme/
│       └── ThemeManager.java
```

---

*Dokumentacja as of v1.0.0 (Android SDK 37).*






















<p align="left">
  <img src="American-P.jpeg" width="500">
</p>

*Powered by sprint.xml*

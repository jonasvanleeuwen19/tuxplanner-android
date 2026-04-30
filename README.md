# TuxPlanner Android

Native Android client for the [TuxPlanner](https://github.com/jonasvanleeuwen19/TuxPlanner) self-hosted calendar & todo app.

Built with **Kotlin + Jetpack Compose + Material 3**.

---

## Features

| Feature | Status |
|---|---|
| Login / First-run setup | ✅ |
| Configurable backend URL | ✅ |
| Events list (create, delete) | ✅ |
| Todos list (create, toggle, delete) | ✅ |
| Material 3 UI + dark mode | ✅ |
| GitHub Actions release build | ✅ |

---

## Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Networking | Retrofit 2 + OkHttp 4 |
| JSON | Gson |
| Auth | Cookie-based (HttpOnly JWT via OkHttp CookieJar) |
| Preferences | Jetpack DataStore |
| Navigation | Navigation Compose |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |

---

## Running Locally

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with API 35

### Steps

1. **Clone the repo**
   ```bash
   git clone https://github.com/jonasvanleeuwen19/tuxplanner-android.git
   cd tuxplanner-android
   ```

2. **Open in Android Studio**
   Open the root folder (`tuxplanner-android/`) as an Android Studio project.

3. **Set the backend URL** *(first launch)*
   - The app defaults to `http://10.0.2.2:8000` which routes to `localhost:8000` on the host machine when using the Android Emulator.
   - If running a real device, use your machine's LAN IP (e.g., `http://192.168.1.x:8000`).
   - You can change the URL at any time in **Settings → Backend URL**.

4. **Start the TuxPlanner backend**
   ```bash
   # In the TuxPlanner repo
   docker compose up --build
   ```

5. **Run the app** via Android Studio (▶) or:
   ```bash
   ./gradlew installDebug
   ```

### First-run setup

If no users exist on the backend yet, the app shows a **"Create Account"** screen instead of the login form. Enter a username and password (min 8 characters) to create the first admin account.

---

## Project Structure

```
app/src/main/java/com/tuxplanner/app/
├── TuxPlannerApp.kt          # Application class + AppContainer (service locator)
├── MainActivity.kt
├── data/
│   ├── model/
│   │   └── Models.kt         # Data classes matching the API
│   ├── network/
│   │   ├── TuxPlannerApiService.kt  # Retrofit interface
│   │   └── ApiClient.kt      # OkHttp + Retrofit factory (cookie jar, dynamic URL)
│   ├── preferences/
│   │   └── AppPreferences.kt # DataStore wrapper (base URL, login state)
│   └── repository/
│       ├── AuthRepository.kt
│       ├── EventRepository.kt
│       └── TodoRepository.kt
└── ui/
    ├── navigation/
    │   ├── Screen.kt          # Route constants
    │   └── AppNavHost.kt      # Root NavHost
    ├── screens/
    │   ├── login/             # LoginScreen + LoginViewModel
    │   ├── home/              # HomeScreen (bottom navigation host)
    │   ├── events/            # EventsScreen + EventsViewModel
    │   ├── todos/             # TodosScreen + TodosViewModel
    │   └── settings/          # SettingsScreen + SettingsViewModel
    └── theme/                 # Material 3 colors, typography, theme
```

---

## GitHub Actions – Release Build

The workflow file is at `.github/workflows/android-release.yml`.

### What it does

| Trigger | Action |
|---|---|
| Push to `main` | Builds release APK, uploads as workflow artifact |
| Pull request to `main` | Same (build check) |
| Tag `v*` (e.g., `v1.0.0`) | Builds release APK **+ creates a GitHub Release** with the APK attached |

### Creating a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow will automatically create a GitHub Release named **"TuxPlanner Android v1.0.0"** with:
- Auto-generated release notes
- The signed APK attached

### Signing

By default the release APK is **signed with the debug key** (installable on any device for testing, but not suitable for Play Store).

To use a real signing key:

1. Generate a keystore:
   ```bash
   keytool -genkey -v -keystore my-release-key.jks \
     -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Base64-encode it:
   ```bash
   base64 -i my-release-key.jks | tr -d '\n'
   ```

3. Add these **repository secrets** (`Settings → Secrets → Actions`):
   | Secret | Value |
   |---|---|
   | `KEYSTORE_BASE64` | base64 output from step 2 |
   | `KEYSTORE_PASSWORD` | your keystore password |
   | `KEY_ALIAS` | your key alias |
   | `KEY_PASSWORD` | your key password |

---

## Backend API Summary

| Endpoint | Method | Description |
|---|---|---|
| `/api/auth/setup-status` | GET | Check if first-run setup is needed |
| `/api/auth/setup` | POST | Create first admin account |
| `/api/auth/login` | POST | Login (form-encoded) → sets httpOnly cookie |
| `/api/auth/logout` | POST | Clear auth cookie |
| `/api/auth/me` | GET | Get current user info |
| `/api/events/` | GET/POST | List or create events |
| `/api/events/{id}` | GET/PUT/DELETE | Read, update, or delete event |
| `/api/todos/` | GET/POST | List or create todos |
| `/api/todos/{id}` | GET/PUT/DELETE | Read, update, or delete todo |

Authentication uses an **httpOnly JWT cookie** (`access_token`). OkHttp's `JavaNetCookieJar` handles this transparently.

---

## License

MIT

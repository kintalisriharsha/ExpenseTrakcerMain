# Expenzo — Expense Tracker (Android)

Expenzo is a native Android expense tracker built with **Kotlin** and **Jetpack Compose**. It can automatically detect transactions from bank SMS alerts, store everything locally in a **Room** database, and surface spending insights through an in-app analytics screen and two home-screen widgets — all without a backend server or network layer.

## Features

- **Automatic SMS transaction detection** — a `BroadcastReceiver` listens for incoming SMS and parses common bank-alert formats (debited/credited, ₹/Rs./USD amounts, merchant/payee names) to auto-log expenses via a foreground `SmsProcessingService`.
- **Manual expense management** — add, edit, and view expense details (`AddExpense`, `EditExpense`, `DetailScreen`).
- **Spending history & analytics** — a searchable history screen plus a dedicated analytics screen for spending trends.
- **To-do list** — a simple todo feature with its own history screen, backed by Room.
- **Home-screen widgets** (via Jetpack Glance) — an **Expense widget** showing spend against daily/weekly/monthly budgets, and a **Todo widget** showing pending tasks at a glance.
- **Notification history** — a log of past notifications/alerts related to detected transactions.
- **Contacts integration** — reads device contacts to help label transactions (e.g. peer-to-peer transfers).
- **Settings** — persisted app preferences (dark mode, notifications, etc.) via `AppPreferences`.
- **Reliable background processing** — a `BackgroundReliabilityHelper` prompts the user to exempt the app from battery optimizations so SMS parsing and widget refresh keep working in the background.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM (`ViewModel` + `Repository` per feature) |
| Local storage | Room (`room-runtime`, `room-ktx`, `room-paging`) |
| Navigation | Jetpack Navigation Compose |
| Widgets | Jetpack Glance (`glance-appwidget`) |
| Async | Kotlin Coroutines / Flow |
| Build | Gradle (Kotlin DSL), KSP for annotation processing |

> Note: this project has no networking layer (no Retrofit/API calls) — all data is stored and processed on-device with Room.

## Project Structure

```
app/src/main/java/com/example/expensetracker/
├── MainActivity.kt                 # Entry point, nav host, permission requests
├── ExpenseTrackerApp.kt            # Application class; keeps widgets in sync with Room
├── frontend/
│   ├── components/                 # Reusable Compose UI components
│   ├── important/                  # App state, preferences, contacts, background helper
│   ├── screens/                    # Home, Add/Edit Expense, Analytics, History, Todo, Settings...
│   ├── services/
│   │   ├── expenseService/         # Expense entity, DAO, repository, view model
│   │   ├── analyticsService/       # Analytics entity, DAO, repository, view model
│   │   ├── homeService/            # Home screen data layer
│   │   ├── settingService/         # Settings entity, DAO, repository, view model
│   │   ├── TodoService/            # Todo entity, DAO, repository, view model
│   │   └── important/              # Room database, type converters
│   ├── sms/                        # SMS receiver, parser, and processing service
│   └── widgets/                    # Glance app widgets (Expense, Todo)
└── ui/theme/                       # Compose theme, color, and typography
```

## Requirements

- Android Studio (Ladybug or newer recommended)
- JDK 11
- Android SDK: `compileSdk 37`, `minSdk 24`, `targetSdk 36`

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/kintalisriharsha/ExpenseTrakcerMain.git
   ```
2. Open the project in Android Studio and let Gradle sync.
3. Build and run on an emulator or device:
   ```bash
   ./gradlew assembleDebug
   ```
4. Install directly to a connected device/emulator:
   ```bash
   ./gradlew installDebug
   ```

## Permissions

The app requests the following permissions, mainly to power automatic SMS-based expense detection and reliable background updates:

- `READ_SMS`, `RECEIVE_SMS` — detect and parse incoming bank transaction alerts
- `READ_CONTACTS` — help label transactions with contact names
- `READ_PHONE_STATE`, `READ_PHONE_NUMBERS` — telephony context for SMS handling
- `POST_NOTIFICATIONS` — show transaction/notification alerts
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` — run the SMS processing service reliably
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — keep background SMS parsing and widget refresh working

All permissions are optional at runtime where applicable (SMS auto-detection can be skipped in favor of manual entry).

## License

No license file is currently included in this repository. Add one (e.g. MIT, Apache 2.0) if you intend for others to use or contribute to this project.

# Pooja Purohit 🕉️

A service marketplace platform connecting customers with verified Purohits (priests) for religious ceremonies and rituals. Consists of an Android app, a Firebase Cloud Functions backend, an admin review portal, and a data-deletion request portal.

## Repository Structure

```
Pooja_Purohit/
├── app/                                    # Android application (Kotlin)
├── FIREBASE CLOUD FUNCTIONS/               # TypeScript Cloud Functions backend
├── FIRESTORE STRUCTURE POOJA PUROHIT/      # Reference Firestore schema (JSON)
├── POOJA PUROHIT ADMIN PORTAL/             # Static JS admin web portal (Firebase Hosting)
├── POOJA PUROHIT DATA DELETION REQUEST PORTAL/  # Static data-deletion request page
├── gradle/                                 # Gradle wrapper + version catalog
└── build.gradle.kts, settings.gradle.kts   # Root Gradle config
```

## 1. Android App (`app/`)

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose (with some legacy View/DataBinding screens)
- **DI**: Hilt
- **Architecture**: MVVM
- **Auth**: Firebase Auth + Google Sign-In (AndroidX Credentials API)
- **Database**: Firebase Firestore
- **Other**: Lottie animations, Coil, Navigation Compose, Google Maps Compose

### SDK Versions
| | Version |
|---|---|
| Compile SDK | 37 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 |
| Kotlin | 2.3.21 |
| Compose BOM | 2026.05.01 |

### Package Structure
```
app/src/main/java/com/poojapurohit/
├── splash/          # App entry point, session management
├── auth/            # Authentication (login, registration)
├── booking/         # Booking flow, models, data layer
├── bookpurohit/      # Purohit discovery/booking screen
├── dashboard/       # Post-auth main dashboard
├── notification/    # In-app notifications (FCM-backed)
├── di/              # Hilt modules
└── ui/theme/        # Compose theming
```

### Core Features
- Google Sign-In based authentication for customers and Purohits
- Multi-step Purohit registration (experience, specialization, location)
- Purohit discovery and booking flow
- Real-time booking status updates and OTP-based service completion
- Push notifications for booking lifecycle events (payment reminders, booking reminders, day-prior reminders)

## 2. Firebase Cloud Functions (`FIREBASE CLOUD FUNCTIONS/`)

TypeScript backend deployed on Firebase Cloud Functions (Node 24), handling the booking lifecycle end-to-end.

### Exported Functions
| Function | Trigger | Purpose |
|---|---|---|
| `onBookingStatusUpdated` | Firestore write on `bookings/{bookingId}` | Dispatches status-specific notifications and schedules Cloud Tasks reminder chains |
| `processPaymentReminder` | Cloud Tasks queue | Fires every 15 min (up to 4x / 1 hr) while a booking is `PENDING_PAYMENT`; auto-cancels with `NO_PAYMENT` on exhaustion |
| `processBookingReminder` | Cloud Tasks queue | Fires every 15 min (up to 8x / 2 hrs) while `PAYMENT_DONE`; auto-cancels with `NO_PUROHIT_RESPONSE` on exhaustion |
| `processDayPriorReminder` | Cloud Tasks queue | Fires once at T-24h for `ACCEPTED` bookings |
| `onCompletionOtpWritten` | Firestore write on `bookings/{bookingId}` | Notifies the user via Firestore + FCM when a completion OTP is generated |

### Structure
```
FIREBASE CLOUD FUNCTIONS/functions/
├── src/
│   ├── index.ts        # Function exports
│   ├── handlers/        # Firestore/Cloud Tasks trigger handlers
│   ├── services/        # fcm, notification, tasks services
│   ├── config/
│   └── types/
├── package.json
└── firebase.json
```

### Local Development
```bash
cd "FIREBASE CLOUD FUNCTIONS/functions"
npm install
npm run build          # compile TypeScript
npm run serve          # build + start local emulator
npm run shell           # interactive functions shell
npm run deploy          # deploy to Firebase
npm run logs             # tail deployed function logs
```

## 3. Admin Portal (`POOJA PUROHIT ADMIN PORTAL/`)

A static HTML/CSS/JavaScript web app (no build step) hosted on Firebase Hosting, used to review and manage Purohit registrations.

```
POOJA PUROHIT ADMIN PORTAL/src/
├── index.html
├── pooja-purohit-admin-portal-config.js   # Firebase config
├── css/
└── js/
    ├── auth.js        # Admin authentication
    ├── firebase.js    # Firebase SDK init
    ├── purohits.js    # Purohit list/management
    ├── detail.js       # Purohit detail view
    ├── services.js     # Service category management
    ├── modal.js
    └── utils.js
```

Deploy with the Firebase CLI:
```bash
cd "POOJA PUROHIT ADMIN PORTAL"
firebase deploy --only hosting
```

## 4. Data Deletion Request Portal (`POOJA PUROHIT DATA DELETION REQUEST PORTAL/`)

A single static page (`index.html`) allowing users to submit account/data deletion requests — required for Google Play data-safety compliance.

## 5. Firestore Schema Reference (`FIRESTORE STRUCTURE POOJA PUROHIT/`)

`Pooja Purohit Firestore Structure.json` documents the Firestore collection/document layout used across the app, functions, and admin portal.

## Getting Started (Android App)

### Prerequisites
- Android Studio (latest stable)
- JDK 17+
- Android SDK with API level 37
- A Firebase project (Auth + Firestore + Cloud Messaging enabled)

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/AdarshMishraIndia/Pooja_Purohit.git
   cd Pooja_Purohit
   ```
2. Create a Firebase project, register the Android app, and download `google-services.json` into `app/`.
3. Enable **Google Sign-In** in Firebase Authentication and set the OAuth client ID in `app/src/main/res/values/strings.xml`.
4. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```
   or use Android Studio's Run configuration.

### Testing
```bash
./gradlew test                    # unit tests
./gradlew connectedAndroidTest    # instrumented tests
```

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "Add your feature"`
4. Push and open a Pull Request

## License

No license file is currently published in this repository. Add a `LICENSE` file to define usage terms.

---

**Connecting communities with spiritual services.**

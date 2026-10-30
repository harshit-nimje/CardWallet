# 🎴 Card Wallet AI — Next-Gen Business Card Scanner & Networking Suite

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini_3.5_Flash--Lite-8E75FF?style=for-the-badge&logo=google&logoColor=white)](https://deepmind.google/technologies/gemini)
[![Material 3](https://img.shields.io/badge/Design-Material_3_Dark-1D192B?style=for-the-badge)](https://m3.material.io)

**Card Wallet AI** is an intelligent, privacy-focused Android application for scanning, managing, and retrieving physical and digital business cards. Built with modern Jetpack Compose, ML Kit on-device OCR, Gemini 3.5 Flash-Lite multimodal AI, 3D gyroscope tilt, interactive vCard QR generation, and one-tap Google Contacts & Maps integration.

---

## ✨ Key Features

### 📸 1. Multimodal OCR & Rectification Tool Suite
- **ML Kit + Gemini 3.5 Flash-Lite**: Dual-engine extraction detects names, titles, companies, phones, emails, addresses, and websites instantly.
- **Card Rectification & Fine Auto-Crop**:
  - 90° manual rotation tool
  - Fine-angle straightening slider (-15° to +15°)
  - Margin trim border crop slider (0% to 20%)
  - Dynamic aspect ratio lock (Vertical 9:16 vs Horizontal 16:9)

### 📲 2. Interactive Quick-Share QR & vCard (.vcf)
- **Instant Camera QR Scan**: Generates dynamic vCard 3.0/4.0 QR codes on screen. iPhone and Android camera apps can scan and save contacts directly into native contacts without installing any software.
- **One-Tap Google Contacts**: Save card contacts directly to your Google Account or device phonebook.

### 🌐 3. Direct Map Directions & Distance
- **Automated Address Detection**: Displays distance pills (e.g. `3.2 km away`) and direct Google Maps navigation triggers with live transit routing.

### 📦 4. Batch Selection, vCard & CSV Export
- **Bulk Export**: Select multiple cards in the wallet to export `.vcf` vCard bundles or `.csv` spreadsheets compatible with Google Contacts, Outlook, and CRM software.
- **JSON Wallet Backup/Restore**: Import and export your complete encrypted wallet with one tap.

### 🧠 5. AI Network Retrieval ("Who in my network can help?")
- **Natural Language Search**: Ask queries like *"Need an IP lawyer or seed fund advisor"* to find relevant contacts in your wallet based on roles, notes, and industry tags.

### 🌌 6. Tactile 3D Gyro Parallax & Dynamic Colors
- **Hardware Gyroscope Sensors**: Tilting your device casts physical holographic sheen sweeps across card surfaces.
- **Haptic Feedback**: Micro-vibration tactile soundscapes on card flips and category page swipes.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose with Material Design 3 (Dark Obsidian Dynamic Theme)
- **Architecture**: MVVM + Clean Repository Pattern with Kotlin Coroutines & StateFlow
- **Local Database**: Room DB (SQLite) with encrypted state options
- **Cloud Synchronization**: Firebase Firestore & Firebase Auth (Google Sign-In via Credential Manager)
- **OCR Engine**: Google ML Kit Vision + Gemini 3.5 Flash-Lite
- **Barcode & QR Engine**: ZXing Barcode Matrix
- **Hardware Integration**: Android `SensorManager` (Rotation Vector / Accelerometer), `CameraX`, and `Vibrator`

---

## 🚀 Building & Releasing

### Requirements
- Android Studio Ladybug or newer
- JDK 17+
- Android SDK 34 (Android 14)

### Clone & Build Debug APK

```bash
# Clone the repository
git clone https://github.com/your-username/card-wallet-ai.git
cd card-wallet-ai

# Build debug APK using Gradle
./gradlew assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### ⚡ Automated Builds with GitHub Actions

A pre-configured GitHub Actions workflow (`.github/workflows/build-apk.yml`) is included in the project:
1. Go to your GitHub repository -> **Actions** tab.
2. Select **Build Android APK** from the left sidebar.
3. Click **Run workflow** -> **Run workflow**.
4. Once completed (~2 minutes), click on the workflow run to download the generated **`CardWallet-AI-Debug-APK`** artifact directly!

---

## 🏷️ GitHub Release Instructions

1. Go to your GitHub Repository -> **Releases** -> **Draft a new release**.
2. Tag version: `v1.0.0`
3. Target branch: `main`
4. Release title: `Card Wallet AI v1.0.0 — Production Release`
5. Attach the pre-built APK artifact downloaded from GitHub Actions.
6. Click **Publish release**.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

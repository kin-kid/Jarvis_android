# J.A.R.V.I.S. Android Assistant for Samsung Galaxy A16

A native Android voice assistant built in Kotlin with Tony Stark's futuristic JARVIS interface, designed specifically for the **Samsung Galaxy A16** (Android 14 / One UI 6).

---

## Key Features

1. **Futuristic Stark HUD Interface**:
   - Deep obsidian background (`#030712`) with neon holographic cyan (`#00F0FF`) and arc reactor pulse animations.
   - Interactive acoustic sensor trigger (Microphone FAB) with real-time soundwave reactivity.

2. **Offline Hardware Voice Commands**:
   - **Open Installed Apps**: Instant launch of Camera, YouTube, WhatsApp, Chrome, Phone, Gallery, Settings, Calculator, Spotify, Maps, and any installed app on your Samsung A16.
   - **System Navigation**: "Go back", "Go home", "Recent apps", "Show notifications".
   - **Safe Screen Lock**: "Lock screen" / "Lock phone" using official Android 9+ `GLOBAL_ACTION_LOCK_SCREEN`.

3. **Text-To-Speech (TTS)**:
   - Customized voice synthesis tuned to Paul Bettany's JARVIS cadence (British baritone pitch 0.92, rate 1.02).

4. **Online Intelligence (Gemini AI)**:
   - Switch between **Local Matrix (100% offline)** and **Gemini 3.8 Flash Neural Net** for answering complex questions, calculations, and general conversation.

5. **Lock Screen Security & Samsung Knox Compliance**:
   - **Crucial Rule**: The app NEVER attempts to bypass PIN, pattern, or fingerprint lock.
   - It respects Android's Knox hardware security module (TrustZone).
   - The lock command safely puts the phone to sleep and engages the lock screen, protecting your device.

---

## How to Build the APK on Your Phone (Without a Computer)

Since you are developing directly on your Samsung Galaxy A16 without a PC, here are the **3 best methods** to build and install your APK:

### Method 1: GitHub Cloud Build (Recommended — 100% Free & Automatic)
*This is the easiest method for beginners. GitHub will compile the APK in the cloud for you in 90 seconds.*

1. Open Chrome or Samsung Internet on your Galaxy A16 and visit **[github.com](https://github.com)**.
2. Sign in or create a free account.
3. Tap **+** > **New repository** (Name it `jarvis-android`).
4. Tap **Add file** > **Upload files** and upload the contents of this project (or import the ZIP).
5. GitHub will automatically detect `.github/workflows/build-apk.yml`.
6. Tap the **Actions** tab on your phone:
   - Watch the build turn green (takes ~90 seconds).
   - Under **Artifacts**, tap `JARVIS-Samsung-A16-Debug-APK`.
7. Download the `.zip` or `.apk` directly to your phone.
8. Open the Samsung **My Files** app > **Downloads**, tap `app-debug.apk`, and tap **Install**!

---

### Method 2: Termux On-Device Build (100% Offline on Phone)
*You can compile Android code directly on your Galaxy A16's octa-core processor using Termux.*

1. Install **Termux** from F-Droid (do not use Google Play Termux as it is outdated).
2. Open Termux on your Samsung A16 and run:
   ```bash
   pkg update -y
   pkg install openjdk-17 git gradle aapt apksigner -y
   termux-setup-storage
   ```
3. Navigate to your project folder:
   ```bash
   cd ~/storage/shared/Download/JARVIS
   gradle assembleDebug
   ```
4. The generated APK will be at:
   `app/build/outputs/apk/debug/app-debug.apk`
5. Tap it in your file manager to install!

---

### Method 3: AIDE (Android IDE on Phone)
1. Download **AIDE - IDE for Android Java/Kotlin** from the app store.
2. Tap **Open Project** > browse to your downloaded JARVIS folder.
3. Tap the **Play / Run** button at the top right. AIDE compiles and prompts you to install the APK directly!

---

## Activating Permissions on Samsung Galaxy A16 (One UI 6)

1. **Microphone Permission**:
   - When you open the app, tap **Allow** when prompted for Audio Recording.

2. **Enable Navigation & Lock Accessibility Service**:
   - Open Samsung **Settings**.
   - Scroll down to **Accessibility** (`접근성` / Accessibility).
   - Tap **Installed apps** (or **Downloaded apps**).
   - Tap **JARVIS Navigation Controller**.
   - Toggle the switch to **ON** and tap **Allow**.
   - *This grants JARVIS the ability to navigate back, home, and lock your screen.*

---

## Voice Commands Cheat Sheet

| Command | Action | Works Offline? |
| :--- | :--- | :---: |
| *"Jarvis, open camera"* | Launches Samsung Camera | ✅ Yes |
| *"Open WhatsApp"* | Launches WhatsApp messenger | ✅ Yes |
| *"Open YouTube"* | Launches YouTube app | ✅ Yes |
| *"Go back"* | Navigates back one screen | ✅ Yes |
| *"Take me home"* | Returns to Android home screen | ✅ Yes |
| *"Show recent apps"* | Opens app switcher | ✅ Yes |
| *"Lock the phone"* | Locks screen (Knox protected) | ✅ Yes |
| *"What is quantum entanglement?"* | Answers with Gemini AI | 🌐 Online |
| *"Status report"* | Reports system telemetry | ✅ Yes |

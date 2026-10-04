# Tele Expense Counter

Offline-first Ethiopian telecom expense tracker.

> Where is my telecom money going?

SMS is parsed **on-device only**. No account, no cloud, no upload.

## What this build fixes (learned from failed CI)

| Problem | Fix |
|---------|-----|
| `android-actions/setup-android@v3` hangs on `y/N` licenses | **Removed**. Use runner preinstalled SDK + `yes \| sdkmanager --licenses` |
| CI ran unit tests before APK | **APK-only** CI (`assembleDebug`) |
| Old workflow still running after edit | Commit **this** `.github/workflows/android-ci.yml` and confirm log shows `Setup Android SDK`, **not** `setup-android@v3` |

## GitHub build (recommended)

```bash
git init
git add .
git commit -m "Tele Expense Counter v1"
git branch -M main
git remote add origin https://github.com/YOUR_USER/tele-expense-counter.git
git push -u origin main
```

Then: **Actions** → wait for **Build debug APK** → download artifact **`app-debug`**.

Manual run: Actions → Android CI → Run workflow.

## Product

- Rule-based SMS parser (COUNT / IGNORE / REFERENCE)
- Groups multi-SMS into one expense (e.g. 3× ETB 5 eBirr → 1)
- Ethiopian calendar first
- Room local DB; full SMS bodies not stored
- First run: scan **current month only** or start from today
- After that: only new SMS

## Stack

Kotlin · Jetpack Compose · Room · minSdk 26 · compileSdk 34

## Local (optional)

JDK 17 + Android SDK:

```bash
./gradlew assembleDebug
```

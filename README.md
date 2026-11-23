# SylesStacks Mobile

Native Android launcher, first development milestone.

## Current scope
- Lists apps available from Android's launcher.
- Shows Android-provided app icons and labels.
- Filters the list with search.
- Opens the selected installed app.

## Build
Open this `android` folder as a Gradle project in Android Studio, or run:

    gradle assembleDebug

The debug APK is created at:

    app/build/outputs/apk/debug/app-debug.apk

GitHub Actions also builds the debug APK when files in `android/` change.

## Not implemented yet
This is not an Android virtual machine or BlueStacks replacement. It does not run a second Android system, include Google Play services, or install APKs silently. APK installation and any virtualization research are later milestones and must use Android's supported user-confirmed flows.

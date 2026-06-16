<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Fitness Tracker

This repository is set up to run locally in Android Studio.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Let Gradle sync finish
4. Create a file named `.env` in the project root and set `GEMINI_API_KEY` there (see `.env.example`)
5. If you want Google Sign-In to work on your debug build, add your local debug SHA-1 and SHA-256 fingerprints to the Firebase project and download the matching `google-services.json` if required
6. Run the app on an emulator or physical device

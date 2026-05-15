# Tsugiba — Walking Navigation App

An Android app that extends Google Maps with real-time traffic-aware suggestions for walkers and runners.

## Features
- Live pedestrian routing via Google Maps SDK
- Real-time traffic suggestions while walking/running
- Alternate route recommendations when congestion detected
- Activity modes: Walk, Jog, Run
- Background location tracking with foreground service
- Turn-by-turn navigation overlay

## Setup

1. Obtain a Google Maps API key from [Google Cloud Console](https://console.cloud.google.com).
2. Enable: Maps SDK for Android, Directions API, Roads API.
3. Add your key to `local.properties`:
   ```
   MAPS_API_KEY=your_key_here
   ```
4. Build and run on Android 7.0+ (API 24+).

## Architecture
MVVM + Repository pattern, Kotlin Coroutines, Retrofit, Hilt DI.

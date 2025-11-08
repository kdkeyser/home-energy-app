# Car Charger Control App

A Kotlin Android application for managing an electric vehicle car charger. This app allows users to control charging operations and configure charging current settings.

## Features

- **Start/Stop Charging**: Toggle the charging state with a single button
- **Charging Modes**:
  - **Solar Power Mode**: Automatically determines charging current based on available solar power
  - **Manual Mode**: Manually set the charging current between 6 and 32 amps
- **Real-time Status Display**: View current charging status and settings
- **Modern UI**: Built with Jetpack Compose and Material Design 3

## Screenshots

The app features:
- Large, clear charging status indicator
- Easy-to-use mode selection with radio buttons
- Slider control for manual current adjustment (6-32 amps)
- Summary card showing current settings

## Technical Details

### Built With

- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern UI toolkit
- **Material Design 3**: UI components and theming
- **ViewModel**: State management with StateFlow
- **Android SDK**: Target SDK 34, Minimum SDK 24

### Architecture

The app follows modern Android development practices:
- **MVVM Pattern**: Separation of UI and business logic
- **Reactive UI**: StateFlow for state management
- **Compose**: Declarative UI framework

### Project Structure

```
app/
├── src/main/
│   ├── java/com/example/carcharger/
│   │   └── MainActivity.kt          # Main activity with UI and ViewModel
│   ├── res/
│   │   ├── values/
│   │   │   ├── strings.xml          # String resources
│   │   │   ├── colors.xml           # Color definitions
│   │   │   └── themes.xml           # App theme
│   │   └── layout/                  # (Compose-based, no XML layouts)
│   └── AndroidManifest.xml          # App manifest
└── build.gradle.kts                 # App-level build configuration
```

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Gradle 8.2
- Kotlin 1.9.0
- JDK 8 or higher

## Installation

### Option 1: Open in Android Studio

1. Clone or download this repository
2. Open Android Studio
3. Select "Open an Existing Project"
4. Navigate to the project directory and select it
5. Wait for Gradle sync to complete
6. Connect an Android device or start an emulator
7. Click "Run" (green play button) or press Shift+F10

### Option 2: Build from Command Line

```bash
# Build the APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Build and install
./gradlew build
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## Usage

### Starting the App

1. Launch the "Car Charger" app from your device
2. The app opens with charging stopped by default

### Controlling Charging

1. **To Start Charging**: Tap the "START CHARGING" button
2. **To Stop Charging**: Tap the "STOP CHARGING" button (appears when charging is active)

### Selecting Charging Mode

#### Solar Power Mode (Default)
- Select the "Solar Power (Automatic)" radio button
- The charging current will be automatically determined based on available solar power
- Best for maximizing use of renewable energy

#### Manual Mode
- Select the "Manual Current" radio button
- A slider will appear below
- Drag the slider to set the desired charging current (6-32 amps)
- The current value is displayed above the slider

### Understanding the Display

- **Charging Status Card**: Shows "CHARGING" (blue) or "STOPPED" (red)
- **Charging Mode Card**: Select between Solar and Manual modes
- **Manual Current Control**: Appears only in Manual mode
- **Current Settings Card**: Summary of active mode and current setting

## App States

The app maintains the following state:
- `isCharging`: Boolean indicating if charging is active
- `chargingMode`: Either SOLAR or MANUAL
- `manualCurrent`: Integer value between 6-32 (only used in Manual mode)

## Customization

### Changing Colors

Edit `app/src/main/res/values/colors.xml` to customize the color scheme.

### Modifying Current Range

In [`MainActivity.kt`](app/src/main/java/com/example/carcharger/MainActivity.kt:169), adjust the slider parameters:

```kotlin
Slider(
    value = uiState.manualCurrent.toFloat(),
    onValueChange = { viewModel.setManualCurrent(it.toInt()) },
    valueRange = 6f..32f,  // Change these values
    steps = 25,            // Adjust number of steps
    modifier = Modifier.fillMaxWidth()
)
```

### Adding Features

The [`ChargerViewModel`](app/src/main/java/com/example/carcharger/MainActivity.kt:245) class manages app state. Add new functions here to extend functionality:

```kotlin
class ChargerViewModel : ViewModel() {
    // Add new state properties
    // Add new functions to modify state
}
```

## Building for Release

1. Generate a signing key:
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

2. Add signing configuration to `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("my-release-key.jks")
            storePassword = "your-password"
            keyAlias = "my-key-alias"
            keyPassword = "your-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

3. Build release APK:
```bash
./gradlew assembleRelease
```

## Troubleshooting

### Gradle Sync Issues
- Ensure you have a stable internet connection
- Try "File > Invalidate Caches / Restart" in Android Studio
- Check that you have the correct Android SDK installed

### Build Errors
- Verify JDK version (should be 8 or higher)
- Clean and rebuild: `./gradlew clean build`
- Check that all dependencies are available

### App Crashes
- Check Logcat in Android Studio for error messages
- Ensure minimum SDK version (24) is met by your device

## Future Enhancements

Potential features to add:
- Persistent state storage (save settings between app restarts)
- Charging history and statistics
- Real-time power consumption display
- Integration with actual charger hardware via Bluetooth/WiFi
- Scheduling features (start/stop at specific times)
- Push notifications for charging status changes
- Multiple charger profiles

## License

This project is provided as-is for educational and development purposes.

## Contributing

Feel free to fork this project and submit pull requests with improvements.

## Support

For issues or questions, please create an issue in the project repository.
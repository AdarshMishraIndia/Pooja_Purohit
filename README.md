# Pooja Purohit 🕉️

A modern Android application that connects customers with qualified religious service providers (Purohits) for various ceremonies and rituals.

## 📱 Overview

Pooja Purohit is a service marketplace platform that bridges the gap between people seeking religious services and certified Purohits. The app provides a seamless experience for both customers looking for religious ceremonies and service providers offering their expertise.

## ✨ Features

### For Customers
- **Easy Registration**: Quick sign-up with Google authentication
- **Service Discovery**: Browse and find qualified Purohits in your area
- **Secure Authentication**: Firebase-based authentication system

### For Service Providers (Purohits)
- **Professional Registration**: Multi-step registration process with experience validation
- **Service Specialization**: Select and showcase specific religious services offered
- **Profile Management**: Comprehensive profile setup with location and experience details

### General Features
- **Modern UI/UX**: Clean, intuitive interface with Material Design
- **Splash Screen**: Elegant app launch experience with session management
- **Multi-user Support**: Separate flows for customers and service providers
- **Real-time Data**: Firebase Firestore integration for live data synchronization

## 🛠️ Tech Stack

### Core Technologies
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI Framework**: Android Views with Data Binding & View Binding
- **Minimum SDK**: API 23 (Android 6.0)
- **Target SDK**: API 36

### Key Dependencies
- **Authentication**: Firebase Auth with Google Sign-In
- **Database**: Firebase Firestore
- **UI Components**: Material Design Components
- **Animations**: Lottie Animations
- **Credentials**: AndroidX Credentials API
- **Lifecycle**: AndroidX Lifecycle & ViewModel
- **Splash Screen**: AndroidX Core SplashScreen

## 🏗️ Project Structure

```
app/src/main/java/com/poojapurohit/
├── SplashActivity.kt                 # App entry point with session management
├── auth/                            # Authentication module
│   ├── AuthActivity.kt              # Main authentication screen
│   ├── AuthViewModel.kt             # Authentication business logic
│   ├── AuthUiManager.kt             # UI state management
│   ├── AuthRepository.kt            # Data layer for auth operations
│   └── adapter/                     # RecyclerView adapters
└── dashboard/                       # Main app dashboard
    └── DashActivity.kt              # Post-authentication main screen
```

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version recommended)
- JDK 11 or higher
- Android SDK with API level 36
- Firebase project setup

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd PoojaPurohit
   ```

2. **Firebase Setup**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Add your Android app to the Firebase project
   - Download `google-services.json` and place it in the `app/` directory
   - Enable Authentication and Firestore in your Firebase project
   - Configure Google Sign-In in Firebase Authentication

3. **Google OAuth Setup**
   - Update the `google_client_id` in `app/src/main/res/values/strings.xml`
   - Replace with your actual Google OAuth client ID

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use Android Studio's build and run functionality.

## 🔧 Configuration

### Firebase Configuration
Ensure your `google-services.json` file is properly configured with:
- Authentication providers (Google)
- Firestore database
- Proper package name matching your app

### Build Configuration
The app is configured with:
- **Compile SDK**: 36
- **Min SDK**: 23
- **Target SDK**: 36
- **Java Version**: 11
- **Kotlin JVM Target**: 11

## 📋 App Flow

1. **Splash Screen**: Checks user authentication status
2. **Authentication**: 
   - New users can sign up as customers or service providers
   - Existing users are redirected to dashboard
3. **Registration Flow**:
   - **Customers**: Basic profile setup
   - **Service Providers**: Multi-step process including experience and specialization
4. **Dashboard**: Main app functionality (post-authentication)

## 🧪 Testing

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Test Structure
- Unit tests: `app/src/test/`
- Instrumented tests: `app/src/androidTest/`

## 🔐 Security Features

- Firebase Authentication with Google OAuth
- Secure credential management using AndroidX Credentials API
- Input validation and sanitization
- Secure data transmission with Firebase

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex business logic
- Maintain consistent indentation and formatting


## 📞 Support

For support and questions:
- Create an issue in the GitHub repository
- Contact the development team

## 🔄 Version History

- **v1.0.0**: Initial release with core authentication and registration features

## 🚧 Roadmap

- [ ] Service booking functionality
- [ ] Payment integration
- [ ] Rating and review system
- [ ] Advanced search and filtering
- [ ] Multi-language support

---

**Built with ❤️ for connecting communities with spiritual services**

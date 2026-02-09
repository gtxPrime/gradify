# Gradify

<div align="center">

![Gradify Banner](https://img.shields.io/badge/Gradify-Your%20Academic%20Companion-6C63FF?style=for-the-badge)

**Your All-in-One Academic Success Platform**

[![License](https://img.shields.io/badge/License-Modified%20MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Version](https://img.shields.io/badge/Version-Mark%207-orange.svg)](https://github.com/gtxPrime/gradify/releases)

### [Features](#-features) | [Tech Stack](#-tech-stack) | [Installation](#-installation) | [Contributing](#-contributing)

</div>

---

## 📱 About Gradify

Gradify is not just another study app; it's a comprehensive academic ecosystem designed to help students excel in their courses. From interactive video lectures to AI-powered assistance, Gradify transforms the challenging journey of learning into an engaging and rewarding experience.

> "Education is not the learning of facts, but the training of the mind to think." - Albert Einstein

Gradify brings together everything you need to succeed academically - all in one beautifully designed Android application.

---

## 🚀 Features

Gradify comes packed with powerful features designed to enhance your learning experience.

### 📚 Interactive Video Lectures

Access a comprehensive library of video lectures across multiple subjects.

- **Structured Learning Paths**: Organized by Foundation and Diploma levels
- **YouTube Integration**: Seamless video playback with custom controls
- **Offline Support**: Download lectures for offline viewing
- **Progress Tracking**: Keep track of completed lectures and your learning journey
- **Multi-Subject Coverage**: From Python and Java to Statistics and Machine Learning

**Supported Subjects:**

- **Foundation Level**: Computational Thinking, English, Mathematics, Python, Statistics
- **Diploma Level**: Business Analytics, BDM, DBMS, Java, MAD, Machine Learning, PDSA, System Commands

### 🎯 Interactive Quizzes

Test your knowledge with subject-specific quizzes.

- **Comprehensive Question Banks**: Hundreds of questions across all subjects
- **Instant Feedback**: Get immediate results and explanations
- **Progress Analytics**: Track your quiz performance over time
- **Encrypted Content**: Secure quiz data with AES encryption
- **Adaptive Learning**: Focus on areas that need improvement

### 📖 Smart Notes System

Access and organize your study materials efficiently.

- **Cloud-Synced Notes**: Access your notes from anywhere
- **Rich Media Support**: Images, PDFs, and formatted text
- **Quick Search**: Find what you need instantly
- **Categorized Content**: Organized by subject and topic
- **Offline Access**: Study even without internet connection

### 🧮 Formula Sheet

Never forget important formulas again.

- **Comprehensive Formula Database**: Math, Statistics, and Science formulas
- **Quick Reference**: Instant access to commonly used formulas
- **Visual Representations**: Clear, formatted mathematical expressions
- **Searchable**: Find formulas by subject or keyword
- **Favorites**: Bookmark frequently used formulas

### 🤖 AI-Powered Assistant

Get instant help with your studies using Google's Gemini AI.

- **Natural Conversations**: Ask questions in plain language
- **Subject-Specific Help**: Tailored responses for your courses
- **Step-by-Step Explanations**: Understand concepts thoroughly
- **24/7 Availability**: Study help whenever you need it
- **Context-Aware**: Remembers your conversation history

### 🎨 Beautiful UI/UX

A stunning interface that makes learning enjoyable.

- **Material Design 3**: Modern, clean, and intuitive interface
- **Dark Mode Support**: Easy on the eyes during late-night study sessions
- **Smooth Animations**: Polished transitions and interactions
- **Customizable Themes**: Personalize your learning environment
- **Responsive Design**: Optimized for all screen sizes

### 🔐 Secure & Private

Your data is protected with industry-standard security.

- **AES Encryption**: Secure content delivery
- **Firebase Authentication**: Safe and reliable user management
- **Encrypted URLs**: Protected access to premium content
- **Privacy First**: Your study data stays yours

### 📊 Progress Tracking

Monitor your academic journey with detailed analytics.

- **Learning Statistics**: Track time spent on each subject
- **Quiz Performance**: Visualize your improvement over time
- **Completion Rates**: See how much you've accomplished
- **Personalized Insights**: Get recommendations based on your progress

---

## 🛠 Tech Stack

Gradify is built with modern Android development practices, ensuring a smooth, secure, and responsive experience.

**Core Technologies:**

```
• Java (Android)
• Firebase (Authentication, Firestore, Crashlytics)
• Material Design 3
• AndroidX Libraries
```

**Key Dependencies:**

```gradle
// UI & Design
androidx.appcompat:appcompat
com.google.android.material:material
androidx.constraintlayout:constraintlayout
com.github.Dimezis:BlurView

// Networking
com.squareup.retrofit2:retrofit
com.squareup.okhttp3:okhttp
com.android.volley:volley

// Media & Images
com.github.bumptech.glide:glide
com.github.chrisbanes:PhotoView
com.pierfrancescosoffritti.androidyoutubeplayer:core

// Firebase
com.google.firebase:firebase-auth
com.google.firebase:firebase-firestore
com.google.firebase:firebase-crashlytics

// AI & Intelligence
com.google.ai.client.generativeai:generativeai

// Data & Storage
androidx.room:room-runtime
androidx.security:security-crypto

// Utilities
org.mariuszgromada.math:MathParser.org-mXparser
io.noties.markwon:core
com.github.skydoves:colorpickerview

// And more... (see app/build.gradle)
```

---

## 🗺 Roadmap

We are constantly improving Gradify. Here's what's coming next:

- [ ] **Offline Mode**: Full offline support for lectures and quizzes
- [ ] **Study Groups**: Collaborate with classmates in real-time
- [ ] **Live Classes**: Interactive live sessions with instructors
- [ ] **Flashcards**: Spaced repetition learning system
- [ ] **Achievement System**: Earn badges and rewards for learning milestones
- [ ] **Calendar Integration**: Sync with your academic calendar
- [ ] **Voice Notes**: Record and transcribe lecture notes
- [ ] **Web Dashboard**: Access your progress on desktop
- [ ] **Multi-Language Support**: Learn in your preferred language
- [ ] **Advanced Analytics**: Detailed insights into your learning patterns

---

## 📚 Documentation

### Project Structure

```
Gradify/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/gxdevs/gradify/
│   │   │   │   ├── activities/        # All app activities
│   │   │   │   ├── adapters/          # RecyclerView adapters
│   │   │   │   ├── fragments/         # UI fragments
│   │   │   │   ├── models/            # Data models
│   │   │   │   └── Utils/             # Utility classes
│   │   │   ├── res/                   # Resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle
├── data/
│   ├── lectures/                      # Lecture JSON files
│   ├── quizzes/                       # Quiz JSON files
│   ├── formulas.json                  # Formula database
│   └── index.json                     # Content index
├── .gitignore
├── LICENSE
└── README.md
```

### Key Components

- **Utils.java**: Core utility functions including encryption/decryption, URL handling, and data fetching
- **LectureActivity.java**: Video lecture player with YouTube integration
- **NotesActivity.java**: Notes viewer with PDF and image support
- **SubjectsActivity.java**: Subject selection and navigation
- **MainActivity.java**: App entry point and navigation hub

---

## 📥 Installation

### Option 1: Download APK (Recommended)

1. Go to the [Releases](https://github.com/gtxPrime/gradify/releases) section
2. Download the latest APK file
3. Install on your Android device (Enable "Install from Unknown Sources" if needed)

### Option 2: Build from Source

**Prerequisites:**

- Android Studio (latest version)
- JDK 8 or higher
- Android SDK (API 24+)

**Steps:**

1. **Clone the repository**

   ```bash
   git clone https://github.com/gtxprime/gradify.git
   cd gradify
   ```

2. **Set up local.properties**

   Create a `local.properties` file in the root directory:

   ```properties
   sdk.dir=YOUR_ANDROID_SDK_PATH
   SECRET_KEY=your_secret_key_here
   ```

3. **Configure signing (optional)**

   Update `gradle.properties` with your keystore details:

   ```properties
   KEYSTORE_FILE=path/to/your/keystore
   KEYSTORE_PASSWORD=your_password
   KEY_ALIAS=your_alias
   KEY_PASSWORD=your_key_password
   ```

4. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

5. **Sync Gradle**
   - Wait for Gradle to sync all dependencies
   - Resolve any dependency issues if prompted

6. **Run the app**
   - Connect an Android device or start an emulator
   - Click the "Run" button (or press Shift+F10)

**Note:** You'll need to configure Firebase for full functionality. Download `google-services.json` from your Firebase console and place it in the `app/` directory.

---

## 🤝 How to Contribute

We love contributions! Whether it's a bug fix, new feature, or documentation improvement, your help is appreciated.

### Contributing Guidelines

1. **Fork the Project**

   ```bash
   # Click the 'Fork' button on GitHub
   ```

2. **Create your Feature Branch**

   ```bash
   git checkout -b feature/AmazingFeature
   ```

3. **Make your changes**
   - Write clean, documented code
   - Follow existing code style and conventions
   - Test your changes thoroughly

4. **Commit your Changes**

   ```bash
   git commit -m 'Add some AmazingFeature'
   ```

5. **Push to the Branch**

   ```bash
   git push origin feature/AmazingFeature
   ```

6. **Open a Pull Request**
   - Go to the repository on GitHub
   - Click "New Pull Request"
   - Describe your changes in detail

### Areas We Need Help With

- 🎨 **UI/UX Improvements**: Make the app even more beautiful
- 📝 **Content Creation**: Add more quizzes, notes, and study materials
- 🐛 **Bug Fixes**: Help us squash bugs
- 📚 **Documentation**: Improve guides and tutorials
- 🌐 **Translations**: Make Gradify accessible in more languages
- ⚡ **Performance**: Optimize app speed and efficiency

---

## 🐛 Bug Reports & Feature Requests

Found a bug or have a feature idea? We'd love to hear from you!

- **Bug Reports**: [Open an issue](https://github.com/gtxPrime/gradify/issues/new?template=bug_report.md)
- **Feature Requests**: [Open an issue](https://github.com/gtxPrime/gradify/issues/new?template=feature_request.md)

---

## 📄 License

Distributed under a **Modified MIT License** with mandatory credit requirement.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software, provided that visible credit is given to **Gradify** when publicly distributed.

See [LICENSE](LICENSE) for more information.

---

## 👨‍💻 Developer

**Developed by gtxPrime**

- GitHub: [@gtxPrime](https://github.com/gtxprime)
- Project Link: [https://github.com/gtxPrime/gradify](https://github.com/gtxPrime/gradify)

---

## 🙏 Acknowledgments

- Thanks to all the students using Gradify to achieve their academic goals
- Special thanks to the open-source community for amazing libraries
- Inspired by the need for better educational tools

---

## 📞 Support

Need help? Have questions?

- 📧 Email: [Contact via GitHub](https://github.com/gtxprime)
- 🐛 Issues: [GitHub Issues](https://github.com/gtxPrime/gradify/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/gtxPrime/gradify/discussions)

---

<div align="center">

**Made with ❤️ for students, by a student**

⭐ Star this repo if Gradify helps you ace your exams!

[Report Bug](https://github.com/gtxPrime/gradify/issues) · [Request Feature](https://github.com/gtxPrime/gradify/issues) · [Contribute](https://github.com/gtxPrime/gradify/pulls)

</div>

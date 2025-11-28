# MedNot - Medication Notification Reminder App

<div align="center">
  <img src="Mednot Project.png" alt="MedNot App Screenshot" width="600">
  <br><br>
</div>

**MedNot** is a Kotlin-based Android application designed to help users manage their medication schedules, monitor stock levels, and receive timely notifications. This app was developed to ensure medication adherence and simplify health management.

## 🌟 Key Features

Based on the source code, here are the application's key features:

* **Secure User Authentication**: Login and Registration system using **Firebase Authentication**.
* **Medication Schedule Management**:
    * Add new medications with full details (name, dosage, type/form, eating instructions).
    * Flexible scheduling options: By Frequency (times per day) or Interval (every X hours).
* **Smart Reminder System**:
    * Alarm notifications using Android's `AlarmManager` to ensure users don't miss a dose.
    * "Mark as Taken" feature that instantly updates the medication status and deducts stock.
* **Medication Stock Tracking**:
    * **Low Stock Alerts**: The dashboard automatically notifies you if estimated stock is low (calculated based on usage).
    * Inventory Management: View, Edit, and Delete stored medication stock.
* **Medication History Logs**: specific logs tracking when medication was taken, separating status into "Upcoming", "Taken", or "Missed".
* **Informative Dashboard**: The home screen displays a summary of today's schedule and critical alerts.

## 🛠️ Technology Stack

This application is built using the following technologies and libraries:

* **Programming Language**: [Kotlin](https://kotlinlang.org/) (Version 2.0.21)
* **Minimum SDK**: API 26 (Android 8.0 Oreo)
* **Target SDK**: API 36
* **Backend & Database**:
    * [Firebase Authentication](https://firebase.google.com/docs/auth): For user management.
    * [Firebase Firestore](https://firebase.google.com/docs/firestore): Real-time NoSQL database for storing medication and profile data.
* **UI/UX**:
    * XML Layouts.
    * Material Design Components (CardView, BottomNavigationView).
    * RecyclerView for displaying lists.
* **Build Tool**: Gradle (Kotlin DSL).

## 📋 Installation Prerequisites

Before running this project, ensure you have:

1.  **Android Studio** (Latest version recommended).
2.  **JDK 11** (Configured in `build.gradle.kts`).
3.  **Google Firebase Account**.

## 🚀 Installation and Setup

Follow these steps to run the project on your local machine:

1.  **Clone the Repository**
    ```bash
    git clone [https://github.com/your-username/MedNot.git](https://github.com/your-username/MedNot.git)
    cd MedNot
    ```

2.  **Firebase Configuration**
    * Create a new project in the [Firebase Console](https://console.firebase.google.com/).
    * Enable **Authentication** (Email/Password provider).
    * Create a **Cloud Firestore** database (start in *test mode* for development).
    * Download the `google-services.json` file from your Firebase console.
    * Place the `google-services.json` file into the `app/` folder in the project directory:
        `MedNot/app/google-services.json`

3.  **Open in Android Studio**
    * Open Android Studio, select **Open**, and navigate to the `MedNot` project folder.
    * Wait for the *Gradle Sync* process to complete.

4.  **Run the Application**
    * Connect a physical Android device or use an Emulator.
    * Click the **Run** button (Shift+F10).

## 📂 Project Structure

Main package structure in `app/src/main/java/com/example/mednot/`:

```text
com.example.mednot
├── Auth/                   # Authentication Features
│   ├── Auth_Login.kt       # Login Page
│   └── Auth_Register.kt    # User Registration Page
├── User/                   # Main User Features
│   ├── Home.kt             # Main Activity (Fragment Container)
│   ├── Home_Fragment.kt    # Dashboard (Schedule & Alerts)
│   ├── Add_Med_Fragment.kt # Add Medication Form
│   ├── Check_Med_Stock.kt  # Medication Stock List
│   ├── Edit_Med_Stock.kt   # Edit Medication Details
│   ├── Profile_Fragment.kt # User Profile & Logout
│   ├── ReminderAdapter.kt  # RecyclerView Logic & Med Status
│   ├── AlarmReceiver.kt    # Handles Notification Triggers
│   └── AlarmScheduler.kt   # Manages System Alarm Scheduling
└── MainActivity.kt         # Entry point (if applicable)

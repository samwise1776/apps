# Desktopcraft Swing edition

The downloadable desktop tutor is a Java Swing application with the website's complete learning feature set. It includes five courses and 2,500 lessons, editable code, a simulated desktop preview, automatically checked challenges, native 20-question quizzes, the five-language tutorial hub, App Maker, lesson creation, Helper AI's built-in course guide, a real-account forum with posts/replies/likes, customization, persistent progress, XP, and the leaderboard.

The desktop navigation also includes a native project workspace with multi-file editing and restorable versions, the shared community app/package library, publishing access, feedback submission, and About. Community apps, publishing, and feedback connect to the same database-backed site server configured in the Community Apps window (by default `http://localhost:8000`). Local lessons, projects, accounts, preferences, and progress continue working when that server is offline.

Account creation and sign-in use a Desktopcraft username and a separate Desktopcraft-only password—never an email password. Accounts stay registered through the operating system's Java Preferences storage, sessions reopen automatically, and passwords use salted PBKDF2-SHA256 verifiers. The app does not create account files or store readable passwords.

The lesson studio lets the learner provide a lesson name, starter code, answer, and expected output. Custom lessons persist locally. Its profanity policy is shown before submission: a violation erases the draft and all custom lessons, then permanently disables creation for the local app profile.

App Maker provides a live blueprint, preview, generated source, clipboard copy, and source-file save flow for Java Swing, Python Tkinter, C# WinForms, C++ Qt Widgets, and JavaScript Electron.

The builder forum and leaderboard contain only actual Desktopcraft accounts stored by the desktop app. Every completed lesson adds 100 XP to that account's saved total; sample names and fabricated scores are not included.

Customization changes the color and brightness of all rebuilt app screens and switches lesson/tutorial/helper explanations between concise, balanced, and detailed modes.

## Install on the Desktop

Download and unzip `Desktopcraft-desktop.zip`, then run the installer for your operating system. It copies `Desktopcraft.jar` and a launcher to the Desktop. A ZIP file cannot choose its extraction location by itself, so the one-click installer performs that step immediately after extraction.

## Run the packaged app

Java 17 or newer is required.

```bash
java -jar Desktopcraft.jar
```

## Build from source

From the repository root:

```bash
mkdir -p desktop-app/build downloads
javac --release 17 -d desktop-app/build desktop-app/src/DesktopcraftApp.java
jar --create --file downloads/Desktopcraft.jar --main-class DesktopcraftApp -C desktop-app/build . lessons-extra.js desktop-courses.js
```

Or build the JAR, installer ZIP, and source ZIP together:

```bash
npm run desktop:build
```

The two JavaScript curriculum files are packaged as read-only JAR resources. The Swing application parses their lesson metadata at startup, generates language-appropriate examples, and stores completion locally through Java Preferences.

# Grub Helper - Developer Documentation (`READ-DEV.md`)

This document provides developer guidelines, technical architecture details, code organization, and update system workflows for **Grub Helper**.

---

## 1. Application Architecture

Grub Helper is built in Java 21 using Swing for the graphical interface and standard Linux utilities for root privilege escalation and package management.

### Component Structure

```
src/main/java/com/grubhelper/
├── gui/
│   └── MainFrame.java        # Main Java Swing GUI (Easy Mode, Advanced Mode, Theme Manager)
├── model/
│   ├── GrubConfigParser.java # /etc/default/grub parser, serializer, and comment preserver
│   ├── GrubEnvironment.java  # System GRUB boot dir, theme path, and update-grub command detector
│   ├── GrubTheme.java        # Data model representing an installed GRUB theme
│   └── ThemeManager.java     # Theme loader and archive extractor (.zip, .tar.gz, .tar.xz)
└── util/
    ├── RootExecutor.java     # Executes privileged shell commands via pkexec or sudo
    ├── UpdateChecker.java    # Asynchronous update checker and self-installer execution
    └── Version.java          # Version constants and repository URLs
```

---

## 2. Semantic Versioning Structure (`vX.Y.Z`)

Grub Helper follows strict semantic versioning (`vX.Y.Z`):

- **Major Version (`X`)**: Incremented for full UI redesigns, major structural overhauls, or breaking platform changes (e.g., `1.0.0` → `2.0.0`).
- **Minor Version (`Y`)**: Incremented when introducing new end-user features or new core capabilities (e.g., `1.0.0` → `1.1.0`).
- **Patch Version (`Z`)**: Incremented for bug fixes, small tweaks, performance enhancements, or code cleanup (e.g., `1.0.0` → `1.0.1`).

---

## 3. How the Update System Works

### Workflow Overview

1. **Local Developer Verification**:
   - The developer builds and verifies code changes locally using `./install.sh` and the unit test suite (`CoreTestRunner.java`).
   - If introducing a fix or feature, the developer increments the version in `Version.java` and `version.json`.
   - The developer commits and pushes changes to `https://github.com/ZimnyySTD/Grub-Helper/`.

2. **Client Update Detection**:
   - On application startup, `MainFrame` launches a background worker thread (`SwingWorker`) executing `UpdateChecker.checkForUpdates()`.
   - `UpdateChecker` fetches `https://raw.githubusercontent.com/ZimnyySTD/Grub-Helper/main/version.json`.
   - `UpdateChecker.isNewerVersion()` parses version strings into integer arrays and compares numeric segments (`Major.Minor.Patch`).

3. **Client Update Execution**:
   - If a higher version is available, an **"Update Available: vX.Y.Z"** button appears in the application header.
   - Clicking the button prompts the user for confirmation.
   - Upon confirmation, `UpdateChecker.performAutoUpdate()` runs a root command executing `git pull` followed by `./install.sh`.
   - The installer updates the source code, rebuilds the application JAR (`/usr/share/java/grub-helper/grub-helper.jar`), and refreshes system launcher files.

---

## 4. Running Unit Tests

To compile and run backend unit tests without running the full GUI:

```bash
mkdir -p bin
javac -d bin src/main/java/com/grubhelper/model/*.java src/main/java/com/grubhelper/util/*.java src/main/java/com/grubhelper/gui/*.java src/test/java/com/grubhelper/test/*.java
java -cp bin com.grubhelper.test.CoreTestRunner
```

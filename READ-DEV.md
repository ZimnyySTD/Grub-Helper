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
    ├── UpdateChecker.java    # Asynchronous GitHub Releases API update checker and self-installer
    └── Version.java          # Version constants and repository URLs
```

---

## 2. Semantic Versioning Structure (`vX.Y.Z`)

Grub Helper follows strict semantic versioning (`vX.Y.Z`):

- **Major Version (`X`)**: Incremented for full UI redesigns, major structural overhauls, or breaking platform changes (e.g., `1.0.0` → `2.0.0`).
- **Minor Version (`Y`)**: Incremented when introducing new end-user features or new core capabilities (e.g., `1.0.0` → `1.1.0`).
- **Patch Version (`Z`)**: Incremented for bug fixes, small tweaks, performance enhancements, or code cleanup (e.g., `1.0.0` → `1.0.1`).

---

## 3. How to Release & Test Updates

For detailed step-by-step instructions on local testing, version bumping, and publishing GitHub Releases, see **[UPDATE.md](UPDATE.md)**.

---

## 4. Running Unit Tests

To compile and run backend unit tests without running the full GUI:

```bash
mkdir -p bin
javac -d bin src/main/java/com/grubhelper/model/*.java src/main/java/com/grubhelper/util/*.java src/main/java/com/grubhelper/gui/*.java src/test/java/com/grubhelper/test/*.java
java -cp bin com.grubhelper.test.CoreTestRunner
```

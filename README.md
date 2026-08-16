# Grub Helper

**Grub Helper** is a modern graphical Linux application written in Java Swing designed to easily manage your GRUB bootloader configuration settings and themes across any Linux distribution (Ubuntu, Debian, Arch Linux, Fedora, openSUSE, etc.) without needing to edit complex configuration files manually.

---

## How the Application Works

### Architectural Overview

1. **Environment Detection (`GrubEnvironment.java`)**:
   - Automatically detects system GRUB boot directories (`/boot/grub` vs `/boot/grub2`).
   - Identifies distro-specific GRUB update commands (`update-grub`, `grub-mkconfig -o /boot/grub/grub.cfg`, or `grub2-mkconfig -o /boot/grub2/grub.cfg`).
   - Locates system theme directories (`/boot/grub/themes` or `/boot/grub2/themes`).

2. **Configuration Parser & Serializer (`GrubConfigParser.java`)**:
   - Parses `/etc/default/grub` into key-value pairs for display and editing in Easy Mode.
   - Retains comments, empty lines, and file structure so non-modified settings remain preserved.
   - Generates formatted GRUB configuration text for Advanced Mode editing.

3. **Theme Management (`ThemeManager.java`)**:
   - Scans system GRUB theme folders.
   - Extracts theme preview screenshots (`preview.png`, `screenshot.jpg`, `background.png`).
   - Supports single-click theme installation from `.zip`, `.tar.gz`, `.tar.xz`, and `.tar` archives with path traversal (Zip-Slip) protection.

4. **Privilege Elevation (`RootExecutor.java`)**:
   - Executes system updates and theme directory writes securely using standard Linux privilege escalation tools (`pkexec` PolicyKit GUI prompt or `sudo`).

5. **User Interface (`MainFrame.java`)**:
   - **Dashboard (Easy Mode)**: Interactive table editor for setting key-value parameters.
   - **Advanced Config Editor**: Raw text editor with real-time bi-directional synchronization between Easy Mode and Advanced Mode upon tab switching.
   - **Theme Manager**: Visual theme browser, live screenshot previewer, and theme archive installer.

---

## How the Update System Works

Grub Helper features a clean, safe, and automated update workflow designed for both end-users and developers testing releases before pushing updates:

### 1. Developer Testing & Update Flow
- Before pushing an update, developers test changes locally using `./install.sh` and the built-in test suite (`CoreTestRunner.java`).
- The developer bumps the version number in `src/main/java/com/grubhelper/util/Version.java` and updates `version.json` with release details and changelog.
- Once verified, the commit/tag is pushed to the main repository branch.

### 2. Client Auto-Update Check Process
- When launched, Grub Helper spawns an asynchronous background thread (`SwingWorker`) using `UpdateChecker.java`.
- The background worker fetches the remote `version.json` manifest from the official repository without blocking the GUI.
- The `UpdateChecker.isNewerVersion()` method compares semantic version strings (e.g., `1.0.0` vs `1.0.1`).
- If a newer version is detected:
  - A green **"Update Available: vX.Y.Z"** button appears dynamically in the header panel.
  - Clicking **"Update Available"** displays a dialog prompting the user to install the update.
- Upon user confirmation, `UpdateChecker.performAutoUpdate()` runs a root-elevated update script (`git pull` followed by `./install.sh`), pulling the tested update and automatically reinstalling the app binary and desktop launchers.

---

## Installation & Setup

Clone the repository and run `install.sh`:

```bash
git clone https://github.com/grub-helper/grub-helper.git
cd grub-helper
chmod +x install.sh
./install.sh
```

The installer script automatically:
1. Detects your distribution package manager (`apt`, `pacman`, `dnf`, `zypper`).
2. Installs required dependencies (Java JDK/JRE 21+, `tar`, `unzip`, `polkit`).
3. Compiles the Java sources and packages the executable JAR file (`/usr/share/java/grub-helper/grub-helper.jar`).
4. Installs the system command shortcut `/usr/local/bin/grub-helper`, desktop application menu entry, and SVG launcher icon.

---

## Running

Launch Grub Helper from your desktop application menu or run from terminal:

```bash
grub-helper
```

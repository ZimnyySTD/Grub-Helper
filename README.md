### Application Preview

| Main Interface | Theme Selector |
| :-: | :-: |
| ![Main Screen](1.png) | ![Themes](2.png) |

| Config Editor | Settings |
| :-: | :-: |
| ![Config](3.png) | ![Settings](4.png) |


# Grub Helper

**Grub Helper** is a simple GUI application that makes configuring Grub Bootloader easier
---

## Features

- **Dashboard (Easy Mode)**: Edit GRUB boot settings in an intuitive table format.
- **Advanced Config Editor**: Direct raw editing of `/etc/default/grub` with automatic synchronization.
- **Theme Manager**:
  - Live screenshot previews for installed GRUB themes.
  - One-click active theme selection.
  - Built-in theme archive installer (`.zip`, `.tar.gz`, `.tar.xz`, `.tar`).
- **Auto-Update System**: Checks GitHub Releases on startup, notifies you when new releases are available, and installs updates with one click.
- **Cross-Distro Compatibility**: Auto-detects GRUB locations, theme directories, and update tools across distros.

---

## Installation

Run `install.sh` from the cloned repository:

```bash
git clone https://github.com/ZimnyySTD/Grub-Helper.git
cd Grub-Helper
chmod +x install.sh
./install.sh
```

The installer detects your Linux distribution package manager (`apt`, `pacman`, `dnf`, `zypper`), installs required dependencies, builds the application, and sets up a launcher shortcut and application menu entry.

---

## Running the App

Launch **Grub Helper** from your application menu or run from terminal:

```bash
grub-helper
```

For developer documentation and internal technical architecture, see [READ-DEV.md](READ-DEV.md). For publishing releases, see [UPDATE.md](UPDATE.md).

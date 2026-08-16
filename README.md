# Grub Helper

**Grub Helper** is a graphical Linux application written in Java Swing designed to easily manage your GRUB bootloader settings and themes across any Linux distribution (Ubuntu, Debian, Arch, Fedora, openSUSE, etc.) without manually editing complex configuration files.

## Features

- **Dashboard / Easy Mode**: Visually view and edit all GRUB configuration key-value settings (`/etc/default/grub`) in an intuitive table format. Add or remove custom parameters effortlessly.
- **Advanced Config Editor**: Raw text editor to directly inspect or edit `/etc/default/grub` with automated syntax synchronization between Easy and Advanced modes.
- **Theme Manager**:
  - Automatically scans `/boot/grub/themes` (or `/boot/grub2/themes`).
  - Displays live image previews for installed themes.
  - Easily switch active themes with one click.
  - Built-in theme installer supporting `.zip`, `.tar.gz`, `.tar.xz`, and `.tar` theme archives.
- **Cross-Distro Detection**: Automatically detects GRUB paths, themes directories, and update commands (`update-grub`, `grub-mkconfig`, `grub2-mkconfig`).
- **Root Elevation**: Securely prompts for elevated permissions using `pkexec` or `sudo` when writing configurations or applying themes.

## Installation

Clone the repository and run `install.sh`:

```bash
git clone https://github.com/your-username/grub-helper.git
cd grub-helper
chmod +x install.sh
./install.sh
```

The installer automatically detects your Linux distribution package manager (`apt`, `pacman`, `dnf`, `zypper`), installs all dependencies (Java JDK/JRE, `tar`, `unzip`, `polkit`), builds the executable JAR, and installs `grub-helper` into `/usr/local/bin` along with desktop entry and application menu icons.

## Running

Launch Grub Helper from your desktop application menu or run from terminal:

```bash
grub-helper
```

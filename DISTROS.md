# Supported Linux Distributions (`DISTROS.md`)

**Grub Helper** is designed to be cross-distro compatible and can be installed and run on virtually **any Linux distribution** running GRUB/GRUB2 bootloader.

The included installer script (`install.sh`) automatically detects your distribution's native package manager to install required dependencies (Java JDK/JRE 21+, `tar`, `unzip`, and `polkit`).

---

## Officially Supported Distribution Families & Package Managers

### 1. Debian & Ubuntu Family (`apt`)
- **Ubuntu** (20.04 LTS, 22.04 LTS, 24.04 LTS, and newer)
- **Debian** (Debian 11 Bullseye, Debian 12 Bookworm, Debian Testing/Unstable)
- **Linux Mint** (20.x, 21.x, 22.x, and Cinnamon/MATE/XFCE editions)
- **Pop!_OS**
- **Elementary OS**
- **Zorin OS**
- **Kubuntu, Xubuntu, Lubuntu, Ubuntu Budgie, Ubuntu Unity**
- **Kali Linux**
- **Parrot OS**
- **MX Linux**
- **Devuan Linux**

### 2. Arch Linux Family (`pacman`)
- **Arch Linux**
- **Manjaro Linux**
- **EndeavourOS**
- **Garuda Linux**
- **ArcoLinux**
- **Artix Linux**
- **RebornOS**

### 3. Red Hat & Fedora Family (`dnf` / `yum`)
- **Fedora Workstation / Silverblue** (Fedora 36, 37, 38, 39, 40, and newer)
- **Red Hat Enterprise Linux (RHEL)** (RHEL 8, RHEL 9+)
- **Rocky Linux**
- **AlmaLinux**
- **CentOS Stream**
- **Nobara Linux**
- **Ultramarine Linux**

### 4. openSUSE Family (`zypper`)
- **openSUSE Leap** (15.4, 15.5, 15.6)
- **openSUSE Tumbleweed** (Rolling release)
- **GeckoLinux**

### 5. Other Linux Distributions (Generic / Manual Setup)
Any distribution where Java (JRE/JDK 21 or default-jre), `tar`, `unzip`, and `polkit` / `pkexec` or `sudo` are installed:
- **Alpine Linux** (`apk add openjdk21-jre tar unzip polkit`)
- **Void Linux** (`xbps-install -S openjdk21-jre tar unzip polkit`)
- **Gentoo Linux** (`emerge --ask dev-java/openjdk tar unzip sys-auth/polkit`)
- **Solus**
- **NixOS**
- **Slackware**

---

## Bootloader & Tools Supported
Grub Helper automatically detects and supports systems using:
- **`update-grub`** (Debian / Ubuntu / Mint)
- **`grub-mkconfig`** (Arch / Manjaro / Gentoo)
- **`grub2-mkconfig`** (Fedora / RHEL / openSUSE)
- Both standard BIOS and UEFI boot paths (`/boot/grub`, `/boot/grub2`, and `/boot/efi/EFI/...`).

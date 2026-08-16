#!/usr/bin/env bash

set -e

echo "=========================================="
echo "    Installing Grub Helper for Linux     "
echo "=========================================="

# Check root/sudo for installation phase
if [ "$EUID" -ne 0 ]; then
    echo "Notice: Installation requires elevated privileges for copying binaries and desktop files."
    echo "Prompting for sudo..."
    SUDO="sudo"
else
    SUDO=""
fi

# Distro Detection & Dependency Installation
detect_and_install_deps() {
    echo "[1/4] Detecting Linux Distribution & Installing Dependencies..."
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        DISTRO_ID=$ID
        DISTRO_LIKE=${ID_LIKE:-""}
    else
        DISTRO_ID="unknown"
    fi

    echo "Detected Distribution: $NAME ($DISTRO_ID)"

    if command -v apt-get &> /dev/null; then
        echo "Using apt package manager..."
        $SUDO apt-get update -qq
        $SUDO apt-get install -y openjdk-21-jdk openjdk-21-jre tar unzip policykit-1 || \
        $SUDO apt-get install -y default-jdk default-jre tar unzip policykit-1
    elif command -v pacman &> /dev/null; then
        echo "Using pacman package manager..."
        $SUDO pacman -Sy --needed --noconfirm jdk-openjdk tar unzip polkit
    elif command -v dnf &> /dev/null; then
        echo "Using dnf package manager..."
        $SUDO dnf install -y java-21-openjdk java-21-openjdk-devel tar unzip polkit
    elif command -v zypper &> /dev/null; then
        echo "Using zypper package manager..."
        $SUDO zypper install -y java-21-openjdk tar unzip polkit
    else
        echo "Warning: Unrecognized package manager. Please ensure Java JRE/JDK (21+), tar, unzip, and polkit/pkexec are installed."
    fi
}

# Compile and Package JAR
build_app() {
    echo "[2/4] Compiling Java source code..."
    rm -rf bin build/jar
    mkdir -p bin build/jar

    find src/main/java -name "*.java" > sources.txt
    javac -d bin @sources.txt
    rm sources.txt

    echo "Creating Manifest and JAR file..."
    cat << 'EOF' > Manifest.txt
Manifest-Version: 1.0
Main-Class: com.grubhelper.gui.MainFrame
EOF

    jar cfm build/jar/grub-helper.jar Manifest.txt -C bin .
    rm Manifest.txt
    echo "Build successful: build/jar/grub-helper.jar"
}

# Install System Files
install_system() {
    echo "[3/4] Installing system files..."

    $SUDO mkdir -p /usr/share/java/grub-helper
    $SUDO cp build/jar/grub-helper.jar /usr/share/java/grub-helper/grub-helper.jar

    # Create launcher script
    cat << 'EOF' | $SUDO tee /usr/local/bin/grub-helper > /dev/null
#!/usr/bin/env bash
java -jar /usr/share/java/grub-helper/grub-helper.jar "$@"
EOF

    $SUDO chmod +x /usr/local/bin/grub-helper

    # Install desktop entry
    if [ -f grub-helper.desktop ]; then
        $SUDO mkdir -p /usr/share/applications
        $SUDO cp grub-helper.desktop /usr/share/applications/
    fi

    # Install icon
    if [ -f grub-helper.svg ]; then
        $SUDO mkdir -p /usr/share/icons/hicolor/scalable/apps
        $SUDO cp grub-helper.svg /usr/share/icons/hicolor/scalable/apps/grub-helper.svg
    fi

    echo "[4/4] Installation Complete!"
    echo "Grub Helper is now installed. You can launch it by typing 'grub-helper' or searching for 'Grub Helper' in your application launcher."
}

detect_and_install_deps
build_app
install_system

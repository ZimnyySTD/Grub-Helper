#!/usr/bin/env bash

# Enable immediate exit if any command fails
set -e

# Print header title banner
echo "=========================================="
echo "    Installing Grub Helper for Linux     "
echo "=========================================="

# Check if script is executed as root user (EUID 0)
if [ "$EUID" -ne 0 ]; then
    # Print notice indicating elevated privileges are required
    echo "Notice: Installation requires elevated privileges for copying binaries and desktop files."
    # Inform user that sudo will be used
    echo "Prompting for sudo..."
    # Assign SUDO variable to sudo
    SUDO="sudo"
else
    # SUDO variable is empty if already running as root
    SUDO=""
fi

# Function to detect Linux distro and install required package dependencies
detect_and_install_deps() {
    # Print step progress message
    echo "[1/4] Detecting Linux Distribution & Installing Dependencies..."
    # Check if /etc/os-release file exists
    if [ -f /etc/os-release ]; then
        # Source /etc/os-release to import distribution identification variables
        . /etc/os-release
        # Assign ID variable to DISTRO_ID
        DISTRO_ID=$ID
        # Assign ID_LIKE variable to DISTRO_LIKE
        DISTRO_LIKE=${ID_LIKE:-""}
    else
        # Fallback DISTRO_ID if /etc/os-release does not exist
        DISTRO_ID="unknown"
    fi

    # Display detected distribution name
    echo "Detected Distribution: $NAME ($DISTRO_ID)"

    # Check if apt-get package manager exists (Debian/Ubuntu/Mint)
    if command -v apt-get &> /dev/null; then
        echo "Using apt package manager..."
        # Update package list quietly
        $SUDO apt-get update -qq
        # Install OpenJDK 21, tar, unzip, and PolicyKit
        $SUDO apt-get install -y openjdk-21-jdk openjdk-21-jre tar unzip policykit-1 || \
        $SUDO apt-get install -y default-jdk default-jre tar unzip policykit-1
    # Check if pacman package manager exists (Arch Linux/Manjaro)
    elif command -v pacman &> /dev/null; then
        echo "Using pacman package manager..."
        # Install JDK, tar, unzip, and Polkit using pacman
        $SUDO pacman -Sy --needed --noconfirm jdk-openjdk tar unzip polkit
    # Check if dnf package manager exists (Fedora/RHEL/CentOS)
    elif command -v dnf &> /dev/null; then
        echo "Using dnf package manager..."
        # Install Java 21 OpenJDK, tar, unzip, and Polkit using dnf
        $SUDO dnf install -y java-21-openjdk java-21-openjdk-devel tar unzip polkit
    # Check if zypper package manager exists (openSUSE)
    elif command -v zypper &> /dev/null; then
        echo "Using zypper package manager..."
        # Install Java 21 OpenJDK, tar, unzip, and Polkit using zypper
        $SUDO zypper install -y java-21-openjdk tar unzip polkit
    else
        # Warning if package manager is not recognized
        echo "Warning: Unrecognized package manager. Please ensure Java JRE/JDK (21+), tar, unzip, and polkit/pkexec are installed."
    fi
}

# Function to compile Java source files and create executable JAR package
build_app() {
    # Print step progress message
    echo "[2/4] Compiling Java source code..."
    # Remove existing build/bin directories to ensure clean build
    rm -rf bin build/jar
    # Create bin and build/jar output directories
    mkdir -p bin build/jar

    # Find all Java source files and list them in sources.txt
    find src/main/java -name "*.java" > sources.txt
    # Compile Java source files into bin directory
    javac -d bin @sources.txt
    # Remove sources.txt temporary file
    rm sources.txt

    # Print manifest creation message
    echo "Creating Manifest and JAR file..."
    # Create Manifest file specifying Main-Class
    cat << 'EOF' > Manifest.txt
Manifest-Version: 1.0
Main-Class: com.grubhelper.gui.MainFrame
EOF

    # Package compiled classes into executable JAR file
    jar cfm build/jar/grub-helper.jar Manifest.txt -C bin .
    # Remove temporary Manifest.txt file
    rm Manifest.txt
    # Print build success status
    echo "Build successful: build/jar/grub-helper.jar"
}

# Function to install application binaries, desktop launcher, and icon into system directories
install_system() {
    # Print step progress message
    echo "[3/4] Installing system files..."

    # Create system directory for java package
    $SUDO mkdir -p /usr/share/java/grub-helper
    # Copy compiled grub-helper.jar into system java folder
    $SUDO cp build/jar/grub-helper.jar /usr/share/java/grub-helper/grub-helper.jar

    # Create launcher script /usr/local/bin/grub-helper
    cat << 'EOF' | $SUDO tee /usr/local/bin/grub-helper > /dev/null
#!/usr/bin/env bash
java -jar /usr/share/java/grub-helper/grub-helper.jar "$@"
EOF

    # Make launcher script executable
    $SUDO chmod +x /usr/local/bin/grub-helper

    # Install desktop entry file if present
    if [ -f grub-helper.desktop ]; then
        $SUDO mkdir -p /usr/share/applications
        $SUDO cp grub-helper.desktop /usr/share/applications/
    fi

    # Install SVG icon file if present
    if [ -f grub-helper.svg ]; then
        $SUDO mkdir -p /usr/share/icons/hicolor/scalable/apps
        $SUDO cp grub-helper.svg /usr/share/icons/hicolor/scalable/apps/grub-helper.svg
    fi

    # Print completion banner
    echo "[4/4] Installation Complete!"
    echo "Grub Helper is now installed. You can launch it by typing 'grub-helper' or searching for 'Grub Helper' in your application launcher."
}

# Execute dependency detection and installation function
detect_and_install_deps
# Execute build function
build_app
# Execute system installation function
install_system

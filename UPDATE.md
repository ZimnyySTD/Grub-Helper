# How to Release an Update for Grub Helper (`UPDATE.md`)

This guide explains the developer release workflow for **Grub Helper** using GitHub Releases and tags.

---

## Developer Release & Testing Guide

To test code changes locally first, tag a release on GitHub, and verify that the application detects and installs the update automatically, follow these steps:

### Step 1: Local Development & Manual Testing
1. Make code edits in your local repository workspace.
2. Build and run unit tests locally:
   ```bash
   mkdir -p bin
   javac -d bin src/main/java/com/grubhelper/model/*.java src/main/java/com/grubhelper/util/*.java src/main/java/com/grubhelper/gui/*.java src/test/java/com/grubhelper/test/*.java
   java -cp bin com.grubhelper.test.CoreTestRunner
   ```
3. Test local installation using `./install.sh`:
   ```bash
   ./install.sh
   grub-helper --version
   ```

---

### Step 2: Bump the Version Number in Source Code
When you are ready to publish a new update:
1. Update `Version.java`:
   Edit `src/main/java/com/grubhelper/util/Version.java`:
   ```java
   public static final String CURRENT_VERSION = "1.0.1"; // Update to new version e.g. 1.0.1
   ```
2. Update `version.json`:
   Edit `version.json`:
   ```json
   {
     "version": "1.0.1",
     "changelog": "Added feature X and fixed bug Y.",
     "downloadUrl": "https://github.com/ZimnyySTD/Grub-Helper"
   }
   ```

---

### Step 3: Push Changes & Create a GitHub Release
1. Commit and push your code changes to GitHub:
   ```bash
   git add .
   git commit -m "Release v1.0.1"
   git push origin master
   ```
2. Create a new GitHub Release:
   - Go to your repository on GitHub: `https://github.com/ZimnyySTD/Grub-Helper/releases`
   - Click **"Draft a new release"** (or **"Create a new release"**).
   - In **Choose a tag**, type the new version tag name (e.g. `v1.0.1` or `1.0.1`) and click **Create new tag**.
   - Set the **Release title** (e.g. `Grub Helper v1.0.1`).
   - Add description notes/changelog in the text box.
   - Click **Publish release**.

---

### Step 4: How the App Auto-Update Works on Startup
1. When any installed instance of **Grub Helper** launches, `UpdateChecker.java` queries the official GitHub Releases API:
   `https://api.github.com/repos/ZimnyySTD/Grub-Helper/releases/latest`
2. `UpdateChecker` extracts the `tag_name` from the GitHub release payload (e.g., `v1.0.1` -> `1.0.1`).
3. `UpdateChecker.isNewerVersion()` compares the current running version (`1.0.0`) against the remote release tag (`1.0.1`).
4. If a newer release tag is found:
   - A green button **"Update Available: v1.0.1"** appears in the top header bar of the app interface.
   - Clicking **"Update Available"** prompts the user for confirmation.
   - Upon user approval, `UpdateChecker.performAutoUpdate()` runs a root-elevated command executing:
     `git fetch --all && git reset --hard origin/master && ./install.sh`
   - The app automatically pulls the release commit, rebuilds `/usr/share/java/grub-helper/grub-helper.jar`, and cleans up temporary update files.

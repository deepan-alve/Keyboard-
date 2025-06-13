# Build the AzhagiKeys project after rebranding
# This script will attempt to build the project to verify that the rebranding changes don't break the build

# Change to the project directory
Set-Location "c:\Users\deepa\Downloads\KEYBO\Keyboard-"

# Clean the project to ensure a fresh build
Write-Host "Cleaning the project..."
.\gradlew clean

# Build the project
Write-Host "Building the project..."
.\gradlew build --info

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build completed successfully!"
} else {
    Write-Host "Build failed with exit code $LASTEXITCODE. Check the logs for details."
}

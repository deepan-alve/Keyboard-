# Clean up old package directories after rebranding
# This script will remove the old package directories that are no longer needed

# Define paths
$oldMainPackage = "c:\Users\deepa\Downloads\KEYBO\Keyboard-\app\src\main\kotlin\dev"
$oldTestPackage = "c:\Users\deepa\Downloads\KEYBO\Keyboard-\app\src\test\kotlin\dev"
$oldAndroidTestPackage = "c:\Users\deepa\Downloads\KEYBO\Keyboard-\app\src\androidTest\kotlin\dev"

# Check if the directories exist and remove them
if (Test-Path -Path $oldMainPackage) {
    Write-Host "Removing old main package directory..."
    Remove-Item -Path $oldMainPackage -Recurse -Force
    Write-Host "Old main package directory removed."
} else {
    Write-Host "Old main package directory not found."
}

if (Test-Path -Path $oldTestPackage) {
    Write-Host "Removing old test package directory..."
    Remove-Item -Path $oldTestPackage -Recurse -Force
    Write-Host "Old test package directory removed."
} else {
    Write-Host "Old test package directory not found."
}

if (Test-Path -Path $oldAndroidTestPackage) {
    Write-Host "Removing old androidTest package directory..."
    Remove-Item -Path $oldAndroidTestPackage -Recurse -Force
    Write-Host "Old androidTest package directory removed."
} else {
    Write-Host "Old androidTest package directory not found."
}

Write-Host "Clean up completed successfully."

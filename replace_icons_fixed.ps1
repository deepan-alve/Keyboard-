# Simple PowerShell script to replace all app icons with AzhagiKeys icons
Write-Host "Starting AzhagiKeys icon replacement..." -ForegroundColor Green

$projectRoot = "c:\Users\deepa\Downloads\KEYBO\Keyboard-"
$sourceIconsPath = "$projectRoot\app\appicons\android"
$targetResPath = "$projectRoot\app\src\main\res"

# Check if source icons exist
if (!(Test-Path $sourceIconsPath)) {
    Write-Host "ERROR: Source icons not found at $sourceIconsPath" -ForegroundColor Red
    exit 1
}

Write-Host "Source icons found at: $sourceIconsPath" -ForegroundColor Yellow

# Define all density folders and icon variants
$densityFolders = @("mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi")
$iconVariants = @("ic_app_icon_debug", "ic_app_icon_debug_round", "ic_app_icon_beta", "ic_app_icon_beta_round", "ic_app_icon_stable", "ic_app_icon_stable_round")

Write-Host "`nCopying icons for all build variants..." -ForegroundColor Cyan

foreach ($density in $densityFolders) {
    $sourcePath = "$sourceIconsPath\$density\AzhagiKeys.png"
    
    if (Test-Path $sourcePath) {
        Write-Host "Processing $density..." -ForegroundColor Yellow
        
        foreach ($iconName in $iconVariants) {
            $targetPath = "$targetResPath\$density\$iconName.png"
            
            try {
                Copy-Item $sourcePath $targetPath -Force
                Write-Host "  ✓ Copied $iconName.png" -ForegroundColor Green
            }
            catch {
                Write-Host "  ✗ Failed to copy $iconName.png" -ForegroundColor Red
            }
        }
    }
    else {
        Write-Host "⚠ Source not found: $sourcePath" -ForegroundColor Yellow
    }
}

Write-Host "`n🎉 Icon replacement completed!" -ForegroundColor Green
Write-Host "All icons have been replaced with your AzhagiKeys design." -ForegroundColor Cyan

# PowerShell script to replace all app icons with new AzhagiKeys icons
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

# Define all density folders
$densityFolders = @("mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi")

# Function to copy and rename icon
function Copy-IconVariant {
    param($density, $targetIconName)
    
    $sourcePath = "$sourceIconsPath\$density\AzhagiKeys.png"
    $targetPath = "$targetResPath\$density\$targetIconName.png"
    
    if (Test-Path $sourcePath) {
        try {
            Copy-Item $sourcePath $targetPath -Force
            Write-Host "✓ Copied $density/$targetIconName.png" -ForegroundColor Green
        }
        catch {
            Write-Host "✗ Failed to copy $density/$targetIconName.png - $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    else {
        Write-Host "✗ Source not found: $sourcePath" -ForegroundColor Red
    }
}

# Copy icons for all build variants and densities
Write-Host "`nCopying icons for all build variants..." -ForegroundColor Cyan

foreach ($density in $densityFolders) {
    Write-Host "`nProcessing $density..." -ForegroundColor Yellow
    
    # Debug variant
    Copy-IconVariant $density "ic_app_icon_debug"
    Copy-IconVariant $density "ic_app_icon_debug_round"
    
    # Beta variant  
    Copy-IconVariant $density "ic_app_icon_beta"
    Copy-IconVariant $density "ic_app_icon_beta_round"
    
    # Stable variant
    Copy-IconVariant $density "ic_app_icon_stable"
    Copy-IconVariant $density "ic_app_icon_stable_round"
}

# Copy Play Store icons
Write-Host "`nCopying Play Store icons..." -ForegroundColor Cyan

$playStoreSource = "$projectRoot\app\appicons\playstore.png"
if (Test-Path $playStoreSource) {
    # Copy to all Play Store icon locations
    $playStoreTargets = @(
        "$projectRoot\app\src\main\ic_app_icon_debug-playstore.png",
        "$projectRoot\app\src\main\ic_app_icon_preview-playstore.png", 
        "$projectRoot\app\src\main\ic_app_icon_stable-playstore.png"
    )
    
    foreach ($target in $playStoreTargets) {
        try {
            Copy-Item $playStoreSource $target -Force
            $fileName = Split-Path $target -Leaf
            Write-Host "✓ Copied $fileName" -ForegroundColor Green
        }
        catch {
            Write-Host "✗ Failed to copy Play Store icon - $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}
else {
    Write-Host "⚠ Play Store icon not found at $playStoreSource" -ForegroundColor Yellow
}

# Copy Fastlane metadata icons
Write-Host "`nCopying Fastlane metadata icons..." -ForegroundColor Cyan

$appStoreSource = "$projectRoot\app\appicons\appstore.png"
if (Test-Path $appStoreSource) {
    $fastlaneTargets = @(
        "$projectRoot\fastlane\metadata\android\en-US\images\icon.png",
        "$projectRoot\fastlane\metadata\androidbeta\en-US\images\icon.png"
    )
    
    foreach ($target in $fastlaneTargets) {
        if (Test-Path (Split-Path $target)) {
            try {
                Copy-Item $appStoreSource $target -Force
                $relativePath = $target.Replace($projectRoot, "").TrimStart('\')
                Write-Host "✓ Copied $relativePath" -ForegroundColor Green
            }
            catch {
                Write-Host "✗ Failed to copy Fastlane icon - $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    }
}
else {
    Write-Host "⚠ App Store icon not found at $appStoreSource" -ForegroundColor Yellow
}

# Copy repository icon
Write-Host "`nCopying repository icon..." -ForegroundColor Cyan
if (Test-Path $appStoreSource) {
    $repoIconTarget = "$projectRoot\.github\repo_icon.png"
    if (Test-Path (Split-Path $repoIconTarget)) {
        try {
            Copy-Item $appStoreSource $repoIconTarget -Force
            Write-Host "✓ Copied .github/repo_icon.png" -ForegroundColor Green
        }
        catch {
            Write-Host "✗ Failed to copy repo icon - $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}
}

Write-Host "`n🎉 Icon replacement completed!" -ForegroundColor Green
Write-Host "`nNext steps:" -ForegroundColor Cyan
Write-Host "1. Update adaptive icon XML files if needed" -ForegroundColor White
Write-Host "2. Update icon references in build.gradle.kts" -ForegroundColor White
Write-Host "3. Clean and rebuild the project" -ForegroundColor White
Write-Host "4. Test all build variants" -ForegroundColor White

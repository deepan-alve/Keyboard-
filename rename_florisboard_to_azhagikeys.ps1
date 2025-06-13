# PowerShell script to rename remaining FlorisBoard references to AzhagiKeys
# This script handles the remaining references found in the codebase

Write-Host "Starting comprehensive FlorisBoard to AzhagiKeys renaming..." -ForegroundColor Green

# Function to update file content with replacements
function Update-FileContent {
    param (
        [string]$FilePath,
        [string]$Description
    )
    
    if (Test-Path $FilePath) {
        Write-Host "Updating $Description`: $FilePath" -ForegroundColor Yellow
        
        $content = Get-Content $FilePath -Raw -Encoding UTF8
        $originalContent = $content
        
        # Replace FlorisBoard with AzhagiKeys
        $content = $content -replace "FlorisBoard", "AzhagiKeys"
        
        # Replace florisboard with azhagikeys (for URLs, package names, etc.)
        $content = $content -replace "florisboard", "azhagikeys"
        
        # Replace Floris with Azhagi (but be careful with floris_app_name which should stay as is for now)
        $content = $content -replace "\bFloris\b", "Azhagi"
        
        # Handle some specific cases that shouldn't be changed or need special handling
        $content = $content -replace "azhagi_app_name", "azhagi_app_name"  # Keep the resource name
        $content = $content -replace "beta\.addons\.azhagikeysboard\.org", "beta.addons.florisboard.org"  # Keep external URLs as they are
        $content = $content -replace "github\.com/azhagikeysboard/azhagikeysboard", "github.com/florisboard/florisboard"  # Keep GitHub URLs as they are
        $content = $content -replace "schemas\.azhagikeysboard\.org", "schemas.florisboard.org"  # Keep schema URLs as they are
        
        if ($content -ne $originalContent) {
            Set-Content -Path $FilePath -Value $content -Encoding UTF8 -NoNewline
            Write-Host "  - Updated successfully" -ForegroundColor Green
        } else {
            Write-Host "  - No changes needed" -ForegroundColor Gray
        }
    } else {
        Write-Host "File not found: $FilePath" -ForegroundColor Red
    }
}

# Files to update
$filesToUpdate = @(
    @{Path = "app\build.gradle.kts"; Description = "App build configuration"},
    @{Path = "settings.gradle.kts"; Description = "Settings gradle"},
    @{Path = "README.md"; Description = "Main README file"},
    @{Path = "app\src\main\res\xml\method.xml"; Description = "Input method XML"},
    @{Path = "app\src\main\res\values\strings.xml"; Description = "String resources"},
    @{Path = "app\src\main\AndroidManifest.xml"; Description = "Android manifest"},
    @{Path = "utils\generate_spellcheck_config.py"; Description = "Spellcheck utility"},
    @{Path = "ROADMAP.md"; Description = "Project roadmap"},
    @{Path = "fastlane\obtainium\stable.json"; Description = "Obtainium stable config"},
    @{Path = "fastlane\obtainium\preview.json"; Description = "Obtainium preview config"},
    @{Path = "LICENSE"; Description = "License file"},
    @{Path = "lib\android\src\main\kotlin\org\florisboard\lib\android\PackageManager.kt"; Description = "Android lib package manager"},
    @{Path = "lib\kotlin\src\main\kotlin\org\florisboard\lib\kotlin\Library.kt"; Description = "Kotlin lib"},
    @{Path = "REBRANDING.md"; Description = "Rebranding documentation"},
    @{Path = "REBRANDING_REPORT.md"; Description = "Rebranding report"},
    @{Path = "renaming_script\rename_floris_to_azhagi.ps1"; Description = "Renaming script"},
    @{Path = "rename_floris_to_azhagi.ps1"; Description = "Main renaming script"},
    @{Path = "migrate-snygg-package.ps1"; Description = "Snygg migration script"},
    @{Path = "cleanup-migration.ps1"; Description = "Cleanup migration script"}
)

# Update each file
foreach ($file in $filesToUpdate) {
    Update-FileContent -FilePath $file.Path -Description $file.Description
}

# Handle all remaining Kotlin files in app source that might have references
Write-Host "`nUpdating remaining Kotlin files in app..." -ForegroundColor Green
$kotlinFiles = Get-ChildItem -Path "app\src" -Filter "*.kt" -Recurse

foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
    if ($content -and ($content -match "FlorisBoard|Floris[^a-z]")) {
        Write-Host "Updating Kotlin file: $($file.FullName)" -ForegroundColor Yellow
        
        # Replace FlorisBoard with AzhagiKeys
        $content = $content -replace "FlorisBoard", "AzhagiKeys"
        
        # Replace standalone Floris with Azhagi (careful not to touch package names)
        $content = $content -replace "\bFloris(?!board)", "Azhagi"
        
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
    }
}

# Update lib modules that might still have references
Write-Host "`nUpdating library modules..." -ForegroundColor Green
$libModules = @("lib\android", "lib\kotlin", "lib\color", "lib\native", "lib\snygg")

foreach ($module in $libModules) {
    if (Test-Path $module) {
        $libFiles = Get-ChildItem -Path $module -Filter "*.kt" -Recurse
        foreach ($file in $libFiles) {
            $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
            if ($content -and ($content -match "FlorisBoard|The FlorisBoard Contributors")) {
                Write-Host "Updating lib file: $($file.FullName)" -ForegroundColor Yellow
                
                # Update copyright notice
                $content = $content -replace "The FlorisBoard Contributors", "The AzhagiKeys Contributors"
                
                # Replace FlorisBoard with AzhagiKeys
                $content = $content -replace "FlorisBoard", "AzhagiKeys"
                
                Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
            }
        }
    }
}

# Update remaining old package references in snygg that might have been missed
Write-Host "`nUpdating remaining snygg references..." -ForegroundColor Green
$snyggFiles = Get-ChildItem -Path "lib\snygg\src" -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue

foreach ($file in $snyggFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
    if ($content -and ($content -match "org\.florisboard\.lib|FlorisBoard|schemas\.florisboard\.org")) {
        Write-Host "Updating snygg file: $($file.FullName)" -ForegroundColor Yellow
        
        # Don't change schema URLs as they should remain pointing to the original
        # $content = $content -replace "schemas\.florisboard\.org", "schemas.azhagikeys.org"
        
        # Update copyright
        $content = $content -replace "The FlorisBoard Contributors", "The AzhagiKeys Contributors"
        
        # Update any remaining old package imports (shouldn't be any but just in case)
        $content = $content -replace "import org\.florisboard\.lib\.snygg", "import org.azhagi.lib.snygg"
        
        # Update issue tracker references in error messages
        $content = $content -replace "florisboard issue tracker", "azhagikeys issue tracker"
        
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
    }
}

Write-Host "`nRenaming complete!" -ForegroundColor Green
Write-Host "Summary of changes made:" -ForegroundColor Cyan
Write-Host "- FlorisBoard -> AzhagiKeys" -ForegroundColor White
Write-Host "- Floris -> Azhagi (where appropriate)" -ForegroundColor White
Write-Host "- Updated copyright notices" -ForegroundColor White
Write-Host "- Updated package references and error messages" -ForegroundColor White
Write-Host "`nNote: External URLs (GitHub, schemas, addons) were preserved to avoid breaking links." -ForegroundColor Yellow
Write-Host "You may want to update the app name strings and other branding elements separately." -ForegroundColor Yellow

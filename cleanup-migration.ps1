# PowerShell script to complete the package migration cleanup

# Function to update package declarations and imports in a file
function Update-PackageReferences {
    param(
        [string]$FilePath
    )
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        $originalContent = $content
        
        # Update package declaration
        $content = $content -replace "package org\.florisboard\.lib\.snygg", "package org.azhagi.lib.snygg"
        
        # Update imports
        $content = $content -replace "import org\.florisboard\.lib\.snygg", "import org.azhagi.lib.snygg"
        
        # Update copyright header
        $content = $content -replace "Copyright \(C\) 2021-2025 The FlorisBoard Contributors", "Copyright (C) 2025 The AzhagiKeys Contributors"
        $content = $content -replace "Copyright \(C\) 2025 The FlorisBoard Contributors", "Copyright (C) 2025 The AzhagiKeys Contributors"
        
        if ($content -ne $originalContent) {
            Set-Content -Path $FilePath -Value $content -Encoding UTF8
            Write-Host "Updated: $FilePath"
        }
    }
}

# Remove any remaining old florisboard directories
$oldMainDir = "lib\snygg\src\main\kotlin\org\florisboard"
if (Test-Path $oldMainDir) {
    Remove-Item -Recurse -Force $oldMainDir
    Write-Host "Removed old main directory: $oldMainDir"
}

# Update test files and create new directory structure
$testSrcDir = "lib\snygg\src\test\kotlin\org"
if (Test-Path $testSrcDir) {
    # Create new test directory structure
    $newTestDir = "lib\snygg\src\test\kotlin\org\azhagi\lib\snygg"
    if (-not (Test-Path $newTestDir)) {
        New-Item -ItemType Directory -Path $newTestDir -Force
        New-Item -ItemType Directory -Path "$newTestDir\value" -Force
    }
    
    # Move and update test files
    $oldTestDir = "lib\snygg\src\test\kotlin\org\florisboard\lib\snygg"
    if (Test-Path $oldTestDir) {
        Get-ChildItem -Path "$oldTestDir\*.kt" -Recurse | ForEach-Object {
            $relativePath = $_.FullName.Substring($oldTestDir.Length + 1)
            $newPath = Join-Path $newTestDir $relativePath
            
            # Ensure directory exists
            $newDir = Split-Path $newPath -Parent
            if (-not (Test-Path $newDir)) {
                New-Item -ItemType Directory -Path $newDir -Force
            }
            
            # Copy and update the file
            Copy-Item $_.FullName $newPath
            Update-PackageReferences -FilePath $newPath
        }
        
        # Remove old test directory
        Remove-Item -Recurse -Force "lib\snygg\src\test\kotlin\org\florisboard"
        Write-Host "Migrated test files to new package structure"
    }
}

# Update any remaining files that might reference the old package
$filesToCheck = @()

# Find all Kotlin files in the project that might reference the old package
Get-ChildItem -Path "." -Recurse -Include "*.kt" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -and ($content -match "org\.florisboard\.lib\.snygg")) {
        $filesToCheck += $_.FullName
    }
}

# Update each file found
foreach ($file in $filesToCheck) {
    Update-PackageReferences -FilePath $file
}

Write-Host "Package migration cleanup completed!"
Write-Host "Updated files that referenced org.florisboard.lib.snygg"

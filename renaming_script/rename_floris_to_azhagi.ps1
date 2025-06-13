# Renaming script for AzhagiKeys rebranding
# This PowerShell script will help migrate files from FlorisBoard to AzhagiKeys
# by copying them to the new package structure and updating the package names

# Create base directories
$sourcePath = "c:\Users\deepa\Downloads\KEYBO\Keyboard-"
$appSrcPath = "$sourcePath\app\src"

# Create all necessary directories
function CreateDirectories {
    # Main directories for app source
    $mainDirs = @(
        "$appSrcPath\main\kotlin\com\azhagi\azhagikeys",
        "$appSrcPath\androidTest\kotlin\com\azhagi\azhagikeys",
        "$appSrcPath\test\kotlin\com\azhagi\azhagikeys"
    )
    
    foreach ($dir in $mainDirs) {
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Force -Path $dir
            Write-Host "Created directory: $dir"
        }
    }
    
    # Find all subdirectories in the original package and recreate them
    $origDirs = Get-ChildItem -Path "$appSrcPath\main\kotlin\dev\patrickgold\florisboard" -Directory -Recurse
    foreach ($dir in $origDirs) {
        $relativePath = $dir.FullName.Replace("$appSrcPath\main\kotlin\dev\patrickgold\florisboard", "")
        $newDir = "$appSrcPath\main\kotlin\com\azhagi\azhagikeys$relativePath"
        if (-not (Test-Path $newDir)) {
            New-Item -ItemType Directory -Force -Path $newDir
            Write-Host "Created directory: $newDir"
        }
    }
}

# Copy and transform files
function CopyAndTransformFiles {
    $files = Get-ChildItem -Path "$appSrcPath\main\kotlin\dev\patrickgold\florisboard" -File -Recurse -Filter "*.kt"
    
    foreach ($file in $files) {
        $relativePath = $file.FullName.Replace("$appSrcPath\main\kotlin\dev\patrickgold\florisboard", "")
        $destFile = "$appSrcPath\main\kotlin\com\azhagi\azhagikeys$relativePath"
        
        # Read content and replace package names
        $content = Get-Content $file.FullName -Raw
        $newContent = $content -replace "dev.patrickgold.florisboard", "com.azhagi.azhagikeys"
        $newContent = $newContent -replace "FlorisBoard", "AzhagiKeys"
        $newContent = $newContent -replace "Floris", "Azhagi"
        
        # Create the file with new content
        $newContent | Out-File -FilePath $destFile -Encoding utf8
        Write-Host "Transformed file: $destFile"
    }
}

# Rename database schemas
function RenameSchemas {
    $sourceSchemas = "$sourcePath\app\schemas\dev.patrickgold.florisboard.*"
    $dirs = Get-ChildItem -Path $sourceSchemas -Directory
    
    foreach ($dir in $dirs) {
        $newName = $dir.Name -replace "dev.patrickgold.florisboard", "com.azhagi.azhagikeys"
        $newName = $newName -replace "Floris", "Azhagi"
        $destDir = "$sourcePath\app\schemas\$newName"
        
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Force -Path $destDir
            Write-Host "Created schema directory: $destDir"
            
            # Copy schema files
            Copy-Item "$($dir.FullName)\*" -Destination $destDir -Recurse -Force
            Write-Host "Copied schema files to: $destDir"
        }
    }
}

# Run the functions
Write-Host "Starting AzhagiKeys rebranding..."
CreateDirectories
CopyAndTransformFiles
RenameSchemas
Write-Host "Rebranding process completed! Please update build configurations and check for any remaining references."

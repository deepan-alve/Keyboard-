# Define the source and destination package paths
$sourceRoot = "c:\Users\deepa\Downloads\KEYBO\Keyboard-\app\src\main\kotlin\dev\patrickgold\florisboard"
$destRoot = "c:\Users\deepa\Downloads\KEYBO\Keyboard-\app\src\main\kotlin\com\azhagi\azhagikeys"

# Create destination directory if it doesn't exist
if (-not (Test-Path -Path $destRoot)) {
    New-Item -ItemType Directory -Path $destRoot -Force
}

# Function to process files
function Convert-Files {
    param (
        [string]$sourcePath,
        [string]$destPath
    )

    # Create the destination directory if it doesn't exist
    if (-not (Test-Path -Path $destPath)) {
        New-Item -ItemType Directory -Path $destPath -Force
    }

    # Get all files in the current directory
    $files = Get-ChildItem -Path $sourcePath -File

    foreach ($file in $files) {
        $destFilePath = Join-Path -Path $destPath -ChildPath $file.Name
        
        # Read the content of the file
        $content = Get-Content -Path $file.FullName -Raw
        
        # Replace package names
        $content = $content -replace "dev\.patrickgold\.florisboard", "com.azhagi.azhagikeys"
        $content = $content -replace "FlorisBoard", "AzhagiKeys"
        $content = $content -replace "Floris", "Azhagi"
        $content = $content -replace "floris", "azhagi"
        
        # Save the modified content to the destination file
        $content | Set-Content -Path $destFilePath -Force
        
        Write-Host "Processed file: $destFilePath"
    }

    # Process subdirectories recursively
    $subdirs = Get-ChildItem -Path $sourcePath -Directory
    
    foreach ($subdir in $subdirs) {        $destSubdirPath = Join-Path -Path $destPath -ChildPath $subdir.Name
        Convert-Files -sourcePath $subdir.FullName -destPath $destSubdirPath
    }
}

# Start processing files from the root
Convert-Files -sourcePath $sourceRoot -destPath $destRoot

# Process test directories
$sourceTestRoot = "c:\Users\deepa\Downloads\KEYBO\Keyboard-\app\src\test\kotlin\dev\patrickgold\florisboard"
$destTestRoot = "c:\Users\deepa\Downloads\KEYBO\Keyboard-\app\src\test\kotlin\com\azhagi\azhagikeys"

if (Test-Path -Path $sourceTestRoot) {
    Convert-Files -sourcePath $sourceTestRoot -destPath $destTestRoot
}

$sourceAndroidTestRoot = "c:\Users\deepa\Downloads\KEYBO\Keyboard-\app\src\androidTest\kotlin\dev\patrickgold\florisboard"
$destAndroidTestRoot = "c:\Users\deepa\Downloads\KEYBO\Keyboard-\app\src\androidTest\kotlin\com\azhagi\azhagikeys"

if (Test-Path -Path $sourceAndroidTestRoot) {
    Convert-Files -sourcePath $sourceAndroidTestRoot -destPath $destAndroidTestRoot
}

Write-Host "Package renaming completed successfully."

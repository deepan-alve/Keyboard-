# PowerShell script to rename package from org.florisboard.lib.snygg to org.azhagi.lib.snygg

# Create the new directory structure
$newPackageDir = "lib\snygg\src\main\kotlin\org\azhagi\lib\snygg"
if (-not (Test-Path $newPackageDir)) {
    New-Item -ItemType Directory -Path $newPackageDir -Force
}

$newValueDir = "$newPackageDir\value"
if (-not (Test-Path $newValueDir)) {
    New-Item -ItemType Directory -Path $newValueDir -Force
}

$newUiDir = "$newPackageDir\ui"
if (-not (Test-Path $newUiDir)) {
    New-Item -ItemType Directory -Path $newUiDir -Force
}

# Function to copy and update package declarations and imports
function Copy-AndUpdateFile {
    param(
        [string]$SourcePath,
        [string]$DestPath
    )
    
    if (Test-Path $SourcePath) {
        $content = Get-Content $SourcePath -Raw
        
        # Update package declaration
        $content = $content -replace "package org\.florisboard\.lib\.snygg", "package org.azhagi.lib.snygg"
        
        # Update imports
        $content = $content -replace "import org\.florisboard\.lib\.snygg", "import org.azhagi.lib.snygg"
        
        # Update copyright header
        $content = $content -replace "Copyright \(C\) 2021-2025 The FlorisBoard Contributors", "Copyright (C) 2025 The AzhagiKeys Contributors"
        $content = $content -replace "Copyright \(C\) 2025 The FlorisBoard Contributors", "Copyright (C) 2025 The AzhagiKeys Contributors"
        
        # Write to destination
        Set-Content -Path $DestPath -Value $content -Encoding UTF8
        Write-Host "Copied and updated: $SourcePath -> $DestPath"
    }
}

# Copy main snygg files
$mainFiles = @(
    "Snygg.kt",
    "SnyggJsonSchemaGenerator.kt", 
    "SnyggPropertySet.kt",
    "SnyggPropertySetEditor.kt",
    "SnyggRule.kt",
    "SnyggSpecDecl.kt",
    "SnyggStylesheet.kt",
    "SnyggStylesheetEditor.kt",
    "SnyggTheme.kt"
)

foreach ($file in $mainFiles) {
    $sourcePath = "lib\snygg\src\main\kotlin\org\florisboard\lib\snygg\$file"
    $destPath = "$newPackageDir\$file"
    Copy-AndUpdateFile -SourcePath $sourcePath -DestPath $destPath
}

# Copy value files
$valueFiles = Get-ChildItem "lib\snygg\src\main\kotlin\org\florisboard\lib\snygg\value\*.kt" -Name
foreach ($file in $valueFiles) {
    $sourcePath = "lib\snygg\src\main\kotlin\org\florisboard\lib\snygg\value\$file"
    $destPath = "$newValueDir\$file"
    Copy-AndUpdateFile -SourcePath $sourcePath -DestPath $destPath
}

# Copy ui files  
$uiFiles = Get-ChildItem "lib\snygg\src\main\kotlin\org\florisboard\lib\snygg\ui\*.kt" -Name
foreach ($file in $uiFiles) {
    $sourcePath = "lib\snygg\src\main\kotlin\org\florisboard\lib\snygg\ui\$file"
    $destPath = "$newUiDir\$file"
    Copy-AndUpdateFile -SourcePath $sourcePath -DestPath $destPath
}

Write-Host "Package migration completed!"
Write-Host "Files copied to new package structure under org.azhagi.lib.snygg"

# Update build.gradle.kts
$buildGradlePath = "lib\snygg\build.gradle.kts"
if (Test-Path $buildGradlePath) {
    $buildContent = Get-Content $buildGradlePath -Raw
    $buildContent = $buildContent -replace 'namespace = "org\.florisboard\.lib\.snygg"', 'namespace = "org.azhagi.lib.snygg"'
    $buildContent = $buildContent -replace 'mainClass\.set\("org\.florisboard\.lib\.snygg\.SnyggJsonSchemaGenerator"\)', 'mainClass.set("org.azhagi.lib.snygg.SnyggJsonSchemaGenerator")'
    Set-Content -Path $buildGradlePath -Value $buildContent -Encoding UTF8
    Write-Host "Updated build.gradle.kts"
}

Write-Host "Next steps:"
Write-Host "1. Remove the old package directory: lib\snygg\src\main\kotlin\org\florisboard"
Write-Host "2. Update any other files that import from org.florisboard.lib.snygg"
Write-Host "3. Run gradle build to test compilation"

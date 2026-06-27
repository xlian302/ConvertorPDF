param(
    [Parameter(Mandatory=$true)]
    [string]$Version,
    [string]$Changelog = "Nueva version"
)

$token = $env:GH_TOKEN
if (-not $token) { Write-Error "Define GH_TOKEN con tu token de GitHub"; exit 1 }
$headers = @{ Authorization = "token $token"; Accept = "application/vnd.github.v3+json" }
$projDir = Split-Path -Parent $PSCommandPath

# 1. Update versionCode in build.gradle.kts
$buildFile = "$projDir\app\build.gradle.kts"
$content = Get-Content $buildFile -Raw
$currentCode = [regex]::Match($content, 'versionCode\s*=\s*(\d+)').Groups[1].Value
$newCode = [int]$currentCode + 1
$content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
$content = $content -replace 'versionName\s*=\s*"[^"]*"', "versionName = `"$Version`""
Set-Content $buildFile $content

# 2. Build the APK
Write-Output ">>> Building APK v$Version (code $newCode)..."
Set-Location $projDir
.\gradlew assembleRelease
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed"; exit 1 }

# 3. Copy APK
$apkName = "ConvertorPDF_v$Version.apk"
Copy-Item "$projDir\app\build\outputs\apk\release\app-release.apk" "$projDir\$apkName" -Force

# 4. Commit and push version bump
git add -A
git commit -m "Bump version to $Version"
git push https://xlian302:$token@github.com/xlian302/ConvertorPDF.git main

# 5. Create GitHub release
$tag = "v$Version"
$body = @{ tag_name = $tag; target_commitish = "main"; name = $tag; body = $Changelog; draft = $false; prerelease = $false }
$bodyJson = $body | ConvertTo-Json
$release = Invoke-RestMethod -Uri "https://api.github.com/repos/xlian302/ConvertorPDF/releases" -Headers $headers -Method Post -Body $bodyJson -ContentType "application/json"
Write-Output ">>> Release creado: $($release.html_url)"

# 6. Upload APK
$uploadUrl = "https://uploads.github.com/repos/xlian302/ConvertorPDF/releases/$($release.id)/assets?name=$apkName"
$apkBytes = [System.IO.File]::ReadAllBytes("$projDir\$apkName")
$uploadHeaders = @{ Authorization = "token $token"; "Content-Type" = "application/vnd.android.package-archive" }
$asset = Invoke-RestMethod -Uri $uploadUrl -Headers $uploadHeaders -Method Post -Body $apkBytes
Write-Output ">>> APK subido: $($asset.browser_download_url)"

# 7. Update version.json with proper UTF-8
$versionJson = @{
    latestVersionCode = $newCode
    latestVersionName = $Version
    downloadUrl = "https://github.com/xlian302/ConvertorPDF/releases/latest/download/$apkName"
    changelog = $Changelog
}
$jsonString = $versionJson | ConvertTo-Json
[System.IO.File]::WriteAllText("$projDir\version.json", $jsonString, [System.Text.Encoding]::UTF8)

# 8. Commit and push version.json update
git add -A
git commit -m "Update version.json for v$Version"
git push https://xlian302:$token@github.com/xlian302/ConvertorPDF.git main

Write-Output "========================================"
Write-Output "Version $Version publicada exitosamente!"
Write-Output "Los usuarios recibiran la actualizacion automaticamente."
Write-Output "========================================"

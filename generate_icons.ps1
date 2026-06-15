Add-Type -AssemblyName System.Drawing

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $scriptDir) { $scriptDir = "." }

$sourcePath = Join-Path $scriptDir "assets\app_icon_trimmed.png"
$baseDir = Join-Path $scriptDir "app\src\main\res"

if (-not (Test-Path $sourcePath)) {
    Write-Error "Source image not found at $sourcePath"
    exit 1
}

$sizes = @{
    "mipmap-mdpi" = @{ "legacy" = 48; "adaptive" = 108 }
    "mipmap-hdpi" = @{ "legacy" = 72; "adaptive" = 162 }
    "mipmap-xhdpi" = @{ "legacy" = 96; "adaptive" = 216 }
    "mipmap-xxhdpi" = @{ "legacy" = 144; "adaptive" = 324 }
    "mipmap-xxxhdpi" = @{ "legacy" = 192; "adaptive" = 432 }
}

# Load the source image
$srcImage = [System.Drawing.Image]::FromFile($sourcePath)

foreach ($folder in $sizes.Keys) {
    $legacySize = $sizes[$folder]["legacy"]
    $adaptiveSize = $sizes[$folder]["adaptive"]
    $destFolder = Join-Path $baseDir $folder
    
    # Create folder if it doesn't exist
    if (-not (Test-Path $destFolder)) {
        New-Item -ItemType Directory -Path $destFolder -Force | Out-Null
    }

    # Output paths
    $squarePath = Join-Path $destFolder "ic_launcher.png"
    $roundPath = Join-Path $destFolder "ic_launcher_round.png"
    $foregroundPath = Join-Path $destFolder "ic_launcher_foreground.png"
    
    # 1. Generate Square Legacy Icon
    $bmp = New-Object System.Drawing.Bitmap($legacySize, $legacySize)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    
    $g.DrawImage($srcImage, 0, 0, $legacySize, $legacySize)
    $bmp.Save($squarePath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    
    # 2. Generate Round Legacy Icon (with circular clip)
    $bmpRound = New-Object System.Drawing.Bitmap($legacySize, $legacySize)
    $gRound = [System.Drawing.Graphics]::FromImage($bmpRound)
    
    $gRound.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gRound.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $gRound.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $gRound.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse(0, 0, $legacySize, $legacySize)
    $gRound.SetClip($path)
    
    $gRound.DrawImage($srcImage, 0, 0, $legacySize, $legacySize)
    $bmpRound.Save($roundPath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    $path.Dispose()
    $gRound.Dispose()
    $bmpRound.Dispose()

    # 3. Generate Adaptive Icon Foreground (Transparent background, centered logo)
    $bmpFg = New-Object System.Drawing.Bitmap($adaptiveSize, $adaptiveSize)
    $gFg = [System.Drawing.Graphics]::FromImage($bmpFg)
    $gFg.Clear([System.Drawing.Color]::Transparent)

    $gFg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gFg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $gFg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $gFg.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    # Scale logo to be 68% of the adaptive icon canvas to fit in the safe central zone
    $logoSize = [int]($adaptiveSize * 0.68)
    $offsetX = [int](($adaptiveSize - $logoSize) / 2)
    $offsetY = [int](($adaptiveSize - $logoSize) / 2)

    $gFg.DrawImage($srcImage, $offsetX, $offsetY, $logoSize, $logoSize)
    $bmpFg.Save($foregroundPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $gFg.Dispose()
    $bmpFg.Dispose()
}

$srcImage.Dispose()
Write-Host "Launcher icons and adaptive foregrounds generated successfully from local source assets/app_icon_trimmed.png!"

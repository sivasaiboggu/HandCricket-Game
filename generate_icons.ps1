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
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

# Load the source image
$srcImage = [System.Drawing.Image]::FromFile($sourcePath)

foreach ($folder in $sizes.Keys) {
    $size = $sizes[$folder]
    $destFolder = Join-Path $baseDir $folder
    
    # Create folder if it doesn't exist
    if (-not (Test-Path $destFolder)) {
        New-Item -ItemType Directory -Path $destFolder -Force | Out-Null
    }

    # Square Icon Path
    $squarePath = Join-Path $destFolder "ic_launcher.png"
    # Round Icon Path
    $roundPath = Join-Path $destFolder "ic_launcher_round.png"
    
    # 1. Generate Square Icon
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    
    $g.DrawImage($srcImage, 0, 0, $size, $size)
    $bmp.Save($squarePath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    # 2. Generate Round Icon (with circular clip)
    $bmpRound = New-Object System.Drawing.Bitmap($size, $size)
    $gRound = [System.Drawing.Graphics]::FromImage($bmpRound)
    
    $gRound.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gRound.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $gRound.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $gRound.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse(0, 0, $size, $size)
    $gRound.SetClip($path)
    
    $gRound.DrawImage($srcImage, 0, 0, $size, $size)
    $bmpRound.Save($roundPath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    # Clean up resources
    $path.Dispose()
    $gRound.Dispose()
    $bmpRound.Dispose()
    $g.Dispose()
    $bmp.Dispose()
}

$srcImage.Dispose()
Write-Host "Launcher icons generated successfully from local source assets/app_icon_trimmed.png!"

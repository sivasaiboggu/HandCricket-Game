Add-Type -AssemblyName System.Drawing

$sourcePath = "c:\Users\HP\HandCricket-Game\assets\app_icon_source.png"
$trimmedPath = "c:\Users\HP\HandCricket-Game\assets\app_icon_trimmed.png"

if (-not (Test-Path $sourcePath)) {
    Write-Error "Source image not found at $sourcePath"
    exit 1
}

$bmp = [System.Drawing.Bitmap]::FromFile($sourcePath)
$w = $bmp.Width
$h = $bmp.Height

$minX = $w
$maxX = 0
$minY = $h
$maxY = 0

# Scan the image to locate the bounding box of non-white & non-transparent pixels
for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
        $c = $bmp.GetPixel($x, $y)
        # Pixel is considered whitespace if it is fully transparent (A < 10)
        # or white/near-white (R > 240, G > 240, B > 240)
        $isWhite = ($c.R -gt 240 -and $c.G -gt 240 -and $c.B -gt 240)
        $isTransparent = ($c.A -lt 10)
        
        if (-not $isWhite -and -not $isTransparent) {
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }
}

# If no non-white region is found, bypass trimming
if ($minX -ge $maxX -or $minY -ge $maxY) {
    Write-Host "No non-white region found to trim. Copying original source."
    $bmp.Dispose()
    Copy-Item $sourcePath $trimmedPath -Force
    exit 0
}

# Add small uniform padding (8% of the width) to make the cropped icon sit perfectly
$cropW = $maxX - $minX + 1
$cropH = $maxY - $minY + 1
$padding = [int]($cropW * 0.08)
if ($padding -lt 4) { $padding = 4 }

$newW = $cropW + (2 * $padding)
$newH = $cropH + (2 * $padding)

# Create a new blank bitmap with transparent background
$trimmedBmp = New-Object System.Drawing.Bitmap($newW, $newH)
$g = [System.Drawing.Graphics]::FromImage($trimmedBmp)
$g.Clear([System.Drawing.Color]::Transparent)

# Set rendering quality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

$srcRect = New-Object System.Drawing.Rectangle($minX, $minY, $cropW, $cropH)
$destRect = New-Object System.Drawing.Rectangle($padding, $padding, $cropW, $cropH)

$g.DrawImage($bmp, $destRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)

$bmp.Dispose()
$g.Dispose()

# Save the trimmed and centered logo
$trimmedBmp.Save($trimmedPath, [System.Drawing.Imaging.ImageFormat]::Png)
$trimmedBmp.Dispose()

Write-Host "Trimmed source image saved successfully to $trimmedPath"

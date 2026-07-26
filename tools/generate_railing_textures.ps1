$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$outputDirectory = Join-Path $PSScriptRoot "..\src\main\resources\assets\railward\textures\block\train\parts"
$outputDirectory = [System.IO.Path]::GetFullPath($outputDirectory)
[System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null

$palettes = [ordered]@{
    "railing_base"   = @(248, 248, 248)
    "railing_red"    = @(180, 58, 50)
    "railing_yellow" = @(230, 185, 68)
    "railing_orange" = @(219, 118, 43)
    "railing_white"  = @(228, 228, 222)
    "railing_gray"   = @(119, 125, 130)
}

function Clamp-Channel([int] $value) {
    return [Math]::Max(0, [Math]::Min(255, $value))
}

foreach ($entry in $palettes.GetEnumerator()) {
    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt 16; $y++) {
            for ($x = 0; $x -lt 16; $x++) {
                $grain = (($x * 17 + $y * 31 + (($x * $y) % 7) * 13) % 13) - 6
                if ($x -eq 3 -or $x -eq 11) {
                    $grain += 7
                }
                if (($y -eq 5 -and $x -ge 2 -and $x -le 7) -or
                    ($y -eq 12 -and $x -ge 9 -and $x -le 14)) {
                    $grain -= 9
                }

                $r = Clamp-Channel ($entry.Value[0] + $grain)
                $g = Clamp-Channel ($entry.Value[1] + $grain)
                $b = Clamp-Channel ($entry.Value[2] + $grain)
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $b))
            }
        }

        $path = Join-Path $outputDirectory ($entry.Key + ".png")
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $bitmap.Dispose()
    }
}

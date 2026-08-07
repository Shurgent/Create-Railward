param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot),
    [ValidateSet("18_100", "triple_axle_equalized_locomotive")]
    [string]$Variant = "18_100"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$isTripleAxleEqualizedLocomotive = $Variant -eq "triple_axle_equalized_locomotive"
$modelFolder = if ($isTripleAxleEqualizedLocomotive) { "triple_axle_equalized_locomotive" } else { "18_100" }
$assetName = if ($isTripleAxleEqualizedLocomotive) { "bogey_triple_axle_equalized_locomotive" } else { "bogey_18_100" }
$displayName = if ($isTripleAxleEqualizedLocomotive) { "Railward Triple-Axle Equalized Locomotive Bogey" } else { "Railward 18-100 Bogey" }
$modelIdentifier = if ($isTripleAxleEqualizedLocomotive) { "railward:bogey_triple_axle_equalized_locomotive" } else { "railward:bogey_18_100" }
$previewTitle = if ($isTripleAxleEqualizedLocomotive) { "Triple-axle equalized locomotive bogey starter" } else { "18-100 / freight bogey starter" }
$previewDimensions = if ($isTripleAxleEqualizedLocomotive) {
    "5 blocks / axle centers Z = -32, 0, +32 px / pivot Z = 0"
} else {
    "3 blocks / axle centers Z = -16, +16 px / pivot Z = 0"
}
$wheelsetOffsets = if ($isTripleAxleEqualizedLocomotive) { @(-2.0, 0.0, 2.0) } else { @(-1.0, 1.0) }

$assetRoot = Join-Path $Root "src/main/resources/assets/railward"
$modelDir = Join-Path $assetRoot "models/block/train/bogey/$modelFolder"
$textureDir = Join-Path $assetRoot "textures/block/train/bogey"
$sourceDir = Join-Path $Root "tools/blockbench/bogeys"
$previewDir = Join-Path $Root "tools/previews"

foreach ($directory in @($modelDir, $textureDir, $sourceDir, $previewDir)) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
}

$script:meshes = @{
    frame = [System.Collections.Generic.List[object]]::new()
    wheelset = [System.Collections.Generic.List[object]]::new()
}

$textureSize = 64
$materialUv = @{
    frame = @(1, 1, 15, 15)
    frame_edge = @(17, 1, 31, 15)
    spring = @(33, 1, 47, 15)
    wheel = @(49, 1, 62, 15)
    tread = @(1, 17, 15, 31)
    axis = @(17, 17, 31, 31)
    brake = @(33, 17, 47, 31)
    rust = @(49, 17, 62, 31)
}

$materialColor = @{
    frame = [System.Drawing.Color]::FromArgb(255, 55, 61, 62)
    frame_edge = [System.Drawing.Color]::FromArgb(255, 78, 85, 84)
    spring = [System.Drawing.Color]::FromArgb(255, 31, 35, 35)
    wheel = [System.Drawing.Color]::FromArgb(255, 63, 69, 69)
    tread = [System.Drawing.Color]::FromArgb(255, 118, 123, 119)
    axis = [System.Drawing.Color]::FromArgb(255, 48, 53, 53)
    brake = [System.Drawing.Color]::FromArgb(255, 54, 42, 35)
    rust = [System.Drawing.Color]::FromArgb(255, 112, 67, 43)
}

if ($isTripleAxleEqualizedLocomotive) {
    $materialColor.frame = [System.Drawing.Color]::FromArgb(255, 48, 54, 55)
    $materialColor.frame_edge = [System.Drawing.Color]::FromArgb(255, 70, 77, 77)
    $materialColor.spring = [System.Drawing.Color]::FromArgb(255, 29, 33, 33)
    $materialColor.wheel = [System.Drawing.Color]::FromArgb(255, 58, 64, 64)
    $materialColor.brake = [System.Drawing.Color]::FromArgb(255, 60, 45, 36)
}

function V([double]$x, [double]$y, [double]$z) {
    return [double[]]@($x, $y, $z)
}

function Add-Vec($a, $b) {
    return V ($a[0] + $b[0]) ($a[1] + $b[1]) ($a[2] + $b[2])
}

function Sub-Vec($a, $b) {
    return V ($a[0] - $b[0]) ($a[1] - $b[1]) ($a[2] - $b[2])
}

function Mul-Vec($a, [double]$value) {
    return V ($a[0] * $value) ($a[1] * $value) ($a[2] * $value)
}

function Dot-Vec($a, $b) {
    return $a[0] * $b[0] + $a[1] * $b[1] + $a[2] * $b[2]
}

function Cross-Vec($a, $b) {
    return V `
        ($a[1] * $b[2] - $a[2] * $b[1]) `
        ($a[2] * $b[0] - $a[0] * $b[2]) `
        ($a[0] * $b[1] - $a[1] * $b[0])
}

function Normalize-Vec($a) {
    $length = [Math]::Sqrt((Dot-Vec $a $a))
    if ($length -lt 0.000001) {
        return V 0 0 0
    }
    return Mul-Vec $a (1.0 / $length)
}

function Limit([double]$value, [double]$minimum, [double]$maximum) {
    return [Math]::Min($maximum, [Math]::Max($minimum, $value))
}

function Add-Face(
    [string]$component,
    [string]$material,
    [object[]]$vertices,
    [string]$name,
    [int[]]$uvOrder = @(0, 1, 2, 3)
) {
    $outwardVertices = @($vertices)
    [Array]::Reverse($outwardVertices)
    $script:meshes[$component].Add([pscustomobject]@{
        Material = $material
        Vertices = $outwardVertices
        Name = $name
        UvOrder = $uvOrder
    })
}

function Add-Box(
    [string]$component,
    [string]$material,
    [double]$x0,
    [double]$y0,
    [double]$z0,
    [double]$x1,
    [double]$y1,
    [double]$z1,
    [string]$name
) {
    $p000 = V $x0 $y0 $z0
    $p001 = V $x0 $y0 $z1
    $p010 = V $x0 $y1 $z0
    $p011 = V $x0 $y1 $z1
    $p100 = V $x1 $y0 $z0
    $p101 = V $x1 $y0 $z1
    $p110 = V $x1 $y1 $z0
    $p111 = V $x1 $y1 $z1

    Add-Face $component $material @($p000, $p100, $p110, $p010) "$name.z-"
    Add-Face $component $material @($p101, $p001, $p011, $p111) "$name.z+"
    Add-Face $component $material @($p001, $p000, $p010, $p011) "$name.x-"
    Add-Face $component $material @($p100, $p101, $p111, $p110) "$name.x+"
    Add-Face $component $material @($p010, $p110, $p111, $p011) "$name.y+"
    Add-Face $component $material @($p001, $p101, $p100, $p000) "$name.y-"
}

function Add-BeamYZ(
    [string]$component,
    [string]$material,
    [double]$xCenter,
    [double]$xWidth,
    [double]$y0,
    [double]$z0,
    [double]$y1,
    [double]$z1,
    [double]$thickness,
    [string]$name
) {
    $dy = $y1 - $y0
    $dz = $z1 - $z0
    $length = [Math]::Sqrt($dy * $dy + $dz * $dz)
    $py = -$dz / $length * $thickness / 2.0
    $pz = $dy / $length * $thickness / 2.0
    $x0p = $xCenter - $xWidth / 2.0
    $x1p = $xCenter + $xWidth / 2.0

    $a0 = V $x0p ($y0 + $py) ($z0 + $pz)
    $b0 = V $x0p ($y0 - $py) ($z0 - $pz)
    $c0 = V $x0p ($y1 - $py) ($z1 - $pz)
    $d0 = V $x0p ($y1 + $py) ($z1 + $pz)
    $a1 = V $x1p ($y0 + $py) ($z0 + $pz)
    $b1 = V $x1p ($y0 - $py) ($z0 - $pz)
    $c1 = V $x1p ($y1 - $py) ($z1 - $pz)
    $d1 = V $x1p ($y1 + $py) ($z1 + $pz)

    Add-Face $component $material @($a0, $b0, $c0, $d0) "$name.outer-"
    Add-Face $component $material @($b1, $a1, $d1, $c1) "$name.outer+"
    Add-Face $component $material @($a1, $a0, $d0, $d1) "$name.edge1"
    Add-Face $component $material @($b0, $b1, $c1, $c0) "$name.edge2"
    Add-Face $component $material @($d0, $c0, $c1, $d1) "$name.end1"
    Add-Face $component $material @($a1, $b1, $b0, $a0) "$name.end0"
}

function Get-CylinderPoint(
    [string]$axis,
    [double]$centerX,
    [double]$centerY,
    [double]$centerZ,
    [double]$along,
    [double]$radius,
    [double]$angle
) {
    $c = [Math]::Cos($angle) * $radius
    $s = [Math]::Sin($angle) * $radius
    switch ($axis) {
        "X" { return V ($centerX + $along) ($centerY + $c) ($centerZ + $s) }
        "Y" { return V ($centerX + $c) ($centerY + $along) ($centerZ + $s) }
        default { return V ($centerX + $c) ($centerY + $s) ($centerZ + $along) }
    }
}

function Add-Cylinder(
    [string]$component,
    [string]$sideMaterial,
    [string]$capMaterial,
    [string]$axis,
    [double]$centerX,
    [double]$centerY,
    [double]$centerZ,
    [double]$radius,
    [double]$length,
    [int]$segments,
    [string]$name
) {
    $half = $length / 2.0
    $center0 = switch ($axis) {
        "X" { V ($centerX - $half) $centerY $centerZ }
        "Y" { V $centerX ($centerY - $half) $centerZ }
        default { V $centerX $centerY ($centerZ - $half) }
    }
    $center1 = switch ($axis) {
        "X" { V ($centerX + $half) $centerY $centerZ }
        "Y" { V $centerX ($centerY + $half) $centerZ }
        default { V $centerX $centerY ($centerZ + $half) }
    }

    for ($i = 0; $i -lt $segments; $i++) {
        $a0 = 2.0 * [Math]::PI * $i / $segments
        $a1 = 2.0 * [Math]::PI * ($i + 1) / $segments
        $p00 = Get-CylinderPoint $axis $centerX $centerY $centerZ (-$half) $radius $a0
        $p01 = Get-CylinderPoint $axis $centerX $centerY $centerZ (-$half) $radius $a1
        $p11 = Get-CylinderPoint $axis $centerX $centerY $centerZ $half $radius $a1
        $p10 = Get-CylinderPoint $axis $centerX $centerY $centerZ $half $radius $a0

        # Add-Face reverses every polygon before export. The circular basis used
        # by a Y-aligned cylinder has the opposite handedness to X/Z, so its
        # side faces need the opposite input winding. Conversely, X/Z end caps
        # need the opposite winding to Y. Keeping this axis-aware prevents
        # Minecraft's back-face culling from exposing the inside of wheels and
        # springs.
        $sideVertices = if ($axis -eq "Y") {
            @($p01, $p11, $p10, $p00)
        } else {
            @($p00, $p10, $p11, $p01)
        }
        $cap0Vertices = if ($axis -eq "Y") {
            @($center0, $p01, $p00)
        } else {
            @($center0, $p00, $p01)
        }
        $cap1Vertices = if ($axis -eq "Y") {
            @($center1, $p10, $p11)
        } else {
            @($center1, $p11, $p10)
        }

        Add-Face $component $sideMaterial $sideVertices "$name.side.$i" @(0, 3, 2, 1)
        Add-Face $component $capMaterial $cap0Vertices "$name.cap0.$i"
        Add-Face $component $capMaterial $cap1Vertices "$name.cap1.$i"
    }
}

function Get-PerpendicularBasis($direction) {
    $helper = if ([Math]::Abs($direction[1]) -lt 0.85) { V 0 1 0 } else { V 1 0 0 }
    $u = Normalize-Vec (Cross-Vec $direction $helper)
    $v = Normalize-Vec (Cross-Vec $direction $u)
    return @($u, $v)
}

function Add-Tube(
    [string]$component,
    [string]$material,
    [object[]]$points,
    [double]$radius,
    [int]$segments,
    [string]$name
) {
    $rings = [System.Collections.Generic.List[object]]::new()
    for ($i = 0; $i -lt $points.Count; $i++) {
        if ($i -eq 0) {
            $direction = Normalize-Vec (Sub-Vec $points[1] $points[0])
        } elseif ($i -eq $points.Count - 1) {
            $direction = Normalize-Vec (Sub-Vec $points[$i] $points[$i - 1])
        } else {
            $direction = Normalize-Vec (Sub-Vec $points[$i + 1] $points[$i - 1])
        }
        $basis = Get-PerpendicularBasis $direction
        $ring = [System.Collections.Generic.List[object]]::new()
        for ($j = 0; $j -lt $segments; $j++) {
            $angle = 2.0 * [Math]::PI * $j / $segments
            $offset = Add-Vec `
                (Mul-Vec $basis[0] ([Math]::Cos($angle) * $radius)) `
                (Mul-Vec $basis[1] ([Math]::Sin($angle) * $radius))
            $ring.Add((Add-Vec $points[$i] $offset))
        }
        $rings.Add($ring)
    }

    for ($i = 0; $i -lt $rings.Count - 1; $i++) {
        for ($j = 0; $j -lt $segments; $j++) {
            $next = ($j + 1) % $segments
            Add-Face $component $material @(
                $rings[$i][$j],
                $rings[$i + 1][$j],
                $rings[$i + 1][$next],
                $rings[$i][$next]
            ) "$name.$i.$j"
        }
    }
}

function Add-HelixSpring(
    [double]$x,
    [double]$z,
    [double]$y0,
    [double]$y1,
    [double]$coilRadius,
    [double]$wireRadius,
    [double]$turns,
    [string]$name
) {
    $pointCount = [Math]::Ceiling($turns * 8) + 1
    $points = [System.Collections.Generic.List[object]]::new()
    for ($i = 0; $i -lt $pointCount; $i++) {
        $t = $i / ($pointCount - 1.0)
        $angle = $t * $turns * 2.0 * [Math]::PI
        $points.Add((V `
            ($x + [Math]::Cos($angle) * $coilRadius) `
            ($y0 + ($y1 - $y0) * $t) `
            ($z + [Math]::Sin($angle) * $coilRadius)
        ))
    }
    Add-Tube "frame" "spring" $points.ToArray() $wireRadius 4 $name
}

function Add-ZigZagSpring(
    [double]$x,
    [double]$z,
    [double]$y0,
    [double]$y1,
    [double]$coilRadius,
    [double]$wireRadius,
    [string]$name
) {
    $points = [System.Collections.Generic.List[object]]::new()
    $steps = 8
    for ($i = 0; $i -le $steps; $i++) {
        $t = $i / [double]$steps
        $offset = if ($i -eq 0 -or $i -eq $steps) {
            0
        } elseif ($i % 2 -eq 0) {
            -$coilRadius
        } else {
            $coilRadius
        }
        $points.Add((V $x ($y0 + ($y1 - $y0) * $t) ($z + $offset)))
    }
    Add-Tube "frame" "spring" $points.ToArray() $wireRadius 4 $name
}

function Add-ArcTubeX(
    [double]$x,
    [double]$centerY,
    [double]$centerZ,
    [double]$radius,
    [double]$angle0,
    [double]$angle1,
    [double]$wireRadius,
    [string]$name
) {
    $points = [System.Collections.Generic.List[object]]::new()
    for ($i = 0; $i -le 8; $i++) {
        $t = $i / 8.0
        $angle = $angle0 + ($angle1 - $angle0) * $t
        $points.Add((V $x `
            ($centerY + [Math]::Cos($angle) * $radius) `
            ($centerZ + [Math]::Sin($angle) * $radius)
        ))
    }
    Add-Tube "frame" "brake" $points.ToArray() $wireRadius 5 $name
}

function Build-Wheelset {
    $axleHalfWidth = if ($isTripleAxleEqualizedLocomotive) { 1.20 } else { 1.17 }
    $axleHalfSize = if ($isTripleAxleEqualizedLocomotive) { 0.070 } else { 0.0625 }
    $wheelRadius = if ($isTripleAxleEqualizedLocomotive) { 0.500 } else { 0.46875 }
    $wheelLength = if ($isTripleAxleEqualizedLocomotive) { 0.195 } else { 0.180 }
    $flangeRadius = if ($isTripleAxleEqualizedLocomotive) { 0.550 } else { 0.500 }
    $flangeLength = if ($isTripleAxleEqualizedLocomotive) { 0.060 } else { 0.055 }
    $hubRadius = if ($isTripleAxleEqualizedLocomotive) { 0.155 } else { 0.130 }
    $hubLength = if ($isTripleAxleEqualizedLocomotive) { 0.245 } else { 0.225 }
    $wheelCenterX = if ($isTripleAxleEqualizedLocomotive) { 0.900 } else { 0.875 }

    Add-Box "wheelset" "axis" `
        (-$axleHalfWidth) (-$axleHalfSize) (-$axleHalfSize) `
        $axleHalfWidth $axleHalfSize $axleHalfSize `
        "square_axle"

    foreach ($side in @(-1, 1)) {
        $centerX = $side * $wheelCenterX
        Add-Cylinder "wheelset" "tread" "wheel" "X" `
            $centerX 0 0 $wheelRadius $wheelLength 8 "wheel.$side"
        Add-Cylinder "wheelset" "tread" "wheel" "X" `
            ($centerX - $side * 0.112) 0 0 $flangeRadius $flangeLength 8 "flange.$side"
        Add-Cylinder "wheelset" "axis" "axis" "X" `
            $centerX 0 0 $hubRadius $hubLength 8 "hub.$side"
    }
}

function Build-TripleAxleEqualizedLocomotiveFrame {
    foreach ($side in @(-1, 1)) {
        $x = $side * 1.055

        # Three prominent axlebox/pedestal units define the straight
        # equalized locomotive side-frame silhouette.
        foreach ($z in $wheelsetOffsets) {
            Add-Box "frame" "frame" `
                ($x - 0.155) -0.20 ($z - 0.27) `
                ($x + 0.155) 0.36 ($z + 0.27) `
                "axlebox.$side.$z"
            Add-Cylinder "frame" "frame_edge" "frame_edge" "X" `
                $x 0.055 $z 0.195 0.35 8 "axlebox_cap.$side.$z"
            Add-Box "frame" "frame_edge" `
                ($x - 0.17) 0.29 ($z - 0.21) `
                ($x + 0.17) 0.46 ($z + 0.21) `
                "axlebox_adapter.$side.$z"
        }

        # The real bogie has a long, nearly straight frame. Slightly lowered
        # ends preserve its characteristic heavy locomotive profile without
        # introducing the trapezoidal 18-100 silhouette.
        Add-BeamYZ "frame" "frame" $x 0.24 0.50 -2.46 0.62 -2.06 0.22 "upper_end_front.$side"
        Add-BeamYZ "frame" "frame" $x 0.24 0.62 -2.06 0.62 2.06 0.22 "upper_main.$side"
        Add-BeamYZ "frame" "frame" $x 0.24 0.62 2.06 0.50 2.46 0.22 "upper_end_rear.$side"

        # Equalizer beams between the axleboxes.
        foreach ($gapCenter in @(-1.0, 1.0)) {
            Add-BeamYZ "frame" "frame_edge" $x 0.20 -0.05 ($gapCenter - 0.73) 0.04 $gapCenter 0.16 `
                "equalizer_a.$side.$gapCenter"
            Add-BeamYZ "frame" "frame_edge" $x 0.20 0.04 $gapCenter -0.05 ($gapCenter + 0.73) 0.16 `
                "equalizer_b.$side.$gapCenter"
        }

        # Four compact spring packs, represented the same Create-friendly way
        # as the 18-100: six-sided cylinders with a coil texture.
        foreach ($springCenter in @(-1.18, 1.18)) {
            Add-Box "frame" "frame_edge" `
                ($x - 0.17) -0.13 ($springCenter - 0.32) `
                ($x + 0.17) -0.04 ($springCenter + 0.32) `
                "spring_seat.$side.$springCenter"
            Add-Box "frame" "frame_edge" `
                ($x - 0.17) 0.48 ($springCenter - 0.32) `
                ($x + 0.17) 0.57 ($springCenter + 0.32) `
                "spring_cap.$side.$springCenter"
            foreach ($springOffset in @(-0.19, 0.0, 0.19)) {
                Add-Cylinder "frame" "spring" "spring" "Y" `
                    ($x - $side * 0.025) 0.22 ($springCenter + $springOffset) `
                    0.067 0.50 6 "spring.$side.$springCenter.$springOffset"
            }
        }

        # Brake shoes around all three wheelsets.
        foreach ($z in $wheelsetOffsets) {
            Add-Box "frame" "brake" `
                (($side * 0.79) - 0.075) -0.18 ($z - 0.49) `
                (($side * 0.79) + 0.075) 0.19 ($z - 0.38) `
                "brake_shoe_front.$side.$z"
            Add-Box "frame" "brake" `
                (($side * 0.79) - 0.075) -0.18 ($z + 0.38) `
                (($side * 0.79) + 0.075) 0.19 ($z + 0.49) `
                "brake_shoe_rear.$side.$z"
        }
    }

    # Crossmembers and a flat central support. No protruding kingpin: the
    # stationary half-block of a future working bogie occupies that space.
    foreach ($z in @(-2.18, 0.0, 2.18)) {
        Add-Box "frame" "frame" -1.20 0.48 ($z - 0.16) 1.20 0.69 ($z + 0.16) "crossmember.$z"
    }
    Add-Box "frame" "frame_edge" -1.10 0.66 -0.38 1.10 0.76 0.38 "central_support"
    Add-Box "frame" "frame" -0.38 0.74 -0.48 0.38 0.82 0.48 "central_plate"

    # Simplified traction-motor and gear housings fill the spaces between the
    # axles while remaining deliberately blocky.
    foreach ($motorZ in @(-1.55, 0.0, 1.55)) {
        Add-Box "frame" "axis" -0.70 0.04 ($motorZ - 0.24) 0.70 0.38 ($motorZ + 0.24) "traction_motor.$motorZ"
        Add-Cylinder "frame" "axis" "axis" "X" 0 0.19 ($motorZ + 0.26) 0.23 1.15 8 "gear_housing.$motorZ"
    }

    Add-Box "frame" "brake" -0.88 0.18 -2.34 0.88 0.25 -2.26 "brake_crossbar_front"
    Add-Box "frame" "brake" -0.88 0.18 2.26 0.88 0.25 2.34 "brake_crossbar_rear"
}

function Build-Frame {
    foreach ($side in @(-1, 1)) {
        $x = $side * 1.02

        foreach ($z in @(-1.0, 1.0)) {
            Add-Box "frame" "frame" `
                ($x - 0.14) -0.16 ($z - 0.22) `
                ($x + 0.14) 0.33 ($z + 0.22) `
                "axlebox.$side.$z"
            Add-Cylinder "frame" "frame_edge" "frame_edge" "X" `
                $x 0.045 $z 0.17 0.32 8 "axlebox_cap.$side.$z"
            Add-Box "frame" "frame_edge" `
                ($x - 0.15) 0.255 ($z - 0.18) `
                ($x + 0.15) 0.41 ($z + 0.18) `
                "adapter.$side.$z"
        }

        Add-BeamYZ "frame" "frame" $x 0.21 0.50 -1.17 0.55 -0.39 0.17 "upper_left.$side"
        Add-BeamYZ "frame" "frame" $x 0.21 0.55 -0.39 0.55 0.39 0.17 "upper_center.$side"
        Add-BeamYZ "frame" "frame" $x 0.21 0.55 0.39 0.50 1.17 0.17 "upper_right.$side"

        Add-BeamYZ "frame" "frame_edge" $x 0.19 -0.01 -0.84 -0.11 -0.39 0.14 "lower_left.$side"
        Add-BeamYZ "frame" "frame_edge" $x 0.19 -0.11 -0.39 -0.11 0.39 0.14 "lower_center.$side"
        Add-BeamYZ "frame" "frame_edge" $x 0.19 -0.11 0.39 -0.01 0.84 0.14 "lower_right.$side"

        Add-Box "frame" "frame" `
            ($x - 0.125) -0.08 -0.44 `
            ($x + 0.125) 0.54 -0.33 `
            "spring_jaw_left.$side"
        Add-Box "frame" "frame" `
            ($x - 0.125) -0.08 0.33 `
            ($x + 0.125) 0.54 0.44 `
            "spring_jaw_right.$side"
        Add-Box "frame" "frame_edge" `
            ($x - 0.15) -0.13 -0.39 `
            ($x + 0.15) -0.04 0.39 `
            "spring_seat.$side"
        Add-Box "frame" "frame_edge" `
            ($x - 0.15) 0.47 -0.39 `
            ($x + 0.15) 0.55 0.39 `
            "spring_cap.$side"

        $springZ = @(-0.28, -0.14, 0.0, 0.14, 0.28)
        for ($i = 0; $i -lt $springZ.Count; $i++) {
            $springRadius = if ($i -eq 0 -or $i -eq $springZ.Count - 1) { 0.055 } else { 0.05 }
            Add-Cylinder "frame" "spring" "spring" "Y" `
                ($x - $side * 0.025) 0.2175 $springZ[$i] `
                $springRadius 0.485 6 "spring.$side.$i"
        }

        Add-BeamYZ "frame" "rust" ($x - $side * 0.025) 0.22 0.11 -0.36 0.42 -0.29 0.10 "wedge_left.$side"
        Add-BeamYZ "frame" "rust" ($x - $side * 0.025) 0.22 0.42 0.29 0.11 0.36 0.10 "wedge_right.$side"

        Add-Box "frame" "brake" `
            (($side * 0.77) - 0.07) -0.15 -0.64 `
            (($side * 0.77) + 0.07) 0.17 -0.55 `
            "brake_shoe_left.$side"
        Add-Box "frame" "brake" `
            (($side * 0.77) - 0.07) -0.15 0.55 `
            (($side * 0.77) + 0.07) 0.17 0.64 `
            "brake_shoe_right.$side"
    }

    Add-Box "frame" "frame" -1.18 0.49 -0.22 1.18 0.72 0.22 "bolster"
    Add-Box "frame" "frame_edge" -1.08 0.68 -0.27 1.08 0.76 0.27 "bolster_top"
    Add-Box "frame" "frame" -0.25 0.72 -0.34 0.25 0.80 0.34 "center_plate_base"
    Add-Cylinder "frame" "frame_edge" "frame_edge" "Y" 0 0.81 0 0.29 0.10 8 "center_bowl"

    Add-Box "frame" "brake" -0.89 0.12 -0.60 0.89 0.18 -0.54 "brake_crossbar_left"
    Add-Box "frame" "brake" -0.89 0.12 0.54 0.89 0.18 0.60 "brake_crossbar_right"
    Add-BeamYZ "frame" "brake" -0.72 0.05 0.15 -0.57 0.20 -0.33 0.05 "brake_link_left"
    Add-BeamYZ "frame" "brake" 0.72 0.05 0.15 0.33 0.20 0.57 0.05 "brake_link_right"
}

function Write-Texture {
    $path = Join-Path $textureDir "$assetName.png"
    $bitmap = [System.Drawing.Bitmap]::new($textureSize, $textureSize)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::FromArgb(255, 25, 28, 28))
    $randomSeed = if ($isTripleAxleEqualizedLocomotive) { 3634000 } else { 18100 }
    $random = [System.Random]::new($randomSeed)

    foreach ($entry in $materialUv.GetEnumerator()) {
        $uv = $entry.Value
        $base = $materialColor[$entry.Key]
        for ($y = $uv[1]; $y -le $uv[3]; $y++) {
            for ($x = $uv[0]; $x -le $uv[2]; $x++) {
                $noise = $random.Next(-8, 9)
                if ((($x + 2 * $y) % 11) -eq 0) {
                    $noise += 5
                }
                $r = Limit ($base.R + $noise) 0 255
                $g = Limit ($base.G + $noise) 0 255
                $b = Limit ($base.B + $noise) 0 255
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $b))
            }
        }
        $borderPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 17, 19, 19), 1)
        $graphics.DrawRectangle($borderPen, $uv[0] - 1, $uv[1] - 1, $uv[2] - $uv[0] + 2, $uv[3] - $uv[1] + 2)
        $borderPen.Dispose()
    }

    $springTile = $materialUv.spring
    for ($y = $springTile[1]; $y -le $springTile[3]; $y++) {
        $phase = ($y - $springTile[1]) % 4
        $shade = switch ($phase) {
            0 { 18 }
            1 { 54 }
            2 { 39 }
            default { 22 }
        }
        for ($x = $springTile[0]; $x -le $springTile[2]; $x++) {
            $edgeShade = if ($x -eq $springTile[0] -or $x -eq $springTile[2]) { -8 } else { 0 }
            $value = Limit ($shade + $edgeShade) 0 255
            $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $value, $value + 3, $value + 3))
        }
    }

    for ($i = 0; $i -lt 22; $i++) {
        $x = $random.Next(1, $textureSize - 1)
        $y = $random.Next(35, $textureSize - 1)
        $color = if ($i % 3 -eq 0) {
            [System.Drawing.Color]::FromArgb(255, 105, 62, 40)
        } else {
            [System.Drawing.Color]::FromArgb(255, 48, 52, 51)
        }
        $bitmap.SetPixel($x, $y, $color)
    }

    $graphics.Dispose()
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Get-ObjUv([string]$material, [int]$corner) {
    $tile = $materialUv[$material]
    $points = @(
        @(($tile[0] / $textureSize), (1.0 - $tile[3] / $textureSize)),
        @(($tile[2] / $textureSize), (1.0 - $tile[3] / $textureSize)),
        @(($tile[2] / $textureSize), (1.0 - $tile[1] / $textureSize)),
        @(($tile[0] / $textureSize), (1.0 - $tile[1] / $textureSize))
    )
    return $points[$corner % 4]
}

function Write-Obj([string]$component) {
    $objPath = Join-Path $modelDir "${assetName}_${component}.obj"
    $mtlPath = Join-Path $modelDir "${assetName}_${component}.mtl"
    $obj = [System.Text.StringBuilder]::new()
    [void]$obj.AppendLine("# $displayName - generated editable starter")
    [void]$obj.AppendLine("mtllib ${assetName}_${component}.mtl")
    [void]$obj.AppendLine("o ${assetName}_${component}")

    $vertexIndex = 1
    foreach ($face in $script:meshes[$component]) {
        [void]$obj.AppendLine("g $($face.Name -replace '[^A-Za-z0-9_.-]', '_')")
        [void]$obj.AppendLine("usemtl $($face.Material)")
        foreach ($vertex in $face.Vertices) {
            [void]$obj.AppendLine((
                "v {0:F6} {1:F6} {2:F6}" -f $vertex[0], $vertex[1], $vertex[2]
            ))
        }
        for ($i = 0; $i -lt $face.Vertices.Count; $i++) {
            $uvCorner = if ($i -lt $face.UvOrder.Count) { $face.UvOrder[$i] } else { $i }
            $uv = Get-ObjUv $face.Material $uvCorner
            [void]$obj.AppendLine(("vt {0:F6} {1:F6}" -f $uv[0], $uv[1]))
        }
        $indices = for ($i = 0; $i -lt $face.Vertices.Count; $i++) {
            "{0}/{0}" -f ($vertexIndex + $i)
        }
        [void]$obj.AppendLine("f $($indices -join ' ')")
        $vertexIndex += $face.Vertices.Count
    }
    [System.IO.File]::WriteAllText($objPath, $obj.ToString(), [System.Text.UTF8Encoding]::new($false))

    $mtl = [System.Text.StringBuilder]::new()
    [void]$mtl.AppendLine("# Texture variables are resolved by the Forge OBJ loader")
    foreach ($material in $materialUv.Keys) {
        [void]$mtl.AppendLine("newmtl $material")
        [void]$mtl.AppendLine("Ka 1.000000 1.000000 1.000000")
        [void]$mtl.AppendLine("Kd 1.000000 1.000000 1.000000")
        [void]$mtl.AppendLine("Ks 0.000000 0.000000 0.000000")
        [void]$mtl.AppendLine("map_Kd #texture")
        [void]$mtl.AppendLine()
    }
    [System.IO.File]::WriteAllText($mtlPath, $mtl.ToString(), [System.Text.UTF8Encoding]::new($false))

    $wrapper = [ordered]@{
        parent = "block/block"
        loader = "forge:obj"
        "flip-v" = $true
        model = "railward:models/block/train/bogey/$modelFolder/${assetName}_${component}.obj"
        textures = [ordered]@{
            particle = "railward:block/train/bogey/$assetName"
            texture = "railward:block/train/bogey/$assetName"
        }
    }
    $jsonPath = Join-Path $modelDir "${assetName}_${component}.json"
    [System.IO.File]::WriteAllText(
        $jsonPath,
        ($wrapper | ConvertTo-Json -Depth 8),
        [System.Text.UTF8Encoding]::new($false)
    )
}

function New-Id([string]$seed) {
    $md5 = [System.Security.Cryptography.MD5]::Create()
    $bytes = [System.Text.Encoding]::UTF8.GetBytes("railward-$Variant-$seed")
    $hash = $md5.ComputeHash($bytes)
    $md5.Dispose()
    return [Guid]::new($hash).ToString()
}

function Get-BbPartName([string]$faceName) {
    return $faceName -replace `
        '\.(?:z-|z\+|x-|x\+|y-|y\+|outer-|outer\+|edge1|edge2|end1|end0|side\.\d+|cap0\.\d+|cap1\.\d+)$', `
        ''
}

function New-BbMesh([string]$component, [string]$partName, [object[]]$faces) {
    $vertices = [ordered]@{}
    $meshFaces = [ordered]@{}
    $vertexCounter = 0
    $faceCounter = 0
    $minimum = @(1.0e9, 1.0e9, 1.0e9)
    $maximum = @(-1.0e9, -1.0e9, -1.0e9)

    foreach ($face in $faces) {
        foreach ($vertex in $face.Vertices) {
            for ($axis = 0; $axis -lt 3; $axis++) {
                $minimum[$axis] = [Math]::Min($minimum[$axis], $vertex[$axis])
                $maximum[$axis] = [Math]::Max($maximum[$axis], $vertex[$axis])
            }
        }
    }
    $origin = @(
        [Math]::Round(($minimum[0] + $maximum[0]) * 8.0, 5),
        [Math]::Round(($minimum[1] + $maximum[1]) * 8.0, 5),
        [Math]::Round(($minimum[2] + $maximum[2]) * 8.0, 5)
    )

    foreach ($face in $faces) {
        $faceIds = [System.Collections.Generic.List[string]]::new()
        $uvMap = [ordered]@{}
        for ($i = 0; $i -lt $face.Vertices.Count; $i++) {
            $id = "v$vertexCounter"
            $vertex = $face.Vertices[$i]
            $vertices[$id] = @(
                [Math]::Round($vertex[0] * 16.0 - $origin[0], 5),
                [Math]::Round($vertex[1] * 16.0 - $origin[1], 5),
                [Math]::Round($vertex[2] * 16.0 - $origin[2], 5)
            )
            $tile = $materialUv[$face.Material]
            $uvCorners = @(
                @($tile[0], $tile[3]),
                @($tile[2], $tile[3]),
                @($tile[2], $tile[1]),
                @($tile[0], $tile[1])
            )
            $uvCorner = if ($i -lt $face.UvOrder.Count) { $face.UvOrder[$i] } else { $i % 4 }
            $cornerUv = $uvCorners[$uvCorner]
            $uvMap[$id] = $cornerUv
            $faceIds.Add($id)
            $vertexCounter++
        }
        $meshFaces["f$faceCounter"] = [ordered]@{
            uv = $uvMap
            vertices = $faceIds.ToArray()
            texture = 0
        }
        $faceCounter++
    }

    return [ordered]@{
        name = $partName
        type = "mesh"
        uuid = New-Id "$component-$partName"
        color = 0
        origin = $origin
        vertices = $vertices
        faces = $meshFaces
    }
}

function Write-BbModel {
    $elements = [System.Collections.Generic.List[object]]::new()
    $outliner = [System.Collections.Generic.List[object]]::new()

    $sourceComponents = [ordered]@{ frame = @($script:meshes.frame) }
    for ($wheelsetIndex = 0; $wheelsetIndex -lt $wheelsetOffsets.Count; $wheelsetIndex++) {
        $offset = $wheelsetOffsets[$wheelsetIndex]
        $componentName = if ($isTripleAxleEqualizedLocomotive) {
            @("wheelset_front", "wheelset_center", "wheelset_rear")[$wheelsetIndex]
        } else {
            @("wheelset_front", "wheelset_rear")[$wheelsetIndex]
        }
        $sourceComponents[$componentName] = @(
            $script:meshes.wheelset | ForEach-Object { Transform-Face $_ $offset }
        )
    }

    foreach ($component in $sourceComponents.Keys) {
        $children = [System.Collections.Generic.List[string]]::new()
        $partNames = @(
            $sourceComponents[$component] |
                ForEach-Object { Get-BbPartName $_.Name } |
                Sort-Object -Unique
        )
        foreach ($partName in $partNames) {
            $faces = @(
                $sourceComponents[$component] |
                    Where-Object { (Get-BbPartName $_.Name) -eq $partName }
            )
            if ($faces.Count -eq 0) {
                continue
            }
            $mesh = New-BbMesh $component $partName $faces
            $elements.Add($mesh)
            $children.Add($mesh.uuid)
        }
        $outliner.Add([ordered]@{
            name = $component
            origin = @(0, 0, 0)
            color = 0
            uuid = New-Id "group-$component"
            export = $true
            isOpen = $true
            children = $children.ToArray()
        })
    }

    $project = [ordered]@{
        meta = [ordered]@{
            format_version = "4.10"
            model_format = "free"
            box_uv = $false
        }
        name = $displayName
        model_identifier = $modelIdentifier
        visible_box = @(1, 1, 0)
        variable_placeholders = ""
        variable_placeholder_buttons = @()
        timeline_setups = @()
        unhandled_root_fields = [ordered]@{}
        resolution = [ordered]@{ width = $textureSize; height = $textureSize }
        elements = $elements.ToArray()
        outliner = $outliner.ToArray()
        textures = @(
            [ordered]@{
                path = "../../../src/main/resources/assets/railward/textures/block/train/bogey/$assetName.png"
                name = "$assetName.png"
                folder = "block/train/bogey"
                namespace = "railward"
                id = "0"
                particle = $true
                render_mode = "default"
                render_sides = "auto"
                frame_time = 1
                frame_order_type = "loop"
                frame_order = ""
                frame_interpolate = $false
                visible = $true
                internal = $false
                saved = $true
                uuid = New-Id "texture"
                relative_path = "block/train/bogey/$assetName.png"
            }
        )
    }
    $path = Join-Path $sourceDir "$assetName.bbmodel"
    [System.IO.File]::WriteAllText(
        $path,
        ($project | ConvertTo-Json -Depth 100),
        [System.Text.UTF8Encoding]::new($false)
    )
}

function Transform-Face($face, [double]$offsetZ) {
    $vertices = [System.Collections.Generic.List[object]]::new()
    foreach ($vertex in $face.Vertices) {
        $vertices.Add((V $vertex[0] $vertex[1] ($vertex[2] + $offsetZ)))
    }
    return [pscustomobject]@{
        Material = $face.Material
        Vertices = $vertices.ToArray()
        Name = $face.Name
        UvOrder = $face.UvOrder
    }
}

function Get-PreviewFaces {
    $faces = [System.Collections.Generic.List[object]]::new()
    foreach ($face in $script:meshes.frame) {
        $faces.Add((Transform-Face $face 0))
    }
    foreach ($offset in $wheelsetOffsets) {
        foreach ($face in $script:meshes.wheelset) {
            $faces.Add((Transform-Face $face $offset))
        }
    }
    return $faces
}

function Darken-Color([System.Drawing.Color]$color, [double]$factor) {
    $factor = Limit $factor 0.30 1.18
    return [System.Drawing.Color]::FromArgb(
        255,
        (Limit ([int]($color.R * $factor)) 0 255),
        (Limit ([int]($color.G * $factor)) 0 255),
        (Limit ([int]($color.B * $factor)) 0 255)
    )
}

function Draw-View(
    [System.Drawing.Graphics]$graphics,
    [object[]]$faces,
    $camera,
    [double]$centerX,
    [double]$centerY,
    [double]$scale,
    [double]$width,
    [double]$height
) {
    $target = V 0 0.25 0
    $forward = Normalize-Vec (Sub-Vec $target $camera)
    $right = Normalize-Vec (Cross-Vec $forward (V 0 1 0))
    $up = Normalize-Vec (Cross-Vec $right $forward)
    $light = Normalize-Vec (V -0.5 0.9 -0.35)

    $drawFaces = [System.Collections.Generic.List[object]]::new()
    foreach ($face in $faces) {
        if ($face.Vertices.Count -lt 3) {
            continue
        }
        $edgeA = Sub-Vec $face.Vertices[1] $face.Vertices[0]
        $edgeB = Sub-Vec $face.Vertices[2] $face.Vertices[0]
        $normal = Normalize-Vec (Cross-Vec $edgeA $edgeB)
        $centroid = V 0 0 0
        foreach ($vertex in $face.Vertices) {
            $centroid = Add-Vec $centroid $vertex
        }
        $centroid = Mul-Vec $centroid (1.0 / $face.Vertices.Count)
        $toCamera = Normalize-Vec (Sub-Vec $camera $centroid)
        $facing = Dot-Vec $normal $toCamera
        if ($facing -le 0.005) {
            continue
        }

        $points = [System.Collections.Generic.List[System.Drawing.PointF]]::new()
        $depth = 0.0
        foreach ($vertex in $face.Vertices) {
            $relative = Sub-Vec $vertex $target
            $screenX = $centerX + (Dot-Vec $relative $right) * $scale
            $screenY = $centerY - (Dot-Vec $relative $up) * $scale
            $points.Add([System.Drawing.PointF]::new([single]$screenX, [single]$screenY))
            $depth += Dot-Vec (Sub-Vec $vertex $camera) $forward
        }
        $depth /= $face.Vertices.Count
        $shade = 0.58 + [Math]::Max(0, (Dot-Vec $normal $light)) * 0.48
        $drawFaces.Add([pscustomobject]@{
            Depth = $depth
            Points = $points.ToArray()
            Color = Darken-Color $materialColor[$face.Material] $shade
        })
    }

    foreach ($drawFace in ($drawFaces | Sort-Object Depth -Descending)) {
        $brush = [System.Drawing.SolidBrush]::new($drawFace.Color)
        $pen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(95, 13, 16, 16), 1.0)
        $graphics.FillPolygon($brush, $drawFace.Points)
        $graphics.DrawPolygon($pen, $drawFace.Points)
        $pen.Dispose()
        $brush.Dispose()
    }
}

function Write-Preview {
    $previewPath = Join-Path $previewDir "${assetName}_preview.png"
    $bitmap = [System.Drawing.Bitmap]::new(1600, 900)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $background = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        [System.Drawing.Point]::new(0, 0),
        [System.Drawing.Point]::new(0, 900),
        [System.Drawing.Color]::FromArgb(255, 40, 47, 51),
        [System.Drawing.Color]::FromArgb(255, 22, 26, 29)
    )
    $graphics.FillRectangle($background, 0, 0, 1600, 900)
    $background.Dispose()

    $gridPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(30, 185, 202, 205), 1)
    for ($x = 0; $x -le 1600; $x += 80) {
        $graphics.DrawLine($gridPen, $x, 0, $x, 900)
    }
    for ($y = 0; $y -le 900; $y += 80) {
        $graphics.DrawLine($gridPen, 0, $y, 1600, $y)
    }
    $gridPen.Dispose()

    $faces = Get-PreviewFaces
    if ($isTripleAxleEqualizedLocomotive) {
        Draw-View $graphics $faces (V 6.2 3.4 -7.8) 570 505 145 1020 740
        Draw-View $graphics $faces (V 7.0 1.2 0.0) 1300 565 95 500 550
    } else {
        Draw-View $graphics $faces (V 4.6 2.8 -5.8) 600 500 210 1050 760
        Draw-View $graphics $faces (V 5.5 1.0 0.0) 1300 565 155 500 550
    }

    $titleFont = [System.Drawing.Font]::new("Segoe UI Semibold", 34)
    $bodyFont = [System.Drawing.Font]::new("Segoe UI", 17)
    $smallFont = [System.Drawing.Font]::new("Consolas", 14)
    $white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(245, 233, 238, 238))
    $muted = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(220, 165, 177, 180))
    $accent = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 210, 135, 80))

    $graphics.DrawString($previewTitle, $titleFont, $white, 58, 42)
    $graphics.DrawString("Railward / independent asset", $bodyFont, $accent, 62, 96)
    $graphics.DrawString($previewDimensions, $smallFont, $muted, 62, 834)
    $graphics.DrawString("3/4 view", $bodyFont, $muted, 62, 145)
    $graphics.DrawString("side view", $bodyFont, $muted, 1215, 250)

    $accent.Dispose()
    $muted.Dispose()
    $white.Dispose()
    $smallFont.Dispose()
    $bodyFont.Dispose()
    $titleFont.Dispose()
    $graphics.Dispose()
    $bitmap.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

Build-Wheelset
if ($isTripleAxleEqualizedLocomotive) {
    Build-TripleAxleEqualizedLocomotiveFrame
} else {
    Build-Frame
}
Write-Texture
Write-Obj "frame"
Write-Obj "wheelset"
Write-BbModel
Write-Preview

Write-Output "Generated $displayName assets:"
Write-Output "  frame faces: $($script:meshes.frame.Count)"
Write-Output "  wheelset faces: $($script:meshes.wheelset.Count)"
Write-Output "  model directory: $modelDir"
Write-Output "  texture: $(Join-Path $textureDir "$assetName.png")"
Write-Output "  source: $(Join-Path $sourceDir "$assetName.bbmodel")"
Write-Output "  preview: $(Join-Path $previewDir "${assetName}_preview.png")"

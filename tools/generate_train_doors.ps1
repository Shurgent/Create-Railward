param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$AssetsRoot = Join-Path $ProjectRoot "src/main/resources/assets/railward"
$DataRoot = Join-Path $ProjectRoot "src/main/resources/data"
$MinecraftJar = Join-Path $env:USERPROFILE ".gradle/caches/forge_gradle/minecraft_repo/versions/1.18.2/client-extra.jar"

$Colors = @(
    @{ id = "blue"; dye = "blue"; rgb = @(55, 67, 191); texture = "train_wall_blue_smooth"; en = "Blue"; ru = "Blue"; uk = "Blue" },
    @{ id = "green"; dye = "green"; rgb = @(67, 111, 27); texture = "train_wall_green_smooth"; en = "Green"; ru = "Green"; uk = "Green" },
    @{ id = "light_blue"; dye = "light_blue"; rgb = @(58, 175, 217); texture = "train_wall_light_blue_smooth"; en = "Light Blue"; ru = "Light Blue"; uk = "Light Blue" }
)

$WindowColors = @("blue", "green", "light_blue")

$Woods = @(
    @{ id = "stripped_oak"; item = "stripped_oak_log"; texture = "stripped_oak_log"; en = "Stripped Oak"; ru = "Stripped Oak"; uk = "Stripped Oak" },
    @{ id = "stripped_spruce"; item = "stripped_spruce_log"; texture = "stripped_spruce_log"; en = "Stripped Spruce"; ru = "Stripped Spruce"; uk = "Stripped Spruce" },
    @{ id = "stripped_birch"; item = "stripped_birch_log"; texture = "stripped_birch_log"; en = "Stripped Birch"; ru = "Stripped Birch"; uk = "Stripped Birch" },
    @{ id = "stripped_jungle"; item = "stripped_jungle_log"; texture = "stripped_jungle_log"; en = "Stripped Jungle"; ru = "Stripped Jungle"; uk = "Stripped Jungle" },
    @{ id = "stripped_acacia"; item = "stripped_acacia_log"; texture = "stripped_acacia_log"; en = "Stripped Acacia"; ru = "Stripped Acacia"; uk = "Stripped Acacia" },
    @{ id = "stripped_dark_oak"; item = "stripped_dark_oak_log"; texture = "stripped_dark_oak_log"; en = "Stripped Dark Oak"; ru = "Stripped Dark Oak"; uk = "Stripped Dark Oak" },
    @{ id = "stripped_crimson"; item = "stripped_crimson_stem"; texture = "stripped_crimson_stem"; en = "Stripped Crimson"; ru = "Stripped Crimson"; uk = "Stripped Crimson" },
    @{ id = "stripped_warped"; item = "stripped_warped_stem"; texture = "stripped_warped_stem"; en = "Stripped Warped"; ru = "Stripped Warped"; uk = "Stripped Warped" }
)

function Ensure-Dir([string] $Path) {
    $dir = Split-Path $Path -Parent
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}

function Write-Text([string] $Path, [string] $Content) {
    Ensure-Dir $Path
    [System.IO.File]::WriteAllText($Path, $Content, $Utf8NoBom)
}

function Write-Json([string] $Path, $Object, [int] $Depth = 20) {
    $json = $Object | ConvertTo-Json -Depth $Depth
    Write-Text $Path ($json + "`n")
}

function Assert-ProjectPath([string] $Path) {
    $root = [System.IO.Path]::GetFullPath($ProjectRoot).TrimEnd('\', '/')
    $full = [System.IO.Path]::GetFullPath($Path)
    if (!$full.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove path outside project root: $full"
    }
}

function Remove-GeneratedMatches([string] $Dir, [string] $Filter, [bool] $Directories) {
    if (!(Test-Path $Dir)) {
        return
    }
    Assert-ProjectPath $Dir
    if ($Directories) {
        $matches = Get-ChildItem -Path $Dir -Filter $Filter -Directory -ErrorAction SilentlyContinue
    } else {
        $matches = Get-ChildItem -Path $Dir -Filter $Filter -File -ErrorAction SilentlyContinue
    }
    foreach ($match in $matches) {
        Assert-ProjectPath $match.FullName
        Remove-Item -LiteralPath $match.FullName -Recurse -Force -ErrorAction Stop
    }
}

function Remove-ExistingDoorResources {
    foreach ($prefix in @("train_hinged_door_", "train_sliding_door_")) {
        Remove-GeneratedMatches (Join-Path $AssetsRoot "blockstates/train/parts") "$prefix*.json" $false
        Remove-GeneratedMatches (Join-Path $AssetsRoot "models/block/train/parts") "$prefix*" $true
        Remove-GeneratedMatches (Join-Path $AssetsRoot "models/item/train/parts") "$prefix*.json" $false
        Remove-GeneratedMatches (Join-Path $AssetsRoot "textures/block/train/parts/doors") "$prefix*.png" $false
        Remove-GeneratedMatches (Join-Path $AssetsRoot "textures/item/train/parts") "$prefix*.png" $false
        Remove-GeneratedMatches (Join-Path $DataRoot "railward/loot_tables/blocks/train/parts") "$prefix*.json" $false
        Remove-GeneratedMatches (Join-Path $DataRoot "railward/recipes") "$prefix*.json" $false
    }
}

function Color([int] $A, [int[]] $Rgb) {
    return [System.Drawing.Color]::FromArgb($A, $Rgb[0], $Rgb[1], $Rgb[2])
}

function Adjust([int[]] $Rgb, [int] $Delta) {
    return @(
        [Math]::Max(0, [Math]::Min(255, $Rgb[0] + $Delta)),
        [Math]::Max(0, [Math]::Min(255, $Rgb[1] + $Delta)),
        [Math]::Max(0, [Math]::Min(255, $Rgb[2] + $Delta))
    )
}

function New-BitmapFromColor([int[]] $Rgb) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $delta = 0
            if ((($x + $y) % 5) -eq 0) {
                $delta = 10
            } elseif ((($x - $y) % 7) -eq 0) {
                $delta = -8
            }
            $bmp.SetPixel($x, $y, (Color 255 (Adjust $Rgb $delta)))
        }
    }
    return $bmp
}

function Load-VanillaTexture([string] $Name) {
    if (!(Test-Path $MinecraftJar)) {
        throw "Minecraft client jar was not found: $MinecraftJar"
    }
    $zip = [System.IO.Compression.ZipFile]::OpenRead($MinecraftJar)
    try {
        $entry = $zip.GetEntry("assets/minecraft/textures/block/$Name.png")
        if ($null -eq $entry) {
            throw "Texture $Name was not found in $MinecraftJar"
        }
        $stream = $entry.Open()
        try {
            $source = New-Object System.Drawing.Bitmap $stream
            $copy = New-Object System.Drawing.Bitmap 16, 16
            $graphics = [System.Drawing.Graphics]::FromImage($copy)
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $graphics.DrawImage($source, 0, 0, 16, 16)
            $graphics.Dispose()
            $source.Dispose()
            return $copy
        } finally {
            $stream.Dispose()
        }
    } finally {
        $zip.Dispose()
    }
}

function Load-RailwardTexture([string] $Name) {
    $path = Join-Path $AssetsRoot "textures/block/train/parts/$Name.png"
    if (!(Test-Path $path)) {
        throw "Railward texture was not found: $path"
    }
    $source = New-Object System.Drawing.Bitmap $path
    try {
        $copy = New-Object System.Drawing.Bitmap 16, 16
        $graphics = [System.Drawing.Graphics]::FromImage($copy)
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.DrawImage($source, 0, 0, 16, 16)
        $graphics.Dispose()
        return $copy
    } finally {
        $source.Dispose()
    }
}

function New-BaseTexture([hashtable] $ColorDef, [hashtable] $WoodDef, [int] $Delta) {
    if ($null -ne $WoodDef) {
        return Load-VanillaTexture $WoodDef.texture
    }
    if ($null -ne $ColorDef -and $ColorDef.ContainsKey("texture")) {
        return Load-RailwardTexture $ColorDef.texture
    }
    return New-BitmapFromColor (Adjust $ColorDef.rgb $Delta)
}

function Draw-PanelDetails([System.Drawing.Bitmap] $Bmp, [string] $Animation, [string] $Window,
        [string] $Role, [string] $HandleSide, [string] $Half) {
    $border = [System.Drawing.Color]::FromArgb(255, 30, 30, 34)
    $seal = [System.Drawing.Color]::FromArgb(255, 18, 18, 20)
    $sealHi = [System.Drawing.Color]::FromArgb(255, 62, 62, 66)
    $handle = [System.Drawing.Color]::FromArgb(255, 210, 210, 190)
    $glass = [System.Drawing.Color]::FromArgb(0, 128, 190, 220)
    $glassHi = [System.Drawing.Color]::FromArgb(255, 215, 245, 255)

    $drawTop = $Half -ne "bottom"
    $drawBottom = $Half -ne "top"
    if ($Role -eq "item" -or $Role -eq "side") {
        $drawTop = $true
        $drawBottom = $true
    }

    for ($i = 0; $i -lt 16; $i++) {
        if ($drawTop) {
            $Bmp.SetPixel($i, 0, $border)
        }
        if ($drawBottom) {
            $Bmp.SetPixel($i, 15, $border)
        }
        $Bmp.SetPixel(0, $i, $border)
        $Bmp.SetPixel(15, $i, $border)
    }

    if ($Animation -eq "sliding" -and ($Role -eq "front" -or $Role -eq "item")) {
        for ($y = 0; $y -lt 16; $y++) {
            $Bmp.SetPixel(7, $y, $sealHi)
            $Bmp.SetPixel(8, $y, $seal)
        }
    }

    function Draw-Window([int] $minX, [int] $maxX, [int] $minY, [int] $maxY) {
        for ($y = $minY; $y -le $maxY; $y++) {
            for ($x = $minX; $x -le $maxX; $x++) {
                $edge = $x -eq $minX -or $x -eq $maxX -or $y -eq $minY -or $y -eq $maxY
                if ($edge) {
                    $Bmp.SetPixel($x, $y, $border)
                } else {
                    $Bmp.SetPixel($x, $y, $glass)
                }
            }
        }
        $Bmp.SetPixel($minX + 1, $minY + 1, $glassHi)
        $Bmp.SetPixel($minX + 2, $minY + 1, $glassHi)
    }

    if ($Window -eq "small") {
        if ($Animation -eq "sliding") {
            Draw-Window 2 5 4 9
            Draw-Window 10 13 4 9
        } else {
            Draw-Window 5 10 4 9
        }
    } elseif ($Window -eq "big") {
        if ($Animation -eq "sliding") {
            Draw-Window 1 6 2 12
            Draw-Window 9 14 2 12
        } else {
            Draw-Window 4 11 2 12
        }
    }

    if ($Animation -eq "hinged" -and $HandleSide) {
        if ($HandleSide -eq "left") {
            foreach ($point in @(@(3, 4), @(4, 4), @(3, 5), @(4, 5))) {
                $Bmp.SetPixel($point[0], $point[1], $handle)
            }
            $Bmp.SetPixel(2, 4, $border)
            $Bmp.SetPixel(2, 5, $border)
        } else {
            foreach ($point in @(@(11, 4), @(12, 4), @(11, 5), @(12, 5))) {
                $Bmp.SetPixel($point[0], $point[1], $handle)
            }
            $Bmp.SetPixel(13, 4, $border)
            $Bmp.SetPixel(13, 5, $border)
        }
    }
}

function Save-Bitmap([System.Drawing.Bitmap] $Bmp, [string] $Path) {
    Ensure-Dir $Path
    $Bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $Bmp.Dispose()
}

function Write-DoorTextures([string] $Id, [string] $Animation, [hashtable] $ColorDef,
        [hashtable] $WoodDef, [string] $Window) {
    $textureBase = Join-Path $AssetsRoot "textures/block/train/parts/doors"

    $bottom = New-BaseTexture $ColorDef $WoodDef 0
    Draw-PanelDetails $bottom $Animation $null "front" $null "bottom"
    Save-Bitmap $bottom (Join-Path $textureBase "$($Id)_bottom.png")

    if ($Animation -eq "hinged") {
        $bottomLeft = New-BaseTexture $ColorDef $WoodDef 0
        Draw-PanelDetails $bottomLeft $Animation $null "front" "left" "bottom"
        Save-Bitmap $bottomLeft (Join-Path $textureBase "$($Id)_bottom_handle_left.png")

        $bottomRight = New-BaseTexture $ColorDef $WoodDef 0
        Draw-PanelDetails $bottomRight $Animation $null "front" "right" "bottom"
        Save-Bitmap $bottomRight (Join-Path $textureBase "$($Id)_bottom_handle_right.png")
    }

    $top = New-BaseTexture $ColorDef $WoodDef 0
    Draw-PanelDetails $top $Animation $Window "front" $null "top"
    Save-Bitmap $top (Join-Path $textureBase "$($Id)_top.png")

    $side = New-BaseTexture $ColorDef $WoodDef -20
    Draw-PanelDetails $side $Animation $null "side" $null "full"
    Save-Bitmap $side (Join-Path $textureBase "$($Id)_side.png")

    $item = New-BaseTexture $ColorDef $WoodDef 0
    $itemHandle = $null
    if ($Animation -eq "hinged") {
        $itemHandle = "right"
    }
    Draw-PanelDetails $item $Animation $Window "item" $itemHandle "full"
    Save-Bitmap $item (Join-Path $AssetsRoot "textures/item/train/parts/$Id.png")
}

function DoorModel([string] $Id, [string] $Half, [string] $TextureSuffix,
        [int] $FromX = 0, [int] $ToX = 16, [int] $UvMin = 0, [int] $UvMax = 16,
        [bool] $RightHinge = $false) {
    $verticalUv = if ($Half -eq "top") { @(0, 4, 16, 7) } else { @(0, 12, 16, 15) }
    $cap = if ($Half -eq "top") { "up" } else { "down" }
    $capUv = if ($Half -eq "top") { @(0, 0, 16, 3) } else { @(0, 8, 16, 11) }
    $southUv = @($UvMin, 0, $UvMax, 16)
    $northUv = @($UvMax, 0, $UvMin, 16)
    if ($RightHinge) {
        $southUv = @($UvMax, 0, $UvMin, 16)
        $northUv = @($UvMin, 0, $UvMax, 16)
    }
    return [ordered]@{
        credit = "Generated by tools/generate_train_doors.ps1"
        parent = "block/block"
        ambientocclusion = $false
        textures = [ordered]@{
            "0" = "railward:block/train/parts/doors/$($Id)_side"
            "2" = "railward:block/train/parts/doors/$($Id)_$TextureSuffix"
            particle = "railward:block/train/parts/doors/$($Id)_bottom"
        }
        elements = @(
            [ordered]@{
                from = @($FromX, 0, 13)
                to = @($ToX, 16, 16)
                faces = [ordered]@{
                    north = [ordered]@{ uv = $northUv; texture = "#2" }
                    east = [ordered]@{ uv = $verticalUv; rotation = $(if ($Half -eq "top") { 90 } else { 270 }); texture = "#0" }
                    south = [ordered]@{ uv = $southUv; texture = "#2" }
                    west = [ordered]@{ uv = $verticalUv; rotation = $(if ($Half -eq "top") { 90 } else { 270 }); texture = "#0" }
                    $cap = [ordered]@{ uv = $capUv; rotation = 90; texture = "#0" }
                }
            }
        )
    }
}

function DoorBlockState([string] $Id, [string] $Animation) {
    $closedRot = @{ north = $null; south = 180; west = 270; east = 90 }
    $openLeftRot = @{ north = 90; south = 270; west = $null; east = 180 }
    $openRightRot = @{ north = 270; south = 90; west = 180; east = $null }
    $variants = [ordered]@{}

    foreach ($visible in @("false", "true")) {
        foreach ($open in @("false", "true")) {
            foreach ($hinge in @("left", "right")) {
                foreach ($half in @("upper", "lower")) {
                    foreach ($facing in @("north", "south", "west", "east")) {
                        $key = "facing=$facing,half=$half,hinge=$hinge,open=$open,visible=$visible"
                        if ($Animation -eq "hinged") {
                            $modelBase = if ($half -eq "upper") { "block_top" } else { "block_bottom" }
                            $useHingeModel = ($hinge -eq "right" -and $open -eq "false") -or ($hinge -eq "left" -and $open -eq "true")
                            $modelName = if ($useHingeModel) { "$($modelBase)_hinge" } else { $modelBase }
                        } else {
                            $modelName = if ($half -eq "upper") { "block_top" } else { "block_bottom" }
                        }
                        $entry = [ordered]@{ model = "railward:block/train/parts/$Id/$modelName" }
                        if ($open -eq "true") {
                            $rot = if ($hinge -eq "left") { $openLeftRot[$facing] } else { $openRightRot[$facing] }
                        } else {
                            $rot = $closedRot[$facing]
                        }
                        if ($null -ne $rot) {
                            $entry.y = $rot
                        }
                        $variants[$key] = $entry
                    }
                }
            }
        }
    }
    return [ordered]@{ variants = $variants }
}

function ShapelessRecipe([array] $Ingredients, [string] $Result) {
    return [ordered]@{
        type = "minecraft:crafting_shapeless"
        ingredients = $Ingredients
        result = [ordered]@{ item = "railward:train/parts/$Result" }
    }
}

function DoorLoot([string] $Id) {
    return [ordered]@{
        type = "minecraft:block"
        pools = @(
            [ordered]@{
                rolls = 1
                entries = @([ordered]@{ type = "minecraft:item"; name = "railward:train/parts/$Id" })
                conditions = @(
                    [ordered]@{
                        condition = "minecraft:block_state_property"
                        block = "railward:train/parts/$Id"
                        properties = [ordered]@{ half = "lower" }
                    }
                )
            }
        )
    }
}

function Remove-LangDoorEntries([string] $Locale) {
    $path = Join-Path $AssetsRoot "lang/$Locale.json"
    $obj = Get-Content $path -Raw -Encoding UTF8 | ConvertFrom-Json
    $remove = @()
    foreach ($prop in $obj.PSObject.Properties) {
        if ($prop.Name -like "block.railward.train.parts.train_hinged_door_*" -or
                $prop.Name -like "block.railward.train.parts.train_sliding_door_*") {
            $remove += $prop.Name
        }
    }
    foreach ($key in $remove) {
        $obj.PSObject.Properties.Remove($key)
    }
    $json = $obj | ConvertTo-Json -Depth 10
    Write-Text $path ($json + "`n")
}

function Add-LangEntries([string] $Locale, [hashtable] $Entries) {
    $path = Join-Path $AssetsRoot "lang/$Locale.json"
    $obj = Get-Content $path -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($key in ($Entries.Keys | Sort-Object)) {
        $prop = $obj.PSObject.Properties[$key]
        if ($null -eq $prop) {
            $obj | Add-Member -NotePropertyName $key -NotePropertyValue $Entries[$key]
        } else {
            $prop.Value = $Entries[$key]
        }
    }
    $json = $obj | ConvertTo-Json -Depth 10
    Write-Text $path ($json + "`n")
}

Remove-ExistingDoorResources

$DoorIds = New-Object System.Collections.Generic.List[string]
$LangEn = @{}
$LangRu = @{}
$LangUk = @{}

function Register-Door([string] $Id, [string] $Animation, [string] $LabelEn, [string] $LabelRu, [string] $LabelUk,
        [hashtable] $ColorDef, [hashtable] $WoodDef, [string] $Window) {
    $DoorIds.Add($Id)
    Write-DoorTextures $Id $Animation $ColorDef $WoodDef $Window

    Write-Json (Join-Path $AssetsRoot "models/block/train/parts/$Id/block_bottom.json") (DoorModel $Id "bottom" "bottom")
    Write-Json (Join-Path $AssetsRoot "models/block/train/parts/$Id/block_top.json") (DoorModel $Id "top" "top")

    if ($Animation -eq "hinged") {
        Write-Json (Join-Path $AssetsRoot "models/block/train/parts/$Id/block_bottom.json") (DoorModel $Id "bottom" "bottom_handle_right")
        Write-Json (Join-Path $AssetsRoot "models/block/train/parts/$Id/block_bottom_hinge.json") (DoorModel $Id "bottom" "bottom_handle_right" 0 16 0 16 $true)
        Write-Json (Join-Path $AssetsRoot "models/block/train/parts/$Id/block_top_hinge.json") (DoorModel $Id "top" "top" 0 16 0 16 $true)
    } else {
        Write-Json (Join-Path $AssetsRoot "models/block/train/parts/$Id/block_bottom_left.json") (DoorModel $Id "bottom" "bottom" 0 8 0 8)
        Write-Json (Join-Path $AssetsRoot "models/block/train/parts/$Id/block_bottom_right.json") (DoorModel $Id "bottom" "bottom" 8 16 8 16)
        Write-Json (Join-Path $AssetsRoot "models/block/train/parts/$Id/block_top_left.json") (DoorModel $Id "top" "top" 0 8 0 8)
        Write-Json (Join-Path $AssetsRoot "models/block/train/parts/$Id/block_top_right.json") (DoorModel $Id "top" "top" 8 16 8 16)
    }

    Write-Json (Join-Path $AssetsRoot "blockstates/train/parts/$Id.json") (DoorBlockState $Id $Animation)
    Write-Json (Join-Path $AssetsRoot "models/item/train/parts/$Id.json") ([ordered]@{
        parent = "minecraft:item/generated"
        textures = [ordered]@{ layer0 = "railward:item/train/parts/$Id" }
    })
    Write-Json (Join-Path $DataRoot "railward/loot_tables/blocks/train/parts/$Id.json") (DoorLoot $Id)

    $langKey = "block.railward.train.parts.$Id"
    $LangEn[$langKey] = $LabelEn
    $LangRu[$langKey] = $LabelRu
    $LangUk[$langKey] = $LabelUk
}

foreach ($kind in @(
    @{ prefix = "train_hinged_door"; animation = "hinged"; casing = "create:andesite_casing"; door = @{ tag = "minecraft:wooden_doors" }; en = "Hinged Train Door"; ru = "Hinged Train Door"; uk = "Hinged Train Door" },
    @{ prefix = "train_sliding_door"; animation = "sliding"; casing = "create:railway_casing"; door = @{ tag = "minecraft:wooden_doors" }; en = "Sliding Train Door"; ru = "Sliding Train Door"; uk = "Sliding Train Door" }
)) {
    foreach ($color in $Colors) {
        $id = "$($kind.prefix)_$($color.id)"
        Register-Door $id $kind.animation "$($kind.en) ($($color.en))" "$($kind.ru) ($($color.ru))" "$($kind.uk) ($($color.uk))" $color $null $null
        Write-Json (Join-Path $DataRoot "railward/recipes/$id.json") (ShapelessRecipe @(
            $kind.door,
            @{ item = $kind.casing },
            @{ tag = "forge:dyes/$($color.dye)" }
        ) $id)
    }

    foreach ($wood in $Woods) {
        $id = "$($kind.prefix)_$($wood.id)"
        Register-Door $id $kind.animation "$($kind.en) ($($wood.en))" "$($kind.ru) ($($wood.ru))" "$($kind.uk) ($($wood.uk))" $null $wood $null
        Write-Json (Join-Path $DataRoot "railward/recipes/$id.json") (ShapelessRecipe @(
            $kind.door,
            @{ item = $kind.casing },
            @{ item = "minecraft:$($wood.item)" }
        ) $id)
    }
}

$ColorById = @{}
foreach ($color in $Colors) {
    $ColorById[$color.id] = $color
}

foreach ($kind in @(
    @{ prefix = "train_hinged_door"; animation = "hinged"; inputDoor = @{ tag = "minecraft:wooden_doors" }; en = "Hinged Train Door"; ru = "Hinged Train Door"; uk = "Hinged Train Door" },
    @{ prefix = "train_sliding_door"; animation = "sliding"; inputDoor = @{ item = "minecraft:iron_door" }; en = "Sliding Train Door"; ru = "Sliding Train Door"; uk = "Sliding Train Door" }
)) {
    foreach ($colorId in $WindowColors) {
        $color = $ColorById[$colorId]
        foreach ($window in @("small", "big")) {
            $id = "$($kind.prefix)_$($color.id)_window_$window"
            $windowEn = if ($window -eq "small") { "Small Window" } else { "Big Window" }
            Register-Door $id $kind.animation "$($kind.en) ($($color.en), $windowEn)" "$($kind.ru) ($($color.ru), $windowEn)" "$($kind.uk) ($($color.uk), $windowEn)" $color $null $window
            Write-Json (Join-Path $DataRoot "railward/recipes/$id.json") (ShapelessRecipe @(
                $kind.inputDoor,
                @{ tag = "railward:train_wall_variants/$($color.id)_window_$window" }
            ) $id)
        }
    }
}

foreach ($locale in @("en_us", "ru_ru", "uk_ua")) {
    Remove-LangDoorEntries $locale
}
Add-LangEntries "en_us" $LangEn
Add-LangEntries "ru_ru" $LangRu
Add-LangEntries "uk_ua" $LangUk

$doorValues = @($DoorIds | ForEach-Object { "railward:train/parts/$_" })

Write-Json (Join-Path $DataRoot "minecraft/tags/blocks/doors.json") ([ordered]@{ replace = $false; values = $doorValues })
Write-Json (Join-Path $DataRoot "minecraft/tags/blocks/wooden_doors.json") ([ordered]@{ replace = $false; values = $doorValues })
Write-Json (Join-Path $DataRoot "minecraft/tags/blocks/mineable/pickaxe.json") ([ordered]@{ replace = $false; values = $doorValues })
Write-Json (Join-Path $DataRoot "minecraft/tags/items/doors.json") ([ordered]@{ replace = $false; values = $doorValues })
Write-Json (Join-Path $DataRoot "create/tags/blocks/non_double_door.json") ([ordered]@{ replace = $false; values = $doorValues })
$contraptionControlledValues = @("railward:train/station/unloading_hatch") + $doorValues
Write-Json (Join-Path $DataRoot "create/tags/items/contraption_controlled.json") ([ordered]@{ replace = $false; values = $contraptionControlledValues })

Write-Host "Generated $($DoorIds.Count) train doors."

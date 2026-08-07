param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"

& (Join-Path $PSScriptRoot "generate_bogey_18_100.ps1") `
    -Root $Root `
    -Variant triple_axle_equalized_locomotive

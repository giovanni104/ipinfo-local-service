param(
    [Parameter(Mandatory=$true)]
    [string]$Token,
    [string]$Output = ".\data\ipinfo_lite.mmdb"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path (Split-Path $Output) | Out-Null
$temporary = "$Output.download"
$url = "https://ipinfo.io/data/ipinfo_lite.mmdb?token=$([uri]::EscapeDataString($Token))"

Invoke-WebRequest -Uri $url -OutFile $temporary
if ((Get-Item $temporary).Length -lt 1000000) {
    Remove-Item $temporary -Force
    throw "El archivo descargado es demasiado pequeño y no parece una base válida."
}
Move-Item -Path $temporary -Destination $Output -Force
Write-Host "Base descargada en $Output"

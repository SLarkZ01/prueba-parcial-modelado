param(
    [string]$ApiBaseUrl = "http://localhost:8080",
    [string]$OutputDir = "docs/openapi",
    [int]$TimeoutSeconds = 120,
    [switch]$NoStartApp
)

$ErrorActionPreference = "Stop"

function Test-ApiReady {
    param([string]$Url)

    try {
        $response = Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing -TimeoutSec 5
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 300
    } catch {
        return $false
    }
}

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $projectRoot

$mavenCommand = if ($IsWindows) { ".\\mvnw.cmd" } else { "./mvnw" }

$apiDocsJsonUrl = "$ApiBaseUrl/v3/api-docs"
$apiDocsYamlUrl = "$ApiBaseUrl/v3/api-docs.yaml"

$absoluteOutputDir = Join-Path $projectRoot $OutputDir
New-Item -ItemType Directory -Force -Path $absoluteOutputDir | Out-Null

$jsonOutputPath = Join-Path $absoluteOutputDir "openapi.json"
$yamlOutputPath = Join-Path $absoluteOutputDir "openapi.yaml"

$startedProcess = $null

if (-not (Test-ApiReady -Url $apiDocsJsonUrl)) {
    if ($NoStartApp) {
        throw "La aplicacion no esta disponible en $ApiBaseUrl. Inicia Spring Boot o ejecuta el script sin -NoStartApp."
    }

    Write-Host "Iniciando Spring Boot para exportar OpenAPI..."

    $targetDir = Join-Path $projectRoot "target"
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

    $stdoutLog = Join-Path $targetDir "openapi-export.out.log"
    $stderrLog = Join-Path $targetDir "openapi-export.err.log"

    $startedProcess = Start-Process -FilePath $mavenCommand -ArgumentList "spring-boot:run" -WorkingDirectory $projectRoot -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog -PassThru

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while (-not (Test-ApiReady -Url $apiDocsJsonUrl)) {
        if ($startedProcess.HasExited) {
            throw "Spring Boot se detuvo antes de exponer OpenAPI. Revisa: $stdoutLog y $stderrLog"
        }

        if ((Get-Date) -gt $deadline) {
            throw "Timeout esperando OpenAPI en $apiDocsJsonUrl"
        }

        Start-Sleep -Seconds 2
    }
}

try {
    Write-Host "Exportando OpenAPI JSON..."
    Invoke-WebRequest -Uri $apiDocsJsonUrl -OutFile $jsonOutputPath -UseBasicParsing

    Write-Host "Exportando OpenAPI YAML..."
    Invoke-WebRequest -Uri $apiDocsYamlUrl -OutFile $yamlOutputPath -UseBasicParsing

    Write-Host "Archivos generados correctamente:"
    Write-Host " - $jsonOutputPath"
    Write-Host " - $yamlOutputPath"
} finally {
    if ($null -ne $startedProcess -and -not $startedProcess.HasExited) {
        Write-Host "Deteniendo proceso temporal de Spring Boot..."
        Stop-Process -Id $startedProcess.Id
    }
}

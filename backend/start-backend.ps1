param(
  [int]$Port = 8080,
  [string]$MavenPath = ""
)

Set-Location -Path $PSScriptRoot

function Resolve-MavenCommand {
  param([string]$InputPath)

  if ($InputPath -and (Test-Path $InputPath)) {
    return $InputPath
  }

  $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
  if ($mvnCmd) {
    return "mvn"
  }

  $candidatePaths = @()
  if ($env:MAVEN_HOME) {
    $candidatePaths += (Join-Path $env:MAVEN_HOME "bin\\mvn.cmd")
  }
  if ($env:M2_HOME) {
    $candidatePaths += (Join-Path $env:M2_HOME "bin\\mvn.cmd")
  }
  $candidatePaths += "D:\\development\\Java\\maven\\apache-maven-3.9.12\\bin\\mvn.cmd"
  $candidatePaths += "C:\\apache-maven\\bin\\mvn.cmd"

  foreach ($candidate in $candidatePaths) {
    if (Test-Path $candidate) {
      return $candidate
    }
  }

  throw "Maven not found. Configure PATH or pass -MavenPath with absolute mvn.cmd path."
}

function Stop-WikiProcessOnPort {
  param([int]$TargetPort)

  $conn = Get-NetTCPConnection -LocalPort $TargetPort -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
  if (-not $conn) {
    return
  }

  $procId = $conn.OwningProcess
  $proc = Get-CimInstance Win32_Process -Filter "ProcessId = $procId" -ErrorAction SilentlyContinue
  $cmd = if ($proc) { $proc.CommandLine } else { "" }

  if ($cmd -match "com\.wiki\.app\.WikiBackendApplication") {
    Write-Host "Port $TargetPort is occupied by an old Wiki backend process. Stopping PID=$procId ..."
    Stop-Process -Id $procId -Force
    Start-Sleep -Seconds 1
    return
  }

  $procName = if ($proc) { $proc.Name } else { "unknown" }
  throw "Port $TargetPort is occupied by another process (PID=$procId, Name=$procName). Free the port or set SERVER_PORT."
}

$mvn = Resolve-MavenCommand -InputPath $MavenPath

if ([string]::IsNullOrWhiteSpace($env:SERVER_PORT)) {
  $env:SERVER_PORT = "$Port"
}

Stop-WikiProcessOnPort -TargetPort ([int]$env:SERVER_PORT)

Write-Host "Config: SERVER_PORT=$($env:SERVER_PORT)"
Write-Host "Maven: $mvn"

& $mvn "spring-boot:run"

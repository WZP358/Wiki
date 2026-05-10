$ErrorActionPreference = 'Stop'

$base = $env:WIKI_API_BASE_URL
if (-not $base) {
  $base = 'http://localhost:8080/api'
}

$script:smokeIp = '10.252.' + (Get-Random -Minimum 1 -Maximum 200) + '.' + (Get-Random -Minimum 1 -Maximum 250)

function Read-ErrorBody($err) {
  try {
    $stream = $err.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    return $reader.ReadToEnd()
  } catch {
    return ''
  }
}

function Invoke-JsonPost($name, $url, $body, $token = $null) {
  $headers = @{ 'X-Forwarded-For' = $smokeIp }
  if ($token) {
    $headers.Authorization = "Bearer $token"
  }
  try {
    $res = Invoke-WebRequest -UseBasicParsing -Method Post -Uri $url -ContentType 'application/json' -Headers $headers -Body ($body | ConvertTo-Json -Depth 10)
    Write-Host "OK: $name"
    return ($res.Content | ConvertFrom-Json)
  } catch {
    Write-Host "FAIL: $name"
    Write-Host $_.Exception.Message
    Write-Host (Read-ErrorBody $_)
    throw
  }
}

function Invoke-JsonGet($name, $url, $token = $null) {
  $headers = @{ 'X-Forwarded-For' = $smokeIp }
  if ($token) {
    $headers.Authorization = "Bearer $token"
  }
  try {
    $res = Invoke-WebRequest -UseBasicParsing -Method Get -Uri $url -Headers $headers
    Write-Host "OK: $name"
    return ($res.Content | ConvertFrom-Json)
  } catch {
    Write-Host "FAIL: $name"
    Write-Host $_.Exception.Message
    Write-Host (Read-ErrorBody $_)
    throw
  }
}

function Invoke-ExpectedFailure($name, $script, $expectedCode) {
  try {
    & $script | Out-Null
    throw "$name unexpectedly passed"
  } catch {
    $body = Read-ErrorBody $_
    if ($body -and $body.Contains("`"code`":`"$expectedCode`"")) {
      Write-Host "OK: $name rejected with $expectedCode"
      return
    }
    if ($_.Exception.Message -like '*unexpectedly passed*') {
      throw
    }
    Write-Host "FAIL: $name expected $expectedCode"
    Write-Host $_.Exception.Message
    Write-Host $body
    throw
  }
}

function Invoke-ExpectedJsonPostFailure($name, $url, $body, $token, $expectedCode) {
  $headers = @{ 'X-Forwarded-For' = $smokeIp }
  if ($token) {
    $headers.Authorization = "Bearer $token"
  }
  try {
    Invoke-WebRequest -UseBasicParsing -Method Post -Uri $url -ContentType 'application/json' -Headers $headers -Body ($body | ConvertTo-Json -Depth 10) | Out-Null
    throw "$name unexpectedly passed"
  } catch {
    $bodyText = Read-ErrorBody $_
    if ($bodyText -and $bodyText.Contains("`"code`":`"$expectedCode`"")) {
      Write-Host "OK: $name rejected with $expectedCode"
      return
    }
    if ($_.Exception.Message -like '*unexpectedly passed*') {
      throw
    }
    Write-Host "FAIL: $name expected $expectedCode"
    Write-Host $_.Exception.Message
    Write-Host $bodyText
    throw
  }
}

Invoke-JsonPost 'admin login' "$base/auth/login" @{ account = 'admin'; password = 'Admin@123456' } | Out-Null
try {
  Invoke-JsonPost 'wrong password' "$base/auth/login" @{ account = 'admin'; password = 'wrong-password' } | Out-Null
  throw 'wrong password unexpectedly passed'
} catch {
  Write-Host 'OK: wrong password rejected'
}

$stamp = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$username = "smoke$stamp"
$email = "$username@example.com"
$codeResp = Invoke-JsonPost 'send code' "$base/auth/send-code" @{ target = $email }
$reg = Invoke-JsonPost 'register' "$base/auth/register" @{ username = $username; password = 'Passw0rd!'; email = $email; code = $codeResp.data.code }
$userToken = $reg.data.token

Invoke-JsonGet 'auth me' "$base/auth/me" $userToken | Out-Null
$kb = Invoke-JsonPost 'create private kb' "$base/kbs" @{ name = "Smoke KB $stamp"; type = 'PRIVATE'; description = 'smoke' } $userToken
$kbId = $kb.data.id
$doc = Invoke-JsonPost 'create doc' "$base/docs" @{ kbId = $kbId; title = 'Smoke Doc'; markdownContent = '# Hello'; visibility = 'PUBLIC'; published = $true } $userToken
$docId = $doc.data.id

Invoke-JsonGet 'doc tree' "$base/docs/tree?kbId=$kbId" $userToken | Out-Null
Invoke-JsonPost 'comment' "$base/docs/$docId/comments" @{ content = 'smoke comment' } $userToken | Out-Null
Invoke-JsonPost 'favorite' "$base/favorites/docs/$docId" @{} $userToken | Out-Null
$share = Invoke-JsonPost 'share' "$base/shares/docs/$docId" @{} $userToken
Invoke-JsonGet 'public share view' "$base/shares/public/$($share.data.token)" | Out-Null

$privateDoc = Invoke-JsonPost 'create private doc' "$base/docs" @{ kbId = $kbId; title = 'Private Smoke Doc'; markdownContent = '# Secret'; visibility = 'PRIVATE'; published = $false } $userToken
$privateDocId = $privateDoc.data.id

$otherUsername = "smokeOther$stamp"
$otherEmail = "$otherUsername@example.com"
$script:smokeIp = '10.251.' + (Get-Random -Minimum 1 -Maximum 200) + '.' + (Get-Random -Minimum 1 -Maximum 250)
$otherCodeResp = Invoke-JsonPost 'send code other user' "$base/auth/send-code" @{ target = $otherEmail }
$otherReg = Invoke-JsonPost 'register other user' "$base/auth/register" @{ username = $otherUsername; password = 'Passw0rd!'; email = $otherEmail; code = $otherCodeResp.data.code }
$otherToken = $otherReg.data.token

Invoke-ExpectedJsonPostFailure 'other user cannot comment private doc' "$base/docs/$privateDocId/comments" @{ content = 'should fail' } $otherToken 'FORBIDDEN'
Invoke-ExpectedJsonPostFailure 'other user cannot favorite private doc' "$base/favorites/docs/$privateDocId" @{} $otherToken 'FORBIDDEN'

Write-Host "API smoke completed with user=$username ip=$smokeIp kb=$kbId doc=$docId"

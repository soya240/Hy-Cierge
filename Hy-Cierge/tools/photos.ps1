$ErrorActionPreference = 'Continue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root 'app\src\main\res\drawable-nodpi'
New-Item -ItemType Directory -Force $dir | Out-Null
$ua = 'HyCierge/1.0 (Pleos Playground contest demo app)'
$list = [ordered]@{
  'gwangjang' = 'Gwangjang_Market'
  'insadong' = 'Insa-dong'
  'gyeongbokgung' = 'Gyeongbokgung'
  'myeongdong' = 'Myeong-dong'
  'cheonggyecheon' = 'Cheonggyecheon'
  'jongmyo' = 'Jongmyo'
  'jogyesa' = 'Jogyesa'
  'ssamziegil' = 'Ssamziegil'
  'palacemuseum' = 'National_Palace_Museum_of_Korea'
  'seochon' = 'Seochon'
  'cathedral' = 'Myeongdong_Cathedral'
  'nseoultower' = 'N_Seoul_Tower'
  'bindaetteok' = 'Bindae-tteok'
  'gimbap' = 'Gimbap'
  'yukhoe' = 'Yukhoe'
  'gwanghwamun' = 'Gwanghwamun'
  'bosingak' = 'Bosingak'
  'sungnyemun' = 'Sungnyemun'
  'deoksugung' = 'Deoksugung'
}
foreach ($k in $list.Keys) {
  try {
    $j = Invoke-RestMethod -Uri ('https://en.wikipedia.org/api/rest_v1/page/summary/' + $list[$k]) -UserAgent $ua
    $src = $j.thumbnail.source
    if (-not $src) { Write-Host "skip $k (no photo)"; continue }
    $big = $src -replace '/\d+px-', '/960px-'
    $out = Join-Path $dir ('img_' + $k + '.jpg')
    try { Invoke-WebRequest -Uri $big -UserAgent $ua -OutFile $out -UseBasicParsing } catch { Invoke-WebRequest -Uri $src -UserAgent $ua -OutFile $out -UseBasicParsing }
    $b = [System.IO.File]::ReadAllBytes($out)
    if (-not ($b[0] -eq 0xFF -and $b[1] -eq 0xD8)) { Remove-Item $out; Write-Host "skip $k (not jpg)"; continue }
    Write-Host "OK   $k"
    Start-Sleep -Milliseconds 300
  } catch { Write-Host "FAIL $k : $_" }
}
Write-Host ''
Write-Host 'Done. Photos saved to app\src\main\res\drawable-nodpi (img_*.jpg). Photos: Wikimedia Commons (free licenses).'

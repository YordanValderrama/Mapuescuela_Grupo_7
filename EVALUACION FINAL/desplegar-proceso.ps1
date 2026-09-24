$ErrorActionPreference = 'Stop'
$url = 'http://localhost:8080/flowable-rest/service'
$usuario = if ($env:FLOWABLE_REST_USERNAME) { $env:FLOWABLE_REST_USERNAME } else { 'rest-admin' }
$clave = if ($env:FLOWABLE_REST_PASSWORD) { $env:FLOWABLE_REST_PASSWORD } else { 'test' }
$bpmn = Join-Path $PSScriptRoot 'proceso-venta-open-source.bpmn20.xml'

Write-Host 'Esperando que Flowable Open Source esté listo...'
$listo = $false
for ($intento = 0; $intento -lt 45; $intento++) {
    & curl.exe --fail --silent --show-error --output NUL --user "${usuario}:${clave}" "$url/management/engine" 2>$null
    if ($LASTEXITCODE -eq 0) { $listo = $true; break }
    Start-Sleep -Seconds 3
}
if (-not $listo) { throw 'El motor REST no respondió. Revise docker compose logs flowable.' }

$resultado = & curl.exe --fail --silent --show-error --user "${usuario}:${clave}" `
    --form "file=@$bpmn" "$url/repository/deployments"
if ($LASTEXITCODE -ne 0) { throw 'Falló el despliegue del BPMN.' }
$despliegue = $resultado | ConvertFrom-Json
Write-Host "Despliegue creado: $($despliegue.id)"

$definiciones = & curl.exe --fail --silent --show-error --user "${usuario}:${clave}" `
    "$url/repository/process-definitions?key=procesoDeVentaMauescuelaTOBE"
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar la definición desplegada.' }
$respuesta = $definiciones | ConvertFrom-Json
if (-not $respuesta.data -or $respuesta.data.Count -lt 1) {
    throw 'El despliegue terminó, pero el motor no devolvió la definición esperada.'
}
Write-Host 'BPMN disponible para recibir pedidos desde Mapuescuela.'

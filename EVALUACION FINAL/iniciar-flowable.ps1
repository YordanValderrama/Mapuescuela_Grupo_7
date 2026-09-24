$ErrorActionPreference = 'Stop'
$war = Join-Path $PSScriptRoot 'flowable-rest.war'
if (-not (Test-Path -LiteralPath $war -PathType Leaf)) {
    throw 'Falta flowable-rest.war. Copia el archivo de la carpeta wars de Flowable Open Source 7.1.0 junto a este script.'
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Instala Java 17 o 21 antes de iniciar Flowable.' }

$db = Join-Path $PSScriptRoot 'data-flowable'
[IO.Directory]::CreateDirectory($db) | Out-Null
$dbPath = ($db -replace '\\','/') + '/ossdb'
$jdbc = 'jdbc:h2:file:' + $dbPath + ';DB_CLOSE_DELAY=-1'
$usuario = if ($env:FLOWABLE_REST_USERNAME) { $env:FLOWABLE_REST_USERNAME } else { 'rest-admin' }
$clave = if ($env:FLOWABLE_REST_PASSWORD) { $env:FLOWABLE_REST_PASSWORD } else { 'test' }

Write-Host 'Iniciando Flowable REST local. Mantén abierta esta ventana.'
Write-Host 'Mapuescuela conserva SQLite; Flowable guarda su historial en data-flowable.'
& java -jar $war '--server.address=127.0.0.1' '--server.port=8080' `
    "--spring.datasource.url=$jdbc" '--spring.datasource.username=flowable' `
    '--spring.datasource.password=flowable' '--flowable.rest.app.create-demo-definitions=false' `
    "--flowable.rest.app.admin.user-id=$usuario" "--flowable.rest.app.admin.password=$clave"

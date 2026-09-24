
$ErrorActionPreference = 'Stop'
$backend = 'http://localhost:8081'
$motor = 'http://localhost:8080/flowable-rest/service'
$usuario = if ($env:FLOWABLE_REST_USERNAME) { $env:FLOWABLE_REST_USERNAME } else { 'rest-admin' }
$clave = if ($env:FLOWABLE_REST_PASSWORD) { $env:FLOWABLE_REST_PASSWORD } else { 'test' }

function Publicar-Json($ruta, $datos) {
    Invoke-RestMethod -Uri "$backend$ruta" -Method Post -ContentType 'application/json' `
        -Body ($datos | ConvertTo-Json -Depth 6 -Compress)
}

function Esperar-Etapa($id, $esperada) {
    for ($i = 0; $i -lt 90; $i++) {
        try {
            $actual = Invoke-RestMethod "$backend/pedidos/$id/etapa-flowable"
            if ($actual.etapa -eq $esperada) { return }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw "El pedido $id no llegó a la tarea $esperada. Revise los logs del backend y de Flowable."
}

function Esperar-Stock($nombre, $esperado) {
    for ($i = 0; $i -lt 90; $i++) {
        $actual = Invoke-RestMethod "$backend/inventario/$([uri]::EscapeDataString($nombre))"
        if ($actual.stockDisponible -eq $esperado) { return }
        Start-Sleep -Seconds 2
    }
    throw "El stock de '$nombre' no llegó a $esperado."
}

function Esperar-Fin($instancia) {
    for ($i = 0; $i -lt 90; $i++) {
        try {
            $json = & curl.exe --fail --silent --show-error --user "${usuario}:${clave}" `
                "$motor/history/historic-process-instances/$instancia" 2>$null
            if ($LASTEXITCODE -eq 0) {
                $dato = $json | ConvertFrom-Json
                if ($dato.endTime) { return }
            }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw "La instancia $instancia no terminó. Revise los jobs del motor."
}

Write-Host 'Verificando motor y backend...'
& curl.exe --fail --silent --show-error --output NUL --user "${usuario}:${clave}" "$motor/management/engine"
if ($LASTEXITCODE -ne 0) { throw 'Flowable REST no está disponible o las credenciales no coinciden.' }
$null = Invoke-RestMethod "$backend/inventario/productos"

$png = Join-Path $env:TEMP ("mapuescuela-prueba-" + [guid]::NewGuid().ToString('N') + '.png')
[IO.File]::WriteAllBytes($png, [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9xI5kAAAAASUVORK5CYII='))

try {
    foreach ($modalidad in @('retiro_tienda', 'reparto_domicilio')) {
        $producto = "Prueba Flowable $modalidad $([guid]::NewGuid().ToString('N').Substring(0,8))"
        $null = Publicar-Json '/inventario/productos' @{
            producto=$producto; stockDisponible=10; precio=1000; descripcion='Prueba automatica'
        }
        $pedido = Publicar-Json '/pedidos' @{
            nombreCliente='Cliente de prueba'; correoCliente='prueba@ejemplo.cl'
            rutCliente='12345678-9'; telefonoCliente='912345678'; producto=$producto
            cantidad=1; modalidadEntrega=$modalidad; direccion='Calle de prueba 1'
            region='Metropolitana'; comuna='Maipu'
        }
        $id = $pedido.idPedido
        if (-not $id) { throw 'El backend no devolvió un número de pedido.' }
        $guardado = Invoke-RestMethod "$backend/pedidos/$id"
        if (-not $guardado.processInstanceId) { throw "El pedido $id no inició un proceso Flowable." }

        Esperar-Etapa $id 'ut_cargar_comprobante'
        $carga = & curl.exe --fail --silent --show-error `
            --form "archivo=@$png;type=image/png" "$backend/pedidos/$id/comprobante"
        if ($LASTEXITCODE -ne 0) { throw "No se pudo subir el comprobante para $id." }
        if (($carga | ConvertFrom-Json).estadoPago -ne 'EN_REVISION') { throw 'El comprobante no quedó en revisión.' }
        Esperar-Etapa $id 'ut_revisar_comprobante'

        $null = Publicar-Json "/pedidos/$id/revision" @{ decision='pago_aprobado'; observaciones='' }
        Esperar-Stock $producto 9
        if ((Invoke-RestMethod "$backend/pedidos/$id").estadoPago -ne 'APROBADO') {
            throw "El pago del pedido $id no quedó aprobado."
        }

        if ($modalidad -eq 'retiro_tienda') {
            Esperar-Etapa $id 'ut_registrar_retiro'
            $null = Publicar-Json "/pedidos/$id/entrega" @{
                tipo='RETIRO'; voluntario='Voluntario de prueba'; rutVoluntario='12345678-9'
                observaciones='Entrega de prueba'
            }
        } else {
            Esperar-Etapa $id 'registraInformacionDespacho'
            $null = Publicar-Json "/pedidos/$id/entrega" @{
                tipo='DESPACHO'; empresa='Transportista de prueba'; numeroSeguimiento='PRUEBA-001'
            }
        }
        $null = Publicar-Json "/pedidos/$id/entrega/completar" @{}
        if (-not (Invoke-RestMethod "$backend/pedidos/$id/entrega")) { throw "No se guardó la entrega $id." }
        Esperar-Fin $guardado.processInstanceId
        Write-Host "OK: $modalidad | $id | pago aprobado | stock 10 a 9 | proceso terminado"
    }
    Write-Host 'PRUEBA COMPLETA: ambas modalidades funcionaron contra Flowable Open Source.'
} finally {
    if (Test-Path $png) { Remove-Item $png }
}

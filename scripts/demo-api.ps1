$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

if (-not $env:THINDESK_USER_USERNAME -or -not $env:THINDESK_USER_PASSWORD -or -not $env:THINDESK_ADMIN_PASSWORD) {
    throw 'Defina THINDESK_USER_USERNAME, THINDESK_USER_PASSWORD e THINDESK_ADMIN_PASSWORD.'
}

$baseUrl = 'http://127.0.0.1:8080'
$client = [System.Net.Http.HttpClient]::new()

function Get-BasicToken([string]$name, [string]$password) {
    return [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${name}:${password}"))
}

function Send-Call([string]$method, [string]$path, [string]$token, [string]$json = '') {
    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($method), "$baseUrl$path")
    if ($token) {
        $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Basic', $token)
    }
    if ($json) {
        $request.Content = [System.Net.Http.StringContent]::new($json, [Text.Encoding]::UTF8, 'application/json')
    }
    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    return [pscustomobject]@{
        Status = [int]$response.StatusCode
        Body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    }
}

$solicitante = Get-BasicToken $env:THINDESK_USER_USERNAME $env:THINDESK_USER_PASSWORD
$tecnico = Get-BasicToken 'admin' $env:THINDESK_ADMIN_PASSWORD
$abertura = Send-Call 'POST' '/api/chamados' $solicitante '{"titulo":"Impressora sem rede","descricao":"Nao responde ao ping"}'
if ($abertura.Status -ne 201) { throw "Abertura falhou: HTTP $($abertura.Status) $($abertura.Body)" }
$chamado = $abertura.Body | ConvertFrom-Json
$id = $chamado.id
Set-Content -LiteralPath (Join-Path $env:TEMP 'thindesk-demo-id.txt') -Value $id
$lista = Send-Call 'GET' "/api/chamados/usuario/$($env:THINDESK_USER_USERNAME)" $solicitante
$consulta = Send-Call 'GET' "/api/chamados/$id" $solicitante
$semPermissao = Send-Call 'PUT' "/api/chamados/$id/resolver" $solicitante '{"solucao":"Cabo substituido"}'
$resolucao = Send-Call 'PUT' "/api/chamados/$id/resolver" $tecnico '{"solucao":"Cabo substituido"}'
$repeticao = Send-Call 'PUT' "/api/chamados/$id/resolver" $tecnico '{"solucao":"Outra solucao"}'
$resolvido = $resolucao.Body | ConvertFrom-Json

# O id permite conferir o mesmo documento diretamente no MongoDB após o roteiro.
[pscustomobject]@{
    Id = $id
    Solicitante = $chamado.usuario
    Abertura = $abertura.Status
    StatusInicial = $chamado.status
    DataAbertura = $chamado.dataAbertura
    Lista = $lista.Status
    Consulta = $consulta.Status
    ResolucaoComoUsuario = $semPermissao.Status
    ResolucaoComoTecnico = $resolucao.Status
    StatusFinal = $resolvido.status
    DataResolucao = $resolvido.dataResolucao
    NovaResolucao = $repeticao.Status
} | Format-List

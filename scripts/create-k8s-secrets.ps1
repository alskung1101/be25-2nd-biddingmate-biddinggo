param(
    [string]$Namespace = "biddinggo",
    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"

$env:HTTP_PROXY = ""
$env:HTTPS_PROXY = ""
$env:ALL_PROXY = ""
$env:http_proxy = ""
$env:https_proxy = ""
$env:all_proxy = ""
$env:NO_PROXY = "localhost,127.0.0.1,kubernetes.docker.internal,.docker.internal"
$env:no_proxy = $env:NO_PROXY

if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "Env file not found: $EnvFile"
}

$vars = @{}
Get-Content -LiteralPath $EnvFile | ForEach-Object {
    if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)$') {
        $vars[$matches[1]] = $matches[2]
    }
}

foreach ($key in @("DB_USERNAME", "DB_PASSWORD", "REDIS_PASSWORD")) {
    if (-not $vars.ContainsKey($key)) {
        throw "Required key missing in ${EnvFile}: $key"
    }
}

kubectl apply -f k8s/base/namespace.yaml | Out-Host

$rootPassword = $vars["DB_PASSWORD"]
$mariadbRoot = "MARIADB_ROOT_PASSWORD=$rootPassword"
$mariadbUser = "MARIADB_USER=$($vars['DB_USERNAME'])"
$mariadbPassword = "MARIADB_PASSWORD=$($vars['DB_PASSWORD'])"

kubectl create secret generic biddinggo-mariadb-secret `
    -n $Namespace `
    --from-literal=$mariadbRoot `
    --from-literal=$mariadbUser `
    --from-literal=$mariadbPassword `
    --dry-run=client `
    -o yaml | kubectl apply -f - | Out-Host

kubectl create secret generic biddinggo-redis-secret `
    -n $Namespace `
    --from-env-file=$EnvFile `
    --dry-run=client `
    -o yaml | kubectl apply -f - | Out-Host

kubectl create secret generic biddinggo-backend-secret `
    -n $Namespace `
    --from-env-file=$EnvFile `
    --dry-run=client `
    -o yaml | kubectl apply -f - | Out-Host

Write-Host "Kubernetes secrets are ready in namespace '$Namespace'."

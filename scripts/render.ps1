param(
    [string]$action = "list",
    [string]$serviceId = "",
    [string]$envKey = "",
    [string]$envVal = ""
)

$token = $env:RENDER_API_KEY
if (-not $token) {
    if (Test-Path "$PSScriptRoot\..\.env") {
        $envLines = Get-Content "$PSScriptRoot\..\.env"
        foreach ($line in $envLines) {
            if ($line -match "^RENDER_API_KEY=(.+)$") {
                $token = $matches[1].Trim()
                break
            }
        }
    }
}

if (-not $token) {
    Write-Error "RENDER_API_KEY not found. Please set RENDER_API_KEY in environment or .env file."
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Accept"        = "application/json"
}

switch ($action.ToLower()) {
    "list" {
        Write-Host "=== Render Services List ===" -ForegroundColor Cyan
        $res = Invoke-RestMethod -Uri "https://api.render.com/v1/services?limit=20" -Headers $headers
        $res | ForEach-Object {
            $s = $_.service
            Write-Host "ID: $($s.id) | Name: $($s.name) | Type: $($s.type) | Updated: $($s.updatedAt)" -ForegroundColor Green
        }
    }
    "deploy" {
        if (-not $serviceId) {
            Write-Error "Usage: .\scripts\render.ps1 deploy <serviceId>"
            exit 1
        }
        Write-Host "Triggering deploy for service $serviceId..." -ForegroundColor Yellow
        $body = @{ clearCache = "do_not_clear" } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri "https://api.render.com/v1/services/$serviceId/deploys" -Method POST -Headers $headers -Body $body -ContentType "application/json"
        Write-Host "Deploy triggered successfully!" -ForegroundColor Green
        Write-Host "Deploy ID: $($res.id) | Status: $($res.status)" -ForegroundColor Cyan
    }
    "deploys" {
        if (-not $serviceId) {
            Write-Error "Usage: .\scripts\render.ps1 deploys <serviceId>"
            exit 1
        }
        Write-Host "=== Recent Deploys for $serviceId ===" -ForegroundColor Cyan
        $res = Invoke-RestMethod -Uri "https://api.render.com/v1/services/$serviceId/deploys?limit=5" -Headers $headers
        $res | ForEach-Object {
            $d = $_.deploy
            Write-Host "ID: $($d.id) | Status: $($d.status) | Created: $($d.createdAt) | Commit: $($d.commit.id)" -ForegroundColor Green
        }
    }
    default {
        Write-Host "Unknown action: $action. Available actions: list, deploy <serviceId>, deploys <serviceId>" -ForegroundColor Red
    }
}

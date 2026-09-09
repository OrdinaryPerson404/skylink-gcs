# SkyLink GCS - allow inbound TCP 8080 (GNSS) and 8135 (tiles) ONLY from the drone AP subnet.
# Requires Administrator. Run: right-click -> Run with PowerShell, accept UAC.
# Idempotent: deletes stale rules first, then (re)creates scoped rules.
$ErrorActionPreference = 'Stop'

$DRONE_SUBNET = '192.168.4.0/24'

function Set-ScopedAllowRule($name, $port) {
    netsh advfirewall firewall delete rule name="$name" > $null 2>&1
    netsh advfirewall firewall add rule name="$name" dir=in action=allow protocol=TCP localport=$port remoteip=$DRONE_SUBNET profile=any
    if ($LASTEXITCODE -eq 0) {
        Write-Host ("[ok] " + $name + "  (TCP " + $port + "  <-  only " + $DRONE_SUBNET + ")")
    } else {
        Write-Error ("FAILED: " + $name)
    }
}

if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "WARNING: not running as Administrator; adding rules may fail. Right-click -> Run with PowerShell."
}

# remove any stale wide-open rules to avoid leftover permissive config
netsh advfirewall firewall delete rule name="SkyLinkGCS-TCP-8080-GNSS" > $null 2>&1
netsh advfirewall firewall delete rule name="SkyLinkGCS-TCP-8135-Tiles" > $null 2>&1
netsh advfirewall firewall delete rule name="SkyLinkGCS 8080" > $null 2>&1
netsh advfirewall firewall delete rule name="SkyLinkGCS 8135" > $null 2>&1

Set-ScopedAllowRule "SkyLinkGCS-TCP-8080-GNSS" 8080
Set-ScopedAllowRule "SkyLinkGCS-TCP-8135-Tiles" 8135

Write-Host "Done."
Write-Host "Verify:  netsh advfirewall firewall show rule name=SkyLinkGCS-TCP-8080-GNSS"
Write-Host "Remove:  netsh advfirewall firewall delete rule name=SkyLinkGCS-TCP-8080-GNSS"
Write-Host "         netsh advfirewall firewall delete rule name=SkyLinkGCS-TCP-8135-Tiles"
$BaseUrl = "http://localhost:8080/api"
$Headers = @{
    "Content-Type" = "application/json"
}

Write-Host "============================="
Write-Host "  TESTING REST API WITH JWT  "
Write-Host "============================="

Write-Host "`n1. Registering new user..."
$registerBody = @{
    username = "api_test_user"
    email = "api_test@example.com"
    fullName = "API Tester"
    masterPassword = "SecretPassword123!"
    confirmPassword = "SecretPassword123!"
    securityQuestions = @(
        @{ questionText = "Q1"; answer = "A1" },
        @{ questionText = "Q2"; answer = "A2" },
        @{ questionText = "Q3"; answer = "A3" }
    )
} | ConvertTo-Json -Depth 10

try {
    $regResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -Headers $Headers -Body $registerBody
    Write-Host "Registration response: $($regResponse.message)" -ForegroundColor Green
} catch {
    Write-Host "Registration failed (user might already exist, proceeding to login)." -ForegroundColor Yellow
}

Write-Host "`n2. Logging in to get JWT token..."
$loginBody = @{
    usernameOrEmail = "api_test_user"
    masterPassword = "SecretPassword123!"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Post -Headers $Headers -Body $loginBody
    $token = $loginResponse.token
    Write-Host "Login successful! Retrieved JWT Token." -ForegroundColor Green
    # Write-Host $token -ForegroundColor Cyan
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

$AuthHeaders = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}

Write-Host "`n3. Adding a new vault entry..."
$vaultBody = @{
    accountName = "My Secret API Account"
    websiteUrl = "https://example-api.com"
    accountUsername = "tester_1"
    password = "SuperSecretVaultPassword123"
    category = "OTHER"
} | ConvertTo-Json

try {
    $vaultResponse = Invoke-RestMethod -Uri "$BaseUrl/vault" -Method Post -Headers $AuthHeaders -Body $vaultBody
    Write-Host "Vault addition response: $($vaultResponse.message)" -ForegroundColor Green
} catch {
    Write-Host "Vault addition failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n4. Retrieving vault entries..."
try {
    $vaultEntries = Invoke-RestMethod -Uri "$BaseUrl/vault" -Method Get -Headers $AuthHeaders
    Write-Host "Found $(if ($vaultEntries) { $vaultEntries.Count } else { 0 }) vault entries." -ForegroundColor Green
    $vaultEntries | Format-Table -Property id, accountName, accountUsername, websiteUrl
} catch {
    Write-Host "Retrieving vault entries failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n5. Generating strong passwords..."
$genBody = @{
    length = 24
    includeUppercase = $true
    includeLowercase = $true
    includeNumbers = $true
    includeSymbols = $true
    count = 3
} | ConvertTo-Json

try {
    $genResponse = Invoke-RestMethod -Uri "$BaseUrl/generator/generate" -Method Post -Headers $AuthHeaders -Body $genBody
    Write-Host "Password generator response:" -ForegroundColor Green
    $genResponse | Format-Table -Property password, score, label
} catch {
    Write-Host "Generating passwords failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n============================="
Write-Host "  API TESTING COMPLETE"
Write-Host "============================="

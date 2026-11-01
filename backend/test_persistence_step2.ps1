# Step 2: Verification AFTER application restart
$ErrorActionPreference = "Stop"

$base = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$testPassword = $env:STUDYFLOW_TEST_PASSWORD
if ([string]::IsNullOrWhiteSpace($testPassword)) {
    throw "Set STUDYFLOW_TEST_PASSWORD before running the persistence test."
}

# 1. Fetch CSRF token
Write-Host "Fetching CSRF token after restart..."
$csrfResp = Invoke-RestMethod -Uri "$base/api/auth/csrf" -Method GET -WebSession $session
$csrfToken = $csrfResp.token
$headers = @{
    "Content-Type" = "application/json"
    "X-XSRF-TOKEN" = $csrfToken
}

# 2. Login using phone number to verify phone login persistence
Write-Host "Verifying login with PHONE NUMBER (+19991234567)..."
$phoneLoginPayload = @{
    identifier = "+19991234567"
    password = $testPassword
} | ConvertTo-Json
$phoneLoginResp = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -Body $phoneLoginPayload -Headers $headers -WebSession $session
Write-Host "Phone login SUCCESS: $($phoneLoginResp.fullName) ($($phoneLoginResp.phoneNumber))"

# 3. Login using EMAIL
Write-Host "Verifying login with EMAIL (scholar@studyflow.edu)..."
$emailLoginPayload = @{
    identifier = "scholar@studyflow.edu"
    password = $testPassword
} | ConvertTo-Json
$emailLoginResp = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -Body $emailLoginPayload -Headers $headers -WebSession $session
Write-Host "Email login SUCCESS: $($emailLoginResp.fullName) ($($emailLoginResp.email))"

# 4. Verify /api/auth/me
$me = Invoke-RestMethod -Uri "$base/api/auth/me" -Method GET -WebSession $session
Write-Host "User record verified: ID=$($me.id), Name=$($me.fullName), Email=$($me.email)"

# 5. Verify Tasks survived restart
$tasks = Invoke-RestMethod -Uri "$base/api/tasks" -Method GET -WebSession $session
Write-Host "Tasks found count: $($tasks.Count)"
foreach ($t in $tasks) {
    Write-Host "  -> Task: ID=$($t.id), Title=$($t.title), Category=$($t.category), Priority=$($t.priority), DueDate=$($t.dueDate), Completed=$($t.completed)"
}
if ($tasks.Count -lt 2) {
    throw "Expected at least 2 tasks to survive restart!"
}

# 6. Verify Study Sessions survived restart
$sessions = Invoke-RestMethod -Uri "$base/api/study-sessions" -Method GET -WebSession $session
Write-Host "Study sessions found count: $($sessions.Count)"
foreach ($s in $sessions) {
    Write-Host "  -> Session: ID=$($s.id), Subject=$($s.subject), Topic=$($s.topic), Date=$($s.sessionDate), Time=$($s.startTime)-$($s.endTime)"
}
if ($sessions.Count -lt 1) {
    throw "Expected at least 1 study session to survive restart!"
}

# 7. Verify Notification Settings survived restart
$settings = Invoke-RestMethod -Uri "$base/api/notifications/settings" -Method GET -WebSession $session
Write-Host "Notification Settings: enabled=$($settings.notificationsEnabled), reminderMinutes=$($settings.reminderMinutes)"
if ($settings.notificationsEnabled -ne $true -or $settings.reminderMinutes -ne 10) {
    throw "Expected notification settings to survive restart!"
}

# 8. Verify Notifications survived restart
$notifications = Invoke-RestMethod -Uri "$base/api/notifications" -Method GET -WebSession $session
Write-Host "Notifications found count: $($notifications.Count)"

Write-Host "=== STEP 2 COMPLETE: PRODUCTIVITY DATA VERIFIED AFTER RESTART ==="

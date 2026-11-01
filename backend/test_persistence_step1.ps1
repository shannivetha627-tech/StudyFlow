# Step 1: Populate data against the running application
$ErrorActionPreference = "Stop"

$base = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$testPassword = $env:STUDYFLOW_TEST_PASSWORD
if ([string]::IsNullOrWhiteSpace($testPassword)) {
    throw "Set STUDYFLOW_TEST_PASSWORD before running the persistence test."
}

# 1. Fetch CSRF token
Write-Host "Fetching CSRF token..."
$csrfResp = Invoke-RestMethod -Uri "$base/api/auth/csrf" -Method GET -WebSession $session
$csrfToken = $csrfResp.token
$headers = @{
    "Content-Type" = "application/json"
    "X-XSRF-TOKEN" = $csrfToken
}
Write-Host "CSRF token received."

# 2. Register account
$regPayload = @{
    fullName = "Persistent Scholar"
    email = "scholar@studyflow.edu"
    phoneNumber = "+19991234567"
    password = $testPassword
    confirmPassword = $testPassword
} | ConvertTo-Json

Write-Host "Registering account..."
$regResp = Invoke-RestMethod -Uri "$base/api/auth/register" -Method POST -Body $regPayload -Headers $headers -WebSession $session
Write-Host "Registration response: $($regResp | ConvertTo-Json -Compress)"

# 3. Login with email
Write-Host "Logging in with email..."
$loginPayload = @{
    identifier = "scholar@studyflow.edu"
    password = $testPassword
} | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -Body $loginPayload -Headers $headers -WebSession $session
Write-Host "Logged in as: $($loginResp.fullName) ($($loginResp.email))"

# 4. Verify /api/auth/me
$meResp = Invoke-RestMethod -Uri "$base/api/auth/me" -Method GET -WebSession $session
Write-Host "Verified /api/auth/me: id=$($meResp.id), email=$($meResp.email)"

# 5. Create Task 1 (All fields)
$task1Payload = @{
    title = "Complete Quantum Physics Lab"
    description = "Analyze interference patterns and write report"
    category = "Physics"
    priority = "HIGH"
    dueDate = "2026-11-05"
    completed = $false
} | ConvertTo-Json
$task1 = Invoke-RestMethod -Uri "$base/api/tasks" -Method POST -Body $task1Payload -Headers $headers -WebSession $session
Write-Host "Created Task 1: id=$($task1.id), title=$($task1.title), priority=$($task1.priority)"

# 6. Create Task 2 (Optional fields empty)
$task2Payload = @{
    title = "Review Linear Algebra Chapter 3"
    category = "Mathematics"
    priority = "Medium"
    description = ""
    dueDate = ""
} | ConvertTo-Json
$task2 = Invoke-RestMethod -Uri "$base/api/tasks" -Method POST -Body $task2Payload -Headers $headers -WebSession $session
Write-Host "Created Task 2: id=$($task2.id), title=$($task2.title), priority=$($task2.priority)"

# 7. Edit Task 1
$task1EditPayload = @{
    title = "Complete Quantum Physics Lab (Final Submission)"
    description = "Finished analysis, reviewing final bibliography"
    category = "Advanced Physics"
    priority = "HIGH"
    dueDate = "2026-11-06"
    completed = $false
} | ConvertTo-Json
$task1Updated = Invoke-RestMethod -Uri "$base/api/tasks/$($task1.id)" -Method PUT -Body $task1EditPayload -Headers $headers -WebSession $session
Write-Host "Updated Task 1: title=$($task1Updated.title)"

# 8. Mark Task 2 Completed
$task2Completed = Invoke-RestMethod -Uri "$base/api/tasks/$($task2.id)/complete" -Method PATCH -Headers $headers -WebSession $session
Write-Host "Completed Task 2: completed=$($task2Completed.completed)"

# 9. Create Study Session
$sessionPayload = @{
    subject = "Computer Science"
    topic = "Distributed Systems Consensus"
    description = "Raft and Paxos algorithm comparison"
    sessionDate = "2026-11-02"
    startTime = "14:00:00"
    endTime = "16:00:00"
    reminderEnabled = $true
    reminderMinutes = 30
    completed = $false
} | ConvertTo-Json
$studySession = Invoke-RestMethod -Uri "$base/api/study-sessions" -Method POST -Body $sessionPayload -Headers $headers -WebSession $session
Write-Host "Created Study Session: id=$($studySession.id), subject=$($studySession.subject)"

# 10. Update notification settings & query notifications
$settingsPayload = @{
    notificationsEnabled = $true
    reminderMinutes = 10
    soundEnabled = $true
    dailyReminderEnabled = $true
    dailyReminderTime = "09:00:00"
} | ConvertTo-Json
$settings = Invoke-RestMethod -Uri "$base/api/notifications/settings" -Method PUT -Body $settingsPayload -Headers $headers -WebSession $session
Write-Host "Updated Notification Settings: enabled=$($settings.notificationsEnabled)"

$notifications = Invoke-RestMethod -Uri "$base/api/notifications" -Method GET -WebSession $session
Write-Host "Synced notifications count: $($notifications.Count)"

Write-Host "=== STEP 1 COMPLETE: PRODUCTIVITY DATA CREATED SUCCESSFULLY ==="

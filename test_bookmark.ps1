# 테스트용 스크립트 - 새로운 토큰 발급 받기

$BASE_URL = "http://localhost:8083/api"

Write-Host "=== 로그인하여 새 토큰 받기 ===" -ForegroundColor Green

# 로그인 (이미 가입된 계정 사용)
$loginBody = @{
    email = "user@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/users/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody

    $accessToken = $response.accessToken

    Write-Host "✓ 로그인 성공!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Access Token:" -ForegroundColor Yellow
    Write-Host $accessToken
    Write-Host ""
    Write-Host "User ID: $($response.userId)" -ForegroundColor Cyan
    Write-Host "Nickname: $($response.nickname)" -ForegroundColor Cyan
    Write-Host ""

    # 북마크 테스트
    Write-Host "=== 북마크 추가 테스트 (Post ID: 1) ===" -ForegroundColor Green
    $headers = @{
        Authorization = "Bearer $accessToken"
    }

    try {
        Invoke-RestMethod -Uri "$BASE_URL/posts/1/bookmarks" `
            -Method Post `
            -Headers $headers
        Write-Host "✓ 북마크 추가 성공!" -ForegroundColor Green
    } catch {
        Write-Host "✗ 북마크 추가 실패:" -ForegroundColor Red
        Write-Host $_.Exception.Message
    }

} catch {
    Write-Host "✗ 로그인 실패" -ForegroundColor Red
    Write-Host $_.Exception.Message
}


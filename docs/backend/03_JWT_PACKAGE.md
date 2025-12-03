# 🔐 JWT 패키지 상세 문서

## 📦 패키지 구조
```
org.likelionhsu.backend.jwt/
├── JwtTokenProvider.java
└── JwtAuthenticationFilter.java
```

---

## 🎯 패키지 역할
JWT(JSON Web Token) 기반 인증 시스템을 구현하여 Stateless한 사용자 인증을 제공합니다.

---

## 📋 클래스별 상세 설명

### 🔷 JwtTokenProvider.java
**역할**: JWT 토큰 생성, 검증, 파싱을 담당하는 핵심 컴포넌트

#### 의존성
```java
@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;
    
    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-validity}") long accessTokenValidityMs,
        @Value("${jwt.refresh-token-validity}") long refreshTokenValidityMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
            secret.getBytes(StandardCharsets.UTF_8)
        );
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }
}
```

**설정값 (application.yml)**:
```yaml
jwt:
  secret: ${JWT_SECRET}  # 최소 256bit (32자 이상)
  access-token-validity: 900000  # 15분 (밀리초)
  refresh-token-validity: 604800000  # 7일 (밀리초)
```

---

#### 주요 메서드

**1. Access Token 생성**:
```java
public String createAccessToken(Long userId, String email) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTokenValidityMs);
    
    return Jwts.builder()
        .subject(String.valueOf(userId))  // 사용자 ID
        .claim("email", email)            // 이메일
        .claim("type", "access")          // 토큰 타입
        .issuedAt(now)                    // 발급 시간
        .expiration(validity)             // 만료 시간
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
}
```

**Access Token Payload 예시**:
```json
{
  "sub": "123",
  "email": "user@example.com",
  "type": "access",
  "iat": 1733184000,
  "exp": 1733184900
}
```

---

**2. Refresh Token 생성**:
```java
public String createRefreshToken(Long userId) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshTokenValidityMs);
    
    return Jwts.builder()
        .subject(String.valueOf(userId))  // 사용자 ID만 포함
        .claim("type", "refresh")         // 토큰 타입
        .issuedAt(now)
        .expiration(validity)
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
}
```

**Refresh Token Payload 예시**:
```json
{
  "sub": "123",
  "type": "refresh",
  "iat": 1733184000,
  "exp": 1733788800
}
```

**특징**:
- Access Token보다 정보가 적음 (보안 강화)
- 긴 유효기간 (7일)
- 오직 토큰 갱신 용도로만 사용

---

**3. Claims 추출**:
```java
public Claims getClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
}

public Long getUserId(String token) {
    return Long.valueOf(getClaims(token).getSubject());
}

public String getEmail(String token) {
    return getClaims(token).get("email", String.class);
}
```

**사용 예시**:
```java
String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
Long userId = jwtTokenProvider.getUserId(token);  // 123
String email = jwtTokenProvider.getEmail(token);   // "user@example.com"
```

---

**4. 토큰 검증**:
```java
public boolean validateToken(String token) {
    try {
        Claims claims = getClaims(token);
        return !claims.getExpiration().before(new Date());
    } catch (Exception e) {
        return false;
    }
}
```

**검증 항목**:
- ✅ 서명 유효성 (SecretKey 일치)
- ✅ 만료 시간 검증
- ❌ 예외 발생 시 false 반환 (변조/만료/형식 오류)

---

**5. 토큰 타입 확인**:
```java
public boolean isRefreshToken(String token) {
    try {
        return "refresh".equals(getClaims(token).get("type", String.class));
    } catch (Exception e) {
        return false;
    }
}

public boolean isAccessToken(String token) {
    try {
        return "access".equals(getClaims(token).get("type", String.class));
    } catch (Exception e) {
        return false;
    }
}
```

**사용 이유**:
- Access Token으로만 API 접근 가능
- Refresh Token은 오직 `/refresh` 엔드포인트에서만 사용
- 토큰 오용 방지

---

### 🔷 JwtAuthenticationFilter.java
**역할**: HTTP 요청에서 JWT를 추출하고 인증 처리하는 Filter

#### 구조
```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        // ...
    }
    
    private String resolveToken(HttpServletRequest request) {
        // ...
    }
}
```

---

#### 주요 메서드

**1. 토큰 추출**:
```java
private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7);  // "Bearer " 제거
    }
    return null;
}
```

**요청 예시**:
```http
GET /api/users/me HTTP/1.1
Host: localhost:8083
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

**2. 필터 로직**:
```java
@Override
protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
) throws ServletException, IOException {
    
    // 1. 요청에서 토큰 추출
    String token = resolveToken(request);
    
    // 2. 토큰 검증 + Access Token 확인
    if (token != null && 
        jwtTokenProvider.validateToken(token) && 
        jwtTokenProvider.isAccessToken(token)) {
        
        // 3. 토큰에서 userId 추출
        Long userId = jwtTokenProvider.getUserId(token);
        
        // 4. UserDetailsService로 사용자 정보 로드
        UserDetails userDetails = userDetailsService.loadUserByUsername(
            String.valueOf(userId)
        );
        
        // 5. Authentication 객체 생성
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
        );
        
        // 6. SecurityContext에 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    // 7. 다음 필터로 진행
    filterChain.doFilter(request, response);
}
```

---

#### 필터 동작 흐름

**성공 케이스**:
```
1. 클라이언트 요청 (Authorization: Bearer {token})
   ↓
2. resolveToken(): 토큰 추출
   ↓
3. validateToken(): 유효성 검증 ✅
   ↓
4. isAccessToken(): Access Token 확인 ✅
   ↓
5. getUserId(): userId 추출 (123)
   ↓
6. loadUserByUsername("123"): User 조회
   ↓
7. SecurityContext에 인증 정보 저장
   ↓
8. Controller에서 @AuthenticationPrincipal로 사용자 정보 사용 가능
```

**실패 케이스 (토큰 없음)**:
```
1. 클라이언트 요청 (Authorization 헤더 없음)
   ↓
2. resolveToken(): null 반환
   ↓
3. 인증 처리 건너뜀
   ↓
4. SecurityContext: 빈 상태
   ↓
5. Controller: @AuthenticationPrincipal = null (익명 사용자)
```

**실패 케이스 (토큰 만료)**:
```
1. 클라이언트 요청 (만료된 토큰)
   ↓
2. resolveToken(): 토큰 추출
   ↓
3. validateToken(): false ❌ (만료 시간 초과)
   ↓
4. 인증 처리 건너뜀
   ↓
5. SecurityContext: 빈 상태
   ↓
6. Controller: 401 Unauthorized 응답
```

**실패 케이스 (Refresh Token 오용)**:
```
1. 클라이언트 요청 (Refresh Token으로 API 호출)
   ↓
2. resolveToken(): 토큰 추출
   ↓
3. validateToken(): true ✅
   ↓
4. isAccessToken(): false ❌ (type: "refresh")
   ↓
5. 인증 처리 건너뜀
   ↓
6. Controller: 401 Unauthorized 응답
```

---

## 🔄 전체 인증 흐름

### 1. 최초 로그인
```
[Client]
  POST /api/users/login
  { "email": "...", "password": "..." }
    ↓
[UserService]
  비밀번호 검증 ✅
    ↓
[JwtTokenProvider]
  createAccessToken(userId, email)  → Access Token (15분)
  createRefreshToken(userId)        → Refresh Token (7일)
    ↓
[Client]
  토큰 저장 (localStorage/memory)
```

---

### 2. API 요청 (인증 필요)
```
[Client]
  GET /api/users/me
  Authorization: Bearer {accessToken}
    ↓
[JwtAuthenticationFilter]
  토큰 추출 → 검증 → userId 추출
    ↓
[CustomUserDetailsService]
  loadUserByUsername(userId) → User 조회
    ↓
[SecurityContext]
  인증 정보 저장
    ↓
[UserController]
  @AuthenticationPrincipal UserDetailsImpl userDetails
  → userDetails.getUserId() 사용
    ↓
[Response]
  사용자 정보 반환
```

---

### 3. Access Token 만료 시
```
[Client]
  GET /api/users/me
  Authorization: Bearer {expiredAccessToken}
    ↓
[JwtAuthenticationFilter]
  validateToken() → false (만료)
  인증 실패 → SecurityContext 빈 상태
    ↓
[Controller]
  401 Unauthorized
    ↓
[Client]
  POST /api/users/refresh
  { "refreshToken": "..." }
    ↓
[UserService]
  refreshToken 검증 ✅
    ↓
[JwtTokenProvider]
  createAccessToken() → 새 Access Token
    ↓
[Client]
  새 Access Token 저장
  → 이전 요청 재시도
```

---

## 💡 주요 설계 포인트

### 1. Stateless 인증
- 서버에 세션 저장 X
- 모든 인증 정보가 토큰에 포함
- 수평 확장 용이 (로드밸런싱)

### 2. 토큰 분리 전략
- **Access Token**: 짧은 유효기간, API 접근용
- **Refresh Token**: 긴 유효기간, 토큰 갱신용
- 보안성과 편의성의 균형

### 3. OncePerRequestFilter
- 요청당 1번만 실행 보장
- 중복 인증 처리 방지

### 4. 타입 안전성
- `type` claim으로 Access/Refresh 구분
- Refresh Token으로 API 접근 차단

### 5. 예외 처리
- 토큰 검증 실패 시 false 반환 (예외 전파 X)
- 필터 레벨에서 조용히 처리
- Controller에서 인증 상태 확인

---

## 🔒 보안 고려사항

### 1. Secret Key 관리
```yaml
# ❌ 하드코딩 금지
jwt:
  secret: "mySecretKey123"

# ✅ 환경변수 사용
jwt:
  secret: ${JWT_SECRET}
```

**요구사항**:
- 최소 256bit (32자 이상)
- 무작위 문자열
- 환경변수로 관리
- 주기적 로테이션 권장

---

### 2. HTTPS 필수
- JWT는 평문 인코딩 (암호화 X)
- HTTPS 없이 사용 시 토큰 탈취 위험
- 프로덕션 환경에서는 반드시 HTTPS

---

### 3. XSS 방지
```javascript
// ❌ localStorage 저장 (XSS 취약)
localStorage.setItem('accessToken', token);

// ✅ HttpOnly Cookie (추천)
// 또는 메모리 저장 (React State)
```

---

### 4. CSRF 방지
- JWT는 Cookie가 아닌 Header로 전송
- CSRF 공격에 비교적 안전
- 하지만 XSS에는 취약 → HttpOnly Cookie 권장

---

### 5. 토큰 재사용 공격 방지
- Refresh Token은 1회용으로 설계 가능 (RTR: Refresh Token Rotation)
- 현재 구현: 기존 Refresh Token 재사용
- 향후 개선: 갱신 시 새 Refresh Token 발급

---

## 📊 토큰 비교

| 항목 | Access Token | Refresh Token |
|-----|--------------|---------------|
| **유효기간** | 15분 | 7일 |
| **포함 정보** | userId, email, type | userId, type |
| **용도** | API 인증 | 토큰 갱신 |
| **재발급** | 15분마다 (자동) | 7일마다 (로그인) |
| **보안 수준** | 높음 (짧은 유효기간) | 중간 (제한된 용도) |
| **저장 위치** | 메모리/Cookie | 보안 저장소 |

---

## 🔗 연관 패키지

- **User**: 사용자 인증 로직
- **SecurityConfig**: Spring Security 설정
- **CustomUserDetailsService**: 사용자 정보 로드

---

## 🧪 테스트 시나리오

### 1. 토큰 생성 테스트
```java
@Test
void createAccessToken() {
    String token = jwtTokenProvider.createAccessToken(123L, "test@example.com");
    assertThat(token).isNotNull();
    assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(123L);
    assertThat(jwtTokenProvider.getEmail(token)).isEqualTo("test@example.com");
    assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
}
```

### 2. 토큰 만료 테스트
```java
@Test
void expiredToken() {
    // validity를 음수로 설정하여 만료된 토큰 생성
    String expiredToken = createExpiredToken();
    assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
}
```

### 3. 토큰 변조 테스트
```java
@Test
void tamperedToken() {
    String token = jwtTokenProvider.createAccessToken(123L, "test@example.com");
    String tampered = token.substring(0, token.length() - 1) + "X";
    assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
}
```

### 4. Refresh Token으로 API 접근 테스트
```java
@Test
void refreshTokenCannotAccessApi() {
    String refreshToken = jwtTokenProvider.createRefreshToken(123L);
    assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse();
}
```


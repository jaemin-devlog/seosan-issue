# ⚙️ Common 패키지 상세 문서

## 📦 패키지 구조
```
org.likelionhsu.backend.common/
├── config/
│   ├── CacheConfig.java
│   ├── CorsConfig.java
│   ├── HttpClientsConfig.java
│   ├── LocalCacheFallbackConfig.java
│   └── NaverSearchProperties.java
└── exception/
    ├── ErrorCode.java
    ├── ErrorResponse.java
    ├── CustomException.java
    ├── GlobalExceptionHandler.java
    └── customexception/
        └── PostCustomException.java

org.likelionhsu.backend.config/
└── SecurityConfig.java
```

---

## 🎯 패키지 역할
애플리케이션 전역 설정, 예외 처리, 공통 유틸리티를 담당합니다.

---

## 📋 클래스별 상세 설명

### 1. Config Layer

#### 🔷 SecurityConfig.java
**역할**: Spring Security 설정

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용)
            .csrf(AbstractHttpConfigurer::disable)
            
            // 세션 사용 안 함 (Stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 경로별 인증 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/users/signup",      // 회원가입
                    "/api/users/login",       // 로그인
                    "/api/users/refresh",     // 토큰 갱신
                    "/swagger-ui/**",         // Swagger UI
                    "/v3/api-docs/**",        // OpenAPI 문서
                    "/api/posts/**",          // 게시글 조회 (인증 불필요)
                    "/api/naver-search/**",   // 네이버 검색
                    "/api/flask/**",          // Flask API
                    "/api/v1/**"              // AI 검색 등
                ).permitAll()
                .anyRequest().authenticated()  // 나머지는 인증 필요
            )
            
            // JWT 필터 추가
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**주요 설정**:
- **CSRF 비활성화**: JWT 토큰 인증 방식에서는 불필요
- **Stateless 세션**: 서버에 세션 저장 X, JWT만 사용
- **경로별 권한**:
  - `permitAll()`: 인증 불필요 (회원가입, 로그인, 게시글 조회 등)
  - `authenticated()`: 인증 필요 (북마크, 좋아요, 댓글 작성 등)
- **BCrypt**: 비밀번호 암호화 (단방향 해시)

---

#### 🔷 HttpClientsConfig.java
**역할**: WebClient 설정 (외부 API 호출용)

```java
@Configuration
@Slf4j
public class HttpClientsConfig {
    
    /** Flask 모델 서버용 WebClient (타임아웃 300초) */
    @Bean("flaskWebClient")
    public WebClient flaskWebClient(
        WebClient.Builder builder,
        @Value("${crawler.api.url:http://crawler:5001}") String baseUrl
    ) {
        HttpClient http = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000)
            .responseTimeout(Duration.ofSeconds(300))  // 요약은 오래 걸림
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(305, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(305, TimeUnit.SECONDS)));
        
        return builder.baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(http))
            .exchangeStrategies(strategies(8))  // 최대 8MB 응답
            .filter(timing("flask"))  // 로깅 필터
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Accept-Encoding", "identity")
            .build();
    }
    
    /** 외부 API용 WebClient (네이버, 기상청 등, 타임아웃 90초) */
    @Bean("externalWebClient")
    public WebClient externalWebClient(WebClient.Builder builder) {
        HttpClient http = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(Duration.ofSeconds(90));
        
        return builder
            .clientConnector(new ReactorClientHttpConnector(http))
            .exchangeStrategies(strategies(8))
            .filter(timing("ext"))
            .defaultHeader("Accept", "application/json")
            .build();
    }
    
    /** 요청/응답 타이밍 로깅 필터 */
    private static ExchangeFilterFunction timing(String tag) {
        return (request, next) -> {
            long t0 = System.nanoTime();
            return next.exchange(request)
                .doOnNext(res -> {
                    long ms = (System.nanoTime() - t0) / 1_000_000;
                    log.info("[{}] {} {} -> {} {}ms",
                        tag, request.method(), request.url(),
                        res.rawStatusCode(), ms);
                })
                .doOnError(err -> {
                    long ms = (System.nanoTime() - t0) / 1_000_000;
                    log.warn("[{}] {} {} -> ERR {}ms : {}",
                        tag, request.method(), request.url(), ms, err.toString());
                });
        };
    }
    
    /** 큰 응답(최대 8MB) 허용 */
    private static ExchangeStrategies strategies(int mb) {
        return ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(mb * 1024 * 1024))
            .build();
    }
}
```

**특징**:
- **두 개의 WebClient**:
  - `flaskWebClient`: Flask 서버용 (긴 타임아웃)
  - `externalWebClient`: 외부 API용 (짧은 타임아웃)
- **타이밍 로깅**: 모든 요청/응답 시간 로깅
- **큰 응답 지원**: 최대 8MB (기본 256KB)
- **Netty 기반**: 비동기 논블로킹 HTTP 클라이언트

**로그 예시**:
```
[flask] POST http://crawler:5001/summarize -> 200 3245ms
[ext] GET https://openapi.naver.com/v1/search/news.json -> 200 142ms
```

---

#### 🔷 CacheConfig.java
**역할**: 캐시 설정 (Caffeine)

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
            buildCache("discovery", 10, TimeUnit.MINUTES),  // 네이버 검색
            buildCache("weather", 30, TimeUnit.MINUTES),    // 날씨 정보
            buildCache("trends", 60, TimeUnit.MINUTES)      // 트렌드 데이터
        ));
        return manager;
    }
    
    private CaffeineCache buildCache(String name, long duration, TimeUnit unit) {
        return new CaffeineCache(name, Caffeine.newBuilder()
            .expireAfterWrite(duration, unit)
            .maximumSize(500)
            .recordStats()
            .build());
    }
}
```

**캐시 정책**:
- **discovery** (네이버 검색): 10분
- **weather** (날씨): 30분
- **trends** (트렌드): 60분
- 최대 500개 항목 저장

**사용 예시**:
```java
@Cacheable(cacheNames = "discovery", key = "#type + '::' + #query")
public List<NaverSearchItemDto> search(String type, String query, int display) {
    // ...
}
```

---

#### 🔷 LocalCacheFallbackConfig.java
**역할**: Redis 없을 때 Caffeine으로 폴백

```java
@Configuration
public class LocalCacheFallbackConfig {
    
    @Bean
    @Primary
    public CacheManager cacheManager() {
        // Redis 연결 시도
        try {
            return redisCacheManager();
        } catch (Exception e) {
            log.warn("Redis 연결 실패, Caffeine 캐시로 폴백", e);
            return caffeineCacheManager();
        }
    }
}
```

**특징**:
- Redis 우선, 연결 실패 시 Caffeine 사용
- 로컬 개발 환경에서 유용

---

#### 🔷 CorsConfig.java
**역할**: CORS 설정 (프론트엔드 연동)

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:3000",   // React 개발 서버
                "https://seosan-issue.com" // 프로덕션 도메인
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

#### 🔷 NaverSearchProperties.java
**역할**: 네이버 API 설정 프로퍼티 바인딩

```java
@ConfigurationProperties(prefix = "naver.api")
@Data
public class NaverSearchProperties {
    private String clientId;
    private String clientSecret;
    private int defaultDisplay = 10;
}
```

**application.yml**:
```yaml
naver:
  api:
    client-id: ${NAVER_CLIENT_ID}
    client-secret: ${NAVER_CLIENT_SECRET}
    default-display: 10
```

---

### 2. Exception Layer

#### 🔷 ErrorCode.java
**역할**: 에러 코드 Enum

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E001", "잘못된 입력값입니다"),
    
    // 404 Not Found
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "E101", "게시글을 찾을 수 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E102", "사용자를 찾을 수 없습니다"),
    
    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E201", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E202", "유효하지 않은 토큰입니다"),
    
    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "E301", "접근 권한이 없습니다"),
    
    // 409 Conflict
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "E401", "이미 사용 중인 이메일입니다"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "E402", "이미 사용 중인 닉네임입니다"),
    
    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "서버 오류가 발생했습니다");
    
    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

---

#### 🔷 ErrorResponse.java
**역할**: 에러 응답 DTO

```java
@Getter
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private LocalDateTime timestamp;
    
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }
}
```

**응답 예시**:
```json
{
  "code": "E101",
  "message": "게시글을 찾을 수 없습니다",
  "timestamp": "2025-12-03T15:30:45"
}
```

---

#### 🔷 CustomException.java
**역할**: 커스텀 예외 기본 클래스

```java
@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;
    
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

---

#### 🔷 PostCustomException.java
**역할**: 게시글 관련 커스텀 예외

```java
public class PostCustomException extends CustomException {
    public PostCustomException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

**사용 예시**:
```java
Post post = postRepository.findById(postId)
    .orElseThrow(() -> new PostCustomException(ErrorCode.POST_NOT_FOUND));
```

---

#### 🔷 GlobalExceptionHandler.java
**역할**: 전역 예외 처리기

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /** 커스텀 예외 처리 */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("CustomException: {}", e.getErrorCode().getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(e.getErrorCode());
        return new ResponseEntity<>(errorResponse, e.getErrorCode().getStatus());
    }
    
    /** 일반 예외 처리 (500) */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception: ", e);
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

**동작 흐름**:
```
1. PostService에서 예외 발생
   throw new PostCustomException(ErrorCode.POST_NOT_FOUND);
   ↓
2. GlobalExceptionHandler.handleCustomException() 호출
   ↓
3. ErrorResponse 생성
   ↓
4. HTTP 404 응답
   {
     "code": "E101",
     "message": "게시글을 찾을 수 없습니다",
     "timestamp": "2025-12-03T15:30:45"
   }
```

---

## 🔄 설정 로딩 순서

```
1. application.yml 로드
   ↓
2. 환경변수 주입 (DB_PASSWORD, NAVER_CLIENT_ID 등)
   ↓
3. @ConfigurationProperties 바인딩
   - NaverSearchProperties
   ↓
4. @Configuration 클래스 실행
   - SecurityConfig
   - HttpClientsConfig
   - CacheConfig
   - CorsConfig
   ↓
5. @Bean 등록
   - flaskWebClient
   - externalWebClient
   - caffeineCacheManager
   - passwordEncoder
   ↓
6. @EnableWebSecurity 활성화
   - SecurityFilterChain 적용
   - JwtAuthenticationFilter 등록
   ↓
7. @EnableCaching 활성화
   - @Cacheable 어노테이션 동작
   ↓
8. 애플리케이션 시작 완료
```

---

## 💡 주요 설계 포인트

### 1. Security
- **JWT 기반 Stateless 인증**: 서버 확장성 ↑
- **경로별 권한 분리**: 공개 API vs 인증 필요 API
- **BCrypt 암호화**: 단방향 해시, 솔트 자동 생성

### 2. HTTP Clients
- **목적별 WebClient 분리**: Flask용 vs 외부 API용
- **타이밍 로깅**: 성능 모니터링 용이
- **큰 응답 지원**: 8MB (AI 모델 응답 대응)

### 3. Caching
- **계층별 TTL**: 검색(10분), 날씨(30분), 트렌드(60분)
- **Redis 폴백**: 개발 환경 편의성
- **통계 기록**: `recordStats()`로 히트율 확인

### 4. Exception Handling
- **통일된 에러 응답**: ErrorCode → ErrorResponse
- **로깅**: 모든 예외 로그 기록
- **클라이언트 친화적**: 명확한 에러 메시지

### 5. CORS
- **프론트엔드 연동**: localhost:3000, 프로덕션 도메인 허용
- **Credentials 지원**: 쿠키/인증 헤더 전송 가능

---

## 🔗 연관 패키지

- **모든 패키지**: Common은 전역 설정이므로 모든 패키지에서 사용

---

## 📊 설정 파일 구조

**application.yml**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:3306/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  access-token-validity: 900000   # 15분
  refresh-token-validity: 604800000  # 7일

naver:
  api:
    client-id: ${NAVER_CLIENT_ID}
    client-secret: ${NAVER_CLIENT_SECRET}

crawler:
  api:
    url: ${CRAWLER_API_URL:http://crawler:5001}

ai:
  summarizer:
    temperature: 0.2
    top-p: 0.3
    repetition-penalty: 1.18
    max-tokens: 220

logging:
  level:
    org.likelionhsu.backend: DEBUG
    org.springframework.security: INFO
```

---

## 📌 환경별 설정

### 개발 환경
- Caffeine 캐시
- H2 DB (선택)
- CORS: localhost:3000
- 상세한 로깅

### 프로덕션 환경
- Redis 캐시
- MySQL
- CORS: 실제 도메인
- ERROR 레벨 로깅
- HTTPS 필수

---

## 🧪 테스트 시나리오

### Security
1. 인증 없이 공개 API 접근 → 성공
2. 인증 없이 보호된 API 접근 → 401
3. 만료된 토큰으로 접근 → 401
4. 유효한 토큰으로 접근 → 성공

### Cache
1. 첫 요청 → DB/API 호출 → 캐시 저장
2. 두 번째 요청 (10분 이내) → 캐시에서 반환
3. 10분 후 요청 → 캐시 만료 → DB/API 재호출

### Exception
1. 존재하지 않는 게시글 조회 → 404 + ErrorResponse
2. 잘못된 입력값 → 400 + ErrorResponse
3. 서버 내부 오류 → 500 + ErrorResponse


# 암호화폐 커뮤니티 플랫폼 아키텍처 설계 및 구현 계획

## 0. 개요

- **목표**: Spring Boot 기반의 암호화폐 커뮤니티 플랫폼을 설계하고 단계별 구현 로드맵을 수립하여, 보안과 확장성이 확보된 포트폴리오 프로젝트를 완성한다.
- **핵심 특징**: JWT 기반 인증, CoinGecko 데이터 집계, 캐싱 전략, 커뮤니티 기능(게시판/댓글/투표/북마크), 하이브리드 렌더링(Thymeleaf + JavaScript), 단계별 고도화 로드맵.
- **기술 스택**: Java 17, Spring Boot 3.x, Spring Web, Spring Security, Spring Data JPA, Thymeleaf, WebClient, Caffeine Cache, MySQL(운영)/H2(개발), Lombok, JWT, Chart.js, Bootstrap 또는 Tailwind CSS.

```
[Browser]
   |-- HTTPS --|
[Spring Security Filters] -- AuthenticationManager -- JWTProvider
   |
[Controller Layer] -- (View Controller) --> Thymeleaf Templates
               \-- (REST Controller) --> JSON Responses
   |
[Service Layer] -- CryptoDataService(WebClient + Cache)
             \-- Domain Services(PostService, CommentService, ...)
   |
[Repository Layer] -- Spring Data JPA -- MySQL/H2
```

---

## 1. 기초 아키텍처 및 핵심 서비스

### 1.1 프로젝트 설정 및 환경 구성

| 항목            | 상세                                                                                                                                                    | 비고                                       |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| 프로젝트 초기화 | start.spring.io, Gradle, Java 17, Spring Boot 3.x                                                                                                       | Spring Initializr 활용                     |
| 필수 의존성     | spring-boot-starter-web, security, data-jpa, thymeleaf, validation, cache, devtools, mysql-connector-j, h2, lombok, jjwt, springdoc(optional), caffeine | Gradle `implementation`/`runtimeOnly` 정리 |
| 프로파일        | `application.yml`(공통), `application-dev.yml`, `application-test.yml`, `application-prod.yml`                                                          | `spring.profiles.active` 로 전환           |
| 개발 환경       | H2 혹은 로컬 MySQL, `ddl-auto=update`, DevTools                                                                                                         | 빠른 피드백                                |
| 운영 환경       | MySQL, `ddl-auto=validate`, 민감 정보는 환경 변수/외부 비밀 관리                                                                                        | TLS 전제                                   |
| 마이그레이션    | Flyway 또는 Liquibase 도입 권장                                                                                                                         | Schema 버전 관리                           |

### 1.2 사용자 및 인증 하위 시스템

- **Member 엔티티**
  - 필드: `id`, `username`, `password`, `email`, `nickname`, `role`, `createdAt`, `updatedAt`
  - 제약: username/email/nickname 유니크 + 인덱스, `role`은 `EnumType.STRING`
  - 공통 필드: `@CreatedDate`, `@LastModifiedDate` (Auditing)
- **역할(Role)**: `USER`, `ADMIN` (추후 `MODERATOR`, `EXPERT_WRITER` 등 확장)
- **비밀번호 보안**: BCryptPasswordEncoder 사용, 솔트 자동 처리
- **JWT 인증 플로우**
  1. 로그인 성공 시 Access Token(30분), 필요 시 Refresh Token(1~7일) 발급.
  2. 모든 보호된 요청에 `Authorization: Bearer <token>` 헤더 사용.
  3. JWT 필터에서 토큰 검증 후 `SecurityContext` 에 Authentication 주입.
- **Spring Security 구성**
  - 세션: `STATELESS`
  - CSRF: REST API에는 비활성, Thymeleaf 폼에는 활성화(필요 시)
  - 화이트리스트: `/auth/**`, `/login`, `/register`, `/api/public/**`

### 2.3 사용자 맞춤 관심코인 시세판

- **WatchlistService**
  - 게스트: `watchlist.defaults` 설정 기반 기본 코인 목록 제공.
  - 로그인 사용자: `watchlist_entries` 테이블에서 코인 ID·라벨·정렬 순서를 불러와 개인화.
  - CoinGecko `/coins/markets` 엔드포인트를 USD·KRW 2회 호출하여 가격/변동률/거래대금을 병합.
  - 캐시 키는 `coins.marketByIds`에 통합, 환율(`watchlist.usd-to-krw-rate`)을 활용해 프리미엄(%) 계산.
- **관리 기능**
  - `/watchlist/manage` 화면에서 CoinGecko ID 기반 추가/삭제, 최대 20개 제한.
  - `WatchlistEntryRepository`가 `(member_id, coin_id)` 유니크 제약, `display_order`로 정렬 유지.
  - 권한 분기: 게시물 쓰기/댓글은 `ROLE_USER`, 관리자 기능은 `ROLE_ADMIN`
  - 예외 처리: `AuthenticationEntryPoint`, `AccessDeniedHandler` 구현

### 1.3 핵심 데이터베이스 스키마 및 관계

| WatchlistEntry                 | id, coinId, label, displayOrder                                     | Member N:1                                  | 관심 코인 시세판   |
| 엔티티                         | 필드                                                                | 관계                                        | 설명               |
| ------------------------------ | ------------------------------------------------------------------- | ------------------------------------------- | ------------------ |
| Member                         | id, username, password, email, nickname, role, createdAt, updatedAt | Post/Comment/Vote/Bookmark 1:N              | 사용자             |
| Board                          | id, name, description, type, slug                                   | Post 1:N                                    | 게시판 카테고리    |
| Post                           | id, title, content(TEXT), viewCount, createdAt, updatedAt, postType | Member/Board N:1, Comment/Vote/Bookmark 1:N | 상위 추상 엔티티   |
| Comment                        | id, content, createdAt, updatedAt                                   | Member/Post N:1, Vote 1:N                   | 댓글               |
| Vote                           | id, value(1/-1), targetId, targetType, createdAt                    | Member N:1                                  | 다형 투표          |
| Bookmark                       | id, createdAt                                                       | Member/Post N:1, (member+post) unique       | 즐겨찾기           |
| CalendarEvent                  | id, title, eventDate, description, sourceUrl                        | Board 혹은 별도                             | 일정               |
| NewsPost/EventPost/AirdropPost | Post 상속                                                           |                                             | 게시판 유형별 확장 |
| `watchlist/manage.html`       | 관심 코인 목록 관리(추가/삭제, 미리보기)                     |

- **상속 전략**: `@Inheritance(strategy = JOINED)` 권장 (정규화 & 확장성)
- 공통 프래그먼트에 `fragments/watchlist.html`을 추가하여 홈/관리 화면 모두에서 시세 테이블 UI를 재사용한다.
- **마이그레이션 예시**: `V1__create_member_board_post.sql`
- **인덱스 설계**: `Member(username/email/nickname)`, `Post(board_id, created_at DESC)`, `Vote(member_id, target_id, target_type)` 유니크

---

## 2. 데이터 집계 및 시장 정보 허브

### 2.1 외부 API 제공자 분석

| 항목        | CoinGecko (무료 플랜)                       | CoinMarketCap (기본 플랜) | 결론                                 |
| ----------- | ------------------------------------------- | ------------------------- | ------------------------------------ |
| API 키      | 공개 엔드포인트 무키, Key 사용 시 서버 보관 | 필수                      | 초기 진입 장벽이 낮은 CoinGecko 선택 |
| 호출 제한   | 분당 10~50 (공유 IP)                        | 월 10,000 크레딧          | 실시간 처리에 CoinGecko가 단순       |
| 과거 데이터 | 무료 제공 (`/market_chart`)                 | 기본 플랜 미제공          | 차트 구현 핵심 요소                  |
| 문서/UX     | 개발자 친화적, 단순                         | 기업 중심, 정책 복잡      | 빠른 개발에 CoinGecko 우수           |

### 2.2 API 통합 아키텍처

- **백엔드 프록시**: 모든 외부 API 호출은 서버에서 수행, API Key 노출 금지.
- **서비스 계층**: `CryptoDataService`
  - WebClient 기반 비동기 호출
  - 공통 Base URL: `https://api.coingecko.com/api/v3`
  - 주요 메서드
    - `getTopCoins(perPage, page)` → `/coins/markets`
    - `getCoinDetail(coinId)` → `/coins/{id}`
    - `getMarketChart(coinId, days)` → `/coins/{id}/market_chart`
    - `getSimplePrices(coinIds)` → `/simple/price`
  - DTO: Jackson record 또는 Lombok `@Value` 활용
  - 예외 처리: Rate limit 대응, 필요 시 Spring Retry/Resilience4j 도입
- **REST 엔드포인트**
  - `/api/coins/markets`
  - `/api/coins/{id}`
  - `/api/coins/{id}/market-chart`
  - `/api/simple-prices`

### 2.4 캐싱 전략

- **기술**: Spring Cache + Caffeine (`expireAfterWrite`, `maximumSize`, `recordStats`)
- **적용 예**
  ```java
  @Cacheable(value = "marketData", key = "#coinId")
  public MarketChartDto getMarketChart(String coinId, int days) { ... }
  ```
- **TTL 가이드**
  - Top coins 리스트: 60초
  - 단일 코인 상세: 1~5분
  - 마켓 차트: 5분 (요청 빈도 고려)
- **효과**: API 호출량 감소, 응답 속도 향상, rate limit 준수
- **추가**: Redis 캐시/Resilience4j CircuitBreaker는 추후 확장 옵션

---

## 3. 커뮤니티 및 콘텐츠 참여 기능

### 3.1 게시판 및 게시물 관리

- **게시판 유형**: `BoardType` enum (`GENERAL`, `NEWS`, `ANALYSIS`, `AIRDROP`, `CALENDAR`)
- **Controller-Service-Repository 3계층 구조**
  - `BoardController`, `PostController`, `CommentController`
  - `PostService` 내 권한 검사: 작성자 본인 또는 ADMIN만 수정/삭제 가능
- **게시물 상속 전략**
  - `Post` 추상 클래스 + `@DiscriminatorColumn("post_type")`
  - 하위 엔티티 예시
    - `NewsPost` (sourceName, sourceUrl, publishedAt)
    - `EventPost` (eventDate, location, sourceUrl)
    - `AnalysisPost` (chartSymbol, timeFrame)
    - `AirdropPost` (projectName, reward, endDate)
- **데이터 접근 최적화**
  - `@EntityGraph` 또는 `fetch join`으로 N+1 최소화
  - 게시판별 최신 글 캐시 (Short TTL)

### 3.2 사용자 참여 기능

- **투표(Vote)**
  - 값: 1(추천), -1(비추천)
  - 중복 투표 방지: `(member_id, target_id, target_type)` 유니크 제약
  - 점수 계산: `SUM(value)` 또는 materialized field, 필요 시 백그라운드 갱신
- **북마크(Bookmark)**
  - 즐겨찾기 추가/해제 API, 내 북마크 목록 조회
- **조회수(viewCount)**
  - 사용자/IP 기반 쿨다운: 캐시에 `(postId, userId/IP)` 저장 후 TTL 동안 중복 방지
- **신고/모더레이션(확장)**
  - `Report` 엔티티, 관리자 알림 큐 등 추후 추가 가능

### 3.3 고급 콘텐츠 및 큐레이션

- **뉴스 애그리게이션**
  - `@Scheduled` 작업으로 외부 RSS/뉴스 API 호출 → `NewsPost` 자동 등록
  - 중복 방지 키(예: 기사 URL 해시)
- **코인 일정(Calendar Event)**
  - `CalendarEvent` 엔티티 + 관리자 CRUD 화면
  - 게시판과 연동하거나 별도 뷰 제공
- **데이터-커뮤니티 통합**
  - 게시물 작성 폼에 "차트 삽입" 기능 제공
  - 선택한 코인/기간을 기반으로 `CryptoDataService` 데이터 전달 → Chart.js 렌더
  - `Post` 엔티티에 JSON 메타데이터 컬럼 도입 고려 (`chartSettings`)
- **검색 기능**
  - 1차: Spring Data `findByTitleContaining` + 인덱스
  - 2차: ElasticSearch/OpenSearch 연동 검토

### 3.4 코인판 벤치마크 핵심 기능 맵

| 영역                    | 코인판 기준 UX                                                                      | 존비오 개선 방향                                                                                              |
| ----------------------- | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------- |
| 메인 히어로             | 상단 티커 + 고정 배너 + 이벤트 링크                                                 | 코인 가격 티커 + 주간 핫토픽 배너(관리자 등록) + 포트폴리오 CTA                                                |
| 멀티 게시판 피드        | 자유/정보/이슈/인기글을 한 화면에서 카드 레이아웃으로 스위칭                        | "지금 뜨는 글", "핫토픽", "실시간 댓글" 3단 컬럼 카드 → PostService 최신/조회수/댓글 기반 aggregate           |
| 실시간 인기 랭킹        | 조회수/추천수 Top 리스트                                                              | `TrendingPostService` (24시간 윈도우) → Redis/Caffeine 캐시, 썸네일/댓글수 표시                                 |
| 사이드바 위젯           | 코인 시세, 커뮤니티 공지, 광고 배너                                                  | CoinGecko 시세/변동률 위젯 + 운영자가 등록하는 `Notice`/`Promo` 엔티티 + 추후 광고 슬롯                        |
| 캘린더·이벤트           | 에어드롭/상장 이벤트 캘린더                                                          | `CalendarEvent` 게시판 기본 제공, 메인에서 다가오는 이벤트 3건 노출                                            |
| 등급/랭크 시스템        | 활동 포인트에 따른 단계                                                              | `MemberTier` 계산기(글/댓글/추천 가중치) + 프로필 뱃지, 포트폴리오용 통계 대시보드                             |
| 푸시 & 알림             | 댓글/추천/멘션 알림                                                                  | 1차: 알림센터 UI + 폴링 API, 2차: SSE/WebSocket 확장                                                           |
| 관리자 운영 도구        | 배너 교체, 공지, 게시판 관리                                                         | `/admin` 콘솔에 배너/공지 CRUD, 게시판 가중치 설정, 사용자 제재 관리                                           |

- **서비스 책임 정리**
  - `TrendingPostService`: 최근 24시간/7일 인기글 계산 (조회수·추천·댓글수 가중치), 배치 or on-demand + 캐시
  - `FeedAggregationService`: 게시판 별 최신 글 + 댓글 많은 글 묶음 제공, 메인 페이지 & 사이드 위젯 공급
  - `NoticeService` & `PromoBannerService`: 운영 공지, 배너, 이벤트 영역 데이터 관리
  - `MemberTierService`: 활동 지수 계산 → 프로필/사이드바 뱃지, 추후 랭킹 페이지 제공
  - `CalendarEventService`: 일정/에어드롭 데이터 CRUD + Soon(다가오는 일정) 조회 API

- **데이터 모델 보강**
  - `Notice`(title, content, priority, publishedAt, targetUrl)
  - `PromoBanner`(title, coverImageUrl, targetUrl, startAt, endAt, displayPosition)
  - `TrendingMetric`(postId, period, score, calculatedAt)
  - `MemberActivity`(memberId, postCount, commentCount, voteReceived, tier)
  - `CalendarEvent` 확장 필드: `category`, `reward`, `projectLogoUrl`

- **알고리즘 가이드**
  - Trend Score = `viewWeight * log(view+1) + voteWeight * voteScore + commentWeight * commentCount`
  - 24h/7d 구간 집계, 새 포스트 >= 기준점수 없으면 자동 보정 (age 를 감쇠 factor 로 사용)
  - 캐시 히트율 목표 90% 이상, 캐시 만료 60초, 새 글 발생 시 해당 게시판 키만 무효화

- **UX 기대효과**
  - 접속 직후 다양한 게시판 온보딩
  - 활동량이 높은 사용자/게시물에 대한 즉각적인 가시성
  - 운영자 편집/광고 공간 확보 → 포트폴리오에서 제품 기획/운영 역량 강조

---

## 4. 사용자 인터페이스 및 경험(Thymeleaf)

### 4.1 페이지 구조 및 레이아웃

| 템플릿                        | 설명                                                          |
| ----------------------------- | ------------------------------------------------------------- |
| `index.html`                  | 메인 허브: 히어로 배너, 트렌딩, 실시간 댓글, 코인 위젯, 공지 |
| `home/partials/*.html`        | 메인 페이지 카드 프래그먼트, 사이드 위젯                     |
| `market.html`                 | 상위 100개 코인 테이블                                        |
| `coins/detail.html`           | 코인 상세 정보 + 차트                                         |
| `boards/list.html`            | 게시판 목록                                                   |
| `boards/detail.html`          | 게시판 내 게시물 목록(페이지네이션)                          |
| `posts/detail.html`           | 게시물 상세 + 댓글 + 투표 + 북마크                            |
| `posts/form.html`             | 작성/수정 폼                                                  |
| `login.html`, `register.html` | 인증 페이지                                                   |
| `profile.html`                | 사용자 정보, 내가 쓴 글, 북마크, 활동 지수                   |
| `admin/*.html`                | 관리자 대시보드 + 배너/공지 관리                              |

- **프래그먼트**: `fragments/header.html`, `fragments/footer.html`, `fragments/sidebar.html`
- **폼 처리**: `th:field`, `th:errors`, CSRF 토큰 자동 포함

### 4.2 데이터 표현 및 동적 콘텐츠

- 서버 사이드 렌더링: Controller → Model → Thymeleaf (`th:each`, `th:if`, `th:classappend` 활용)
- 클라이언트 사이드 보강:
  - 실시간 가격 티커: `setInterval` + Fetch → `/api/simple-prices`
  - Chart.js/TradingView Lightweight Charts: `/api/coins/{id}/market-chart` 비동기 요청 후 렌더
  - AJAX 댓글/투표/북마크 처리로 UX 향상
- 국제화: `messages.properties` 로 다국어 지원 준비

### 4.3 UI/UX 고려사항

- 반응형 디자인: Bootstrap 5 또는 Tailwind CSS
- 페이지네이션: Spring Data `Page<T>` → Thymeleaf 페이지네이션 컴포넌트
- 접근성: ARIA 속성, 키보드 포커스, 명도 대비 준수
- 다크 모드(선택): CSS 변수 기반 테마 전환
- 폼 검증 UX: 클라이언트/서버 검증 메시지, 로딩 인디케이터

---

## 5. 단계별 개발 로드맵 및 쇼케이스 전략

### 5.1 단계별 개발 로드맵

| 단계                            | 목표                       | 주요 기능                                                                                                                                |
| ------------------------------- | -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| 1단계 (MVP, 4~6주)              | 배포 가능한 기본 기능 완성 | 프로젝트 부트스트랩, JWT 인증/인가, 자유게시판 CRUD, CoinGecko 상위 20개 목록 + 캐싱, 기본 템플릿, Docker/CI 초안                          |
| 2단계 (핵심 커뮤니티, 3~4주)    | 사용자 상호작용 강화       | 댓글, 다중 게시판, 투표/북마크, 프로필, 관리자 기초, OpenAPI 문서, 테스트 확장                                                           |
| 3단계 (데이터 통합 & UX, 3~4주) | 데이터 기반 기능 고도화    | 차트 렌더, 게시물 상속 구조, 뉴스/일정 게시판, AJAX UX, 캐시 모니터링                                                                    |
| 4단계 (코인판 수준 허브, 4~6주) | 메인 허브 및 트렌딩 강화   | Trending/Feed Aggregation 서비스, 메인 대시보드, 티커/배너, 활동 지수, 공지/배너 관리, 사이드 위젯, 관리자 퍼블리싱 툴                     |
| 5단계 (완성도 & 운영, 4주~)     | 운영 안정화 및 쇼케이스    | 뉴스 자동 수집, 관리자 패널 확장, 보안 강화, CI/CD 고도화, 클라우드 배포(HTTPS), README/블로그/데모 영상, 실시간 알림(SSE/WebSocket) 고도화 |

### 5.2 포트폴리오 쇼케이스 전략

- **GitHub README**
  - 프로젝트 개요, 기술 스택, 아키텍처 다이어그램, 설치/실행 가이드, 스크린샷/GIF, 주요 기능/하이라이트, 로드맵/향후 계획
- **문서 자료**
  - ERD, 시퀀스 다이어그램, API 명세서, 테이블 정의서 등 `docs/` 폴더에 정리
- **라이브 데모**
  - Render/Railway/AWS Elastic Beanstalk 등 무료/저비용 호스팅
  - 데모 계정 제공 (`user/demo`, `admin/demo`), JWT 흐름 시연
- **블로그 & 발표 자료**
  - CoinGecko 선택 이유, Rate Limit & 캐싱 전략, JWT vs 세션, 하이브리드 렌더링 경험
  - 개발 일지/회고 정리

---

## 6. 운영 및 유지 전략

- **테스트**: 단위(Testcontainers), 통합(MockMvc), E2E(Selenium/Playwright)
- **CI/CD**: GitHub Actions → Build/Test → Docker 이미지 → Staging/Prod 배포
- **로깅/모니터링**: Spring Boot Actuator, Micrometer + Prometheus/Grafana, Logback JSON + ELK
- **보안 점검**: OWASP Top10 체크, 보안 헤더 설정(CSP/HSTS), 의존성 취약점 스캐닝
- **성능 관리**: API 응답 시간 모니터링, 캐시 히트율, DB Slow Query 로깅

---

## 7. 후속 과제

- ERD 및 마이그레이션 스크립트 작성
- JWT 비밀 키 회전 정책 및 보안 문서화
- Chart.js & Thymeleaf 데모 페이지 프로토타입
- README 및 블로그 초안 작성
- 향후 기능 백로그 정리(알림, 웹소켓, 모바일 대응 등)

---

## 8. 요구사항 매핑

| 요구사항                                       | 상태         |
| ---------------------------------------------- | ------------ |
| 섹션 1: 프로젝트 설정/보안/데이터 모델 설계    | ✅ 반영      |
| 섹션 2: CoinGecko 기반 데이터 집계 & 캐싱 전략 | ✅ 반영      |
| 섹션 3: 커뮤니티 기능 및 데이터 통합           | ✅ 반영      |
| 섹션 4: UI/UX(Thymeleaf + JS) 설계             | ✅ 반영      |
| 섹션 5: 개발 로드맵 및 포트폴리오 전략         | ✅ 반영      |
| 결론/운영 전략/후속 과제                       | ✅ 보강 완료 |

---

## 9. 요약

본 문서는 암호화폐 커뮤니티 플랫폼 구축을 위한 아키텍처와 구현 로드맵을 포괄적으로 다루며, 보안(JWT 기반 인증), 데이터 집계(CoinGecko), 캐싱 전략(Caffeine), 커뮤니티 기능(게시판/댓글/투표/북마크), 하이브리드 렌더링(Thymeleaf + JS), 단계별 개발/쇼케이스 전략을 체계적으로 정리하였다. 이 계획서를 기반으로 개발을 진행하면 기능적 완성도뿐 아니라 아키텍처적 사고와 문서화 역량을 포트폴리오에 효과적으로 전달할 수 있다.

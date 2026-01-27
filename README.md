# 헥사고날 아키텍처 결제 도메인 서버

## 기술 스택
- Kotlin
- Spring Boot
- Spring Data JPA
- H2 / MariaDB (선택과제)
- Flyway (선택과제)
- Swagger (선택과제)
- WebClient (외부 PG 연동)

## 아키텍처

본 프로젝트는 헥사고날 아키텍처를 기반으로 멀티모듈 구조를 사용합니다.

- domain  
  - 순수 도메인 모델 및 수수료 계산 로직
- application  
  - 유스케이스, 포트 정의 및 비즈니스 흐름
- infrastructure  
  - JPA 기반 영속성 어댑터
- external  
  - 외부 PG 연동 어댑터
- bootstrap  
  - Spring Boot API 및 Controller

## 구현기능

### 결제 생성
- 외부 PG 승인 요청 후 결제 정보 저장
- 제휴사별 수수료 정책(percentage/ fixedFee / effective_from) 적용
- 카드 민감 정보는 마스킹 하여 부분 저장
### 결제 내역 조회 + 통계
- 필터 조건(partnerId, status, 기간)에 따른 조회
- 커서 기반 페이지네이션 적용
- 조회 결과와 동일한 집합을 기준으로 통계(count, totalAmount, totalNetAmount) 계산

### 수수료 정책 적용

- 제휴사별 수수료 정책은 effective_from 기준 가장 최근 정책을 적용합니다.
- 수수료 계산은 도메인 유틸리티(FeeCalculator)에서 수행합니다.
- 퍼센트 수수료는 HALF_UP 기준으로 반올림합니다.

## 테스트

핵심 비즈니스 로직에 대해 단위 테스트 및 통합 테스트를 작성했습니다.

### 주요 테스트 항목
- 결제 시 제휴사별 수수료 정책 적용 검증
- 퍼센트/정액 수수료 계산 정확성
- 커서 기반 페이지네이션 동작 검증
- 페이지 간 중복 데이터 미발생 검증
- 조회 결과와 통계 집합 일관성 검증

모든 테스트는 결정적이며, `./gradlew test`로 실행 가능합니다.

## 실행 방법

본 프로젝트는 실행 환경에 따라 `h2` 또는 `mariadb` 프로파일로 실행할 수 있습니다.

---

### H2 (인메모리 DB) 실행

로컬에서 별도 DB 없이 빠르게 실행할 수 있는 기본 실행 방식입니다.

```bash
SPRING_PROFILES_ACTIVE=h2 \
./gradlew :modules:bootstrap:api-payment-gateway:bootRun
```
### MariaDB 실행
```bash
docker compose up -d                      # 도커 컴포즈를 통해서 MariaDB 세팅
SPRING_PROFILES_ACTIVE=mariadb \
./gradlew :modules:bootstrap:api-payment-gateway:bootRun
```
### Swagger 테스트
실행 후 API 테스트는 http://localhost:8080/swagger-ui/index.html#/ url을 통해서 API 테스트 가능

## API 사양(요약)
1) 결제 생성
```
POST /api/v1/payments
{
  "partnerId": 2,
  "amount": 1000,
  "cardNumber": "1111-1111-1111-1111",
  "productName": "샘플",
  "birthDate": "19900101",
  "expiry": "1227",
  "password": "12"
}

200 OK
{
  "id": 18,
  "partnerId": 2,
  "amount": 1000,
  "appliedFeeRate": 0.09,
  "feeAmount": 290,
  "netAmount": 710,
  "cardLast4": "1111",
  "approvalCode": "12297800",
  "approvedAt": "2025-12-29 16:52:26",
  "status": "APPROVED",
  "createdAt": "2025-12-29 16:52:26"
}
```

2) 결제 조회(통계+커서)
```
'http://localhost:8080/api/v1/payments?partnerId=2&status=APPROVED&from=2025-01-01%2000%3A00%3A00&to=2026-12-31%2000%3A00%3A00&limit=20' \
{
  "items": [
    {
      "id": 18,
      "partnerId": 2,
      "amount": 1000,
      "appliedFeeRate": 0.09,
      "feeAmount": 290,
      "netAmount": 710,
      "cardLast4": "1111",
      "approvalCode": "12297800",
      "approvedAt": "2025-12-29 16:52:26",
      "status": "APPROVED",
      "createdAt": "2025-12-29 16:52:26"
    },
  ],
  "summary": {
    "count": 8,
    "totalAmount": 8000,
    "totalNetAmount": 5680
  },
  "nextCursor": "MTc2NzAyMTAzNDcwNTo0",
  "hasNext": true
}
```

## 변경 이력
- 하드코드 수수료 로직 제거 → 제휴사 별 수수료 정책 조회 후 계산 적용
- 커서 기반 페이지네이션 구현
- 외부 PG 연동 로직 분리 및 전략 선택 구조 적용
- 예외 처리 및 HTTP 상태 매핑 개선

## 선택 과제

- 추가 PG 어댑터 구조 설계 및 전략 선택 방식 적용(partnerId=1은 MockPGClient, partnerId=2는 TestPGClient 전략 선택)
- Swagger(OpenAPI) 문서화
- MariaDB + Flyway 마이그레이션 적용

## 참고자료
- [과제 내 연동 대상 API 문서](https://api-test-pg.bigs.im/docs/index.html)


# ZOOM NEAR (seenear)

가까운 사람들과 연결되는 소셜 링크 서비스. 폴더명은 `seenear`, 패키지·앱명은 `zoomnear`입니다.

## 로컬 실행 가이드

### 1. DB 실행

```bash
docker compose up -d
```

### 2. API 서버 실행

```bash
cd api && ./gradlew bootRun
```

### 3. 앱 실행

```bash
cd app && flutter run
```

## 폴더 구조

```
seenear/
├── api/        # Spring Boot 백엔드 (Kotlin/Gradle)
├── app/        # Flutter 모바일 앱
├── admin/      # 어드민 웹 (Day 2 작업 예정)
└── docs/       # 설계 문서
```

> 주의: 폴더는 `seenear`, 패키지·앱명은 `zoomnear`입니다.

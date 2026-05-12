# 로컬 개발 환경 셋업 가이드

## 사전 요구사항

| 도구 | 버전 | 설치 링크 |
|------|------|-----------|
| Java (Temurin) | 21 | [Adoptium](https://adoptium.net/temurin/releases/?version=21) |
| Docker Desktop | 최신 | [Docker](https://www.docker.com/products/docker-desktop/) |
| Node.js | 20 LTS | [Node.js](https://nodejs.org/en/download) |
| pnpm | 9.x | `npm install -g pnpm` |
| Flutter | 3.x stable | [Flutter](https://docs.flutter.dev/get-started/install) |

---

## 1. 저장소 클론

```bash
git clone https://github.com/your-org/seenear.git
cd seenear
```

---

## 2. 인프라 기동 (PostgreSQL)

```bash
docker compose up -d
```

접속 확인:

```bash
# macOS / Linux
psql postgresql://zoomnear:zoomnear@localhost:5432/zoomnear -c '\dt'

# Windows (PowerShell) — psql 없으면 Docker exec 활용
docker exec -it seenear-db-1 psql -U zoomnear -d zoomnear -c '\dt'
```

---

## 3. Spring Boot API (api/)

### macOS / Linux

```bash
cd api
./gradlew bootRun
```

### Windows (PowerShell)

```powershell
cd api
.\gradlew.bat bootRun
```

> 기본 포트: `http://localhost:8080`  
> `application-local.yml` — DB 커넥션 설정 확인

---

## 4. Flutter 앱 (app/)

```bash
cd app
flutter pub get
flutter run          # 연결된 디바이스/에뮬레이터
flutter run -d chrome  # 웹 모드
```

---

## 5. Next.js 어드민 (admin/)

```bash
cd admin
cp .env.local.example .env.local   # 필요 시 API URL 수정
pnpm install
pnpm dev
```

> 기본 포트: `http://localhost:3000`

---

## 6. IDE 설정

### IntelliJ IDEA (API 개발)

1. `api/` 폴더를 **Gradle 프로젝트**로 열기
2. **SDK**: Amazon Corretto 21 또는 Eclipse Temurin 21 설정
3. `Run > Edit Configurations` → Spring Boot → `ZoomnearApplication`
4. Active profile: `local`

### VS Code (Admin 개발)

권장 익스텐션:

```json
{
  "recommendations": [
    "bradlc.vscode-tailwindcss",
    "esbenp.prettier-vscode",
    "dbaeumer.vscode-eslint",
    "ms-vscode.vscode-typescript-next"
  ]
}
```

### VS Code (Flutter 개발)

```
확장: Dart, Flutter (Dart Code 팀)
```

---

## 7. 자주 쓰는 Make 명령 (macOS / Linux)

```bash
make up      # docker compose up -d
make api     # Spring Boot bootRun
make app     # Flutter run
make admin   # pnpm dev
make test    # 전체 테스트
```

> Windows에서는 Make 대신 각 명령을 직접 실행하세요.

---

## 8. 환경변수 주의사항

- `application-local.yml` — 로컬 전용, Git 커밋 O (더미 시크릿만 포함)
- `.env.local` — `admin/.gitignore`에 의해 무시됨, 커밋 X
- JWT 시크릿은 운영 배포 시 반드시 `ZOOMNEAR_JWT_SECRET` 환경변수로 교체

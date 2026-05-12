.PHONY: up down api app admin test

## docker compose up -d
up:
	docker compose up -d

## docker compose down
down:
	docker compose down

## Spring Boot dev server (api/)
api:
	cd api && ./gradlew bootRun

## Flutter run (app/)
app:
	cd app && flutter run

## Next.js dev server (admin/)
admin:
	cd admin && pnpm dev

## Run all tests
test:
	cd api && ./gradlew check
	@if [ -d app ]; then cd app && flutter test; fi
	@if [ -d admin ]; then cd admin && pnpm build; fi

.PHONY: help server server-test web web-install web-build dev

help:
	@echo "Asteroid Hunter monorepo commands"
	@echo ""
	@echo "  make server       Run Spring Boot API on default port (8080)"
	@echo "  make server-test  Run backend tests"
	@echo "  make web-install  Install frontend dependencies"
	@echo "  make web          Run Vite dev server"
	@echo "  make web-build    Build frontend"
	@echo "  make dev          Print parallel startup commands"

server:
	cd server && ./mvnw spring-boot:run

server-test:
	cd server && ./mvnw test

web-install:
	cd web && npm install

web:
	cd web && npm run dev

web-build:
	cd web && npm run build

dev:
	@echo "Run in separate terminals:"
	@echo "  make server"
	@echo "  make web"


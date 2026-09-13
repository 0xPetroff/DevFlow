SHELL := /bin/bash
JAVA_HOME := $(shell /usr/libexec/java_home -v 25 2>/dev/null)
export JAVA_HOME

.PHONY: help db-up db-down db-reset db-shell backend-run backend-test backend-build \
        frontend-install frontend-dev frontend-test frontend-build \
        images up down restart ps logs smoke infra-validate clean

TF_ENVS := staging production

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

db-up: ## Start PostgreSQL and wait until it is healthy
	docker compose up -d postgres
	@until [ "$$(docker inspect -f '{{.State.Health.Status}}' devflow-postgres 2>/dev/null)" = "healthy" ]; do sleep 1; done
	@echo "postgres ready"

db-down: ## Stop PostgreSQL
	docker compose stop postgres

db-reset: ## Destroy the database volume and re-create it from migrations
	docker compose down -v
	$(MAKE) db-up

db-shell: ## Open a psql shell
	docker exec -it devflow-postgres psql -U $${POSTGRES_USER:-devflow} -d $${POSTGRES_DB:-devflow}

backend-run: db-up ## Run the API locally against the containerised database
	set -a; . ./.env; set +a; \
	SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:$$POSTGRES_PORT/$$POSTGRES_DB" \
	SPRING_DATASOURCE_USERNAME="$$POSTGRES_USER" \
	SPRING_DATASOURCE_PASSWORD="$$POSTGRES_PASSWORD" \
	SPRING_PROFILES_ACTIVE=dev \
	mvn -f backend/pom.xml spring-boot:run

backend-test: ## Run the full backend test suite (Testcontainers needs Docker running)
	mvn -f backend/pom.xml verify

backend-build: ## Package the backend jar
	mvn -f backend/pom.xml clean package

frontend-install: ## Install frontend dependencies
	cd frontend && npm ci

frontend-dev: ## Run the Vite dev server
	cd frontend && npm run dev

frontend-test: ## Run frontend tests
	cd frontend && npm run test

frontend-build: ## Type-check, lint and build the frontend
	cd frontend && npm run typecheck && npm run lint && npm run build

images: ## Build both container images
	docker compose build

up: ## Build and start the whole stack, waiting until every service is healthy
	docker compose up -d --build --wait

down: ## Stop the whole stack
	docker compose down

restart: ## Recreate the stack from the current images, keeping the database volume
	docker compose down
	docker compose up -d --wait

ps: ## Show the stack's containers and their health
	docker compose ps

logs: ## Tail stack logs
	docker compose logs -f

smoke: ## Check a running stack answers on every entry point
	@set -a; . ./.env; set +a; \
	curl -fsS "http://localhost:$${BACKEND_PORT:-8080}/actuator/health/readiness" >/dev/null && echo "api readiness ok"; \
	curl -fsS "http://localhost:$${FRONTEND_PORT:-8081}/healthz" >/dev/null && echo "web healthz ok"; \
	curl -fsS -o /dev/null "http://localhost:$${FRONTEND_PORT:-8081}/" && echo "web index ok"; \
	test "$$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$${FRONTEND_PORT:-8081}/api/auth/me")" = "401" \
		&& echo "web to api proxy ok"

infra-validate: ## Validate and format-check the Terraform for every environment
	tofu -chdir=infrastructure/aws/terraform fmt -recursive -check
	@for env in $(TF_ENVS); do \
		dir=infrastructure/aws/terraform/envs/$$env; \
		echo "validating $$env"; \
		tofu -chdir=$$dir init -backend=false -input=false >/dev/null || exit 1; \
		tofu -chdir=$$dir validate || exit 1; \
	done

clean: ## Remove build output
	mvn -f backend/pom.xml clean
	rm -rf frontend/dist frontend/node_modules/.vite

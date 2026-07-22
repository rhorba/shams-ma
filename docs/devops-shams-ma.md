# DevOps Foundation: Shams.ma
**Architecture**: docs/architecture-shams-ma.md
**Security**: docs/security-shams-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: DevOps/DevSecOps

## 1. Environment Strategy
| Environment | Purpose | Deploy Trigger |
|---|---|---|
| local | Development | `docker-compose up` (manual) |
| staging | QA / Preview | Auto on PR merge to `main` |
| production | Live users | Manual tag / approved release |

## 2. CI Pipeline (GitHub Actions)
```yaml
name: ci
on: [push, pull_request]
permissions:
  contents: read
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '21' }
      - name: Lint (Checkstyle/Spotless)
        run: ./mvnw spotless:check
      - name: Test + Coverage (JaCoCo)
        run: ./mvnw test jacoco:report
      - name: Coverage gate (>= 80%)
        run: ./mvnw jacoco:check
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - run: npm ci
      - run: npm run lint
      - run: npm test -- --coverage
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 } # full history so gitleaks fingerprints (commit-pinned) stay stable across pushes
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '21', cache: 'maven', cache-dependency-path: backend/pom.xml }
      - name: Pre-populate Maven cache (avoids Trivy hitting Maven Central live and getting rate-limited)
        working-directory: ./backend
        run: ./mvnw -q dependency:resolve
      - uses: returntocorp/semgrep-action@v1
        with: { config: p/owasp-top-ten }
      - uses: aquasecurity/trivy-action@master
        with: { scan-type: fs, severity: "CRITICAL,HIGH", exit-code: 1 }
      - name: Install gitleaks
        run: |
          GITLEAKS_VERSION=$(curl -sSL "https://api.github.com/repos/gitleaks/gitleaks/releases/latest" | grep '"tag_name"' | cut -d '"' -f4 | tr -d v)
          curl -sSL "https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}/gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz" | tar -xz gitleaks
          sudo mv gitleaks /usr/local/bin/
      - name: Secrets scan
        run: gitleaks detect --source . --verbose
  build:
    needs: [backend, frontend, security]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build backend image
        run: docker build -t shamsma-api:${{ github.sha }} ./backend
      - name: Build frontend image
        run: docker build -t shamsma-web:${{ github.sha }} ./frontend
      - name: Scan backend image
        uses: aquasecurity/trivy-action@master
        with: { scan-type: image, image-ref: "shamsma-api:${{ github.sha }}", severity: "CRITICAL,HIGH", exit-code: 1 }
      - name: Scan frontend image
        uses: aquasecurity/trivy-action@master
        with: { scan-type: image, image-ref: "shamsma-web:${{ github.sha }}", severity: "CRITICAL,HIGH", exit-code: 1 }
  deploy-staging:
    needs: [build]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploy to staging"  # adapt to hosting target
```

## 3. Infrastructure
- **Hosting**: single-region managed container platform (e.g., a single VM running Docker Compose, or a managed container service) — per System Design, no multi-region/Kubernetes needed at this scale
- **Compute**: Docker containers — 1-2 replicas of the Spring Boot API behind a reverse proxy/load balancer; React app served as static build via nginx or a CDN-backed static host
- **Database**: managed PostgreSQL + PostGIS (daily automated backups, per Database doc)
- **Secrets**: environment variables at deploy time via the hosting platform's secret store; `.env.example` documents required vars, `.env` never committed
- **Monitoring**: structured JSON logs → hosting-provider log aggregation; health check endpoint (`/actuator/health`) polled by load balancer and uptime monitor

## 4. Security Scanning Gates
| Scanner | Scan Type | Fail Threshold |
|---|---|---|
| Semgrep | SAST — code vulnerabilities (OWASP Top 10 ruleset) | Critical findings |
| Trivy | SCA — dependency CVEs (Maven + npm) + container image scan | Critical CVEs |
| Gitleaks | Secrets detection | Any secrets found |

## 5. Docker Setup

### Backend (Spring Boot)
```dockerfile
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=builder --chown=app:app /app/target/*.jar app.jar
USER app
HEALTHCHECK --interval=30s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### Frontend (React)
```dockerfile
FROM node:20-slim AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
```

### docker-compose (dev)
```yaml
services:
  api:
    build: ./backend
    ports: ["8080:8080"]
    env_file: .env
    depends_on:
      db: { condition: service_healthy }
  web:
    build: ./frontend
    ports: ["4173:80"]
    depends_on: [api]
  db:
    image: postgis/postgis:16-3.4-alpine
    environment:
      POSTGRES_DB: shamsma
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes: ["pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER}"]
      interval: 5s
volumes:
  pgdata:
```

## 6. Monitoring Baseline
| Signal | Tool | Alert Threshold |
|---|---|---|
| Logs | hosting-provider log aggregation (structured JSON) | Error rate > 1% for 5 min |
| Metrics | Spring Boot Actuator + Micrometer | Latency p99 > 2s for 5 min |
| Uptime | health check ping (`/actuator/health`) | 2 consecutive failures |
| Payment reconciliation | nightly job comparing CMI transaction log vs. local `payments` table (per Architecture technical risk: webhook loss) | Any mismatch → alert |

# Documentation Platform

A unified platform for managing technical documentation with GitHub sync, semantic search, and version control.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)
![React](https://img.shields.io/badge/React-19-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## Features

- **GitHub Repository Sync** - Automatically fetch and index documentation from GitHub releases
- **AI Semantic Search** - Powered by Gemini Embedding + pgvector for intelligent document discovery
- **Multi-version Support** - Manage multiple versions of documentation libraries
- **API Key Management** - Generate and manage API access keys
- **OAuth2 Authentication** - Secure access with OAuth2/OIDC integration
- **Apple Liquid Glass Design** - Modern, elegant UI with glassmorphism effects

## Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | React 19 + Vite 6 + React Router 7 |
| Backend | Java 25 + Spring Boot 4.0.2 + Spring AI 2.0.0-M2 |
| Database | PostgreSQL 18 + pgvector |
| AI/ML | Gemini Embedding (768 dimensions) |
| Auth | OAuth2 Stateless (JWT) |

## Quick Start

### Prerequisites

- Java 25
- Node.js 20+
- Docker Desktop

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/samzhu/documentation-platform.git
cd documentation-platform

# 2. Start backend (includes PostgreSQL via Docker Compose)
cd backend
../gradlew bootRun

# 3. Start frontend (in a new terminal)
cd frontend
npm install
npm run dev
```

### Access

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api |

## Project Structure

```
documentation-platform/
├── frontend/              # React frontend application
│   ├── src/
│   │   ├── components/    # Shared components (Sidebar, RequireAuth, etc.)
│   │   ├── pages/         # Page components (Dashboard, Libraries, Search, etc.)
│   │   ├── hooks/         # Custom hooks (useAuth)
│   │   ├── services/      # API and auth services
│   │   └── styles/        # CSS modules (Liquid Glass design system)
│   └── package.json
├── backend/               # Spring Boot backend application
│   ├── src/main/java/
│   │   └── .../platform/
│   │       ├── config/        # Spring configuration
│   │       ├── domain/        # Domain models
│   │       ├── repository/    # Spring Data JDBC repositories
│   │       ├── service/       # Business logic
│   │       ├── web/api/       # REST Controllers
│   │       ├── infrastructure/# External integrations (GitHub, Parsers)
│   │       └── scheduler/     # Scheduled tasks
│   └── build.gradle
├── docs/
│   └── PRD.md             # Product Requirements Document
└── CLAUDE.md              # AI Development Guidelines
```

## Main Features

### Library Management
Create, edit, and delete documentation libraries with support for multiple versions. Each library can be configured with different source types (GitHub, Local).

### GitHub Sync
Automatically sync documentation from GitHub repositories:
- Fetch releases and tags
- Parse Markdown, AsciiDoc, and HTML files
- Chunk documents and generate vector embeddings

### Semantic Search
Three search modes available:
- **Hybrid** - Combines semantic and full-text search (default)
- **Semantic** - Pure vector similarity search
- **Fulltext** - Traditional keyword search

### API Key Management
Generate and manage API keys for external integrations (e.g., MCP Server):
- Secure key generation with `dmcp_` prefix
- BCrypt hashed storage
- Key prefix for fast lookup

## API Overview

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/libraries` | GET | List all libraries |
| `/api/libraries` | POST | Create a new library |
| `/api/libraries/{id}` | GET | Get library details |
| `/api/libraries/{id}/sync` | POST | Trigger sync |
| `/api/search` | GET | Search documents |
| `/api/api-keys` | GET | List API keys |
| `/api/api-keys` | POST | Create API key |
| `/api/api-keys/{id}` | DELETE | Revoke API key |

## Configuration

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `platform-google-api-key` | Google GenAI API Key | Yes |
| `platform-db-url` | Database connection URL | Yes |
| `platform-db-username` | Database username | Yes |
| `platform-db-password` | Database password | Yes |
| `platform-oauth2-client-id` | OAuth2 Client ID | Production |
| `platform-oauth2-client-secret` | OAuth2 Client Secret | Production |

### Profiles

| Profile | Description |
|---------|-------------|
| `local` | Local development, OAuth2 disabled |
| `oauth2` | Production with OAuth2 enabled |

## Development

### Frontend Development
```bash
cd frontend
npm run dev        # Start dev server with hot reload
npm run build      # Build for production
npm run lint       # Run ESLint
```

### Backend Development
```bash
cd backend
../gradlew bootRun           # Run application
../gradlew test              # Run unit tests
../gradlew integrationTest   # Run integration tests
../gradlew build             # Build JAR
```

### Documentation
- Frontend Guide: `frontend/CLAUDE.md`
- Backend Guide: `backend/CLAUDE.md`
- Design System: `frontend/docs/DESIGN_LANGUAGE.md`

## Manual Deployment

Build and run the application locally with OAuth2 authentication.

### Step 1: Build Frontend
```bash
cd frontend
npm run clean       # Clear previous build artifacts
npm install
npm run build
```

### Step 2: Copy Frontend to Backend
```bash
rm -rf backend/src/main/resources/static/*
cp -r frontend/dist/* backend/src/main/resources/static/
```

### Step 3: Configure Secrets
Edit `backend/config/application-secrets.properties`:
```properties
# Google AI API Key (required)
platform-google-api-key=your-google-api-key

# OAuth2 Configuration (for production)
platform-oauth2-issuer-uri=https://your-auth-server.com/realms/your-realm
platform-oauth2-client-id=your-client-id
platform-oauth2-client-secret=your-client-secret
```

### Step 4: Start Backend
```bash
cd backend
./gradlew clean bootRun --no-daemon
```

### Step 5: Access
| Service | URL |
|---------|-----|
| Application | http://localhost:8080 |
| API | http://localhost:8080/api |
| Health Check | http://localhost:8080/actuator/health |

## Docker Deployment

### Quick Start
```bash
docker run -d \
  --name documentation-platform \
  -p 8080:8080 \
  -e platform-google-api-key=your-api-key \
  -e platform-db-url=jdbc:postgresql://db:5432/mydatabase \
  -e platform-db-username=myuser \
  -e platform-db-password=secret \
  ghcr.io/samzhu/documentation-platform:latest
```

### Docker Compose
```yaml
services:
  platform:
    image: ghcr.io/samzhu/documentation-platform:latest
    ports:
      - "8080:8080"
    environment:
      platform-google-api-key: ${PLATFORM_GOOGLE_API_KEY}
      platform-db-url: jdbc:postgresql://db:5432/mydatabase
      platform-db-username: myuser
      platform-db-password: secret
    depends_on:
      - db

  db:
    image: pgvector/pgvector:pg17
    environment:
      POSTGRES_DB: mydatabase
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: secret
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

## License

MIT License

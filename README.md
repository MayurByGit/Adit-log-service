# Audit Log Service

## Overview

A Spring Boot REST API prototype for recording audit events, querying them, detecting changes through a SHA-256 hash chain, archiving events, exporting audit records with optional payload redaction, and generating compliance reports.

## Technology Stack

- Java 17
- Spring Boot 4.1.1
- Spring Data JPA
- H2 database
- Maven
- SHA-256
- JUnit-based automated tests

## Prerequisites

- JDK 17
- Git
- Maven Wrapper included in the repository
- An internet connection for the initial Maven dependency download, unless dependencies are already cached

## Run the Application

From the repository root:

```bash
./mvnw spring-boot:run
```

On Windows Command Prompt:

```bat
mvnw.cmd spring-boot:run
```

## Run Tests

On Windows:

```bat
mvnw.cmd clean test
```

On macOS/Linux:

```bash
./mvnw clean test
```

## API Documentation

See `docs/api-design.md` for the implemented endpoint contracts and examples.

## Architecture

See `docs/architecture.md`.

## Requirements and Scenarios

- `docs/requirements.md`
- `docs/scenario-c-assumptions.md`
- `docs/engineering-summary.md`

## Risks and Limitations

See `docs/risk-analysis.md`. The project is a prototype and must not be considered production-ready without the security, persistence, verification and operational controls described there.

## Validation Status

Record the actual build and test results here only after executing the commands. Include the date tested, test counts, any failures, and endpoints manually verified.

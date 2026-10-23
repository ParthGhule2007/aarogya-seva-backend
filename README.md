# Rural Healthcare Platform

A backend-first digital healthcare system designed for rural populations. Built as a Java Spring Boot modular monolith.

## Status: Modules 1, 2 & 3 Completed & Verified
* **Module 1: Healthcare Facility & PostGIS Geospatial Care Discovery** (Complete & Verified)
* **Module 2: Patient & Healthcare Case Management** (Complete & Verified)
* **Module 3: Symptom Intake & Management** (Complete & Verified)
* **Module 4: Core Screening & AI Decision Support Module** (Planned next)
* **Module 5: Referral Workflow Module** (Planned)
* **Module 6: Case Aggregation, Facility Alerts & SOS Module** (Planned)

## Prerequisites
* Java 17 or higher
* Maven 3.8+
* Supabase PostgreSQL instance (PostGIS enabled) or Docker for Testcontainers

## Configuration
Copy `.env.example` to `.env` and set your credentials:
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://db.<your-supabase-project-ref>.supabase.co:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=YOUR_SUPABASE_DB_PASSWORD
```
> [!CAUTION]
> Never commit `.env` or plain-text database passwords to version control.

## Running the Application
```bash
mvn spring-boot:run
```

## Running Tests
```bash
mvn test
```

## Seed / Demo Data Notice
> [!WARNING]
> Seeded facility records (`V2__seed_rural_facilities.sql`) are **DEMO / DEVELOPMENT DATA — NOT VERIFIED REAL-WORLD HEALTHCARE DATA**.
> All demo records are tagged with `is_demo_data = true` and must be cleared or replaced with verified data before clinical operation.

## API Documentation
Once running, explore the OpenAPI Swagger UI at:
`http://localhost:8081/swagger-ui.html`

## Postman Collection
A complete Postman Collection is located at:
[`docs/postman/Rural_Healthcare_Platform_Postman_Collection.json`](file:///d:/mit-india/Grand-Finale/docs/postman/Rural_Healthcare_Platform_Postman_Collection.json)
Import this JSON file into Postman for instant end-to-end API testing.

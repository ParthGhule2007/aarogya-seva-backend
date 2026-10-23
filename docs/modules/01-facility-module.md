# Module 1: Healthcare Facility & PostGIS Geospatial Discovery Module

## 1. Overview
The Facility Module maintains the master catalog of healthcare facilities (Sub-Centres, PHCs, CHCs, Sub-District Hospitals, and District Hospitals) and their medical service capabilities. It powers location-based care discovery for the screening and referral workflows using database-level PostGIS spatial queries.

---

## 2. Public Java Contracts (For Other Modules)
Any module needing facility verification or nearby discovery must inject `FacilityQueryService`:

```java
package com.ruralhealthcare.facility.service;

public interface FacilityQueryService {
    Optional<FacilityDto> getFacilityById(UUID facilityId);
    
    List<FacilityRecommendationDto> findNearbyFacilities(
            double latitude,
            double longitude,
            double maxRadiusKm,
            Set<MedicalServiceCode> requiredServices,
            int limit
    );

    boolean isFacilityOperational(UUID facilityId);
}
```

---

## 3. Database Schema & Flyway Migrations
Managed via Flyway scripts for Supabase PostgreSQL:
* **`V1__init_facility_schema.sql`**:
  - Enables PostGIS spatial extension (`CREATE EXTENSION IF NOT EXISTS postgis;`).
  - Creates `healthcare_facilities` with a first-class `location GEOGRAPHY(Point, 4326)` column.
  - Attaches a PostgreSQL trigger (`trg_update_facility_location`) to automatically synchronize `location` from `latitude` and `longitude`.
  - Creates spatial **GiST index** (`idx_facilities_location_gist`) on `location` for radius filtering.
  - Creates `facility_service_offerings` mapping medical service codes (`GENERAL_OPD`, `EMERGENCY`, `MATERNAL_HEALTH`, `PEDIATRIC_CARE`, `INFECTIOUS_DISEASE`, `SURGICAL_CARE`, `DIAGNOSTIC_LAB`).
  - Includes `is_demo_data BOOLEAN NOT NULL DEFAULT FALSE` to distinguish synthetic demo data from verified providers.
* **`V2__seed_rural_facilities.sql`**:
  - Inserts demo facilities in the Lucknow/Mohanlalganj rural belt with `is_demo_data = TRUE`.
  - Clearly documented as **DEMO / DEVELOPMENT DATA — NOT VERIFIED REAL-WORLD HEALTHCARE DATA**.
  - Can be removed prior to production via: `DELETE FROM healthcare_facilities WHERE is_demo_data = TRUE;`.

---

## 4. PostGIS Database-Level Proximity Search
Proximity queries execute directly in PostgreSQL using spatial indexing:
```sql
SELECT f.id, f.name, f.facility_type, f.latitude, f.longitude,
       f.address_line1, f.address_line2, f.village_town, f.sub_district,
       f.district, f.state, f.pincode, f.contact_number, f.emergency_number,
       f.is_operational, f.is_demo_data,
       ROUND(CAST(ST_Distance(f.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) / 1000.0 AS numeric), 2) AS distanceKm
FROM healthcare_facilities f
WHERE f.is_operational = true
  AND ST_DWithin(f.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)
  AND (
      :filterByServices = false OR
      EXISTS (
          SELECT 1 FROM facility_service_offerings s
          WHERE s.facility_id = f.id
            AND s.is_available = true
            AND s.service_code IN (:serviceCodes)
      )
  )
ORDER BY distanceKm ASC
LIMIT :limit;
```

---

## 5. REST API Catalog

| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/facilities/search` | Proximity search by coordinates, radius, and services | Public / CHW / Patient |
| `GET` | `/api/v1/facilities/{id}` | Retrieve facility details by ID | Public / CHW / Patient |
| `POST` | `/api/v1/facilities` | Onboard a new facility with services | Admin / Health Officer |
| `PUT` | `/api/v1/facilities/{id}` | Update facility details | Admin / Health Officer |
| `PATCH` | `/api/v1/facilities/{id}/status` | Toggle operational status | Admin / Health Officer |
| `DELETE` | `/api/v1/facilities/{id}` | Remove facility | Admin / Health Officer |

---

## 6. Removed Unapproved Scoring & Ordering Rules
* The previously introduced formula `(60% service match + 40% proximity)` and `suitabilityScore` have been **completely removed**.
* Results are ordered strictly and transparently by distance ascending (`distanceKm ASC`).
* Any composite scoring or AI-driven ranking requires explicit future requirement definition and approval.

package com.ruralhealthcare.facility.repository;

import java.util.UUID;

/**
 * Interface-based projection for database-level PostGIS distance search results.
 * Distances are calculated directly in PostgreSQL/PostGIS on the spheroid in kilometers.
 */
public interface FacilityDistanceProjection {

    UUID getId();

    String getName();

    String getFacilityType();

    Double getLatitude();

    Double getLongitude();

    String getAddressLine1();

    String getAddressLine2();

    String getVillageTown();

    String getSubDistrict();

    String getDistrict();

    String getState();

    String getPincode();

    String getContactNumber();

    String getEmergencyNumber();

    Boolean getIsOperational();

    Boolean getIsDemoData();

    Double getDistanceKm();
}

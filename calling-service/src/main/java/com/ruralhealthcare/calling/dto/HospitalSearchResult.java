package com.ruralhealthcare.calling.dto;

import java.util.ArrayList;
import java.util.List;

public class HospitalSearchResult {

    private boolean found;
    private String queryLocation;
    private String message;
    private String provider;
    private Double searchRadiusKm;
    private List<HospitalItem> hospitals = new ArrayList<>();

    public HospitalSearchResult() {
    }

    public HospitalSearchResult(boolean found, String queryLocation, String message, String provider, List<HospitalItem> hospitals) {
        this(found, queryLocation, message, provider, null, hospitals);
    }

    public HospitalSearchResult(boolean found, String queryLocation, String message, String provider, Double searchRadiusKm, List<HospitalItem> hospitals) {
        this.found = found;
        this.queryLocation = queryLocation;
        this.message = message;
        this.provider = provider;
        this.searchRadiusKm = searchRadiusKm;
        this.hospitals = hospitals != null ? hospitals : new ArrayList<>();
    }

    public static HospitalSearchResult notFound(String queryLocation, String message, String provider) {
        return new HospitalSearchResult(false, queryLocation, message, provider, null, List.of());
    }

    public static HospitalSearchResult notFound(String queryLocation, String message, String provider, Double searchRadiusKm) {
        return new HospitalSearchResult(false, queryLocation, message, provider, searchRadiusKm, List.of());
    }

    public static HospitalSearchResult success(String queryLocation, List<HospitalItem> hospitals, String provider) {
        String msg = hospitals.isEmpty()
                ? "No verified healthcare facilities found for the requested location."
                : "Found " + hospitals.size() + " verified healthcare facility(ies).";
        return new HospitalSearchResult(!hospitals.isEmpty(), queryLocation, msg, provider, null, hospitals);
    }

    public static HospitalSearchResult success(String queryLocation, String message, List<HospitalItem> hospitals, String provider, Double searchRadiusKm) {
        return new HospitalSearchResult(true, queryLocation, message, provider, searchRadiusKm, hospitals);
    }

    public boolean isFound() {
        return found;
    }

    public String getQueryLocation() {
        return queryLocation;
    }

    public String getMessage() {
        return message;
    }

    public String getProvider() {
        return provider;
    }

    public List<HospitalItem> getHospitals() {
        return hospitals;
    }

    public Double getSearchRadiusKm() {
        return searchRadiusKm;
    }

    public void setSearchRadiusKm(Double searchRadiusKm) {
        this.searchRadiusKm = searchRadiusKm;
    }

    public static class HospitalItem {
        private String name;
        private String address;
        private Double distanceKm;
        private String contactNumber;
        private String emergencyNumber;
        private String facilityType;
        private Double latitude;
        private Double longitude;

        public HospitalItem() {
        }

        public HospitalItem(String name, String address, Double distanceKm, String contactNumber, String emergencyNumber, String facilityType, Double latitude, Double longitude) {
            this.name = name;
            this.address = address;
            this.distanceKm = distanceKm;
            this.contactNumber = contactNumber;
            this.emergencyNumber = emergencyNumber;
            this.facilityType = facilityType;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public Double getDistanceKm() {
            return distanceKm;
        }

        public String getContactNumber() {
            return contactNumber;
        }

        public String getEmergencyNumber() {
            return emergencyNumber;
        }

        public String getFacilityType() {
            return facilityType;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }
    }
}

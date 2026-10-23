package com.ruralhealthcare.surveillance.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OutbreakZoneDto {

    private String zoneId;
    private String areaName;
    private String villageTown;
    private String district;
    private String state;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private String zoneColor; // RED, YELLOW, GREEN
    private String zoneLabel;
    private int totalCases;
    private int activeCases;
    private int highRiskCases;
    private double averageRiskScore;
    private List<String> topDiseases = new ArrayList<>();
    private List<String> reportedSymptoms = new ArrayList<>();
    private String nearestFacilityName;
    private String advisoryMessage;
    private Instant evaluatedAt;

    public OutbreakZoneDto() {
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getVillageTown() {
        return villageTown;
    }

    public void setVillageTown(String villageTown) {
        this.villageTown = villageTown;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getZoneColor() {
        return zoneColor;
    }

    public void setZoneColor(String zoneColor) {
        this.zoneColor = zoneColor;
    }

    public String getZoneLabel() {
        return zoneLabel;
    }

    public void setZoneLabel(String zoneLabel) {
        this.zoneLabel = zoneLabel;
    }

    public int getTotalCases() {
        return totalCases;
    }

    public void setTotalCases(int totalCases) {
        this.totalCases = totalCases;
    }

    public int getActiveCases() {
        return activeCases;
    }

    public void setActiveCases(int activeCases) {
        this.activeCases = activeCases;
    }

    public int getHighRiskCases() {
        return highRiskCases;
    }

    public void setHighRiskCases(int highRiskCases) {
        this.highRiskCases = highRiskCases;
    }

    public double getAverageRiskScore() {
        return averageRiskScore;
    }

    public void setAverageRiskScore(double averageRiskScore) {
        this.averageRiskScore = averageRiskScore;
    }

    public List<String> getTopDiseases() {
        return topDiseases;
    }

    public void setTopDiseases(List<String> topDiseases) {
        this.topDiseases = topDiseases;
    }

    public List<String> getReportedSymptoms() {
        return reportedSymptoms;
    }

    public void setReportedSymptoms(List<String> reportedSymptoms) {
        this.reportedSymptoms = reportedSymptoms;
    }

    public String getNearestFacilityName() {
        return nearestFacilityName;
    }

    public void setNearestFacilityName(String nearestFacilityName) {
        this.nearestFacilityName = nearestFacilityName;
    }

    public String getAdvisoryMessage() {
        return advisoryMessage;
    }

    public void setAdvisoryMessage(String advisoryMessage) {
        this.advisoryMessage = advisoryMessage;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}

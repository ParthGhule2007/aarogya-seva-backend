export type OutbreakZoneColor = 'RED' | 'YELLOW' | 'GREEN';

export interface OutbreakZoneDto {
  zoneId: string;
  areaId?: string;
  areaName: string;
  villageTown?: string;
  district: string;
  state: string;
  latitude: number | null;
  longitude: number | null;
  distanceKm: number | null;
  zoneColor: OutbreakZoneColor;
  zoneLabel?: string;
  severityLabel?: string;
  totalCases: number;
  activeCases: number;
  highRiskCases: number;
  averageRiskScore: number;
  topDiseases: string[];
  reportedSymptoms: string[];
  nearestFacilityName?: string;
  advisoryMessage?: string;
  advisory?: string;
  evaluatedAt?: string;
  lastUpdated?: string;
}

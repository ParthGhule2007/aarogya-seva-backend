import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  TouchableOpacity,
  Linking,
  Alert,
} from 'react-native';
import * as Location from 'expo-location';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { facilityApi } from '../../api/facilityApi';
import { surveillanceApi } from '../../api/surveillanceApi';
import { FacilityRecommendationDto } from '../../types/facility';
import { OutbreakZoneDto } from '../../types/surveillance';
import { Header } from '../../components/common/Header';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { LoadingState } from '../../components/common/LoadingState';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { EmergencySosButton } from '../../components/common/EmergencySosButton';
import { DemoModeBanner } from '../../components/common/DemoModeBanner';
import { OutbreakLeafletMap } from '../../components/map/OutbreakLeafletMap';
import { IS_DEMO_MODE, MOCK_LOCATION } from '../../config/apiConfig';

export const FacilitiesScreen: React.FC = () => {
  const { colors } = useTheme();
  const { t } = useLanguage();

  const [facilities, setFacilities] = useState<FacilityRecommendationDto[]>([]);
  const [outbreakZones, setOutbreakZones] = useState<OutbreakZoneDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [locationNotice, setLocationNotice] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [usedDemoData, setUsedDemoData] = useState(false);

  // Default to Aerocity (28.5562, 77.1000) or MOCK_LOCATION
  const [currentCoords, setCurrentCoords] = useState<{ latitude: number; longitude: number }>(
    MOCK_LOCATION || { latitude: 28.5562, longitude: 77.1000 }
  );

  const fetchNearbyFacilities = useCallback(async () => {
    setError(null);
    setUsedDemoData(false);

    let lat = currentCoords.latitude;
    let lon = currentCoords.longitude;

    if (MOCK_LOCATION) {
      lat = MOCK_LOCATION.latitude;
      lon = MOCK_LOCATION.longitude;
      setCurrentCoords(MOCK_LOCATION);
      setLocationNotice(`Mock location active (${lat.toFixed(4)}, ${lon.toFixed(4)})`);
    } else {
      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (status === 'granted') {
          const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
          if (loc?.coords) {
            lat = loc.coords.latitude;
            lon = loc.coords.longitude;
            setCurrentCoords({ latitude: lat, longitude: lon });
            setLocationNotice(null);
            setPermissionDenied(false);
          }
        } else {
          setPermissionDenied(true);
          setLocationNotice('Location permission not granted. Showing Aerocity / New Delhi regional network.');
        }
      } catch {
        setPermissionDenied(true);
        setLocationNotice('Unable to acquire GPS. Showing Aerocity / New Delhi regional network.');
      }
    }

    try {
      const [results, zones] = await Promise.all([
        facilityApi.searchNearbyFacilities({
          latitude: lat,
          longitude: lon,
          radiusKm: 50,
          limit: 15,
        }),
        surveillanceApi.getOutbreakZones(lat, lon).catch((zErr) => {
          console.error('Outbreak zones fetch error:', zErr);
          return [] as OutbreakZoneDto[];
        }),
      ]);

      setOutbreakZones(Array.isArray(zones) ? zones : []);

      if (Array.isArray(results) && results.length > 0) {
        setFacilities(results);
      } else {
        const all = await facilityApi.getAllFacilities();
        const mapped: FacilityRecommendationDto[] = (all || []).map((f) => ({
          facilityId: f.id,
          name: f.name || f.facilityName || 'Rural Primary Health Center',
          facilityType: f.facilityType || 'PHC',
          villageTown: f.villageTown || f.district || 'Rural Center',
          district: f.district || 'District',
          state: f.state || 'Delhi',
          pincode: f.pincode || '110037',
          contactNumber: f.contactNumber || '108',
          emergencyNumber: f.emergencyNumber || '108',
          isOperational: f.isOperational ?? true,
          isDemoData: f.isDemoData ?? false,
          latitude: f.latitude || 28.5562,
          longitude: f.longitude || 77.1000,
          availableServices: f.availableServices || [],
        }));
        setFacilities(mapped);
      }
    } catch (err: any) {
      console.error('Failed to load facilities or outbreak zones:', err);
      setError(err.message || 'Unable to load healthcare network.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [currentCoords.latitude, currentCoords.longitude]);

  useEffect(() => {
    fetchNearbyFacilities();
  }, [fetchNearbyFacilities]);

  const onRefresh = () => {
    setRefreshing(true);
    fetchNearbyFacilities();
  };

  const handleCall = (number?: string) => {
    if (!number) {
      Alert.alert('No Contact Number', 'Emergency contact is not available for this facility.');
      return;
    }
    Linking.openURL(`tel:${number}`).catch(() => {
      Alert.alert('Dialer Error', `Cannot dial ${number}`);
    });
  };

  const handleDirections = (lat?: number, lon?: number, name?: string) => {
    if (lat == null || lon == null) {
      Alert.alert('No Coordinates', 'Location coordinates are not registered for this facility.');
      return;
    }
    const label = encodeURIComponent(name || 'Hospital');
    const url = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lon}&destination_place_id=${label}`;
    Linking.openURL(url).catch(() => {
      Alert.alert('Navigation Error', 'Could not open map navigation.');
    });
  };

  const stripDemoPrefix = (name?: string) => (name || '').replace(/\[DEMO\]\s*/i, '').trim();

  return (
    <View style={{ flex: 1, backgroundColor: colors.bgApp }}>
      <Header />
      {usedDemoData && <DemoModeBanner message="DEMO MODE: Showing seeded regional healthcare data" />}

      <ScrollView
        contentContainerStyle={styles.scrollContainer}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={[colors.primary]}
            tintColor={colors.primary}
          />
        }
      >
        {/* Leaflet OpenStreetMap View */}
        <OutbreakLeafletMap
          userLocation={currentCoords}
          locationPermissionDenied={permissionDenied}
          facilities={facilities}
          zones={outbreakZones}
          height={320}
        />

        {/* Outbreak / Disease-Spread Surveillance Section */}
        {outbreakZones.length > 0 && (
          <View style={styles.surveillanceSection}>
            <View style={styles.sectionHeaderRow}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                <Ionicons name="shield-checkmark-outline" size={18} color={colors.primary} />
                <Text style={[styles.sectionTitle, { color: colors.textMain }]}>{t('epiRiskZones')}</Text>
              </View>
              <View style={[styles.liveBadge, { backgroundColor: colors.riskHighBg }]}>
                <Text style={[styles.liveText, { color: colors.riskHigh }]}>{t('liveSurveillance')}</Text>
              </View>
            </View>
            <Text style={[styles.sectionSubtitle, { color: colors.textSubtle }]}>
              Real-time classification based on active clinical cases, reported symptoms, and AI triage
            </Text>

            {outbreakZones.slice(0, 5).map((zone) => {
              const isRed = zone.zoneColor === 'RED';
              const isYellow = zone.zoneColor === 'YELLOW';
              const badgeBg = isRed ? colors.riskHighBg : isYellow ? '#FEF3C7' : colors.riskLowBg;
              const badgeBorder = isRed ? colors.riskHighBorder : isYellow ? '#FDE68A' : colors.riskLowBorder;
              const badgeText = isRed ? colors.riskHigh : isYellow ? '#B45309' : colors.riskLow;

              return (
                <Card
                  key={zone.zoneId}
                  style={[
                    styles.zoneCard,
                    {
                      borderColor: isRed ? colors.riskHighBorder : badgeBorder,
                      borderLeftWidth: isRed ? 5 : 1,
                      borderLeftColor: isRed ? colors.riskHigh : badgeBorder,
                    },
                  ]}
                >
                  <View style={styles.facRow}>
                    <View style={{ flex: 1 }}>
                      <Text style={[styles.zoneName, { color: colors.textMain }]}>{zone.areaName || `${zone.villageTown}, ${zone.district}`}</Text>
                      <Text style={[styles.facLocation, { color: colors.textSubtle }]}>
                        {zone.distanceKm != null ? `${zone.distanceKm.toFixed(1)} km away • ` : ''}
                        Active: {zone.activeCases} (High Risk: {zone.highRiskCases})
                      </Text>
                    </View>
                    <View
                      style={{
                        backgroundColor: badgeBg,
                        borderColor: badgeBorder,
                        borderWidth: 1,
                        paddingHorizontal: 10,
                        paddingVertical: 4,
                        borderRadius: 999,
                      }}
                    >
                      <Text style={{ color: badgeText, fontWeight: '800', fontSize: 11 }}>
                        {isRed ? t('redZone') : isYellow ? t('yellowZone') : t('greenZone')}
                      </Text>
                    </View>
                  </View>

                  {zone.reportedSymptoms && zone.reportedSymptoms.length > 0 && (
                    <Text style={{ fontSize: 12, color: colors.textMain, fontWeight: '600', marginTop: 4 }}>
                      Top Symptoms: {zone.reportedSymptoms.slice(0, 3).join(', ')}
                    </Text>
                  )}

                  <Text style={{ fontSize: 12, color: colors.textSubtle, marginTop: 4 }}>
                    {zone.advisoryMessage || zone.advisory}
                  </Text>
                </Card>
              );
            })}
          </View>
        )}

        <View style={styles.topBanner}>
          <Text style={[styles.title, { color: colors.textMain }]}>{t('nearbyCareCenters')}</Text>
          <Text style={[styles.subtitle, { color: colors.textSubtle }]}>
            Geospatial care network matching nearest Sub-Centres, PHCs & District Hospitals
          </Text>
        </View>

        {locationNotice ? (
          <View style={[styles.noticeBox, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight }]}>
            <Ionicons name="information-circle-outline" size={16} color={colors.primary} />
            <Text style={[styles.noticeText, { color: colors.textMain }]}>{locationNotice}</Text>
          </View>
        ) : null}

        {error ? <ErrorState message={error} onRetry={fetchNearbyFacilities} /> : null}

        {loading ? (
          <LoadingState message="Discovering nearby hospitals via PostGIS..." />
        ) : facilities.length === 0 ? (
          <EmptyState
            title="No Facilities Found in Range"
            description="No primary healthcare centers discovered within 50 km."
            actionTitle="Refresh Network"
            onAction={fetchNearbyFacilities}
          />
        ) : (
          facilities.map((fac) => (
            <Card key={fac.facilityId || fac.id} style={styles.facCard}>
              <View style={styles.facRow}>
                <View style={{ flex: 1 }}>
                  <Text style={[styles.facName, { color: colors.textMain }]}>{stripDemoPrefix(fac.name)}</Text>
                  <Text style={[styles.facLocation, { color: colors.textSubtle }]}>
                    {fac.villageTown}, {fac.district}
                  </Text>
                </View>
                <StatusBadge status={fac.facilityType} />
              </View>

              <View style={styles.detailsRow}>
                <View style={[styles.distanceBadge, { backgroundColor: colors.primaryLight }]}>
                  <Text style={[styles.distanceText, { color: colors.primary }]}>
                    {fac.distanceKm ? `${fac.distanceKm.toFixed(1)} km away` : 'Nearby'}
                  </Text>
                </View>

                {fac.isOperational && (
                  <View style={[styles.opBadge, { backgroundColor: colors.riskLowBg }]}>
                    <Text style={[styles.opText, { color: colors.riskLow }]}>Open 24/7</Text>
                  </View>
                )}
              </View>

              <View style={styles.cardActions}>
                <Button
                  title={t('callHospital')}
                  variant="outline"
                  onPress={() => handleCall(fac.emergencyNumber || fac.contactNumber)}
                  style={styles.actionBtn}
                />
                <Button
                  title={t('getDirections')}
                  onPress={() => handleDirections(fac.latitude, fac.longitude, fac.name)}
                  style={styles.actionBtn}
                />
              </View>
            </Card>
          ))
        )}
      </ScrollView>

      <EmergencySosButton />
    </View>
  );
};

const styles = StyleSheet.create({
  scrollContainer: {
    padding: 16,
    paddingBottom: 90,
  },
  topBanner: {
    marginBottom: 12,
  },
  title: {
    fontSize: 20,
    fontWeight: '800',
  },
  subtitle: {
    fontSize: 13,
    marginTop: 2,
  },
  noticeBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    marginBottom: 12,
  },
  noticeText: {
    fontSize: 12,
    fontWeight: '500',
    flex: 1,
  },
  surveillanceSection: {
    marginBottom: 20,
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '800',
  },
  liveBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  liveText: {
    fontSize: 9,
    fontWeight: '900',
    letterSpacing: 0.5,
  },
  sectionSubtitle: {
    fontSize: 12,
    marginBottom: 10,
  },
  zoneCard: {
    padding: 12,
    marginBottom: 10,
    borderRadius: 12,
    borderWidth: 1,
  },
  zoneName: {
    fontSize: 15,
    fontWeight: '700',
  },
  facCard: {
    padding: 14,
    marginBottom: 12,
    borderRadius: 14,
  },
  facRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  facName: {
    fontSize: 16,
    fontWeight: '700',
  },
  facLocation: {
    fontSize: 13,
    marginTop: 2,
  },
  detailsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 10,
  },
  distanceBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  distanceText: {
    fontSize: 12,
    fontWeight: '700',
  },
  opBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  opText: {
    fontSize: 11,
    fontWeight: '700',
  },
  cardActions: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 14,
    borderTopWidth: 1,
    borderTopColor: '#F1F5F9',
    paddingTop: 10,
  },
  actionBtn: {
    flex: 1,
  },
});

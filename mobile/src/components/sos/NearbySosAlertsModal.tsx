import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  Modal,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Linking,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { sosApi } from '../../api/sosApi';
import { SosStatusResponse, getSosSenderName } from '../../types/sos';
import { Button } from '../common/Button';
import { StatusBadge } from '../common/StatusBadge';

interface NearbySosAlertsModalProps {
  visible: boolean;
  onClose: () => void;
}

function computeDistanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function formatRelativeTime(dateStr?: string): string {
  if (!dateStr) return '';
  const diffMs = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

export const NearbySosAlertsModal: React.FC<NearbySosAlertsModalProps> = ({ visible, onClose }) => {
  const { colors } = useTheme();
  const { user } = useAuth();

  const [alerts, setAlerts] = useState<SosStatusResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [userCoords, setUserCoords] = useState<{ lat: number; lon: number } | null>(null);

  const fetchActiveAlerts = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setLoading(true);

    try {
      // Get device GPS location
      let currentLat = user?.latitude ?? 18.6750;
      let currentLon = user?.longitude ?? 73.8980;

      try {
        const { status } = await Location.getForegroundPermissionsAsync();
        if (status === 'granted') {
          const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
          currentLat = loc.coords.latitude;
          currentLon = loc.coords.longitude;
        }
      } catch (e) {
        console.warn('GPS location lookup skipped in SOS modal:', e);
      }

      setUserCoords({ lat: currentLat, lon: currentLon });

      const data = await sosApi.getActiveSosAlerts();
      const genuineActive = Array.isArray(data)
        ? data.filter((s) => s && s.sosId && s.status !== 'RESOLVED' && !s.sosId.startsWith('mock-'))
        : [];

      // Sort by proximity to current user
      genuineActive.sort((a, b) => {
        const distA =
          a.latitude && a.longitude ? computeDistanceKm(currentLat, currentLon, a.latitude, a.longitude) : 9999;
        const distB =
          b.latitude && b.longitude ? computeDistanceKm(currentLat, currentLon, b.latitude, b.longitude) : 9999;
        return distA - distB;
      });

      setAlerts(genuineActive);
    } catch (err: any) {
      console.warn('Failed to load active nearby SOS alerts:', err?.message);
      setAlerts([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [user?.latitude, user?.longitude]);

  useEffect(() => {
    if (visible) {
      fetchActiveAlerts(false);
    }
  }, [visible, fetchActiveAlerts]);

  return (
    <Modal visible={visible} animationType="slide" transparent={false} onRequestClose={onClose}>
      <View style={[styles.container, { backgroundColor: colors.bgApp }]}>
        {/* Header */}
        <View style={[styles.header, { backgroundColor: colors.bgSurface, borderBottomColor: colors.borderLight }]}>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
            <View style={[styles.iconBox, { backgroundColor: '#FEE2E2' }]}>
              <Ionicons name="warning" size={20} color="#DC2626" />
            </View>
            <View>
              <Text style={[styles.headerTitle, { color: colors.textMain }]}>Emergency SOS Around You</Text>
              <Text style={[styles.headerSubtitle, { color: colors.textSubtle }]}>
                {alerts.length} genuine emergency event(s) active
              </Text>
            </View>
          </View>
          <TouchableOpacity onPress={onClose} style={styles.closeButton}>
            <Ionicons name="close" size={24} color={colors.textMain} />
          </TouchableOpacity>
        </View>

        {/* Content */}
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={() => fetchActiveAlerts(true)}
              colors={[colors.primary]}
              tintColor={colors.primary}
            />
          }
        >
          {loading ? (
            <View style={styles.centerContainer}>
              <ActivityIndicator size="large" color="#DC2626" />
              <Text style={[styles.statusText, { color: colors.textSubtle }]}>Scanning for nearby emergency triggers...</Text>
            </View>
          ) : alerts.length === 0 ? (
            <View style={[styles.emptyCard, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight }]}>
              <Ionicons name="shield-checkmark-outline" size={48} color={colors.primary} />
              <Text style={[styles.emptyTitle, { color: colors.textMain }]}>No Active Emergencies Nearby</Text>
              <Text style={[styles.emptySubtitle, { color: colors.textSubtle }]}>
                There are no open SOS emergency triggers around your current location. All community alerts are resolved.
              </Text>
            </View>
          ) : (
            alerts.map((sos) => {
              const distanceKm =
                userCoords && sos.latitude && sos.longitude
                  ? computeDistanceKm(userCoords.lat, userCoords.lon, sos.latitude, sos.longitude)
                  : null;

              const isSelf = sos.patientId === user?.patientId || sos.patientId === user?.id;
              const senderName = getSosSenderName(sos.message, isSelf ? (user?.displayName || user?.fullName) : undefined);

              return (
                <View
                  key={sos.sosId}
                  style={[
                    styles.alertCard,
                    {
                      backgroundColor: colors.bgSurface,
                      borderColor: '#FECACA',
                    },
                  ]}
                >
                  <View style={styles.alertHeaderRow}>
                    <View style={{ flex: 1, paddingRight: 8 }}>
                      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                        <Text style={[styles.alertSeverity, { color: '#DC2626' }]}>
                          ACTIVE EMERGENCY
                        </Text>
                        {distanceKm != null && (
                          <View style={[styles.distancePill, { backgroundColor: '#FEE2E2' }]}>
                            <Ionicons name="navigate" size={11} color="#DC2626" />
                            <Text style={[styles.distanceText, { color: '#DC2626' }]}>
                              {distanceKm.toFixed(1)} km away
                            </Text>
                          </View>
                        )}
                      </View>
                      <Text style={[styles.alertTime, { color: colors.textSubtle }]}>
                        Triggered {formatRelativeTime(sos.triggeredAt)} • {senderName}
                      </Text>
                    </View>
                    <StatusBadge status={sos.status} />
                  </View>

                  <Text style={[styles.alertMessage, { color: colors.textMain }]}>
                    {sos.message || 'Emergency assistance requested via Aarogya Seva'}
                  </Text>

                  {sos.latitude != null && sos.longitude != null && (
                    <View style={styles.metaRow}>
                      <Text style={[styles.coordText, { color: colors.textSubtle }]}>
                        GPS: {sos.latitude.toFixed(4)}, {sos.longitude.toFixed(4)}
                      </Text>
                      <Text style={[styles.coordText, { color: colors.textSubtle }]}>
                        SMS/Push: {sos.recipientsNotified || 0} notified
                      </Text>
                    </View>
                  )}

                  <View style={styles.actionRow}>
                    {sos.latitude != null && sos.longitude != null && (
                      <Button
                        title="View Location on Map"
                        onPress={() =>
                          Linking.openURL(
                            `https://www.google.com/maps/search/?api=1&query=${sos.latitude},${sos.longitude}`
                          )
                        }
                        variant="outline"
                        style={{ flex: 1, minHeight: 38 }}
                      />
                    )}
                  </View>
                </View>
              );
            })
          )}
        </ScrollView>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  iconBox: {
    width: 36,
    height: 36,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    fontSize: 16,
    fontWeight: '700',
  },
  headerSubtitle: {
    fontSize: 12,
    marginTop: 2,
  },
  closeButton: {
    padding: 6,
  },
  scrollContent: {
    padding: 16,
    gap: 12,
  },
  centerContainer: {
    paddingVertical: 48,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  statusText: {
    fontSize: 13,
  },
  emptyCard: {
    padding: 24,
    borderRadius: 12,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    marginTop: 24,
  },
  emptyTitle: {
    fontSize: 16,
    fontWeight: '700',
    marginTop: 6,
  },
  emptySubtitle: {
    fontSize: 13,
    textAlign: 'center',
    lineHeight: 18,
  },
  alertCard: {
    padding: 14,
    borderRadius: 10,
    borderWidth: 1.5,
    gap: 8,
  },
  alertHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  alertSeverity: {
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  distancePill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  distanceText: {
    fontSize: 10,
    fontWeight: '700',
  },
  alertTime: {
    fontSize: 11,
    marginTop: 2,
  },
  alertMessage: {
    fontSize: 14,
    fontWeight: '600',
    lineHeight: 20,
  },
  metaRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 2,
  },
  coordText: {
    fontSize: 11,
  },
  actionRow: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 4,
  },
});

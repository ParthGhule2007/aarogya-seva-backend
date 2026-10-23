import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import {
  View,
  Text,
  Modal,
  StyleSheet,
  TouchableOpacity,
  Linking,
} from 'react-native';
import * as SecureStore from 'expo-secure-store';
import { setNotificationHandler } from 'expo-notifications/build/NotificationsHandler';
import { scheduleNotificationAsync } from 'expo-notifications/build/scheduleNotificationAsync';
import { requestPermissionsAsync } from 'expo-notifications/build/NotificationPermissions';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from './AuthContext';
import { useTheme } from './ThemeContext';
import { sosApi } from '../api/sosApi';
import { NearbyAlertDto, getSosSenderName } from '../types/sos';
import { MOCK_LOCATION } from '../config/apiConfig';
import { Button } from '../components/common/Button';

const STORAGE_KEY = 'aarogya_share_location';
const DEFAULT_COORDS = { latitude: 18.5204, longitude: 73.8567 };

interface LocationSharingContextType {
  isSharingEnabled: boolean;
  setIsSharingEnabled: (val: boolean) => Promise<void>;
  activeAlert: NearbyAlertDto | null;
  dismissAlert: (sosId: string) => void;
}

const LocationSharingContext = createContext<LocationSharingContextType | undefined>(undefined);

export const LocationSharingProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { user } = useAuth();
  const { colors } = useTheme();

  const [isSharingEnabled, setIsSharingEnabledState] = useState(true);
  const [activeAlert, setActiveAlert] = useState<NearbyAlertDto | null>(null);
  const [dismissedAlertIds, setDismissedAlertIds] = useState<Set<string>>(new Set());

  // Safe notification initialization for Expo Go (local notifications only)
  useEffect(() => {
    try {
      setNotificationHandler({
        handleNotification: async () => ({
          shouldShowAlert: true,
          shouldPlaySound: true,
          shouldSetBadge: true,
          shouldShowBanner: true,
          shouldShowList: true,
        }),
      });
      requestPermissionsAsync().catch(() => {});
    } catch {
      // safe fallback if notification engine restricted
    }
  }, []);

  // Load persisted preference
  useEffect(() => {
    (async () => {
      try {
        const val = await SecureStore.getItemAsync(STORAGE_KEY);
        if (val !== null) {
          setIsSharingEnabledState(val === 'true');
        }
      } catch {
        // default true
      }
    })();
  }, []);

  const setIsSharingEnabled = async (val: boolean) => {
    setIsSharingEnabledState(val);
    try {
      await SecureStore.setItemAsync(STORAGE_KEY, val ? 'true' : 'false');
    } catch {
      // ignore
    }
  };

  const getEffectiveUserId = useCallback((): string | null => {
    return user?.id || user?.associatedEntityId || user?.patientId || null;
  }, [user]);

  const getEffectiveCoordinates = useCallback(() => {
    if (MOCK_LOCATION) {
      return MOCK_LOCATION;
    }
    return DEFAULT_COORDS;
  }, []);

  // Send periodic live location every ~30s
  const sendLocationPing = useCallback(async () => {
    const userId = getEffectiveUserId();
    if (!userId) return;

    const coords = getEffectiveCoordinates();
    try {
      await sosApi.updateLocation({
        userId,
        latitude: coords.latitude,
        longitude: coords.longitude,
        emergencyAlertOptIn: isSharingEnabled,
        userType: (user?.role as any) || 'PATIENT',
      });
    } catch (err: any) {
      // Background ping error - silent
      console.warn('Live location update failed:', err?.message);
    }
  }, [getEffectiveUserId, getEffectiveCoordinates, isSharingEnabled, user?.role]);

  // Poll for nearby emergency alerts every ~10s
  const pollNearbyAlerts = useCallback(async () => {
    const userId = getEffectiveUserId();
    if (!userId || user?.role === 'DOCTOR') return;

    try {
      const alerts = await sosApi.getUserAlerts(userId);
      if (Array.isArray(alerts) && alerts.length > 0) {
        // Pick the first un-dismissed alert
        const freshAlert = alerts.find(
          (a) => !dismissedAlertIds.has(a.sosId) && a.status !== 'RESOLVED'
        );

        if (freshAlert) {
          setActiveAlert((prev) => {
            if (!prev || prev.sosId !== freshAlert.sosId) {
              const senderName = getSosSenderName(freshAlert.message);
              // Trigger local push notification
              scheduleNotificationAsync({
                content: {
                  title: `EMERGENCY: ${senderName} Needs Help!`,
                  body: `${senderName} (${freshAlert.distanceKm ? freshAlert.distanceKm.toFixed(1) + ' km away' : 'nearby'}): ${freshAlert.message || 'Immediate medical aid requested'}`,
                  data: { sosId: freshAlert.sosId },
                },
                trigger: null,
              }).catch(() => {});
            }
            return freshAlert;
          });
        }
      }
    } catch (err: any) {
      // Background poll error - silent
    }
  }, [getEffectiveUserId, dismissedAlertIds]);

  // Location send interval (~30s)
  useEffect(() => {
    sendLocationPing();
    const locInterval = setInterval(() => {
      sendLocationPing();
    }, 30000);
    return () => clearInterval(locInterval);
  }, [sendLocationPing]);

  // Alert poll interval (~10s)
  useEffect(() => {
    pollNearbyAlerts();
    const alertInterval = setInterval(() => {
      pollNearbyAlerts();
    }, 10000);
    return () => clearInterval(alertInterval);
  }, [pollNearbyAlerts]);

  const dismissAlert = (sosId: string) => {
    setDismissedAlertIds((prev) => new Set(prev).add(sosId));
    setActiveAlert(null);
  };

  const handleNavigate = () => {
    if (!activeAlert) return;
    const url = `https://www.google.com/maps/dir/?api=1&destination=${activeAlert.latitude},${activeAlert.longitude}`;
    Linking.openURL(url).catch(() => {});
  };

  return (
    <LocationSharingContext.Provider
      value={{
        isSharingEnabled,
        setIsSharingEnabled,
        activeAlert,
        dismissAlert,
      }}
    >
      {children}

      {/* Full-Screen Nearby Emergency Alert Modal */}
      {activeAlert && (
        <Modal
          visible={!!activeAlert}
          transparent
          animationType="fade"
          onRequestClose={() => dismissAlert(activeAlert.sosId)}
        >
          <View style={styles.modalBackdrop}>
            <View style={[styles.alertCard, { backgroundColor: colors.bgSurface }]}>
              {/* Header Badge */}
              <View style={styles.alertHeaderRow}>
                <View style={styles.iconCircle}>
                  <Ionicons name="warning" size={28} color="#DC2626" />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={styles.alertTitle}>Emergency Alert: {getSosSenderName(activeAlert.message)}</Text>
                  <Text style={styles.alertSub}>Community First-Responder Alert</Text>
                </View>
              </View>

              {/* Distance & Coordinates Box */}
              <View style={[styles.detailBox, { backgroundColor: '#FEF2F2', borderColor: '#FCA5A5' }]}>
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                  <Ionicons name="location" size={18} color="#DC2626" />
                  <Text style={styles.distanceText}>
                    {activeAlert.distanceKm != null
                      ? `${activeAlert.distanceKm.toFixed(1)} km away from you`
                      : 'Immediate vicinity'}
                  </Text>
                </View>

                <Text style={styles.messageText}>
                  "{activeAlert.message || 'Emergency assistance requested via Aarogya Seva'}"
                </Text>

                <Text style={styles.coordsText}>
                  GPS: {activeAlert.latitude.toFixed(4)}, {activeAlert.longitude.toFixed(4)}
                  {activeAlert.triggeredAt ? ` • ${new Date(activeAlert.triggeredAt).toLocaleTimeString()}` : ''}
                </Text>
              </View>

              {/* Actions: Navigate via Google Maps & Dismiss */}
              <View style={styles.actionRow}>
                <Button
                  title="Dismiss"
                  onPress={() => dismissAlert(activeAlert.sosId)}
                  variant="outline"
                  style={{ flex: 1, minHeight: 46 }}
                />
                <Button
                  title="Navigate (Maps)"
                  onPress={handleNavigate}
                  variant="primary"
                  style={{ flex: 2, minHeight: 46, backgroundColor: '#DC2626' }}
                />
              </View>
            </View>
          </View>
        </Modal>
      )}
    </LocationSharingContext.Provider>
  );
};

export const useLocationSharing = (): LocationSharingContextType => {
  const context = useContext(LocationSharingContext);
  if (!context) {
    throw new Error('useLocationSharing must be used within LocationSharingProvider');
  }
  return context;
};

const styles = StyleSheet.create({
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.75)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  alertCard: {
    width: '100%',
    borderRadius: 20,
    padding: 20,
    elevation: 8,
    borderWidth: 2,
    borderColor: '#DC2626',
  },
  alertHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 16,
  },
  iconCircle: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#FEE2E2',
    justifyContent: 'center',
    alignItems: 'center',
  },
  alertTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: '#DC2626',
  },
  alertSub: {
    fontSize: 12,
    color: '#64748B',
    marginTop: 2,
  },
  detailBox: {
    padding: 14,
    borderRadius: 12,
    borderWidth: 1,
    marginBottom: 20,
  },
  distanceText: {
    fontSize: 15,
    fontWeight: '800',
    color: '#DC2626',
  },
  messageText: {
    fontSize: 14,
    color: '#1E293B',
    marginTop: 8,
    fontStyle: 'italic',
  },
  coordsText: {
    fontSize: 11,
    color: '#64748B',
    marginTop: 8,
  },
  actionRow: {
    flexDirection: 'row',
    gap: 10,
  },
});

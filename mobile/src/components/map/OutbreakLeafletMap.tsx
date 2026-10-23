import React, { useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  Platform,
} from 'react-native';
import { WebView } from 'react-native-webview';
import { Ionicons } from '@expo/vector-icons';
import { useLanguage } from '../../i18n/LanguageContext';
import { OutbreakZoneDto } from '../../types/surveillance';
import { FacilitySummaryDto } from '../../types/facility';

interface OutbreakLeafletMapProps {
  userLocation: { latitude: number; longitude: number } | null;
  locationPermissionDenied?: boolean;
  facilities: FacilitySummaryDto[];
  zones: OutbreakZoneDto[];
  onSelectFacility?: (facility: FacilitySummaryDto) => void;
  onSelectZone?: (zone: OutbreakZoneDto) => void;
  onLocationSelect?: (coords: { latitude: number; longitude: number }) => void;
  height?: number;
}

export const OutbreakLeafletMap: React.FC<OutbreakLeafletMapProps> = ({
  userLocation,
  locationPermissionDenied = false,
  facilities = [],
  zones = [],
  onSelectFacility,
  onSelectZone,
  onLocationSelect,
  height = 320,
}) => {
  const { t } = useLanguage();
  const webViewRef = useRef<any>(null);
  const WebViewComponent = WebView as any;
  const [selectedItem, setSelectedItem] = useState<{
    type: 'FACILITY' | 'ZONE';
    data: any;
  } | null>(null);
  const [tileErrorNotice, setTileErrorNotice] = useState<string | null>(null);

  const defaultCenter = userLocation || { latitude: 28.5562, longitude: 77.1000 };

  // Prepare map data payload for webview
  const facilitiesData = facilities
    .filter((f) => f.latitude != null && f.longitude != null && !isNaN(Number(f.latitude)) && !isNaN(Number(f.longitude)))
    .slice(0, 15)
    .map((f) => ({
      id: f.facilityId,
      name: f.name,
      type: f.facilityTypeDisplayName || f.facilityType,
      lat: Number(f.latitude),
      lon: Number(f.longitude),
      distanceKm: f.distanceKm,
      address: f.addressSummary || f.villageTown || '',
      emergencyNumber: f.emergencyNumber || f.contactNumber || '',
    }));

  const zonesData = zones
    .filter((z) => z.latitude != null && z.longitude != null && !isNaN(Number(z.latitude)) && !isNaN(Number(z.longitude)))
    .map((z) => ({
      id: z.zoneId,
      name: z.areaName || `${z.villageTown}, ${z.district}`,
      color: z.zoneColor === 'RED' ? '#DC2626' : z.zoneColor === 'YELLOW' ? '#EAB308' : '#16A34A',
      zoneColor: z.zoneColor,
      label: z.zoneLabel,
      lat: Number(z.latitude),
      lon: Number(z.longitude),
      totalCases: z.totalCases,
      activeCases: z.activeCases,
      highRiskCases: z.highRiskCases,
      avgRisk: z.averageRiskScore,
      topDiseases: z.topDiseases || [],
      reportedSymptoms: z.reportedSymptoms || [],
      advisory: z.advisoryMessage,
    }));

  const htmlContent = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <script>
    window.onerror = function(msg, url, line) {
      if (window.ReactNativeWebView) {
        window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'JS_ERROR', error: String(msg), line: line }));
      }
    };
  </script>
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, sans-serif; }
    .hospital-pin {
      background: #0F766E;
      color: white;
      border-radius: 50%;
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      font-size: 14px;
      border: 2px solid white;
      box-shadow: 0 2px 6px rgba(0,0,0,0.3);
    }
  </style>
</head>
<body>
  <div id="map"></div>
  <script>
    try {
      var userLat = ${userLocation ? userLocation.latitude : 'null'};
      var userLon = ${userLocation ? userLocation.longitude : 'null'};
      var centerLat = ${defaultCenter.latitude};
      var centerLon = ${defaultCenter.longitude};

      var map = L.map('map', { zoomControl: false }).setView([centerLat, centerLon], 12);

      // OpenStreetMap Standard Tiles (No API key required)
      var osmLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      });

      // Esri World Street Map Tiles fallback
      var esriLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
        maxZoom: 19,
        attribution: 'Tiles &copy; Esri'
      });

      var tileErrors = 0;
      var activeTileLayer = osmLayer;
      activeTileLayer.addTo(map);

      osmLayer.on('tileerror', function() {
        tileErrors++;
        if (tileErrors > 2 && activeTileLayer === osmLayer) {
          map.removeLayer(osmLayer);
          activeTileLayer = esriLayer;
          esriLayer.addTo(map);
          if (window.ReactNativeWebView) {
            window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'TILE_FALLBACK' }));
          }
        }
      });

      esriLayer.on('tileerror', function() {
        if (window.ReactNativeWebView) {
          window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'TILE_ERROR' }));
        }
      });

      map.on('click', function(e) {
        if (window.ReactNativeWebView && e.latlng) {
          window.ReactNativeWebView.postMessage(JSON.stringify({
            type: 'MAP_TAP',
            latitude: e.latlng.lat,
            longitude: e.latlng.lng
          }));
        }
      });

      var bounds = [];

      // User Blue Dot
      if (userLat !== null && userLon !== null) {
        L.circleMarker([userLat, userLon], {
          radius: 8,
          color: '#1D4ED8',
          fillColor: '#3B82F6',
          fillOpacity: 1.0,
          weight: 3
        }).addTo(map).bindPopup("<b>Your Current Location</b>");
        bounds.push([userLat, userLon]);
      }

      // Outbreak Zones Circles (5 km radius)
      var zones = ${JSON.stringify(zonesData)};
      zones.forEach(function(z) {
        if (z.lat && z.lon) {
          var circle = L.circle([z.lat, z.lon], {
            radius: 5000,
            color: z.color,
            fillColor: z.color,
            fillOpacity: 0.22,
            weight: 2
          }).addTo(map);

          circle.on('click', function() {
            window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'ZONE_CLICK', data: z }));
          });
        }
      });

      // Hospital Markers
      var hospitals = ${JSON.stringify(facilitiesData)};
      hospitals.forEach(function(h) {
        if (h.lat && h.lon) {
          bounds.push([h.lat, h.lon]);
          var icon = L.divIcon({
            className: 'custom-icon',
            html: '<div class="hospital-pin">H</div>',
            iconSize: [28, 28],
            iconAnchor: [14, 14]
          });

          var marker = L.marker([h.lat, h.lon], { icon: icon }).addTo(map);
          marker.on('click', function() {
            window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'FACILITY_CLICK', data: h }));
          });
        }
      });

      if (bounds.length > 1) {
        map.fitBounds(bounds.slice(0, 5), { padding: [40, 40] });
      }

      function recenter() {
        if (userLat !== null && userLon !== null) {
          map.setView([userLat, userLon], 13);
        } else {
          map.setView([centerLat, centerLon], 12);
        }
      }

      window.recenterMap = recenter;
    } catch (e) {
      if (window.ReactNativeWebView) {
        window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'JS_ERROR', error: String(e) }));
      }
    }
  </script>
</body>
</html>
  `;

  const handleMessage = (event: any) => {
    try {
      const msg = JSON.parse(event.nativeEvent.data);
      if (msg.type === 'FACILITY_CLICK') {
        setSelectedItem({ type: 'FACILITY', data: msg.data });
        if (onSelectFacility) onSelectFacility(msg.data);
      } else if (msg.type === 'ZONE_CLICK') {
        setSelectedItem({ type: 'ZONE', data: msg.data });
        if (onSelectZone) onSelectZone(msg.data);
      } else if (msg.type === 'MAP_TAP') {
        if (onLocationSelect && msg.latitude && msg.longitude) {
          onLocationSelect({ latitude: Number(msg.latitude), longitude: Number(msg.longitude) });
        }
      } else if (msg.type === 'TILE_FALLBACK') {
        setTileErrorNotice('Switched to backup Esri street map tiles');
      } else if (msg.type === 'TILE_ERROR') {
        setTileErrorNotice('Street tiles offline. Showing GPS markers & surveillance zones.');
      } else if (msg.type === 'JS_ERROR') {
        console.warn('Leaflet Internal JS Error:', msg.error, msg.line);
      }
    } catch {
      // ignore
    }
  };

  const handleRecenter = () => {
    if (webViewRef.current) {
      webViewRef.current.injectJavaScript(`window.recenterMap && window.recenterMap(); true;`);
    }
  };

  return (
    <View style={[styles.container, { height }]}>
      {tileErrorNotice && (
        <View style={[styles.permissionWarning, { backgroundColor: '#FEF3C7', borderBottomColor: '#F59E0B' }]}>
          <Ionicons name="information-circle-outline" size={16} color="#D97706" />
          <Text style={[styles.permissionText, { color: '#92400E' }]}>
            {tileErrorNotice}
          </Text>
        </View>
      )}

      {locationPermissionDenied && (
        <View style={styles.permissionWarning}>
          <Ionicons name="location-outline" size={16} color="#DC2626" />
          <Text style={styles.permissionText}>
            Location permission disabled. Showing default Aerocity / Delhi area.
          </Text>
        </View>
      )}

      <WebViewComponent
        ref={webViewRef}
        originWhitelist={['*']}
        source={{ html: htmlContent, baseUrl: 'https://localhost' }}
        onMessage={handleMessage}
        onError={(syntheticEvent: any) => {
          console.warn('Leaflet WebView Native Error:', syntheticEvent.nativeEvent);
        }}
        onHttpError={(syntheticEvent: any) => {
          console.warn('Leaflet WebView HTTP Error:', syntheticEvent.nativeEvent.statusCode, syntheticEvent.nativeEvent.url);
        }}
        style={[styles.webview, { height }]}
        javaScriptEnabled
        domStorageEnabled
        scrollEnabled={false}
      />

      {/* Recenter Button */}
      <TouchableOpacity style={styles.recenterBtn} onPress={handleRecenter}>
        <Ionicons name="locate" size={20} color="#0F766E" />
      </TouchableOpacity>

      {/* Legend */}
      <View style={styles.legendContainer}>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: '#3B82F6' }]} />
          <Text style={styles.legendLabel}>{t('mapYou')}</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: '#DC2626' }]} />
          <Text style={styles.legendLabel}>{t('mapRed')}</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: '#EAB308' }]} />
          <Text style={styles.legendLabel}>{t('mapYellow')}</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: '#16A34A' }]} />
          <Text style={styles.legendLabel}>{t('mapGreen')}</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendSquare, { backgroundColor: '#0F766E' }]}>
            <Text style={styles.legendH}>H</Text>
          </View>
          <Text style={styles.legendLabel}>{t('mapHospital')}</Text>
        </View>
      </View>

      {/* Bottom Detail Card on selection */}
      {selectedItem && (
        <View style={styles.bottomCard}>
          <View style={styles.bottomCardHeader}>
            <View style={{ flex: 1 }}>
              <Text style={styles.bottomCardTitle}>
                {selectedItem.data.name || selectedItem.data.areaName}
              </Text>
              <Text style={styles.bottomCardSubtitle}>
                {selectedItem.type === 'FACILITY'
                  ? `${selectedItem.data.type || 'Healthcare Facility'} • ${selectedItem.data.distanceKm != null ? `${selectedItem.data.distanceKm} km away` : ''}`
                  : `${selectedItem.data.zoneColor} ZONE • ${selectedItem.data.activeCases} active cases (${selectedItem.data.highRiskCases} high risk)`}
              </Text>
            </View>
            <TouchableOpacity onPress={() => setSelectedItem(null)}>
              <Ionicons name="close" size={20} color="#64748B" />
            </TouchableOpacity>
          </View>

          {selectedItem.type === 'ZONE' && (
            <View style={styles.zoneDetails}>
              {selectedItem.data.reportedSymptoms && selectedItem.data.reportedSymptoms.length > 0 && (
                <Text style={styles.symptomsText}>
                  Symptoms: {selectedItem.data.reportedSymptoms.slice(0, 4).join(', ')}
                </Text>
              )}
              {selectedItem.data.advisory && (
                <Text style={styles.advisoryText} numberOfLines={2}>
                  {selectedItem.data.advisory}
                </Text>
              )}
            </View>
          )}

          {selectedItem.type === 'FACILITY' && selectedItem.data.emergencyNumber ? (
            <Text style={styles.facilityContact}>
              Emergency: {selectedItem.data.emergencyNumber}
            </Text>
          ) : null}
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    borderRadius: 16,
    overflow: 'hidden',
    position: 'relative',
    backgroundColor: '#F1F5F9',
    marginBottom: 16,
  },
  webview: {
    flex: 1,
    backgroundColor: '#F8FAFC',
  },
  permissionWarning: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FEE2E2',
    paddingHorizontal: 12,
    paddingVertical: 6,
    gap: 6,
  },
  permissionText: {
    fontSize: 12,
    color: '#991B1B',
    fontWeight: '600',
  },
  recenterBtn: {
    position: 'absolute',
    top: 12,
    right: 12,
    backgroundColor: 'white',
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 3,
    shadowColor: '#000',
    shadowOpacity: 0.15,
    shadowOffset: { width: 0, height: 2 },
  },
  legendContainer: {
    position: 'absolute',
    bottom: 8,
    left: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.92)',
    flexDirection: 'row',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    gap: 8,
    alignItems: 'center',
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  legendDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  legendSquare: {
    width: 12,
    height: 12,
    borderRadius: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  legendH: {
    fontSize: 8,
    color: 'white',
    fontWeight: '800',
  },
  legendLabel: {
    fontSize: 10,
    color: '#334155',
    fontWeight: '600',
  },
  bottomCard: {
    position: 'absolute',
    bottom: 8,
    left: 8,
    right: 8,
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 12,
    elevation: 4,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowOffset: { width: 0, height: 2 },
  },
  bottomCardHeader: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
  },
  bottomCardTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: '#0F172A',
  },
  bottomCardSubtitle: {
    fontSize: 12,
    color: '#64748B',
    marginTop: 2,
    fontWeight: '500',
  },
  zoneDetails: {
    marginTop: 6,
    paddingTop: 6,
    borderTopWidth: 1,
    borderTopColor: '#F1F5F9',
  },
  symptomsText: {
    fontSize: 11,
    color: '#334155',
    fontWeight: '600',
  },
  advisoryText: {
    fontSize: 11,
    color: '#64748B',
    marginTop: 2,
  },
  facilityContact: {
    fontSize: 11,
    color: '#0F766E',
    marginTop: 4,
    fontWeight: '700',
  },
});

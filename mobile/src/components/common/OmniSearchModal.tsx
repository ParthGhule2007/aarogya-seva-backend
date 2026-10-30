import React, { useState, useEffect, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  TextInput,
  TouchableOpacity,
  ScrollView,
  FlatList,
  Platform,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { doctorApi } from '../../api/doctorApi';
import { facilityApi } from '../../api/facilityApi';
import { AvailableDoctorResponseDTO } from '../../types/doctor';
import { FacilityRecommendationDto } from '../../types/facility';

export interface OmniSearchItem {
  id: string;
  title: string;
  subtitle: string;
  category: 'ACTION' | 'DOCTOR' | 'MEDICINE' | 'FACILITY' | 'SOS';
  icon: keyof typeof Ionicons.glyphMap;
  iconColor: string;
  badge?: string;
  onPress: () => void;
}

interface OmniSearchModalProps {
  visible: boolean;
  onClose: () => void;
  navigation: any;
  onOpenSos?: () => void;
  onOpenTriage?: () => void;
  onOpenRecords?: () => void;
}

export const OmniSearchModal: React.FC<OmniSearchModalProps> = ({
  visible,
  onClose,
  navigation,
  onOpenSos,
  onOpenTriage,
  onOpenRecords,
}) => {
  const { colors, isElderlyMode, fontScale, touchTargetMin } = useTheme();
  const { language, t } = useLanguage();

  const [query, setQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<'ALL' | 'ACTION' | 'DOCTOR' | 'MEDICINE' | 'FACILITY'>('ALL');
  const [doctors, setDoctors] = useState<AvailableDoctorResponseDTO[]>([]);
  const [facilities, setFacilities] = useState<FacilityRecommendationDto[]>([]);
  const [isListeningVoice, setIsListeningVoice] = useState(false);

  useEffect(() => {
    if (visible) {
      setQuery('');
      setSelectedCategory('ALL');
      // Load live doctors and facilities in background
      doctorApi.getAvailableDoctors()
        .then((docs) => setDoctors(docs || []))
        .catch(() => {});
      facilityApi.searchNearbyFacilities({ latitude: 19.076, longitude: 72.8777 })
        .then((facs) => setFacilities(facs || []))
        .catch(() => {});
    }
  }, [visible]);

  // Standard Navigation Actions
  const quickActions: OmniSearchItem[] = useMemo(() => [
    {
      id: 'action-sos',
      title: '🚨 Emergency SOS 108 / 112 (आपातकालीन सेवा)',
      subtitle: 'Instant ambulance dispatch & 5km responder alert',
      category: 'SOS',
      icon: 'alert-circle',
      iconColor: colors.sosRed,
      badge: 'URGENT 108',
      onPress: () => {
        onClose();
        if (onOpenSos) onOpenSos();
      },
    },
    {
      id: 'action-voice',
      title: '📞 Talk to AI Doctor (आरोग्य मित्र कॉल)',
      subtitle: 'Real-time spoken Hindi/Marathi/English triage helpline',
      category: 'ACTION',
      icon: 'call',
      iconColor: '#0F766E',
      badge: 'VOICE AI',
      onPress: () => {
        onClose();
        navigation.navigate('TalkToAi');
      },
    },
    {
      id: 'action-triage',
      title: '🩺 AI Symptom Checker & Triage (लक्षण जांच)',
      subtitle: 'Check symptoms with Qwen2.5-3B clinical intelligence',
      category: 'ACTION',
      icon: 'pulse',
      iconColor: '#059669',
      badge: 'CLINICAL AI',
      onPress: () => {
        onClose();
        if (onOpenTriage) onOpenTriage();
        else navigation.navigate('Consultations');
      },
    },
    {
      id: 'action-records',
      title: '🗂️ Medical Records & Digital Health Pass (स्वास्थ्य रिकॉर्ड)',
      subtitle: 'View BP, Sugar, vitals history, past visits & ABHA card',
      category: 'ACTION',
      icon: 'folder-open',
      iconColor: '#2563EB',
      badge: 'ABHA PASS',
      onPress: () => {
        onClose();
        if (onOpenRecords) onOpenRecords();
        else navigation.navigate('Dashboard');
      },
    },
    {
      id: 'action-meds',
      title: '💊 Today’s Medicine Schedule (आज की दवाइयाँ)',
      subtitle: 'View daily doses, pill reminders, and mark doses as taken',
      category: 'MEDICINE',
      icon: 'medkit',
      iconColor: '#D97706',
      badge: 'TODAY',
      onPress: () => {
        onClose();
        navigation.navigate('Dashboard');
      },
    },
    {
      id: 'action-consult',
      title: '📹 Book Video Consultation (डॉक्टर परामर्श)',
      subtitle: 'Schedule video call with available government & specialist doctors',
      category: 'DOCTOR',
      icon: 'videocam',
      iconColor: '#7C3AED',
      badge: 'TELEMEDICINE',
      onPress: () => {
        onClose();
        navigation.navigate('Consultations');
      },
    },
    {
      id: 'action-chat',
      title: '🤖 24/7 Health Chatbot (आरोग्य सेवा चैटबॉट)',
      subtitle: 'Ask medical doubts, first-aid precautions, and health tips',
      category: 'ACTION',
      icon: 'chatbubbles',
      iconColor: '#0D9488',
      badge: 'CHAT 24/7',
      onPress: () => {
        onClose();
        navigation.navigate('Chatbot');
      },
    },
    {
      id: 'action-hospitals',
      title: '🏥 Nearest PHC & Hospitals (नजदीकी अस्पताल)',
      subtitle: 'Find government primary health centers, beds, and contact',
      category: 'FACILITY',
      icon: 'business',
      iconColor: '#0891B2',
      badge: 'MAP & PHC',
      onPress: () => {
        onClose();
        navigation.navigate('Facilities');
      },
    },
  ], [colors, navigation, onClose, onOpenSos, onOpenTriage, onOpenRecords]);

  // Dynamic doctor search items
  const doctorItems: OmniSearchItem[] = useMemo(() => {
    return doctors.map((doc) => ({
      id: `doc-${doc.id}`,
      title: `Dr. ${doc.name}`,
      subtitle: `${doc.specialization} • ${doc.experienceYears || 5}+ yrs exp • Available for Call`,
      category: 'DOCTOR',
      icon: 'person',
      iconColor: colors.primary,
      badge: 'DOCTOR',
      onPress: () => {
        onClose();
        navigation.navigate('Consultations');
      },
    }));
  }, [doctors, colors, navigation, onClose]);

  // Dynamic facility items
  const facilityItems: OmniSearchItem[] = useMemo(() => {
    return facilities.map((f) => ({
      id: `fac-${f.facilityId || f.id || Math.random()}`,
      title: f.name || f.facilityName || 'Rural Health Center',
      subtitle: `${f.facilityTypeDisplayName || f.facilityType} • ${f.addressSummary || f.villageTown || 'Rural Center'} • ${f.distanceKm ? `${f.distanceKm.toFixed(1)} km away` : 'Nearby'}`,
      category: 'FACILITY',
      icon: 'navigate-circle',
      iconColor: '#0891B2',
      badge: 'PHC',
      onPress: () => {
        onClose();
        navigation.navigate('Facilities');
      },
    }));
  }, [facilities, navigation, onClose]);

  // Combine and filter items based on search query and category
  const filteredItems = useMemo(() => {
    let all = [...quickActions, ...doctorItems, ...facilityItems];

    if (selectedCategory !== 'ALL') {
      all = all.filter((item) => item.category === selectedCategory);
    }

    const q = query.trim().toLowerCase();
    if (!q) return all;

    return all.filter(
      (item) =>
        item.title.toLowerCase().includes(q) ||
        item.subtitle.toLowerCase().includes(q) ||
        (item.badge && item.badge.toLowerCase().includes(q))
    );
  }, [quickActions, doctorItems, facilityItems, selectedCategory, query]);

  // Voice search mock / trigger
  const handleVoiceSearch = () => {
    setIsListeningVoice(true);
    setTimeout(() => {
      setIsListeningVoice(false);
      setQuery('बुखार और दवा (Fever & Medicine)');
    }, 1400);
  };

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <View style={styles.modalOverlay}>
        <View style={[styles.container, { backgroundColor: colors.bgSurface }]}>
          {/* Top Drag Handle & Close Header */}
          <View style={styles.topBar}>
            <View style={styles.dragHandle} />
            <TouchableOpacity
              onPress={onClose}
              style={[styles.closeButton, { backgroundColor: colors.bgSubtle }]}
              accessibilityLabel="Close Search"
            >
              <Ionicons name="close" size={22} color={colors.textMain} />
            </TouchableOpacity>
          </View>

          {/* Search Header Title */}
          <View style={styles.titleRow}>
            <Ionicons name="compass" size={24} color={colors.primary} />
            <Text style={[styles.sheetTitle, { color: colors.textMain, fontSize: 18 * fontScale }]}>
              {language === 'hi' ? 'त्वरित खोज व नेविगेशन' : language === 'mr' ? 'जलद शोध व नेव्हिगेशन' : 'OmniSearch & Fast Navigator'}
            </Text>
          </View>

          {/* Search Input Box */}
          <View style={[styles.searchBox, { backgroundColor: colors.bgSubtle, borderColor: colors.borderMedium }]}>
            <Ionicons name="search" size={20} color={colors.primary} style={styles.searchIcon} />
            <TextInput
              style={[styles.searchInput, { color: colors.textMain, fontSize: 16 * fontScale }]}
              placeholder={
                language === 'hi'
                  ? 'कुछ भी खोजें: डॉक्टर, दवाइयाँ, 108 SOS, अस्पताल...'
                  : language === 'mr'
                  ? 'काहीही शोधा: डॉक्टर, औषधे, 108 SOS, रुग्णालय...'
                  : 'Search anything: Doctors, medicines, 108 SOS, PHCs...'
              }
              placeholderTextColor={colors.textSubtle}
              value={query}
              onChangeText={setQuery}
              autoFocus
              clearButtonMode="while-editing"
            />
            {query.length > 0 && (
              <TouchableOpacity onPress={() => setQuery('')} style={styles.clearBtn}>
                <Ionicons name="close-circle" size={18} color={colors.textSubtle} />
              </TouchableOpacity>
            )}
            {/* Voice Search Button */}
            <TouchableOpacity
              onPress={handleVoiceSearch}
              style={[
                styles.micBtn,
                { backgroundColor: isListeningVoice ? colors.sosRed : colors.primaryLight },
              ]}
              accessibilityLabel="Voice Search"
            >
              <Ionicons
                name={isListeningVoice ? 'mic' : 'mic-outline'}
                size={18}
                color={isListeningVoice ? '#FFFFFF' : colors.primary}
              />
            </TouchableOpacity>
          </View>

          {isListeningVoice && (
            <View style={styles.voicePrompt}>
              <Text style={{ color: colors.sosRed, fontWeight: '700', fontSize: 13 }}>
                🎙️ {language === 'hi' ? 'सुन रहे हैं... बोलिए (बोलकर खोजें)' : 'Listening... Speak in Hindi, Marathi, or English'}
              </Text>
            </View>
          )}

          {/* Category Filter Pills */}
          <View style={styles.filterPillsRow}>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterPills}>
              {[
                { id: 'ALL', label: 'All (सभी)' },
                { id: 'ACTION', label: '⚡ Fast Actions' },
                { id: 'DOCTOR', label: '🩺 Doctors' },
                { id: 'MEDICINE', label: '💊 Medicines' },
                { id: 'FACILITY', label: '🏥 Hospitals / PHC' },
              ].map((cat) => {
                const isActive = selectedCategory === cat.id;
                return (
                  <TouchableOpacity
                    key={cat.id}
                    onPress={() => setSelectedCategory(cat.id as any)}
                    style={[
                      styles.filterPill,
                      {
                        backgroundColor: isActive ? colors.primary : colors.bgSubtle,
                        borderColor: isActive ? colors.primary : colors.borderLight,
                      },
                    ]}
                  >
                    <Text
                      style={[
                        styles.filterPillText,
                        { color: isActive ? '#FFFFFF' : colors.textMuted, fontWeight: isActive ? '700' : '600' },
                      ]}
                    >
                      {cat.label}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </ScrollView>
          </View>

          {/* Results List */}
          <FlatList
            data={filteredItems}
            keyExtractor={(item) => item.id}
            keyboardShouldPersistTaps="handled"
            contentContainerStyle={styles.resultsList}
            ListEmptyComponent={
              <View style={styles.emptyContainer}>
                <Ionicons name="search-outline" size={44} color={colors.textSubtle} />
                <Text style={[styles.emptyText, { color: colors.textSubtle }]}>
                  {language === 'hi'
                    ? `"${query}" के लिए कोई परिणाम नहीं मिला`
                    : `No results found for "${query}"`}
                </Text>
                <TouchableOpacity
                  onPress={() => {
                    onClose();
                    navigation.navigate('Chatbot');
                  }}
                  style={[styles.emptyActionBtn, { backgroundColor: colors.primaryLight }]}
                >
                  <Text style={[styles.emptyActionText, { color: colors.primary }]}>
                    Ask AI Health Assistant instead 🤖
                  </Text>
                </TouchableOpacity>
              </View>
            }
            renderItem={({ item }) => (
              <TouchableOpacity
                onPress={item.onPress}
                activeOpacity={0.7}
                style={[
                  styles.resultCard,
                  {
                    backgroundColor: colors.bgSurface,
                    borderColor: colors.borderLight,
                    minHeight: touchTargetMin,
                  },
                ]}
              >
                <View style={[styles.iconCircle, { backgroundColor: `${item.iconColor}15` }]}>
                  <Ionicons name={item.icon} size={22} color={item.iconColor} />
                </View>
                <View style={styles.cardContent}>
                  <View style={styles.cardTitleRow}>
                    <Text
                      style={[styles.cardTitle, { color: colors.textMain, fontSize: (isElderlyMode ? 16 : 14) * fontScale }]}
                      numberOfLines={1}
                    >
                      {item.title}
                    </Text>
                    {item.badge && (
                      <View style={[styles.badge, { backgroundColor: `${item.iconColor}15` }]}>
                        <Text style={[styles.badgeText, { color: item.iconColor }]}>{item.badge}</Text>
                      </View>
                    )}
                  </View>
                  <Text
                    style={[styles.cardSubtitle, { color: colors.textSubtle, fontSize: (isElderlyMode ? 13 : 12) * fontScale }]}
                    numberOfLines={2}
                  >
                    {item.subtitle}
                  </Text>
                </View>
                <Ionicons name="chevron-forward" size={18} color={colors.textSubtle} />
              </TouchableOpacity>
            )}
          />
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(7, 11, 20, 0.65)',
    justifyContent: 'flex-end',
  },
  container: {
    height: '86%',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingTop: 8,
    paddingHorizontal: 16,
    paddingBottom: Platform.OS === 'ios' ? 34 : 20,
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: -4 },
        shadowOpacity: 0.15,
        shadowRadius: 12,
      },
      android: {
        elevation: 12,
      },
    }),
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 4,
  },
  dragHandle: {
    width: 44,
    height: 5,
    backgroundColor: '#CBD5E1',
    borderRadius: 99,
    alignSelf: 'center',
    marginHorizontal: 'auto',
  },
  closeButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 6,
    marginBottom: 10,
  },
  sheetTitle: {
    fontWeight: '800',
  },
  searchBox: {
    flexDirection: 'row',
    alignItems: 'center',
    height: 48,
    borderRadius: 14,
    borderWidth: 1.5,
    paddingHorizontal: 12,
    marginBottom: 10,
  },
  searchIcon: {
    marginRight: 8,
  },
  searchInput: {
    flex: 1,
    height: '100%',
    fontWeight: '500',
  },
  clearBtn: {
    padding: 4,
  },
  micBtn: {
    width: 34,
    height: 34,
    borderRadius: 17,
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 6,
  },
  voicePrompt: {
    marginBottom: 8,
    paddingHorizontal: 4,
  },
  filterPillsRow: {
    marginBottom: 12,
  },
  filterPills: {
    gap: 8,
    paddingVertical: 2,
  },
  filterPill: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 99,
    borderWidth: 1,
  },
  filterPillText: {
    fontSize: 12,
  },
  resultsList: {
    paddingBottom: 24,
    gap: 8,
  },
  resultCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderRadius: 14,
    borderWidth: 1,
    gap: 12,
  },
  iconCircle: {
    width: 44,
    height: 44,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardContent: {
    flex: 1,
  },
  cardTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 6,
    marginBottom: 2,
  },
  cardTitle: {
    fontWeight: '700',
    flex: 1,
  },
  badge: {
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6,
  },
  badgeText: {
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  cardSubtitle: {
    lineHeight: 16,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 40,
    gap: 12,
  },
  emptyText: {
    fontSize: 14,
    fontWeight: '500',
    textAlign: 'center',
  },
  emptyActionBtn: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 99,
    marginTop: 8,
  },
  emptyActionText: {
    fontSize: 13,
    fontWeight: '700',
  },
});

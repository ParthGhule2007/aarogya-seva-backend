import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { Button } from '../../components/common/Button';
import { Card } from '../../components/common/Card';
import { UserRole } from '../../types/auth';
import { facilityApi } from '../../api/facilityApi';
import { authApi } from '../../api/authApi';
import { GoogleSignInButton } from '../../components/common/GoogleSignInButton';
import { FacilitySummaryDto, FacilityType } from '../../types/facility';
import * as Location from 'expo-location';
import { MOCK_LOCATION } from '../../config/apiConfig';
import { OutbreakLeafletMap } from '../../components/map/OutbreakLeafletMap';

interface RegisterScreenProps {
  navigation: any;
  route?: any;
}

export const RegisterScreen: React.FC<RegisterScreenProps> = ({ navigation, route }) => {
  const { register, loginWithToken, isLoading, error } = useAuth();
  const { colors } = useTheme();
  const { t } = useLanguage();

  const [step, setStep] = useState<1 | 2>(1);
  const [googleIdToken, setGoogleIdToken] = useState<string | null>(route?.params?.googleIdToken || null);

  // Step 1: Account fields
  const [username, setUsername] = useState(route?.params?.googleEmail || '');
  const [fullName, setFullName] = useState(route?.params?.googleFullName || '');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<UserRole>('PATIENT');

  useEffect(() => {
    if (route?.params?.googleIdToken) {
      setGoogleIdToken(route.params.googleIdToken);
      if (route.params.googleEmail) {
        setUsername(route.params.googleEmail);
      }
      if (route.params.googleFullName) {
        setFullName(route.params.googleFullName);
      }
    }
  }, [route?.params]);

  // Step 2: Patient profile
  const [gender, setGender] = useState('OTHER');
  const [dob, setDob] = useState('');
  const [age, setAge] = useState('30');
  const [villageTown, setVillageTown] = useState('Aerocity');
  const [district, setDistrict] = useState('New Delhi');
  const [state, setState] = useState('Delhi');
  const [pincode, setPincode] = useState('110037');
  const [heightCm, setHeightCm] = useState('');
  const [weightKg, setWeightKg] = useState('');
  const [medicalHistory, setMedicalHistory] = useState('');

  // Step 2: Doctor profile
  const [specialization, setSpecialization] = useState('General Medicine');
  const [licenseNumber, setLicenseNumber] = useState('');
  const [experienceYears, setExperienceYears] = useState('5');
  const [qualification, setQualification] = useState('MBBS, MD');
  const [hospitalName, setHospitalName] = useState('Aerocity Demo Hospital');

  // Step 2: Facility Admin profile
  const [facilityMode, setFacilityMode] = useState<'NEW' | 'EXISTING'>('NEW');
  const [facilities, setFacilities] = useState<FacilitySummaryDto[]>([]);
  const [facilitySearch, setFacilitySearch] = useState('');
  const [selectedFacilityId, setSelectedFacilityId] = useState<string>('');
  const [loadingFacilities, setLoadingFacilities] = useState(false);

  // New Hospital Fields
  const [newFacilityName, setNewFacilityName] = useState('');
  const [newFacilityType, setNewFacilityType] = useState<FacilityType>('PHC');
  const [newFacilityPhone, setNewFacilityPhone] = useState('');
  const [newFacilityAddress, setNewFacilityAddress] = useState('');
  const [newFacilityVillage, setNewFacilityVillage] = useState('Pune');
  const [newFacilityDistrict, setNewFacilityDistrict] = useState('Pune');
  const [newFacilityState, setNewFacilityState] = useState('Maharashtra');
  const [newFacilityPincode, setNewFacilityPincode] = useState('411001');
  const [newFacilityLat, setNewFacilityLat] = useState(
    MOCK_LOCATION ? MOCK_LOCATION.latitude.toString() : '18.5204'
  );
  const [newFacilityLng, setNewFacilityLng] = useState(
    MOCK_LOCATION ? MOCK_LOCATION.longitude.toString() : '73.8567'
  );

  // Step 2: Caregiver profile
  const [linkedPatientPhone, setLinkedPatientPhone] = useState('');
  const [linkedPatientUsername, setLinkedPatientUsername] = useState('');

  useEffect(() => {
    if (role === 'FACILITY_ADMIN' || role === 'ADMIN') {
      setLoadingFacilities(true);
      facilityApi
        .getAllFacilities()
        .then((facs) => {
          if (Array.isArray(facs) && facs.length > 0) {
            const mapped: FacilitySummaryDto[] = facs.map((f: any) => ({
              facilityId: f.id || f.facilityId,
              name: f.name,
              facilityType: f.facilityType,
              facilityTypeDisplayName: f.facilityTypeDisplayName,
              addressSummary: f.addressLine1 || f.addressSummary,
              villageTown: f.villageTown,
              district: f.district,
              state: f.state,
              pincode: f.pincode,
              latitude: f.latitude,
              longitude: f.longitude,
              distanceKm: 0,
              contactNumber: f.contactNumber,
              emergencyNumber: f.emergencyNumber,
              availableServices: f.availableServices || [],
              isOperational: f.isOperational ?? true,
              isDemoData: f.isDemoData ?? false,
            }));
            setFacilities(mapped);
            if (!selectedFacilityId) setSelectedFacilityId(mapped[0].facilityId);
          }
        })
        .catch(() => {
          // fallback
        })
        .finally(() => setLoadingFacilities(false));
    }
  }, [role]);

  const handleUseCurrentLocation = async () => {
    try {
      if (MOCK_LOCATION) {
        setNewFacilityLat(MOCK_LOCATION.latitude.toFixed(4));
        setNewFacilityLng(MOCK_LOCATION.longitude.toFixed(4));
        return;
      }
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status === 'granted') {
        const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
        if (loc && loc.coords) {
          setNewFacilityLat(loc.coords.latitude.toFixed(4));
          setNewFacilityLng(loc.coords.longitude.toFixed(4));
        }
      }
    } catch (err: any) {
      Alert.alert('GPS Location', 'Could not obtain current GPS fix. You can enter lat/lng manually.');
    }
  };

  const calculateAgeFromDob = (dobStr: string): string => {
    const clean = dobStr.trim();
    let bDate: Date | null = null;
    if (/^\d{4}-\d{2}-\d{2}$/.test(clean)) {
      bDate = new Date(clean);
    } else if (/^\d{2}[/-]\d{2}[/-]\d{4}$/.test(clean)) {
      const parts = clean.split(/[/-]/);
      bDate = new Date(parseInt(parts[2], 10), parseInt(parts[1], 10) - 1, parseInt(parts[0], 10));
    }
    if (bDate && !isNaN(bDate.getTime())) {
      const today = new Date();
      let years = today.getFullYear() - bDate.getFullYear();
      const m = today.getMonth() - bDate.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < bDate.getDate())) {
        years--;
      }
      if (years >= 0 && years <= 125) {
        return String(years);
      }
    }
    return '';
  };

  const handleNextStep = () => {
    if (!username.trim() || !fullName.trim()) {
      Alert.alert('Required Fields', 'Please fill in username and full name.');
      return;
    }
    if (!googleIdToken) {
      if (!password.trim()) {
        Alert.alert('Required Fields', 'Please fill in password.');
        return;
      }
      if (password.length < 6) {
        Alert.alert('Password Length', 'Password must be at least 6 characters.');
        return;
      }
    }
    setStep(2);
  };

  const handleRegister = async () => {
    try {
      let finalFacilityId = selectedFacilityId;

      if ((role === 'FACILITY_ADMIN' || role === 'ADMIN') && facilityMode === 'NEW') {
        if (!newFacilityName.trim()) {
          Alert.alert('Hospital Name', 'Please provide a valid hospital name.');
          return;
        }
        if (!newFacilityDistrict.trim()) {
          Alert.alert('District Required', 'District is required for regional zone assignment.');
          return;
        }
        const lat = parseFloat(newFacilityLat);
        const lng = parseFloat(newFacilityLng);
        if (isNaN(lat) || isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
          Alert.alert('Invalid Coordinates', 'Please provide valid latitude (-90 to 90) and longitude (-180 to 180).');
          return;
        }

        const createdFacility = await facilityApi.createFacility({
          name: newFacilityName.trim(),
          facilityType: newFacilityType,
          latitude: lat,
          longitude: lng,
          addressLine1: newFacilityAddress.trim() || `${newFacilityVillage}, ${newFacilityDistrict}`,
          villageTown: newFacilityVillage.trim() || 'Pune',
          subDistrict: newFacilityVillage.trim() || 'Haveli',
          district: newFacilityDistrict.trim(),
          state: newFacilityState.trim() || 'Maharashtra',
          pincode: (newFacilityPincode.trim() && /^[1-9][0-9]{5}$/.test(newFacilityPincode.trim()))
            ? newFacilityPincode.trim()
            : '411001',
          contactNumber: newFacilityPhone.trim() || phoneNumber.trim() || '9876543210',
          isOperational: true,
        });

        finalFacilityId = (createdFacility as any).id || (createdFacility as any).facilityId;
      }

      if (googleIdToken) {
        const res = await authApi.googleAuth({
          idToken: googleIdToken,
          role,
          fullName: fullName.trim(),
          phoneNumber: phoneNumber.trim() || undefined,
          // Patient fields
          gender: role === 'PATIENT' ? gender : undefined,
          age: role === 'PATIENT' ? parseInt(age, 10) || 30 : undefined,
          villageTown: role === 'PATIENT' ? villageTown.trim() : undefined,
          district: role === 'PATIENT' ? district.trim() : undefined,
          state: role === 'PATIENT' ? state.trim() : undefined,
          pincode: role === 'PATIENT' ? pincode.trim() : undefined,
          heightCm: role === 'PATIENT' && heightCm.trim() ? parseFloat(heightCm) : undefined,
          weightKg: role === 'PATIENT' && weightKg.trim() ? parseFloat(weightKg) : undefined,
          medicalHistory: role === 'PATIENT' && medicalHistory.trim() ? medicalHistory.trim() : undefined,
          // Doctor fields
          specialization: role === 'DOCTOR' ? specialization.trim() : undefined,
          licenseNumber: role === 'DOCTOR' ? licenseNumber.trim() || `LIC-${Date.now()}` : undefined,
          experienceYears: role === 'DOCTOR' ? parseInt(experienceYears, 10) || 5 : undefined,
          qualification: role === 'DOCTOR' ? qualification.trim() : undefined,
          hospitalName: role === 'DOCTOR' ? hospitalName.trim() : undefined,
          // Facility Admin fields
          facilityId: (role === 'FACILITY_ADMIN' || role === 'ADMIN') ? finalFacilityId : undefined,
          // Caregiver fields
          linkedPatientPhone: role === 'CAREGIVER' ? linkedPatientPhone.trim() : undefined,
          linkedPatientUsername: role === 'CAREGIVER' ? linkedPatientUsername.trim() : undefined,
        });
        await loginWithToken(res);
      } else {
        await register({
          username: username.trim(),
          password: password.trim(),
          fullName: fullName.trim(),
          phoneNumber: phoneNumber.trim() || undefined,
          role,
          // Patient fields
          gender: role === 'PATIENT' ? gender : undefined,
          age: role === 'PATIENT' ? parseInt(age, 10) || 30 : undefined,
          villageTown: role === 'PATIENT' ? villageTown.trim() : undefined,
          district: role === 'PATIENT' ? district.trim() : undefined,
          state: role === 'PATIENT' ? state.trim() : undefined,
          pincode: role === 'PATIENT' ? pincode.trim() : undefined,
          heightCm: role === 'PATIENT' && heightCm.trim() ? parseFloat(heightCm) : undefined,
          weightKg: role === 'PATIENT' && weightKg.trim() ? parseFloat(weightKg) : undefined,
          medicalHistory: role === 'PATIENT' && medicalHistory.trim() ? medicalHistory.trim() : undefined,
          // Doctor fields
          specialization: role === 'DOCTOR' ? specialization.trim() : undefined,
          licenseNumber: role === 'DOCTOR' ? licenseNumber.trim() || `LIC-${Date.now()}` : undefined,
          experienceYears: role === 'DOCTOR' ? parseInt(experienceYears, 10) || 5 : undefined,
          qualification: role === 'DOCTOR' ? qualification.trim() : undefined,
          hospitalName: role === 'DOCTOR' ? hospitalName.trim() : undefined,
          // Facility Admin fields
          facilityId: (role === 'FACILITY_ADMIN' || role === 'ADMIN') ? finalFacilityId : undefined,
          // Caregiver fields
          linkedPatientPhone: role === 'CAREGIVER' ? linkedPatientPhone.trim() : undefined,
          linkedPatientUsername: role === 'CAREGIVER' ? linkedPatientUsername.trim() : undefined,
        });
      }
    } catch (err: any) {
      Alert.alert('Registration Notice', err.message || 'Registration could not be completed.');
    }
  };

  const filteredFacilities = facilities.filter(
    (f) =>
      f.name.toLowerCase().includes(facilitySearch.toLowerCase()) ||
      (f.villageTown && f.villageTown.toLowerCase().includes(facilitySearch.toLowerCase()))
  );

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={{ flex: 1, backgroundColor: colors.bgApp }}
    >
      <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">
        <Card style={styles.card}>
          <View style={styles.headerRow}>
            <Text style={[styles.title, { color: colors.textMain }]}>{t('createAccount')}</Text>
            <View style={[styles.stepBadge, { backgroundColor: colors.primaryLight }]}>
              <Text style={[styles.stepText, { color: colors.primary }]}>Step {step} of 2</Text>
            </View>
          </View>
          <Text style={[styles.subtitle, { color: colors.textSubtle }]}>
            {step === 1 ? 'Account credentials and system role' : `Complete your ${role} profile`}
          </Text>

          {error ? (
            <View style={[styles.errorBox, { backgroundColor: colors.riskHighBg, borderColor: colors.riskHighBorder }]}>
              <Text style={[styles.errorText, { color: colors.riskHigh }]}>{error}</Text>
            </View>
          ) : null}

          {step === 1 ? (
            <>
              {googleIdToken ? (
                <View style={[styles.googleLinkedBanner, { backgroundColor: colors.primaryLight, borderColor: colors.primary }]}>
                  <Ionicons name="checkmark-circle-outline" size={20} color={colors.primary} />
                  <View style={{ flex: 1, marginLeft: 8 }}>
                    <Text style={[styles.googleLinkedTitle, { color: colors.primary }]}>Linked to Google Account</Text>
                    <Text style={[styles.googleLinkedSub, { color: colors.textMuted }]}>{username}</Text>
                  </View>
                </View>
              ) : (
                <>
                  <GoogleSignInButton
                    onSignInSuccess={async (googleUser: any) => {
                      try {
                        const res = await authApi.googleAuth({ idToken: googleUser.idToken });
                        if (res.registrationRequired) {
                          setGoogleIdToken(googleUser.idToken);
                          if (res.email || googleUser.user?.email) setUsername(res.email || googleUser.user?.email);
                          if (res.fullName || googleUser.user?.name) setFullName(res.fullName || googleUser.user?.name);
                        } else {
                          await loginWithToken(res);
                        }
                      } catch (e: any) {
                        Alert.alert('Google Sign-In Notice', e.message || 'Google authentication failed');
                      }
                    }}
                    onError={(err: any) => Alert.alert('Google Sign-In', err.message)}
                  />
                  <View style={styles.dividerRow}>
                    <View style={[styles.dividerLine, { backgroundColor: colors.borderLight }]} />
                    <Text style={[styles.dividerText, { color: colors.textMuted }]}>or register with credentials</Text>
                    <View style={[styles.dividerLine, { backgroundColor: colors.borderLight }]} />
                  </View>
                </>
              )}

              {/* Full Name */}
              <Text style={[styles.label, { color: colors.textMuted }]}>{t('fullName')}</Text>
              <TextInput
                style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                placeholder="e.g. Ramesh Patil or Dr. Ananya"
                placeholderTextColor={colors.textSubtle}
                value={fullName}
                onChangeText={setFullName}
              />

              {/* Username */}
              <Text style={[styles.label, { color: colors.textMuted }]}>{t('username')}</Text>
              <TextInput
                style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                placeholder="Unique username"
                placeholderTextColor={colors.textSubtle}
                autoCapitalize="none"
                value={username}
                onChangeText={setUsername}
                editable={!googleIdToken}
              />

              {/* Phone Number */}
              <Text style={[styles.label, { color: colors.textMuted }]}>{t('phoneNumber')}</Text>
              <TextInput
                style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                placeholder="10-digit mobile number"
                placeholderTextColor={colors.textSubtle}
                keyboardType="phone-pad"
                value={phoneNumber}
                onChangeText={setPhoneNumber}
              />

              {/* Password */}
              {!googleIdToken && (
                <>
                  <Text style={[styles.label, { color: colors.textMuted }]}>{t('password')}</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="Minimum 6 characters"
                    placeholderTextColor={colors.textSubtle}
                    secureTextEntry
                    value={password}
                    onChangeText={setPassword}
                  />
                </>
              )}

              {/* Role Selection */}
              <Text style={[styles.label, { color: colors.textMuted, marginTop: 12 }]}>{t('selectRole')}</Text>
              <View style={styles.roleGrid}>
                {(['PATIENT', 'DOCTOR', 'FACILITY_ADMIN', 'CAREGIVER', 'ASHA'] as UserRole[]).map((r) => (
                  <TouchableOpacity
                    key={r}
                    style={[
                      styles.roleButton,
                      {
                        backgroundColor: role === r ? colors.primaryLight : colors.bgSubtle,
                        borderColor: role === r ? colors.primary : colors.borderLight,
                      },
                    ]}
                    onPress={() => setRole(r)}
                  >
                    <Text style={[styles.roleText, { color: role === r ? colors.primary : colors.textMain }]}>
                      {r === 'PATIENT' ? 'Patient' : r === 'DOCTOR' ? 'Doctor' : r === 'FACILITY_ADMIN' ? 'Hospital Admin' : r === 'CAREGIVER' ? 'Caregiver' : 'ASHA Worker'}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              <Button
                title="Next: Profile Setup"
                onPress={handleNextStep}
                style={styles.submitBtn}
              />
            </>
          ) : (
            <>
              {/* Step 2 Profile based on Role */}
              {role === 'PATIENT' && (
                <>
                  <Text style={[styles.label, { color: colors.textMuted }]}>Gender</Text>
                  <View style={styles.roleGrid}>
                    {['MALE', 'FEMALE', 'OTHER'].map((g) => (
                      <TouchableOpacity
                        key={g}
                        style={[
                          styles.roleButton,
                          {
                            backgroundColor: gender === g ? colors.primaryLight : colors.bgSubtle,
                            borderColor: gender === g ? colors.primary : colors.borderLight,
                          },
                        ]}
                        onPress={() => setGender(g)}
                      >
                        <Text style={[styles.roleText, { color: gender === g ? colors.primary : colors.textMain }]}>{g}</Text>
                      </TouchableOpacity>
                    ))}
                  </View>

                  <Text style={[styles.label, { color: colors.textMuted }]}>Date of Birth (Optional)</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="YYYY-MM-DD or DD/MM/YYYY"
                    placeholderTextColor={colors.textSubtle}
                    value={dob}
                    onChangeText={(val) => {
                      setDob(val);
                      const calculated = calculateAgeFromDob(val);
                      if (calculated) {
                        setAge(calculated);
                      }
                    }}
                  />

                  <Text style={[styles.label, { color: colors.textMuted }]}>
                    Age {dob && calculateAgeFromDob(dob) ? '(Calculated from DOB)' : ''}
                  </Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="e.g. 35"
                    placeholderTextColor={colors.textSubtle}
                    keyboardType="numeric"
                    value={age}
                    onChangeText={setAge}
                  />

                  <Text style={[styles.label, { color: colors.textMuted }]}>Village / Town</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="Village or Town name"
                    placeholderTextColor={colors.textSubtle}
                    value={villageTown}
                    onChangeText={setVillageTown}
                  />

                  <Text style={[styles.label, { color: colors.textMuted }]}>District</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="District name"
                    placeholderTextColor={colors.textSubtle}
                    value={district}
                    onChangeText={setDistrict}
                  />

                  <Text style={[styles.label, { color: colors.textMuted }]}>Pincode</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="6-digit pincode"
                    placeholderTextColor={colors.textSubtle}
                    keyboardType="numeric"
                    value={pincode}
                    onChangeText={setPincode}
                  />

                  {/* Optional Vitals & Medical History */}
                  <View style={{ flexDirection: 'row', gap: 12, marginTop: 4 }}>
                    <View style={{ flex: 1 }}>
                      <Text style={[styles.label, { color: colors.textMuted }]}>Height (cm) (Optional)</Text>
                      <TextInput
                        style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                        placeholder="e.g. 170"
                        placeholderTextColor={colors.textSubtle}
                        keyboardType="numeric"
                        value={heightCm}
                        onChangeText={setHeightCm}
                      />
                    </View>
                    <View style={{ flex: 1 }}>
                      <Text style={[styles.label, { color: colors.textMuted }]}>Weight (kg) (Optional)</Text>
                      <TextInput
                        style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                        placeholder="e.g. 65"
                        placeholderTextColor={colors.textSubtle}
                        keyboardType="numeric"
                        value={weightKg}
                        onChangeText={setWeightKg}
                      />
                    </View>
                  </View>

                  <Text style={[styles.label, { color: colors.textMuted, marginTop: 8 }]}>Medical History / Chronic Conditions (Optional)</Text>
                  <TextInput
                    style={[
                      styles.input,
                      {
                        backgroundColor: colors.bgSubtle,
                        borderColor: colors.borderLight,
                        color: colors.textMain,
                        height: 72,
                        textAlignVertical: 'top',
                        paddingTop: 8,
                      },
                    ]}
                    placeholder="e.g. Diabetes, Hypertension, Asthma, allergies"
                    placeholderTextColor={colors.textSubtle}
                    multiline
                    numberOfLines={3}
                    value={medicalHistory}
                    onChangeText={setMedicalHistory}
                  />
                </>
              )}

              {role === 'DOCTOR' && (
                <>
                  <Text style={[styles.label, { color: colors.textMuted }]}>Clinical Specialization</Text>
                  <View style={styles.roleGrid}>
                    {['General Medicine', 'Cardiology', 'Pediatrics', 'Orthopedics', 'Gynecology'].map((spec) => (
                      <TouchableOpacity
                        key={spec}
                        style={[
                          styles.roleButton,
                          {
                            backgroundColor: specialization === spec ? colors.primaryLight : colors.bgSubtle,
                            borderColor: specialization === spec ? colors.primary : colors.borderLight,
                          },
                        ]}
                        onPress={() => setSpecialization(spec)}
                      >
                        <Text style={[styles.roleText, { color: specialization === spec ? colors.primary : colors.textMain }]}>{spec}</Text>
                      </TouchableOpacity>
                    ))}
                  </View>

                  <Text style={[styles.label, { color: colors.textMuted }]}>Medical Council License Number</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="e.g. MCI-2024-8891"
                    placeholderTextColor={colors.textSubtle}
                    value={licenseNumber}
                    onChangeText={setLicenseNumber}
                  />

                  <Text style={[styles.label, { color: colors.textMuted }]}>Experience (Years)</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="e.g. 8"
                    placeholderTextColor={colors.textSubtle}
                    keyboardType="numeric"
                    value={experienceYears}
                    onChangeText={setExperienceYears}
                  />

                  <Text style={[styles.label, { color: colors.textMuted }]}>Qualifications</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="e.g. MBBS, MD"
                    placeholderTextColor={colors.textSubtle}
                    value={qualification}
                    onChangeText={setQualification}
                  />

                  <Text style={[styles.label, { color: colors.textMuted }]}>Associated Hospital / Clinic</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="e.g. Aerocity Demo Hospital"
                    placeholderTextColor={colors.textSubtle}
                    value={hospitalName}
                    onChangeText={setHospitalName}
                  />
                </>
              )}

              {(role === 'FACILITY_ADMIN' || role === 'ADMIN') && (
                <>
                  <Text style={[styles.label, { color: colors.textMuted }]}>Hospital Administration Mode</Text>
                  <View style={[styles.roleGrid, { marginBottom: 14 }]}>
                    <TouchableOpacity
                      style={[
                        styles.roleButton,
                        {
                          backgroundColor: facilityMode === 'NEW' ? colors.primaryLight : colors.bgSubtle,
                          borderColor: facilityMode === 'NEW' ? colors.primary : colors.borderLight,
                        },
                      ]}
                      onPress={() => setFacilityMode('NEW')}
                    >
                      <Text style={[styles.roleText, { color: facilityMode === 'NEW' ? colors.primary : colors.textMain }]}>
                        Register New Hospital
                      </Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                      style={[
                        styles.roleButton,
                        {
                          backgroundColor: facilityMode === 'EXISTING' ? colors.primaryLight : colors.bgSubtle,
                          borderColor: facilityMode === 'EXISTING' ? colors.primary : colors.borderLight,
                        },
                      ]}
                      onPress={() => setFacilityMode('EXISTING')}
                    >
                      <Text style={[styles.roleText, { color: facilityMode === 'EXISTING' ? colors.primary : colors.textMain }]}>
                        Join Existing Hospital
                      </Text>
                    </TouchableOpacity>
                  </View>

                  {facilityMode === 'NEW' ? (
                    <>
                      <Text style={[styles.label, { color: colors.textMuted }]}>Hospital / Facility Name *</Text>
                      <TextInput
                        style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                        placeholder="e.g. Pune City General Hospital"
                        placeholderTextColor={colors.textSubtle}
                        value={newFacilityName}
                        onChangeText={setNewFacilityName}
                      />

                      <Text style={[styles.label, { color: colors.textMuted }]}>Facility Type</Text>
                      <View style={styles.roleGrid}>
                        {(['PHC', 'CHC', 'SUB_DISTRICT_HOSPITAL', 'DISTRICT_HOSPITAL'] as FacilityType[]).map((tVal) => (
                          <TouchableOpacity
                            key={tVal}
                            style={[
                              styles.roleButton,
                              {
                                backgroundColor: newFacilityType === tVal ? colors.primaryLight : colors.bgSubtle,
                                borderColor: newFacilityType === tVal ? colors.primary : colors.borderLight,
                              },
                            ]}
                            onPress={() => setNewFacilityType(tVal)}
                          >
                            <Text style={[styles.roleText, { color: newFacilityType === tVal ? colors.primary : colors.textMain }]}>
                              {tVal.replace(/_/g, ' ')}
                            </Text>
                          </TouchableOpacity>
                        ))}
                      </View>

                      <Text style={[styles.label, { color: colors.textMuted }]}>Hospital Contact Number</Text>
                      <TextInput
                        style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                        placeholder="10-digit telephone/mobile"
                        placeholderTextColor={colors.textSubtle}
                        keyboardType="phone-pad"
                        value={newFacilityPhone}
                        onChangeText={setNewFacilityPhone}
                      />

                      <Text style={[styles.label, { color: colors.textMuted }]}>Address</Text>
                      <TextInput
                        style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                        placeholder="Street / Area / Landmark"
                        placeholderTextColor={colors.textSubtle}
                        value={newFacilityAddress}
                        onChangeText={setNewFacilityAddress}
                      />

                      <View style={{ flexDirection: 'row', gap: 8 }}>
                        <View style={{ flex: 1 }}>
                          <Text style={[styles.label, { color: colors.textMuted }]}>Town / Village</Text>
                          <TextInput
                            style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                            placeholder="e.g. Pune"
                            placeholderTextColor={colors.textSubtle}
                            value={newFacilityVillage}
                            onChangeText={setNewFacilityVillage}
                          />
                        </View>
                        <View style={{ flex: 1 }}>
                          <Text style={[styles.label, { color: colors.textMuted }]}>District *</Text>
                          <TextInput
                            style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                            placeholder="e.g. Pune"
                            placeholderTextColor={colors.textSubtle}
                            value={newFacilityDistrict}
                            onChangeText={setNewFacilityDistrict}
                          />
                        </View>
                      </View>

                      <View style={{ flexDirection: 'row', gap: 8 }}>
                        <View style={{ flex: 1 }}>
                          <Text style={[styles.label, { color: colors.textMuted }]}>State</Text>
                          <TextInput
                            style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                            placeholder="e.g. Maharashtra"
                            placeholderTextColor={colors.textSubtle}
                            value={newFacilityState}
                            onChangeText={setNewFacilityState}
                          />
                        </View>
                        <View style={{ flex: 1 }}>
                          <Text style={[styles.label, { color: colors.textMuted }]}>Pincode</Text>
                          <TextInput
                            style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                            placeholder="6-digit pincode"
                            placeholderTextColor={colors.textSubtle}
                            keyboardType="numeric"
                            value={newFacilityPincode}
                            onChangeText={setNewFacilityPincode}
                          />
                        </View>
                      </View>

                      {/* Map Location Picker */}
                      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 8, marginBottom: 4 }}>
                        <Text style={[styles.label, { color: colors.textMuted, marginBottom: 0 }]}>
                          GPS Coordinates & Map
                        </Text>
                        <TouchableOpacity
                          style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}
                          onPress={handleUseCurrentLocation}
                        >
                          <Ionicons name="locate" size={14} color={colors.primary} />
                          <Text style={{ fontSize: 12, color: colors.primary, fontWeight: '700' }}>
                            Use My Current Location
                          </Text>
                        </TouchableOpacity>
                      </View>

                      <View style={{ borderRadius: 10, overflow: 'hidden', borderWidth: 1, borderColor: colors.borderLight, marginBottom: 8 }}>
                        <OutbreakLeafletMap
                          userLocation={{
                            latitude: parseFloat(newFacilityLat) || 18.5204,
                            longitude: parseFloat(newFacilityLng) || 73.8567,
                          }}
                          height={180}
                          facilities={[]}
                          zones={[]}
                          onLocationSelect={(coords) => {
                            setNewFacilityLat(coords.latitude.toFixed(4));
                            setNewFacilityLng(coords.longitude.toFixed(4));
                          }}
                        />
                      </View>
                      <Text style={{ fontSize: 11, color: colors.textSubtle, marginBottom: 8 }}>
                        Tap anywhere on the map to pin your hospital location.
                      </Text>

                      <View style={{ flexDirection: 'row', gap: 8 }}>
                        <View style={{ flex: 1 }}>
                          <Text style={[styles.label, { color: colors.textMuted }]}>Latitude</Text>
                          <TextInput
                            style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                            placeholder="e.g. 18.5204"
                            placeholderTextColor={colors.textSubtle}
                            keyboardType="numeric"
                            value={newFacilityLat}
                            onChangeText={setNewFacilityLat}
                          />
                        </View>
                        <View style={{ flex: 1 }}>
                          <Text style={[styles.label, { color: colors.textMuted }]}>Longitude</Text>
                          <TextInput
                            style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                            placeholder="e.g. 73.8567"
                            placeholderTextColor={colors.textSubtle}
                            keyboardType="numeric"
                            value={newFacilityLng}
                            onChangeText={setNewFacilityLng}
                          />
                        </View>
                      </View>
                    </>
                  ) : (
                    <>
                      <Text style={[styles.label, { color: colors.textMuted }]}>Select Healthcare Facility to Manage</Text>
                      <TextInput
                        style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain, marginBottom: 8 }]}
                        placeholder="Search facility by name or location..."
                        placeholderTextColor={colors.textSubtle}
                        value={facilitySearch}
                        onChangeText={setFacilitySearch}
                      />
                      {loadingFacilities ? (
                        <ActivityIndicator size="small" color={colors.primary} style={{ marginVertical: 12 }} />
                      ) : (
                        <View style={{ maxHeight: 220, marginBottom: 12 }}>
                          <ScrollView nestedScrollEnabled>
                            {filteredFacilities.map((fac) => (
                              <TouchableOpacity
                                key={fac.facilityId}
                                style={[
                                  styles.facilityItem,
                                  {
                                    backgroundColor: selectedFacilityId === fac.facilityId ? colors.primaryLight : colors.bgSubtle,
                                    borderColor: selectedFacilityId === fac.facilityId ? colors.primary : colors.borderLight,
                                  },
                                ]}
                                onPress={() => setSelectedFacilityId(fac.facilityId)}
                              >
                                <Text style={[styles.facilityName, { color: colors.textMain }]}>{fac.name}</Text>
                                <Text style={[styles.facilityDetail, { color: colors.textSubtle }]}>
                                  {fac.facilityTypeDisplayName || fac.facilityType} • {fac.villageTown || fac.district}
                                </Text>
                              </TouchableOpacity>
                            ))}
                          </ScrollView>
                        </View>
                      )}
                    </>
                  )}
                </>
              )}

              {role === 'CAREGIVER' && (
                <>
                  <Text style={[styles.label, { color: colors.textMuted }]}>Linked Patient Phone Number</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="e.g. 9811099882 (Patient's mobile)"
                    placeholderTextColor={colors.textSubtle}
                    keyboardType="phone-pad"
                    value={linkedPatientPhone}
                    onChangeText={setLinkedPatientPhone}
                  />

                  <Text style={[styles.label, { color: colors.textMuted }]}>Or Linked Patient Username</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="e.g. patient.anita.demo"
                    placeholderTextColor={colors.textSubtle}
                    autoCapitalize="none"
                    value={linkedPatientUsername}
                    onChangeText={setLinkedPatientUsername}
                  />
                </>
              )}

              {role === 'ASHA' && (
                <>
                  <Text style={[styles.label, { color: colors.textMuted }]}>Assigned Village / Sub-Centre Sector</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="e.g. Aerocity Sector 4"
                    placeholderTextColor={colors.textSubtle}
                    value={villageTown}
                    onChangeText={setVillageTown}
                  />
                </>
              )}

              <View style={styles.buttonRow}>
                <TouchableOpacity
                  style={[styles.backBtn, { borderColor: colors.borderLight }]}
                  onPress={() => setStep(1)}
                >
                  <Ionicons name="arrow-back" size={18} color={colors.textMain} />
                  <Text style={[styles.backBtnText, { color: colors.textMain }]}>Back</Text>
                </TouchableOpacity>

                <Button
                  title={isLoading ? t('registering') : 'Complete Registration'}
                  onPress={handleRegister}
                  loading={isLoading}
                  style={styles.completeBtn}
                />
              </View>
            </>
          )}

          <TouchableOpacity style={styles.backLink} onPress={() => navigation.goBack()}>
            <Text style={[styles.backLinkText, { color: colors.primary }]}>
              {t('alreadyHaveAccount')}
            </Text>
          </TouchableOpacity>
        </Card>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  scrollContainer: {
    padding: 20,
    paddingTop: Platform.OS === 'android' ? 40 : 20,
    paddingBottom: 40,
  },
  card: {
    padding: 20,
    borderRadius: 18,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  title: {
    fontSize: 22,
    fontWeight: '800',
  },
  stepBadge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  stepText: {
    fontSize: 12,
    fontWeight: '700',
  },
  subtitle: {
    fontSize: 13,
    marginTop: 4,
    marginBottom: 16,
  },
  errorBox: {
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    marginBottom: 12,
  },
  errorText: {
    fontSize: 13,
    fontWeight: '600',
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 6,
    marginTop: 8,
  },
  input: {
    height: 48,
    borderRadius: 10,
    borderWidth: 1,
    paddingHorizontal: 14,
    fontSize: 15,
  },
  roleGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  roleButton: {
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 10,
    borderWidth: 1,
  },
  roleText: {
    fontSize: 12,
    fontWeight: '700',
  },
  submitBtn: {
    marginTop: 12,
  },
  buttonRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginTop: 16,
  },
  backBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 10,
    borderWidth: 1,
    gap: 6,
  },
  backBtnText: {
    fontSize: 14,
    fontWeight: '600',
  },
  completeBtn: {
    flex: 1,
  },
  facilityItem: {
    padding: 12,
    borderRadius: 10,
    borderWidth: 1,
    marginBottom: 8,
  },
  facilityName: {
    fontSize: 14,
    fontWeight: '700',
  },
  facilityDetail: {
    fontSize: 12,
    marginTop: 2,
  },
  backLink: {
    marginTop: 16,
    alignItems: 'center',
  },
  backLinkText: {
    fontSize: 13,
    fontWeight: '700',
  },
  googleLinkedBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderRadius: 10,
    borderWidth: 1,
    marginBottom: 16,
  },
  googleLinkedTitle: {
    fontSize: 13,
    fontWeight: '700',
  },
  googleLinkedSub: {
    fontSize: 12,
    marginTop: 2,
  },
  dividerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 14,
  },
  dividerLine: {
    flex: 1,
    height: 1,
  },
  dividerText: {
    fontSize: 12,
    marginHorizontal: 10,
    fontWeight: '500',
  },
});

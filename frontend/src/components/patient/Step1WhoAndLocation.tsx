import React, { useState, useEffect } from 'react';
import { MapPin, User, Users, AlertCircle, ArrowRight, RefreshCw, CheckCircle2, Search } from 'lucide-react';
import { patientApi } from '../../api/patientApi';
import { caseApi } from '../../api/caseApi';
import { PatientDto, Gender } from '../../types/patient';
import { HealthcareCaseDto } from '../../types/case';
import { AlertBox } from '../common/AlertBox';

interface Step1WhoAndLocationProps {
  onCaseCreated: (patient: PatientDto, healthcareCase: HealthcareCaseDto, coordinates: { lat: number; lng: number }) => void;
  initialPatient?: PatientDto | null;
  initialCoords?: { lat: number; lng: number } | null;
}

export const Step1WhoAndLocation: React.FC<Step1WhoAndLocationProps> = ({
  onCaseCreated,
  initialPatient,
  initialCoords,
}) => {
  const [screeningFor, setScreeningFor] = useState<'myself' | 'someone_else'>('myself');

  // Existing vs New Patient State
  const [existingPatient, setExistingPatient] = useState<PatientDto | null>(initialPatient || null);
  const [isSearchingPatient, setIsSearchingPatient] = useState(false);
  const [patientLookupNotice, setPatientLookupNotice] = useState<string | null>(null);

  // Form Fields (Required by backend CreatePatientRequest)
  const [fullName, setFullName] = useState(initialPatient?.fullName || '');
  const [age, setAge] = useState<number | string>(initialPatient?.age !== undefined ? initialPatient.age : '');
  const [gender, setGender] = useState<Gender>(initialPatient?.gender || 'MALE');
  const [contactNumber, setContactNumber] = useState(initialPatient?.contactNumber || '');
  const [villageTown, setVillageTown] = useState(initialPatient?.villageTown || 'Rampur');
  const [subDistrict, setSubDistrict] = useState(initialPatient?.subDistrict || 'Mohanlalganj');
  const [district, setDistrict] = useState(initialPatient?.district || 'Lucknow');
  const [state, setState] = useState(initialPatient?.state || 'Uttar Pradesh');
  const [pincode, setPincode] = useState(initialPatient?.pincode || '226301');

  // Location State
  const [locationStatus, setLocationStatus] = useState<'detecting' | 'detected' | 'denied' | 'fallback'>('detecting');
  const [coords, setCoords] = useState<{ lat: number; lng: number }>(
    initialCoords || { lat: 26.8467, lng: 80.9462 } // Lucknow / Rampur regional coordinates
  );

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Request browser geolocation on mount
  const detectLocation = () => {
    setLocationStatus('detecting');
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setCoords({
            lat: Number(position.coords.latitude.toFixed(6)),
            lng: Number(position.coords.longitude.toFixed(6)),
          });
          setLocationStatus('detected');
        },
        (_error) => {
          setLocationStatus('denied');
        },
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 }
      );
    } else {
      setLocationStatus('fallback');
    }
  };

  useEffect(() => {
    detectLocation();
  }, []);

  // Format phone to standard E.164 (+91XXXXXXXXXX)
  const formatPhoneNumber = (phone: string): string => {
    const digits = phone.replace(/[^0-9]/g, '');
    if (digits.length === 10) {
      return `+91${digits}`;
    }
    if (digits.length === 12 && digits.startsWith('91')) {
      return `+${digits}`;
    }
    return phone.trim();
  };

  // Lookup existing patient by contact number
  const handleLookupContact = async (phoneToLookup?: string) => {
    const raw = (phoneToLookup || contactNumber).trim();
    const formatted = formatPhoneNumber(raw);
    if (!formatted.match(/^(\+91[\\-\\s]?)?[0-9]{10,12}$/)) {
      setPatientLookupNotice('Please enter a valid 10-digit mobile number before searching.');
      return;
    }

    setIsSearchingPatient(true);
    setPatientLookupNotice(null);
    setErrorMessage(null);

    try {
      const found = await patientApi.getPatientByContact(formatted);
      if (found && found.id) {
        setExistingPatient(found);
        setFullName(found.fullName);
        setAge(found.age || 35);
        setGender(found.gender);
        setVillageTown(found.villageTown || found.village || '');
        if (found.subDistrict) setSubDistrict(found.subDistrict);
        if (found.district) setDistrict(found.district);
        if (found.state) setState(found.state);
        if (found.pincode) setPincode(found.pincode);
        if (found.latitude && found.longitude) {
          setCoords({ lat: found.latitude, lng: found.longitude });
          setLocationStatus('detected');
        }
        setPatientLookupNotice(`✓ Registered patient profile found: ${found.fullName} (${found.age} yrs, ${found.villageTown}).`);
      }
    } catch (err: any) {
      // 404 means new patient
      setExistingPatient(null);
      setPatientLookupNotice('New patient number. Please enter demographic details below to register.');
    } finally {
      setIsSearchingPatient(false);
    }
  };

  const handleContinue = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitting) return; // Prevent duplicate submissions

    setErrorMessage(null);

    const trimmedName = fullName.trim();
    if (!trimmedName || trimmedName.length < 2) {
      setErrorMessage('Please enter patient full name (at least 2 characters) • कृपया पूर्ण नाव प्रविष्ट करा');
      return;
    }

    const parsedAge = Number(age);
    if (isNaN(parsedAge) || parsedAge < 0 || parsedAge > 120 || age === '') {
      setErrorMessage('Please enter a valid age between 0 and 120 • कृपया योग्य वय प्रविष्ट करा');
      return;
    }

    const formattedContact = formatPhoneNumber(contactNumber);
    if (!formattedContact.match(/^(\+91[\\-\\s]?)?[0-9]{10,12}$/)) {
      setErrorMessage('Please enter a valid 10-digit mobile number • कृपया 10 अंकी मोबाईल क्रमांक प्रविष्ट करा');
      return;
    }

    const trimmedVillage = villageTown.trim();
    if (!trimmedVillage) {
      setErrorMessage('Please enter your village or town • कृपया गाव किंवा शहर प्रविष्ट करा');
      return;
    }

    if (!pincode.trim().match(/^[1-9][0-9]{5}$/)) {
      setErrorMessage('Please enter a valid 6-digit Indian pincode • कृपया योग्य 6-अंकी पिनकोड प्रविष्ट करा');
      return;
    }

    setIsSubmitting(true);
    try {
      let patient = existingPatient;

      // 1. If not an existing patient, register in backend
      if (!patient) {
        try {
          const created = await patientApi.createPatient({
            fullName: trimmedName,
            age: parsedAge,
            gender,
            contactNumber: formattedContact,
            villageTown: trimmedVillage,
            subDistrict: subDistrict.trim(),
            district: district.trim(),
            state: state.trim(),
            pincode: pincode.trim(),
            latitude: coords.lat,
            longitude: coords.lng,
          });
          patient = created;
        } catch (createErr: any) {
          // If already exists, retrieve existing patient
          if (createErr.message && createErr.message.includes('already registered')) {
            const found = await patientApi.getPatientByContact(formattedContact);
            patient = found;
          } else {
            throw createErr;
          }
        }
      }

      if (!patient || !patient.id) {
        throw new Error('Failed to obtain a valid patient record from the server.');
      }

      localStorage.setItem('aarogya_patient_id', patient.id);
      localStorage.setItem('aarogya_patient_name', patient.fullName);

      // 2. Automatically create HealthcareCase in the backend
      const caseRes = await caseApi.createCase({
        patientId: patient.id,
        intakeChannel: 'FORM',
        latitude: coords.lat,
        longitude: coords.lng,
        chiefComplaint: `Screening for ${patient.fullName}`,
        notes: `Intake initiated for ${screeningFor === 'myself' ? 'patient directly' : 'dependent/family member'} in ${patient.villageTown}`,
      });

      if (!caseRes || !caseRes.id) {
        throw new Error('Failed to obtain a valid healthcare case ID from the server.');
      }

      // Pass created patient and healthcare case to parent wizard
      onCaseCreated(patient, caseRes, coords);
    } catch (err: any) {
      setErrorMessage(err.message || 'Could not start screening. Please check backend connection.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleContinue} className="card-simple" style={{ maxWidth: '640px', margin: '0 auto' }}>
      <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '0.4rem' }}>
        Step 1: Patient Information & Location
      </h2>
      <p style={{ color: 'var(--text-muted)', fontSize: '1.05rem', marginBottom: '1.5rem' }}>
        सुरुवात: रुग्णाची माहिती आणि ठिकाण
      </p>

      {errorMessage && (
        <AlertBox type="danger">
          <strong>{errorMessage}</strong>
        </AlertBox>
      )}

      {/* Choice: Myself vs Someone Else */}
      <div className="form-group">
        <label className="form-label">Screening For (कोणासाठी)</label>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
          <button
            type="button"
            className={`btn-large ${screeningFor === 'myself' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setScreeningFor('myself')}
            aria-pressed={screeningFor === 'myself'}
          >
            <User size={22} />
            <span>Myself • माझ्यासाठी</span>
          </button>

          <button
            type="button"
            className={`btn-large ${screeningFor === 'someone_else' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setScreeningFor('someone_else')}
            aria-pressed={screeningFor === 'someone_else'}
          >
            <Users size={22} />
            <span>Someone else • दुसऱ्यासाठी</span>
          </button>
        </div>
      </div>

      {/* Contact Number with Lookup / Verification */}
      <div className="form-group">
        <label htmlFor="patient-phone" className="form-label">
          Mobile Number <span style={{ color: 'var(--risk-high)' }}>*</span>
          <span className="form-label-hint">(मोबाईल नंबर - 10 अंक)</span>
        </label>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <input
            id="patient-phone"
            type="tel"
            className="form-input"
            placeholder="e.g. 9822012345"
            value={contactNumber}
            onChange={(e) => {
              setContactNumber(e.target.value);
              setExistingPatient(null);
              setPatientLookupNotice(null);
            }}
            required
            maxLength={13}
          />
          <button
            type="button"
            className="btn-large btn-outline"
            style={{ width: 'auto', minHeight: '48px', padding: '0.5rem 1rem', fontSize: '0.95rem', flexShrink: 0 }}
            onClick={() => handleLookupContact()}
            disabled={isSearchingPatient || !contactNumber.trim()}
          >
            {isSearchingPatient ? <RefreshCw size={18} className="spin-fast" /> : <Search size={18} />}
            <span>Lookup • शोधा</span>
          </button>
        </div>
        {patientLookupNotice && (
          <p style={{ fontSize: '0.9rem', color: existingPatient ? 'var(--secondary)' : 'var(--text-muted)', marginTop: '0.35rem', fontWeight: 600 }}>
            {patientLookupNotice}
          </p>
        )}
      </div>

      {/* If existing patient found, show confirmation badge */}
      {existingPatient && (
        <div
          style={{
            backgroundColor: 'var(--secondary-light)',
            border: '2px solid var(--secondary)',
            borderRadius: 'var(--radius-md)',
            padding: '1rem',
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '0.5rem',
          }}
        >
          <div>
            <strong style={{ color: 'var(--secondary)', fontSize: '1.05rem', display: 'block' }}>
              ✓ Registered Patient Profile Selected
            </strong>
            <span style={{ fontSize: '0.95rem', color: 'var(--text-main)' }}>
              {existingPatient.fullName} • Age: {existingPatient.age} • {existingPatient.villageTown}
            </span>
          </div>
          <button
            type="button"
            className="btn-ghost"
            style={{ padding: '0.3rem 0.6rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}
            onClick={() => {
              setExistingPatient(null);
              setPatientLookupNotice(null);
            }}
          >
            Clear / Register New
          </button>
        </div>
      )}

      {/* Full Name */}
      <div className="form-group">
        <label htmlFor="patient-name" className="form-label">
          Full Name <span style={{ color: 'var(--risk-high)' }}>*</span>
          <span className="form-label-hint">(पूर्ण नाव)</span>
        </label>
        <input
          id="patient-name"
          type="text"
          className="form-input"
          placeholder="e.g. Ramesh Patil"
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          required
        />
      </div>

      {/* Age & Gender */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
        <div className="form-group">
          <label htmlFor="patient-age" className="form-label">
            Age <span style={{ color: 'var(--risk-high)' }}>*</span>
            <span className="form-label-hint">(वय)</span>
          </label>
          <input
            id="patient-age"
            type="number"
            min="0"
            max="120"
            className="form-input"
            placeholder="e.g. 58"
            value={age}
            onChange={(e) => setAge(e.target.value === '' ? '' : Number(e.target.value))}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="patient-gender" className="form-label">
            Gender <span style={{ color: 'var(--risk-high)' }}>*</span>
            <span className="form-label-hint">(लिंग)</span>
          </label>
          <select
            id="patient-gender"
            className="form-select"
            value={gender}
            onChange={(e) => setGender(e.target.value as Gender)}
          >
            <option value="MALE">Male (पुरुष)</option>
            <option value="FEMALE">Female (महिला)</option>
            <option value="OTHER">Other (इतर)</option>
          </select>
        </div>
      </div>

      {/* Village & Pincode */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
        <div className="form-group">
          <label htmlFor="patient-village" className="form-label">
            Village / Town <span style={{ color: 'var(--risk-high)' }}>*</span>
            <span className="form-label-hint">(गाव / शहर)</span>
          </label>
          <input
            id="patient-village"
            type="text"
            className="form-input"
            placeholder="e.g. Shirwal"
            value={villageTown}
            onChange={(e) => setVillageTown(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="patient-pincode" className="form-label">
            Pincode <span style={{ color: 'var(--risk-high)' }}>*</span>
            <span className="form-label-hint">(पिनकोड)</span>
          </label>
          <input
            id="patient-pincode"
            type="text"
            maxLength={6}
            className="form-input"
            placeholder="e.g. 412801"
            value={pincode}
            onChange={(e) => setPincode(e.target.value)}
            required
          />
        </div>
      </div>

      {/* Geolocation Human-Friendly Banner */}
      <div style={{ margin: '1.25rem 0' }}>
        {locationStatus === 'detecting' && (
          <div className="alert-box alert-info">
            <RefreshCw size={24} className="spin-fast" style={{ color: 'var(--primary)', flexShrink: 0 }} />
            <div>
              <strong>Detecting approximate location...</strong>
              <p style={{ fontSize: '0.9rem', margin: 0 }}>
                This helps us find the closest Primary Health Centre (PHC) for you.
              </p>
            </div>
          </div>
        )}

        {locationStatus === 'detected' && (
          <div className="alert-box alert-success">
            <CheckCircle2 size={24} style={{ color: 'var(--secondary)', flexShrink: 0 }} />
            <div>
              <strong>📍 Location detected ({coords.lat.toFixed(4)}, {coords.lng.toFixed(4)})</strong>
              <p style={{ fontSize: '0.9rem', margin: 0 }}>
                Your approximate location will be used to find nearby healthcare facilities.
              </p>
            </div>
          </div>
        )}

        {(locationStatus === 'denied' || locationStatus === 'fallback') && (
          <div className="alert-box alert-warning">
            <MapPin size={24} style={{ color: 'var(--accent-warm)', flexShrink: 0 }} />
            <div>
              <strong>Using regional area coordinates ({coords.lat}, {coords.lng})</strong>
              <p style={{ fontSize: '0.9rem', margin: '0.2rem 0' }}>
                Location permission was not provided. Using default regional area ({villageTown || 'Satara/Shirwal'}).
              </p>
              <button
                type="button"
                className="btn-large btn-outline"
                style={{ minHeight: '38px', padding: '0.4rem 0.8rem', fontSize: '0.9rem', marginTop: '0.4rem' }}
                onClick={detectLocation}
              >
                <RefreshCw size={16} /> Retry Location Detection
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Next Action Button */}
      <div style={{ marginTop: '1.75rem' }}>
        <button
          type="submit"
          className="btn-large btn-primary"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
              <RefreshCw size={20} className="spin-fast" />
              Starting Screening...
            </span>
          ) : (
            <>
              <span>CONTINUE TO SYMPTOMS • पुढील पायरी</span>
              <ArrowRight size={22} />
            </>
          )}
        </button>
      </div>
    </form>
  );
};

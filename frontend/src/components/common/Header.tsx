import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useLanguage, SupportedLanguage } from '../../context/LanguageContext';
import { useSos } from '../../hooks/useSos';
import { UserRole } from '../../types/auth';
import {
  HeartPulse,
  Globe,
  Sun,
  Moon,
  LogOut,
  LogIn,
  AlertTriangle,
  User,
  Shield,
  Stethoscope,
  Building2,
  Users,
  PhoneCall,
} from 'lucide-react';
import { LoginModal } from './LoginModal';
import { AarogyaMitraVoiceCallModal } from '../calling/AarogyaMitraVoiceCallModal';
import { authApi } from '../../api/authApi';
import { MapPin, Navigation, CheckCircle, X } from 'lucide-react';

export const Header: React.FC = () => {
  const { user, role, isAuthenticated, logout, switchDemoRole } = useAuth();
  const { language, setLanguage, isHighContrast, toggleHighContrast, fontSize, cycleFontSize, t } = useLanguage();
  const {
    setIsSosModalOpen,
    currentCoords,
    detectLocation,
    isLocating,
    emergencyAlertOptIn,
    syncDeviceAndLocation,
  } = useSos();
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isVoiceModalOpen, setIsVoiceModalOpen] = useState(false);
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);

  // Profile & Live Location form state
  const [profileFullName, setProfileFullName] = useState(user?.displayName || user?.fullName || '');
  const [profilePhone, setProfilePhone] = useState(user?.contactNumber || user?.phoneNumber || '');
  const [profileEmail, setProfileEmail] = useState(user?.email || '');
  const [profileVillage, setProfileVillage] = useState('Palasdeo');
  const [profileDistrict, setProfileDistrict] = useState('Pune');
  const [profileLat, setProfileLat] = useState<string>(currentCoords ? String(currentCoords.latitude) : '18.5204');
  const [profileLon, setProfileLon] = useState<string>(currentCoords ? String(currentCoords.longitude) : '73.8567');
  const [profileOptIn, setProfileOptIn] = useState<boolean>(emergencyAlertOptIn);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileStatusMsg, setProfileStatusMsg] = useState<string | null>(null);
  const [profileErrorMsg, setProfileErrorMsg] = useState<string | null>(null);

  const openProfileModal = () => {
    setProfileFullName(user?.displayName || user?.fullName || '');
    setProfilePhone(user?.contactNumber || user?.phoneNumber || '');
    setProfileEmail(user?.email || '');
    if (currentCoords) {
      setProfileLat(String(currentCoords.latitude));
      setProfileLon(String(currentCoords.longitude));
    }
    setProfileOptIn(emergencyAlertOptIn);
    setProfileStatusMsg(null);
    setProfileErrorMsg(null);
    setIsProfileModalOpen(true);
  };

  const handleSaveProfileAndLocation = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSavingProfile(true);
    setProfileStatusMsg(null);
    setProfileErrorMsg(null);
    const latNum = parseFloat(profileLat);
    const lonNum = parseFloat(profileLon);
    try {
      await authApi.updateProfile({
        fullName: profileFullName.trim() || undefined,
        phoneNumber: profilePhone.trim() || undefined,
        email: profileEmail.trim() || undefined,
        villageTown: profileVillage.trim() || undefined,
        district: profileDistrict.trim() || undefined,
        latitude: !isNaN(latNum) ? latNum : undefined,
        longitude: !isNaN(lonNum) ? lonNum : undefined,
      });
      if (!isNaN(latNum) && !isNaN(lonNum)) {
        await syncDeviceAndLocation(latNum, lonNum, profileOptIn);
      }
      setProfileStatusMsg('Profile & 5 km SOS live location saved successfully!');
    } catch (err: any) {
      if (!isNaN(latNum) && !isNaN(lonNum)) {
        await syncDeviceAndLocation(latNum, lonNum, profileOptIn);
      }
      setProfileErrorMsg(err.message || 'Updated local SOS GPS responder coordinates.');
    } finally {
      setIsSavingProfile(false);
    }
  };

  const getRoleIcon = (r: UserRole) => {
    switch (r) {
      case 'PATIENT':
        return <User size={16} />;
      case 'ASHA':
        return <Users size={16} />;
      case 'DOCTOR':
        return <Stethoscope size={16} />;
      case 'ADMIN':
      case 'FACILITY_ADMIN':
        return <Building2 size={16} />;
      case 'CAREGIVER':
        return <Shield size={16} />;
      default:
        return <User size={16} />;
    }
  };

  const getRoleLabel = (r: UserRole) => {
    switch (r) {
      case 'PATIENT':
        return t('rolePatient');
      case 'ASHA':
        return t('roleAsha');
      case 'DOCTOR':
        return t('roleDoctor');
      case 'ADMIN':
        return t('roleFacility') || 'Admin';
      case 'FACILITY_ADMIN':
        return 'Facility Staff';
      case 'CAREGIVER':
        return t('roleCaregiver');
      default:
        return r;
    }
  };

  return (
    <>
      {/* Demo Mode Notice & Role Switcher */}
      <div className="demo-banner">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
          <AlertTriangle size={16} />
          <span>{t('demoModeNotice')}</span>
          {currentCoords && (
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.3rem',
                padding: '0.12rem 0.55rem',
                borderRadius: '9999px',
                backgroundColor: 'rgba(13, 148, 136, 0.14)',
                color: '#0f766e',
                fontSize: '0.74rem',
                fontWeight: 800,
                border: '1px solid rgba(13, 148, 136, 0.35)',
              }}
              title="5 km Geo-Fenced Emergency Responder Active"
            >
              <span className="pulse-dot" style={{ backgroundColor: '#0d9488', color: '#0d9488', width: '6px', height: '6px' }} />
              <span>5 km SOS GPS Ready ({currentCoords.latitude.toFixed(2)}, {currentCoords.longitude.toFixed(2)})</span>
            </span>
          )}
        </div>
        <div className="demo-buttons-group">
          {(['PATIENT', 'ASHA', 'DOCTOR', 'FACILITY_ADMIN'] as UserRole[]).map((r) => (
            <button
              key={r}
              className={`demo-btn ${role === r ? 'active' : ''}`}
              onClick={() => switchDemoRole(r)}
              title={`Switch to ${r} portal`}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.35rem',
              }}
            >
              {getRoleIcon(r)}
              <span>{r === 'FACILITY_ADMIN' ? 'FACILITY PORTAL' : `${r} PORTAL`}</span>
            </button>
          ))}
        </div>
      </div>

      <header className="app-header">
        <div className="header-inner">
          {/* Brand */}
          <div className="brand-container">
            <div className="brand-logo-icon">
              <HeartPulse size={26} />
            </div>
            <div className="brand-text">
              <h1>{t('brandName')}</h1>
              <span>{t('brandSubtitle')}</span>
            </div>
          </div>

          {/* Controls */}
          <div className="header-actions">
            {/* Language Selector */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <Globe size={18} color="var(--text-subtle)" />
              <select
                className="form-control"
                style={{ minHeight: '40px', padding: '0.25rem 0.5rem', width: 'auto', fontWeight: 600 }}
                value={language}
                onChange={(e) => setLanguage(e.target.value as SupportedLanguage)}
                aria-label={t('language')}
              >
                <option value="en">English</option>
                <option value="hi">हिन्दी (Hindi)</option>
                <option value="mr">मराठी (Marathi)</option>
              </select>
            </div>

            {/* High Contrast Toggle */}
            <button
              className="icon-btn"
              onClick={toggleHighContrast}
              aria-label={t('highContrast')}
              title={t('highContrast')}
            >
              {isHighContrast ? <Sun size={18} /> : <Moon size={18} />}
              <span style={{ fontSize: '0.85rem' }}>{isHighContrast ? 'Normal' : 'High Contrast'}</span>
            </button>

            {/* Accessible Font Size Adjuster (A / A+ / A++) */}
            <button
              className="icon-btn"
              onClick={cycleFontSize}
              aria-label="Adjust Text Size for Readability"
              title={`Text Size: ${fontSize.toUpperCase()} (Click to toggle)`}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.35rem',
                minWidth: '70px',
                justifyContent: 'center',
              }}
            >
              <strong style={{ fontSize: fontSize === 'xlarge' ? '1.15rem' : fontSize === 'large' ? '1rem' : '0.88rem' }}>
                A{fontSize === 'xlarge' ? '++' : fontSize === 'large' ? '+' : ''}
              </strong>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-subtle)' }}>
                {fontSize === 'xlarge' ? 'Max' : fontSize === 'large' ? 'Large' : 'Normal'}
              </span>
            </button>

            {/* Aarogya Mitra Voice Call Button */}
            <button
              className="btn btn-outline"
              style={{
                minHeight: '40px',
                padding: '0 0.85rem',
                fontWeight: 700,
                borderColor: 'var(--primary)',
                color: 'var(--primary)',
              }}
              onClick={() => setIsVoiceModalOpen(true)}
              title="Aarogya Mitra AI Voice Healthcare Call"
            >
              <PhoneCall size={18} />
              <span>AI Call</span>
            </button>

            {/* Emergency SOS Quick Button */}
            <button
              className="btn btn-danger"
              style={{ minHeight: '40px', padding: '0 0.85rem', fontWeight: 700 }}
              onClick={() => setIsSosModalOpen(true)}
            >
              <AlertTriangle size={18} />
              <span>SOS</span>
            </button>

            {/* Auth Profile / Login */}
            {isAuthenticated ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <div
                  onClick={openProfileModal}
                  title="Update Profile & Live Location (5 km SOS Responder Settings)"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.4rem',
                    background: 'var(--bg-subtle)',
                    padding: '0.35rem 0.75rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-medium)',
                    cursor: 'pointer',
                  }}
                >
                  {getRoleIcon(role)}
                  <div style={{ textAlign: 'left', lineHeight: 1.1 }}>
                    <div style={{ fontSize: '0.85rem', fontWeight: 700 }}>
                      {user?.displayName || user?.username}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--primary)', display: 'flex', alignItems: 'center', gap: '0.2rem' }}>
                      <span>{getRoleLabel(role)}</span>
                      <MapPin size={11} />
                    </div>
                  </div>
                </div>

                <button
                  className="icon-btn"
                  onClick={logout}
                  title={t('logout')}
                  aria-label={t('logout')}
                >
                  <LogOut size={18} />
                </button>
              </div>
            ) : (
              <button
                className="btn btn-primary"
                style={{ minHeight: '40px' }}
                onClick={() => setIsLoginModalOpen(true)}
              >
                <LogIn size={18} />
                <span>{t('login')}</span>
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Login Modal */}
      {isLoginModalOpen && <LoginModal onClose={() => setIsLoginModalOpen(false)} />}

      {/* Aarogya Mitra AI Voice Call Modal */}
      <AarogyaMitraVoiceCallModal
        isOpen={isVoiceModalOpen}
        onClose={() => setIsVoiceModalOpen(false)}
      />

      {/* Profile & Live Location Modal */}
      {isProfileModalOpen && (
        <div className="modal-overlay" onClick={() => setIsProfileModalOpen(false)} role="dialog" aria-modal="true">
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <User size={20} color="var(--primary)" />
                <h2 style={{ fontSize: '1.2rem', fontWeight: 700 }}>
                  Profile & Live GPS Location
                </h2>
              </div>
              <button className="icon-btn" onClick={() => setIsProfileModalOpen(false)} aria-label="Close">
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSaveProfileAndLocation}>
              <div className="modal-body" style={{ maxHeight: '72vh', overflowY: 'auto' }}>
                {profileStatusMsg && (
                  <div className="alert-box success" style={{ marginBottom: '1rem' }}>
                    <CheckCircle size={18} />
                    <span>{profileStatusMsg}</span>
                  </div>
                )}
                {profileErrorMsg && (
                  <div className="alert-box warning" style={{ marginBottom: '1rem' }}>
                    <AlertTriangle size={18} />
                    <span>{profileErrorMsg}</span>
                  </div>
                )}

                <div className="form-group">
                  <label className="form-label">Full Name</label>
                  <input
                    type="text"
                    className="form-control"
                    value={profileFullName}
                    onChange={(e) => setProfileFullName(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div className="form-group">
                    <label className="form-label">Mobile Number</label>
                    <input
                      type="tel"
                      className="form-control"
                      value={profilePhone}
                      onChange={(e) => setProfilePhone(e.target.value)}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Email</label>
                    <input
                      type="email"
                      className="form-control"
                      value={profileEmail}
                      onChange={(e) => setProfileEmail(e.target.value)}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div className="form-group">
                    <label className="form-label">Village / Town</label>
                    <input
                      type="text"
                      className="form-control"
                      value={profileVillage}
                      onChange={(e) => setProfileVillage(e.target.value)}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">District</label>
                    <input
                      type="text"
                      className="form-control"
                      value={profileDistrict}
                      onChange={(e) => setProfileDistrict(e.target.value)}
                    />
                  </div>
                </div>

                <div
                  style={{
                    background: 'var(--bg-subtle)',
                    padding: '1rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-light)',
                    marginBottom: '1rem',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                    <div style={{ fontWeight: 700, fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                      <MapPin size={16} color="var(--primary)" />
                      <span>Live GPS Coordinates (5 km SOS Radius)</span>
                    </div>
                    <button
                      type="button"
                      className="btn btn-outline"
                      style={{ minHeight: '32px', padding: '0 0.6rem', fontSize: '0.8rem' }}
                      onClick={() => {
                        detectLocation();
                        if (navigator.geolocation) {
                          navigator.geolocation.getCurrentPosition((pos) => {
                            setProfileLat(pos.coords.latitude.toFixed(6));
                            setProfileLon(pos.coords.longitude.toFixed(6));
                          });
                        }
                      }}
                      disabled={isLocating}
                    >
                      <Navigation size={13} />
                      <span>{isLocating ? 'Locating...' : 'Detect GPS'}</span>
                    </button>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                    <div>
                      <label className="form-label" style={{ fontSize: '0.8rem' }}>Latitude</label>
                      <input
                        type="number"
                        step="any"
                        className="form-control"
                        value={profileLat}
                        onChange={(e) => setProfileLat(e.target.value)}
                      />
                    </div>
                    <div>
                      <label className="form-label" style={{ fontSize: '0.8rem' }}>Longitude</label>
                      <input
                        type="number"
                        step="any"
                        className="form-control"
                        value={profileLon}
                        onChange={(e) => setProfileLon(e.target.value)}
                      />
                    </div>
                  </div>

                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.75rem', fontSize: '0.85rem', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={profileOptIn}
                      onChange={(e) => setProfileOptIn(e.target.checked)}
                      style={{ width: '18px', height: '18px', accentColor: 'var(--primary)' }}
                    />
                    <span>Opt-in to receive SOS alerts for emergencies within 5 km</span>
                  </label>
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-outline" onClick={() => setIsProfileModalOpen(false)}>
                  Close
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSavingProfile}>
                  {isSavingProfile ? 'Saving...' : 'Save Profile & Location'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
};

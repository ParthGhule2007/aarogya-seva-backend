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
  ChevronDown,
} from 'lucide-react';
import { LoginModal } from './LoginModal';
import { AarogyaMitraVoiceCallModal } from '../calling/AarogyaMitraVoiceCallModal';
import { GlobalSearchBar } from './GlobalSearchBar';
import { UserProfileModal } from './UserProfileModal';

export const Header: React.FC = () => {
  const { user, role, isAuthenticated, logout, switchDemoRole } = useAuth();
  const { language, setLanguage, isHighContrast, toggleHighContrast, fontSize, cycleFontSize, t } = useLanguage();
  const { setIsSosModalOpen, currentCoords } = useSos();

  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isVoiceModalOpen, setIsVoiceModalOpen] = useState(false);
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [profileInitialTab, setProfileInitialTab] = useState<'profile' | 'caregivers' | 'location'>('profile');

  const openProfileWithTab = (tab: 'profile' | 'caregivers' | 'location' = 'profile') => {
    setProfileInitialTab(tab);
    setIsProfileModalOpen(true);
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

  const displayName = user?.displayName || user?.fullName || 'Ramesh Patil';

  return (
    <>
      {/* Demo Mode Notice & Role Switcher Banner */}
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
              <span className="pulse-dot" style={{ backgroundColor: '#0d9488', width: '6px', height: '6px' }} />
              <span>5 km SOS GPS ({currentCoords.latitude.toFixed(2)}, {currentCoords.longitude.toFixed(2)})</span>
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

      {/* Main Header */}
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

          {/* GLOBAL SEARCH BAR (Prominent in Header) */}
          <div className="header-search-slot">
            <GlobalSearchBar
              onOpenProfile={openProfileWithTab}
              onOpenVoiceCall={() => setIsVoiceModalOpen(true)}
            />
          </div>

          {/* Header Controls & Profile Button */}
          <div className="header-actions">
            {/* Language Selector */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <Globe size={18} color="var(--text-subtle)" />
              <select
                className="form-control"
                style={{ minHeight: '38px', padding: '0.2rem 0.5rem', width: 'auto', fontWeight: 600, fontSize: '0.85rem' }}
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
              style={{ minHeight: '38px', padding: '0 0.6rem' }}
            >
              {isHighContrast ? <Sun size={17} /> : <Moon size={17} />}
            </button>

            {/* Accessible Font Size Adjuster (A / A+ / A++) */}
            <button
              className="icon-btn"
              onClick={cycleFontSize}
              aria-label="Adjust Text Size for Readability"
              title={`Text Size: ${fontSize.toUpperCase()}`}
              style={{ minHeight: '38px', padding: '0 0.5rem', minWidth: '48px', justifyContent: 'center' }}
            >
              <strong style={{ fontSize: fontSize === 'xlarge' ? '1.1rem' : fontSize === 'large' ? '0.95rem' : '0.85rem' }}>
                A{fontSize === 'xlarge' ? '++' : fontSize === 'large' ? '+' : ''}
              </strong>
            </button>

            {/* Aarogya Mitra Voice Call Button */}
            <button
              className="btn btn-outline voice-call-header-btn"
              style={{
                minHeight: '38px',
                padding: '0 0.75rem',
                fontWeight: 700,
                borderColor: 'var(--primary)',
                color: 'var(--primary)',
                fontSize: '0.85rem',
              }}
              onClick={() => setIsVoiceModalOpen(true)}
              title="Aarogya Mitra AI Voice Healthcare Call"
            >
              <PhoneCall size={16} />
              <span>AI Call</span>
            </button>

            {/* Emergency SOS Quick Button */}
            <button
              className="btn btn-danger sos-header-btn"
              style={{ minHeight: '38px', padding: '0 0.85rem', fontWeight: 800, fontSize: '0.85rem' }}
              onClick={() => setIsSosModalOpen(true)}
            >
              <AlertTriangle size={16} />
              <span>SOS</span>
            </button>

            {/* DEDICATED PROMINENT PROFILE BUTTON */}
            <button
              type="button"
              className="header-profile-btn"
              onClick={() => openProfileWithTab('profile')}
              title="View & Edit Profile, Manage Caregivers & GPS Settings"
              aria-label="Open User Profile & Caregiver Hub"
            >
              <div className="header-profile-avatar">
                {displayName.slice(0, 2).toUpperCase()}
              </div>
              <div className="header-profile-info">
                <div className="header-profile-name">{displayName}</div>
                <div className="header-profile-role">{getRoleLabel(role)}</div>
              </div>
              <ChevronDown size={14} className="header-profile-chevron" />
            </button>

            {/* Sign Out or Login */}
            {isAuthenticated ? (
              <button
                className="icon-btn"
                onClick={logout}
                title={t('logout')}
                aria-label={t('logout')}
                style={{ minHeight: '38px', padding: '0 0.5rem', color: 'var(--text-subtle)' }}
              >
                <LogOut size={16} />
              </button>
            ) : (
              <button
                className="btn btn-primary"
                onClick={() => setIsLoginModalOpen(true)}
                style={{ minHeight: '38px', padding: '0 0.8rem', fontSize: '0.85rem' }}
              >
                <LogIn size={16} />
                <span>{t('login')}</span>
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Global Modals */}
      {isLoginModalOpen && <LoginModal onClose={() => setIsLoginModalOpen(false)} />}

      <AarogyaMitraVoiceCallModal
        isOpen={isVoiceModalOpen}
        onClose={() => setIsVoiceModalOpen(false)}
      />

      <UserProfileModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
        initialTab={profileInitialTab}
      />
    </>
  );
};

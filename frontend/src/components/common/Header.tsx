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

export const Header: React.FC = () => {
  const { user, role, isAuthenticated, logout, switchDemoRole } = useAuth();
  const { language, setLanguage, isHighContrast, toggleHighContrast, t } = useLanguage();
  const { setIsSosModalOpen } = useSos();
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isVoiceModalOpen, setIsVoiceModalOpen] = useState(false);

  const getRoleIcon = (r: UserRole) => {
    switch (r) {
      case 'PATIENT':
        return <User size={16} />;
      case 'ASHA':
        return <Users size={16} />;
      case 'DOCTOR':
        return <Stethoscope size={16} />;
      case 'ADMIN':
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
        return t('roleFacility');
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
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <AlertTriangle size={16} />
          <span>{t('demoModeNotice')}</span>
        </div>
        <div className="demo-buttons-group">
          {(['PATIENT', 'ASHA', 'DOCTOR', 'ADMIN'] as UserRole[]).map((r) => (
            <button
              key={r}
              className={`demo-btn ${role === r ? 'active' : ''}`}
              onClick={() => switchDemoRole(r)}
              title={`Switch to ${r} mode`}
            >
              {r}
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
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.4rem',
                    background: 'var(--bg-subtle)',
                    padding: '0.35rem 0.75rem',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-medium)',
                  }}
                >
                  {getRoleIcon(role)}
                  <div style={{ textAlign: 'left', lineHeight: 1.1 }}>
                    <div style={{ fontSize: '0.85rem', fontWeight: 700 }}>
                      {user?.displayName || user?.username}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--primary)' }}>
                      {getRoleLabel(role)}
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
    </>
  );
};

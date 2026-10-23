import React from 'react';
import { Activity, Heart, Pill, Video, AlertTriangle } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';
import { useSos } from '../../hooks/useSos';

export type PatientTab = 'overview' | 'screening' | 'records' | 'medicines' | 'consultations' | 'voiceCall';

interface MobileBottomNavProps {
  activeTab: PatientTab;
  onTabChange: (tab: PatientTab) => void;
}

export const MobileBottomNav: React.FC<MobileBottomNavProps> = ({ activeTab, onTabChange }) => {
  const { t } = useLanguage();
  const { setIsSosModalOpen } = useSos();

  return (
    <nav className="mobile-bottom-nav" aria-label="Mobile Navigation Bar">
      <button
        type="button"
        className={`mobile-nav-item ${activeTab === 'overview' ? 'active' : ''}`}
        onClick={() => onTabChange('overview')}
      >
        <div className="nav-icon-wrapper">
          <Activity size={22} />
        </div>
        <span className="nav-label">{t('navOverview')}</span>
      </button>

      <button
        type="button"
        className={`mobile-nav-item ${activeTab === 'screening' ? 'active' : ''}`}
        onClick={() => onTabChange('screening')}
      >
        <div className="nav-icon-wrapper">
          <Heart size={22} />
        </div>
        <span className="nav-label">{t('navCareFlow')}</span>
      </button>

      {/* Center Emergency SOS Button */}
      <button
        type="button"
        className="mobile-nav-item mobile-nav-sos"
        onClick={() => setIsSosModalOpen(true)}
        aria-label="Trigger Emergency SOS"
      >
        <div className="nav-sos-glow">
          <AlertTriangle size={24} />
        </div>
        <span className="nav-label">SOS 108</span>
      </button>

      <button
        type="button"
        className={`mobile-nav-item ${activeTab === 'medicines' ? 'active' : ''}`}
        onClick={() => onTabChange('medicines')}
      >
        <div className="nav-icon-wrapper">
          <Pill size={22} />
        </div>
        <span className="nav-label">{t('navMedicines')}</span>
      </button>

      <button
        type="button"
        className={`mobile-nav-item ${activeTab === 'consultations' ? 'active' : ''}`}
        onClick={() => onTabChange('consultations')}
      >
        <div className="nav-icon-wrapper">
          <Video size={22} />
        </div>
        <span className="nav-label">{t('navConsultations')}</span>
      </button>
    </nav>
  );
};

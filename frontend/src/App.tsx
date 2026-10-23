import React from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LanguageProvider, useLanguage } from './context/LanguageContext';
import { SosProvider, useSos } from './hooks/useSos';
import { ConsultationProvider } from './hooks/useConsultation';
import { Header } from './components/common/Header';
import { EmergencySosModal } from './components/common/EmergencySosModal';
import { JitsiCallModal } from './components/common/JitsiCallModal';
import { PatientWorkspace } from './components/patient/PatientWorkspace';
import { AshaWorkspace } from './components/asha/AshaWorkspace';
import { DoctorWorkspace } from './components/doctor/DoctorWorkspace';
import { FacilityDashboardPage } from './pages/FacilityDashboardPage';
import { ShieldCheck, Heart, AlertTriangle } from 'lucide-react';

const AppContent: React.FC = () => {
  const { role } = useAuth();
  const { t } = useLanguage();
  const { setIsSosModalOpen } = useSos();

  return (
    <div className="app-container">
      {/* Header with demo role switcher, language selector, high contrast, login */}
      <Header />

      {/* Main Workspace by Role */}
      <main className={`main-content ${role !== 'PATIENT' ? 'wide' : ''}`}>
        {(role === 'PATIENT' || role === 'CAREGIVER') && <PatientWorkspace />}
        {role === 'ASHA' && <AshaWorkspace />}
        {role === 'DOCTOR' && <DoctorWorkspace />}
        {(role === 'ADMIN' || role === 'FACILITY_ADMIN') && <FacilityDashboardPage />}
      </main>

      {/* Floating Emergency SOS Action Button */}
      <button
        className="emergency-floating-trigger"
        onClick={() => setIsSosModalOpen(true)}
        aria-label="Trigger Emergency SOS"
      >
        <AlertTriangle size={24} />
        <span>SOS 108 / 112</span>
      </button>

      {/* Global Modals */}
      <EmergencySosModal />
      <JitsiCallModal />

      {/* Accessible Footer */}
      <footer
        style={{
          borderTop: '2px solid var(--border-light)',
          backgroundColor: 'var(--bg-surface)',
          padding: '1.25rem 1rem',
          fontSize: '0.9rem',
          color: 'var(--text-subtle)',
          marginTop: 'auto',
        }}
      >
        <div
          style={{
            maxWidth: '1280px',
            margin: '0 auto',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '0.75rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <ShieldCheck size={18} color="var(--primary)" />
            <span>
              <strong>Aarogya Seva (आरोग्य सेवा)</strong> • Rural Healthcare Ecosystem
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <span>Dedicated to Rural Telemedicine & Care Navigation with</span>
            <Heart size={16} color="var(--risk-high)" fill="var(--risk-high)" />
          </div>
        </div>
      </footer>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <LanguageProvider>
      <AuthProvider>
        <SosProvider>
          <ConsultationProvider>
            <AppContent />
          </ConsultationProvider>
        </SosProvider>
      </AuthProvider>
    </LanguageProvider>
  );
};

export default App;

import React, { useState, useEffect, useRef } from 'react';
import {
  Search,
  X,
  Activity,
  Heart,
  Pill,
  Video,
  FileText,
  PhoneCall,
  AlertTriangle,
  User,
  Users,
  Building2,
  Stethoscope,
  Sparkles,
  ArrowRight,
  MapPin,
  HelpCircle,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useSos } from '../../hooks/useSos';
import { useLanguage } from '../../context/LanguageContext';

export interface SearchItem {
  id: string;
  title: string;
  subtitle: string;
  category: 'Nav' | 'Clinical' | 'Emergency' | 'Portal' | 'Symptom';
  icon: React.ReactNode;
  shortcut?: string;
  action: () => void;
}

interface GlobalSearchBarProps {
  onOpenProfile?: (initialTab?: 'profile' | 'caregivers' | 'location') => void;
  onOpenVoiceCall?: () => void;
}

export const GlobalSearchBar: React.FC<GlobalSearchBarProps> = ({
  onOpenProfile,
  onOpenVoiceCall,
}) => {
  const { role, switchDemoRole } = useAuth();
  const { setIsSosModalOpen } = useSos();
  const { t } = useLanguage();

  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  // Global hotkey: Ctrl+K or / opens the search palette
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setIsOpen((prev) => !prev);
      } else if (e.key === '/' && document.activeElement?.tagName !== 'INPUT' && document.activeElement?.tagName !== 'TEXTAREA') {
        e.preventDefault();
        setIsOpen(true);
      } else if (e.key === 'Escape' && isOpen) {
        setIsOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen]);

  useEffect(() => {
    if (isOpen) {
      setTimeout(() => inputRef.current?.focus(), 50);
      setSelectedIndex(0);
    } else {
      setQuery('');
    }
  }, [isOpen]);

  const dispatchNav = (tab: string) => {
    window.dispatchEvent(new CustomEvent('aarogya:navigate', { detail: { tab } }));
    setIsOpen(false);
  };

  const items: SearchItem[] = [
    // Core Patient Navigation
    {
      id: 'nav-overview',
      title: 'Health Overview & Vitals (आरोग्य आढावा)',
      subtitle: 'Daily vital parameters, health score, active prescriptions',
      category: 'Nav',
      icon: <Activity size={18} color="var(--primary)" />,
      action: () => dispatchNav('overview'),
    },
    {
      id: 'nav-medicines',
      title: 'My Medicines & Daily Doses (औषधे व वेळापत्रक)',
      subtitle: 'Prescriptions, medication adherence, dosage reminders',
      category: 'Nav',
      icon: <Pill size={18} color="#059669" />,
      action: () => dispatchNav('medicines'),
    },
    {
      id: 'nav-consultations',
      title: 'Doctor Video Teleconsultation (व्हिडिओ तपासणी)',
      subtitle: 'Join live video call with MBBS/MD doctor, view past appointments',
      category: 'Nav',
      icon: <Video size={18} color="#0284c7" />,
      action: () => dispatchNav('consultations'),
    },
    {
      id: 'nav-screening',
      title: 'Care Flow & AI Symptom Screening (लक्षण तपासणी)',
      subtitle: 'AI-assisted medical triage, urgency assessment & referral wizard',
      category: 'Nav',
      icon: <Heart size={18} color="#e11d48" />,
      action: () => dispatchNav('screening'),
    },
    {
      id: 'nav-records',
      title: 'Medical History & Records (नोंदवही / अहवाल)',
      subtitle: 'View historical doctor cases, diagnoses, prescriptions & lab reports',
      category: 'Nav',
      icon: <FileText size={18} color="#7c3aed" />,
      action: () => dispatchNav('records'),
    },
    {
      id: 'nav-voice',
      title: 'Aarogya Mitra Voice AI Call (आरोग्य मित्र कॉल)',
      subtitle: 'Dial Indian-language voice AI assistant for medical queries',
      category: 'Nav',
      icon: <PhoneCall size={18} color="#0d9488" />,
      action: () => {
        setIsOpen(false);
        if (onOpenVoiceCall) onOpenVoiceCall();
      },
    },

    // Profile & Caregivers
    {
      id: 'user-profile',
      title: 'My Profile Details (वैयक्तिक माहिती)',
      subtitle: 'View & edit full name, phone number, address, blood group',
      category: 'Clinical',
      icon: <User size={18} color="var(--primary)" />,
      action: () => {
        setIsOpen(false);
        if (onOpenProfile) onOpenProfile('profile');
      },
    },
    {
      id: 'user-caregivers',
      title: 'My Caregivers & Guardians (माझे काळजीवाहक)',
      subtitle: 'View assigned caregivers, add new guardians, configure SMS/WhatsApp alerts',
      category: 'Clinical',
      icon: <Users size={18} color="#d97706" />,
      action: () => {
        setIsOpen(false);
        if (onOpenProfile) onOpenProfile('caregivers');
      },
    },
    {
      id: 'user-location',
      title: 'Live GPS Location & 5 km SOS Responder',
      subtitle: 'Detect live coordinates and toggle community SOS emergency responder',
      category: 'Clinical',
      icon: <MapPin size={18} color="#2563eb" />,
      action: () => {
        setIsOpen(false);
        if (onOpenProfile) onOpenProfile('location');
      },
    },

    // Emergency Action
    {
      id: 'sos-108',
      title: 'EMERGENCY SOS 108 / 112 (आपत्कालीन मदत)',
      subtitle: 'Instant ambulance dispatch, broadcast live coordinates to nearby doctors & ASHA',
      category: 'Emergency',
      icon: <AlertTriangle size={18} color="#b91c1c" />,
      action: () => {
        setIsOpen(false);
        setIsSosModalOpen(true);
      },
    },

    // Portals
    {
      id: 'portal-doctor',
      title: 'Switch to Doctor OPD Workspace (डॉक्टर पोर्टल)',
      subtitle: 'Clinician triage queue, accept consultations, prescribe medicines',
      category: 'Portal',
      icon: <Stethoscope size={18} color="#0284c7" />,
      action: () => {
        setIsOpen(false);
        switchDemoRole('DOCTOR');
      },
    },
    {
      id: 'portal-asha',
      title: 'Switch to ASHA Community Portal (आशा ताई पोर्टल)',
      subtitle: 'Field worker patient tracking, household screening, local SOS response',
      category: 'Portal',
      icon: <Users size={18} color="#db2777" />,
      action: () => {
        setIsOpen(false);
        switchDemoRole('ASHA');
      },
    },
    {
      id: 'portal-facility',
      title: 'Switch to Facility Admin Portal (रुग्णालय व्यवस्थापन)',
      subtitle: 'Sub-district hospital bed availability, ICU triage, incoming referrals',
      category: 'Portal',
      icon: <Building2 size={18} color="#4b5563" />,
      action: () => {
        setIsOpen(false);
        switchDemoRole('FACILITY_ADMIN');
      },
    },

    // Symptom shortcuts
    {
      id: 'sym-fever',
      title: 'High Fever & Chills (ताप आणि थंडी)',
      subtitle: 'Start symptom triage for acute fever and malaria/dengue screening',
      category: 'Symptom',
      icon: <Sparkles size={18} color="#ea580c" />,
      action: () => {
        dispatchNav('screening');
      },
    },
    {
      id: 'sym-cough',
      title: 'Persistent Cough & Breathlessness (खोकला व श्वास लागणे)',
      subtitle: 'Start respiratory evaluation and oxygen saturation check',
      category: 'Symptom',
      icon: <Sparkles size={18} color="#0891b2" />,
      action: () => {
        dispatchNav('screening');
      },
    },
    {
      id: 'sym-chest',
      title: 'Chest Pain or Discomfort (छातीत दुखणे)',
      subtitle: 'Immediate cardiac triage, ECG recommendation & emergency consultation',
      category: 'Symptom',
      icon: <AlertTriangle size={18} color="#dc2626" />,
      action: () => {
        dispatchNav('screening');
      },
    },
  ];

  const filteredItems = items.filter((item) => {
    if (!query.trim()) return true;
    const q = query.toLowerCase();
    return (
      item.title.toLowerCase().includes(q) ||
      item.subtitle.toLowerCase().includes(q) ||
      item.category.toLowerCase().includes(q)
    );
  });

  const handleSelect = (item: SearchItem) => {
    item.action();
  };

  const handleKeyDownInput = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev < filteredItems.length - 1 ? prev + 1 : 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev > 0 ? prev - 1 : filteredItems.length - 1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (filteredItems[selectedIndex]) {
        handleSelect(filteredItems[selectedIndex]);
      }
    }
  };

  return (
    <>
      {/* Top Header Trigger Button */}
      <div className="global-search-trigger-wrap">
        <button
          type="button"
          className="global-search-trigger"
          onClick={() => setIsOpen(true)}
          title="Search sections, medicines, doctors, SOS, records... (Press / or Ctrl+K)"
          aria-label="Open App Search Bar"
        >
          <Search size={18} className="search-icon-muted" />
          <span className="search-placeholder-text">
            Search sections, doctors, medicines, SOS, caregivers...
          </span>
          <span className="search-hotkey-badge">
            <kbd>Ctrl</kbd> <kbd>K</kbd>
          </span>
        </button>
      </div>

      {/* Command Palette Overlay Modal */}
      {isOpen && (
        <div
          className="cmd-palette-backdrop"
          onClick={() => setIsOpen(false)}
          role="dialog"
          aria-modal="true"
        >
          <div
            className="cmd-palette-modal"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Search Input Bar */}
            <div className="cmd-input-container">
              <Search size={22} className="cmd-search-icon" />
              <input
                ref={inputRef}
                type="text"
                className="cmd-search-input"
                placeholder="Type where you want to go or search symptoms..."
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setSelectedIndex(0);
                }}
                onKeyDown={handleKeyDownInput}
              />
              {query ? (
                <button
                  type="button"
                  className="cmd-clear-btn"
                  onClick={() => {
                    setQuery('');
                    inputRef.current?.focus();
                  }}
                >
                  <X size={18} />
                </button>
              ) : (
                <span className="cmd-esc-badge">ESC to close</span>
              )}
            </div>

            {/* Quick Action Category Pills */}
            <div className="cmd-chips-row">
              <button
                type="button"
                className="cmd-chip"
                onClick={() => setQuery('medicines')}
              >
                💊 Medicines
              </button>
              <button
                type="button"
                className="cmd-chip"
                onClick={() => setQuery('doctor')}
              >
                📹 Video Doctor
              </button>
              <button
                type="button"
                className="cmd-chip"
                onClick={() => setQuery('caregiver')}
              >
                👥 Caregivers
              </button>
              <button
                type="button"
                className="cmd-chip"
                onClick={() => setQuery('records')}
              >
                📋 Records
              </button>
              <button
                type="button"
                className="cmd-chip emergency"
                onClick={() => setQuery('sos')}
              >
                🚨 SOS 108
              </button>
            </div>

            {/* Results List */}
            <div className="cmd-results-list">
              {filteredItems.length === 0 ? (
                <div className="cmd-empty-state">
                  <HelpCircle size={32} color="var(--text-subtle)" />
                  <p style={{ fontWeight: 600, marginTop: '0.5rem' }}>No sections found for "{query}"</p>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
                    Try searching for "medicines", "video doctor", "caregiver", or "fever"
                  </span>
                </div>
              ) : (
                filteredItems.map((item, idx) => {
                  const isSelected = idx === selectedIndex;
                  return (
                    <div
                      key={item.id}
                      className={`cmd-result-item ${isSelected ? 'selected' : ''}`}
                      onClick={() => handleSelect(item)}
                      onMouseEnter={() => setSelectedIndex(idx)}
                    >
                      <div className="cmd-item-icon-wrap">{item.icon}</div>
                      <div className="cmd-item-info">
                        <div className="cmd-item-title-row">
                          <span className="cmd-item-title">{item.title}</span>
                          <span className={`cmd-item-category ${item.category.toLowerCase()}`}>
                            {item.category}
                          </span>
                        </div>
                        <span className="cmd-item-subtitle">{item.subtitle}</span>
                      </div>
                      <ArrowRight size={16} className="cmd-item-arrow" />
                    </div>
                  );
                })
              )}
            </div>

            {/* Footer Navigation Tip */}
            <div className="cmd-palette-footer">
              <div className="cmd-footer-shortcuts">
                <span><kbd>↑</kbd> <kbd>↓</kbd> to navigate</span>
                <span><kbd>↵</kbd> to select</span>
                <span><kbd>esc</kbd> to dismiss</span>
              </div>
              <span className="cmd-footer-tag">Aarogya Seva Intelligence</span>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

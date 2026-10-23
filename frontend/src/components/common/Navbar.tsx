import React from 'react';
import { Stethoscope, Building2, ClipboardList, Activity, MapPin } from 'lucide-react';

interface NavbarProps {
  currentTab: 'patient' | 'facility' | 'facilities' | 'cases';
  onSelectTab: (tab: 'patient' | 'facility' | 'facilities' | 'cases') => void;
}

export const Navbar: React.FC<NavbarProps> = ({ currentTab, onSelectTab }) => {
  return (
    <header className="navbar">
      <div className="navbar-inner">
        <a href="#home" onClick={(e) => { e.preventDefault(); onSelectTab('patient'); }} className="navbar-brand">
          <div className="brand-icon">
            <Stethoscope size={22} />
          </div>
          <div>
            <div className="brand-title">Aarogya Seva <span style={{ fontSize: '0.8rem', color: 'var(--primary)', fontWeight: 600 }}>आरोग्य सेवा</span></div>
            <div className="brand-subtitle">Rural Healthcare Intelligence & Facility Referral</div>
          </div>
        </a>

        <nav className="nav-links">
          <button
            onClick={() => onSelectTab('patient')}
            className={`nav-link ${currentTab === 'patient' ? 'active' : ''}`}
          >
            <Activity size={18} />
            <span>Care Navigation</span>
          </button>

          <button
            onClick={() => onSelectTab('facility')}
            className={`nav-link ${currentTab === 'facility' ? 'active' : ''}`}
          >
            <Building2 size={18} />
            <span>Facility Portal</span>
          </button>

          <button
            onClick={() => onSelectTab('facilities')}
            className={`nav-link ${currentTab === 'facilities' ? 'active' : ''}`}
          >
            <MapPin size={18} />
            <span>Facilities Directory</span>
          </button>

          <button
            onClick={() => onSelectTab('cases')}
            className={`nav-link ${currentTab === 'cases' ? 'active' : ''}`}
          >
            <ClipboardList size={18} />
            <span>All Cases</span>
          </button>
        </nav>
      </div>
    </header>
  );
};

import React from 'react';
import { Stethoscope, ClipboardList, ShieldCheck, MapPin, Heart, ArrowRight, UserCheck, ShieldAlert } from 'lucide-react';

interface PatientHomeProps {
  onStartScreening: () => void;
  onViewRequests: () => void;
  onViewDoctors: () => void;
  onTriggerSos: () => void;
}

export const PatientHome: React.FC<PatientHomeProps> = ({
  onStartScreening,
  onViewRequests,
  onViewDoctors,
  onTriggerSos,
}) => {
  return (
    <div style={{ maxWidth: '680px', margin: '0 auto', textAlign: 'center' }}>
      {/* Welcome Banner */}
      <div
        className="card-simple"
        style={{
          padding: '2.5rem 1.5rem',
          backgroundColor: 'var(--bg-surface)',
          border: '2px solid var(--primary-border)',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-md)',
          marginBottom: '2rem',
        }}
      >
        <div
          style={{
            width: '80px',
            height: '80px',
            borderRadius: '50%',
            backgroundColor: 'var(--primary-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 1.25rem auto',
          }}
        >
          <Heart size={44} color="var(--primary)" strokeWidth={2.5} />
        </div>

        <h1 style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '0.5rem' }}>
          Aarogya Seva • आरोग्य सेवा
        </h1>
        <p style={{ fontSize: '1.25rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.75rem' }}>
          Tell us what health problem you have. We will help you understand the urgency and find nearby healthcare facilities.
        </p>
        <p style={{ fontSize: '1rem', color: 'var(--text-subtle)' }}>
          आपल्याला काय त्रास होत आहे ते सांगा. आम्ही आपल्याला योग्य दवाखाना आणि सल्ला मिळवून देऊ.
        </p>

        {/* Dedicated Emergency SOS Section */}
        <div
          style={{
            marginTop: '2rem',
            padding: '1.25rem',
            backgroundColor: '#FEF2F2',
            border: '2px solid #FCA5A5',
            borderRadius: 'var(--radius-lg)',
            boxShadow: '0 4px 12px rgba(220, 38, 38, 0.15)',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              color: '#B91C1C',
              fontWeight: 800,
              fontSize: '1.15rem',
              marginBottom: '0.4rem',
            }}
          >
            <ShieldAlert size={26} color="#DC2626" />
            <span>CRITICAL MEDICAL EMERGENCY? • तात्काळ आणीबाणी आहे?</span>
          </div>

          <p
            style={{
              margin: '0 0 1rem 0',
              fontSize: '0.95rem',
              color: '#7F1D1D',
              fontWeight: 500,
            }}
          >
            Instantly alert your caregiver, nearby hospital, and 5 KM emergency helpers with your current location.
            <br />
            <span style={{ fontSize: '0.85rem', color: '#991B1B' }}>
              आपल्या स्थानासह कुटुंब, रुग्णालय आणि ५ किमी परिसरातील मदतनीसांना तात्काळ इशारा पाठवा.
            </span>
          </p>

          <button
            onClick={onTriggerSos}
            style={{
              width: '100%',
              padding: '1.2rem 1.5rem',
              fontSize: '1.35rem',
              fontWeight: 900,
              letterSpacing: '0.5px',
              backgroundColor: '#DC2626',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: 'var(--radius-md)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.75rem',
              boxShadow: '0 8px 20px rgba(220, 38, 38, 0.35)',
              transition: 'transform 0.15s ease',
            }}
            aria-label="Trigger Emergency SOS"
          >
            <ShieldAlert size={30} />
            <span>TRIGGER EMERGENCY SOS • आणीबाणी इशारा</span>
          </button>
        </div>

        {/* Primary CTA */}
        <div style={{ marginTop: '2rem' }}>
          <button
            className="btn-large btn-primary"
            style={{
              padding: '1.2rem 2rem',
              fontSize: '1.35rem',
              borderRadius: 'var(--radius-lg)',
              boxShadow: 'var(--shadow-md)',
            }}
            onClick={onStartScreening}
            aria-label="Start Health Screening"
          >
            <Stethoscope size={28} />
            <span>START HEALTH SCREENING • तपासणी सुरू करा</span>
            <ArrowRight size={24} />
          </button>
        </div>

        {/* Secondary CTA Row */}
        <div
          style={{
            marginTop: '1.25rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.75rem',
          }}
        >
          <button
            className="btn-large btn-outline"
            style={{ fontSize: '1.15rem', borderColor: 'var(--primary)', color: 'var(--primary)' }}
            onClick={onViewDoctors}
            aria-label="Find Available Doctors"
          >
            <UserCheck size={22} />
            <span>FIND AVAILABLE DOCTORS • उपलब्ध डॉक्टर पहा</span>
          </button>

          <button
            className="btn-large btn-outline"
            style={{ fontSize: '1.15rem' }}
            onClick={onViewRequests}
            aria-label="Track My Request"
          >
            <ClipboardList size={22} />
            <span>TRACK MY REQUEST • माझी विनंती तपासा</span>
          </button>
        </div>
      </div>

      {/* 3 Simple Reassuring Steps */}
      <div className="grid-3" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', textAlign: 'left' }}>
        <div className="card-simple" style={{ padding: '1.25rem', marginBottom: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <span style={{ background: 'var(--primary-light)', color: 'var(--primary)', fontWeight: 800, width: '28px', height: '28px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>1</span>
            <strong style={{ fontSize: '1.05rem' }}>Tell Problem</strong>
          </div>
          <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)' }}>
            Simple questions about your fever, cough, or pain in plain language.
          </p>
        </div>

        <div className="card-simple" style={{ padding: '1.25rem', marginBottom: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <span style={{ background: 'var(--secondary-light)', color: 'var(--secondary)', fontWeight: 800, width: '28px', height: '28px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>2</span>
            <strong style={{ fontSize: '1.05rem' }}>Get Screening</strong>
          </div>
          <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)' }}>
            Understand urgency level and precautions you should take right away.
          </p>
        </div>

        <div className="card-simple" style={{ padding: '1.25rem', marginBottom: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <span style={{ background: 'var(--accent-warm-light)', color: 'var(--accent-warm)', fontWeight: 800, width: '28px', height: '28px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>3</span>
            <strong style={{ fontSize: '1.05rem' }}>Find Healthcare</strong>
          </div>
          <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)' }}>
            Discover nearest PHCs & hospitals and send a care request directly.
          </p>
        </div>
      </div>
    </div>
  );
};

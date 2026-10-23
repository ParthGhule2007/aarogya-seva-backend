import React from 'react';
import { Modal } from '../common/Modal';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { ReferralDetailDto } from '../../types/referral';
import {
  User,
  Clipboard,
  Activity,
  Sparkles,
  Building2,
  Calendar,
  Phone,
  Heart,
  Thermometer,
  ShieldAlert,
  AlertTriangle,
  CheckCircle2,
  Stethoscope,
  Info,
  Clock,
  MapPin,
  MessageSquare
} from 'lucide-react';
import { VitalsGridDisplay } from '../common/medical/VitalCard';
import { RiskGauge } from '../common/medical/RiskGauge';
import { ReferralTimeline } from '../common/medical/ReferralTimeline';
import { SymptomChip } from '../common/medical/SymptomChip';

interface ReferralDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  detail: ReferralDetailDto | null;
}

export const ReferralDetailModal: React.FC<ReferralDetailModalProps> = ({
  isOpen,
  onClose,
  detail,
}) => {
  if (!detail) return null;

  const { referral, patient, healthcareCase, symptoms, aiAssessment, facility } = detail;
  const referralCode = referral.referralToken || `REF-${referral.id.substring(0, 8).toUpperCase()}`;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Clinical Evaluation Context • रुग्ण संदर्भ तपशील"
      subtitle={`${referralCode} • Patient Referral & AI Screening Dossier`}
      maxWidth="840px"
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {/* Top Status & Date Banner */}
        <div
          style={{
            padding: '1rem 1.25rem',
            backgroundColor: 'var(--bg-subtle)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--border-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '0.75rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', fontWeight: 700, textTransform: 'uppercase' }}>
                Referral Status
              </div>
              <div style={{ marginTop: '0.25rem' }}>
                <Badge variant="status" value={referral.status} />
              </div>
            </div>

            {referral.priority && (
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', fontWeight: 700, textTransform: 'uppercase' }}>
                  Priority
                </div>
                <div style={{ marginTop: '0.25rem' }}>
                  <span
                    style={{
                      display: 'inline-block',
                      padding: '0.2rem 0.6rem',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: '0.8rem',
                      fontWeight: 700,
                      backgroundColor: referral.priority === 'CRITICAL' ? '#fee2e2' : '#fef3c7',
                      color: referral.priority === 'CRITICAL' ? '#991b1b' : '#92400e',
                    }}
                  >
                    {referral.priority}
                  </span>
                </div>
              </div>
            )}
          </div>

          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', fontWeight: 700, textTransform: 'uppercase' }}>
              Referral Requested At
            </div>
            <div style={{ fontSize: '0.88rem', fontWeight: 600, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.35rem', marginTop: '0.25rem' }}>
              <Calendar size={14} color="var(--primary)" />
              <span>{referral.requestedAt ? new Date(referral.requestedAt).toLocaleString() : 'Recently'}</span>
            </div>
          </div>
        </div>

        {/* Visual Referral Progression Timeline */}
        <ReferralTimeline
          status={referral.status}
          requestedAt={referral.requestedAt}
          respondedAt={referral.respondedAt}
          acceptedAt={referral.acceptedAt}
          arrivedAt={referral.arrivedAt}
          completedAt={referral.completedAt}
        />

        {/* Patient Referral Reason & Facility Response */}
        {referral.patientMessage && (
          <div
            style={{
              padding: '1rem 1.25rem',
              backgroundColor: '#f8fafc',
              border: '1.5px solid #cbd5e1',
              borderRadius: 'var(--radius-md)',
              borderLeft: '5px solid var(--primary)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.4rem' }}>
              <MessageSquare size={16} color="var(--primary)" />
              <span style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--primary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Patient Referral Reason & Intake Notes
              </span>
            </div>
            <p style={{ fontSize: '0.95rem', color: 'var(--text-main)', lineHeight: 1.5, margin: 0 }}>
              "{referral.patientMessage}"
            </p>
          </div>
        )}

        {referral.facilityResponse && (
          <div
            style={{
              padding: '1rem 1.25rem',
              backgroundColor: referral.status === 'REJECTED' ? '#fff1f2' : '#ecfdf5',
              border: `1.5px solid ${referral.status === 'REJECTED' ? '#fecdd3' : '#a7f3d0'}`,
              borderRadius: 'var(--radius-md)',
              borderLeft: `5px solid ${referral.status === 'REJECTED' ? '#e11d48' : '#059669'}`,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.4rem' }}>
              {referral.status === 'REJECTED' ? (
                <AlertTriangle size={16} color="#e11d48" />
              ) : (
                <CheckCircle2 size={16} color="#059669" />
              )}
              <span
                style={{
                  fontSize: '0.8rem',
                  fontWeight: 800,
                  color: referral.status === 'REJECTED' ? '#9f1239' : '#065f46',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                }}
              >
                Recorded Facility Response
              </span>
            </div>
            <p style={{ fontSize: '0.92rem', color: 'var(--text-main)', lineHeight: 1.5, margin: 0 }}>
              "{referral.facilityResponse}"
            </p>
          </div>
        )}

        {/* Section 1: Patient Demographics */}
        {patient && (
          <div
            style={{
              padding: '1.25rem',
              backgroundColor: 'var(--bg-surface)',
              border: '1px solid var(--border-light)',
              borderRadius: 'var(--radius-md)',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', borderBottom: '1px solid var(--border-light)', paddingBottom: '0.5rem' }}>
              <User size={18} color="var(--primary)" />
              <h4 style={{ fontSize: '1.05rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
                Patient Demographics • रुग्ण तपशील
              </h4>
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '1rem',
                fontSize: '0.9rem',
              }}
            >
              <div>
                <span style={{ color: 'var(--text-subtle)', fontSize: '0.8rem', display: 'block' }}>Full Name</span>
                <strong style={{ fontSize: '1.05rem', color: 'var(--text-main)' }}>{patient.fullName}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-subtle)', fontSize: '0.8rem', display: 'block' }}>Age & Gender</span>
                <strong>{patient.age} yrs • {patient.gender}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-subtle)', fontSize: '0.8rem', display: 'block' }}>Blood Group</span>
                <strong>{patient.bloodGroup || 'Not Recorded'}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-subtle)', fontSize: '0.8rem', display: 'block' }}>Contact Phone</span>
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontWeight: 600 }}>
                  <Phone size={13} color="var(--primary)" />
                  {patient.contactNumber}
                </span>
              </div>
              <div>
                <span style={{ color: 'var(--text-subtle)', fontSize: '0.8rem', display: 'block' }}>Village / Town</span>
                <strong>{patient.villageTown}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-subtle)', fontSize: '0.8rem', display: 'block' }}>District & State</span>
                <strong>{patient.district}, {patient.state}</strong>
              </div>
            </div>
          </div>
        )}

        {/* Section 2: Consultation Case Details */}
        {healthcareCase && (
          <div
            style={{
              padding: '1.25rem',
              backgroundColor: 'var(--bg-surface)',
              border: '1px solid var(--border-light)',
              borderRadius: 'var(--radius-md)',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', borderBottom: '1px solid var(--border-light)', paddingBottom: '0.5rem' }}>
              <Clipboard size={18} color="var(--secondary)" />
              <h4 style={{ fontSize: '1.05rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
                Consultation Case Context: {healthcareCase.caseNumber}
              </h4>
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                gap: '0.85rem',
                fontSize: '0.88rem',
                marginBottom: '0.85rem',
              }}
            >
              <div>
                <span style={{ color: 'var(--text-subtle)', fontSize: '0.78rem', display: 'block' }}>Case Status</span>
                <span style={{ fontWeight: 700, color: 'var(--primary-dark)' }}>{healthcareCase.status}</span>
              </div>
              <div>
                <span style={{ color: 'var(--text-subtle)', fontSize: '0.78rem', display: 'block' }}>Priority</span>
                <span style={{ fontWeight: 700 }}>{healthcareCase.priority}</span>
              </div>
              <div>
                <span style={{ color: 'var(--text-subtle)', fontSize: '0.78rem', display: 'block' }}>Intake Modality</span>
                <span style={{ fontWeight: 600 }}>{healthcareCase.intakeChannel}</span>
              </div>
            </div>

            {healthcareCase.chiefComplaint && (
              <div style={{ marginTop: '0.5rem', padding: '0.65rem 0.85rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)' }}>
                <strong style={{ fontSize: '0.85rem', color: 'var(--text-main)' }}>Chief Complaint:</strong>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', margin: '0.2rem 0 0 0' }}>
                  {healthcareCase.chiefComplaint}
                </p>
              </div>
            )}

            {healthcareCase.notes && (
              <div style={{ marginTop: '0.5rem', padding: '0.65rem 0.85rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)' }}>
                <strong style={{ fontSize: '0.85rem', color: 'var(--text-main)' }}>Consultation Notes:</strong>
                <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', margin: '0.2rem 0 0 0' }}>
                  {healthcareCase.notes}
                </p>
              </div>
            )}

            {/* Recorded Vitals Grid */}
            {healthcareCase.vitals && (
              <div style={{ marginTop: '1rem' }}>
                <div style={{ fontWeight: 800, fontSize: '0.82rem', color: 'var(--primary)', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.35rem', textTransform: 'uppercase' }}>
                  <Heart size={14} color="var(--primary)" />
                  <span>Recorded Clinical Vitals (शारीरिक मापदंड)</span>
                </div>
                <VitalsGridDisplay vitals={healthcareCase.vitals} />
              </div>
            )}
          </div>
        )}

        {/* Section 3: Recorded Symptoms */}
        {symptoms && symptoms.length > 0 && (
          <div
            style={{
              padding: '1.25rem',
              backgroundColor: 'var(--bg-surface)',
              border: '1px solid var(--border-light)',
              borderRadius: 'var(--radius-md)',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.85rem', borderBottom: '1px solid var(--border-light)', paddingBottom: '0.5rem' }}>
              <Activity size={18} color="var(--accent-warm)" />
              <h4 style={{ fontSize: '1.05rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
                Reported Patient Symptoms ({symptoms.length}) • नोंदवलेली लक्षणे
              </h4>
            </div>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.55rem' }}>
              {symptoms.map((s, idx) => (
                <SymptomChip
                  key={idx}
                  name={s.symptomName}
                  bilingualLabel={s.bodySite ? `Site: ${s.bodySite}` : s.description}
                  severity={s.severity}
                  durationDays={s.durationDays}
                />
              ))}
            </div>
          </div>
        )}

        {/* Section 4: AI Clinical Screening Intelligence */}
        {aiAssessment && (
          <div
            style={{
              padding: '1.25rem',
              backgroundColor: '#faf5ff',
              border: '1.5px solid #d8b4fe',
              borderRadius: 'var(--radius-md)',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '0.85rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Sparkles size={20} color="#9333ea" />
                <h4 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#581c87', margin: 0 }}>
                  AI Clinical Screening Summary • क्लिनिकल AI विश्लेषण
                </h4>
              </div>
              <span style={{ fontSize: '0.75rem', color: '#7e22ce', fontWeight: 600 }}>
                Model: {aiAssessment.modelVersion || 'Clinical-v2.0'}
              </span>
            </div>

            <div style={{ marginBottom: '1rem' }}>
              <RiskGauge
                riskScore={aiAssessment.riskScore}
                riskLevel={aiAssessment.riskLevel}
                urgencyLevel={aiAssessment.urgencyLevel}
                confidence={aiAssessment.confidence}
                compact
              />
            </div>

            {aiAssessment.urgencyReason && (
              <div style={{ marginBottom: '0.85rem', padding: '0.75rem 1rem', backgroundColor: '#ffffff', borderRadius: 'var(--radius-sm)', border: '1px solid #e9d5ff' }}>
                <strong style={{ fontSize: '0.85rem', color: '#581c87', display: 'block', marginBottom: '0.2rem' }}>
                  Urgency Rationale:
                </strong>
                <p style={{ fontSize: '0.9rem', color: '#3b0764', margin: 0, lineHeight: 1.5 }}>
                  {aiAssessment.urgencyReason}
                </p>
              </div>
            )}

            {aiAssessment.explanation && (
              <div style={{ marginBottom: '0.85rem', padding: '0.75rem 1rem', backgroundColor: '#ffffff', borderRadius: 'var(--radius-sm)', border: '1px solid #e9d5ff' }}>
                <strong style={{ fontSize: '0.85rem', color: '#581c87', display: 'block', marginBottom: '0.2rem' }}>
                  Clinical Insights:
                </strong>
                <p style={{ fontSize: '0.88rem', color: '#3b0764', margin: 0, lineHeight: 1.5 }}>
                  {aiAssessment.explanation}
                </p>
              </div>
            )}

            {/* Probable Conditions with visual probability bars */}
            {aiAssessment.possibleDiseases && aiAssessment.possibleDiseases.length > 0 && (
              <div>
                <strong style={{ fontSize: '0.85rem', color: '#581c87', display: 'block', marginBottom: '0.4rem' }}>
                  Probable Differential Conditions:
                </strong>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {aiAssessment.possibleDiseases.map((d, idx) => {
                    const pct = Math.round((d.probability || 0) * 100);
                    return (
                      <div
                        key={idx}
                        style={{
                          padding: '0.6rem 0.85rem',
                          backgroundColor: '#ffffff',
                          borderRadius: 'var(--radius-sm)',
                          border: '1px solid #e9d5ff',
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.3rem' }}>
                          <span style={{ fontWeight: 700, fontSize: '0.9rem', color: '#3b0764' }}>{d.category}</span>
                          <span style={{ fontWeight: 800, fontSize: '0.85rem', color: '#7e22ce' }}>{pct}% probability</span>
                        </div>
                        <div style={{ width: '100%', height: '6px', backgroundColor: '#f3e8ff', borderRadius: '4px', overflow: 'hidden' }}>
                          <div
                            style={{
                              width: `${pct}%`,
                              height: '100%',
                              backgroundColor: pct > 60 ? '#9333ea' : '#c084fc',
                              borderRadius: '4px',
                            }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Section 5: Recommended Services & Precautions */}
        {aiAssessment && ((aiAssessment.precautions && aiAssessment.precautions.length > 0) || (aiAssessment.recommendedServices && aiAssessment.recommendedServices.length > 0)) && (
          <div
            style={{
              padding: '1.25rem',
              backgroundColor: 'var(--bg-surface)',
              border: '1px solid var(--border-light)',
              borderRadius: 'var(--radius-md)',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.85rem', borderBottom: '1px solid var(--border-light)', paddingBottom: '0.5rem' }}>
              <ShieldAlert size={18} color="var(--primary)" />
              <h4 style={{ fontSize: '1.05rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
                Recommended Services & Precautions • आवश्यक सेवा व दक्षता
              </h4>
            </div>

            {/* Recommended Services Chips */}
            {aiAssessment.recommendedServices && aiAssessment.recommendedServices.length > 0 && (
              <div style={{ marginBottom: '1rem' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', fontWeight: 700, display: 'block', marginBottom: '0.4rem', textTransform: 'uppercase' }}>
                  Recommended Clinical Services
                </span>
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                  {aiAssessment.recommendedServices.map((srv, idx) => (
                    <span
                      key={idx}
                      style={{
                        padding: '0.35rem 0.75rem',
                        borderRadius: 'var(--radius-full)',
                        backgroundColor: '#f0fdfa',
                        border: '1px solid #99f6e4',
                        color: '#0f766e',
                        fontSize: '0.82rem',
                        fontWeight: 700,
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.3rem',
                      }}
                    >
                      <Stethoscope size={13} />
                      {String(srv).replace(/_/g, ' ')}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Clinical Precautions */}
            {aiAssessment.precautions && aiAssessment.precautions.length > 0 && (
              <div>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', fontWeight: 700, display: 'block', marginBottom: '0.4rem', textTransform: 'uppercase' }}>
                  Care Precautions & Warning Points
                </span>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
                  {aiAssessment.precautions.map((p, idx) => (
                    <div
                      key={idx}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '0.5rem',
                        fontSize: '0.88rem',
                        color: 'var(--text-main)',
                      }}
                    >
                      <AlertTriangle size={15} color="var(--accent-warm)" style={{ flexShrink: 0, marginTop: '3px' }} />
                      <span>{p}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Section 6: Destination Facility Details */}
        {facility && (
          <div
            style={{
              padding: '1rem 1.25rem',
              backgroundColor: 'var(--bg-subtle)',
              border: '1px solid var(--border-light)',
              borderRadius: 'var(--radius-md)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '0.75rem',
              fontSize: '0.88rem',
            }}
          >
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 800, color: 'var(--text-main)' }}>
                <Building2 size={16} color="var(--primary)" />
                <span>{facility.name}</span>
                <span style={{ fontSize: '0.78rem', color: 'var(--text-subtle)', fontWeight: 500 }}>
                  ({facility.facilityTypeDisplayName || facility.facilityType})
                </span>
              </div>
              <div style={{ color: 'var(--text-muted)', fontSize: '0.82rem', marginTop: '0.2rem' }}>
                {facility.addressLine1}, {facility.villageTown}, {facility.district} - {facility.pincode}
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-main)', fontWeight: 600 }}>
              <Phone size={14} color="var(--primary)" />
              <span>{facility.contactNumber}</span>
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          alignItems: 'center',
          gap: '0.75rem',
          paddingTop: '1.25rem',
          borderTop: '1px solid var(--border-light)',
          marginTop: '1.25rem',
        }}
      >
        <Button variant="neutral" onClick={onClose}>
          Close (बंद करा)
        </Button>
      </div>
    </Modal>
  );
};

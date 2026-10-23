import React from 'react';
import { Modal } from '../common/Modal';
import { Badge } from '../common/Badge';
import { ReferralDetailDto } from '../../types/referral';
import { User, Clipboard, Activity, Sparkles, Building2, Calendar, Phone } from 'lucide-react';

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

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Referral Request Evaluation Context"
      subtitle={`Referral ID: ${referral.id}`}
      maxWidth="780px"
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {/* Status Header */}
        <div
          style={{
            padding: '1rem',
            backgroundColor: 'var(--bg-surface-subtle)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--border-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '0.75rem',
          }}
        >
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', fontWeight: 600 }}>CURRENT REFERRAL STATUS</div>
            <div style={{ marginTop: '0.2rem' }}>
              <Badge variant="status" value={referral.status} />
            </div>
          </div>

          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', fontWeight: 600 }}>REFERRAL REQUESTED AT</div>
            <div style={{ fontSize: '0.9rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
              <Calendar size={14} />
              <span>{new Date(referral.requestedAt).toLocaleString()}</span>
            </div>
          </div>
        </div>

        {/* Patient Message & Facility Response */}
        {(referral.patientMessage || referral.facilityResponse) && (
          <div className="grid-2">
            {referral.patientMessage && (
              <div style={{ padding: '0.85rem', backgroundColor: 'var(--bg-surface)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-md)' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase' }}>
                  Patient Referral Note
                </span>
                <p style={{ fontSize: '0.9rem', marginTop: '0.3rem' }}>{referral.patientMessage}</p>
              </div>
            )}
            {referral.facilityResponse && (
              <div style={{ padding: '0.85rem', backgroundColor: 'var(--bg-surface)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-md)' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase' }}>
                  Facility Response Note
                </span>
                <p style={{ fontSize: '0.9rem', marginTop: '0.3rem' }}>{referral.facilityResponse}</p>
              </div>
            )}
          </div>
        )}

        {/* Patient Demographics */}
        {patient && (
          <div style={{ padding: '1rem', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <User size={18} style={{ color: 'var(--primary)' }} />
              <h4 style={{ fontSize: '1rem' }}>Patient Demographics (रुग्ण तपशील)</h4>
            </div>
            <div className="grid-3" style={{ fontSize: '0.875rem' }}>
              <div><strong>Name:</strong> {patient.fullName}</div>
              <div><strong>Age / Gender:</strong> {patient.age} yrs • {patient.gender}</div>
              <div><strong>Blood Group:</strong> {patient.bloodGroup || 'N/A'}</div>
              <div><strong>Phone:</strong> {patient.contactNumber}</div>
              <div><strong>Village/Town:</strong> {patient.villageTown}</div>
              <div><strong>District:</strong> {patient.district}, {patient.state}</div>
            </div>
          </div>
        )}

        {/* Case & Intake */}
        {healthcareCase && (
          <div style={{ padding: '1rem', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <Clipboard size={18} style={{ color: 'var(--secondary)' }} />
              <h4 style={{ fontSize: '1rem' }}>Consultation Case: {healthcareCase.caseNumber}</h4>
            </div>
            <div className="grid-3" style={{ fontSize: '0.875rem' }}>
              <div><strong>Case Status:</strong> {healthcareCase.status}</div>
              <div><strong>Priority:</strong> {healthcareCase.priority}</div>
              <div><strong>Intake Modality:</strong> {healthcareCase.intakeChannel}</div>
            </div>
            {healthcareCase.chiefComplaint && (
              <p style={{ fontSize: '0.875rem', color: 'var(--text-main)', marginTop: '0.5rem' }}>
                <strong>Chief Complaint:</strong> {healthcareCase.chiefComplaint}
              </p>
            )}
            {healthcareCase.notes && (
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
                <strong>Notes:</strong> {healthcareCase.notes}
              </p>
            )}
          </div>
        )}

        {/* Recorded Symptoms */}
        {symptoms && symptoms.length > 0 && (
          <div style={{ padding: '1rem', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <Activity size={18} style={{ color: 'var(--warning)' }} />
              <h4 style={{ fontSize: '1rem' }}>Recorded Symptoms ({symptoms.length})</h4>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {symptoms.map((s, idx) => (
                <div
                  key={idx}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '0.5rem 0.75rem',
                    backgroundColor: 'var(--bg-surface-subtle)',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: '0.875rem',
                  }}
                >
                  <div>
                    <strong>{s.symptomName}</strong>
                    {s.bodySite && <span style={{ color: 'var(--text-subtle)' }}> ({s.bodySite})</span>}
                    {s.description && <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{s.description}</div>}
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <span style={{ fontWeight: 700, color: s.severity === 'CRITICAL' ? 'var(--danger)' : 'var(--text-main)' }}>
                      {s.severity}
                    </span>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>{s.durationDays} day(s)</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* AI Screening Review */}
        {aiAssessment && (
          <div style={{ padding: '1rem', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-md)', backgroundColor: '#faf5ff' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <Sparkles size={18} style={{ color: '#9333ea' }} />
              <h4 style={{ fontSize: '1rem', color: '#6b21a8' }}>AI Clinical Screening Intelligence</h4>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '0.75rem' }}>
              <Badge variant="risk" value={aiAssessment.riskLevel} />
              <Badge variant="urgency" value={aiAssessment.urgencyLevel} />
            </div>
            {aiAssessment.urgencyReason && (
              <p style={{ fontSize: '0.875rem', marginBottom: '0.5rem' }}>
                <strong>Urgency Rationale:</strong> {aiAssessment.urgencyReason}
              </p>
            )}
            {aiAssessment.possibleDiseases && aiAssessment.possibleDiseases.length > 0 && (
              <div style={{ fontSize: '0.85rem' }}>
                <strong>Probable Conditions:</strong>{' '}
                {aiAssessment.possibleDiseases.map((d) => `${d.category} (${(d.probability * 100).toFixed(0)}%)`).join(', ')}
              </div>
            )}
          </div>
        )}

        {/* Facility Information */}
        {facility && (
          <div style={{ padding: '1rem', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <Building2 size={18} style={{ color: 'var(--primary)' }} />
              <h4 style={{ fontSize: '1rem' }}>Destination Facility: {facility.name}</h4>
            </div>
            <div className="grid-2" style={{ fontSize: '0.85rem' }}>
              <div><strong>Type:</strong> {facility.facilityTypeDisplayName || facility.facilityType}</div>
              <div><Phone size={14} style={{ display: 'inline', marginRight: '4px' }} />{facility.contactNumber}</div>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};

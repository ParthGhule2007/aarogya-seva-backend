import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { caseApi } from '../../api/caseApi';
import { PatientMedicalRecordDto } from '../../types/case';
import { Badge } from '../common/Badge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { EmptyState } from '../common/EmptyState';
import { QRCodeSVG } from 'qrcode.react';
import {
  FileText,
  Activity,
  Heart,
  Thermometer,
  Calendar,
  Building2,
  Clock,
  Sparkles,
  QrCode,
  AlertCircle,
  RefreshCw,
  CheckCircle,
  Shield,
  Stethoscope,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { VitalsGridDisplay } from '../common/medical/VitalCard';
import { RiskGauge } from '../common/medical/RiskGauge';
import { ReferralTimeline } from '../common/medical/ReferralTimeline';
import { SymptomChip } from '../common/medical/SymptomChip';

export const PatientMedicalRecords: React.FC = () => {
  const { user, selectedPatientId, selectedPatientName } = useAuth();
  const { t } = useLanguage();

  const [records, setRecords] = useState<PatientMedicalRecordDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedCaseId, setExpandedCaseId] = useState<string | null>(null);

  const patientId = selectedPatientId || user?.patientId;

  const loadMedicalRecords = useCallback(async () => {
    if (!patientId) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const data = await caseApi.getPatientMedicalRecords(patientId);
      setRecords(data || []);
      // Expand the first case by default if available
      if (data && data.length > 0 && !expandedCaseId) {
        setExpandedCaseId(data[0].caseDetails?.id);
      }
    } catch (err: any) {
      console.error('Failed to load patient medical records:', err);
      setError(
        err?.response?.data?.message ||
          err?.message ||
          'Failed to load permanent medical records. Please retry.'
      );
    } finally {
      setIsLoading(false);
    }
  }, [patientId, expandedCaseId]);

  useEffect(() => {
    loadMedicalRecords();
  }, [loadMedicalRecords]);

  const toggleExpand = (caseId: string) => {
    setExpandedCaseId((prev) => (prev === caseId ? null : caseId));
  };

  if (!patientId) {
    return (
      <div className="card" style={{ maxWidth: '900px', margin: '0 auto', textAlign: 'center', padding: '3rem 1.5rem' }}>
        <AlertCircle size={40} color="var(--risk-high)" style={{ margin: '0 auto 1rem' }} />
        <h3 style={{ fontSize: '1.25rem', fontWeight: 700 }}>No Patient Profile Active</h3>
        <p style={{ color: 'var(--text-subtle)', marginTop: '0.5rem' }}>
          Please select or register a patient profile to review permanent electronic medical records.
        </p>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
      {/* Header Banner */}
      <div
        className="card"
        style={{
          background: 'linear-gradient(135deg, #0284c7 0%, #0f766e 100%)',
          color: '#ffffff',
          padding: '1.5rem 1.75rem',
          borderRadius: 'var(--radius-lg)',
          marginBottom: '1.5rem',
          boxShadow: 'var(--shadow-md)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
            <div
              style={{
                width: '48px',
                height: '48px',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'rgba(255, 255, 255, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <FileText size={28} color="#ffffff" />
            </div>
            <div>
              <div style={{ fontSize: '0.85rem', opacity: 0.9, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Electronic Health Record (EHR) • कायमस्वरूपी वैद्यकीय नोंदवही
              </div>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 900, margin: '0.2rem 0' }}>
                {selectedPatientName || user?.displayName || 'Patient Records'}
              </h2>
              <div style={{ fontSize: '0.85rem', opacity: 0.9 }}>
                Patient ID: {patientId} • Authoritative PostgreSQL Registry
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <span
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.2)',
                padding: '0.4rem 0.85rem',
                borderRadius: 'var(--radius-full)',
                fontSize: '0.85rem',
                fontWeight: 700,
              }}
            >
              {records.length} {records.length === 1 ? 'Case Recorded' : 'Cases Recorded'}
            </span>

            <button
              className="btn btn-outline"
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                color: '#ffffff',
                borderColor: 'rgba(255, 255, 255, 0.4)',
                minHeight: '38px',
              }}
              onClick={loadMedicalRecords}
              disabled={isLoading}
            >
              <RefreshCw size={14} className={isLoading ? 'spin-animation' : ''} />
              <span>Refresh Records</span>
            </button>
          </div>
        </div>
      </div>

      {/* Loading state */}
      {isLoading && (
        <div className="card" style={{ padding: '3rem 1.5rem' }}>
          <LoadingSpinner message="Retrieving patient electronic health records from Core database..." />
        </div>
      )}

      {/* Error state */}
      {error && !isLoading && (
        <div className="alert-box danger" style={{ marginBottom: '1.5rem' }}>
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Empty state */}
      {!isLoading && !error && records.length === 0 && (
        <EmptyState
          title="No Medical History Recorded"
          description="No clinical triage cases or facility referrals have been recorded for this patient profile yet. Start a 5-step triage screening to create the patient's first medical record."
        />
      )}

      {/* Records Timeline List */}
      {!isLoading && records.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {records.map((rec) => {
            const caseObj = rec.caseDetails;
            const vitals = caseObj?.vitals;
            const symptoms = caseObj?.symptoms || [];
            const ai = rec.aiAssessment;
            const referrals = rec.referrals || [];
            const isExpanded = expandedCaseId === caseObj?.id;

            return (
              <div
                key={caseObj?.id}
                className="card"
                style={{
                  marginBottom: 0,
                  border: isExpanded ? '2px solid var(--primary)' : '1px solid var(--border-light)',
                  boxShadow: isExpanded ? 'var(--shadow-md)' : 'var(--shadow-sm)',
                  transition: 'all 0.2s ease',
                  overflow: 'hidden',
                }}
              >
                {/* Case Summary Bar (Clickable to toggle expand) */}
                <div
                  onClick={() => toggleExpand(caseObj.id)}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    gap: '0.75rem',
                    cursor: 'pointer',
                    userSelect: 'none',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div
                      style={{
                        width: '40px',
                        height: '40px',
                        borderRadius: 'var(--radius-md)',
                        backgroundColor: 'var(--primary-light)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'var(--primary)',
                      }}
                    >
                      <Stethoscope size={20} />
                    </div>

                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <span style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--text-main)' }}>
                          Case {caseObj.caseNumber || caseObj.id.substring(0, 8)}
                        </span>
                        <span className="badge badge-routine" style={{ fontSize: '0.75rem' }}>
                          {caseObj.status}
                        </span>
                        {caseObj.priority && (
                          <span
                            className={`badge ${
                              caseObj.priority === 'CRITICAL' || caseObj.priority === 'HIGH'
                                ? 'badge-urgent'
                                : 'badge-routine'
                            }`}
                            style={{ fontSize: '0.75rem' }}
                          >
                            Priority: {caseObj.priority}
                          </span>
                        )}
                      </div>

                      <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginTop: '0.2rem' }}>
                        <strong>Complaint:</strong> {caseObj.chiefComplaint || 'General Assessment'}
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <div style={{ textAlign: 'right', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', justifyContent: 'flex-end' }}>
                        <Calendar size={13} />
                        <span>{caseObj.createdAt ? new Date(caseObj.createdAt).toLocaleDateString() : 'Recent'}</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', justifyContent: 'flex-end', marginTop: '0.15rem' }}>
                        <Clock size={13} />
                        <span>{caseObj.createdAt ? new Date(caseObj.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}</span>
                      </div>
                    </div>

                    <button
                      className="icon-btn"
                      aria-label="Toggle Case Details"
                      style={{ color: 'var(--primary)' }}
                    >
                      {isExpanded ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                    </button>
                  </div>
                </div>

                {/* Expanded Dossier Content */}
                {isExpanded && (
                  <div
                    style={{
                      marginTop: '1.25rem',
                      paddingTop: '1.25rem',
                      borderTop: '1px solid var(--border-light)',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '1.25rem',
                    }}
                  >
                    {/* Notes & Modality */}
                    {caseObj.notes && (
                      <div
                        style={{
                          padding: '0.75rem 1rem',
                          backgroundColor: 'var(--bg-surface-subtle)',
                          borderRadius: 'var(--radius-sm)',
                          fontSize: '0.875rem',
                          color: 'var(--text-main)',
                        }}
                      >
                        <strong>Clinical Notes:</strong> {caseObj.notes}
                      </div>
                    )}

                    {/* Section 1: Field Vitals */}
                    {vitals && (
                      <div
                        style={{
                          backgroundColor: '#f8fafc',
                          border: '1px solid var(--border-light)',
                          borderRadius: 'var(--radius-md)',
                          padding: '1rem',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                          <Heart size={18} color="var(--primary)" />
                          <h4 style={{ fontSize: '0.95rem', fontWeight: 800, margin: 0 }}>
                            Clinical Field Vitals (शारीरिक तपासणी)
                          </h4>
                        </div>
                        <VitalsGridDisplay vitals={vitals} />
                      </div>
                    )}

                    {/* Section 2: Symptoms */}
                    {symptoms.length > 0 && (
                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                          <Activity size={16} color="var(--primary)" />
                          <h4 style={{ fontSize: '0.95rem', fontWeight: 800, margin: 0 }}>
                            Recorded Symptoms ({symptoms.length})
                          </h4>
                        </div>

                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                          {symptoms.map((s, idx) => (
                            <SymptomChip
                              key={idx}
                              name={s.symptomName}
                              severity={s.severity}
                              durationDays={s.durationDays}
                            />
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Section 3: AI Clinical Screening Results */}
                    {ai && (
                      <div
                        style={{
                          backgroundColor: '#faf5ff',
                          border: '1.5px solid #e9d5ff',
                          borderRadius: 'var(--radius-md)',
                          padding: '1.15rem',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.85rem' }}>
                          <Sparkles size={18} color="#9333ea" />
                          <h4 style={{ fontSize: '0.95rem', fontWeight: 800, color: '#6b21a8', margin: 0 }}>
                            AI Clinical Screening Assessment
                          </h4>
                        </div>

                        <div
                          style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                            gap: '1rem',
                            alignItems: 'center',
                          }}
                        >
                          <RiskGauge
                            riskScore={ai.riskScore}
                            riskLevel={ai.riskLevel}
                            urgencyLevel={ai.urgencyLevel}
                            confidence={ai.confidence}
                            compact
                          />

                          <div>
                            {ai.urgencyReason && (
                              <div style={{ fontSize: '0.85rem', color: '#4c1d95', marginBottom: '0.5rem' }}>
                                <strong>Clinical Rationale:</strong> {ai.urgencyReason}
                              </div>
                            )}

                            {ai.possibleDiseases && ai.possibleDiseases.length > 0 && (
                              <div style={{ fontSize: '0.85rem', marginBottom: '0.5rem' }}>
                                <strong>Predicted Conditions:</strong>{' '}
                                {ai.possibleDiseases.map((d, i) => (
                                  <span key={i} style={{ marginRight: '0.5rem' }}>
                                    • {d.category} ({(d.probability * 100).toFixed(0)}%)
                                  </span>
                                ))}
                              </div>
                            )}

                            {ai.precautions && ai.precautions.length > 0 && (
                              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                                <strong>Advisories:</strong> {ai.precautions.join(' • ')}
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Section 4: Outbound Facility Referrals */}
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.65rem' }}>
                        <Building2 size={16} color="var(--primary)" />
                        <h4 style={{ fontSize: '0.95rem', fontWeight: 800, margin: 0 }}>
                          Facility Referrals ({referrals.length})
                        </h4>
                      </div>

                      {referrals.length === 0 ? (
                        <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', fontStyle: 'italic' }}>
                          No hospital referral issued for this consultation case yet.
                        </div>
                      ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                          {referrals.map((ref) => (
                            <div
                              key={ref.id}
                              style={{
                                padding: '1rem',
                                backgroundColor: '#ffffff',
                                border: '1px solid var(--border-light)',
                                borderRadius: 'var(--radius-md)',
                                boxShadow: 'var(--shadow-sm)',
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'center',
                                flexWrap: 'wrap',
                                gap: '1rem',
                              }}
                            >
                              <div style={{ flex: 1, minWidth: '240px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
                                  <span style={{ fontWeight: 800, fontSize: '1.05rem', color: 'var(--primary)' }}>
                                    {ref.facilityName || 'Regional Healthcare Facility'}
                                  </span>
                                  <Badge variant="status" value={ref.status} />
                                </div>

                                <div style={{ fontSize: '0.85rem', color: 'var(--text-main)', marginBottom: '0.25rem' }}>
                                  <strong>Token:</strong>{' '}
                                  <span style={{ fontFamily: 'monospace', fontWeight: 700 }}>
                                    {ref.referralToken || `REF-${ref.id.substring(0, 8).toUpperCase()}`}
                                  </span>{' '}
                                  • Urgency: <span className="badge badge-urgent">{ref.urgencyLevel || 'URGENT'}</span>
                                </div>

                                {ref.patientMessage && (
                                  <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', marginBottom: '0.25rem' }}>
                                    <strong>Referral Reason:</strong> {ref.patientMessage}
                                  </div>
                                )}

                                {ref.facilityResponse && (
                                  <div
                                    style={{
                                      fontSize: '0.8rem',
                                      color: '#15803d',
                                      backgroundColor: '#f0fdf4',
                                      padding: '0.35rem 0.65rem',
                                      borderRadius: 'var(--radius-sm)',
                                      marginTop: '0.35rem',
                                    }}
                                  >
                                    <strong>Facility Response:</strong> {ref.facilityResponse}
                                  </div>
                                )}

                                <div style={{ display: 'flex', gap: '1rem', fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.4rem', flexWrap: 'wrap' }}>
                                  <div>
                                    Requested: {new Date(ref.requestedAt).toLocaleDateString()}{' '}
                                    {new Date(ref.requestedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                  </div>
                                  {ref.arrivedAt && (
                                    <div style={{ color: 'var(--primary-dark)', fontWeight: 600 }}>
                                      ✓ Arrived & Admitted: {new Date(ref.arrivedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                    </div>
                                  )}
                                  {ref.completedAt && (
                                    <div style={{ color: '#15803d', fontWeight: 600 }}>
                                      ✓ Completed: {new Date(ref.completedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                    </div>
                                  )}
                                </div>

                                <ReferralTimeline
                                  status={ref.status}
                                  requestedAt={ref.requestedAt}
                                  respondedAt={ref.respondedAt}
                                  acceptedAt={ref.acceptedAt}
                                  arrivedAt={ref.arrivedAt}
                                  completedAt={ref.completedAt}
                                  compact
                                />
                              </div>

                              {/* QR Code thumbnail */}
                              <div
                                style={{
                                  backgroundColor: '#ffffff',
                                  padding: '0.5rem',
                                  borderRadius: 'var(--radius-sm)',
                                  border: '1px solid var(--border-light)',
                                  textAlign: 'center',
                                }}
                              >
                                <QRCodeSVG
                                  value={ref.qrCodePayload || ref.referralToken || `AAROGYA-REF:${ref.id}`}
                                  size={72}
                                  level="M"
                                />
                                <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                                  Check-in QR
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

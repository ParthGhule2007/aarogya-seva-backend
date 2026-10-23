import React, { useState, useEffect } from 'react';
import { caseApi } from '../api/caseApi';
import { HealthcareCaseDto } from '../types/case';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { AlertBanner } from '../components/common/AlertBanner';
import { EmptyState } from '../components/common/EmptyState';
import { ClipboardList, Calendar, MapPin, ArrowRight } from 'lucide-react';

interface CasesHistoryPageProps {
  onSelectCase: (caseDto: HealthcareCaseDto) => void;
}

export const CasesHistoryPage: React.FC<CasesHistoryPageProps> = ({ onSelectCase }) => {
  const [cases, setCases] = useState<HealthcareCaseDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [banner, setBanner] = useState<{ type: 'info' | 'success' | 'warning' | 'danger'; message: string } | null>(null);

  useEffect(() => {
    loadCases();
  }, []);

  const loadCases = async () => {
    setIsLoading(true);
    try {
      const data = await caseApi.getAllCases();
      setCases(data);
    } catch (err: any) {
      setBanner({ type: 'danger', message: `Could not load cases: ${err.message}` });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {banner && (
        <AlertBanner
          type={banner.type}
          message={banner.message}
          onDismiss={() => setBanner(null)}
        />
      )}

      <div>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 800 }}>Consultation & Healthcare Cases (केस नोंदी)</h2>
        <p style={{ color: 'var(--text-muted)' }}>
          Audit list of all consultation records, intake channels, triage priorities, and case states.
        </p>
      </div>

      <div className="card">
        {isLoading ? (
          <LoadingSpinner message="Loading all healthcare cases..." />
        ) : cases.length === 0 ? (
          <EmptyState
            title="No Healthcare Cases"
            description="No consultation cases have been created yet. Navigate to 'Care Navigation' to open a case."
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {cases.map((c) => (
              <div
                key={c.id}
                style={{
                  padding: '1.25rem',
                  backgroundColor: 'var(--bg-surface)',
                  border: '1px solid var(--border-light)',
                  borderRadius: 'var(--radius-md)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '1rem',
                  boxShadow: 'var(--shadow-sm)',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.35rem' }}>
                    <ClipboardList size={18} style={{ color: 'var(--primary)' }} />
                    <span style={{ fontWeight: 800, fontSize: '1.1rem' }}>{c.caseNumber}</span>
                    <Badge variant="neutral">{c.status}</Badge>
                    <span
                      className="badge"
                      style={{
                        backgroundColor: c.priority === 'CRITICAL' ? 'var(--danger-bg)' : c.priority === 'HIGH' ? '#ffedd5' : '#fef3c7',
                        color: c.priority === 'CRITICAL' ? 'var(--danger)' : c.priority === 'HIGH' ? '#c2410c' : '#b45309',
                      }}
                    >
                      {c.priority}
                    </span>
                  </div>

                  <div style={{ display: 'flex', gap: '1.25rem', fontSize: '0.85rem', color: 'var(--text-muted)', flexWrap: 'wrap' }}>
                    <div><strong>Modality:</strong> {c.intakeChannel}</div>
                    {c.latitude && c.longitude && (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                        <MapPin size={13} />
                        <span>({c.latitude.toFixed(4)}, {c.longitude.toFixed(4)})</span>
                      </div>
                    )}
                    {c.createdAt && (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                        <Calendar size={13} />
                        <span>{new Date(c.createdAt).toLocaleString()}</span>
                      </div>
                    )}
                  </div>

                  {c.chiefComplaint && (
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-main)', marginTop: '0.4rem', fontWeight: 600 }}>
                      Complaint: {c.chiefComplaint}
                    </div>
                  )}
                  {c.notes && (
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginTop: '0.2rem' }}>
                      "{c.notes}"
                    </p>
                  )}
                </div>

                <Button
                  size="sm"
                  variant="primary"
                  icon={<ArrowRight size={14} />}
                  onClick={() => onSelectCase(c)}
                >
                  Open in Care Navigation
                </Button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

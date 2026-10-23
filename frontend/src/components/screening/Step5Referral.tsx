import React, { useState, useEffect } from 'react';
import { referralApi } from '../../api/referralApi';
import { useLanguage } from '../../context/LanguageContext';
import { ReferralResponseDto } from '../../types/referral';
import { HealthcareFacilityDto } from '../../types/facility';
import { AiAnalysisResponse } from '../../types/ai';
import { QRCodeSVG } from 'qrcode.react';
import {
  QrCode,
  ShieldCheck,
  CheckCircle,
  Building2,
  Calendar,
  Download,
  AlertTriangle,
  RefreshCw,
  Home,
} from 'lucide-react';

interface Step5ReferralProps {
  caseId: string;
  patientId: string;
  patientName: string;
  facility: HealthcareFacilityDto;
  aiAnalysis: AiAnalysisResponse | null;
  onFinish: () => void;
}

export const Step5Referral: React.FC<Step5ReferralProps> = ({
  caseId,
  patientId,
  patientName,
  facility,
  aiAnalysis,
  onFinish,
}) => {
  const { t } = useLanguage();
  const [referral, setReferral] = useState<ReferralResponseDto | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const createReferralToken = async () => {
      setIsLoading(true);
      setError(null);
      const facId = facility.facilityId || facility.id || '';
      const urgency = (aiAnalysis?.urgency?.level || aiAnalysis?.urgencyLevel || 'ROUTINE') as any;
      try {
        const res = await referralApi.createReferral({
          caseId,
          patientId,
          facilityId: facId,
          urgencyLevel: urgency,
          reason: aiAnalysis?.explanation || 'Clinical triage care referral',
        });
        setReferral(res);
      } catch (err: any) {
        // If referral already exists for case, try fetching
        try {
          const list = await referralApi.getReferralsByCaseId(caseId);
          if (list && list.length > 0) {
            setReferral(list[0]);
          } else {
            throw new Error('No existing referral found');
          }
        } catch {
          // Resilient provisional referral token for offline rural care continuity
          const provisionalRef: ReferralResponseDto = {
            id: 'ref-prov-' + Date.now(),
            referralToken: 'REF-MH-' + Math.floor(100000 + Math.random() * 900000),
            qrCodePayload: `AAROGYA-REF:${caseId}:${facId}:${patientId}`,
            caseId,
            patientId,
            facilityId: facId,
            facilityName: facility.name || facility.facilityName || 'Primary Health Centre',
            urgencyLevel: urgency,
            status: 'PENDING',
            requestedAt: new Date().toISOString(),
            createdAt: new Date().toISOString(),
          };
          setReferral(provisionalRef);
          setError('Referral service offline: Generated provisional offline token for clinic registration.');
        }
      } finally {
        setIsLoading(false);
      }
    };

    if (caseId && (facility?.id || facility?.facilityId)) {
      createReferralToken();
    }
  }, [caseId, patientId, facility, aiAnalysis]);

  const qrPayload = referral?.qrCodePayload || referral?.referralToken || `AAROGYA-REF:${referral?.id || caseId}`;

  return (
    <div className="card">
      <div className="card-header">
        <div className="card-title">
          <QrCode size={22} color="var(--primary)" />
          <span>{t('step5Title')}</span>
        </div>
        <span className="badge badge-routine">Step 5 of 5</span>
      </div>

      <p style={{ color: 'var(--text-subtle)', marginBottom: '1.25rem' }}>
        {t('step5Subtitle')}
      </p>

      {isLoading && (
        <div style={{ textAlign: 'center', padding: '3rem 1rem' }}>
          <RefreshCw size={36} color="var(--primary)" style={{ animation: 'spin 1s linear infinite' }} />
          <div style={{ marginTop: '1rem', fontWeight: 700, fontSize: '1.1rem' }}>
            Issuing Official Healthcare Referral Token...
          </div>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginTop: '0.25rem' }}>
            Registering case token with destination facility database.
          </div>
        </div>
      )}

      {error && (
        <div className="alert-box danger">
          <AlertTriangle size={20} />
          <span>{error}</span>
        </div>
      )}

      {referral && !isLoading && (
        <div
          style={{
            border: '2px dashed var(--primary)',
            borderRadius: 'var(--radius-lg)',
            padding: '2rem 1.5rem',
            background: 'var(--primary-light)',
            textAlign: 'center',
            marginBottom: '1.5rem',
          }}
        >
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', color: 'var(--primary-hover)', fontWeight: 800, fontSize: '1.2rem', marginBottom: '0.5rem' }}>
            <ShieldCheck size={26} />
            <span>Official Telemedicine Care Referral Token</span>
          </div>

          <div style={{ fontSize: '1.3rem', fontWeight: 900, letterSpacing: '1px', color: 'var(--text-main)', margin: '0.75rem 0' }}>
            {referral.referralToken || `REF-${referral.id.substring(0, 8).toUpperCase()}`}
          </div>

          {/* Scannable SVG QR Code */}
          <div
            style={{
              background: '#ffffff',
              padding: '1.25rem',
              borderRadius: 'var(--radius-md)',
              display: 'inline-block',
              boxShadow: 'var(--shadow-md)',
              margin: '1rem auto',
            }}
          >
            <QRCodeSVG value={qrPayload} size={180} level="M" />
            <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginTop: '0.5rem', fontWeight: 600 }}>
              Scan at Facility Desk for Check-In
            </div>
          </div>

          {/* Referral Metadata Dossier */}
          <div
            style={{
              maxWidth: '480px',
              margin: '1rem auto 0 auto',
              textAlign: 'left',
              background: '#ffffff',
              padding: '1rem 1.25rem',
              borderRadius: 'var(--radius-md)',
              fontSize: '0.9rem',
              border: '1px solid var(--border-light)',
            }}
          >
            <div style={{ marginBottom: '0.4rem' }}>
              <strong>Patient:</strong> {patientName}
            </div>
            <div style={{ marginBottom: '0.4rem' }}>
              <strong>Referred Facility:</strong> {facility.facilityName}
            </div>
            <div style={{ marginBottom: '0.4rem' }}>
              <strong>Urgency Level:</strong> <span className="badge badge-urgent">{referral.urgencyLevel || 'URGENT'}</span>
            </div>
            <div style={{ marginBottom: '0.4rem' }}>
              <strong>Status:</strong> <span className="badge badge-routine">{referral.status || 'PENDING'}</span>
            </div>
            <div>
              <strong>Issued On:</strong> {new Date().toLocaleDateString()}
            </div>
          </div>
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'center', gap: '1rem', marginTop: '1.5rem' }}>
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => window.print()}
          disabled={!referral}
        >
          <Download size={18} />
          <span>Print / Save Token</span>
        </button>

        <button
          type="button"
          className="btn btn-primary"
          style={{ minWidth: '180px' }}
          onClick={onFinish}
        >
          <Home size={18} />
          <span>Complete & Return</span>
        </button>
      </div>
    </div>
  );
};

import React from 'react';
import { AlertCircle, CheckCircle, Clock, ShieldAlert, Sparkles, Building } from 'lucide-react';
import { RiskLevel, UrgencyLevel } from '../../types/ai';
import { ReferralStatus } from '../../types/referral';
import { FacilityType } from '../../types/facility';

interface BadgeProps {
  children?: React.ReactNode;
  variant?: 'risk' | 'urgency' | 'status' | 'facilityType' | 'demo' | 'neutral';
  value?: string;
  size?: 'sm' | 'md';
}

export const Badge: React.FC<BadgeProps> = ({ variant = 'neutral', value, children, size = 'md' }) => {
  const text = children || value;

  if (variant === 'risk' && value) {
    const risk = value as RiskLevel;
    let className = 'badge-moderate';
    let icon = <AlertCircle size={12} />;

    if (risk === 'LOW') {
      className = 'badge-low';
      icon = <CheckCircle size={12} />;
    } else if (risk === 'HIGH') {
      className = 'badge-high';
      icon = <AlertCircle size={12} />;
    } else if (risk === 'CRITICAL') {
      className = 'badge-critical';
      icon = <ShieldAlert size={12} />;
    }

    return (
      <span className={`badge ${className} ${size === 'sm' ? 'btn-sm' : ''}`}>
        {icon}
        <span>Risk: {risk}</span>
      </span>
    );
  }

  if (variant === 'urgency' && value) {
    const urgency = value as UrgencyLevel;
    let className = 'badge-routine';
    let icon = <Clock size={12} />;

    if (urgency === 'PRIORITY') {
      className = 'badge-priority';
    } else if (urgency === 'URGENT') {
      className = 'badge-urgent';
      icon = <AlertCircle size={12} />;
    } else if (urgency === 'EMERGENCY') {
      className = 'badge-emergency';
      icon = <ShieldAlert size={12} />;
    }

    return (
      <span className={`badge ${className} ${size === 'sm' ? 'btn-sm' : ''}`}>
        {icon}
        <span>{urgency}</span>
      </span>
    );
  }

  if (variant === 'status' && value) {
    const status = value as ReferralStatus;
    let className = 'badge-neutral';
    let icon = <Clock size={12} />;

    if (status === 'PENDING') {
      className = 'badge-pending';
      icon = <Clock size={12} />;
    } else if (status === 'ACCEPTED') {
      className = 'badge-accepted';
      icon = <CheckCircle size={12} />;
    } else if (status === 'IN_PROGRESS') {
      className = 'badge-in-progress';
      icon = <Clock size={12} />;
    } else if (status === 'COMPLETED') {
      className = 'badge-completed';
      icon = <CheckCircle size={12} />;
    } else if (status === 'REJECTED') {
      className = 'badge-rejected';
      icon = <AlertCircle size={12} />;
    }

    return (
      <span className={`badge ${className} ${size === 'sm' ? 'btn-sm' : ''}`}>
        {icon}
        <span>{status.replace('_', ' ')}</span>
      </span>
    );
  }

  if (variant === 'facilityType' && value) {
    const type = value as FacilityType;
    return (
      <span className="badge badge-neutral">
        <Building size={12} />
        <span>{type.replace(/_/g, ' ')}</span>
      </span>
    );
  }

  if (variant === 'demo') {
    return (
      <span className="badge badge-demo">
        <Sparkles size={12} />
        <span>DEMO / SEED FACILITY</span>
      </span>
    );
  }

  return <span className="badge badge-neutral">{text}</span>;
};

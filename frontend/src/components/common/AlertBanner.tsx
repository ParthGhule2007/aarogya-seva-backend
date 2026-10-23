import React from 'react';
import { AlertCircle, CheckCircle, Info, ShieldAlert } from 'lucide-react';

interface AlertBannerProps {
  type?: 'info' | 'success' | 'warning' | 'danger';
  title?: string;
  message: string;
  onDismiss?: () => void;
}

export const AlertBanner: React.FC<AlertBannerProps> = ({
  type = 'info',
  title,
  message,
}) => {
  const icons = {
    info: <Info size={20} />,
    success: <CheckCircle size={20} />,
    warning: <AlertCircle size={20} />,
    danger: <ShieldAlert size={20} />,
  };

  return (
    <div className={`alert alert-${type}`}>
      <div style={{ flexShrink: 0, marginTop: '2px' }}>{icons[type]}</div>
      <div style={{ flex: 1 }}>
        {title && <div style={{ fontWeight: 700, marginBottom: '0.2rem' }}>{title}</div>}
        <div>{message}</div>
      </div>
    </div>
  );
};

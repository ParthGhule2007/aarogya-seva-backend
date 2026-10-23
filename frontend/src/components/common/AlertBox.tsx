import React, { ReactNode } from 'react';
import { Info, CheckCircle2, AlertTriangle, AlertOctagon } from 'lucide-react';

interface AlertBoxProps {
  type?: 'info' | 'success' | 'warning' | 'danger';
  children: ReactNode;
  icon?: ReactNode;
}

export const AlertBox: React.FC<AlertBoxProps> = ({ type = 'info', children, icon }) => {
  const getIcon = () => {
    if (icon) return icon;
    switch (type) {
      case 'success':
        return <CheckCircle2 size={24} style={{ color: 'var(--secondary)', flexShrink: 0 }} />;
      case 'warning':
        return <AlertTriangle size={24} style={{ color: 'var(--risk-moderate)', flexShrink: 0 }} />;
      case 'danger':
        return <AlertOctagon size={24} style={{ color: 'var(--risk-high)', flexShrink: 0 }} />;
      default:
        return <Info size={24} style={{ color: 'var(--primary)', flexShrink: 0 }} />;
    }
  };

  return (
    <div className={`alert-box alert-${type}`} role="alert">
      {getIcon()}
      <div style={{ flex: 1 }}>{children}</div>
    </div>
  );
};

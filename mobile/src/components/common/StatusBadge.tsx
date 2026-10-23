import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';

interface StatusBadgeProps {
  status: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const { colors } = useTheme();
  const { t } = useLanguage();

  const s = (status || '').toUpperCase();

  let bg = colors.bgSubtle;
  let text = colors.textSubtle;
  let border = colors.borderLight;

  if (s === 'TAKEN' || s === 'COMPLETED' || s === 'RESOLVED' || s === 'AVAILABLE' || s === 'ACCEPTED') {
    bg = colors.riskLowBg;
    text = colors.riskLow;
    border = colors.riskLowBorder;
  } else if (s === 'PENDING' || s === 'REQUESTED' || s === 'SCHEDULED' || s === 'READY' || s === 'BUSY') {
    bg = colors.riskModerateBg;
    text = colors.riskModerate;
    border = colors.riskModerateBorder;
  } else if (s === 'MISSED' || s === 'FAILED' || s === 'REJECTED' || s === 'CANCELLED' || s === 'EMERGENCY' || s === 'CRITICAL') {
    bg = colors.riskHighBg;
    text = colors.riskHigh;
    border = colors.riskHighBorder;
  } else if (s === 'IN_CALL' || s === 'ACTIVE') {
    bg = colors.primaryLight;
    text = colors.primary;
    border = colors.primaryBorder;
  }

  const getTranslatedLabel = (val: string) => {
    switch (val) {
      case 'TAKEN': return t('statusTaken');
      case 'MISSED': return t('statusMissed');
      case 'PENDING': return t('statusPending');
      case 'SKIPPED': return t('statusSkipped');
      case 'ACCEPTED': return t('statusAccepted');
      case 'COMPLETED': return t('statusCompleted');
      case 'REQUESTED': return t('statusRequested');
      case 'AVAILABLE': return t('statusAvailable');
      case 'BUSY': return t('statusBusy');
      case 'OFFLINE': return t('statusOffline');
      case 'RESOLVED': return t('statusResolved');
      case 'PROCESSING': return t('statusProcessing');
      case 'TRIGGERED': return t('statusTriggered');
      case 'FAILED': return t('statusFailed');
      case 'CANCELLED': return t('statusCancelled');
      case 'IN_CALL': return t('statusInCall');
      case 'READY': return t('statusReady');
      default: return val;
    }
  };

  return (
    <View style={[styles.badge, { backgroundColor: bg, borderColor: border }]}>
      <Text style={[styles.badgeText, { color: text }]}>{getTranslatedLabel(s)}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    borderWidth: 1,
    alignSelf: 'flex-start',
  },
  badgeText: {
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.4,
  },
});

import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../../context/ThemeContext';
import { Button } from './Button';

interface EmptyStateProps {
  icon?: any;
  title: string;
  description?: string;
  actionTitle?: string;
  onAction?: () => void;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon = 'document-text-outline',
  title,
  description,
  actionTitle,
  onAction,
}) => {
  const { colors } = useTheme();

  // If icon is an emoji or single char string, map common ones or show icon
  const isIonicons = typeof icon === 'string' && icon.includes('-');

  return (
    <View style={[styles.container, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight }]}>
      {isIonicons ? (
        <Ionicons name={icon as any} size={42} color={colors.primary} style={{ marginBottom: 12 }} />
      ) : (
        <Ionicons name="document-text-outline" size={42} color={colors.primary} style={{ marginBottom: 12 }} />
      )}
      <Text style={[styles.title, { color: colors.textMain }]}>{title}</Text>
      {description ? (
        <Text style={[styles.description, { color: colors.textSubtle }]}>{description}</Text>
      ) : null}
      {actionTitle && onAction ? (
        <Button
          title={actionTitle}
          onPress={onAction}
          variant="outline"
          style={styles.actionBtn}
        />
      ) : null}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 24,
    borderRadius: 16,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 12,
  },
  icon: {
    fontSize: 40,
    marginBottom: 12,
  },
  title: {
    fontSize: 16,
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: 6,
  },
  description: {
    fontSize: 13,
    textAlign: 'center',
    lineHeight: 18,
    marginBottom: 14,
  },
  actionBtn: {
    marginTop: 8,
    minHeight: 40,
    paddingVertical: 8,
  },
});

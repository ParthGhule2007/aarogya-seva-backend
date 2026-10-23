import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../../context/ThemeContext';
import { EmergencySosModal } from './EmergencySosModal';

export const EmergencySosButton: React.FC = () => {
  const { colors } = useTheme();
  const [modalVisible, setModalVisible] = useState(false);

  return (
    <>
      <TouchableOpacity
        style={[styles.floatingButton, { backgroundColor: colors.sosRed }]}
        onPress={() => setModalVisible(true)}
        activeOpacity={0.8}
        accessibilityRole="button"
        accessibilityLabel="Trigger Emergency SOS"
      >
        <Ionicons name="warning" size={18} color="#FFFFFF" style={{ marginRight: 6 }} />
        <Text style={styles.label}>SOS 108 / 112</Text>
      </TouchableOpacity>

      <EmergencySosModal
        visible={modalVisible}
        onClose={() => setModalVisible(false)}
      />
    </>
  );
};

const styles = StyleSheet.create({
  floatingButton: {
    position: 'absolute',
    bottom: 80,
    right: 16,
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 9999,
    elevation: 8,
    shadowColor: '#DC2626',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 8,
    zIndex: 999,
  },
  icon: {
    fontSize: 20,
    marginRight: 6,
  },
  label: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 14,
    letterSpacing: 0.5,
  },
});

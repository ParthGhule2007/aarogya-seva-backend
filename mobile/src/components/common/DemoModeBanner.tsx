import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { IS_DEMO_MODE } from '../../config/apiConfig';

interface DemoModeBannerProps {
  visible?: boolean;
  message?: string;
}

export const DemoModeBanner: React.FC<DemoModeBannerProps> = ({
  visible = IS_DEMO_MODE,
  message = 'DEMO DATA — EXPO_PUBLIC_DEMO_MODE=true',
}) => {
  if (!visible) return null;

  return (
    <View style={styles.banner}>
      <Text style={styles.text}>⚠️ {message}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  banner: {
    backgroundColor: '#FEF3C7',
    borderColor: '#F59E0B',
    borderWidth: 1,
    borderRadius: 8,
    paddingVertical: 6,
    paddingHorizontal: 12,
    marginBottom: 12,
    alignItems: 'center',
  },
  text: {
    color: '#92400E',
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0.4,
  },
});

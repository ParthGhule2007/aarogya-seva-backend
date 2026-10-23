import React from 'react';
import {
  TouchableOpacity,
  Text,
  StyleSheet,
  ActivityIndicator,
  ViewStyle,
  TextStyle,
} from 'react-native';
import { useTheme } from '../../context/ThemeContext';

interface ButtonProps {
  title: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'danger' | 'outline' | 'subtle';
  loading?: boolean;
  disabled?: boolean;
  style?: ViewStyle | ViewStyle[];
  textStyle?: TextStyle | TextStyle[];
  icon?: string;
}

export const Button: React.FC<ButtonProps> = ({
  title,
  onPress,
  variant = 'primary',
  loading = false,
  disabled = false,
  style,
  textStyle,
  icon,
}) => {
  const { colors } = useTheme();

  let bgColor = colors.primary;
  let textColor = '#FFFFFF';
  let borderColor = 'transparent';

  if (variant === 'secondary') {
    bgColor = colors.secondary;
  } else if (variant === 'danger') {
    bgColor = colors.sosRed;
  } else if (variant === 'outline') {
    bgColor = 'transparent';
    borderColor = colors.primary;
    textColor = colors.primary;
  } else if (variant === 'subtle') {
    bgColor = colors.bgSubtle;
    borderColor = colors.borderLight;
    textColor = colors.textMain;
  }

  return (
    <TouchableOpacity
      style={[
        styles.button,
        {
          backgroundColor: disabled ? colors.borderLight : bgColor,
          borderColor: disabled ? colors.borderMedium : borderColor,
        },
        style,
      ]}
      onPress={onPress}
      disabled={disabled || loading}
      activeOpacity={0.8}
    >
      {loading ? (
        <ActivityIndicator color={textColor} size="small" />
      ) : (
        <Text
          style={[
            styles.text,
            { color: disabled ? colors.textSubtle : textColor },
            textStyle,
          ]}
        >
          {icon ? `${icon}  ` : ''}
          {title}
        </Text>
      )}
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  button: {
    minHeight: 48,
    borderRadius: 12,
    borderWidth: 1,
    paddingVertical: 12,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    fontSize: 15,
    fontWeight: '700',
    textAlign: 'center',
  },
});

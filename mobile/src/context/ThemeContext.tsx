import React, { createContext, useContext, useState, ReactNode } from 'react';
import { ThemeColors, lightColors, darkColors } from '../theme/theme';

interface ThemeContextType {
  isDark: boolean;
  colors: ThemeColors;
  toggleTheme: () => void;
  isElderlyMode: boolean;
  toggleElderlyMode: () => void;
  fontScale: number;
  touchTargetMin: number;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [isDark, setIsDark] = useState(false);
  const [isElderlyMode, setIsElderlyMode] = useState(false);

  const colors = isDark ? darkColors : lightColors;

  const toggleTheme = () => setIsDark((prev) => !prev);
  const toggleElderlyMode = () => setIsElderlyMode((prev) => !prev);

  const fontScale = isElderlyMode ? 1.25 : 1.0;
  const touchTargetMin = isElderlyMode ? 62 : 44;

  return (
    <ThemeContext.Provider
      value={{
        isDark,
        colors,
        toggleTheme,
        isElderlyMode,
        toggleElderlyMode,
        fontScale,
        touchTargetMin,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = (): ThemeContextType => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

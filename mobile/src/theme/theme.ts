export interface ThemeColors {
  primary: string;
  primaryHover: string;
  primaryLight: string;
  primaryBorder: string;

  secondary: string;
  secondaryHover: string;
  secondaryLight: string;

  accentWarm: string;
  accentWarmLight: string;

  bgApp: string;
  bgSurface: string;
  bgSubtle: string;

  textMain: string;
  textMuted: string;
  textSubtle: string;
  textInverse: string;

  borderLight: string;
  borderMedium: string;

  // Clinical risk levels
  riskLow: string;
  riskLowBg: string;
  riskLowBorder: string;

  riskModerate: string;
  riskModerateBg: string;
  riskModerateBorder: string;

  riskHigh: string;
  riskHighBg: string;
  riskHighBorder: string;

  riskCritical: string;
  riskCriticalBg: string;
  riskCriticalBorder: string;

  // SOS Emergency
  sosRed: string;
  sosRedLight: string;
}

export const lightColors: ThemeColors = {
  primary: '#0F766E',          // Medical Teal 700 (Primary brand color)
  primaryHover: '#0D9488',     // Medical Teal 600
  primaryLight: '#F0FDFA',     // Teal 50
  primaryBorder: '#99F6E4',    // Teal 200

  secondary: '#059669',        // Health Emerald 600
  secondaryHover: '#047857',   // Emerald 700
  secondaryLight: '#ECFDF5',   // Emerald 50

  accentWarm: '#D97706',       // Amber 600
  accentWarmLight: '#FEF3C7',

  bgApp: '#F8FAFC',           // Clean neutral off-white
  bgSurface: '#FFFFFF',
  bgSubtle: '#F1F5F9',

  textMain: '#0F172A',        // Slate 900
  textMuted: '#334155',       // Slate 700
  textSubtle: '#64748B',      // Slate 500
  textInverse: '#FFFFFF',

  borderLight: '#E2E8F0',
  borderMedium: '#CBD5E1',

  riskLow: '#15803D',
  riskLowBg: '#DCFCE7',
  riskLowBorder: '#86EFAC',

  riskModerate: '#B45309',
  riskModerateBg: '#FEF3C7',
  riskModerateBorder: '#FDE68A',

  riskHigh: '#B91C1C',
  riskHighBg: '#FEE2E2',
  riskHighBorder: '#FCA5A5',

  riskCritical: '#991B1B',
  riskCriticalBg: '#FFE4E6',
  riskCriticalBorder: '#FECDD3',

  sosRed: '#DC2626',
  sosRedLight: '#FEE2E2',
};

export const darkColors: ThemeColors = {
  primary: '#2DD4BF',         // Teal 400
  primaryHover: '#5EEAD4',
  primaryLight: '#134E4A',
  primaryBorder: '#5EEAD4',

  secondary: '#34D399',
  secondaryHover: '#6EE7B7',
  secondaryLight: '#064E3B',

  accentWarm: '#F59E0B',
  accentWarmLight: '#78350F',

  bgApp: '#090D16',           // Deep dark blue-black
  bgSurface: '#111827',       // Gray 900
  bgSubtle: '#1F2937',        // Gray 800

  textMain: '#F9FAFB',
  textMuted: '#E5E7EB',
  textSubtle: '#9CA3AF',
  textInverse: '#000000',

  borderLight: '#374151',
  borderMedium: '#6B7280',

  riskLow: '#6EE7B7',
  riskLowBg: '#064E3B',
  riskLowBorder: '#059669',

  riskModerate: '#FDE68A',
  riskModerateBg: '#78350F',
  riskModerateBorder: '#B45309',

  riskHigh: '#FCA5A5',
  riskHighBg: '#7F1D1D',
  riskHighBorder: '#B91C1C',

  riskCritical: '#FECDD3',
  riskCriticalBg: '#881337',
  riskCriticalBorder: '#991B1B',

  sosRed: '#EF4444',
  sosRedLight: '#7F1D1D',
};

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
};

export const radii = {
  sm: 8,
  md: 12,
  lg: 16,
  full: 9999,
};

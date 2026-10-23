import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { en, TranslationKey } from '../locales/en';
import { hi } from '../locales/hi';
import { mr } from '../locales/mr';

export type SupportedLanguage = 'en' | 'hi' | 'mr';
export type FontSizeScale = 'normal' | 'large' | 'xlarge';

interface LanguageContextType {
  language: SupportedLanguage;
  setLanguage: (lang: SupportedLanguage) => void;
  t: (key: TranslationKey, fallback?: string) => string;
  isHighContrast: boolean;
  toggleHighContrast: () => void;
  fontSize: FontSizeScale;
  setFontSize: (size: FontSizeScale) => void;
  cycleFontSize: () => void;
}

const translations: Record<SupportedLanguage, Record<TranslationKey, string>> = {
  en,
  hi,
  mr,
};

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

export const LanguageProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [language, setLanguageState] = useState<SupportedLanguage>(() => {
    try {
      const saved = localStorage.getItem('aarogya_lang');
      if (saved === 'en' || saved === 'hi' || saved === 'mr') return saved;
    } catch {
      // fallback
    }
    return 'en';
  });

  const [isHighContrast, setIsHighContrast] = useState<boolean>(() => {
    try {
      return localStorage.getItem('aarogya_high_contrast') === 'true';
    } catch {
      return false;
    }
  });

  const [fontSize, setFontSizeState] = useState<FontSizeScale>(() => {
    try {
      const saved = localStorage.getItem('aarogya_font_size');
      if (saved === 'normal' || saved === 'large' || saved === 'xlarge') return saved;
    } catch {
      // fallback
    }
    return 'normal';
  });

  useEffect(() => {
    try {
      localStorage.setItem('aarogya_lang', language);
    } catch {
      // ignore
    }
  }, [language]);

  useEffect(() => {
    try {
      localStorage.setItem('aarogya_high_contrast', String(isHighContrast));
    } catch {
      // ignore
    }
    if (isHighContrast) {
      document.documentElement.setAttribute('data-contrast', 'high');
    } else {
      document.documentElement.removeAttribute('data-contrast');
    }
  }, [isHighContrast]);

  useEffect(() => {
    try {
      localStorage.setItem('aarogya_font_size', fontSize);
    } catch {
      // ignore
    }
    document.documentElement.setAttribute('data-font-size', fontSize);
  }, [fontSize]);

  const setLanguage = (lang: SupportedLanguage) => {
    setLanguageState(lang);
  };

  const toggleHighContrast = () => {
    setIsHighContrast((prev) => !prev);
  };

  const setFontSize = (size: FontSizeScale) => {
    setFontSizeState(size);
  };

  const cycleFontSize = () => {
    setFontSizeState((prev) => {
      if (prev === 'normal') return 'large';
      if (prev === 'large') return 'xlarge';
      return 'normal';
    });
  };

  const t = (key: TranslationKey, fallback?: string): string => {
    const dict = translations[language] || translations.en;
    if (dict && dict[key]) {
      return dict[key];
    }
    return translations.en[key] || fallback || key;
  };

  return (
    <LanguageContext.Provider
      value={{
        language,
        setLanguage,
        t,
        isHighContrast,
        toggleHighContrast,
        fontSize,
        setFontSize,
        cycleFontSize,
      }}
    >
      {children}
    </LanguageContext.Provider>
  );
};

export const useLanguage = (): LanguageContextType => {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider');
  }
  return context;
};

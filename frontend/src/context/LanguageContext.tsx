import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { en, TranslationKey } from '../locales/en';
import { hi } from '../locales/hi';
import { mr } from '../locales/mr';

export type SupportedLanguage = 'en' | 'hi' | 'mr';

interface LanguageContextType {
  language: SupportedLanguage;
  setLanguage: (lang: SupportedLanguage) => void;
  t: (key: TranslationKey, fallback?: string) => string;
  isHighContrast: boolean;
  toggleHighContrast: () => void;
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

  const setLanguage = (lang: SupportedLanguage) => {
    setLanguageState(lang);
  };

  const toggleHighContrast = () => {
    setIsHighContrast((prev) => !prev);
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

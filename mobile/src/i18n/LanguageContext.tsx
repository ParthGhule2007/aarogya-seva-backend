import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import * as SecureStore from 'expo-secure-store';
import { en, TranslationKey } from './en';
import { hi } from './hi';
import { mr } from './mr';

export type SupportedLanguage = 'en' | 'hi' | 'mr';

interface LanguageContextType {
  language: SupportedLanguage;
  setLanguage: (lang: SupportedLanguage) => void;
  t: (key: TranslationKey, fallback?: string) => string;
}

const translations: Record<SupportedLanguage, Record<TranslationKey, string>> = {
  en,
  hi,
  mr,
};

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);
const LANGUAGE_STORAGE_KEY = 'aarogya_lang';

export const LanguageProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [language, setLanguageState] = useState<SupportedLanguage>('en');

  useEffect(() => {
    (async () => {
      try {
        const saved = await SecureStore.getItemAsync(LANGUAGE_STORAGE_KEY);
        if (saved === 'en' || saved === 'hi' || saved === 'mr') {
          setLanguageState(saved);
        }
      } catch {
        // ignore
      }
    })();
  }, []);

  const setLanguage = async (lang: SupportedLanguage) => {
    setLanguageState(lang);
    try {
      await SecureStore.setItemAsync(LANGUAGE_STORAGE_KEY, lang);
    } catch {
      // ignore
    }
  };

  const t = (key: TranslationKey, fallback?: string): string => {
    const dict = translations[language] || translations.en;
    if (dict && dict[key]) {
      return dict[key];
    }
    if (translations.en[key]) {
      return translations.en[key];
    }
    return fallback || String(key);
  };

  return (
    <LanguageContext.Provider value={{ language, setLanguage, t }}>
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

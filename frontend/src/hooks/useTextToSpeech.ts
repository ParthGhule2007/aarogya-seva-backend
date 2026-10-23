import { useState, useEffect, useCallback, useRef } from 'react';
import { SupportedLanguage } from '../context/LanguageContext';

interface UseTextToSpeechReturn {
  speak: (text: string, customLang?: SupportedLanguage) => void;
  stop: () => void;
  isSpeaking: boolean;
  isSupported: boolean;
}

export function useTextToSpeech(defaultLang: SupportedLanguage = 'hi'): UseTextToSpeechReturn {
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [voices, setVoices] = useState<SpeechSynthesisVoice[]>([]);
  const utteranceRef = useRef<SpeechSynthesisUtterance | null>(null);

  const isSupported = typeof window !== 'undefined' && 'speechSynthesis' in window;

  useEffect(() => {
    if (!isSupported) return;

    const updateVoices = () => {
      const available = window.speechSynthesis.getVoices();
      setVoices(available);
    };

    updateVoices();
    window.speechSynthesis.onvoiceschanged = updateVoices;

    return () => {
      if (window.speechSynthesis.speaking) {
        window.speechSynthesis.cancel();
      }
    };
  }, [isSupported]);

  const getBcp47Lang = (l: SupportedLanguage): string => {
    switch (l) {
      case 'hi':
        return 'hi-IN';
      case 'mr':
        return 'mr-IN';
      case 'en':
      default:
        return 'en-IN';
    }
  };

  const stop = useCallback(() => {
    if (!isSupported) return;
    try {
      window.speechSynthesis.cancel();
    } catch {
      // ignore
    }
    setIsSpeaking(false);
  }, [isSupported]);

  const speak = useCallback(
    (text: string, customLang?: SupportedLanguage) => {
      if (!isSupported || !text.trim()) return;

      // Stop any current speech first
      stop();

      const langCode = getBcp47Lang(customLang || defaultLang);
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = langCode;
      utterance.rate = 0.92; // Slightly slower, clear cadence for elderly comprehension
      utterance.pitch = 1.0;

      // Find matching voice if available
      const matchingVoice = voices.find(
        (v) => v.lang.toLowerCase() === langCode.toLowerCase() || v.lang.startsWith(langCode.slice(0, 2))
      );
      if (matchingVoice) {
        utterance.voice = matchingVoice;
      }

      utterance.onstart = () => {
        setIsSpeaking(true);
      };

      utterance.onend = () => {
        setIsSpeaking(false);
      };

      utterance.onerror = (e) => {
        console.warn('Speech synthesis error:', e);
        setIsSpeaking(false);
      };

      utteranceRef.current = utterance;
      window.speechSynthesis.speak(utterance);
    },
    [isSupported, defaultLang, voices, stop]
  );

  return {
    speak,
    stop,
    isSpeaking,
    isSupported,
  };
}

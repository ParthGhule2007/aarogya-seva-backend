import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { chatApi } from '../../api/chatApi';
import { SymptomSeverity } from '../../types/symptom';
import {
  MessageSquare,
  FileText,
  Mic,
  MicOff,
  Activity,
  Thermometer,
  Heart,
  Send,
  AlertCircle,
  Clock,
  CheckCircle2,
  X,
  Stethoscope,
  RefreshCw,
} from 'lucide-react';
import { ChatMessageResponse } from '../../types/chat';

export interface SymptomsFormData {
  chiefComplaint: string;
  selectedSymptoms: string[];
  severity: SymptomSeverity | number;
  durationDays: number;
  notes: string;
  vitals: {
    temperatureFahrenheit?: number;
    systolicBp?: number;
    diastolicBp?: number;
    pulseBpm?: number;
  };
}

interface Step2SymptomsProps {
  formData: SymptomsFormData;
  setFormData: React.Dispatch<React.SetStateAction<SymptomsFormData>>;
  onNext: () => void;
  onBack: () => void;
  isSubmitting?: boolean;
  submitError?: string | null;
}

interface SymptomOption {
  id: string;
  en: string;
  hi: string;
  mr: string;
  icon: string;
}

const RURAL_SYMPTOMS: SymptomOption[] = [
  { id: 'fever', en: 'Fever', hi: 'बुखार', mr: 'ताप', icon: '🌡️' },
  { id: 'cough', en: 'Cough', hi: 'खांसी', mr: 'खोकला', icon: '🫁' },
  { id: 'chest_pain', en: 'Chest Pain', hi: 'छाती में दर्द', mr: 'छातीत दुखणे', icon: '🫀' },
  { id: 'shortness_of_breath', en: 'Shortness of Breath', hi: 'सांस फूलना', mr: 'दम लागणे', icon: '😮‍💨' },
  { id: 'headache', en: 'Headache', hi: 'सिरदर्द', mr: 'डोकेदुखी', icon: '🤕' },
  { id: 'abdominal_pain', en: 'Abdominal Pain', hi: 'पेट दर्द', mr: 'पोटदुखी', icon: '🤢' },
  { id: 'vomiting', en: 'Vomiting', hi: 'उल्टी', mr: 'उलटी', icon: '🤮' },
  { id: 'diarrhea', en: 'Diarrhea', hi: 'दस्त', mr: 'जुलाब', icon: '💧' },
  { id: 'body_ache', en: 'Body Ache', hi: 'बदन दर्द', mr: 'अंगदुखी', icon: '⚡' },
  { id: 'dizziness', en: 'Dizziness', hi: 'चक्कर आना', mr: 'चक्कर', icon: '💫' },
  { id: 'cold_sore_throat', en: 'Cold / Sore Throat', hi: 'जुकाम / गले में खराश', mr: 'सर्दी / घसा खवखवणे', icon: '🤧' },
  { id: 'skin_rash', en: 'Skin Rash', hi: 'त्वचा पर चकत्ते', mr: 'त्वचेवर पुरळ', icon: '🔴' },
];

const SEVERITY_LEVELS: {
  level: SymptomSeverity;
  labelEn: string;
  labelHi: string;
  labelMr: string;
  descEn: string;
  descHi: string;
  descMr: string;
  color: string;
  bg: string;
  border: string;
}[] = [
  {
    level: 'MILD',
    labelEn: 'MILD',
    labelHi: 'हल्का',
    labelMr: 'सौम्य',
    descEn: 'Can manage daily routine, mild discomfort',
    descHi: 'रोज के काम कर पा रहे हैं, हल्का दर्द',
    descMr: 'रोजचे काम करू शकता, हलका त्रास',
    color: '#15803d',
    bg: '#dcfce7',
    border: '#86efac',
  },
  {
    level: 'MODERATE',
    labelEn: 'MODERATE',
    labelHi: 'मध्यम',
    labelMr: 'मध्यम',
    descEn: 'Interferes with work, needs doctor review',
    descHi: 'काम करने में परेशानी, डॉक्टर को दिखाना जरूरी',
    descMr: 'कामात अडथळा, डॉक्टरांचा सल्ला आवश्यक',
    color: '#b45309',
    bg: '#fef3c7',
    border: '#fde68a',
  },
  {
    level: 'SEVERE',
    labelEn: 'SEVERE',
    labelHi: 'गंभीर',
    labelMr: 'तीव्र',
    descEn: 'High fever or severe pain, visit clinic today',
    descHi: 'तेज बुखार या अधिक दर्द, आज ही अस्पताल जाएं',
    descMr: 'तीव्र ताप किंवा जास्त वेदना, आजच तपासणी करा',
    color: '#c2410c',
    bg: '#ffedd5',
    border: '#fdba74',
  },
  {
    level: 'CRITICAL',
    labelEn: 'CRITICAL',
    labelHi: 'अति-गंभीर',
    labelMr: 'अत्यंत गंभीर',
    descEn: 'Emergency signs, immediate hospital care needed',
    descHi: 'आपातकालीन लक्षण, तुरंत बड़े अस्पताल जाएं',
    descMr: 'तातडीची वैद्यकीय मदत आवश्यक',
    color: '#b91c1c',
    bg: '#fee2e2',
    border: '#fca5a5',
  },
];

export const Step2Symptoms: React.FC<Step2SymptomsProps> = ({
  formData,
  setFormData,
  onNext,
  onBack,
  isSubmitting = false,
  submitError = null,
}) => {
  const { user, selectedPatientId, selectedPatientName } = useAuth();
  const { language, t } = useLanguage();

  const [activeMethod, setActiveMethod] = useState<'form' | 'ai' | 'voice'>('form');

  // AI Chat state
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [isAiLoading, setIsAiLoading] = useState(false);

  // Voice State
  const [isListening, setIsListening] = useState(false);
  const [voiceError, setVoiceError] = useState<string | null>(null);
  const recognitionRef = useRef<any>(null);

  // Initialize Chatbot conversation
  useEffect(() => {
    if (activeMethod === 'ai' && !conversationId) {
      chatApi
        .createConversation({
          userId: selectedPatientId || user?.patientId || user?.id || 'anonymous-patient',
          username: selectedPatientName || user?.displayName || 'Patient',
          age: 40,
        })
        .then((res) => {
          setConversationId(res.conversationId);
          setMessages([
            {
              messageId: 'welcome-1',
              role: 'ASSISTANT',
              content:
                language === 'hi'
                  ? 'नमस्ते! कृपया बताएं कि आपको क्या तकलीफ़ हो रही है? (जैसे बुखार, खांसी, पेट दर्द)'
                  : language === 'mr'
                  ? 'नमस्कार! कृपया सांगा तुम्हाला काय त्रास होत आहे? (उदा. ताप, खोकला, पोटदुखी)'
                  : 'Hello! Please tell me what symptoms or discomfort you are experiencing today.',
              createdAt: new Date().toISOString(),
            },
          ]);
        })
        .catch(() => {});
    }
  }, [activeMethod, conversationId, selectedPatientId, selectedPatientName, user, language]);

  // Handle AI Chat send
  const handleSendChatMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim() || !conversationId) return;

    const userMsgText = chatInput.trim();
    setChatInput('');
    const userMsg: ChatMessageResponse = {
      messageId: `user-${Date.now()}`,
      role: 'USER',
      content: userMsgText,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setIsAiLoading(true);

    if (!formData.chiefComplaint) {
      setFormData((prev) => ({ ...prev, chiefComplaint: userMsgText }));
    }

    try {
      const reply = await chatApi.sendMessage(conversationId, {
        userId: selectedPatientId || user?.patientId || user?.id || 'anonymous-patient',
        message: userMsgText,
      });
      setMessages((prev) => [...prev, reply]);
      setFormData((prev) => ({
        ...prev,
        notes: prev.notes
          ? `${prev.notes}\nPatient: ${userMsgText}\nAI: ${reply.content}`
          : `AI Chat intake:\nPatient: ${userMsgText}\nAI: ${reply.content}`,
      }));
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          messageId: `err-${Date.now()}`,
          role: 'ASSISTANT',
          content: 'Recorded symptoms for tele-triage screening.',
          createdAt: new Date().toISOString(),
        },
      ]);
    } finally {
      setIsAiLoading(false);
    }
  };

  // Voice Input via Web Speech API
  const startVoiceInput = () => {
    setVoiceError(null);
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setVoiceError('Speech recognition is not supported in this browser. Please use the button form above.');
      return;
    }

    const recognition = new SpeechRecognition();
    recognitionRef.current = recognition;
    recognition.lang = language === 'hi' ? 'hi-IN' : language === 'mr' ? 'mr-IN' : 'en-IN';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognition.onstart = () => {
      setIsListening(true);
    };

    recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript;
      if (transcript) {
        setFormData((prev) => ({
          ...prev,
          chiefComplaint: prev.chiefComplaint ? `${prev.chiefComplaint}, ${transcript}` : transcript,
          notes: prev.notes ? `${prev.notes}\nVoice Transcript: ${transcript}` : `Voice Transcript: ${transcript}`,
        }));
      }
    };

    recognition.onerror = (event: any) => {
      setVoiceError(`Voice recognition: ${event.error || 'Permission denied'}. Please use the buttons below.`);
      setIsListening(false);
    };

    recognition.onend = () => {
      setIsListening(false);
    };

    try {
      recognition.start();
    } catch (e: any) {
      setVoiceError(e.message || 'Could not start microphone');
      setIsListening(false);
    }
  };

  const stopVoiceInput = () => {
    if (recognitionRef.current) {
      recognitionRef.current.stop();
    }
    setIsListening(false);
  };

  // Toggle symptom tile
  const toggleSymptom = (symName: string) => {
    const isSelected = formData.selectedSymptoms.includes(symName);
    if (isSelected) {
      setFormData((prev) => ({
        ...prev,
        selectedSymptoms: prev.selectedSymptoms.filter((s) => s !== symName),
      }));
    } else {
      setFormData((prev) => ({
        ...prev,
        selectedSymptoms: [...prev.selectedSymptoms, symName],
        chiefComplaint: prev.chiefComplaint || symName,
      }));
    }
  };

  // Remove symptom chip
  const removeSymptom = (symName: string) => {
    setFormData((prev) => ({
      ...prev,
      selectedSymptoms: prev.selectedSymptoms.filter((s) => s !== symName),
    }));
  };

  // Current severity normalized string
  const currentSeverity: SymptomSeverity =
    typeof formData.severity === 'string'
      ? (formData.severity as SymptomSeverity)
      : Number(formData.severity) <= 3
      ? 'MILD'
      : Number(formData.severity) <= 6
      ? 'MODERATE'
      : Number(formData.severity) <= 8
      ? 'SEVERE'
      : 'CRITICAL';

  const isFormValid =
    formData.chiefComplaint.trim().length > 0 || formData.selectedSymptoms.length > 0;

  return (
    <div className="card" style={{ padding: '1.5rem' }}>
      {/* Step Header */}
      <div className="card-header" style={{ marginBottom: '1rem' }}>
        <div className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <Activity size={24} color="var(--primary)" />
          <span style={{ fontSize: '1.25rem', fontWeight: 800 }}>{t('step2Title')}</span>
        </div>
        <span className="badge badge-routine" style={{ fontSize: '0.85rem', padding: '0.35rem 0.75rem' }}>
          Step 2 of 5
        </span>
      </div>

      <p style={{ color: 'var(--text-subtle)', marginBottom: '1.25rem', fontSize: '0.95rem' }}>
        {language === 'hi'
          ? 'रोगी के लक्षण, गंभीरता और अवधि चुनें। आप एक से अधिक लक्षण चुन सकते हैं।'
          : language === 'mr'
          ? 'रुग्णाचे लक्षणे, तीव्रता आणि कालावधी निवडा. आपण एकापेक्षा जास्त लक्षणे निवडू शकता.'
          : 'Select symptoms, severity, and duration. Multiple symptoms can be chosen.'}
      </p>

      {/* Input Method Switcher */}
      <div className="method-selector-grid" style={{ marginBottom: '1.5rem' }}>
        <div
          className={`method-card ${activeMethod === 'form' ? 'selected' : ''}`}
          onClick={() => setActiveMethod('form')}
          style={{ padding: '0.85rem', cursor: 'pointer' }}
        >
          <FileText size={28} color="var(--primary)" style={{ margin: '0 auto 0.4rem' }} />
          <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{t('methodManual')}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
            {language === 'hi' ? 'लक्षण बटन' : language === 'mr' ? 'लक्षण बटने' : 'Touch Buttons'}
          </div>
        </div>

        <div
          className={`method-card ${activeMethod === 'ai' ? 'selected' : ''}`}
          onClick={() => setActiveMethod('ai')}
          style={{ padding: '0.85rem', cursor: 'pointer' }}
        >
          <MessageSquare size={28} color="var(--secondary)" style={{ margin: '0 auto 0.4rem' }} />
          <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{t('methodAiChat')}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
            {language === 'hi' ? 'बातचीत' : language === 'mr' ? 'संभाषण' : 'AI Assistant'}
          </div>
        </div>

        <div
          className={`method-card ${activeMethod === 'voice' ? 'selected' : ''}`}
          onClick={() => setActiveMethod('voice')}
          style={{ padding: '0.85rem', cursor: 'pointer' }}
        >
          <Mic size={28} color="var(--accent-warm)" style={{ margin: '0 auto 0.4rem' }} />
          <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{t('methodVoice')}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
            {language === 'hi' ? 'बोलकर बताएं' : language === 'mr' ? 'बोलून सांगा' : 'Spoken Voice'}
          </div>
        </div>
      </div>

      {/* METHOD 1: MANUAL TOUCH FORM */}
      {activeMethod === 'form' && (
        <div>
          {/* Selected Symptoms Pill Bar */}
          {formData.selectedSymptoms.length > 0 && (
            <div
              style={{
                background: 'var(--primary-light)',
                border: '1.5px solid var(--primary-border)',
                borderRadius: 'var(--radius-md)',
                padding: '0.85rem 1rem',
                marginBottom: '1.25rem',
              }}
            >
              <div style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--primary-hover)', marginBottom: '0.5rem' }}>
                ✓ {language === 'hi' ? 'चुने गए लक्षण' : language === 'mr' ? 'निवडलेली लक्षणे' : 'Selected Symptoms'} ({formData.selectedSymptoms.length}):
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                {formData.selectedSymptoms.map((sym) => (
                  <span
                    key={sym}
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.4rem',
                      background: 'var(--bg-surface)',
                      border: '1px solid var(--primary)',
                      color: 'var(--primary-hover)',
                      fontWeight: 700,
                      padding: '0.3rem 0.7rem',
                      borderRadius: 'var(--radius-full)',
                      fontSize: '0.85rem',
                    }}
                  >
                    <span>{sym}</span>
                    <button
                      type="button"
                      onClick={() => removeSymptom(sym)}
                      style={{
                        background: 'transparent',
                        border: 'none',
                        color: 'var(--text-subtle)',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        padding: 0,
                      }}
                      title="Remove symptom"
                    >
                      <X size={14} />
                    </button>
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Touch-Friendly Symptoms Grid */}
          <div className="form-group" style={{ marginBottom: '1.5rem' }}>
            <label className="form-label" style={{ fontSize: '1rem', fontWeight: 700 }}>
              {language === 'hi'
                ? 'लक्षणों पर टैप करें (Choose Symptoms):'
                : language === 'mr'
                ? 'खालील लक्षणांवर टॅप करा (Choose Symptoms):'
                : 'Tap to Select Symptoms:'}
            </label>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                gap: '0.75rem',
                marginTop: '0.5rem',
              }}
            >
              {RURAL_SYMPTOMS.map((opt) => {
                const isSelected = formData.selectedSymptoms.includes(opt.en);
                const localLabel = language === 'hi' ? opt.hi : language === 'mr' ? opt.mr : '';

                return (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() => toggleSymptom(opt.en)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.75rem',
                      minHeight: '56px',
                      padding: '0.65rem 0.9rem',
                      borderRadius: 'var(--radius-md)',
                      border: isSelected ? '2px solid var(--primary)' : '1.5px solid var(--border-medium)',
                      background: isSelected ? 'var(--primary-light)' : 'var(--bg-surface)',
                      color: isSelected ? 'var(--primary-hover)' : 'var(--text-main)',
                      cursor: 'pointer',
                      textAlign: 'left',
                      transition: 'all 0.15s ease',
                      boxShadow: isSelected ? '0 2px 8px rgba(13, 148, 136, 0.15)' : 'none',
                    }}
                  >
                    <span style={{ fontSize: '1.4rem' }}>{opt.icon}</span>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: isSelected ? 800 : 600, fontSize: '0.95rem' }}>
                        {opt.en}
                      </div>
                      {localLabel && (
                        <div style={{ fontSize: '0.8rem', color: isSelected ? 'var(--primary)' : 'var(--text-subtle)' }}>
                          {localLabel}
                        </div>
                      )}
                    </div>
                    {isSelected && <CheckCircle2 size={20} color="var(--primary)" />}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Primary Chief Complaint Box */}
          <div className="form-group" style={{ marginBottom: '1.5rem' }}>
            <label className="form-label" htmlFor="chief-complaint" style={{ fontWeight: 700 }}>
              {language === 'hi'
                ? 'मुख्य शिकायत या अतिरिक्त परेशानी (Primary Complaint):'
                : language === 'mr'
                ? 'मुख्य तक्रार किंवा इतर त्रास (Primary Complaint):'
                : 'Primary Chief Complaint / Summary:'}
            </label>
            <input
              id="chief-complaint"
              type="text"
              className="form-control"
              style={{ fontSize: '1rem', minHeight: '48px' }}
              placeholder={
                language === 'hi'
                  ? 'उदा. 3 दिन से तेज बुखार और बदन दर्द'
                  : language === 'mr'
                  ? 'उदा. 3 दिवसांपासून तीव्र ताप आणि डोकेदुखी'
                  : 'e.g. High fever with body ache for 3 days'
              }
              value={formData.chiefComplaint}
              onChange={(e) => setFormData({ ...formData, chiefComplaint: e.target.value })}
            />
          </div>

          {/* 4-Card Severity Level Selector */}
          <div className="form-group" style={{ marginBottom: '1.5rem' }}>
            <label className="form-label" style={{ fontWeight: 700, fontSize: '1rem' }}>
              {language === 'hi'
                ? 'बीमारी की गंभीरता (Severity Level):'
                : language === 'mr'
                ? 'आजाराची तीव्रता (Severity Level):'
                : 'Symptom Severity Level:'}
            </label>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                gap: '0.75rem',
                marginTop: '0.5rem',
              }}
            >
              {SEVERITY_LEVELS.map((s) => {
                const isSelected = currentSeverity === s.level;
                const label = language === 'hi' ? `${s.labelEn} (${s.labelHi})` : language === 'mr' ? `${s.labelEn} (${s.labelMr})` : s.labelEn;
                const desc = language === 'hi' ? s.descHi : language === 'mr' ? s.descMr : s.descEn;

                return (
                  <div
                    key={s.level}
                    onClick={() => setFormData({ ...formData, severity: s.level })}
                    style={{
                      padding: '0.85rem',
                      borderRadius: 'var(--radius-md)',
                      border: `2.5px solid ${isSelected ? s.color : 'var(--border-medium)'}`,
                      background: isSelected ? s.bg : 'var(--bg-surface)',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                      <span style={{ fontWeight: 800, fontSize: '0.95rem', color: s.color }}>
                        {label}
                      </span>
                      {isSelected && <CheckCircle2 size={18} color={s.color} />}
                    </div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', lineHeight: 1.35 }}>
                      {desc}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Duration Selector */}
          <div className="form-group" style={{ marginBottom: '1.5rem' }}>
            <label className="form-label" style={{ fontWeight: 700, fontSize: '1rem' }}>
              <Clock size={16} style={{ display: 'inline', marginRight: '0.3rem', verticalAlign: 'middle' }} />
              {language === 'hi'
                ? 'कितने दिनों से है? (Duration):'
                : language === 'mr'
                ? 'किती दिवसांपासून त्रास आहे? (Duration):'
                : 'Symptom Duration:'}
            </label>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '0.4rem', marginBottom: '0.75rem' }}>
              {[
                { days: 1, label: language === 'hi' ? '1 दिन (1d)' : language === 'mr' ? '1 दिवस (1d)' : '1 Day' },
                { days: 2, label: language === 'hi' ? '2-3 दिन (2-3d)' : language === 'mr' ? '2-3 दिवस (2-3d)' : '2-3 Days' },
                { days: 5, label: language === 'hi' ? '4-7 दिन (4-7d)' : language === 'mr' ? '4-7 दिवस (4-7d)' : '4-7 Days' },
                { days: 10, label: language === 'hi' ? '> 1 हफ्ता (>1w)' : language === 'mr' ? '> 1 आठवडा (>1w)' : '> 1 Week' },
              ].map((item) => (
                <button
                  key={item.days}
                  type="button"
                  onClick={() => setFormData({ ...formData, durationDays: item.days })}
                  className="btn btn-outline"
                  style={{
                    minHeight: '42px',
                    borderColor: formData.durationDays === item.days ? 'var(--primary)' : 'var(--border-medium)',
                    background: formData.durationDays === item.days ? 'var(--primary-light)' : 'var(--bg-surface)',
                    color: formData.durationDays === item.days ? 'var(--primary-hover)' : 'var(--text-main)',
                    fontWeight: formData.durationDays === item.days ? 700 : 500,
                  }}
                >
                  {item.label}
                </button>
              ))}
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', maxWidth: '240px' }}>
              <span style={{ fontSize: '0.9rem', color: 'var(--text-subtle)' }}>Exact Days:</span>
              <input
                id="symptom-duration"
                type="number"
                min="1"
                max="365"
                className="form-control"
                style={{ minHeight: '40px' }}
                value={formData.durationDays}
                onChange={(e) => setFormData({ ...formData, durationDays: Math.max(1, Number(e.target.value)) })}
              />
            </div>
          </div>

          {/* Clinical Vitals Section */}
          <div
            style={{
              background: 'var(--bg-subtle)',
              padding: '1.25rem',
              borderRadius: 'var(--radius-md)',
              marginBottom: '1.5rem',
              border: '1px solid var(--border-light)',
            }}
          >
            <div style={{ fontWeight: 700, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Heart size={18} color="var(--primary)" />
              <span>
                {language === 'hi'
                  ? 'जांच किए गए महत्वपूर्ण लक्षण / Vitals (ऐच्छिक - ASHA / फील्ड जांच):'
                  : language === 'mr'
                  ? 'तपासलेली महत्त्वाची लक्षणे / Vitals (ऐच्छिक - ASHA तपासणी):'
                  : 'Recorded Field Vitals (Optional / Field Intake):'}
              </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '0.75rem' }}>
              <div>
                <label className="form-label" style={{ fontSize: '0.85rem' }}>
                  {t('vitalsTemp')}
                </label>
                <input
                  type="number"
                  step="0.1"
                  className="form-control"
                  placeholder="98.6"
                  value={formData.vitals.temperatureFahrenheit || ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      vitals: { ...formData.vitals, temperatureFahrenheit: e.target.value ? Number(e.target.value) : undefined },
                    })
                  }
                />
                <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>Normal: 97°F - 99°F</span>
              </div>

              <div>
                <label className="form-label" style={{ fontSize: '0.85rem' }}>
                  {t('vitalsBpSystolic')}
                </label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="120"
                  value={formData.vitals.systolicBp || ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      vitals: { ...formData.vitals, systolicBp: e.target.value ? Number(e.target.value) : undefined },
                    })
                  }
                />
                <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>Normal: 110 - 130</span>
              </div>

              <div>
                <label className="form-label" style={{ fontSize: '0.85rem' }}>
                  {t('vitalsBpDiastolic')}
                </label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="80"
                  value={formData.vitals.diastolicBp || ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      vitals: { ...formData.vitals, diastolicBp: e.target.value ? Number(e.target.value) : undefined },
                    })
                  }
                />
                <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>Normal: 70 - 85</span>
              </div>

              <div>
                <label className="form-label" style={{ fontSize: '0.85rem' }}>
                  {t('vitalsPulse')}
                </label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="72"
                  value={formData.vitals.pulseBpm || ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      vitals: { ...formData.vitals, pulseBpm: e.target.value ? Number(e.target.value) : undefined },
                    })
                  }
                />
                <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>Normal: 60 - 100</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* METHOD 2: AI CONVERSATION */}
      {activeMethod === 'ai' && (
        <div style={{ border: '1px solid var(--border-light)', borderRadius: 'var(--radius-md)', padding: '1rem', marginBottom: '1.25rem' }}>
          <div
            style={{
              height: '240px',
              overflowY: 'auto',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.75rem',
              marginBottom: '1rem',
              paddingRight: '0.5rem',
            }}
          >
            {messages.map((m) => (
              <div
                key={m.messageId}
                style={{
                  alignSelf: m.role === 'USER' ? 'flex-end' : 'flex-start',
                  maxWidth: '80%',
                  background: m.role === 'USER' ? 'var(--primary)' : 'var(--bg-subtle)',
                  color: m.role === 'USER' ? '#ffffff' : 'var(--text-main)',
                  padding: '0.65rem 0.9rem',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.9rem',
                }}
              >
                {m.content}
              </div>
            ))}
            {isAiLoading && (
              <div style={{ alignSelf: 'flex-start', color: 'var(--text-subtle)', fontSize: '0.85rem' }}>
                AI Assistant is typing...
              </div>
            )}
          </div>

          <form onSubmit={handleSendChatMessage} style={{ display: 'flex', gap: '0.5rem' }}>
            <input
              type="text"
              className="form-control"
              placeholder="Describe symptoms in Hindi, Marathi, or English..."
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
            />
            <button type="submit" className="btn btn-primary" disabled={isAiLoading || !chatInput.trim()}>
              <Send size={18} />
            </button>
          </form>
        </div>
      )}

      {/* METHOD 3: VOICE INPUT */}
      {activeMethod === 'voice' && (
        <div
          style={{
            textAlign: 'center',
            padding: '2rem 1rem',
            background: 'var(--bg-subtle)',
            borderRadius: 'var(--radius-md)',
            marginBottom: '1.25rem',
          }}
        >
          {voiceError && (
            <div className="alert-box danger" style={{ textAlign: 'left', marginBottom: '1rem' }}>
              <AlertCircle size={20} />
              <span>{voiceError}</span>
            </div>
          )}

          <div style={{ marginBottom: '1.5rem' }}>
            {isListening ? (
              <button
                type="button"
                className="btn btn-danger"
                style={{
                  width: '90px',
                  height: '90px',
                  borderRadius: 'var(--radius-full)',
                  margin: '0 auto',
                  animation: 'pulse-ring 1.5s infinite',
                }}
                onClick={stopVoiceInput}
              >
                <MicOff size={36} />
              </button>
            ) : (
              <button
                type="button"
                className="btn btn-primary"
                style={{
                  width: '90px',
                  height: '90px',
                  borderRadius: 'var(--radius-full)',
                  margin: '0 auto',
                }}
                onClick={startVoiceInput}
              >
                <Mic size={36} />
              </button>
            )}
          </div>

          <div style={{ fontWeight: 700, fontSize: '1.1rem', marginBottom: '0.5rem' }}>
            {isListening ? t('voiceListening') : t('voiceStart')}
          </div>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
            Language: {language === 'hi' ? 'हिन्दी (Hindi)' : language === 'mr' ? 'मराठी (Marathi)' : 'English (India)'}
          </div>

          {formData.chiefComplaint && (
            <div style={{ marginTop: '1.5rem', textAlign: 'left' }}>
              <label className="form-label">{t('voiceTranscript')}</label>
              <textarea
                className="form-control"
                rows={3}
                value={formData.chiefComplaint}
                onChange={(e) => setFormData({ ...formData, chiefComplaint: e.target.value })}
              />
            </div>
          )}
        </div>
      )}

      {/* Submission Error Banner */}
      {submitError && (
        <div className="alert-box danger" style={{ marginBottom: '1.25rem' }}>
          <AlertCircle size={20} />
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 700 }}>Submission Notice</div>
            <div style={{ fontSize: '0.9rem' }}>{submitError}</div>
          </div>
        </div>
      )}

      {/* Review Summary */}
      <div style={{ borderTop: '1px solid var(--border-light)', paddingTop: '1rem', marginTop: '1rem' }}>
        <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginBottom: '0.35rem' }}>
          {language === 'hi' ? 'दर्ज किए गए लक्षणों का सारांश:' : language === 'mr' ? 'नोंदवलेली लक्षणे सारांश:' : 'Recorded Symptoms Summary:'}
        </div>
        <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-main)' }}>
          {formData.selectedSymptoms.length > 0
            ? formData.selectedSymptoms.join(', ') + (formData.chiefComplaint ? ` (${formData.chiefComplaint})` : '')
            : formData.chiefComplaint || 'No symptoms selected yet. Please select at least one.'}
        </div>
      </div>

      {/* Navigation Buttons */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.5rem', gap: '1rem' }}>
        <button type="button" className="btn btn-outline" onClick={onBack} disabled={isSubmitting}>
          <span>{t('back')}</span>
        </button>

        <button
          type="button"
          className="btn btn-primary"
          style={{ minWidth: '180px', minHeight: '48px', fontSize: '1rem', fontWeight: 700 }}
          disabled={!isFormValid || isSubmitting}
          onClick={onNext}
        >
          {isSubmitting ? (
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
              <RefreshCw size={18} style={{ animation: 'spin 1s linear infinite' }} />
              <span>Saving Symptoms...</span>
            </span>
          ) : (
            <span>{t('next')} →</span>
          )}
        </button>
      </div>
    </div>
  );
};

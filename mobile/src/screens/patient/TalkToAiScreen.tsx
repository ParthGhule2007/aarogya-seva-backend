import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  Platform,
} from 'react-native';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { callingApi } from '../../api/callingApi';
import { CallResponse, VoiceLanguage } from '../../types/calling';
import { Ionicons } from '@expo/vector-icons';
import { Header } from '../../components/common/Header';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { EmergencySosButton } from '../../components/common/EmergencySosButton';
import { DemoModeBanner } from '../../components/common/DemoModeBanner';
import { IS_DEMO_MODE } from '../../config/apiConfig';

export const TalkToAiScreen: React.FC = () => {
  const { user } = useAuth();
  const { colors } = useTheme();
  const { t } = useLanguage();

  const isDummyNumber = (num?: string) => {
    if (!num) return true;
    const clean = num.replace(/[^0-9]/g, '');
    return clean.includes('9422366324') || clean.includes('1234567890') || clean.length < 10;
  };

  const registeredPhone = !isDummyNumber(user?.phoneNumber)
    ? (user?.phoneNumber || '')
    : (!isDummyNumber(user?.contactNumber) ? (user?.contactNumber || '') : '');
  const [customPhone, setCustomPhone] = useState(registeredPhone);
  const [phoneInput, setPhoneInput] = useState('');
  const [selectedLang, setSelectedLang] = useState<VoiceLanguage>('hi');
  const [chosenName, setChosenName] = useState(user?.displayName || 'Ramesh Patil');

  React.useEffect(() => {
    let isMounted = true;
    callingApi.getVerifiedRecipients().then((verifiedList) => {
      if (isMounted && verifiedList && verifiedList.length > 0) {
        setCustomPhone((prev) => {
          if (!prev || isDummyNumber(prev)) {
            return verifiedList[0];
          }
          return prev;
        });
      }
    });
    return () => { isMounted = false; };
  }, []);

  React.useEffect(() => {
    if (user?.phoneNumber && !isDummyNumber(user.phoneNumber)) {
      setCustomPhone(user.phoneNumber);
    } else if (user?.contactNumber && !isDummyNumber(user.contactNumber)) {
      setCustomPhone(user.contactNumber);
    }
  }, [user?.phoneNumber, user?.contactNumber]);
  const [isCalling, setIsCalling] = useState(false);
  const [isFollowUpCalling, setIsFollowUpCalling] = useState(false);
  const [activeCallSession, setActiveCallSession] = useState<CallResponse | null>(null);
  const [readableStatusText, setReadableStatusText] = useState<string | null>(null);
  const [callError, setCallError] = useState<string | null>(null);
  const [displayPhone, setDisplayPhone] = useState('+91 80103 81892');
  const [usedDemoData, setUsedDemoData] = useState(false);

  const addLog = (_msg: string) => {};

  const pollingRef = React.useRef<any>(null);

  const stopPolling = () => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  };

  React.useEffect(() => {
    return () => stopPolling();
  }, []);

  const getLanguageLabel = (lang?: string) => {
    if (lang === 'hi') return 'Hindi';
    if (lang === 'mr') return 'Marathi';
    return 'English';
  };

  const startStatusPolling = (sessionId: string) => {
    stopPolling();
    let tickCount = 0;
    const maxTicks = 30; // 30 * 3s = 90s

    pollingRef.current = setInterval(async () => {
      tickCount++;
      if (tickCount > maxTicks) {
        stopPolling();
        setReadableStatusText('Call timed out after 90 seconds without connection.');
        return;
      }

      try {
        const detail = await callingApi.getCallStatusDetail(sessionId);
        if (detail) {
          setActiveCallSession((prev) =>
            prev ? { ...prev, status: detail.status, message: detail.readableStatus } : null
          );
          setReadableStatusText(detail.readableStatus);
          addLog(`Status: ${detail.status} (${detail.readableStatus || ''})${detail.errorCode ? ' [Twilio Code ' + detail.errorCode + ']' : ''}`);

          if (
            !detail.active ||
            detail.status === 'COMPLETED' ||
            detail.status === 'FAILED' ||
            detail.status === 'BUSY' ||
            detail.status === 'NO_ANSWER' ||
            detail.status === 'CANCELLED'
          ) {
            stopPolling();
            if (detail.status !== 'COMPLETED' && detail.status !== 'IN_PROGRESS') {
              const termError = detail.errorMessage || detail.readableStatus || `Call ended (${detail.status})`;
              setCallError(termError);
              addLog(`Ended: ${termError}`);
            }
          }
        }
      } catch (err: any) {
        addLog(`Polling check: ${err?.message || 'waiting...'}`);
      }
    }, 3000);
  };

  const handleStartCall = async () => {
    const targetPhone = customPhone || '+918010381892';

    setIsCalling(true);
    setCallError(null);
    setUsedDemoData(false);
    stopPolling();
    addLog(`Calling AI doctor on line ...${targetPhone.slice(-4)}`);

    try {
      const session = await callingApi.talkToAi({
        phoneNumber: targetPhone,
        language: selectedLang,
        chosenName: chosenName.trim(),
        consentToSaveSummary: true,
      });

      setActiveCallSession(session);
      const sId = session.sessionId || (session as any).callSessionId || '';
      addLog(`Session ${sId ? sId.slice(0, 8) : 'active'}: ${session.status}${session.message ? ' (' + session.message + ')' : ''}`);

      if (session.status === 'FAILED') {
        const failureReason = session.message || 'Call failed to initiate on telephony carrier.';
        setCallError(failureReason);
        addLog(`Initiation Failed: ${failureReason}`);
      } else {
        setReadableStatusText('Calling...');
        if (sId) {
          startStatusPolling(sId);
        }
      }
    } catch (err: any) {
      console.error('TalkToAi call error:', err);
      const errMsg = err?.message || 'Failed to initiate AI voice call.';
      const fullErr = `Voice request failed (Status ${err?.status || 500}): ${errMsg}`;
      setCallError(fullErr);
      addLog(`Error: ${fullErr}`);
      if (IS_DEMO_MODE) {
        setUsedDemoData(true);
        const mockSession: CallResponse = {
          sessionId: `call-${Date.now()}`,
          phoneNumber: targetPhone,
          callType: 'TALK_TO_AI',
          language: selectedLang,
          status: 'RINGING',
          message: 'DEMO DATA: Outbound session connected to clinical AI agent.',
          startedAt: new Date().toISOString(),
        };
        setActiveCallSession(mockSession);
        setReadableStatusText('Ringing');
      } else {
        setActiveCallSession(null);
        Alert.alert('Calling Service Error', errMsg);
      }
    } finally {
      setIsCalling(false);
    }
  };

  const handleFollowUpCall = async () => {
    const targetPhone = customPhone || '+918010381892';

    setIsFollowUpCalling(true);
    setCallError(null);
    setUsedDemoData(false);
    stopPolling();
    addLog(`Initiating follow-up call on line ...${targetPhone.slice(-4)}`);

    try {
      const session = await callingApi.followUpCall({
        phoneNumber: targetPhone,
        language: selectedLang,
      });

      setActiveCallSession(session);
      const sId = session.sessionId || (session as any).callSessionId || '';
      addLog(`FollowUp Session: ${session.status}${session.message ? ' (' + session.message + ')' : ''}`);

      if (session.status === 'FAILED') {
        const failureReason = session.message || 'Follow-up call failed to initiate.';
        setCallError(failureReason);
        addLog(`Initiation Failed: ${failureReason}`);
      } else {
        setReadableStatusText('Calling...');
        if (sId) {
          startStatusPolling(sId);
        }
      }
    } catch (err: any) {
      console.error('FollowUp call error:', err);
      const errMsg = err?.message || 'Failed to initiate follow-up call.';
      const fullErr = `Follow-up request failed (Status ${err?.status || 500}): ${errMsg}`;
      setCallError(fullErr);
      addLog(`Error: ${fullErr}`);
      if (IS_DEMO_MODE) {
        setUsedDemoData(true);
        setActiveCallSession({
          sessionId: `fu-${Date.now()}`,
          phoneNumber: targetPhone,
          callType: 'FOLLOW_UP',
          language: selectedLang,
          status: 'IN_PROGRESS',
          message: 'DEMO DATA: Patient-requested follow-up triage call in progress.',
          startedAt: new Date().toISOString(),
        });
        setReadableStatusText('Connected');
      } else {
        setActiveCallSession(null);
        Alert.alert('Follow-Up Call Error', errMsg);
      }
    } finally {
      setIsFollowUpCalling(false);
    }
  };

  return (
    <View style={{ flex: 1, backgroundColor: colors.bgApp }}>
      <Header />

      <ScrollView contentContainerStyle={styles.container}>
        <DemoModeBanner visible={usedDemoData} />

        <View style={styles.banner}>
          <Ionicons name="medical" size={36} color={colors.primary} style={{ marginBottom: 6 }} />
          <Text style={[styles.bannerTitle, { color: colors.textMain }]}>
            {t('aarogyaMitraTitle')}
          </Text>
          <Text style={[styles.bannerSub, { color: colors.textSubtle }]}>
            AI-powered medical triaging phone call in Hindi, Marathi, and English.
          </Text>
        </View>

        {callError && (
          <Card style={[styles.statusCard, { borderColor: '#FCA5A5', backgroundColor: '#FEF2F2' }]}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
              <Ionicons name="alert-circle-outline" size={18} color="#DC2626" />
              <Text style={[styles.statusTitle, { color: '#DC2626' }]}>Voice Service Status</Text>
            </View>
            <Text style={[styles.statusMsg, { color: '#991B1B', marginTop: 4 }]}>{callError}</Text>
          </Card>
        )}

        {/* Active Call Status Banner */}
        {activeCallSession && (
          <Card style={[styles.statusCard, { borderColor: colors.primaryBorder, backgroundColor: colors.primaryLight }]}>
            <View style={styles.statusRow}>
              <View>
                <Text style={[styles.statusTitle, { color: colors.primary }]}>Active Voice Session</Text>
                <Text style={[styles.statusSub, { color: colors.textMain }]}>
                  Verified line • {getLanguageLabel(activeCallSession.language)}
                </Text>
              </View>
              <StatusBadge status={activeCallSession.status} />
            </View>
            <Text style={[styles.statusMsg, { color: colors.textSubtle, marginTop: 4 }]}>
              {readableStatusText || activeCallSession.message || 'Call initiated'}
            </Text>
          </Card>
        )}

        {/* Main Initiation Card */}
        <Card style={styles.mainCard}>
          <Text style={[styles.cardTitle, { color: colors.textMain }]}>Start Outbound Phone Call</Text>
          <Text style={[styles.cardSub, { color: colors.textSubtle }]}>
            Our AI doctor will call your phone and conduct a full conversational triage.
          </Text>

          {/* Recipient Mobile / Phone Number (Display Input) */}
          <Text style={[styles.label, { color: colors.textMuted }]}>Mobile / Phone Number</Text>
          <View
            style={{
              flexDirection: 'row',
              alignItems: 'center',
              backgroundColor: colors.bgSubtle,
              borderColor: colors.borderLight,
              borderWidth: 1,
              borderRadius: 10,
              paddingHorizontal: 14,
              height: 48,
              marginBottom: 8,
            }}
          >
            <Ionicons name="call-outline" size={18} color={colors.textSubtle} style={{ marginRight: 10 }} />
            <TextInput
              style={{
                flex: 1,
                fontSize: 15,
                color: colors.textMain,
                height: '100%',
              }}
              placeholder="+91 98765 43210"
              placeholderTextColor={colors.textSubtle}
              keyboardType="phone-pad"
              value={displayPhone}
              onChangeText={setDisplayPhone}
            />
          </View>

          {/* Patient Name */}
          <Text style={[styles.label, { color: colors.textMuted }]}>{t('fullName')}</Text>
          <TextInput
            style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
            placeholder="Patient name"
            placeholderTextColor={colors.textSubtle}
            value={chosenName}
            onChangeText={setChosenName}
          />

          {/* Language Selector */}
          <Text style={[styles.label, { color: colors.textMuted, marginTop: 12 }]}>
            Spoken Voice Language:
          </Text>
          <View style={styles.langGrid}>
            {[
              { code: 'hi' as VoiceLanguage, label: 'Hindi' },
              { code: 'mr' as VoiceLanguage, label: 'Marathi' },
              { code: 'en' as VoiceLanguage, label: 'English' },
            ].map((item) => (
              <TouchableOpacity
                key={item.code}
                style={[
                  styles.langBtn,
                  {
                    backgroundColor: selectedLang === item.code ? colors.primaryLight : colors.bgSubtle,
                    borderColor: selectedLang === item.code ? colors.primary : colors.borderLight,
                  },
                ]}
                onPress={() => setSelectedLang(item.code)}
              >
                <Text style={[styles.langText, { color: selectedLang === item.code ? colors.primary : colors.textMain }]}>
                  {item.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          {/* Button 1: Talk to AI */}
          <Button
            title={isCalling ? 'Calling...' : 'Talk to AI doctor'}
            onPress={handleStartCall}
            loading={isCalling}
            variant="primary"
            style={styles.callBtn}
          />

          {/* Button 2: Follow-Up Call */}
          <Button
            title={isFollowUpCalling ? 'Scheduling...' : 'Request follow-up check-in'}
            onPress={handleFollowUpCall}
            loading={isFollowUpCalling}
            variant="outline"
            style={styles.followUpBtn}
          />
        </Card>
      </ScrollView>

      <EmergencySosButton />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
    paddingBottom: 120,
  },
  banner: {
    alignItems: 'center',
    marginBottom: 20,
    marginTop: 8,
  },
  bannerIcon: {
    fontSize: 40,
    marginBottom: 8,
  },
  bannerTitle: {
    fontSize: 22,
    fontWeight: '800',
  },
  bannerSub: {
    fontSize: 13,
    textAlign: 'center',
    marginTop: 4,
    maxWidth: 280,
  },
  statusCard: {
    padding: 14,
    marginBottom: 16,
  },
  statusRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  statusTitle: {
    fontSize: 14,
    fontWeight: '800',
  },
  statusSub: {
    fontSize: 12,
    marginTop: 2,
  },
  statusMsg: {
    fontSize: 12,
  },
  mainCard: {
    padding: 18,
    borderRadius: 16,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '800',
  },
  cardSub: {
    fontSize: 13,
    marginTop: 2,
    marginBottom: 16,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 6,
    marginTop: 8,
  },
  input: {
    height: 48,
    borderRadius: 10,
    borderWidth: 1,
    paddingHorizontal: 14,
    fontSize: 15,
  },
  langGrid: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 16,
  },
  langBtn: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 10,
    borderWidth: 1,
    alignItems: 'center',
  },
  langText: {
    fontSize: 12,
    fontWeight: '700',
  },
  callBtn: {
    marginTop: 8,
  },
  followUpBtn: {
    marginTop: 10,
  },
});

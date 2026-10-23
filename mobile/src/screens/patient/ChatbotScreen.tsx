import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { chatApi } from '../../api/chatApi';
import { ChatMessageResponse } from '../../types/chat';
import { Header } from '../../components/common/Header';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { EmergencySosButton } from '../../components/common/EmergencySosButton';

type ChatLang = 'en' | 'hi' | 'mr';

const QUICK_PROMPTS: Record<ChatLang, string[]> = {
  en: [
    'I have high fever and chills for 3 days',
    'Severe headache and dizziness since morning',
    'Stomach pain and nausea after meals',
  ],
  hi: [
    'मुझे 3 दिनों से तेज बुखार और ठंड लग रही है',
    'सुबह से तेज सिरदर्द और चक्कर आ रहे हैं',
    'खाना खाने के बाद पेट दर्द और मतली हो रही है',
  ],
  mr: [
    'मला ३ दिवसांपासून तीव्र ताप आणि थंडी वाजत आहे',
    'सकाळपासून तीव्र डोकेदुखी आणि चक्कर येत आहे',
    'जेवणानंतर पोटदुखी आणि मळमळ होत आहे',
  ],
};

export const ChatbotScreen: React.FC = () => {
  const { user, selectedPatientId } = useAuth();
  const { colors } = useTheme();
  const scrollRef = useRef<any>(null);

  const [conversationId, setConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [inputText, setInputText] = useState('');
  const [chatLang, setChatLang] = useState<ChatLang>('en');
  const [initializing, setInitializing] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const userId =
    selectedPatientId ||
    user?.patientId ||
    user?.associatedEntityId ||
    user?.id ||
    'b1000000-0000-0000-0000-000000000001';
  const username = user?.displayName || user?.fullName || user?.username || 'Ramesh Patil';
  const patientAge = user?.age || 30;

  const startNewConversation = useCallback(async () => {
    setInitializing(true);
    setError(null);
    try {
      const conv = await chatApi.createConversation({
        userId,
        username,
        age: patientAge,
      });
      setConversationId(conv.conversationId);
      const history = await chatApi.getMessages(conv.conversationId);
      setMessages(Array.isArray(history) ? history : []);
    } catch (err: any) {
      console.error('Failed to start chat conversation:', err);
      setError(
        `POST /api/v1/chat/conversations failed (${err?.status || 'Network'}): ${
          err?.message || 'Unable to reach Chatbot Service on port 8085.'
        }`
      );
    } finally {
      setInitializing(false);
    }
  }, [userId, username, patientAge]);

  useEffect(() => {
    startNewConversation();
  }, [startNewConversation]);

  const handleSendMessage = async (customText?: string) => {
    const rawText = (customText ?? inputText).trim();
    if (!rawText || sending) return;

    let activeConvId = conversationId;
    if (!activeConvId) {
      setInitializing(true);
      try {
        const conv = await chatApi.createConversation({
          userId,
          username,
          age: patientAge,
        });
        activeConvId = conv.conversationId;
        setConversationId(activeConvId);
      } catch (err: any) {
        setInitializing(false);
        const msg = err?.message || 'Failed to create conversation.';
        setError(msg);
        Alert.alert('Chatbot Error', msg);
        return;
      }
      setInitializing(false);
    }

    if (!customText) {
      setInputText('');
    }
    setSending(true);
    setError(null);

    // Optimistically display user message while waiting for AI response
    const tempUserMsg: ChatMessageResponse = {
      messageId: `temp-${Date.now()}`,
      role: 'USER',
      content: rawText,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, tempUserMsg]);

    // Add language hint if Hindi or Marathi is selected
    const langInstruction =
      chatLang === 'hi'
        ? ' (Please respond in Hindi / हिन्दी में उत्तर दें)'
        : chatLang === 'mr'
        ? ' (Please respond in Marathi / मराठीत उत्तर द्या)'
        : '';

    try {
      await chatApi.sendMessage(activeConvId, {
        userId,
        message: `${rawText}${langInstruction}`,
      });
      // Fetch full updated multi-turn conversation history from backend
      const updated = await chatApi.getMessages(activeConvId);
      if (Array.isArray(updated)) {
        setMessages(updated);
      }
      setTimeout(() => scrollRef.current?.scrollToEnd({ animated: true }), 150);
    } catch (err: any) {
      console.error('Failed to send chat message:', err);
      const errMsg = `POST /api/v1/chat/conversations/${activeConvId}/messages failed (${
        err?.status || 500
      }): ${err?.message || 'Unable to get AI response.'}`;
      setError(errMsg);
      Alert.alert('AI Chatbot Error', err?.message || 'Unable to get AI response.');
    } finally {
      setSending(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1, backgroundColor: colors.bgApp }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <Header />

      <View style={styles.container}>
        {/* Top Header & Controls */}
        <View style={[styles.topBar, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight }]}>
          <View style={{ flex: 1, paddingRight: 8 }}>
            <Text style={[styles.title, { color: colors.textMain }]}>
              Aarogya Clinical AI Chat
            </Text>
            <Text style={[styles.subtitle, { color: colors.textSubtle }]}>
              {conversationId
                ? `Session: ${conversationId.slice(0, 8)}... • Live HF Clinical AI`
                : 'Connecting to Chatbot Service (8085)...'}
            </Text>
          </View>

          <TouchableOpacity
            style={[styles.newChatBtn, { backgroundColor: colors.primaryLight, borderColor: colors.primary }]}
            onPress={startNewConversation}
            disabled={initializing || sending}
          >
            <Text style={[styles.newChatText, { color: colors.primary }]}>↻ New Chat</Text>
          </TouchableOpacity>
        </View>

        {/* Multilingual Selector */}
        <View style={styles.langRow}>
          {[
            { code: 'en' as ChatLang, label: 'English' },
            { code: 'hi' as ChatLang, label: 'हिन्दी (Hindi)' },
            { code: 'mr' as ChatLang, label: 'मराठी (Marathi)' },
          ].map((l) => (
            <TouchableOpacity
              key={l.code}
              style={[
                styles.langChip,
                {
                  backgroundColor: chatLang === l.code ? colors.primary : colors.bgSurface,
                  borderColor: chatLang === l.code ? colors.primary : colors.borderLight,
                },
              ]}
              onPress={() => setChatLang(l.code)}
            >
              <Text
                style={{
                  fontSize: 12,
                  fontWeight: '700',
                  color: chatLang === l.code ? '#FFFFFF' : colors.textMain,
                }}
              >
                {l.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Error Banner */}
        {error && (
          <Card style={[styles.errorCard, { backgroundColor: '#FEF2F2', borderColor: '#FCA5A5' }]}>
            <Text style={{ color: '#DC2626', fontWeight: '700', fontSize: 12 }}>{error}</Text>
            <Button
              title="Retry Connection"
              onPress={startNewConversation}
              variant="outline"
              style={{ marginTop: 8, minHeight: 34, paddingVertical: 4 }}
            />
          </Card>
        )}

        {/* Messages Area */}
        <ScrollView
          ref={scrollRef}
          style={styles.messagesList}
          contentContainerStyle={{ paddingBottom: 20 }}
          onContentSizeChange={() => scrollRef.current?.scrollToEnd({ animated: true })}
        >
          {initializing ? (
            <View style={styles.centerBox}>
              <ActivityIndicator size="large" color={colors.primary} />
              <Text style={[styles.loadingText, { color: colors.textSubtle }]}>
                Initializing clinical AI session...
              </Text>
            </View>
          ) : messages.length === 0 ? (
            <Card style={[styles.welcomeCard, { backgroundColor: colors.bgSurface }]}>
              <Text style={[styles.welcomeTitle, { color: colors.primary }]}>
                Namaste {username}!
              </Text>
              <Text style={[styles.welcomeBody, { color: colors.textMain }]}>
                Describe your symptoms, duration, and severity. Our Clinical AI Assistant will ask follow-up questions and provide triage guidance.
              </Text>
              <Text style={[styles.promptHeading, { color: colors.textSubtle }]}>
                Tap a sample symptom to start:
              </Text>
              {QUICK_PROMPTS[chatLang].map((prompt, idx) => (
                <TouchableOpacity
                  key={idx}
                  style={[styles.promptBtn, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight }]}
                  onPress={() => handleSendMessage(prompt)}
                >
                  <Text style={{ color: colors.primary, fontSize: 13, fontWeight: '600' }}>
                    "{prompt}"
                  </Text>
                </TouchableOpacity>
              ))}
            </Card>
          ) : (
            messages.map((msg) => {
              const isUser = msg.role === 'USER';
              return (
                <View
                  key={msg.messageId}
                  style={[
                    styles.bubbleWrapper,
                    isUser ? styles.bubbleRight : styles.bubbleLeft,
                  ]}
                >
                  <View
                    style={[
                      styles.bubble,
                      isUser
                        ? { backgroundColor: colors.primary }
                        : {
                            backgroundColor: colors.bgSurface,
                            borderColor: colors.borderLight,
                            borderWidth: 1,
                          },
                    ]}
                  >
                    <Text
                      style={[
                        styles.roleLabel,
                        { color: isUser ? '#E0F2FE' : colors.primary },
                      ]}
                    >
                      {isUser ? 'You' : '🩺 Aarogya Clinical AI'}
                    </Text>
                    <Text
                      style={[
                        styles.msgText,
                        { color: isUser ? '#FFFFFF' : colors.textMain },
                      ]}
                    >
                      {msg.content}
                    </Text>
                  </View>
                </View>
              );
            })
          )}

          {sending && (
            <View style={[styles.bubbleWrapper, styles.bubbleLeft]}>
              <View
                style={[
                  styles.bubble,
                  {
                    backgroundColor: colors.bgSurface,
                    borderColor: colors.borderLight,
                    borderWidth: 1,
                    flexDirection: 'row',
                    alignItems: 'center',
                    gap: 8,
                  },
                ]}
              >
                <ActivityIndicator size="small" color={colors.primary} />
                <Text style={{ color: colors.textSubtle, fontSize: 13 }}>
                  Aarogya AI is analyzing your symptoms...
                </Text>
              </View>
            </View>
          )}
        </ScrollView>

        {/* Input Composer */}
        <View style={[styles.composerBar, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight }]}>
          <TextInput
            style={[styles.textInput, { backgroundColor: colors.bgSubtle, color: colors.textMain, borderColor: colors.borderLight }]}
            placeholder={
              chatLang === 'hi'
                ? 'अपने लक्षण यहाँ लिखें...'
                : chatLang === 'mr'
                ? 'तुमची लक्षणे येथे लिहा...'
                : 'Describe your symptoms...'
            }
            placeholderTextColor={colors.textSubtle}
            value={inputText}
            onChangeText={setInputText}
            multiline
          />
          <TouchableOpacity
            style={[
              styles.sendBtn,
              {
                backgroundColor:
                  !inputText.trim() || sending ? colors.borderLight : colors.primary,
              },
            ]}
            onPress={() => handleSendMessage()}
            disabled={!inputText.trim() || sending}
          >
            <Text style={styles.sendBtnText}>{sending ? '...' : 'Send'}</Text>
          </TouchableOpacity>
        </View>
      </View>

      <EmergencySosButton />
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: 12,
    paddingTop: 10,
    paddingBottom: 8,
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
    marginBottom: 8,
  },
  title: {
    fontSize: 16,
    fontWeight: '800',
  },
  subtitle: {
    fontSize: 11,
    marginTop: 2,
  },
  ageRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  ageLabel: {
    fontSize: 12,
    fontWeight: '600',
  },
  ageInput: {
    width: 42,
    height: 32,
    borderWidth: 1,
    borderRadius: 6,
    textAlign: 'center',
    fontSize: 13,
    fontWeight: '700',
  },
  newChatBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
    borderWidth: 1,
  },
  newChatText: {
    fontSize: 12,
    fontWeight: '700',
  },
  langRow: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 8,
  },
  langChip: {
    flex: 1,
    paddingVertical: 7,
    borderRadius: 8,
    borderWidth: 1,
    alignItems: 'center',
  },
  errorCard: {
    padding: 10,
    marginBottom: 8,
  },
  messagesList: {
    flex: 1,
  },
  centerBox: {
    paddingVertical: 40,
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 10,
    fontSize: 13,
  },
  welcomeCard: {
    padding: 16,
    marginTop: 8,
  },
  welcomeTitle: {
    fontSize: 17,
    fontWeight: '800',
    marginBottom: 6,
  },
  welcomeBody: {
    fontSize: 13,
    lineHeight: 19,
    marginBottom: 12,
  },
  promptHeading: {
    fontSize: 12,
    fontWeight: '700',
    marginBottom: 8,
    textTransform: 'uppercase',
  },
  promptBtn: {
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    marginBottom: 8,
  },
  bubbleWrapper: {
    marginVertical: 5,
    flexDirection: 'row',
  },
  bubbleRight: {
    justifyContent: 'flex-end',
  },
  bubbleLeft: {
    justifyContent: 'flex-start',
  },
  bubble: {
    maxWidth: '85%',
    padding: 12,
    borderRadius: 14,
  },
  roleLabel: {
    fontSize: 11,
    fontWeight: '800',
    marginBottom: 4,
  },
  msgText: {
    fontSize: 14,
    lineHeight: 20,
  },
  composerBar: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 8,
    borderRadius: 12,
    borderWidth: 1,
    gap: 8,
    marginTop: 4,
  },
  textInput: {
    flex: 1,
    minHeight: 42,
    maxHeight: 90,
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontSize: 14,
  },
  sendBtn: {
    paddingHorizontal: 14,
    paddingVertical: 11,
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sendBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 13,
  },
});

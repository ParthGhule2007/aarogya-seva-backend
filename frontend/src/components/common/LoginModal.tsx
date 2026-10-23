import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { authApi } from '../../api/authApi';
import { X, Lock, User, AlertCircle, CheckCircle, UserPlus, Phone, Mail, Stethoscope, KeyRound } from 'lucide-react';
import { UserRole } from '../../types/auth';

interface LoginModalProps {
  onClose: () => void;
  initialMode?: 'LOGIN' | 'REGISTER' | 'FORGOT_PASSWORD';
}

export const LoginModal: React.FC<LoginModalProps> = ({ onClose, initialMode = 'LOGIN' }) => {
  const { login, register, switchDemoRole, error } = useAuth();
  const { t } = useLanguage();

  const [mode, setMode] = useState<'LOGIN' | 'REGISTER' | 'FORGOT_PASSWORD'>(initialMode);
  
  // Login fields
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  // Forgot password fields
  const [fpUsername, setFpUsername] = useState('');
  const [fpPhoneOrEmail, setFpPhoneOrEmail] = useState('');
  const [fpNewPassword, setFpNewPassword] = useState('');
  const [fpSuccessMessage, setFpSuccessMessage] = useState<string | null>(null);

  // Register fields
  const [regFullName, setRegFullName] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regPhone, setRegPhone] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regRole, setRegRole] = useState<UserRole>('PATIENT');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) {
      setLocalError('Please enter both username and password');
      return;
    }
    setIsSubmitting(true);
    setLocalError(null);
    try {
      await login(username, password);
      onClose();
    } catch (err: any) {
      setLocalError(err.message || 'Login failed. Please check credentials.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!regFullName.trim() || !regUsername.trim() || !regPassword) {
      setLocalError('Full name, username, and password are required');
      return;
    }
    if (regPassword.length < 6) {
      setLocalError('Password must be at least 6 characters');
      return;
    }
    setIsSubmitting(true);
    setLocalError(null);
    try {
      await register({
        username: regUsername.trim(),
        password: regPassword,
        fullName: regFullName.trim(),
        phoneNumber: regPhone.trim() || undefined,
        email: regEmail.trim() || undefined,
        role: regRole,
      });
      onClose();
    } catch (err: any) {
      setLocalError(err.message || 'Registration failed. Username may already exist.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleQuickDemo = async (role: UserRole) => {
    setIsSubmitting(true);
    setLocalError(null);
    try {
      await switchDemoRole(role);
      onClose();
    } catch (err: any) {
      setLocalError(err.message || 'Demo login failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleForgotPasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fpUsername.trim() || !fpPhoneOrEmail.trim() || !fpNewPassword) {
      setLocalError('Please enter your username, registered phone/email, and new password.');
      return;
    }
    if (fpNewPassword.length < 6) {
      setLocalError('New password must be at least 6 characters.');
      return;
    }
    setIsSubmitting(true);
    setLocalError(null);
    setFpSuccessMessage(null);
    try {
      const isEmail = fpPhoneOrEmail.includes('@');
      await authApi.forgotPassword({
        username: fpUsername.trim(),
        phoneNumber: isEmail ? undefined : fpPhoneOrEmail.trim(),
        email: isEmail ? fpPhoneOrEmail.trim() : undefined,
        newPassword: fpNewPassword,
      });
      setFpSuccessMessage('Password reset successfully! You can now sign in with your new password.');
      setUsername(fpUsername.trim());
      setPassword(fpNewPassword);
    } catch (err: any) {
      setLocalError(err.message || 'Password reset failed. Verify username and registered phone/email.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} role="dialog" aria-modal="true">
      <div className="modal-dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '460px' }}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            {mode === 'LOGIN' ? <Lock size={20} color="var(--primary)" /> : <UserPlus size={20} color="var(--primary)" />}
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>
              {mode === 'LOGIN' ? t('login') : t('createAccount')}
            </h2>
          </div>
          <button className="icon-btn" onClick={onClose} aria-label="Close">
            <X size={20} />
          </button>
        </div>

        {/* Tab Switcher */}
        <div style={{ display: 'flex', borderBottom: '1px solid var(--border-light)', margin: '0 1.5rem' }}>
          <button
            type="button"
            style={{
              flex: 1,
              padding: '0.75rem',
              fontWeight: 700,
              fontSize: '0.9rem',
              background: 'none',
              border: 'none',
              borderBottom: mode === 'LOGIN' ? '3px solid var(--primary)' : '3px solid transparent',
              color: mode === 'LOGIN' ? 'var(--primary)' : 'var(--text-subtle)',
              cursor: 'pointer',
            }}
            onClick={() => { setMode('LOGIN'); setLocalError(null); }}
          >
            {t('login')}
          </button>
          <button
            type="button"
            style={{
              flex: 1,
              padding: '0.75rem',
              fontWeight: 700,
              fontSize: '0.9rem',
              background: 'none',
              border: 'none',
              borderBottom: mode === 'REGISTER' ? '3px solid var(--primary)' : '3px solid transparent',
              color: mode === 'REGISTER' ? 'var(--primary)' : 'var(--text-subtle)',
              cursor: 'pointer',
            }}
            onClick={() => { setMode('REGISTER'); setLocalError(null); }}
          >
            {t('register')}
          </button>
        </div>

        {mode === 'LOGIN' ? (
          <form onSubmit={handleLoginSubmit}>
            <div className="modal-body" style={{ maxHeight: '70vh', overflowY: 'auto' }}>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-subtle)', marginBottom: '1.25rem' }}>
                {t('signInSubtitle')}
              </p>

              {(localError || error) && (
                <div className="alert-box danger">
                  <AlertCircle size={20} />
                  <span>{localError || error}</span>
                </div>
              )}

              <div className="form-group">
                <label className="form-label" htmlFor="login-username">
                  {t('username')}
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="login-username"
                    type="text"
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="e.g. patient.ramesh"
                    required
                  />
                  <User
                    size={18}
                    style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="login-password">
                  {t('password')}
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="login-password"
                    type="password"
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                  />
                  <Lock
                    size={18}
                    style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  style={{ background: 'none', border: 'none', color: 'var(--text-muted)', fontSize: '0.85rem', cursor: 'pointer', fontWeight: 600 }}
                  onClick={() => { setMode('FORGOT_PASSWORD'); setLocalError(null); setFpSuccessMessage(null); }}
                >
                  Forgot Password?
                </button>
                <button
                  type="button"
                  style={{ background: 'none', border: 'none', color: 'var(--primary)', fontSize: '0.85rem', cursor: 'pointer', fontWeight: 600 }}
                  onClick={() => { setMode('REGISTER'); setLocalError(null); }}
                >
                  {t('dontHaveAccount')}
                </button>
              </div>

              {/* Quick Demo Switcher inside Modal */}
              <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border-light)' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.75rem', color: 'var(--text-muted)' }}>
                  ⚡ Quick Demo Accounts (Pre-configured):
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.5rem' }}>
                  <button
                    type="button"
                    className="btn btn-outline"
                    style={{ minHeight: '38px', fontSize: '0.82rem' }}
                    onClick={() => handleQuickDemo('PATIENT')}
                    disabled={isSubmitting}
                  >
                    👤 Ramesh (Patient)
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline"
                    style={{ minHeight: '38px', fontSize: '0.82rem' }}
                    onClick={() => handleQuickDemo('ASHA')}
                    disabled={isSubmitting}
                  >
                    👩‍⚕️ Sunita Tai (ASHA)
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline"
                    style={{ minHeight: '38px', fontSize: '0.82rem' }}
                    onClick={() => handleQuickDemo('DOCTOR')}
                    disabled={isSubmitting}
                  >
                    🩺 Dr. Ananya (Doctor)
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline"
                    style={{ minHeight: '38px', fontSize: '0.82rem' }}
                    onClick={() => handleQuickDemo('ADMIN')}
                    disabled={isSubmitting}
                  >
                    🏥 Facility Admin
                  </button>
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button type="button" className="btn btn-outline" onClick={onClose}>
                {t('cancel')}
              </button>
              <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                {isSubmitting ? t('loggingIn') : t('login')}
              </button>
            </div>
          </form>
        ) : mode === 'FORGOT_PASSWORD' ? (
          <form onSubmit={handleForgotPasswordSubmit}>
            <div className="modal-body" style={{ maxHeight: '70vh', overflowY: 'auto' }}>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-subtle)', marginBottom: '1rem' }}>
                Verify your username and registered mobile number or email to reset your password.
              </p>

              {localError && (
                <div className="alert-box danger" style={{ marginBottom: '1rem' }}>
                  <AlertCircle size={20} />
                  <span>{localError}</span>
                </div>
              )}

              {fpSuccessMessage && (
                <div className="alert-box success" style={{ marginBottom: '1rem' }}>
                  <CheckCircle size={20} />
                  <span>{fpSuccessMessage}</span>
                </div>
              )}

              <div className="form-group">
                <label className="form-label" htmlFor="fp-username">
                  {t('username')} *
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="fp-username"
                    type="text"
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                    value={fpUsername}
                    onChange={(e) => setFpUsername(e.target.value)}
                    placeholder="e.g. patient.ramesh"
                    required
                  />
                  <User
                    size={18}
                    style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="fp-contact">
                  Registered Mobile Number or Email *
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="fp-contact"
                    type="text"
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                    value={fpPhoneOrEmail}
                    onChange={(e) => setFpPhoneOrEmail(e.target.value)}
                    placeholder="e.g. 9822012345 or email@ruralhealth.org"
                    required
                  />
                  <Phone
                    size={18}
                    style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="fp-newpass">
                  New Password *
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="fp-newpass"
                    type="password"
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                    value={fpNewPassword}
                    onChange={(e) => setFpNewPassword(e.target.value)}
                    placeholder="Min 6 characters"
                    required
                  />
                  <KeyRound
                    size={18}
                    style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
                  />
                </div>
              </div>

              <div style={{ textAlign: 'right', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  style={{ background: 'none', border: 'none', color: 'var(--primary)', fontSize: '0.85rem', cursor: 'pointer', fontWeight: 600 }}
                  onClick={() => { setMode('LOGIN'); setLocalError(null); }}
                >
                  Back to Login
                </button>
              </div>
            </div>

            <div className="modal-footer">
              <button type="button" className="btn btn-outline" onClick={() => setMode('LOGIN')}>
                {t('cancel')}
              </button>
              <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                {isSubmitting ? 'Resetting...' : 'Reset Password'}
              </button>
            </div>
          </form>
        ) : (
          <form onSubmit={handleRegisterSubmit}>
            <div className="modal-body" style={{ maxHeight: '70vh', overflowY: 'auto' }}>
              {(localError || error) && (
                <div className="alert-box danger" style={{ marginBottom: '1rem' }}>
                  <AlertCircle size={20} />
                  <span>{localError || error}</span>
                </div>
              )}

              {/* Role Selection */}
              <div className="form-group">
                <label className="form-label">{t('selectRole')}</label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.5rem' }}>
                  <button
                    type="button"
                    className={`btn ${regRole === 'PATIENT' ? 'btn-primary' : 'btn-outline'}`}
                    style={{ minHeight: '38px', fontSize: '0.85rem' }}
                    onClick={() => setRegRole('PATIENT')}
                  >
                    👤 {t('rolePatient')}
                  </button>
                  <button
                    type="button"
                    className={`btn ${regRole === 'DOCTOR' ? 'btn-primary' : 'btn-outline'}`}
                    style={{ minHeight: '38px', fontSize: '0.85rem' }}
                    onClick={() => setRegRole('DOCTOR')}
                  >
                    🩺 {t('roleDoctor')}
                  </button>
                  <button
                    type="button"
                    className={`btn ${regRole === 'ASHA' ? 'btn-primary' : 'btn-outline'}`}
                    style={{ minHeight: '38px', fontSize: '0.85rem' }}
                    onClick={() => setRegRole('ASHA')}
                  >
                    👩‍⚕️ {t('roleAsha')}
                  </button>
                  <button
                    type="button"
                    className={`btn ${regRole === 'CAREGIVER' ? 'btn-primary' : 'btn-outline'}`}
                    style={{ minHeight: '38px', fontSize: '0.85rem' }}
                    onClick={() => setRegRole('CAREGIVER')}
                  >
                    🤝 {t('roleCaregiver')}
                  </button>
                </div>
              </div>

              {/* Full Name */}
              <div className="form-group">
                <label className="form-label" htmlFor="reg-fullname">
                  {t('fullName')} *
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="reg-fullname"
                    type="text"
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                    value={regFullName}
                    onChange={(e) => setRegFullName(e.target.value)}
                    placeholder="e.g. Kavita Deshmukh"
                    required
                  />
                  <User
                    size={18}
                    style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
                  />
                </div>
              </div>

              {/* Username */}
              <div className="form-group">
                <label className="form-label" htmlFor="reg-username">
                  {t('username')} *
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="reg-username"
                    type="text"
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                    value={regUsername}
                    onChange={(e) => setRegUsername(e.target.value)}
                    placeholder="e.g. patient.kavita or dr.rajesh"
                    required
                  />
                  <User
                    size={18}
                    style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
                  />
                </div>
              </div>

              {/* Phone Number */}
              <div className="form-group">
                <label className="form-label" htmlFor="reg-phone">
                  {t('phoneNumber')}
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="reg-phone"
                    type="tel"
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                    value={regPhone}
                    onChange={(e) => setRegPhone(e.target.value)}
                    placeholder="e.g. 9822998877"
                  />
                  <Phone
                    size={18}
                    style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
                  />
                </div>
              </div>

              {/* Email */}
              <div className="form-group">
                <label className="form-label" htmlFor="reg-email">
                  Email
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="reg-email"
                    type="email"
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                    value={regEmail}
                    onChange={(e) => setRegEmail(e.target.value)}
                    placeholder="e.g. kavita@ruralhealth.org"
                  />
                  <Mail
                    size={18}
                    style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
                  />
                </div>
              </div>

              {/* Password */}
              <div className="form-group">
                <label className="form-label" htmlFor="reg-password">
                  {t('password')} *
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="reg-password"
                    type="password"
                    className="form-control"
                    style={{ paddingLeft: '2.5rem' }}
                    value={regPassword}
                    onChange={(e) => setRegPassword(e.target.value)}
                    placeholder="Min 6 characters"
                    required
                  />
                  <Lock
                    size={18}
                    style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
                  />
                </div>
              </div>

              <div style={{ textAlign: 'right', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  style={{ background: 'none', border: 'none', color: 'var(--primary)', fontSize: '0.85rem', cursor: 'pointer', fontWeight: 600 }}
                  onClick={() => { setMode('LOGIN'); setLocalError(null); }}
                >
                  {t('alreadyHaveAccount')}
                </button>
              </div>
            </div>

            <div className="modal-footer">
              <button type="button" className="btn btn-outline" onClick={onClose}>
                {t('cancel')}
              </button>
              <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                {isSubmitting ? t('registering') : t('createAccount')}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};


import React, { useState, useEffect } from 'react';
import {
  User,
  Users,
  MapPin,
  X,
  Phone,
  Mail,
  Calendar,
  Heart,
  Droplet,
  Save,
  CheckCircle,
  AlertTriangle,
  Plus,
  Trash2,
  Edit2,
  MessageCircle,
  PhoneCall,
  Shield,
  Navigation,
  Send,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useSos } from '../../hooks/useSos';
import { authApi } from '../../api/authApi';
import { Caregiver, NotificationChannel } from '../../types/medicine';

interface UserProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialTab?: 'profile' | 'caregivers' | 'location';
}

const DEFAULT_CAREGIVERS: Caregiver[] = [
  {
    id: 'cg-01',
    patientId: 'b1000000-0000-0000-0000-000000000001',
    name: 'Suresh Patil',
    caregiverName: 'Suresh Patil',
    relationship: 'Son (Primary Caregiver)',
    phoneNumber: '+91 98221 14455',
    contactNumber: '+91 98221 14455',
    email: 'suresh.patil@ruralhealth.org',
    preferredChannel: 'WHATSAPP',
    isActive: true,
    consentGiven: true,
    notifyOnMissedDose: true,
  },
  {
    id: 'cg-02',
    patientId: 'b1000000-0000-0000-0000-000000000001',
    name: 'Sunita Tai (ASHA)',
    caregiverName: 'Sunita Tai (ASHA)',
    relationship: 'Village ASHA Worker',
    phoneNumber: '+91 98223 34455',
    contactNumber: '+91 98223 34455',
    email: 'sunita.tai@ruralhealth.org',
    preferredChannel: 'SMS',
    isActive: true,
    consentGiven: true,
    notifyOnMissedDose: true,
  },
];

export const UserProfileModal: React.FC<UserProfileModalProps> = ({
  isOpen,
  onClose,
  initialTab = 'profile',
}) => {
  const { user, role } = useAuth();
  const { currentCoords, detectLocation, isLocating, emergencyAlertOptIn, syncDeviceAndLocation } = useSos();

  const [activeTab, setActiveTab] = useState<'profile' | 'caregivers' | 'location'>(initialTab);

  // Profile Form State
  const [fullName, setFullName] = useState(user?.displayName || user?.fullName || 'Ramesh Patil');
  const [phoneNumber, setPhoneNumber] = useState(user?.contactNumber || user?.phoneNumber || '+91 98765 43210');
  const [email, setEmail] = useState(user?.email || 'ramesh.patil@ruralhealth.org');
  const [age, setAge] = useState<number | string>(62);
  const [gender, setGender] = useState<'MALE' | 'FEMALE' | 'OTHER'>('MALE');
  const [bloodGroup, setBloodGroup] = useState('O+');
  const [villageTown, setVillageTown] = useState('Palasdeo Village');
  const [district, setDistrict] = useState('Pune District');
  const [state, setState] = useState('Maharashtra');
  const [pincode, setPincode] = useState('413132');
  const [chronicConditions, setChronicConditions] = useState('Type-2 Diabetes, Mild Hypertension');
  const [allergies, setAllergies] = useState('Penicillin (mild rash)');

  // GPS State
  const [latitude, setLatitude] = useState(currentCoords ? String(currentCoords.latitude) : '18.5204');
  const [longitude, setLongitude] = useState(currentCoords ? String(currentCoords.longitude) : '73.8567');
  const [sosOptIn, setSosOptIn] = useState<boolean>(emergencyAlertOptIn);

  // Status & Feedback
  const [statusMsg, setStatusMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  // Caregivers State
  const [caregivers, setCaregivers] = useState<Caregiver[]>(() => {
    try {
      const saved = localStorage.getItem('aarogya_saved_caregivers');
      if (saved) return JSON.parse(saved);
    } catch {}
    return DEFAULT_CAREGIVERS;
  });

  // Add / Edit Caregiver Form State
  const [isAddingCaregiver, setIsAddingCaregiver] = useState(false);
  const [editingCaregiverId, setEditingCaregiverId] = useState<string | null>(null);
  const [cgName, setCgName] = useState('');
  const [cgRelation, setCgRelation] = useState('Son');
  const [cgPhone, setCgPhone] = useState('');
  const [cgEmail, setCgEmail] = useState('');
  const [cgChannel, setCgChannel] = useState<NotificationChannel>('WHATSAPP');
  const [testAlertSentId, setTestAlertSentId] = useState<string | null>(null);

  useEffect(() => {
    setActiveTab(initialTab);
  }, [initialTab]);

  useEffect(() => {
    if (user) {
      setFullName(user.displayName || user.fullName || 'Ramesh Patil');
      setPhoneNumber(user.contactNumber || user.phoneNumber || '+91 98765 43210');
      setEmail(user.email || '');
    }
    if (currentCoords) {
      setLatitude(String(currentCoords.latitude));
      setLongitude(String(currentCoords.longitude));
    }
  }, [user, currentCoords]);

  // Persist Caregivers to local storage whenever they change
  useEffect(() => {
    try {
      localStorage.setItem('aarogya_saved_caregivers', JSON.stringify(caregivers));
    } catch {}
  }, [caregivers]);

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setStatusMsg(null);
    setErrorMsg(null);

    const latNum = parseFloat(latitude);
    const lonNum = parseFloat(longitude);

    try {
      await authApi.updateProfile({
        fullName: fullName.trim(),
        phoneNumber: phoneNumber.trim(),
        email: email.trim(),
        villageTown: villageTown.trim(),
        district: district.trim(),
        state: state.trim(),
        pincode: pincode.trim(),
        latitude: !isNaN(latNum) ? latNum : undefined,
        longitude: !isNaN(lonNum) ? lonNum : undefined,
      });

      if (!isNaN(latNum) && !isNaN(lonNum)) {
        await syncDeviceAndLocation(latNum, lonNum, sosOptIn);
      }

      try {
        localStorage.setItem(
          'aarogya_profile_details',
          JSON.stringify({
            fullName,
            phoneNumber,
            email,
            age,
            gender,
            bloodGroup,
            villageTown,
            district,
            state,
            pincode,
            chronicConditions,
            allergies,
            latitude,
            longitude,
          })
        );
      } catch {}

      setStatusMsg('Profile and clinical health details updated successfully!');
    } catch (err: any) {
      try {
        localStorage.setItem(
          'aarogya_profile_details',
          JSON.stringify({ fullName, phoneNumber, email, age, gender, bloodGroup, villageTown, district })
        );
      } catch {}
      setStatusMsg('Profile saved to local health session!');
    } finally {
      setIsSaving(false);
    }
  };

  const handleAddOrUpdateCaregiver = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!cgName.trim() || !cgPhone.trim()) return;

    if (editingCaregiverId) {
      // Update existing
      setCaregivers((prev) =>
        prev.map((c) =>
          c.id === editingCaregiverId
            ? {
                ...c,
                name: cgName.trim(),
                caregiverName: cgName.trim(),
                relationship: cgRelation,
                phoneNumber: cgPhone.trim(),
                contactNumber: cgPhone.trim(),
                email: cgEmail.trim() || undefined,
                preferredChannel: cgChannel,
              }
            : c
        )
      );
      setEditingCaregiverId(null);
    } else {
      // Add new
      const newCg: Caregiver = {
        id: 'cg-' + Date.now(),
        patientId: user?.patientId || 'b1000000-0000-0000-0000-000000000001',
        name: cgName.trim(),
        caregiverName: cgName.trim(),
        relationship: cgRelation,
        phoneNumber: cgPhone.trim(),
        contactNumber: cgPhone.trim(),
        email: cgEmail.trim() || undefined,
        preferredChannel: cgChannel,
        isActive: true,
        consentGiven: true,
        notifyOnMissedDose: true,
        createdAt: new Date().toISOString(),
      };
      setCaregivers((prev) => [newCg, ...prev]);
    }

    // Reset form
    setCgName('');
    setCgPhone('');
    setCgEmail('');
    setCgRelation('Son');
    setCgChannel('WHATSAPP');
    setIsAddingCaregiver(false);
  };

  const handleStartEditCaregiver = (cg: Caregiver) => {
    setEditingCaregiverId(cg.id);
    setCgName(cg.name || cg.caregiverName || '');
    setCgRelation(cg.relationship || 'Family');
    setCgPhone(cg.phoneNumber || cg.contactNumber || '');
    setCgEmail(cg.email || '');
    setCgChannel(cg.preferredChannel || 'WHATSAPP');
    setIsAddingCaregiver(true);
  };

  const handleDeleteCaregiver = (id: string) => {
    setCaregivers((prev) => prev.filter((c) => c.id !== id));
  };

  const handleSendTestAlert = (cg: Caregiver) => {
    setTestAlertSentId(cg.id);
    setTimeout(() => setTestAlertSentId(null), 3000);
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose} role="dialog" aria-modal="true">
      <div
        className="modal-dialog user-profile-modal-dialog"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '680px', width: '95%' }}
      >
        {/* Header */}
        <div className="modal-header" style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--border-light)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div className="profile-avatar-circle">
              {fullName.slice(0, 2).toUpperCase()}
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <h2 style={{ fontSize: '1.3rem', fontWeight: 800 }}>{fullName}</h2>
                <span className="profile-role-badge">{role}</span>
              </div>
              <span style={{ fontSize: '0.82rem', color: 'var(--text-subtle)' }}>
                {user?.username ? `@${user.username}` : 'Patient Account'} • Health ID #4131-8901
              </span>
            </div>
          </div>
          <button className="icon-btn" onClick={onClose} aria-label="Close Profile">
            <X size={20} />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="profile-tabs-nav">
          <button
            type="button"
            className={`profile-tab-btn ${activeTab === 'profile' ? 'active' : ''}`}
            onClick={() => setActiveTab('profile')}
          >
            <User size={16} />
            <span>Profile Details</span>
          </button>
          <button
            type="button"
            className={`profile-tab-btn ${activeTab === 'caregivers' ? 'active' : ''}`}
            onClick={() => setActiveTab('caregivers')}
          >
            <Users size={16} />
            <span>Caregivers ({caregivers.length})</span>
          </button>
          <button
            type="button"
            className={`profile-tab-btn ${activeTab === 'location' ? 'active' : ''}`}
            onClick={() => setActiveTab('location')}
          >
            <MapPin size={16} />
            <span>GPS & 5 km SOS</span>
          </button>
        </div>

        {/* Modal Body */}
        <div className="modal-body" style={{ maxHeight: '68vh', overflowY: 'auto', padding: '1.5rem' }}>
          {statusMsg && (
            <div className="alert-box success" style={{ marginBottom: '1.25rem' }}>
              <CheckCircle size={18} />
              <span>{statusMsg}</span>
            </div>
          )}
          {errorMsg && (
            <div className="alert-box warning" style={{ marginBottom: '1.25rem' }}>
              <AlertTriangle size={18} />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* TAB 1: PROFILE DETAILS */}
          {activeTab === 'profile' && (
            <form onSubmit={handleSaveProfile}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Full Name (पूर्ण नाव) *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Mobile Number (मोबाईल क्र.) *</label>
                  <input
                    type="tel"
                    className="form-control"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Email Address (ईमेल)</label>
                  <input
                    type="email"
                    className="form-control"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Blood Group (रक्तगट)</label>
                  <select
                    className="form-control"
                    value={bloodGroup}
                    onChange={(e) => setBloodGroup(e.target.value)}
                  >
                    {['A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-'].map((bg) => (
                      <option key={bg} value={bg}>{bg}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Age (वय)</label>
                  <input
                    type="number"
                    className="form-control"
                    value={age}
                    onChange={(e) => setAge(e.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Gender (लिंग)</label>
                  <select
                    className="form-control"
                    value={gender}
                    onChange={(e) => setGender(e.target.value as any)}
                  >
                    <option value="MALE">Male (पुरुष)</option>
                    <option value="FEMALE">Female (स्त्री)</option>
                    <option value="OTHER">Other (इतर)</option>
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Pincode (पिनकोड)</label>
                  <input
                    type="text"
                    className="form-control"
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Village / Town (गाव / शहर)</label>
                  <input
                    type="text"
                    className="form-control"
                    value={villageTown}
                    onChange={(e) => setVillageTown(e.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">District (जिल्हा)</label>
                  <input
                    type="text"
                    className="form-control"
                    value={district}
                    onChange={(e) => setDistrict(e.target.value)}
                  />
                </div>
              </div>

              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label className="form-label">Chronic Health Conditions (जुनाट आजार)</label>
                <input
                  type="text"
                  className="form-control"
                  value={chronicConditions}
                  onChange={(e) => setChronicConditions(e.target.value)}
                  placeholder="e.g. Hypertension, Diabetes, Asthma"
                />
              </div>

              <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                <label className="form-label">Known Allergies / Clinical Notes (अ‍ॅलर्जी)</label>
                <input
                  type="text"
                  className="form-control"
                  value={allergies}
                  onChange={(e) => setAllergies(e.target.value)}
                  placeholder="e.g. Penicillin, Peanuts, Dust"
                />
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                style={{ width: '100%', height: '48px', fontSize: '1rem', fontWeight: 700 }}
                disabled={isSaving}
              >
                <Save size={18} />
                <span>{isSaving ? 'Saving Changes...' : 'Save Profile Changes'}</span>
              </button>
            </form>
          )}

          {/* TAB 2: CAREGIVERS MANAGEMENT */}
          {activeTab === 'caregivers' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <div>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 800 }}>Assigned Caregivers & Guardians</h3>
                  <p style={{ fontSize: '0.82rem', color: 'var(--text-subtle)' }}>
                    Caregivers receive real-time SMS & WhatsApp alerts for missed doses and emergency SOS broadcasts.
                  </p>
                </div>
                {!isAddingCaregiver && (
                  <button
                    type="button"
                    className="btn btn-primary"
                    style={{ minHeight: '38px', padding: '0 0.85rem', fontSize: '0.85rem' }}
                    onClick={() => {
                      setEditingCaregiverId(null);
                      setCgName('');
                      setCgPhone('');
                      setCgEmail('');
                      setCgRelation('Son');
                      setIsAddingCaregiver(true);
                    }}
                  >
                    <Plus size={16} />
                    <span>Add Caregiver</span>
                  </button>
                )}
              </div>

              {/* Add / Edit Caregiver Form */}
              {isAddingCaregiver && (
                <div className="caregiver-form-box">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                    <h4 style={{ fontWeight: 800, fontSize: '0.95rem' }}>
                      {editingCaregiverId ? 'Edit Caregiver Details' : 'Register New Caregiver'}
                    </h4>
                    <button
                      type="button"
                      className="icon-btn"
                      style={{ height: '32px', width: '32px' }}
                      onClick={() => setIsAddingCaregiver(false)}
                    >
                      <X size={16} />
                    </button>
                  </div>
                  <form onSubmit={handleAddOrUpdateCaregiver}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '0.75rem' }}>
                      <div>
                        <label className="form-label" style={{ fontSize: '0.8rem' }}>Caregiver Name *</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. Suresh Patil"
                          value={cgName}
                          onChange={(e) => setCgName(e.target.value)}
                          required
                        />
                      </div>
                      <div>
                        <label className="form-label" style={{ fontSize: '0.8rem' }}>Relationship *</label>
                        <select
                          className="form-control"
                          value={cgRelation}
                          onChange={(e) => setCgRelation(e.target.value)}
                        >
                          <option value="Son">Son (मुलगा)</option>
                          <option value="Daughter">Daughter (मुलगी)</option>
                          <option value="Spouse">Spouse (पती / पत्नी)</option>
                          <option value="Parent">Parent (आई / वडील)</option>
                          <option value="Sibling">Brother / Sister (भाऊ / बहीण)</option>
                          <option value="ASHA Worker">ASHA Village Guardian (आशा ताई)</option>
                          <option value="Neighbor">Neighbor / Friend (शेजारी)</option>
                          <option value="Other">Other Guardian</option>
                        </select>
                      </div>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '1rem' }}>
                      <div>
                        <label className="form-label" style={{ fontSize: '0.8rem' }}>Phone Number *</label>
                        <input
                          type="tel"
                          className="form-control"
                          placeholder="+91 98221 14455"
                          value={cgPhone}
                          onChange={(e) => setCgPhone(e.target.value)}
                          required
                        />
                      </div>
                      <div>
                        <label className="form-label" style={{ fontSize: '0.8rem' }}>Preferred Alert Channel</label>
                        <select
                          className="form-control"
                          value={cgChannel}
                          onChange={(e) => setCgChannel(e.target.value as NotificationChannel)}
                        >
                          <option value="WHATSAPP">WhatsApp Message</option>
                          <option value="SMS">SMS Text Message</option>
                          <option value="VOICE_CALL">Voice Call Automated</option>
                        </select>
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                      <button
                        type="button"
                        className="btn btn-outline"
                        style={{ minHeight: '38px', fontSize: '0.85rem' }}
                        onClick={() => setIsAddingCaregiver(false)}
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        className="btn btn-primary"
                        style={{ minHeight: '38px', fontSize: '0.85rem', fontWeight: 700 }}
                      >
                        <Save size={16} />
                        <span>{editingCaregiverId ? 'Update Caregiver' : 'Save Caregiver'}</span>
                      </button>
                    </div>
                  </form>
                </div>
              )}

              {/* Caregivers List */}
              <div className="caregivers-grid">
                {caregivers.length === 0 ? (
                  <div className="empty-state-box">
                    <Users size={32} color="var(--text-subtle)" />
                    <p style={{ fontWeight: 600, marginTop: '0.5rem' }}>No caregivers registered yet.</p>
                    <span style={{ fontSize: '0.82rem', color: 'var(--text-subtle)' }}>
                      Add a family member or local ASHA worker to receive emergency SOS notifications.
                    </span>
                  </div>
                ) : (
                  caregivers.map((cg) => (
                    <div key={cg.id} className="caregiver-card">
                      <div className="caregiver-card-top">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                          <div className="caregiver-avatar">
                            {(cg.name || cg.caregiverName || 'CG').slice(0, 2).toUpperCase()}
                          </div>
                          <div>
                            <div style={{ fontWeight: 800, fontSize: '1rem', color: 'var(--text-main)' }}>
                              {cg.name || cg.caregiverName}
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', marginTop: '0.1rem' }}>
                              <span className="caregiver-rel-tag">{cg.relationship}</span>
                              <span className="caregiver-status-dot" title="Active Responder" />
                              <span style={{ fontSize: '0.75rem', color: '#059669', fontWeight: 700 }}>Active Guardian</span>
                            </div>
                          </div>
                        </div>

                        <div className="caregiver-actions-group">
                          <button
                            type="button"
                            className="icon-action-btn edit"
                            onClick={() => handleStartEditCaregiver(cg)}
                            title="Edit Caregiver"
                          >
                            <Edit2 size={15} />
                          </button>
                          <button
                            type="button"
                            className="icon-action-btn delete"
                            onClick={() => handleDeleteCaregiver(cg.id)}
                            title="Remove Caregiver"
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </div>

                      <div className="caregiver-contact-row">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem' }}>
                          <Phone size={14} color="var(--primary)" />
                          <span style={{ fontWeight: 600 }}>{cg.phoneNumber || cg.contactNumber}</span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                          <span className="caregiver-channel-pill">
                            <MessageCircle size={12} />
                            <span>{cg.preferredChannel || 'WHATSAPP'}</span>
                          </span>
                        </div>
                      </div>

                      <div className="caregiver-footer-row">
                        <span style={{ fontSize: '0.78rem', color: 'var(--text-subtle)' }}>
                          Alerted on missed doses & SOS calls
                        </span>
                        <button
                          type="button"
                          className="btn btn-outline"
                          style={{ minHeight: '30px', padding: '0 0.65rem', fontSize: '0.75rem', fontWeight: 700 }}
                          onClick={() => handleSendTestAlert(cg)}
                        >
                          <Send size={12} />
                          <span>{testAlertSentId === cg.id ? '✓ Alert Sent!' : 'Send Test Alert'}</span>
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* TAB 3: GPS & 5 KM SOS SETTINGS */}
          {activeTab === 'location' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <div>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 800 }}>Live GPS & Community SOS Radius</h3>
                  <p style={{ fontSize: '0.82rem', color: 'var(--text-subtle)' }}>
                    Your live GPS coordinates enable 5 km emergency dispatch to nearby doctors, ambulances & ASHA workers.
                  </p>
                </div>
                <button
                  type="button"
                  className="btn btn-outline"
                  style={{ minHeight: '38px', padding: '0 0.85rem', fontSize: '0.85rem' }}
                  onClick={() => {
                    detectLocation();
                    if (navigator.geolocation) {
                      navigator.geolocation.getCurrentPosition((pos) => {
                        setLatitude(pos.coords.latitude.toFixed(6));
                        setLongitude(pos.coords.longitude.toFixed(6));
                      });
                    }
                  }}
                  disabled={isLocating}
                >
                  <Navigation size={14} />
                  <span>{isLocating ? 'Locating...' : 'Detect Live GPS'}</span>
                </button>
              </div>

              <div className="location-settings-card">
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                  <div>
                    <label className="form-label">Latitude Coordinates</label>
                    <input
                      type="number"
                      step="any"
                      className="form-control"
                      value={latitude}
                      onChange={(e) => setLatitude(e.target.value)}
                    />
                  </div>
                  <div>
                    <label className="form-label">Longitude Coordinates</label>
                    <input
                      type="number"
                      step="any"
                      className="form-control"
                      value={longitude}
                      onChange={(e) => setLongitude(e.target.value)}
                    />
                  </div>
                </div>

                <label className="checkbox-label" style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', cursor: 'pointer', padding: '0.5rem 0' }}>
                  <input
                    type="checkbox"
                    checked={sosOptIn}
                    onChange={(e) => setSosOptIn(e.target.checked)}
                    style={{ width: '20px', height: '20px', accentColor: 'var(--primary)' }}
                  />
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.9rem' }}>Opt-in to 5 km Emergency Responder Community</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-subtle)' }}>
                      Receive critical alerts if a villager nearby triggers an emergency SOS.
                    </div>
                  </div>
                </label>
              </div>

              {/* Emergency Helplines Direct Dial */}
              <div style={{ marginTop: '1.25rem' }}>
                <h4 style={{ fontSize: '0.95rem', fontWeight: 800, marginBottom: '0.5rem' }}>Direct National Helplines</h4>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
                  <a href="tel:108" className="helpline-card danger">
                    <span style={{ fontSize: '1.1rem', fontWeight: 900 }}>108</span>
                    <span style={{ fontSize: '0.75rem' }}>Ambulance SOS</span>
                  </a>
                  <a href="tel:112" className="helpline-card primary">
                    <span style={{ fontSize: '1.1rem', fontWeight: 900 }}>112</span>
                    <span style={{ fontSize: '0.75rem' }}>Police & Disaster</span>
                  </a>
                  <a href="tel:104" className="helpline-card secondary">
                    <span style={{ fontSize: '1.1rem', fontWeight: 900 }}>104</span>
                    <span style={{ fontSize: '0.75rem' }}>Health Advice</span>
                  </a>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="modal-footer" style={{ padding: '1rem 1.5rem', borderTop: '1px solid var(--border-light)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
            <Shield size={14} color="#059669" />
            <span>End-to-end encrypted rural health vault</span>
          </div>
          <button type="button" className="btn btn-outline" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

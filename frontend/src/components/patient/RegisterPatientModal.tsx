import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { CreatePatientRequest, Gender, PatientDto } from '../../types/patient';
import { patientApi } from '../../api/patientApi';

interface RegisterPatientModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (newPatient: PatientDto) => void;
  initialFullName?: string;
  initialContactNumber?: string;
}

export const RegisterPatientModal: React.FC<RegisterPatientModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  initialFullName = '',
  initialContactNumber = '',
}) => {
  const [formData, setFormData] = useState<CreatePatientRequest>({
    fullName: initialFullName || '',
    gender: 'MALE',
    age: 35,
    contactNumber: initialContactNumber || '+919876543210',
    villageTown: 'Shirwal',
    subDistrict: 'Khandala',
    district: 'Satara',
    state: 'Maharashtra',
    pincode: '412801',
    emergencyContactName: '',
    emergencyContactNumber: '',
    latitude: 18.5204,
    longitude: 73.8567,
    bloodGroup: 'B+',
  });

  React.useEffect(() => {
    if (isOpen) {
      setFormData((prev) => ({
        ...prev,
        fullName: initialFullName || prev.fullName,
        contactNumber: initialContactNumber || prev.contactNumber,
      }));
    }
  }, [isOpen, initialFullName, initialContactNumber]);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    const payload: CreatePatientRequest = {
      ...formData,
      contactNumber: formData.contactNumber.trim(),
      emergencyContactName: formData.emergencyContactName?.trim() || undefined,
      emergencyContactNumber: formData.emergencyContactNumber?.trim() || undefined,
      bloodGroup: formData.bloodGroup?.trim() || undefined,
      addressLine1: formData.addressLine1?.trim() || undefined,
      addressLine2: formData.addressLine2?.trim() || undefined,
    };

    try {
      const created = await patientApi.registerPatient(payload);
      onSuccess(created);
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to register patient');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Register Rural Patient (रुग्ण नोंदणी)"
      subtitle="Enter patient demographics, village location, and emergency contact details"
      maxWidth="680px"
    >
      {error && (
        <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="grid-2">
          <div className="form-group">
            <label className="form-label">
              Full Name <span className="required">*</span>
            </label>
            <input
              type="text"
              required
              className="form-input"
              value={formData.fullName}
              onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
              placeholder="e.g. Ramesh Jadhav"
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              Gender <span className="required">*</span>
            </label>
            <select
              className="form-select"
              value={formData.gender}
              onChange={(e) => setFormData({ ...formData, gender: e.target.value as Gender })}
            >
              <option value="MALE">Male (पुरुष)</option>
              <option value="FEMALE">Female (स्त्री)</option>
              <option value="OTHER">Other (इतर)</option>
            </select>
          </div>
        </div>

        <div className="grid-3">
          <div className="form-group">
            <label className="form-label">
              Age (Years) <span className="required">*</span>
            </label>
            <input
              type="number"
              required
              min={0}
              max={120}
              className="form-input"
              value={formData.age}
              onChange={(e) => setFormData({ ...formData, age: parseInt(e.target.value) || 0 })}
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              Contact Number <span className="required">*</span>
            </label>
            <input
              type="tel"
              required
              className="form-input"
              value={formData.contactNumber}
              onChange={(e) => setFormData({ ...formData, contactNumber: e.target.value })}
              placeholder="+919876543210"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Blood Group</label>
            <select
              className="form-select"
              value={formData.bloodGroup || ''}
              onChange={(e) => setFormData({ ...formData, bloodGroup: e.target.value })}
            >
              <option value="A+">A+</option>
              <option value="A-">A-</option>
              <option value="B+">B+</option>
              <option value="B-">B-</option>
              <option value="O+">O+</option>
              <option value="O-">O-</option>
              <option value="AB+">AB+</option>
              <option value="AB-">AB-</option>
            </select>
          </div>
        </div>

        <div className="grid-3">
          <div className="form-group">
            <label className="form-label">
              Village / Town <span className="required">*</span>
            </label>
            <input
              type="text"
              required
              className="form-input"
              value={formData.villageTown}
              onChange={(e) => setFormData({ ...formData, villageTown: e.target.value })}
              placeholder="e.g. Shirwal"
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              Sub-District (तालुका) <span className="required">*</span>
            </label>
            <input
              type="text"
              required
              className="form-input"
              value={formData.subDistrict}
              onChange={(e) => setFormData({ ...formData, subDistrict: e.target.value })}
              placeholder="e.g. Khandala"
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              District (जिल्हा) <span className="required">*</span>
            </label>
            <input
              type="text"
              required
              className="form-input"
              value={formData.district}
              onChange={(e) => setFormData({ ...formData, district: e.target.value })}
              placeholder="e.g. Satara"
            />
          </div>
        </div>

        <div className="grid-2">
          <div className="form-group">
            <label className="form-label">
              State <span className="required">*</span>
            </label>
            <input
              type="text"
              required
              className="form-input"
              value={formData.state}
              onChange={(e) => setFormData({ ...formData, state: e.target.value })}
              placeholder="Maharashtra"
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              Pincode <span className="required">*</span>
            </label>
            <input
              type="text"
              required
              pattern="^[1-9][0-9]{5}$"
              className="form-input"
              value={formData.pincode}
              onChange={(e) => setFormData({ ...formData, pincode: e.target.value })}
              placeholder="412801"
            />
          </div>
        </div>

        <div className="grid-2">
          <div className="form-group">
            <label className="form-label">GPS Latitude (अक्षांश)</label>
            <input
              type="number"
              step="any"
              className="form-input"
              value={formData.latitude || ''}
              onChange={(e) => setFormData({ ...formData, latitude: parseFloat(e.target.value) })}
              placeholder="18.5204"
            />
          </div>

          <div className="form-group">
            <label className="form-label">GPS Longitude (रेखांश)</label>
            <input
              type="number"
              step="any"
              className="form-input"
              value={formData.longitude || ''}
              onChange={(e) => setFormData({ ...formData, longitude: parseFloat(e.target.value) })}
              placeholder="73.8567"
            />
          </div>
        </div>

        <div className="modal-footer" style={{ margin: '1rem -1.5rem -1.5rem', padding: '1rem 1.5rem' }}>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" isLoading={isLoading}>
            Register Patient
          </Button>
        </div>
      </form>
    </Modal>
  );
};

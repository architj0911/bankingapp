import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  getMyCustomer,
  createCustomer,
  getMyKyc,
  verifyPan,
  uploadAadhaarOfflinePackage,
  matchFace
} from '../api/customerApi';
import FaceLivenessVerification from '../components/FaceLivenessVerification';
import {
  User,
  Phone,
  Mail,
  FileText,
  CheckCircle2,
  AlertTriangle,
  Send,
  RefreshCw,
  Camera,
  ShieldCheck,
  Check,
  XCircle,
  Clock,
  Sparkles,
  Upload
} from 'lucide-react';

const CustomerProfile = ({ showToast }) => {
  const { user } = useAuth();
  const [customer, setCustomer] = useState(null);
  const [kyc, setKyc] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  // Profile Form
  const [profileForm, setProfileForm] = useState({ name: '', phone: '', email: '' });
  const [creatingProfile, setCreatingProfile] = useState(false);

  // PAN state
  const [panInput, setPanInput] = useState('');
  const [verifyingPan, setVerifyingPan] = useState(false);

  // Aadhaar state
  const [aadhaarFile, setAadhaarFile] = useState(null);
  const [uploadingAadhaar, setUploadingAadhaar] = useState(false);

  // Liveness & Face Match state
  const [showLivenessModal, setShowLivenessModal] = useState(false);
  const [capturedLiveImage, setCapturedLiveImage] = useState(null);
  const [matchingFace, setMatchingFace] = useState(false);

  const fetchCustomerAndKyc = async () => {
    if (!user || !user.userId) return;
    setLoading(true);
    setLoadError('');
    try {
      const res = await getMyCustomer();
      setCustomer(res.data);
      if (res.data) {
        setProfileForm({
          name: res.data.name || '',
          phone: res.data.phone || '',
          email: res.data.email || ''
        });
      }
    } catch (err) {
      setLoadError(err.response?.data?.message || `Could not load customer profile${err.response?.status ? ` (HTTP ${err.response.status})` : ''}.`);
      setCustomer(null);
    }

    try {
      const kycRes = await getMyKyc();
      setKyc(kycRes.data);
    } catch (err) {
      setLoadError((current) => current || err.response?.data?.message || `Could not load KYC status${err.response?.status ? ` (HTTP ${err.response.status})` : ''}.`);
      setKyc(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCustomerAndKyc();
  }, [user]);

  const handleCreateCustomer = async (e) => {
    e.preventDefault();
    setCreatingProfile(true);
    try {
      const res = await createCustomer(profileForm);
      showToast('Customer profile created successfully!', 'success');
      setCustomer(res.data);
      fetchCustomerAndKyc();
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || err.message || 'Failed to create customer profile', 'error');
    } finally {
      setCreatingProfile(false);
    }
  };

  const handleVerifyPan = async (e) => {
    e.preventDefault();
    if (!panInput || !panInput.trim()) {
      showToast('Please enter your PAN number', 'error');
      return;
    }
    setVerifyingPan(true);
    try {
      await verifyPan(panInput.trim().toUpperCase());
      showToast('PAN verified in Demo mode (Format Valid).', 'success');
      setPanInput('');
      fetchCustomerAndKyc();
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || err.message || 'PAN verification failed', 'error');
    } finally {
      setVerifyingPan(false);
    }
  };

  const handleAadhaarUpload = async (e) => {
    e.preventDefault();
    if (!aadhaarFile) {
      showToast('Select an XML or ZIP offline e-KYC package', 'error');
      return;
    }
    setUploadingAadhaar(true);
    try {
      await uploadAadhaarOfflinePackage(customer?.id, aadhaarFile);
      showToast('Aadhaar Offline e-KYC package verified in Demo mode.', 'success');
      setAadhaarFile(null);
      fetchCustomerAndKyc();
    } catch (err) {
      showToast(err.response?.data?.message || err.message || 'Offline e-KYC upload failed', 'error');
    } finally {
      setUploadingAadhaar(false);
    }
  };

  const handleLivenessComplete = (liveImage) => {
    setShowLivenessModal(false);
    setCapturedLiveImage(liveImage);
    showToast('Face liveness passed! Proceed to Face Match.', 'success');
    fetchCustomerAndKyc();
  };

  const handleTriggerFaceMatch = async () => {
    setMatchingFace(true);
    try {
      await matchFace(capturedLiveImage || 'CAPTURED_DEMO_IMAGE', 'KYC_AADHAAR_PHOTO_REF');
      showToast('Face matching verified successfully! KYC Complete.', 'success');
      fetchCustomerAndKyc();
    } catch (err) {
      showToast(err.response?.data?.message || 'Face match verification failed', 'error');
    } finally {
      setMatchingFace(false);
    }
  };

  const isVerified = (status) => status === 'VERIFIED' || status === 'DEMO_VERIFIED' || status === 'PASSED';
  const overallKycVerified = kyc?.overallStatus === 'KYC_VERIFIED' || kyc?.overallStatus === 'VERIFIED' || kyc?.status === 'KYC_VERIFIED' || kyc?.status === 'VERIFIED';

  const formatVerifiedDate = (dateStr) => {
    if (!dateStr) return new Date().toLocaleDateString('en-GB');
    const d = new Date(dateStr);
    return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear()}`;
  };

  if (loading) {
    return (
      <div style={{ padding: '3rem', textAlign: 'center', color: '#94a3b8' }}>
        <RefreshCw size={36} style={{ animation: 'spin 1s linear infinite' }} />
        <p style={{ marginTop: '0.75rem', fontSize: '0.95rem' }}>Loading customer profile & KYC status...</p>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="glass-card" style={{ padding: '2rem', maxWidth: '900px', margin: '2rem auto' }} role="alert">
        <h2 style={{ color: '#f8fafc', marginBottom: '0.5rem' }}>Profile data could not be loaded</h2>
        <p style={{ color: '#fca5a5', marginBottom: '1rem' }}>{loadError}</p>
        <button className="btn-primary" onClick={fetchCustomerAndKyc}><RefreshCw size={14} /> Retry</button>
      </div>
    );
  }

  return (
    <div style={{ padding: '2rem', maxWidth: '1080px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* Header */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <h1 style={{ fontSize: '1.85rem', fontWeight: '800', color: '#f8fafc' }}>
            APEXBANK KYC VERIFICATION
          </h1>
        </div>
        <p style={{ color: '#94a3b8', fontSize: '0.92rem', marginTop: '0.25rem' }}>
          Identity Verification, Aadhaar Offline e-KYC, and Biometric Liveness Verification Portal
        </p>
      </div>

      {/* Customer Profile Creation Section (if missing) */}
      {!customer ? (
        <div className="glass-card" style={{ padding: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.25rem', color: '#38bdf8' }}>
            <User size={24} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: '600', color: '#f8fafc' }}>Initialize Customer Profile</h2>
          </div>
          <p style={{ color: '#94a3b8', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
            Create your digital customer profile for User ID <strong>{user?.userId}</strong> before starting KYC.
          </p>

          <form onSubmit={handleCreateCustomer} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div style={{ gridColumn: 'span 2' }}>
              <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.3rem' }}>Full Legal Name</label>
              <input
                type="text"
                className="input-field"
                placeholder="e.g. John Doe"
                value={profileForm.name}
                onChange={(e) => setProfileForm({ ...profileForm, name: e.target.value })}
                required
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.3rem' }}>Phone Number</label>
              <input
                type="text"
                className="input-field"
                placeholder="e.g. 9876543210"
                value={profileForm.phone}
                onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                required
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.3rem' }}>Email Address</label>
              <input
                type="email"
                className="input-field"
                placeholder="e.g. john@example.com"
                value={profileForm.email}
                onChange={(e) => setProfileForm({ ...profileForm, email: e.target.value })}
                required
              />
            </div>
            <div style={{ gridColumn: 'span 2', marginTop: '0.5rem' }}>
              <button type="submit" className="btn-primary" disabled={creatingProfile}>
                {creatingProfile ? 'Creating Customer Record...' : 'Create Customer Profile'}
              </button>
            </div>
          </form>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.3fr', gap: '1.5rem' }}>
          {/* Left Column: Personal Profile Details */}
          <div className="glass-card" style={{ padding: '1.75rem', height: 'fit-content' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.2rem', fontWeight: '700', color: '#f8fafc' }}>Customer Profile</h2>
              <span className={`badge ${customer.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'}`}>
                {customer.status || 'PENDING'}
              </span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.1rem' }}>
              <div>
                <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Customer ID</span>
                <p style={{ fontSize: '1rem', fontWeight: '700', color: '#38bdf8' }}>{customer.id}</p>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em' }}>User ID</span>
                <p style={{ fontSize: '1rem', fontWeight: '700', color: '#38bdf8' }}>{customer.userId || user?.userId}</p>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Full Name</span>
                <p style={{ fontSize: '1rem', fontWeight: '600', color: '#f8fafc' }}>{customer.name}</p>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Email</span>
                <p style={{ fontSize: '0.95rem', fontWeight: '500', color: '#cbd5e1' }}>{customer.email}</p>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Phone</span>
                <p style={{ fontSize: '0.95rem', fontWeight: '500', color: '#cbd5e1' }}>{customer.phone}</p>
              </div>
            </div>

            {/* Overall Status Banner */}
            <div style={{
              marginTop: '2rem',
              padding: '1.25rem',
              borderRadius: '0.75rem',
              background: overallKycVerified ? 'rgba(16, 185, 129, 0.15)' : 'rgba(245, 158, 11, 0.12)',
              border: `1px solid ${overallKycVerified ? '#10b981' : '#f59e0b'}`
            }}>
              <span style={{ fontSize: '0.75rem', color: '#94a3b8', textTransform: 'uppercase' }}>Overall Status</span>
              <h3 style={{
                fontSize: '1.15rem',
                fontWeight: '800',
                color: overallKycVerified ? '#10b981' : '#f59e0b',
                marginTop: '0.25rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem'
              }}>
                {overallKycVerified ? <CheckCircle2 size={20} /> : <Clock size={20} />}
                {overallKycVerified ? 'KYC VERIFIED' : 'KYC IN PROGRESS'}
              </h3>
              {overallKycVerified && (
                <p style={{ fontSize: '0.8rem', color: '#cbd5e1', marginTop: '0.4rem', margin: 0 }}>
                  KYC Verified on: <strong>{formatVerifiedDate(kyc?.verifiedAt)}</strong>
                </p>
              )}
            </div>
          </div>

          {/* Right Column: Complete KYC Verification Stages */}
          <div className="glass-card" style={{ padding: '1.75rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: '700', color: '#f8fafc' }}>Verification Stages</h2>
              <span className="badge badge-warning" style={{ fontSize: '0.75rem' }}>DEMO VERIFICATION</span>
            </div>

            {/* Verification Stage Checklist per Section 17 */}
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '0.85rem',
              background: 'rgba(15, 23, 42, 0.5)',
              padding: '1.25rem',
              borderRadius: '0.75rem',
              border: '1px solid rgba(51, 65, 85, 0.7)',
              marginBottom: '1.5rem'
            }}>
              {/* Stage 1: Email */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  {isVerified(kyc?.emailStatus) ? <CheckCircle2 size={18} color="#10b981" /> : <Clock size={18} color="#f59e0b" />}
                  <span style={{ fontSize: '0.9rem', color: '#f8fafc', fontWeight: '500' }}>Email Verification</span>
                </div>
                <span className={`badge ${isVerified(kyc?.emailStatus) ? 'badge-success' : 'badge-warning'}`}>
                  {isVerified(kyc?.emailStatus) ? '✓ Verified' : 'Pending'}
                </span>
              </div>

              {/* Stage 2: PAN */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  {isVerified(kyc?.panStatus) ? <CheckCircle2 size={18} color="#10b981" /> : <Clock size={18} color="#f59e0b" />}
                  <span style={{ fontSize: '0.9rem', color: '#f8fafc', fontWeight: '500' }}>PAN Verification</span>
                </div>
                <span className={`badge ${isVerified(kyc?.panStatus) ? 'badge-success' : 'badge-warning'}`}>
                  {isVerified(kyc?.panStatus) ? `✓ ${kyc.panMasked || 'Verified'}` : 'Pending'}
                </span>
              </div>

              {/* Stage 3: Aadhaar Offline e-KYC */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  {isVerified(kyc?.aadhaarStatus) ? <CheckCircle2 size={18} color="#10b981" /> : <Clock size={18} color="#f59e0b" />}
                  <span style={{ fontSize: '0.9rem', color: '#f8fafc', fontWeight: '500' }}>Aadhaar Offline e-KYC</span>
                </div>
                <span className={`badge ${isVerified(kyc?.aadhaarStatus) ? 'badge-success' : 'badge-warning'}`}>
                  {isVerified(kyc?.aadhaarStatus) ? '✓ Package Accepted' : 'Pending'}
                </span>
              </div>

              {/* Stage 4: Face Liveness */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  {isVerified(kyc?.livenessStatus) ? <CheckCircle2 size={18} color="#10b981" /> : <Clock size={18} color="#f59e0b" />}
                  <span style={{ fontSize: '0.9rem', color: '#f8fafc', fontWeight: '500' }}>Face Liveness</span>
                </div>
                <span className={`badge ${isVerified(kyc?.livenessStatus) ? 'badge-success' : 'badge-warning'}`}>
                  {isVerified(kyc?.livenessStatus) ? '✓ Passed' : 'Pending'}
                </span>
              </div>

              {/* Stage 5: Face Match */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                  {isVerified(kyc?.faceStatus) ? <CheckCircle2 size={18} color="#10b981" /> : <Clock size={18} color="#f59e0b" />}
                  <span style={{ fontSize: '0.9rem', color: '#f8fafc', fontWeight: '500' }}>Face Match</span>
                </div>
                <span className={`badge ${isVerified(kyc?.faceStatus) ? 'badge-success' : 'badge-warning'}`}>
                  {isVerified(kyc?.faceStatus) ? '✓ Passed' : 'Pending'}
                </span>
              </div>
            </div>

            {/* Interactive Forms for Pending Stages */}
            {!overallKycVerified ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                {/* Step A: PAN Input */}
                {!isVerified(kyc?.panStatus) && (
                  <form onSubmit={handleVerifyPan} style={{ background: 'rgba(30, 41, 59, 0.4)', padding: '1rem', borderRadius: '0.75rem', border: '1px solid rgba(51, 65, 85, 0.6)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem', color: '#38bdf8' }}>
                      <FileText size={18} />
                      <span style={{ fontWeight: '600', fontSize: '0.9rem' }}>Step 2: Submit PAN Card</span>
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <input
                        type="text"
                        className="input-field"
                        placeholder="ABCDE1234F"
                        maxLength={10}
                        value={panInput}
                        onChange={(e) => setPanInput(e.target.value.toUpperCase())}
                        style={{ letterSpacing: '0.1em', fontWeight: '600' }}
                        required
                      />
                      <button type="submit" className="btn-primary" disabled={verifyingPan || panInput.length !== 10}>
                        {verifyingPan ? 'Validating...' : 'Validate PAN'}
                      </button>
                    </div>
                    <small style={{ color: '#64748b', marginTop: '0.4rem', display: 'block' }}>
                      PAN Format Valid check (DEMO VERIFICATION).
                    </small>
                  </form>
                )}

                {/* Step B: Aadhaar Offline e-KYC Upload */}
                {!isVerified(kyc?.aadhaarStatus) && (
                  <form onSubmit={handleAadhaarUpload} style={{ background: 'rgba(30, 41, 59, 0.4)', padding: '1rem', borderRadius: '0.75rem', border: '1px solid rgba(51, 65, 85, 0.6)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem', color: '#38bdf8' }}>
                      <Upload size={18} />
                      <span style={{ fontWeight: '600', fontSize: '0.9rem' }}>Step 3: Offline Aadhaar e-KYC Package</span>
                    </div>
                    <input
                      type="file"
                      accept=".xml,.zip"
                      onChange={(e) => setAadhaarFile(e.target.files?.[0] || null)}
                      style={{ fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.5rem' }}
                    />
                    <button type="submit" className="btn-primary" disabled={uploadingAadhaar || !aadhaarFile}>
                      {uploadingAadhaar ? 'Processing...' : 'Upload Demo Package'}
                    </button>
                    <small style={{ color: '#64748b', marginTop: '0.4rem', display: 'block' }}>
                      Upload .xml or .zip package. Digital signature/demo verified.
                    </small>
                  </form>
                )}

                {/* Step C: Face Liveness Verification Button */}
                {!isVerified(kyc?.livenessStatus) && (
                  <div style={{ background: 'rgba(30, 41, 59, 0.4)', padding: '1rem', borderRadius: '0.75rem', border: '1px solid rgba(51, 65, 85, 0.6)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem', color: '#38bdf8' }}>
                      <Camera size={18} />
                      <span style={{ fontWeight: '600', fontSize: '0.9rem' }}>Step 4: Interactive Face Liveness Check</span>
                    </div>
                    <p style={{ fontSize: '0.82rem', color: '#94a3b8', marginBottom: '0.75rem' }}>
                      Launch your camera to complete dynamic head rotation and gesture challenges.
                    </p>
                    <button
                      type="button"
                      className="btn-primary"
                      onClick={() => setShowLivenessModal(true)}
                    >
                      <Camera size={16} /> Launch Webcam Liveness Check
                    </button>
                  </div>
                )}

                {/* Step D: Face Match Verification Button */}
                {isVerified(kyc?.livenessStatus) && !isVerified(kyc?.faceStatus) && (
                  <div style={{ background: 'rgba(30, 41, 59, 0.4)', padding: '1rem', borderRadius: '0.75rem', border: '1px solid rgba(51, 65, 85, 0.6)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem', color: '#38bdf8' }}>
                      <Sparkles size={18} />
                      <span style={{ fontWeight: '600', fontSize: '0.9rem' }}>Step 5: Face Match Verification</span>
                    </div>
                    <p style={{ fontSize: '0.82rem', color: '#94a3b8', marginBottom: '0.75rem' }}>
                      Match live webcam capture against Aadhaar demographic photo.
                    </p>
                    <button
                      type="button"
                      className="btn-primary"
                      onClick={handleTriggerFaceMatch}
                      disabled={matchingFace}
                    >
                      {matchingFace ? 'Matching Face Features...' : 'Verify Face Match & Complete KYC'}
                    </button>
                  </div>
                )}
              </div>
            ) : (
              /* Success Complete View per Section 17 */
              <div style={{
                background: 'rgba(16, 185, 129, 0.12)',
                border: '1px solid #10b981',
                borderRadius: '0.75rem',
                padding: '1.5rem',
                textAlign: 'center'
              }}>
                <div style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: '50%',
                  background: 'rgba(16, 185, 129, 0.2)',
                  color: '#10b981',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 1rem'
                }}>
                  <ShieldCheck size={36} />
                </div>
                <h3 style={{ fontSize: '1.3rem', fontWeight: '800', color: '#10b981', marginBottom: '0.5rem' }}>
                  ✓ KYC VERIFIED
                </h3>
                <p style={{ fontSize: '0.88rem', color: '#cbd5e1', maxWidth: '420px', margin: '0 auto 1rem' }}>
                  All identity verification, document validation, and biometric checks have been completed successfully. Your banking account is active.
                </p>
                <div style={{ display: 'inline-block', background: 'rgba(15, 23, 42, 0.6)', padding: '0.4rem 1rem', borderRadius: '2rem', fontSize: '0.85rem', color: '#38bdf8' }}>
                  KYC Date: {formatVerifiedDate(kyc?.verifiedAt)}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Face Liveness Camera Modal */}
      {showLivenessModal && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0, 0, 0, 0.75)',
          backdropFilter: 'blur(6px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '1rem'
        }}>
          <FaceLivenessVerification
            onComplete={handleLivenessComplete}
            onCancel={() => setShowLivenessModal(false)}
            showToast={showToast}
          />
        </div>
      )}
    </div>
  );
};

export default CustomerProfile;

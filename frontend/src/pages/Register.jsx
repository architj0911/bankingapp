import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerUser, sendRegistrationEmailOtp, verifyRegistrationEmailOtp } from '../api/authApi';
import OtpInput from '../components/OtpInput';
import { ShieldCheck, User, Lock, Mail, Phone, Building, BadgeCheck, ArrowRight, CheckCircle2, Clock } from 'lucide-react';

const Register = ({ showToast }) => {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    branchId: 'BRANCH001',
    employeeCode: 'EMP100',
    phoneNumber: '',
    email: ''
  });

  const [loading, setLoading] = useState(false);
  const [emailOtp, setEmailOtp] = useState('');
  const [emailVerified, setEmailVerified] = useState(false);
  const [emailOtpSent, setEmailOtpSent] = useState(false);
  const [emailLoading, setEmailLoading] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  const navigate = useNavigate();

  const passwordRules = {
    length: formData.password.length >= 8,
    uppercase: /[A-Z]/.test(formData.password),
    number: /\d/.test(formData.password),
    special: /[^A-Za-z0-9]/.test(formData.password)
  };
  const passwordValid = Object.values(passwordRules).every(Boolean);

  useEffect(() => {
    if (cooldown <= 0) return;
    const interval = setInterval(() => setCooldown(prev => Math.max(0, prev - 1)), 1000);
    return () => clearInterval(interval);
  }, [cooldown]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (e.target.name === 'email') {
      setEmailVerified(false);
      setEmailOtpSent(false);
      setEmailOtp('');
    }
  };

  const handleSendEmailOtp = async () => {
    if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(formData.email)) {
      showToast('Enter a valid email before requesting verification code', 'error');
      return;
    }
    setEmailLoading(true);
    try {
      await sendRegistrationEmailOtp(formData.email);
      setEmailOtpSent(true);
      setCooldown(60);
      showToast('Verification code dispatched to your email.', 'success');
    } catch (err) {
      const msg = err.response?.data?.message || (err.message === 'Network Error' 
        ? 'Cannot reach backend server. Please verify API Gateway is running.' 
        : err.message || 'Unable to send verification code');
      showToast(msg, 'error');
    } finally {
      setEmailLoading(false);
    }
  };

  const handleVerifyEmailOtp = async () => {
    if (!emailOtp || emailOtp.trim().length !== 6) {
      showToast('Please enter the complete 6-digit OTP', 'error');
      return;
    }
    setEmailLoading(true);
    try {
      await verifyRegistrationEmailOtp(formData.email, emailOtp.trim());
      setEmailVerified(true);
      showToast('Email address verified successfully.', 'success');
    } catch (err) {
      setEmailVerified(false);
      const msg = err.response?.data?.message || 'Email verification failed. Please check the code.';
      showToast(msg, 'error');
    } finally {
      setEmailLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!passwordValid) {
      showToast('Password must be 8+ characters with uppercase, number, and special character', 'error');
      return;
    }
    if (!emailVerified) {
      showToast('Mandatory email OTP verification required before registration', 'error');
      return;
    }
    if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(formData.email)) {
      showToast('Please enter a valid email address', 'error');
      return;
    }

    setLoading(true);
    try {
      await registerUser(formData);
      showToast('Registration complete! You may now sign in.', 'success');
      navigate('/login');
    } catch (err) {
      console.error('Registration failed:', err);
      const msg = err.response?.data?.message || err.message || 'Registration failed. Try a different username.';
      showToast(msg, 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page" style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'radial-gradient(circle at top right, #1e293b, #0f172a)',
      padding: '2rem'
    }}>
      <div className="glass-card auth-card" style={{ width: '100%', maxWidth: '520px', padding: '2.5rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '1.75rem' }}>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '14px',
            background: 'linear-gradient(135deg, #0284c7, #38bdf8)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            margin: '0 auto 0.75rem',
            boxShadow: '0 8px 25px rgba(56, 189, 248, 0.4)'
          }}>
            <ShieldCheck size={28} />
          </div>
          <h2 className="auth-title" style={{ fontSize: '1.5rem', fontWeight: '700', color: '#f8fafc', marginBottom: '0.25rem' }}>
            Open an ApexBank Account
          </h2>
          <p className="auth-subtitle" style={{ color: '#94a3b8', fontSize: '0.85rem' }}>
            Enterprise Digital Banking & Wallet Platform
          </p>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <div style={{ gridColumn: 'span 2' }}>
            <label className="auth-label" style={{ display: 'block', fontSize: '0.85rem', fontWeight: '500', color: '#cbd5e1', marginBottom: '0.3rem' }}>
              Username
            </label>
            <div style={{ position: 'relative' }}>
              <User size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
              <input type="text" name="username" className="input-field" style={{ paddingLeft: '2.5rem' }} value={formData.username} onChange={handleChange} placeholder="Desired username" required />
            </div>
          </div>

          <div style={{ gridColumn: 'span 2' }}>
            <label className="auth-label" style={{ display: 'block', fontSize: '0.85rem', fontWeight: '500', color: '#cbd5e1', marginBottom: '0.3rem' }}>
              Password
            </label>
            <div style={{ position: 'relative' }}>
              <Lock size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
              <input type="password" name="password" className="input-field" style={{ paddingLeft: '2.5rem' }} value={formData.password} onChange={handleChange} placeholder="Strong password" required />
            </div>
            <div id="password-rules" className="password-rules" style={{ marginTop: '0.4rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap', fontSize: '0.75rem' }}>
              <span className={passwordRules.length ? 'rule-valid' : ''} style={{ color: passwordRules.length ? '#10b981' : '#94a3b8' }}>8+ chars</span>
              <span className={passwordRules.uppercase ? 'rule-valid' : ''} style={{ color: passwordRules.uppercase ? '#10b981' : '#94a3b8' }}>1 Uppercase</span>
              <span className={passwordRules.number ? 'rule-valid' : ''} style={{ color: passwordRules.number ? '#10b981' : '#94a3b8' }}>1 Number</span>
              <span className={passwordRules.special ? 'rule-valid' : ''} style={{ color: passwordRules.special ? '#10b981' : '#94a3b8' }}>1 Special</span>
            </div>
          </div>

          <div style={{ gridColumn: 'span 2' }}>
            <label className="auth-label" style={{ display: 'block', fontSize: '0.85rem', fontWeight: '500', color: '#cbd5e1', marginBottom: '0.3rem' }}>
              Email Address (For Real OTP Delivery)
            </label>
            <div style={{ position: 'relative' }}>
              <Mail size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
              <input type="email" name="email" className="input-field" style={{ paddingLeft: '2.5rem' }} value={formData.email} onChange={handleChange} placeholder="yourname@gmail.com" required />
            </div>
          </div>

          {/* Email OTP Verification Section */}
          <div style={{ gridColumn: 'span 2', background: 'rgba(30, 41, 59, 0.6)', padding: '1rem', borderRadius: '0.75rem', border: '1px solid rgba(51, 65, 85, 0.7)' }}>
            {!emailVerified ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '0.85rem', color: '#cbd5e1', fontWeight: '500' }}>Mandatory Email Verification</span>
                  <button
                    type="button"
                    className="btn-secondary"
                    onClick={handleSendEmailOtp}
                    disabled={emailLoading || cooldown > 0}
                    style={{ fontSize: '0.8rem', padding: '0.4rem 0.85rem' }}
                  >
                    {emailLoading ? 'Sending...' : (cooldown > 0 ? `Resend (${cooldown}s)` : 'Send OTP')}
                  </button>
                </div>
                {emailOtpSent && (
                  <div>
                    <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: '0 0 0.5rem 0', textAlign: 'center' }}>
                      Enter the 6-digit verification code sent to your inbox:
                    </p>
                    <OtpInput
                      value={emailOtp}
                      onChange={setEmailOtp}
                      disabled={emailLoading}
                    />
                    <button
                      type="button"
                      className="btn-primary"
                      onClick={handleVerifyEmailOtp}
                      disabled={emailLoading || emailOtp.length !== 6}
                      style={{ width: '100%', justifyContent: 'center', marginTop: '0.5rem' }}
                    >
                      {emailLoading ? 'Verifying...' : 'Verify Code'}
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#10b981', fontSize: '0.9rem', fontWeight: '600' }}>
                <CheckCircle2 size={20} />
                <span>Email Address Verified ✓</span>
              </div>
            )}
          </div>

          <div>
            <label className="auth-label" style={{ display: 'block', fontSize: '0.85rem', fontWeight: '500', color: '#cbd5e1', marginBottom: '0.3rem' }}>
              Phone Number
            </label>
            <div style={{ position: 'relative' }}>
              <Phone size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
              <input type="text" name="phoneNumber" className="input-field" style={{ paddingLeft: '2.5rem' }} value={formData.phoneNumber} onChange={handleChange} placeholder="+91 9876543210" required />
            </div>
          </div>

          <div>
            <label className="auth-label" style={{ display: 'block', fontSize: '0.85rem', fontWeight: '500', color: '#cbd5e1', marginBottom: '0.3rem' }}>
              Branch ID
            </label>
            <div style={{ position: 'relative' }}>
              <Building size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
              <input type="text" name="branchId" className="input-field" style={{ paddingLeft: '2.5rem' }} value={formData.branchId} onChange={handleChange} required />
            </div>
          </div>

          <div style={{ gridColumn: 'span 2', marginTop: '0.5rem' }}>
            <button type="submit" className="btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={loading || !emailVerified}>
              {loading ? 'Creating Account...' : (
                <>
                  Complete Registration <ArrowRight size={18} />
                </>
              )}
            </button>
          </div>
        </form>

        <div className="auth-footer" style={{ textAlign: 'center', marginTop: '1.25rem', fontSize: '0.875rem', color: '#94a3b8' }}>
          Already registered?{' '}
          <Link to="/login" style={{ color: '#38bdf8', textDecoration: 'none', fontWeight: '600' }}>
            Sign In Here
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Register;

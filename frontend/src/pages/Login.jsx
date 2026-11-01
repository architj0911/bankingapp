import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { loginUser, verifyOtp, resendLoginOtp } from '../api/authApi';
import { useAuth } from '../context/AuthContext';
import OtpInput from '../components/OtpInput';
import { ShieldCheck, User, ArrowRight, Lock, Sparkles, RefreshCw, Mail, AlertCircle, Clock } from 'lucide-react';

const Login = ({ showToast }) => {
  const [step, setStep] = useState(1); // 1: Credentials, 2: OTP Challenge
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [otp, setOtp] = useState('');
  const [challengeId, setChallengeId] = useState('');
  const [maskedEmail, setMaskedEmail] = useState('');
  const [deviceId, setDeviceId] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);
  const [otpExpiresIn, setOtpExpiresIn] = useState(300); // 5 minutes in seconds
  const [errorMessage, setErrorMessage] = useState('');

  const { loginToken, login } = useAuth();
  const navigate = useNavigate();

  // Cooldown countdown timer for Resend button
  useEffect(() => {
    if (resendCooldown <= 0) return;
    const interval = setInterval(() => {
      setResendCooldown((prev) => Math.max(0, prev - 1));
    }, 1000);
    return () => clearInterval(interval);
  }, [resendCooldown]);

  // 5-minute expiration countdown timer for current OTP
  useEffect(() => {
    if (step !== 2 || otpExpiresIn <= 0) return;
    const interval = setInterval(() => {
      setOtpExpiresIn((prev) => {
        if (prev <= 1) {
          setErrorMessage('The verification code has expired. Please request a new OTP.');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [step, otpExpiresIn]);

  const handleCredentialsSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!username.trim() || !password) {
      showToast('Please enter both username and password', 'error');
      return;
    }

    setLoading(true);
    try {
      const generatedDeviceId = `device_${Date.now()}`;
      setDeviceId(generatedDeviceId);
      const res = await loginUser(username.trim(), password, generatedDeviceId);

      const cid = res.data?.challengeId || `challenge_${Date.now()}`;
      const email = res.data?.maskedEmail || 'registered email';

      setChallengeId(cid);
      setMaskedEmail(email);
      setOtp('');
      setResendCooldown(60);
      setOtpExpiresIn(300);
      setStep(2);
      showToast('A 6-digit verification code has been dispatched to your email.', 'success');
    } catch (err) {
      console.error('Credentials login error:', err);
      const errMsg = err.response?.data?.message || (err.message === 'Network Error' 
        ? 'Cannot connect to ApexBank backend. Please verify that the API Gateway is running.' 
        : err.message || 'Invalid username or password');
      setErrorMessage(errMsg);
      showToast(errMsg, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleOtpSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!otp || otp.trim().length !== 6) {
      showToast('Please enter the complete 6-digit OTP', 'error');
      return;
    }

    if (otpExpiresIn <= 0) {
      setErrorMessage('Verification code has expired. Please request a new code.');
      showToast('Verification code has expired. Please request a new code.', 'error');
      return;
    }

    setLoading(true);
    try {
      const res = await verifyOtp(challengeId, otp.trim(), deviceId);
      const token = res.data?.access_token || res.data?.accessToken || (typeof res.data === 'string' ? res.data : null);

      if (token && typeof token === 'string') {
        const doLogin = loginToken || login;
        doLogin(token);
        showToast('Identity verified successfully! Welcome to ApexBank.', 'success');
        navigate('/dashboard');
      } else {
        throw new Error('Invalid token structure received from server');
      }
    } catch (err) {
      console.error('OTP verification error:', err);
      const errMsg = err.response?.data?.message || err.message || 'Invalid or expired verification code';
      setErrorMessage(errMsg);
      showToast(errMsg, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (resendCooldown > 0 || !challengeId) return;
    setLoading(true);
    setErrorMessage('');

    try {
      await resendLoginOtp(challengeId);
      setResendCooldown(60);
      setOtpExpiresIn(300);
      setOtp('');
      showToast('A fresh verification code has been sent to your email.', 'success');
    } catch (err) {
      const errMsg = err.response?.data?.message || 'Failed to resend verification code';
      setErrorMessage(errMsg);
      showToast(errMsg, 'error');
    } finally {
      setLoading(false);
    }
  };

  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
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
      <div className="glass-card auth-card" style={{ width: '100%', maxWidth: '460px', padding: '2.5rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            width: '54px',
            height: '54px',
            borderRadius: '16px',
            background: 'linear-gradient(135deg, #0284c7, #38bdf8)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            margin: '0 auto 1rem',
            boxShadow: '0 8px 25px rgba(56, 189, 248, 0.4)'
          }}>
            <ShieldCheck size={32} />
          </div>
          <h2 className="auth-title" style={{ fontSize: '1.75rem', fontWeight: '700', color: '#f8fafc', marginBottom: '0.5rem' }}>
            APEXBANK
          </h2>
          <p className="auth-subtitle" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>
            {step === 1 ? 'Digital Banking Portal Login' : 'Mandatory Email OTP Authentication'}
          </p>
        </div>

        {errorMessage && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            background: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: '0.5rem',
            padding: '0.75rem 1rem',
            marginBottom: '1.25rem',
            color: '#f87171',
            fontSize: '0.85rem'
          }}>
            <AlertCircle size={18} style={{ flexShrink: 0 }} />
            <span>{errorMessage}</span>
          </div>
        )}

        {step === 1 ? (
          <form onSubmit={handleCredentialsSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div>
              <label className="auth-label" style={{ display: 'block', fontSize: '0.85rem', fontWeight: '500', color: '#cbd5e1', marginBottom: '0.4rem' }}>
                Username or Registered Email
              </label>
              <div style={{ position: 'relative' }}>
                <User size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
                <input
                  type="text"
                  className="input-field"
                  style={{ paddingLeft: '2.5rem' }}
                  placeholder="Enter your username or email"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                />
              </div>
            </div>

            <div>
              <label className="auth-label" style={{ display: 'block', fontSize: '0.85rem', fontWeight: '500', color: '#cbd5e1', marginBottom: '0.4rem' }}>
                Password
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
                <input
                  type="password"
                  className="input-field"
                  style={{ paddingLeft: '2.5rem' }}
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <button type="submit" className="btn-primary" style={{ width: '100%', justifyContent: 'center', marginTop: '0.5rem' }} disabled={loading}>
              {loading ? 'Verifying Credentials...' : (
                <>
                  Continue <ArrowRight size={18} />
                </>
              )}
            </button>

            <div className="auth-footer" style={{ textAlign: 'center', marginTop: '1rem', fontSize: '0.875rem', color: '#94a3b8' }}>
              Don't have an account?{' '}
              <Link to="/register" style={{ color: '#38bdf8', textDecoration: 'none', fontWeight: '600' }}>
                Register Here
              </Link>
            </div>
          </form>
        ) : (
          <form onSubmit={handleOtpSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div style={{
              background: 'rgba(56, 189, 248, 0.08)',
              border: '1px solid rgba(56, 189, 248, 0.2)',
              borderRadius: '0.75rem',
              padding: '1rem',
              textAlign: 'center'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', color: '#38bdf8', marginBottom: '0.25rem' }}>
                <Mail size={18} />
                <span style={{ fontWeight: '600', fontSize: '0.95rem' }}>ENTER SECURITY CODE</span>
              </div>
              <p style={{ fontSize: '0.82rem', color: '#94a3b8', margin: 0 }}>
                We've sent a 6-digit OTP code to:
              </p>
              <p style={{ fontSize: '0.95rem', fontWeight: '600', color: '#f8fafc', marginTop: '0.25rem' }}>
                {maskedEmail}
              </p>
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
                <label className="auth-label" style={{ fontSize: '0.85rem', fontWeight: '500', color: '#cbd5e1' }}>
                  6-Digit Verification Code
                </label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.8rem', color: otpExpiresIn < 60 ? '#f87171' : '#94a3b8' }}>
                  <Clock size={14} />
                  <span>Expires: {formatTimer(otpExpiresIn)}</span>
                </div>
              </div>

              {/* 6 Individual OTP Input Boxes with Auto Focus and Paste Support */}
              <OtpInput
                value={otp}
                onChange={setOtp}
                disabled={loading || otpExpiresIn <= 0}
              />

              {/* Resend OTP Row with 60s Timer */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.75rem', fontSize: '0.82rem' }}>
                <span style={{ color: '#94a3b8' }}>Didn't receive the email?</span>
                {resendCooldown > 0 ? (
                  <span style={{ color: '#38bdf8', fontWeight: '600' }}>
                    Resend in {resendCooldown}s
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={handleResendOtp}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#38bdf8',
                      fontWeight: '600',
                      cursor: 'pointer',
                      padding: 0,
                      textDecoration: 'underline'
                    }}
                    disabled={loading}
                  >
                    Resend OTP
                  </button>
                )}
              </div>
            </div>

            <button
              type="submit"
              className="btn-primary"
              style={{ width: '100%', justifyContent: 'center' }}
              disabled={loading || otp.length !== 6 || otpExpiresIn <= 0}
            >
              {loading ? 'Authenticating...' : (
                <>
                  Verify & Sign In <Sparkles size={18} />
                </>
              )}
            </button>

            <button
              type="button"
              className="btn-secondary"
              style={{ width: '100%', justifyContent: 'center' }}
              onClick={() => {
                setStep(1);
                setOtp('');
                setErrorMessage('');
              }}
              disabled={loading}
            >
              Back to Credentials
            </button>
          </form>
        )}
      </div>
    </div>
  );
};

export default Login;

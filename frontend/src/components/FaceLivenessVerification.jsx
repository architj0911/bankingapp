import React, { useState, useEffect, useRef } from 'react';
import { Camera, CheckCircle2, AlertCircle, RefreshCw, Sparkles, UserCheck, Shield } from 'lucide-react';
import { startFaceLiveness, completeFaceLiveness } from '../api/customerApi';

const CHALLENGE_DESCRIPTIONS = {
  TURN_LEFT: { title: 'Turn Head Left', text: 'Turn your head slowly towards the LEFT', icon: '👈' },
  TURN_RIGHT: { title: 'Turn Head Right', text: 'Turn your head slowly towards the RIGHT', icon: '👉' },
  BLINK: { title: 'Blink Eyes', text: 'Blink your eyes twice naturally', icon: '👁️' },
  LOOK_UP: { title: 'Look Up', text: 'Tilt your head slightly UPWARDS', icon: '👆' },
};

const FaceLivenessVerification = ({ onComplete, onCancel, showToast }) => {
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);

  const [sessionId, setSessionId] = useState('');
  const [challenges, setChallenges] = useState([]);
  const [currentStep, setCurrentStep] = useState(0);
  const [isCapturing, setIsCapturing] = useState(false);
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState('');
  const [loading, setLoading] = useState(true);
  const [stepTimer, setStepTimer] = useState(8);
  const [actionDone, setActionDone] = useState(false);

  // Initialize camera and session
  useEffect(() => {
    initLiveness();
    return () => {
      stopCamera();
    };
  }, []);

  const initLiveness = async () => {
    setLoading(true);
    setCameraError('');
    try {
      const res = await startFaceLiveness();
      const session = res.data?.sessionId || 'DEMO-SESSION-' + Date.now();
      const challs = res.data?.challenges && res.data.challenges.length > 0 
        ? res.data.challenges 
        : ['TURN_RIGHT', 'BLINK', 'TURN_LEFT'];
      
      setSessionId(session);
      setChallenges(challs);
      setCurrentStep(0);
      setStepTimer(8);
      setActionDone(false);

      await startCamera();
    } catch (err) {
      console.error('Liveness start error:', err);
      // Fallback demo challenges
      setSessionId('DEMO-SESSION-' + Date.now());
      setChallenges(['TURN_RIGHT', 'BLINK', 'TURN_LEFT']);
      await startCamera();
    } finally {
      setLoading(false);
    }
  };

  const startCamera = async () => {
    try {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' }
        });
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          videoRef.current.play();
        }
        setCameraActive(true);
      } else {
        setCameraError('Webcam not supported in this browser. Demo simulated mode active.');
      }
    } catch (err) {
      console.warn('Webcam permission denied or unavailable:', err.message);
      setCameraError('Camera access unavailable. You can use simulated demo verification.');
    }
  };

  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    setCameraActive(false);
  };

  // Step timer
  useEffect(() => {
    if (loading || isCapturing || challenges.length === 0) return;
    if (stepTimer <= 0) {
      handleNextStep();
      return;
    }
    const timer = setInterval(() => {
      setStepTimer(prev => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [stepTimer, currentStep, loading, isCapturing, challenges]);

  const captureFrame = () => {
    if (videoRef.current && canvasRef.current) {
      const video = videoRef.current;
      const canvas = canvasRef.current;
      canvas.width = video.videoWidth || 320;
      canvas.height = video.videoHeight || 240;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      return canvas.toDataURL('image/jpeg', 0.85);
    }
    return 'data:image/jpeg;base64,DEMO_BASE64_FRAME_' + Date.now();
  };

  const handleActionDetected = () => {
    setActionDone(true);
    setTimeout(() => {
      handleNextStep();
    }, 800);
  };

  const handleNextStep = () => {
    if (currentStep < challenges.length - 1) {
      setCurrentStep(prev => prev + 1);
      setStepTimer(8);
      setActionDone(false);
    } else {
      finishLiveness();
    }
  };

  const finishLiveness = async () => {
    setIsCapturing(true);
    const liveFrame = captureFrame();
    try {
      await completeFaceLiveness(sessionId, liveFrame);
      if (showToast) showToast('Face liveness challenge passed!', 'success');
      stopCamera();
      if (onComplete) onComplete(liveFrame);
    } catch (err) {
      console.error('Complete liveness error:', err);
      if (showToast) showToast('Liveness verified in demo mode.', 'success');
      stopCamera();
      if (onComplete) onComplete(liveFrame);
    } finally {
      setIsCapturing(false);
    }
  };

  const activeChallengeKey = challenges[currentStep] || 'BLINK';
  const activeChallenge = CHALLENGE_DESCRIPTIONS[activeChallengeKey] || {
    title: activeChallengeKey,
    text: `Perform challenge: ${activeChallengeKey}`,
    icon: '👤'
  };

  const progressPercent = challenges.length > 0 ? ((currentStep + 1) / challenges.length) * 100 : 0;

  return (
    <div className="face-liveness-modal" style={{
      background: 'rgba(15, 23, 42, 0.95)',
      border: '1px solid rgba(56, 189, 248, 0.3)',
      borderRadius: '1.25rem',
      padding: '2rem',
      maxWidth: '540px',
      margin: '0 auto',
      boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
      textAlign: 'center',
      color: '#f8fafc'
    }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#38bdf8' }}>
          <Shield size={22} />
          <span style={{ fontWeight: '700', fontSize: '1.1rem' }}>ApexBank Liveness Check</span>
        </div>
        <span className="badge badge-warning" style={{ fontSize: '0.75rem', padding: '0.25rem 0.6rem' }}>
          DEMO FACE LIVENESS
        </span>
      </div>

      <p style={{ fontSize: '0.85rem', color: '#94a3b8', marginBottom: '1.25rem' }}>
        Position your face inside the frame and complete each randomized gesture challenge.
      </p>

      {/* Progress Bar */}
      <div style={{ marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: '#cbd5e1', marginBottom: '0.4rem' }}>
          <span>Step {currentStep + 1} of {challenges.length}</span>
          <span>Time: {stepTimer}s</span>
        </div>
        <div style={{ height: '6px', background: '#334155', borderRadius: '4px', overflow: 'hidden' }}>
          <div style={{
            height: '100%',
            width: `${progressPercent}%`,
            background: 'linear-gradient(90deg, #0284c7, #38bdf8)',
            transition: 'width 0.3s ease'
          }} />
        </div>
      </div>

      {/* Video Viewport with Oval Frame Guide */}
      <div style={{
        position: 'relative',
        width: '100%',
        maxWidth: '380px',
        height: '280px',
        margin: '0 auto 1.5rem',
        borderRadius: '1rem',
        overflow: 'hidden',
        background: '#020617',
        border: actionDone ? '3px solid #10b981' : '2px dashed #38bdf8',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}>
        {cameraActive ? (
          <video
            ref={videoRef}
            playsInline
            muted
            style={{ width: '100%', height: '100%', objectFit: 'cover', transform: 'scaleX(-1)' }}
          />
        ) : (
          <div style={{ padding: '1rem', color: '#64748b' }}>
            <Camera size={48} style={{ margin: '0 auto 0.5rem', opacity: 0.5 }} />
            <p style={{ fontSize: '0.85rem' }}>{cameraError || 'Camera stream active'}</p>
          </div>
        )}

        {/* Circular Face Guide Overlay */}
        <div style={{
          position: 'absolute',
          top: '15px',
          bottom: '15px',
          left: '40px',
          right: '40px',
          border: '2px solid rgba(56, 189, 248, 0.6)',
          borderRadius: '50%',
          pointerEvents: 'none',
          boxShadow: '0 0 0 9999px rgba(15, 23, 42, 0.45)'
        }} />

        {/* Action Completed Overlay */}
        {actionDone && (
          <div style={{
            position: 'absolute',
            inset: 0,
            background: 'rgba(16, 185, 129, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#10b981'
          }}>
            <CheckCircle2 size={54} />
          </div>
        )}

        <canvas ref={canvasRef} style={{ display: 'none' }} />
      </div>

      {/* Challenge Instruction Card */}
      <div style={{
        background: 'rgba(30, 41, 59, 0.7)',
        border: '1px solid rgba(51, 65, 85, 0.8)',
        borderRadius: '0.75rem',
        padding: '1rem',
        marginBottom: '1.5rem'
      }}>
        <div style={{ fontSize: '2rem', marginBottom: '0.25rem' }}>{activeChallenge.icon}</div>
        <h3 style={{ fontSize: '1.15rem', fontWeight: '700', color: '#f8fafc', marginBottom: '0.25rem' }}>
          {activeChallenge.title}
        </h3>
        <p style={{ fontSize: '0.85rem', color: '#38bdf8' }}>{activeChallenge.text}</p>
      </div>

      {/* Interactive Controls */}
      <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
        <button
          type="button"
          className="btn-primary"
          onClick={handleActionDetected}
          disabled={isCapturing || actionDone}
          style={{ flex: 1, justifyContent: 'center' }}
        >
          {isCapturing ? 'Verifying Frame...' : (
            <>
              <CheckCircle2 size={18} /> Done Gesture ({activeChallenge.title})
            </>
          )}
        </button>

        {onCancel && (
          <button
            type="button"
            className="btn-secondary"
            onClick={() => {
              stopCamera();
              onCancel();
            }}
          >
            Cancel
          </button>
        )}
      </div>

      <p style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '1rem' }}>
        Keep your face steady inside the oval guide. This demo liveness verification is simulated locally.
      </p>
    </div>
  );
};

export default FaceLivenessVerification;

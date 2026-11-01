import React from 'react';
import { CheckCircle, AlertCircle, X } from 'lucide-react';

const Toast = ({ message, type = 'success', onClose }) => {
  if (!message) return null;

  const isSuccess = type === 'success';

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '24px',
        right: '24px',
        zIndex: 9999,
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        padding: '14px 20px',
        borderRadius: '12px',
        background: isSuccess ? '#064e3b' : '#7f1d1d',
        border: `1px solid ${isSuccess ? '#10b981' : '#ef4444'}`,
        color: '#ffffff',
        boxShadow: '0 10px 25px rgba(0,0,0,0.4)',
        animation: 'slideUp 0.3s ease-out',
      }}
    >
      {isSuccess ? <CheckCircle size={20} color="#34d399" /> : <AlertCircle size={20} color="#f87171" />}
      <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>{message}</span>
      <button
        onClick={onClose}
        style={{
          background: 'none',
          border: 'none',
          color: '#94a3b8',
          cursor: 'pointer',
          padding: '2px',
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <X size={16} />
      </button>
    </div>
  );
};

export { Toast };
export default Toast;

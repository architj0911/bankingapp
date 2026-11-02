import React, { useRef, useEffect } from 'react';

/**
 * Professional 6-digit OTP Input component.
 * Supports auto-focus, paste, keyboard backspace navigation, and mobile input.
 */
const OtpInput = ({ value = '', onChange, disabled = false, autoFocus = true }) => {
  const inputRefs = useRef([]);

  // Ensure digits array always has 6 items
  const digits = Array(6).fill('').map((_, i) => (value && value[i]) ? value[i] : '');

  useEffect(() => {
    if (autoFocus && inputRefs.current[0] && !disabled) {
      inputRefs.current[0].focus();
    }
  }, [autoFocus, disabled]);

  const handleChange = (e, index) => {
    const val = e.target.value.replace(/\D/g, '');
    if (!val) {
      // Clear digit
      const nextDigits = [...digits];
      nextDigits[index] = '';
      onChange(nextDigits.join(''));
      return;
    }

    // If multiple digits were typed/input
    if (val.length > 1) {
      handlePastedDigits(val, index);
      return;
    }

    const nextDigits = [...digits];
    nextDigits[index] = val;
    const combined = nextDigits.join('');
    onChange(combined);

    // Auto-advance to next box
    if (val && index < 5 && inputRefs.current[index + 1]) {
      inputRefs.current[index + 1].focus();
    }
  };

  const handleKeyDown = (e, index) => {
    if (e.key === 'Backspace') {
      if (!digits[index] && index > 0 && inputRefs.current[index - 1]) {
        // Move back and clear previous
        const nextDigits = [...digits];
        nextDigits[index - 1] = '';
        onChange(nextDigits.join(''));
        inputRefs.current[index - 1].focus();
        e.preventDefault();
      } else {
        const nextDigits = [...digits];
        nextDigits[index] = '';
        onChange(nextDigits.join(''));
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === 'ArrowRight' && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasteData = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pasteData) return;
    handlePastedDigits(pasteData, 0);
  };

  const handlePastedDigits = (pastedString, startIndex) => {
    const nextDigits = [...digits];
    for (let i = 0; i < pastedString.length; i++) {
      const targetIdx = startIndex + i;
      if (targetIdx < 6) {
        nextDigits[targetIdx] = pastedString[i];
      }
    }
    const combined = nextDigits.join('').slice(0, 6);
    onChange(combined);

    // Focus the box following the last pasted digit or the last box
    const focusIdx = Math.min(startIndex + pastedString.length, 5);
    inputRefs.current[focusIdx]?.focus();
  };

  return (
    <div
      style={{
        display: 'flex',
        gap: '0.5rem',
        justifyContent: 'center',
        alignItems: 'center',
        margin: '1rem 0'
      }}
      onPaste={handlePaste}
      role="group"
      aria-label="6-digit verification code"
    >
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(el) => (inputRefs.current[index] = el)}
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          pattern="[0-9]*"
          maxLength={1}
          value={digit}
          disabled={disabled}
          onChange={(e) => handleChange(e, index)}
          onKeyDown={(e) => handleKeyDown(e, index)}
          onFocus={(e) => e.target.select()}
          aria-label={`Digit ${index + 1} of 6`}
          style={{
            width: '46px',
            height: '54px',
            textAlign: 'center',
            fontSize: '1.4rem',
            fontWeight: '700',
            fontFamily: 'monospace',
            color: '#0284c7',
            background: disabled ? '#f1f5f9' : '#ffffff',
            border: digit ? '2px solid #0284c7' : '1.5px solid #cbd5e1',
            borderRadius: '10px',
            outline: 'none',
            boxShadow: digit ? '0 0 0 3px rgba(2, 132, 199, 0.15)' : 'none',
            transition: 'all 0.15s ease-in-out',
            boxSizing: 'border-box'
          }}
        />
      ))}
    </div>
  );
};

export default OtpInput;

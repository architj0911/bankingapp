import axiosClient from './axiosClient';

// User Registration
export const registerUser = async (data) => {
  return await axiosClient.post('/api/auth/register', {
    username: data.username,
    password: data.password,
    branchId: data.branchId || 'BR001',
    employeeCode: data.employeeCode || 'EMP001',
    phoneNumber: data.phoneNumber,
    email: data.email,
  });
};

// Send / Resend Email OTP for Registration
export const sendRegistrationEmailOtp = async (email) => {
  try {
    return await axiosClient.post('/api/auth/send-email-otp', { email });
  } catch (err) {
    // Fallback for direct gateway route if needed
    return await axiosClient.post('/api/kyc/email/send-otp', { email });
  }
};

// Verify Email OTP for Registration
export const verifyRegistrationEmailOtp = async (email, otp) => {
  try {
    return await axiosClient.post('/api/auth/verify-email-otp', { email, otp });
  } catch (err) {
    // Fallback for direct gateway route if needed
    return await axiosClient.post('/api/kyc/email/verify-otp', { email, otp });
  }
};

// Login - Step 1: Verify Credentials & Request OTP Challenge
export const loginUser = async (userName, password, deviceId = 'web_client_device_1') => {
  return await axiosClient.post(
    '/api/auth/login',
    {
      username: userName,
      password: password,
    },
    {
      headers: {
        'X-Device-Id': deviceId,
      },
    }
  );
};

// Login - Step 2: Verify OTP Challenge to Obtain JWT Access Token
export const verifyOtp = async (challengeIdOrUsername, otp, deviceId = 'web_client_device_1') => {
  return await axiosClient.post('/api/auth/verify-login-otp', {
      challengeId: challengeIdOrUsername,
      otp: otp,
  });
};

// Login - Resend OTP Challenge
export const resendLoginOtp = async (challengeId) => {
  try {
    return await axiosClient.post('/api/auth/resend-login-otp', { challengeId });
  } catch (err) {
    return await axiosClient.post('/auth/login/resend-otp', { challengeId });
  }
};

// Password Reset Endpoints
export const requestPasswordReset = async (email) => {
  return await axiosClient.post('/api/auth/forgot-password', { email });
};

export const verifyPasswordReset = async (email, otp) => {
  return await axiosClient.post('/api/auth/verify-password-reset-otp', { email, otp });
};

export const authApi = {
  register: registerUser,
  login: loginUser,
  verifyOtp: verifyOtp,
  resendLoginOtp: resendLoginOtp,
  sendRegistrationEmailOtp,
  verifyRegistrationEmailOtp,
  requestPasswordReset,
  verifyPasswordReset,
};

export default authApi;

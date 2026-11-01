import axiosClient from './axiosClient';

export const createCustomer = async (data) => {
  return await axiosClient.post('/api/customers', {
    name: data.name,
    phone: data.phone,
    email: data.email,
  });
};

export const getMyCustomer = async () => {
  return await axiosClient.get('/api/customers/me');
};

export const getCustomerByUserId = async (userId) => {
  return await axiosClient.get('/api/customers/me');
};

export const getMyKyc = async () => {
  return await axiosClient.get('/api/kyc/status');
};

export const verifyPan = async (pan) => {
  return await axiosClient.post('/api/kyc/pan/verify', { pan });
};

export const uploadAadhaarOfflinePackage = async (customerId, file) => {
  if (customerId) {
    return await axiosClient.post(`/api/kyc/${customerId}/aadhaar/upload`, null, {
      params: { fileName: file.name, fileSize: file.size }
    });
  }
  const formData = new FormData();
  formData.append('file', file);
  return await axiosClient.post('/api/kyc/aadhaar/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

export const startFaceLiveness = async () => {
  return await axiosClient.post('/api/kyc/face/liveness/start');
};

export const completeFaceLiveness = async (sessionId, image) => {
  return await axiosClient.post('/api/kyc/face/liveness/complete', {
    sessionId,
    image,
  });
};

export const matchFace = async (liveImage, kycPhoto) => {
  return await axiosClient.post('/api/kyc/face/match', {
    liveImage,
    kycPhoto,
  });
};

export const submitKyc = async (customerId, kycData) => {
  return await axiosClient.post(`/api/kyc/${customerId}`, {
    panNumber: kycData.panNumber,
  });
};

export const customerApi = {
  createCustomer,
  getMyCustomer,
  getCustomerByUserId,
  getMyKyc,
  verifyPan,
  uploadAadhaarOfflinePackage,
  startFaceLiveness,
  completeFaceLiveness,
  matchFace,
  submitKyc,
};

export default customerApi;

import axiosClient from './axiosClient';

export const createAccount = async (data) => {
  return await axiosClient.post('/api/accounts', {
    accountType: data.accountType, // "SAVING" or "CURRENT"
    intialDeposit: data.initialDeposit || data.intialDeposit || 0,
    currency: data.currency || 'INR',
  });
};

export const getMyAccounts = async () => {
  return await axiosClient.get('/api/accounts/me');
};

export const getAccountByNumber = async (accountNumber) => {
  return await axiosClient.get(`/api/accounts/${accountNumber}`);
};

export const accountApi = {
  createAccount,
  getMyAccounts,
  getAccountByNumber,
  getAccountInternal: getAccountByNumber,
};

export default accountApi;

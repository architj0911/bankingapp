import axiosClient from './axiosClient';

export const executeTransaction = async (data) => {
  return await axiosClient.post('/api/transactions', {
    referenceId: data.referenceId,
    accountNumber: data.accountNumber,
    amount: data.amount,
    type: data.type, // "DEBIT" or "CREDIT"
  });
};

export const transferMoney = async (data) => {
  return await axiosClient.post('/api/transactions/transfer', {
    fromAccountNumber: data.fromAccountNumber,
    toAccountNumber: data.toAccountNumber,
    amount: data.amount,
    description: data.description || 'Transfer',
  }, { headers: { 'Idempotency-Key': data.idempotencyKey || crypto.randomUUID() } });
};

export const previewTransfer = async (data) => {
  return await axiosClient.post('/api/transactions/transfer/preview', {
    fromAccountNumber: data.fromAccountNumber,
    toAccountNumber: data.toAccountNumber,
    amount: data.amount,
  });
};

export const getExchangeRate = async (fromCurrency, toCurrency) => {
  return await axiosClient.get(`/api/exchange-rates/${fromCurrency}/${toCurrency}`);
};

export const getMyTransactions = async () => {
  return await axiosClient.get('/api/transactions/me');
};

export const transactionApi = {
  executeTransaction,
  transferMoney,
  previewTransfer,
  getExchangeRate,
  getMyTransactions,
  transact: executeTransaction,
};

export default transactionApi;

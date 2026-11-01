import React, { useState, useEffect } from 'react';
import { transferMoney, previewTransfer, getMyTransactions } from '../api/transactionApi';
import { getMyAccounts } from '../api/accountApi';
import { ArrowLeftRight, CheckCircle, AlertCircle, ShieldAlert, Send, DollarSign, RefreshCw, History, Wallet } from 'lucide-react';

const TransactionsPage = ({ showToast }) => {
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [loadingTransactions, setLoadingTransactions] = useState(true);

  // Form state
  const [fromAccount, setFromAccount] = useState('');
  const [toAccount, setToAccount] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [lastTxResult, setLastTxResult] = useState(null);
  const [transferQuote, setTransferQuote] = useState(null);

  const fetchAccounts = async () => {
    setLoadingAccounts(true);
    try {
      const res = await getMyAccounts();
      const accList = Array.isArray(res.data) ? res.data : [];
      setAccounts(accList);
      if (accList.length > 0 && !fromAccount) {
        setFromAccount(accList[0].accountNumber);
      }
    } catch (err) {
      console.error('Failed to load accounts:', err);
      setAccounts([]);
    } finally {
      setLoadingAccounts(false);
    }
  };

  const fetchTransactions = async () => {
    setLoadingTransactions(true);
    try {
      const res = await getMyTransactions();
      setTransactions(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Failed to load transactions:', err);
      setTransactions([]);
    } finally {
      setLoadingTransactions(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
    fetchTransactions();
  }, []);

  useEffect(() => {
    if (!fromAccount || !toAccount.trim() || !amount || Number(amount) <= 0) {
      setTransferQuote(null);
      return undefined;
    }
    const timer = setTimeout(async () => {
      try {
        const res = await previewTransfer({
          fromAccountNumber: fromAccount,
          toAccountNumber: toAccount.trim(),
          amount: Number(amount)
        });
        setTransferQuote(res.data);
      } catch {
        setTransferQuote(null);
      }
    }, 350);
    return () => clearTimeout(timer);
  }, [fromAccount, toAccount, amount]);

  const selectedAccObj = accounts.find((a) => a.accountNumber === fromAccount);
  const availableBalance = selectedAccObj ? selectedAccObj.balance : 0;
  const currencySymbol = (currency) => ({ INR: '₹', USD: '$', EUR: '€', GBP: '£', JPY: '¥', AED: 'د.إ', CAD: 'C$', AUD: 'A$' }[currency] || currency || '₹');

  const handleReviewTransfer = (e) => {
    e.preventDefault();
    if (!fromAccount) {
      showToast('Please select a sender account', 'error');
      return;
    }
    if (!toAccount.trim()) {
      showToast('Please enter a valid recipient account number', 'error');
      return;
    }
    if (fromAccount === toAccount.trim()) {
      showToast('Sender and receiver accounts cannot be the same', 'error');
      return;
    }
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      showToast('Please enter a valid positive transfer amount', 'error');
      return;
    }
    if (numAmount > availableBalance) {
      showToast(`Insufficient balance! Available balance: ${currencySymbol(selectedAccObj?.currency || 'INR')}${availableBalance} ${selectedAccObj?.currency || 'INR'}`, 'error');
      return;
    }

    setShowConfirmModal(true);
  };

  const handleConfirmTransfer = async () => {
    setShowConfirmModal(false);
    setSubmitting(true);
    setLastTxResult(null);

    try {
      const payload = {
        fromAccountNumber: fromAccount,
        toAccountNumber: toAccount.trim(),
        amount: parseFloat(amount),
        description: description.trim() || 'Fund Transfer'
      };

      const res = await transferMoney(payload);
      setLastTxResult({
        success: true,
        data: res.data
      });

      showToast('Money transfer completed successfully!', 'success');

      // Reset form fields
      setToAccount('');
      setAmount('');
      setDescription('');

      // Refresh balances and transactions
      fetchAccounts();
      fetchTransactions();
    } catch (err) {
      console.error('Transfer failed:', err);
      const errMsg = err.response?.data?.message || err.response?.data || err.message || 'Transfer failed';
      setLastTxResult({
        success: false,
        error: typeof errMsg === 'string' ? errMsg : JSON.stringify(errMsg)
      });
      showToast(typeof errMsg === 'string' ? errMsg : 'Transfer failed', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ padding: '2rem', maxWidth: '1100px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      <div>
        <h1 style={{ fontSize: '1.75rem', fontWeight: '700', color: '#f8fafc' }}>Money Transfer & Transaction History</h1>
        <p style={{ color: '#94a3b8', fontSize: '0.9rem', marginTop: '0.2rem' }}>
          Transfer funds instantly between accounts and view your full transaction history
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
        {/* Transfer Form Box */}
        <div className="glass-card" style={{ padding: '1.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem', color: '#10b981' }}>
            <ArrowLeftRight size={24} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: '600', color: '#f8fafc' }}>Transfer Money</h2>
          </div>

          {accounts.length === 0 ? (
            <div style={{ padding: '1rem', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.2)', borderRadius: '10px', color: '#f87171' }}>
              No active bank accounts found. Please open an account first before transferring funds.
            </div>
          ) : (
            <form onSubmit={handleReviewTransfer} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.4rem' }}>From Account (Sender)</label>
                <select
                  className="input-field"
                  value={fromAccount}
                  onChange={(e) => setFromAccount(e.target.value)}
                  required
                >
                  {accounts.map((acc) => (
                    <option key={acc.accountNumber} value={acc.accountNumber}>
                      {acc.accountNumber} ({acc.type || acc.accountType}) - Balance: {currencySymbol(acc.currency || 'INR')}{acc.balance} {acc.currency || 'INR'}
                    </option>
                  ))}
                </select>
                {selectedAccObj && (
                  <div style={{ fontSize: '0.8rem', color: '#34d399', marginTop: '0.3rem' }}>
                    Available Balance: <strong>{currencySymbol(selectedAccObj.currency || 'INR')}{selectedAccObj.balance} {selectedAccObj.currency || 'INR'}</strong>
                  </div>
                )}
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.4rem' }}>To Account Number (Recipient)</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="Enter recipient account number"
                  value={toAccount}
                  onChange={(e) => setToAccount(e.target.value)}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.4rem' }}>Amount ({currencySymbol(selectedAccObj?.currency || 'INR')} {selectedAccObj?.currency || 'INR'})</label>
                <input
                  type="number"
                  className="input-field"
                  placeholder="1000"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                  min="1"
                  step="any"
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.4rem' }}>Description / Note</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="e.g. Rent Payment"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>

              {transferQuote && (
                <div style={{ padding: '1rem', background: 'rgba(56, 189, 248, 0.1)', border: '1px solid rgba(56, 189, 248, 0.25)', borderRadius: '10px', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                  <strong style={{ color: '#38bdf8' }}>International Transfer Preview</strong>
                  <div>You send: {currencySymbol(transferQuote.senderCurrency)}{transferQuote.senderAmount} {transferQuote.senderCurrency}</div>
                  <div>Exchange rate: 1 {transferQuote.senderCurrency} = {transferQuote.exchangeRate} {transferQuote.receiverCurrency}</div>
                  <div>Receiver gets: {currencySymbol(transferQuote.receiverCurrency)}{transferQuote.receiverAmount} {transferQuote.receiverCurrency}</div>
                  <div>Transfer fee: {currencySymbol(transferQuote.senderCurrency)}{transferQuote.fee} {transferQuote.senderCurrency}</div>
                  <small style={{ color: '#94a3b8' }}>Rate time: {transferQuote.exchangeRateTimestamp ? new Date(transferQuote.exchangeRateTimestamp).toLocaleString() : 'current'}</small>
                </div>
              )}

              <button type="submit" className="btn-primary" style={{ justifyContent: 'center', marginTop: '0.5rem' }} disabled={submitting}>
                {submitting ? 'Processing Transfer...' : (
                  <>
                    <Send size={18} /> Review Transfer
                  </>
                )}
              </button>
            </form>
          )}

          {/* Transfer Result Box */}
          {lastTxResult && (
            <div style={{
              marginTop: '1.5rem',
              padding: '1.25rem',
              borderRadius: '12px',
              background: lastTxResult.success ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
              border: `1px solid ${lastTxResult.success ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
              display: 'flex',
              alignItems: 'flex-start',
              gap: '1rem'
            }}>
              {lastTxResult.success ? <CheckCircle color="#34d399" size={24} /> : <AlertCircle color="#f87171" size={24} />}
              <div>
                <h4 style={{ fontSize: '1rem', fontWeight: '600', color: lastTxResult.success ? '#34d399' : '#f87171' }}>
                  {lastTxResult.success ? 'Transfer Confirmed & Completed!' : 'Transfer Failed'}
                </h4>
                {lastTxResult.success && lastTxResult.data && (
                  <div style={{ fontSize: '0.85rem', color: '#cbd5e1', marginTop: '0.4rem', display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                    <div><strong>Transaction ID:</strong> <span style={{ color: '#38bdf8', fontFamily: 'monospace' }}>{lastTxResult.data.transactionId || lastTxResult.data.id}</span></div>
                    <div><strong>From Account:</strong> {lastTxResult.data.fromAccountNumber}</div>
                    <div><strong>To Account:</strong> {lastTxResult.data.toAccountNumber}</div>
                    <div><strong>Sent:</strong> {currencySymbol(lastTxResult.data.senderCurrency)}{lastTxResult.data.senderAmount} {lastTxResult.data.senderCurrency}</div>
                    <div><strong>Received:</strong> {currencySymbol(lastTxResult.data.receiverCurrency)}{lastTxResult.data.receiverAmount} {lastTxResult.data.receiverCurrency}</div>
                    <div><strong>Exchange rate:</strong> {lastTxResult.data.exchangeRate || 1}</div>
                    <div><strong>Status:</strong> <span className="badge badge-success">{lastTxResult.data.status}</span></div>
                  </div>
                )}
                {!lastTxResult.success && (
                  <p style={{ fontSize: '0.85rem', color: '#cbd5e1', marginTop: '0.25rem' }}>
                    {lastTxResult.error}
                  </p>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Account Balance Summary Box */}
        <div className="glass-card" style={{ padding: '1.75rem', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: '#38bdf8' }}>
            <Wallet size={24} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: '600', color: '#f8fafc' }}>Account Overview</h2>
          </div>

          {accounts.map((acc) => (
            <div key={acc.accountNumber} style={{ background: '#0f172a', padding: '1.25rem', borderRadius: '12px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span className="badge badge-info">{acc.type || acc.accountType} · {acc.currency || 'INR'}</span>
                <span style={{ fontSize: '0.85rem', color: '#34d399', fontWeight: '600' }}>Active</span>
              </div>
              <div style={{ fontSize: '1.1rem', fontWeight: '700', color: '#f8fafc', margin: '0.5rem 0 0.25rem 0', fontFamily: 'monospace' }}>
                {acc.accountNumber}
              </div>
              <div style={{ fontSize: '1.25rem', fontWeight: '700', color: '#34d399' }}>
                {currencySymbol(acc.currency || 'INR')}{Number(acc.balance).toFixed(2)} {acc.currency || 'INR'}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Full Transaction History Table */}
      <div className="glass-card" style={{ padding: '1.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: '#a855f7' }}>
            <History size={24} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: '600', color: '#f8fafc' }}>Transaction History</h2>
          </div>
          <button className="btn-secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }} onClick={fetchTransactions}>
            <RefreshCw size={14} /> Refresh History
          </button>
        </div>

        {loadingTransactions ? (
          <p style={{ color: '#94a3b8' }}>Loading transaction history...</p>
        ) : transactions.length === 0 ? (
          <p style={{ color: '#94a3b8' }}>No transactions recorded for your accounts yet.</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.1)', color: '#64748b' }}>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Transaction ID</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Account Number</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Type</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Amount</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Status</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Date & Time</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => (
                  <tr key={tx.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.05)', color: '#f8fafc' }}>
                    <td style={{ padding: '0.75rem 0.5rem', fontFamily: 'monospace', color: '#38bdf8' }}>{tx.id}</td>
                    <td style={{ padding: '0.75rem 0.5rem', fontFamily: 'monospace' }}>{tx.accountNumber}</td>
                    <td style={{ padding: '0.75rem 0.5rem' }}>
                      <span className={`badge ${tx.type === 'DEBIT' ? 'badge-warning' : 'badge-success'}`}>
                        {tx.type}
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', fontWeight: '600', color: tx.type === 'DEBIT' ? '#f87171' : '#34d399' }}>
                      {tx.type === 'DEBIT' ? '-' : '+'}{currencySymbol(tx.senderCurrency || tx.receiverCurrency || 'INR')}{Number(tx.amount).toFixed(2)} {tx.senderCurrency || tx.receiverCurrency || 'INR'}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem' }}>
                      <span className="badge badge-success">{tx.status || 'COMPLETED'}</span>
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', color: '#94a3b8' }}>
                      {tx.createAt ? new Date(tx.createAt).toLocaleString() : 'N/A'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Confirmation Modal */}
      {showConfirmModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0, 0, 0, 0.75)',
          backdropFilter: 'blur(4px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div className="glass-card" style={{ width: '100%', maxWidth: '450px', padding: '2rem', background: '#1e293b' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: '#f59e0b', marginBottom: '1rem' }}>
              <ShieldAlert size={26} />
              <h3 style={{ fontSize: '1.2rem', fontWeight: '600', color: '#f8fafc' }}>Confirm Money Transfer</h3>
            </div>

            <p style={{ color: '#cbd5e1', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
              Please review the transfer details carefully before sending funds.
            </p>

            <div style={{ background: '#0f172a', padding: '1rem', borderRadius: '10px', fontSize: '0.85rem', color: '#f8fafc', display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '1.5rem' }}>
              <div><strong>From Account:</strong> <span style={{ color: '#38bdf8', fontFamily: 'monospace' }}>{fromAccount}</span></div>
              <div><strong>To Recipient Account:</strong> <span style={{ color: '#34d399', fontFamily: 'monospace' }}>{toAccount}</span></div>
              <div><strong>Transfer Amount:</strong> ${parseFloat(amount).toFixed(2)}</div>
              {description && <div><strong>Note:</strong> {description}</div>}
            </div>

            <div style={{ display: 'flex', gap: '1rem' }}>
              <button className="btn-secondary" style={{ flex: 1, justifyContent: 'center' }} onClick={() => setShowConfirmModal(false)}>
                Cancel
              </button>
              <button className="btn-primary" style={{ flex: 1, justifyContent: 'center' }} onClick={handleConfirmTransfer}>
                Confirm & Transfer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TransactionsPage;

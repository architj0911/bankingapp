import React, { useState, useEffect } from 'react';
import { createAccount, getAccountByNumber, getMyAccounts } from '../api/accountApi';
import { CreditCard, Search, PlusCircle, DollarSign, ShieldCheck, RefreshCw, Wallet } from 'lucide-react';

const AccountsPage = ({ showToast }) => {
  const currencySymbol = (currency) => ({ INR: '₹', USD: '$', EUR: '€', GBP: '£', JPY: '¥', AED: 'د.إ', CAD: 'C$', AUD: 'A$' }[currency] || currency || '₹');
  // My Accounts list state
  const [myAccounts, setMyAccounts] = useState([]);
  const [loadingAccounts, setLoadingAccounts] = useState(true);

  // Create Account Form state
  const [createForm, setCreateForm] = useState({
    accountType: 'SAVING',
    intialDeposit: 5000,
    currency: 'INR'
  });
  const [creating, setCreating] = useState(false);
  const [newAccountResult, setNewAccountResult] = useState(null);

  // Account Lookup state
  const [searchAccNum, setSearchAccNum] = useState('');
  const [searching, setSearching] = useState(false);
  const [searchResult, setSearchResult] = useState(null);

  const fetchMyAccounts = async () => {
    setLoadingAccounts(true);
    try {
      const res = await getMyAccounts();
      setMyAccounts(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Failed to load accounts:', err);
      setMyAccounts([]);
    } finally {
      setLoadingAccounts(false);
    }
  };

  useEffect(() => {
    fetchMyAccounts();
  }, []);

  const handleCreateAccount = async (e) => {
    e.preventDefault();
    if (createForm.intialDeposit < 0) {
      showToast('Initial deposit cannot be negative', 'error');
      return;
    }
    setCreating(true);
    try {
      const res = await createAccount({
        accountType: createForm.accountType,
        intialDeposit: Number(createForm.intialDeposit),
        currency: createForm.currency
      });
      showToast('Bank account created successfully!', 'success');
      setNewAccountResult(res.data);
      fetchMyAccounts();
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || err.message || 'Failed to create account', 'error');
    } finally {
      setCreating(false);
    }
  };

  const handleSearchAccount = async (e) => {
    e.preventDefault();
    if (!searchAccNum.trim()) {
      showToast('Please enter an account number to lookup', 'error');
      return;
    }
    setSearching(true);
    try {
      const res = await getAccountByNumber(searchAccNum.trim());
      setSearchResult(res.data);
      showToast('Account details retrieved!', 'success');
    } catch (err) {
      console.error(err);
      setSearchResult(null);
      showToast(err.response?.data?.message || 'Account not found', 'error');
    } finally {
      setSearching(false);
    }
  };

  return (
    <div style={{ padding: '2rem', maxWidth: '1100px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      <div>
        <h1 style={{ fontSize: '1.75rem', fontWeight: '700', color: '#f8fafc' }}>Bank Account Management</h1>
        <p style={{ color: '#94a3b8', fontSize: '0.9rem', marginTop: '0.2rem' }}>
          Open a new account, view your active accounts, or lookup account balances
        </p>
      </div>

      {/* My Accounts List */}
      <div className="glass-card" style={{ padding: '1.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: '#38bdf8' }}>
            <Wallet size={24} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: '600', color: '#f8fafc' }}>My Bank Accounts</h2>
          </div>
          <button className="btn-secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }} onClick={fetchMyAccounts}>
            <RefreshCw size={14} /> Refresh List
          </button>
        </div>

        {loadingAccounts ? (
          <p style={{ color: '#94a3b8' }}>Loading accounts...</p>
        ) : myAccounts.length === 0 ? (
          <p style={{ color: '#94a3b8' }}>No bank accounts opened yet. Open your first account below!</p>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
            {myAccounts.map((acc) => (
              <div key={acc.id || acc.accountNumber} style={{ background: '#0f172a', padding: '1.25rem', borderRadius: '12px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <span className="badge badge-info">{acc.type || acc.accountType} | {acc.currency || 'INR'}</span>
                  <span className="badge badge-success">{acc.status || 'ACTIVE'}</span>
                </div>
                <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Account Number</div>
                <div style={{ fontSize: '1.1rem', fontWeight: '700', color: '#38bdf8', fontFamily: 'monospace', marginBottom: '0.75rem' }}>
                  {acc.accountNumber}
                </div>
                <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Current Balance</div>
                <div style={{ fontSize: '1.3rem', fontWeight: '700', color: '#34d399' }}>
                  {acc.currency === 'USD' ? '$' : acc.currency === 'EUR' ? '€' : acc.currency === 'GBP' ? '£' : acc.currency === 'JPY' ? '¥' : '₹'}{Number(acc.balance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })} {acc.currency || 'INR'}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
        {/* Create Account Box */}
        <div className="glass-card" style={{ padding: '1.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem', color: '#38bdf8' }}>
            <PlusCircle size={24} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: '600', color: '#f8fafc' }}>Open New Bank Account</h2>
          </div>

          <form onSubmit={handleCreateAccount} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.4rem' }}>Account Currency</label>
              <select
                className="input-field"
                value={createForm.currency}
                onChange={(e) => setCreateForm({ ...createForm, currency: e.target.value })}
              >
                {['INR', 'USD', 'EUR', 'GBP', 'JPY', 'AED', 'CAD', 'AUD'].map((currency) => <option key={currency} value={currency}>{currency}</option>)}
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.4rem' }}>Account Type</label>
              <select
                className="input-field"
                value={createForm.accountType}
                onChange={(e) => setCreateForm({ ...createForm, accountType: e.target.value })}
              >
                <option value="SAVING">SAVING</option>
                <option value="CURRENT">CURRENT</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.4rem' }}>Initial Deposit ($ / ₹)</label>
              <input
                type="number"
                className="input-field"
                placeholder="5000"
                value={createForm.intialDeposit}
                onChange={(e) => setCreateForm({ ...createForm, intialDeposit: e.target.value })}
                required
                min="0"
              />
            </div>

            <button type="submit" className="btn-primary" style={{ justifyContent: 'center' }} disabled={creating}>
              {creating ? 'Creating Account...' : 'Open Account Now'}
            </button>
          </form>

          {newAccountResult && (
            <div style={{ marginTop: '1.5rem', padding: '1rem', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: '12px' }}>
              <div style={{ fontSize: '0.85rem', fontWeight: '600', color: '#34d399', marginBottom: '0.5rem' }}>Account Created Successfully!</div>
              <div style={{ fontSize: '0.85rem', color: '#f8fafc', display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
                <div><strong>Account Number:</strong> {newAccountResult.accountNumber}</div>
                <div><strong>Account Type:</strong> {newAccountResult.accountType}</div>
                  <div><strong>Currency:</strong> {newAccountResult.currency || 'INR'}</div>
                <div><strong>Balance:</strong> {currencySymbol(newAccountResult.currency || 'INR')}{newAccountResult.balance || newAccountResult.intialDeposit || 0} {newAccountResult.currency || 'INR'}</div>
                <div><strong>Status:</strong> {newAccountResult.status || 'ACTIVE'}</div>
              </div>
            </div>
          )}
        </div>

        {/* Lookup Account Box */}
        <div className="glass-card" style={{ padding: '1.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem', color: '#10b981' }}>
            <Search size={24} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: '600', color: '#f8fafc' }}>Account Lookup</h2>
          </div>

          <form onSubmit={handleSearchAccount} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: '#cbd5e1', marginBottom: '0.4rem' }}>Account Number</label>
              <input
                type="text"
                className="input-field"
                placeholder="Enter account number"
                value={searchAccNum}
                onChange={(e) => setSearchAccNum(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="btn-secondary" style={{ justifyContent: 'center' }} disabled={searching}>
              {searching ? 'Searching...' : 'Search Account'}
            </button>
          </form>

          {searchResult && (
            <div style={{ marginTop: '1.5rem', padding: '1rem', background: '#0f172a', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '12px' }}>
              <div style={{ fontSize: '0.85rem', fontWeight: '600', color: '#38bdf8', marginBottom: '0.5rem' }}>Account Details Found</div>
              <div style={{ fontSize: '0.85rem', color: '#f8fafc', display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
                <div><strong>Account Number:</strong> {searchResult.accountNumber}</div>
                <div><strong>Account Type:</strong> {searchResult.accountType}</div>
                <div><strong>Current Balance:</strong> {currencySymbol(searchResult.currency || 'INR')}{searchResult.balance} {searchResult.currency || 'INR'}</div>
                <div><strong>Status:</strong> <span className="badge badge-success">{searchResult.status || 'ACTIVE'}</span></div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AccountsPage;

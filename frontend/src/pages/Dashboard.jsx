import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getMyCustomer } from '../api/customerApi';
import { getMyAccounts } from '../api/accountApi';
import { getMyTransactions, getExchangeRate } from '../api/transactionApi';
import {
  UserCheck,
  CreditCard,
  ArrowLeftRight,
  ShieldCheck,
  PlusCircle,
  TrendingUp,
  CheckCircle,
  AlertTriangle,
  ArrowRight,
  RefreshCw,
  Wallet
} from 'lucide-react';

const Dashboard = ({ showToast }) => {
  const { user } = useAuth();
  const [customer, setCustomer] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [totalBalance, setTotalBalance] = useState(null);
  const [conversionUnavailable, setConversionUnavailable] = useState(false);
  const navigate = useNavigate();

  const fetchDashboardData = async () => {
    if (!user || !user.userId) return;
    setLoading(true);
    try {
      // Fetch Customer
      try {
        const custRes = await getMyCustomer();
        setCustomer(custRes.data);
      } catch (err) {
        setCustomer(null);
      }

      // Fetch Accounts
      try {
        const accRes = await getMyAccounts();
        const nextAccounts = Array.isArray(accRes.data) ? accRes.data : [];
        setAccounts(nextAccounts);
        setConversionUnavailable(false);
        try {
          const convertedBalances = await Promise.all(nextAccounts.map(async (account) => {
            const currency = account.currency || 'INR';
            const rateRes = await getExchangeRate(currency, 'INR');
            return Number(account.balance || 0) * Number(rateRes.data?.rate || 1);
          }));
          setTotalBalance(convertedBalances.reduce((sum, balance) => sum + balance, 0));
        } catch (conversionError) {
          console.error('Failed to convert portfolio balances:', conversionError);
          setTotalBalance(null);
          setConversionUnavailable(true);
        }
      } catch (err) {
        setAccounts([]);
        setTotalBalance(null);
        setConversionUnavailable(false);
      }

      // Fetch Transactions
      try {
        const txRes = await getMyTransactions();
        setTransactions(Array.isArray(txRes.data) ? txRes.data : []);
      } catch (err) {
        setTransactions([]);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, [user]);

  const currencySymbol = (currency) => ({ INR: '₹', USD: '$', EUR: '€', GBP: '£', JPY: '¥', AED: 'د.إ', CAD: 'C$', AUD: 'A$' }[currency] || currency || '₹');
  const accountCurrency = (accountNumber) => accounts.find((account) => account.accountNumber === accountNumber)?.currency || 'INR';

  const quickActions = [
    { title: 'Open Account', desc: 'Create a new savings or current bank account', icon: PlusCircle, path: '/accounts', color: '#38bdf8' },
    { title: 'Transfer Money', desc: 'Send funds securely with instant processing', icon: ArrowLeftRight, path: '/transactions', color: '#10b981' },
    { title: 'Customer Profile', desc: 'View details and complete KYC verification', icon: UserCheck, path: '/customer', color: '#f59e0b' },
    { title: 'View Accounts', desc: 'Check balances and account statuses', icon: CreditCard, path: '/accounts', color: '#a855f7' },
  ];

  return (
    <div style={{ padding: '2rem', maxWidth: '1100px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* Welcome Banner */}
      <div className="glass-card" style={{ padding: '2rem', background: 'linear-gradient(135deg, rgba(2, 132, 199, 0.2), rgba(15, 23, 42, 0.8))', border: '1px solid rgba(56, 189, 248, 0.2)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <span style={{ fontSize: '0.85rem', color: '#38bdf8', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Digital Banking Overview
            </span>
            <h1 style={{ fontSize: '2rem', fontWeight: '700', color: '#f8fafc', marginTop: '0.2rem' }}>
              Welcome back, {customer?.name || user?.username || 'Valued Customer'}!
            </h1>
            <p style={{ color: '#94a3b8', fontSize: '0.95rem', marginTop: '0.4rem' }}>
              Manage your accounts, transfer money, and keep your KYC status updated securely.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <div style={{ background: '#0f172a', padding: '0.75rem 1.25rem', borderRadius: '12px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
              <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>KYC Status</div>
              <div style={{ marginTop: '0.25rem' }}>
                {customer?.kycStatus === 'VERIFIED' ? (
                  <span className="badge badge-success"><CheckCircle size={12} /> VERIFIED</span>
                ) : (
                  <span className="badge badge-warning"><AlertTriangle size={12} /> {customer?.kycStatus || 'NOT VERIFIED'}</span>
                )}
              </div>
            </div>

            <div style={{ background: '#0f172a', padding: '0.75rem 1.25rem', borderRadius: '12px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
              <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Total Balance</div>
              <div style={{ fontSize: '1.2rem', fontWeight: '700', color: '#34d399', marginTop: '0.25rem' }}>
                {totalBalance === null ? 'Unavailable' : `₹${totalBalance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
              </div>
              <div style={{ fontSize: '0.68rem', color: '#7890a1', marginTop: '0.2rem' }}>
                {conversionUnavailable ? 'FX conversion unavailable' : 'INR equivalent'}
              </div>
            </div>

            <div style={{ background: '#0f172a', padding: '0.75rem 1.25rem', borderRadius: '12px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
              <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Total Accounts</div>
              <div style={{ fontSize: '1.2rem', fontWeight: '700', color: '#38bdf8', marginTop: '0.25rem' }}>
                {accounts.length}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions Grid */}
      <div>
        <h2 style={{ fontSize: '1.25rem', fontWeight: '600', color: '#f8fafc', marginBottom: '1rem' }}>
          Quick Actions
        </h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem' }}>
          {quickActions.map((action, idx) => {
            const Icon = action.icon;
            return (
              <div
                key={idx}
                className="glass-card"
                style={{ padding: '1.5rem', cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}
                onClick={() => navigate(action.path)}
              >
                <div style={{
                  width: '42px',
                  height: '42px',
                  borderRadius: '10px',
                  background: `${action.color}20`,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: action.color
                }}>
                  <Icon size={22} />
                </div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: '600', color: '#f8fafc' }}>{action.title}</h3>
                <p style={{ fontSize: '0.85rem', color: '#94a3b8', flex: 1 }}>{action.desc}</p>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem', fontWeight: '600', color: action.color }}>
                  Go to action <ArrowRight size={14} />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Customer Status & Info */}
      <div className="glass-card" style={{ padding: '1.75rem' }}>
        <h2 style={{ fontSize: '1.2rem', fontWeight: '600', color: '#f8fafc', marginBottom: '1rem' }}>
          Account Status Summary
        </h2>
        {customer ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.5rem' }}>
            <div>
              <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Customer ID</span>
              <p style={{ fontSize: '1.1rem', fontWeight: '600', color: '#38bdf8' }}>{customer.id}</p>
            </div>
            <div>
              <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>User ID</span>
              <p style={{ fontSize: '1.1rem', fontWeight: '600', color: '#38bdf8' }}>{customer.userId || user?.userId}</p>
            </div>
            <div>
              <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Customer Name</span>
              <p style={{ fontSize: '1.1rem', fontWeight: '600', color: '#f8fafc' }}>{customer.name}</p>
            </div>
            <div>
              <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Contact Email</span>
              <p style={{ fontSize: '1rem', color: '#cbd5e1' }}>{customer.email}</p>
            </div>
            <div>
              <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Phone Number</span>
              <p style={{ fontSize: '1rem', color: '#cbd5e1' }}>{customer.phone}</p>
            </div>
          </div>
        ) : (
          <div style={{ padding: '1rem', background: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.2)', borderRadius: '10px', color: '#fbbf24', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
            <div>
              <strong>No Customer Profile Created Yet.</strong>
              <p style={{ fontSize: '0.85rem', color: '#cbd5e1', marginTop: '0.2rem' }}>
                Please navigate to the Customer Profile section to link your identity before opening accounts or transferring funds.
              </p>
            </div>
            <button className="btn-primary" style={{ padding: '0.5rem 1rem', fontSize: '0.85rem', whiteSpace: 'nowrap' }} onClick={() => navigate('/customer')}>
              Create Profile
            </button>
          </div>
        )}
      </div>

      {/* Recent Transactions List */}
      <div className="glass-card" style={{ padding: '1.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <h2 style={{ fontSize: '1.2rem', fontWeight: '600', color: '#f8fafc' }}>
            Recent Transactions
          </h2>
          <button className="btn-secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }} onClick={fetchDashboardData}>
            <RefreshCw size={14} /> Refresh
          </button>
        </div>

        {transactions.length === 0 ? (
          <p style={{ color: '#94a3b8', fontSize: '0.9rem' }}>No recent transactions found.</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.1)', color: '#64748b' }}>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Transaction ID</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Account</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Type</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Amount</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Status</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Time</th>
                </tr>
              </thead>
              <tbody>
                {transactions.slice(0, 5).map((tx) => (
                  <tr key={tx.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.05)', color: '#f8fafc' }}>
                    <td style={{ padding: '0.75rem 0.5rem', fontFamily: 'monospace', color: '#38bdf8' }}>{tx.id}</td>
                    <td style={{ padding: '0.75rem 0.5rem' }}>{tx.accountNumber}</td>
                    <td style={{ padding: '0.75rem 0.5rem' }}>
                      <span className={`badge ${tx.type === 'DEBIT' ? 'badge-warning' : 'badge-success'}`}>
                        {tx.type}
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', fontWeight: '600', color: tx.type === 'DEBIT' ? '#f87171' : '#34d399' }}>
                      {tx.type === 'DEBIT' ? '-' : '+'}{currencySymbol(tx.senderCurrency || tx.receiverCurrency || accountCurrency(tx.accountNumber))}{Number(tx.amount).toFixed(2)} {tx.senderCurrency || tx.receiverCurrency || accountCurrency(tx.accountNumber)}
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
    </div>
  );
};

export default Dashboard;

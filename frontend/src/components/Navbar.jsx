import React from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, LogOut, User as UserIcon } from 'lucide-react';

const Navbar = () => {
  const { user, logout } = useAuth();

  return (
    <header style={{
      height: '64px',
      background: 'rgba(255, 255, 255, 0.82)',
      backdropFilter: 'blur(12px)',
      borderBottom: '1px solid rgba(15, 48, 72, 0.1)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 2rem',
      position: 'sticky',
      top: 0,
      zIndex: 100
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <div style={{
          width: '36px',
          height: '36px',
          borderRadius: '10px',
          background: 'linear-gradient(135deg, #0d9488, #38bdf8)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'white',
          fontWeight: 'bold',
          boxShadow: '0 8px 18px rgba(13, 148, 136, 0.2)'
        }}>
          <ShieldCheck size={22} />
        </div>
        <span style={{ fontSize: '1.2rem', fontWeight: '800', color: '#12344d', letterSpacing: '0.01em' }}>
          ApexBank Portal
        </span>
      </div>

      {user && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', background: '#eef8f6', padding: '0.4rem 0.9rem', borderRadius: '20px', border: '1px solid rgba(13,148,136,0.14)' }}>
            <UserIcon size={16} color="#0d9488" />
            <span style={{ fontSize: '0.875rem', fontWeight: '700', color: '#12344d' }}>{user.username}</span>
            <span className="badge badge-success" style={{ fontSize: '0.65rem' }}>ONLINE</span>
          </div>

          <button
            onClick={logout}
            className="btn-secondary"
            style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem' }}
          >
            <LogOut size={16} />
            Logout
          </button>
        </div>
      )}
    </header>
  );
};

export default Navbar;

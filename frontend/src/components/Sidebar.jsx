import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, UserCheck, CreditCard, ArrowLeftRight } from 'lucide-react';

const Sidebar = () => {
  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/customer', label: 'Customer Profile & KYC', icon: UserCheck },
    { path: '/accounts', label: 'Bank Accounts', icon: CreditCard },
    { path: '/transactions', label: 'Money Transfers', icon: ArrowLeftRight },
  ];

  return (
    <aside style={{
      width: '260px',
      background: 'rgba(249, 252, 251, 0.92)',
      borderRight: '1px solid rgba(15, 48, 72, 0.09)',
      padding: '1.5rem 1rem',
      display: 'flex',
      flexDirection: 'column',
      gap: '0.5rem',
      minHeight: 'calc(100vh - 64px)'
    }}>
      <div style={{ padding: '0 0.75rem 1rem', fontSize: '0.72rem', fontWeight: '800', color: '#7890a1', textTransform: 'uppercase', letterSpacing: '0.09em' }}>
        Navigation Menu
      </div>
      {navItems.map((item) => {
        const Icon = item.icon;
        return (
          <NavLink
            key={item.path}
            to={item.path}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: '0.85rem',
              padding: '0.8rem 1rem',
              borderRadius: '12px',
              textDecoration: 'none',
              fontWeight: '500',
              fontSize: '0.95rem',
              transition: 'all 0.2s ease',
              color: isActive ? '#087f75' : '#5e7485',
              background: isActive ? 'rgba(13, 148, 136, 0.1)' : 'transparent',
              border: isActive ? '1px solid rgba(13, 148, 136, 0.18)' : '1px solid transparent'
            })}
          >
            <Icon size={20} />
            {item.label}
          </NavLink>
        );
      })}
    </aside>
  );
};

export default Sidebar;

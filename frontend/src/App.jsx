import React, { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Toast from './components/Toast';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import ProtectedRoute from './components/ProtectedRoute';

import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import CustomerProfile from './pages/CustomerProfile';
import AccountsPage from './pages/AccountsPage';
import TransactionsPage from './pages/TransactionsPage';

const MainLayout = ({ children }) => {
  return (
    <div className="app-shell">
      <Navbar />
      <div className="app-body">
        <Sidebar />
        <main className="app-main">
          {children}
        </main>
      </div>
    </div>
  );
};

const OpeningSplash = ({ leaving }) => (
  <div className={`opening-splash${leaving ? ' opening-splash--leaving' : ''}`} role="status" aria-label="Opening ApexBank">
    <div className="opening-splash__grid" />
    <div className="opening-splash__mark">
      <ShieldCheck size={34} strokeWidth={1.8} />
    </div>
    <div className="opening-splash__wordmark">ApexBank</div>
    <div className="opening-splash__caption">Banking, with a clearer horizon.</div>
    <div className="opening-splash__line"><span /></div>
  </div>
);

function AppContent() {
  const [toast, setToast] = useState(null);
  const [splashVisible, setSplashVisible] = useState(true);
  const [splashLeaving, setSplashLeaving] = useState(false);

  useEffect(() => {
    const leaveTimer = setTimeout(() => setSplashLeaving(true), 1450);
    const removeTimer = setTimeout(() => setSplashVisible(false), 2050);
    return () => {
      clearTimeout(leaveTimer);
      clearTimeout(removeTimer);
    };
  }, []);

  const showToast = (message, type = 'info') => {
    setToast({ message, type });
  };

  return (
    <>
      {splashVisible && <OpeningSplash leaving={splashLeaving} />}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      <Routes>
        {/* Public Auth Routes */}
        <Route path="/login" element={<Login showToast={showToast} />} />
        <Route path="/register" element={<Register showToast={showToast} />} />

        {/* Protected Application Routes */}
        <Route element={<ProtectedRoute />}>
          <Route
            path="/dashboard"
            element={
              <MainLayout>
                <Dashboard showToast={showToast} />
              </MainLayout>
            }
          />
          <Route
            path="/customer"
            element={
              <MainLayout>
                <CustomerProfile showToast={showToast} />
              </MainLayout>
            }
          />
          <Route
            path="/accounts"
            element={
              <MainLayout>
                <AccountsPage showToast={showToast} />
              </MainLayout>
            }
          />
          <Route
            path="/transactions"
            element={
              <MainLayout>
                <TransactionsPage showToast={showToast} />
              </MainLayout>
            }
          />
        </Route>

        {/* Catch-all redirect */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </>
  );
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </Router>
  );
}

export default App;

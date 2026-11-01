import React, { createContext, useContext, useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('bank_access_token') || null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
      try {
        const decoded = jwtDecode(token);
        const currentTime = Date.now() / 1000;
        
        if (decoded.exp && decoded.exp < currentTime) {
          logout();
        } else {
          setUser({
            userId: decoded.userId ?? decoded.user_id ?? decoded.sub,
            username: decoded.sub,
            authorities: decoded.authorities || [],
          });
        }
      } catch (err) {
        console.error('Invalid JWT token:', err);
        logout();
      }
    } else {
      setUser(null);
    }
    setLoading(false);
  }, [token]);

  const loginToken = (accessToken) => {
    localStorage.setItem('bank_access_token', accessToken);
    setToken(accessToken);
  };

  const logout = () => {
    localStorage.removeItem('bank_access_token');
    localStorage.removeItem('bank_user');
    localStorage.removeItem('active_account_number');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        userId: user?.userId,
        username: user?.username,
        isAuthenticated: !!token && !!user,
        login: loginToken,
        loginToken,
        logout,
        loading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

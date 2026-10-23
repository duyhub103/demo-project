import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, NavLink, useNavigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';

import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import MedicinePage from './pages/MedicinePage';
import ImportOrderPage from './pages/ImportOrderPage';
import ExportOrderPage from './pages/ExportOrderPage';
import TransferOrderPage from './pages/TransferOrderPage';
import UserManagementPage from './pages/UserManagementPage';

import {
  LayoutDashboard,
  Pill,
  ArrowDownToLine,
  ArrowUpFromLine,
  ArrowLeftRight,
  Users,
  LogOut,
  Warehouse
} from 'lucide-react';

function ProtectedLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getRoleLabel = (role) => {
    switch (role) {
      case 'ADMIN': return 'Quản trị viên';
      case 'WAREHOUSE_MANAGER': return 'Quản lý kho';
      case 'WAREHOUSE_STAFF': return 'Nhân viên kho';
      default: return role;
    }
  };

  return (
    <div className="app-layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-logo">
          <h2>💊 Pharma WMS</h2>
          <p>Hệ thống Quản lý Kho Dược FEFO</p>
        </div>

        <nav className="sidebar-nav">
          <div className="nav-section-title">Nghiệp vụ kho</div>
          <NavLink to="/" end className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <LayoutDashboard size={18} />
            <span>Tổng quan kho</span>
          </NavLink>
          <NavLink to="/medicines" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <Pill size={18} />
            <span>Danh mục thuốc</span>
          </NavLink>
          <NavLink to="/import" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <ArrowDownToLine size={18} />
            <span>Nhập kho</span>
          </NavLink>
          <NavLink to="/export" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <ArrowUpFromLine size={18} />
            <span>Xuất kho (FEFO)</span>
          </NavLink>
          <NavLink to="/transfer" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <ArrowLeftRight size={18} />
            <span>Điều chuyển kho</span>
          </NavLink>

          {user.role === 'ADMIN' && (
            <>
              <div className="nav-section-title">Hệ thống</div>
              <NavLink to="/users" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
                <Users size={18} />
                <span>Quản lý người dùng</span>
              </NavLink>
            </>
          )}
        </nav>

        {/* User Info & Logout */}
        <div className="sidebar-user">
          <div className="user-info">
            <div className="user-avatar">
              {user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
            </div>
            <div style={{ overflow: 'hidden' }}>
              <div className="user-name" title={user.fullName}>{user.fullName}</div>
              <div className="user-role">
                <span className={`role-badge role-${user.role}`}>
                  {getRoleLabel(user.role)}
                </span>
              </div>
              {user.warehouseName && (
                <div style={{ fontSize: '11px', color: 'var(--text-secondary)', marginTop: '2px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Warehouse size={12} />
                  <span>{user.warehouseName}</span>
                </div>
              )}
            </div>
          </div>
          <button className="logout-btn" onClick={handleLogout}>
            <LogOut size={16} />
            <span>Đăng xuất</span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="main-content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/medicines" element={<MedicinePage />} />
          <Route path="/import" element={<ImportOrderPage />} />
          <Route path="/export" element={<ExportOrderPage />} />
          <Route path="/transfer" element={<TransferOrderPage />} />
          {user.role === 'ADMIN' && (
            <Route path="/users" element={<UserManagementPage />} />
          )}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

function PublicRoute({ children }) {
  const { user } = useAuth();
  if (user) {
    return <Navigate to="/" replace />;
  }
  return children;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster position="top-right" />
        <Routes>
          <Route
            path="/login"
            element={
              <PublicRoute>
                <LoginPage />
              </PublicRoute>
            }
          />
          <Route path="/*" element={<ProtectedLayout />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

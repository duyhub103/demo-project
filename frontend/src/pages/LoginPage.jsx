import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authAPI } from '../api';
import toast from 'react-hot-toast';

export default function LoginPage() {
  const { login } = useAuth();
  const [form, setForm] = useState({ username: '', password: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authAPI.login(form);
      login(res.data);
      toast.success(`Xin chào, ${res.data.fullName}!`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Tên đăng nhập hoặc mật khẩu không đúng');
    } finally {
      setLoading(false);
    }
  };

  const fillDemo = (username, password) => {
    setForm({ username, password });
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">
          <div className="icon">💊</div>
          <h1>Pharma WMS</h1>
          <p>Hệ thống Quản lý Kho Dược phẩm FEFO</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Tên đăng nhập</label>
            <input
              id="username-input"
              type="text"
              className="form-control"
              placeholder="Nhập tên đăng nhập"
              value={form.username}
              onChange={e => setForm(f => ({...f, username: e.target.value}))}
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">Mật khẩu</label>
            <input
              id="password-input"
              type="password"
              className="form-control"
              placeholder="Nhập mật khẩu"
              value={form.password}
              onChange={e => setForm(f => ({...f, password: e.target.value}))}
              required
            />
          </div>
          <button id="login-btn" type="submit" className="login-btn" disabled={loading}>
            {loading ? '⏳ Đang đăng nhập...' : '🔐 Đăng nhập'}
          </button>
        </form>

        <div className="demo-accounts">
          <h4>🧪 Tài khoản demo (click để điền)</h4>
          <div onClick={() => fillDemo('admin', 'Admin@123')}>
            👑 Admin: <strong>admin</strong> / Admin@123
          </div>
          <div onClick={() => fillDemo('manager_hn', 'Manager@123')}>
            📋 Quản lý KHN: <strong>manager_hn</strong> / Manager@123
          </div>
          <div onClick={() => fillDemo('staff_hn', 'Staff@123')}>
            👷 NV Kho HN: <strong>staff_hn</strong> / Staff@123
          </div>
          <div onClick={() => fillDemo('manager_hcm', 'Manager@123')}>
            📋 Quản lý K.HCM: <strong>manager_hcm</strong> / Manager@123
          </div>
        </div>
      </div>
    </div>
  );
}

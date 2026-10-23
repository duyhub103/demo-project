import React, { useState, useEffect } from 'react';
import { userAPI, catalogAPI } from '../api';
import toast from 'react-hot-toast';

const ROLE_LABELS = { ADMIN: 'Quản trị viên', WAREHOUSE_MANAGER: 'Quản lý kho', WAREHOUSE_STAFF: 'Nhân viên kho' };

export default function UserManagementPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [warehouses, setWarehouses] = useState([]);
  const [form, setForm] = useState({ username:'', password:'', fullName:'', email:'', role:'WAREHOUSE_STAFF', warehouseId:'' });

  useEffect(() => { loadUsers(); loadWarehouses(); }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const res = await userAPI.getAll();
      setUsers(Array.isArray(res.data) ? res.data : (res.data?.content || []));
    } catch (e) {
      console.error('Lỗi tải danh sách người dùng:', e);
      toast.error(e.response?.data?.message || 'Không thể tải danh sách người dùng');
    } finally {
      setLoading(false);
    }
  };

  const loadWarehouses = async () => {
    try {
      const res = await catalogAPI.getWarehouses();
      setWarehouses(res.data);
    } catch (e) {}
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await userAPI.create({ ...form, warehouseId: form.warehouseId ? Number(form.warehouseId) : null });
      toast.success('Tạo tài khoản thành công!');
      setShowModal(false);
      loadUsers();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Lỗi tạo tài khoản');
    }
  };

  const toggleStatus = async (id) => {
    try {
      await userAPI.toggleStatus(id);
      toast.success('Đã cập nhật trạng thái tài khoản');
      loadUsers();
    } catch (e) { toast.error('Lỗi'); }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">👥 Quản lý Người dùng</h1>
          <p className="page-subtitle">Phân quyền theo vai trò và kiểm soát phạm vi kho</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>＋ Tạo tài khoản</button>
      </div>

      {/* Permission diagram */}
      <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:12,marginBottom:20}}>
        {[
          { role:'ADMIN', icon:'👑', color:'#f87171', desc:'Toàn quyền hệ thống, quản lý tài khoản, xem tất cả kho' },
          { role:'WAREHOUSE_MANAGER', icon:'📋', color:'#818cf8', desc:'Duyệt phiếu nhập/xuất, kiểm kê, chuyển kho trong phạm vi kho' },
          { role:'WAREHOUSE_STAFF', icon:'👷', color:'#34d399', desc:'Tạo phiếu nhập/xuất, kiểm kê trong kho được gán' },
        ].map(r => (
          <div key={r.role} className="card" style={{border:`1px solid ${r.color}30`}}>
            <div style={{fontSize:24,marginBottom:8}}>{r.icon}</div>
            <div style={{fontWeight:700,color:r.color,marginBottom:4}}>{ROLE_LABELS[r.role]}</div>
            <div style={{fontSize:12,color:'var(--text-secondary)'}}>{r.desc}</div>
          </div>
        ))}
      </div>

      {loading ? <div className="loading"><div className="spinner"/></div> : (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr><th>Tên đăng nhập</th><th>Họ tên</th><th>Email</th><th>Vai trò</th><th>Kho</th><th>Trạng thái</th><th></th></tr>
              </thead>
              <tbody>
                {users.map(u => (
                  <tr key={u.id}>
                    <td><code style={{fontSize:12}}>{u.username}</code></td>
                    <td><strong>{u.fullName}</strong></td>
                    <td style={{fontSize:12}}>{u.email || '—'}</td>
                    <td><span className={`role-badge role-${u.role}`}>{ROLE_LABELS[u.role]}</span></td>
                    <td style={{fontSize:12}}>{u.warehouse?.name || <span style={{color:'#818cf8'}}>Toàn hệ thống</span>}</td>
                    <td>{u.active ? <span className="badge badge-approved">Hoạt động</span> : <span className="badge badge-rejected">Vô hiệu</span>}</td>
                    <td>
                      <button className={`btn btn-sm ${u.active ? 'btn-danger' : 'btn-success'}`} onClick={() => toggleStatus(u.id)}>
                        {u.active ? '🔒 Khóa' : '🔓 Mở'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setShowModal(false)}>
          <div className="modal">
            <div className="modal-header">
              <h2 className="modal-title">＋ Tạo tài khoản mới</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label">Tên đăng nhập *</label>
                  <input className="form-control" value={form.username} onChange={e => setForm(f=>({...f,username:e.target.value}))} required minLength={4} />
                </div>
                <div className="form-group">
                  <label className="form-label">Mật khẩu *</label>
                  <input type="password" className="form-control" value={form.password} onChange={e => setForm(f=>({...f,password:e.target.value}))} required minLength={6} />
                </div>
                <div className="form-group">
                  <label className="form-label">Họ tên *</label>
                  <input className="form-control" value={form.fullName} onChange={e => setForm(f=>({...f,fullName:e.target.value}))} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Email</label>
                  <input type="email" className="form-control" value={form.email} onChange={e => setForm(f=>({...f,email:e.target.value}))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Vai trò *</label>
                  <select className="form-control" value={form.role} onChange={e => setForm(f=>({...f,role:e.target.value}))}>
                    <option value="ADMIN">Quản trị viên</option>
                    <option value="WAREHOUSE_MANAGER">Quản lý kho</option>
                    <option value="WAREHOUSE_STAFF">Nhân viên kho</option>
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Kho phụ trách</label>
                  <select className="form-control" value={form.warehouseId} onChange={e => setForm(f=>({...f,warehouseId:e.target.value}))}
                    disabled={form.role === 'ADMIN'}>
                    <option value="">-- Toàn hệ thống (Admin) --</option>
                    {warehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
                  </select>
                </div>
              </div>
              <div className="flex justify-between mt-4">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Hủy</button>
                <button type="submit" className="btn btn-primary">Tạo tài khoản</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

import React, { useState, useEffect } from 'react';
import { catalogAPI } from '../api';
import toast from 'react-hot-toast';

export default function MedicinePage() {
  const [medicines, setMedicines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [form, setForm] = useState({
    name:'', code:'', activeIngredient:'', dosageForm:'', unit:'', concentration:'',
    manufacturer:'', minStockLevel:100, unitPrice:''
  });

  useEffect(() => { loadMedicines(); }, [keyword]);

  const loadMedicines = async () => {
    setLoading(true);
    try {
      const res = await catalogAPI.getMedicines(keyword);
      setMedicines(res.data.content || res.data);
    } catch (e) {} finally { setLoading(false); }
  };

  const openCreate = () => {
    setEditItem(null);
    setForm({ name:'', code:'', activeIngredient:'', dosageForm:'', unit:'Viên', concentration:'', manufacturer:'', minStockLevel:100, unitPrice:'' });
    setShowModal(true);
  };

  const openEdit = (m) => {
    setEditItem(m);
    setForm({ ...m, unitPrice: m.unitPrice || '' });
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = { ...form, minStockLevel: Number(form.minStockLevel), unitPrice: form.unitPrice ? Number(form.unitPrice) : null };
      if (editItem) {
        await catalogAPI.updateMedicine(editItem.id, payload);
        toast.success('Cập nhật thuốc thành công!');
      } else {
        await catalogAPI.createMedicine(payload);
        toast.success('Thêm thuốc thành công!');
      }
      setShowModal(false);
      loadMedicines();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Lỗi');
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">💊 Danh mục thuốc</h1>
          <p className="page-subtitle">Quản lý thông tin thuốc và mức tồn tối thiểu</p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>＋ Thêm thuốc</button>
      </div>

      <div className="card" style={{marginBottom:16}}>
        <input
          id="medicine-search"
          className="form-control"
          placeholder="🔍 Tìm kiếm theo tên, mã, hoạt chất..."
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
          style={{maxWidth:400}}
        />
      </div>

      {loading ? <div className="loading"><div className="spinner"/></div> : (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Mã</th><th>Tên thuốc</th><th>Hoạt chất</th><th>Dạng bào chế</th>
                  <th>Đơn vị</th><th>Tồn tối thiểu</th><th>Đơn giá</th><th>Trạng thái</th><th></th>
                </tr>
              </thead>
              <tbody>
                {medicines.map(m => (
                  <tr key={m.id}>
                    <td><code style={{fontSize:11}}>{m.code}</code></td>
                    <td><strong>{m.name}</strong><div style={{fontSize:11,color:'var(--text-secondary)'}}>{m.concentration}</div></td>
                    <td style={{fontSize:13}}>{m.activeIngredient}</td>
                    <td style={{fontSize:13}}>{m.dosageForm}</td>
                    <td>{m.unit}</td>
                    <td><strong>{m.minStockLevel?.toLocaleString()}</strong></td>
                    <td>{m.unitPrice ? `${Number(m.unitPrice).toLocaleString()}đ` : '—'}</td>
                    <td>{m.active ? <span className="badge badge-approved">Hoạt động</span> : <span className="badge badge-rejected">Ngừng</span>}</td>
                    <td><button className="btn btn-secondary btn-sm" onClick={() => openEdit(m)}>✏️</button></td>
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
              <h2 className="modal-title">{editItem ? '✏️ Cập nhật thuốc' : '＋ Thêm thuốc mới'}</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label">Tên thuốc *</label>
                  <input className="form-control" value={form.name} onChange={e => setForm(f=>({...f,name:e.target.value}))} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Mã thuốc *</label>
                  <input className="form-control" value={form.code} onChange={e => setForm(f=>({...f,code:e.target.value}))} required disabled={!!editItem} />
                </div>
                <div className="form-group">
                  <label className="form-label">Hoạt chất</label>
                  <input className="form-control" value={form.activeIngredient} onChange={e => setForm(f=>({...f,activeIngredient:e.target.value}))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Dạng bào chế</label>
                  <input className="form-control" placeholder="Viên nén, Viên nang, Gói..." value={form.dosageForm} onChange={e => setForm(f=>({...f,dosageForm:e.target.value}))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Đơn vị tính *</label>
                  <input className="form-control" value={form.unit} onChange={e => setForm(f=>({...f,unit:e.target.value}))} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Hàm lượng</label>
                  <input className="form-control" placeholder="500mg, 1g..." value={form.concentration} onChange={e => setForm(f=>({...f,concentration:e.target.value}))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Nhà sản xuất</label>
                  <input className="form-control" value={form.manufacturer} onChange={e => setForm(f=>({...f,manufacturer:e.target.value}))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Tồn tối thiểu *</label>
                  <input type="number" className="form-control" min="0" value={form.minStockLevel} onChange={e => setForm(f=>({...f,minStockLevel:e.target.value}))} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Đơn giá (VNĐ)</label>
                  <input type="number" className="form-control" min="0" value={form.unitPrice} onChange={e => setForm(f=>({...f,unitPrice:e.target.value}))} />
                </div>
              </div>
              <div className="flex justify-between mt-4">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Hủy</button>
                <button type="submit" className="btn btn-primary">{editItem ? 'Cập nhật' : 'Thêm thuốc'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

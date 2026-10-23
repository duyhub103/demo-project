import React, { useState, useEffect } from 'react';
import { importAPI, catalogAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const STATUS_LABELS = {
  DRAFT: 'Nháp', PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối'
};

export default function ImportOrderPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [warehouses, setWarehouses] = useState([]);
  const [medicines, setMedicines] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [form, setForm] = useState({
    warehouseId: user.warehouseId || '',
    supplierId: '',
    note: '',
    details: [{ medicineId: '', batchNumber: '', manufactureDate: '', expiryDate: '', quantity: 1, unitPrice: '' }]
  });

  useEffect(() => {
    loadOrders();
    loadCatalog();
  }, [page]);

  const loadOrders = async () => {
    setLoading(true);
    try {
      const res = await importAPI.list(user.warehouseId, null, page);
      setOrders(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (e) {
      toast.error('Lỗi tải danh sách phiếu nhập');
    } finally {
      setLoading(false);
    }
  };

  const loadCatalog = async () => {
    try {
      const [wRes, mRes, sRes] = await Promise.all([
        catalogAPI.getWarehouses(),
        catalogAPI.getMedicines(''),
        catalogAPI.getSuppliers(),
      ]);
      setWarehouses(wRes.data);
      setMedicines(mRes.data.content || mRes.data);
      setSuppliers(sRes.data);
    } catch (e) {}
  };

  const addDetail = () => {
    setForm(f => ({...f, details: [...f.details, { medicineId: '', batchNumber: '', manufactureDate: '', expiryDate: '', quantity: 1, unitPrice: '' }]}));
  };

  const removeDetail = (i) => {
    setForm(f => ({...f, details: f.details.filter((_,idx) => idx !== i)}));
  };

  const updateDetail = (i, field, value) => {
    setForm(f => ({...f, details: f.details.map((d,idx) => idx === i ? {...d, [field]: value} : d)}));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        warehouseId: Number(form.warehouseId),
        supplierId: form.supplierId ? Number(form.supplierId) : null,
        note: form.note,
        details: form.details.map(d => ({
          medicineId: Number(d.medicineId),
          batchNumber: d.batchNumber,
          manufactureDate: d.manufactureDate,
          expiryDate: d.expiryDate,
          quantity: Number(d.quantity),
          unitPrice: d.unitPrice ? Number(d.unitPrice) : null,
        }))
      };
      await importAPI.create(payload);
      toast.success('Tạo phiếu nhập thành công!');
      setShowModal(false);
      loadOrders();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Lỗi tạo phiếu nhập');
    }
  };

  const handleApprove = async (id, approved) => {
    const rejectReason = approved ? null : prompt('Lý do từ chối:');
    if (!approved && !rejectReason) return;
    try {
      await importAPI.approve(id, approved, rejectReason);
      toast.success(approved ? '✅ Đã duyệt phiếu nhập, tồn kho đã cập nhật!' : '❌ Đã từ chối phiếu nhập');
      loadOrders();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Lỗi xử lý phiếu nhập');
    }
  };

  const canApprove = ['ADMIN','WAREHOUSE_MANAGER'].includes(user.role);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">📥 Phiếu Nhập Kho</h1>
          <p className="page-subtitle">Tạo và quản lý phiếu nhập hàng</p>
        </div>
        <button id="create-import-btn" className="btn btn-primary" onClick={() => setShowModal(true)}>
          ＋ Tạo phiếu nhập
        </button>
      </div>

      {loading ? (
        <div className="loading"><div className="spinner"/><span>Đang tải...</span></div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Mã phiếu</th>
                  <th>Kho</th>
                  <th>Nhà CC</th>
                  <th>Người tạo</th>
                  <th>Trạng thái</th>
                  <th>Ngày tạo</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {orders.length === 0 ? (
                  <tr><td colSpan="7"><div className="empty-state">Chưa có phiếu nhập nào</div></td></tr>
                ) : orders.map(o => (
                  <tr key={o.id}>
                    <td><code style={{fontSize:12}}>{o.orderCode}</code></td>
                    <td>{o.warehouseName}</td>
                    <td>{o.supplierName || <span className="text-muted">—</span>}</td>
                    <td>{o.createdByName}</td>
                    <td>
                      <span className={`badge badge-${o.status.toLowerCase()}`}>
                        {STATUS_LABELS[o.status]}
                      </span>
                    </td>
                    <td style={{fontSize:12}}>{new Date(o.createdAt).toLocaleString('vi-VN')}</td>
                    <td>
                      {o.status === 'PENDING' && canApprove && (
                        <div className="flex gap-2">
                          <button className="btn btn-success btn-sm" onClick={() => handleApprove(o.id, true)}>✓ Duyệt</button>
                          <button className="btn btn-danger btn-sm" onClick={() => handleApprove(o.id, false)}>✗ Từ chối</button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {totalPages > 1 && (
            <div className="pagination">
              <button className="page-btn" disabled={page === 0} onClick={() => setPage(p => p-1)}>‹</button>
              <span style={{fontSize:13,color:'var(--text-secondary)'}}>{page+1}/{totalPages}</span>
              <button className="page-btn" disabled={page >= totalPages-1} onClick={() => setPage(p => p+1)}>›</button>
            </div>
          )}
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setShowModal(false)}>
          <div className="modal">
            <div className="modal-header">
              <h2 className="modal-title">📥 Tạo phiếu nhập kho</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label">Kho nhập *</label>
                  <select
                    className="form-control"
                    value={form.warehouseId}
                    onChange={e => setForm(f => ({...f, warehouseId: e.target.value}))}
                    required
                    disabled={!!user.warehouseId}
                  >
                    <option value="">-- Chọn kho --</option>
                    {warehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Nhà cung cấp</label>
                  <select
                    className="form-control"
                    value={form.supplierId}
                    onChange={e => setForm(f => ({...f, supplierId: e.target.value}))}
                  >
                    <option value="">-- Chọn nhà CC --</option>
                    {suppliers.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Ghi chú</label>
                <input type="text" className="form-control" value={form.note} onChange={e => setForm(f => ({...f, note: e.target.value}))} />
              </div>

              <div style={{marginBottom:12}}>
                <div className="flex items-center justify-between mb-4">
                  <h4 style={{fontSize:14,fontWeight:600}}>Chi tiết thuốc nhập</h4>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={addDetail}>＋ Thêm dòng</button>
                </div>
                {form.details.map((d, i) => (
                  <div key={i} style={{background:'var(--bg-dark)',borderRadius:8,padding:12,marginBottom:8}}>
                    <div className="form-grid" style={{gridTemplateColumns:'2fr 1fr 1fr'}}>
                      <div className="form-group" style={{marginBottom:8}}>
                        <label className="form-label">Thuốc *</label>
                        <select className="form-control" value={d.medicineId} onChange={e => updateDetail(i,'medicineId',e.target.value)} required>
                          <option value="">-- Chọn thuốc --</option>
                          {medicines.map(m => <option key={m.id} value={m.id}>{m.name} ({m.code})</option>)}
                        </select>
                      </div>
                      <div className="form-group" style={{marginBottom:8}}>
                        <label className="form-label">Số lô *</label>
                        <input type="text" className="form-control" placeholder="VD: LOT-2024-001" value={d.batchNumber} onChange={e => updateDetail(i,'batchNumber',e.target.value)} required />
                      </div>
                      <div className="form-group" style={{marginBottom:8}}>
                        <label className="form-label">SL nhập *</label>
                        <input type="number" className="form-control" min="1" value={d.quantity} onChange={e => updateDetail(i,'quantity',e.target.value)} required />
                      </div>
                    </div>
                    <div className="form-grid" style={{gridTemplateColumns:'1fr 1fr 1fr auto'}}>
                      <div className="form-group" style={{marginBottom:0}}>
                        <label className="form-label">Ngày SX *</label>
                        <input type="date" className="form-control" value={d.manufactureDate} onChange={e => updateDetail(i,'manufactureDate',e.target.value)} required />
                      </div>
                      <div className="form-group" style={{marginBottom:0}}>
                        <label className="form-label">Hạn dùng *</label>
                        <input type="date" className="form-control" value={d.expiryDate} onChange={e => updateDetail(i,'expiryDate',e.target.value)} required />
                      </div>
                      <div className="form-group" style={{marginBottom:0}}>
                        <label className="form-label">Đơn giá (VNĐ)</label>
                        <input type="number" className="form-control" placeholder="0" value={d.unitPrice} onChange={e => updateDetail(i,'unitPrice',e.target.value)} />
                      </div>
                      {form.details.length > 1 && (
                        <button type="button" className="btn btn-danger btn-sm" style={{alignSelf:'flex-end',marginBottom:0}} onClick={() => removeDetail(i)}>✕</button>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              <div className="flex justify-between mt-4">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Hủy</button>
                <button type="submit" className="btn btn-primary">📥 Tạo phiếu nhập</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

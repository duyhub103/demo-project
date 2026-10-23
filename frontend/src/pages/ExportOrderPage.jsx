import React, { useState, useEffect } from 'react';
import { exportAPI, catalogAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const STATUS_LABELS = {
  DRAFT: 'Nháp', PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối', COMPLETED: 'Hoàn thành'
};

export default function ExportOrderPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [warehouses, setWarehouses] = useState([]);
  const [medicines, setMedicines] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [form, setForm] = useState({
    warehouseId: user.warehouseId || '',
    recipient: '',
    note: '',
    details: [{ medicineId: '', quantity: 1 }]
  });

  useEffect(() => { loadOrders(); loadCatalog(); }, [page]);

  const loadOrders = async () => {
    setLoading(true);
    try {
      const res = await exportAPI.list(user.warehouseId, null, page);
      setOrders(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (e) {
      toast.error('Lỗi tải danh sách phiếu xuất');
    } finally { setLoading(false); }
  };

  const loadCatalog = async () => {
    try {
      const [wRes, mRes] = await Promise.all([catalogAPI.getWarehouses(), catalogAPI.getMedicines('')]);
      setWarehouses(wRes.data);
      setMedicines(mRes.data.content || mRes.data);
    } catch (e) {}
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        warehouseId: Number(form.warehouseId),
        recipient: form.recipient,
        note: form.note,
        details: form.details.map(d => ({
          medicineId: Number(d.medicineId),
          quantity: Number(d.quantity),
        }))
      };
      const res = await exportAPI.create(payload);
      toast.success(
        `✅ Tạo phiếu xuất theo FEFO thành công!\n` +
        `Hệ thống đã tự động chọn ${res.data.details?.length || 0} lô theo hạn dùng gần nhất.`,
        { duration: 5000 }
      );
      setShowModal(false);
      loadOrders();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Lỗi tạo phiếu xuất');
    }
  };

  const handleApprove = async (id, approved) => {
    try {
      await exportAPI.approve(id, approved);
      toast.success(approved ? '✅ Đã duyệt phiếu xuất!' : '❌ Đã từ chối');
      loadOrders();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Lỗi');
    }
  };

  const canApprove = ['ADMIN','WAREHOUSE_MANAGER'].includes(user.role);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">📤 Phiếu Xuất Kho</h1>
          <p className="page-subtitle">
            Xuất kho tự động theo cơ chế{' '}
            <span className="fefo-badge">⚡ FEFO - First Expired, First Out</span>
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          ＋ Tạo phiếu xuất
        </button>
      </div>

      {/* FEFO Explanation Banner */}
      <div style={{background:'linear-gradient(135deg,rgba(99,102,241,0.1),rgba(14,165,233,0.1))',border:'1px solid rgba(99,102,241,0.2)',borderRadius:12,padding:'14px 20px',marginBottom:20,display:'flex',alignItems:'center',gap:12}}>
        <span style={{fontSize:24}}>🔬</span>
        <div>
          <div style={{fontWeight:700,fontSize:14}}>Cơ chế FEFO (First Expired, First Out)</div>
          <div style={{fontSize:12,color:'var(--text-secondary)'}}>
            Khi tạo phiếu xuất, hệ thống <strong>tự động chọn lô có hạn dùng gần nhất trước</strong> (không cho chọn tay).
            Một phiếu xuất có thể lấy từ nhiều lô. Tồn kho được trừ ngay với Pessimistic Lock để tránh đồng thời.
          </div>
        </div>
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
                  <th>Đơn vị nhận</th>
                  <th>Người tạo</th>
                  <th>Trạng thái</th>
                  <th>Ngày tạo</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {orders.length === 0 ? (
                  <tr><td colSpan="7"><div className="empty-state">Chưa có phiếu xuất nào</div></td></tr>
                ) : orders.map(o => (
                  <tr key={o.id}>
                    <td>
                      <div style={{display:'flex',alignItems:'center',gap:6}}>
                        <code style={{fontSize:12}}>{o.orderCode}</code>
                        <span className="fefo-badge" style={{fontSize:9}}>FEFO</span>
                      </div>
                    </td>
                    <td>{o.warehouseName}</td>
                    <td>{o.recipient || <span className="text-muted">—</span>}</td>
                    <td>{o.createdByName}</td>
                    <td>
                      <span className={`badge badge-${o.status.toLowerCase()}`}>
                        {STATUS_LABELS[o.status]}
                      </span>
                    </td>
                    <td style={{fontSize:12}}>{new Date(o.createdAt).toLocaleString('vi-VN')}</td>
                    <td>
                      <div className="flex gap-2">
                        <button className="btn btn-secondary btn-sm" onClick={() => setSelectedOrder(o)}>
                          🔍 Chi tiết
                        </button>
                        {o.status === 'PENDING' && canApprove && (
                          <>
                            <button className="btn btn-success btn-sm" onClick={() => handleApprove(o.id, true)}>✓</button>
                            <button className="btn btn-danger btn-sm" onClick={() => handleApprove(o.id, false)}>✗</button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Create Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setShowModal(false)}>
          <div className="modal">
            <div className="modal-header">
              <h2 className="modal-title">📤 Tạo phiếu xuất kho (FEFO)</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>✕</button>
            </div>

            <div style={{background:'rgba(99,102,241,0.08)',border:'1px solid rgba(99,102,241,0.2)',borderRadius:8,padding:12,marginBottom:20,fontSize:13}}>
              💡 <strong>Lưu ý:</strong> Bạn chỉ cần nhập <strong>tên thuốc</strong> và <strong>số lượng cần xuất</strong>.
              Hệ thống sẽ tự động phân bổ lô theo FEFO (hạn gần nhất trước).
            </div>

            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label">Kho xuất *</label>
                  <select className="form-control" value={form.warehouseId}
                    onChange={e => setForm(f=>({...f,warehouseId:e.target.value}))} required disabled={!!user.warehouseId}>
                    <option value="">-- Chọn kho --</option>
                    {warehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Đơn vị nhận</label>
                  <input type="text" className="form-control" placeholder="Tên phòng/bệnh viện..." value={form.recipient}
                    onChange={e => setForm(f=>({...f,recipient:e.target.value}))} />
                </div>
              </div>

              <div style={{marginBottom:12}}>
                <div className="flex items-center justify-between mb-4">
                  <h4 style={{fontSize:14,fontWeight:600}}>Thuốc cần xuất</h4>
                  <button type="button" className="btn btn-secondary btn-sm"
                    onClick={() => setForm(f=>({...f,details:[...f.details,{medicineId:'',quantity:1}]}))}>
                    ＋ Thêm thuốc
                  </button>
                </div>
                {form.details.map((d,i) => (
                  <div key={i} style={{display:'flex',gap:12,marginBottom:8,alignItems:'flex-end'}}>
                    <div style={{flex:3}}>
                      <label className="form-label">Thuốc *</label>
                      <select className="form-control" value={d.medicineId}
                        onChange={e => setForm(f=>({...f,details:f.details.map((x,xi)=>xi===i?{...x,medicineId:e.target.value}:x)}))} required>
                        <option value="">-- Chọn thuốc --</option>
                        {medicines.map(m => <option key={m.id} value={m.id}>{m.name} ({m.unit})</option>)}
                      </select>
                    </div>
                    <div style={{flex:1}}>
                      <label className="form-label">Số lượng *</label>
                      <input type="number" className="form-control" min="1" value={d.quantity}
                        onChange={e => setForm(f=>({...f,details:f.details.map((x,xi)=>xi===i?{...x,quantity:e.target.value}:x)}))} required />
                    </div>
                    {form.details.length > 1 && (
                      <button type="button" className="btn btn-danger btn-sm"
                        onClick={() => setForm(f=>({...f,details:f.details.filter((_,xi)=>xi!==i)}))}>✕</button>
                    )}
                  </div>
                ))}
              </div>

              <div className="flex justify-between mt-4">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Hủy</button>
                <button type="submit" className="btn btn-primary">⚡ Xuất kho (FEFO auto)</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {selectedOrder && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setSelectedOrder(null)}>
          <div className="modal">
            <div className="modal-header">
              <h2 className="modal-title">Chi tiết phiếu xuất - {selectedOrder.orderCode}</h2>
              <button className="modal-close" onClick={() => setSelectedOrder(null)}>✕</button>
            </div>
            <div style={{marginBottom:16}}>
              <p><strong>Kho xuất:</strong> {selectedOrder.warehouseName}</p>
              <p><strong>Người tạo:</strong> {selectedOrder.createdByName}</p>
              <p><strong>Trạng thái:</strong> <span className={`badge badge-${selectedOrder.status.toLowerCase()}`}>{STATUS_LABELS[selectedOrder.status]}</span></p>
            </div>
            <h4 style={{marginBottom:10,fontSize:14}}>📋 Lô hàng được chọn theo FEFO:</h4>
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Thuốc</th>
                    <th>Số lô (FEFO)</th>
                    <th>Hạn dùng</th>
                    <th>Số lượng</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedOrder.details?.map((d,i) => (
                    <tr key={i}>
                      <td>{d.medicineName}</td>
                      <td>
                        <div style={{display:'flex',alignItems:'center',gap:6}}>
                          <code style={{fontSize:12}}>{d.batchNumber}</code>
                          <span className="fefo-badge" style={{fontSize:9}}>FEFO</span>
                        </div>
                      </td>
                      <td style={{fontSize:12}}>{d.expiryDate}</td>
                      <td><strong>{d.quantity}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

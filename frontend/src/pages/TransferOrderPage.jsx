import React, { useState, useEffect } from 'react';
import { transferAPI, catalogAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const STATUS_LABELS = {
  PENDING: 'Chờ gửi', IN_TRANSIT: 'Đang vận chuyển', DELIVERED: 'Đã giao', CANCELLED: 'Đã hủy'
};

const STATUS_FLOW = {
  PENDING: { next: 'IN_TRANSIT', label: '🚚 Gửi hàng', action: 'send' },
  IN_TRANSIT: { next: 'DELIVERED', label: '✅ Xác nhận nhận', action: 'confirm' },
};

export default function TransferOrderPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [warehouses, setWarehouses] = useState([]);
  const [batches, setBatches] = useState([]);

  const [form, setForm] = useState({
    sourceWarehouseId: user.warehouseId || '',
    destWarehouseId: '',
    note: '',
    details: [{ batchId: '', quantity: 1 }]
  });

  useEffect(() => { loadOrders(); loadCatalog(); }, []);

  const loadOrders = async () => {
    setLoading(true);
    try {
      const res = await transferAPI.list(user.warehouseId, null, 0);
      setOrders(res.data.content || []);
    } catch (e) {} finally { setLoading(false); }
  };

  const loadCatalog = async () => {
    try {
      const wRes = await catalogAPI.getWarehouses();
      setWarehouses(wRes.data);
    } catch (e) {}
  };

  const loadBatches = async (warehouseId) => {
    if (!warehouseId) return;
    try {
      const res = await catalogAPI.getBatches(warehouseId);
      setBatches(res.data.filter(b => !b.expired && b.quantity > 0));
    } catch (e) {}
  };

  const handleAction = async (order, action) => {
    try {
      if (action === 'send') {
        await transferAPI.send(order.id);
        toast.success('🚚 Đã gửi hàng! Tồn kho nguồn đã được trừ.');
      } else if (action === 'confirm') {
        await transferAPI.confirmDelivery(order.id);
        toast.success('✅ Đã xác nhận nhận hàng! Tồn kho đích đã được cộng.');
      } else if (action === 'cancel') {
        await transferAPI.cancel(order.id);
        toast.success('Đã hủy phiếu chuyển kho');
      }
      loadOrders();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Lỗi xử lý phiếu chuyển');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await transferAPI.create({
        sourceWarehouseId: Number(form.sourceWarehouseId),
        destWarehouseId: Number(form.destWarehouseId),
        note: form.note,
        details: form.details.map(d => ({ batchId: Number(d.batchId), quantity: Number(d.quantity) }))
      });
      toast.success('Tạo phiếu chuyển kho thành công!');
      setShowModal(false);
      loadOrders();
    } catch (e) {
      toast.error(e.response?.data?.message || 'Lỗi tạo phiếu chuyển');
    }
  };

  const canAct = ['ADMIN','WAREHOUSE_MANAGER'].includes(user.role);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">🔄 Chuyển Kho</h1>
          <p className="page-subtitle">Luồng trạng thái: PENDING → IN_TRANSIT → DELIVERED</p>
        </div>
        {canAct && <button className="btn btn-primary" onClick={() => setShowModal(true)}>＋ Tạo phiếu chuyển</button>}
      </div>

      {/* Status Flow Visual */}
      <div style={{display:'flex',alignItems:'center',gap:8,marginBottom:20,padding:'14px 20px',background:'var(--bg-card)',borderRadius:12,border:'1px solid var(--border)'}}>
        {['PENDING','IN_TRANSIT','DELIVERED'].map((s,i) => (
          <React.Fragment key={s}>
            <div style={{display:'flex',flexDirection:'column',alignItems:'center',gap:4}}>
              <div style={{padding:'6px 16px',borderRadius:20,fontSize:12,fontWeight:600,
                background: s==='PENDING'?'rgba(245,158,11,0.2)':s==='IN_TRANSIT'?'rgba(14,165,233,0.2)':'rgba(16,185,129,0.2)',
                color: s==='PENDING'?'#fbbf24':s==='IN_TRANSIT'?'#38bdf8':'#34d399'}}>
                {STATUS_LABELS[s]}
              </div>
              <div style={{fontSize:10,color:'var(--text-secondary)'}}>
                {s==='PENDING'?'Kho nguồn chưa trừ':s==='IN_TRANSIT'?'Kho nguồn đã trừ':'Kho đích đã cộng'}
              </div>
            </div>
            {i < 2 && <span style={{color:'var(--text-secondary)',fontSize:20}}>→</span>}
          </React.Fragment>
        ))}
      </div>

      {loading ? <div className="loading"><div className="spinner"/></div> : (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Mã phiếu</th>
                  <th>Kho nguồn</th>
                  <th>Kho đích</th>
                  <th>Người tạo</th>
                  <th>Trạng thái</th>
                  <th>Ngày tạo</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {orders.length === 0 ? (
                  <tr><td colSpan="7"><div className="empty-state">Chưa có phiếu chuyển kho nào</div></td></tr>
                ) : orders.map(o => (
                  <tr key={o.id}>
                    <td><code style={{fontSize:12}}>{o.orderCode}</code></td>
                    <td>{o.sourceWarehouse?.name}</td>
                    <td>{o.destWarehouse?.name}</td>
                    <td>{o.createdBy?.fullName}</td>
                    <td><span className={`badge badge-${o.status.toLowerCase()}`}>{STATUS_LABELS[o.status]}</span></td>
                    <td style={{fontSize:12}}>{new Date(o.createdAt).toLocaleString('vi-VN')}</td>
                    <td>
                      {canAct && STATUS_FLOW[o.status] && (
                        <div className="flex gap-2">
                          <button className="btn btn-success btn-sm" onClick={() => handleAction(o, STATUS_FLOW[o.status].action)}>
                            {STATUS_FLOW[o.status].label}
                          </button>
                          {o.status === 'PENDING' && (
                            <button className="btn btn-danger btn-sm" onClick={() => handleAction(o, 'cancel')}>Hủy</button>
                          )}
                        </div>
                      )}
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
              <h2 className="modal-title">🔄 Tạo phiếu chuyển kho</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label">Kho nguồn *</label>
                  <select className="form-control" value={form.sourceWarehouseId}
                    onChange={e => { setForm(f=>({...f,sourceWarehouseId:e.target.value})); loadBatches(e.target.value); }}
                    required disabled={!!user.warehouseId}>
                    <option value="">-- Chọn kho nguồn --</option>
                    {warehouses.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Kho đích *</label>
                  <select className="form-control" value={form.destWarehouseId}
                    onChange={e => setForm(f=>({...f,destWarehouseId:e.target.value}))} required>
                    <option value="">-- Chọn kho đích --</option>
                    {warehouses.filter(w => w.id !== Number(form.sourceWarehouseId)).map(w => (
                      <option key={w.id} value={w.id}>{w.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div style={{marginBottom:12}}>
                <div className="flex items-center justify-between mb-4">
                  <h4 style={{fontSize:14,fontWeight:600}}>Lô hàng cần chuyển</h4>
                  <button type="button" className="btn btn-secondary btn-sm"
                    onClick={() => setForm(f=>({...f,details:[...f.details,{batchId:'',quantity:1}]}))}>
                    ＋ Thêm lô
                  </button>
                </div>
                {form.details.map((d,i) => (
                  <div key={i} style={{display:'flex',gap:12,marginBottom:8,alignItems:'flex-end'}}>
                    <div style={{flex:3}}>
                      <label className="form-label">Lô hàng *</label>
                      <select className="form-control" value={d.batchId}
                        onChange={e => setForm(f=>({...f,details:f.details.map((x,xi)=>xi===i?{...x,batchId:e.target.value}:x)}))} required>
                        <option value="">-- Chọn lô --</option>
                        {batches.map(b => (
                          <option key={b.id} value={b.id}>
                            {b.medicineName} - Lô {b.batchNumber} (HSD: {b.expiryDate}, tồn: {b.quantity})
                          </option>
                        ))}
                      </select>
                    </div>
                    <div style={{flex:1}}>
                      <label className="form-label">Số lượng *</label>
                      <input type="number" className="form-control" min="1" value={d.quantity}
                        onChange={e => setForm(f=>({...f,details:f.details.map((x,xi)=>xi===i?{...x,quantity:e.target.value}:x)}))} required />
                    </div>
                  </div>
                ))}
              </div>

              <div className="flex justify-between mt-4">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Hủy</button>
                <button type="submit" className="btn btn-primary">🔄 Tạo phiếu chuyển</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

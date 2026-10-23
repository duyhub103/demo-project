import React, { useState, useEffect } from 'react';
import { catalogAPI, inventoryAPI, importAPI, exportAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, LineChart, Line
} from 'recharts';

export default function Dashboard() {
  const { user } = useAuth();
  const [alerts, setAlerts] = useState(null);
  const [warehouses, setWarehouses] = useState([]);
  const [selectedWarehouse, setSelectedWarehouse] = useState(null);
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadWarehouses();
  }, []);

  useEffect(() => {
    if (selectedWarehouse) {
      loadData(selectedWarehouse);
    }
  }, [selectedWarehouse]);

  const loadWarehouses = async () => {
    try {
      const res = await catalogAPI.getWarehouses();
      setWarehouses(res.data);
      const warehouseId = user.warehouseId || res.data[0]?.id;
      setSelectedWarehouse(warehouseId);
    } catch (e) {}
  };

  const loadData = async (warehouseId) => {
    setLoading(true);
    try {
      const [alertRes, batchRes] = await Promise.all([
        inventoryAPI.getAlerts(warehouseId),
        catalogAPI.getBatches(warehouseId),
      ]);
      setAlerts(alertRes.data);
      setBatches(batchRes.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  // Aggregate stock by medicine for chart
  const stockByMedicine = React.useMemo(() => {
    const map = {};
    batches.filter(b => !b.expired && b.quantity > 0).forEach(b => {
      if (!map[b.medicineName]) map[b.medicineName] = 0;
      map[b.medicineName] += b.quantity;
    });
    return Object.entries(map)
      .map(([name, qty]) => ({ name: name.length > 15 ? name.substring(0,15)+'…' : name, qty }))
      .sort((a,b) => b.qty - a.qty)
      .slice(0, 8);
  }, [batches]);

  const totalStock = batches.reduce((sum, b) => sum + (b.expired ? 0 : b.quantity), 0);
  const expiredBatches = batches.filter(b => b.expired).length;
  const nearExpiryCount = alerts?.totalNearExpiry || 0;
  const lowStockCount = alerts?.totalLowStock || 0;

  const warehouseName = warehouses.find(w => w.id === selectedWarehouse)?.name || '';

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">📊 Dashboard</h1>
          <p className="page-subtitle">Tổng quan hệ thống kho dược phẩm</p>
        </div>
        {warehouses.length > 1 && !user.warehouseId && (
          <select
            id="warehouse-select"
            className="form-control"
            style={{width: 220}}
            value={selectedWarehouse || ''}
            onChange={e => setSelectedWarehouse(Number(e.target.value))}
          >
            {warehouses.map(w => (
              <option key={w.id} value={w.id}>{w.name}</option>
            ))}
          </select>
        )}
      </div>

      {loading ? (
        <div className="loading"><div className="spinner"/><span>Đang tải...</span></div>
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-icon blue">📦</div>
              <div>
                <div className="stat-value">{totalStock.toLocaleString()}</div>
                <div className="stat-label">Tổng tồn kho</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon purple">🗃️</div>
              <div>
                <div className="stat-value">{batches.length}</div>
                <div className="stat-label">Số lô hàng</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon orange">⚠️</div>
              <div>
                <div className="stat-value" style={{color: nearExpiryCount > 0 ? '#fbbf24' : undefined}}>{nearExpiryCount}</div>
                <div className="stat-label">Lô cận hạn (≤90 ngày)</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon red">🔻</div>
              <div>
                <div className="stat-value" style={{color: lowStockCount > 0 ? '#f87171' : undefined}}>{lowStockCount}</div>
                <div className="stat-label">Thuốc tồn thấp</div>
              </div>
            </div>
          </div>

          <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:16, marginBottom:24}}>
            {/* Stock Chart */}
            <div className="card">
              <div style={{display:'flex',alignItems:'center',justifyContent:'space-between',marginBottom:8}}>
                <h3 style={{fontSize:15,fontWeight:700}}>📈 Tồn kho theo thuốc</h3>
                <span className="fefo-badge">⚡ FEFO Active</span>
              </div>
              <p style={{fontSize:12,color:'var(--text-secondary)',marginBottom:8}}>{warehouseName}</p>
              <div className="chart-container">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={stockByMedicine} margin={{top:0,right:0,left:-20,bottom:0}}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#334155"/>
                    <XAxis dataKey="name" tick={{fontSize:10,fill:'#94a3b8'}} />
                    <YAxis tick={{fontSize:10,fill:'#94a3b8'}} />
                    <Tooltip
                      contentStyle={{background:'#1e293b',border:'1px solid #334155',borderRadius:8}}
                      labelStyle={{color:'#f1f5f9'}}
                    />
                    <Bar dataKey="qty" fill="#6366f1" radius={[4,4,0,0]} name="Tồn kho" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Alerts Panel */}
            <div className="card">
              <h3 style={{fontSize:15,fontWeight:700,marginBottom:16}}>🚨 Cảnh báo hệ thống</h3>
              
              {lowStockCount > 0 && (
                <div style={{marginBottom:12}}>
                  <p style={{fontSize:12,color:'#fbbf24',fontWeight:600,marginBottom:8}}>
                    ⚠️ TỒN THẤP HƠN NGƯỠNG ({lowStockCount} thuốc)
                  </p>
                  {alerts.lowStockAlerts.slice(0,3).map((a,i) => (
                    <div key={i} className="alert-item alert-low">
                      <div>
                        <div style={{fontSize:13,fontWeight:600}}>{a.medicineName}</div>
                        <div style={{fontSize:11,color:'var(--text-secondary)'}}>Tồn: {a.currentStock} / Ngưỡng: {a.minStockLevel}</div>
                      </div>
                      <span style={{color:'#f87171',fontWeight:700}}>-{a.deficit}</span>
                    </div>
                  ))}
                </div>
              )}

              {nearExpiryCount > 0 && (
                <div>
                  <p style={{fontSize:12,color:'#f87171',fontWeight:600,marginBottom:8}}>
                    🔴 LÔ CẬN HẠN ({nearExpiryCount} lô)
                  </p>
                  {alerts.nearExpiryAlerts.slice(0,3).map((a,i) => (
                    <div key={i} className="alert-item alert-expiry">
                      <div>
                        <div style={{fontSize:13,fontWeight:600}}>{a.medicineName}</div>
                        <div style={{fontSize:11,color:'var(--text-secondary)'}}>
                          Lô {a.batchNumber} • HSD: {a.expiryDate} • {a.quantity} đơn vị
                        </div>
                      </div>
                      <span className={a.daysUntilExpiry <= 30 ? 'expiry-critical' : 'expiry-warning'} style={{fontWeight:700}}>
                        {a.daysUntilExpiry}d
                      </span>
                    </div>
                  ))}
                </div>
              )}

              {lowStockCount === 0 && nearExpiryCount === 0 && (
                <div className="empty-state">
                  <div style={{fontSize:32}}>✅</div>
                  <p style={{marginTop:8}}>Không có cảnh báo</p>
                </div>
              )}
            </div>
          </div>

          {/* Batch table */}
          <div className="card">
            <div style={{display:'flex',alignItems:'center',justifyContent:'space-between',marginBottom:16}}>
              <h3 style={{fontSize:15,fontWeight:700}}>📋 Tồn kho theo lô (FEFO order)</h3>
              <span className="text-muted text-sm">{batches.length} lô</span>
            </div>
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Số lô</th>
                    <th>Thuốc</th>
                    <th>Tồn kho</th>
                    <th>Ngày sản xuất</th>
                    <th>Hạn dùng</th>
                    <th>Còn</th>
                    <th>Trạng thái</th>
                  </tr>
                </thead>
                <tbody>
                  {batches.slice(0,10).map(b => (
                    <tr key={b.id}>
                      <td style={{fontFamily:'monospace',fontSize:13}}>{b.batchNumber}</td>
                      <td>
                        <div style={{fontWeight:600}}>{b.medicineName}</div>
                        <div style={{fontSize:11,color:'var(--text-secondary)'}}>{b.medicineCode}</div>
                      </td>
                      <td><strong>{b.quantity.toLocaleString()}</strong></td>
                      <td style={{fontSize:12}}>{b.manufactureDate}</td>
                      <td style={{fontSize:12}}>{b.expiryDate}</td>
                      <td>
                        <span className={b.expired ? 'expiry-critical' : b.daysUntilExpiry <= 30 ? 'expiry-critical' : b.daysUntilExpiry <= 90 ? 'expiry-warning' : 'expiry-ok'}>
                          {b.expired ? 'Hết hạn' : `${b.daysUntilExpiry}d`}
                        </span>
                      </td>
                      <td>
                        {b.expired ? <span className="badge badge-rejected">Hết hạn</span>
                          : b.nearExpiry ? <span className="badge badge-pending">Cận hạn</span>
                          : <span className="badge badge-approved">Còn hạn</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

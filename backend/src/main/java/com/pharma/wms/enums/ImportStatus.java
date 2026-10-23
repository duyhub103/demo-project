package com.pharma.wms.enums;

public enum ImportStatus {
    DRAFT,      // Nháp - nhân viên tạo
    PENDING,    // Chờ duyệt
    APPROVED,   // Đã duyệt - tự động cộng tồn
    REJECTED    // Từ chối
}

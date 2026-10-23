package com.pharma.wms.enums;

public enum TransferStatus {
    PENDING,    // Chờ xử lý
    IN_TRANSIT, // Đang vận chuyển (đã trừ kho nguồn)
    DELIVERED,  // Đã giao (đã cộng kho đích)
    CANCELLED   // Hủy
}

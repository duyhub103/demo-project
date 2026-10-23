package com.pharma.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/**
 * Request tạo phiếu xuất kho.
 * Không có batchId ở đây - hệ thống tự chọn lô theo FEFO.
 */
@Data
public class CreateExportOrderRequest {

    @NotNull(message = "Kho xuất hàng không được để trống")
    private Long warehouseId;

    private String note;
    private String recipient; // Đơn vị nhận

    @NotNull
    private List<ExportDetailItem> details;

    @Data
    public static class ExportDetailItem {
        @NotNull(message = "Mã thuốc không được để trống")
        private Long medicineId;

        @NotNull @Positive(message = "Số lượng phải > 0")
        private Integer quantity; // Tổng SL cần xuất - hệ thống tự phân bổ theo FEFO
    }
}

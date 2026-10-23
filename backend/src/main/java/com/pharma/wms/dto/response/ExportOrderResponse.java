package com.pharma.wms.dto.response;

import com.pharma.wms.enums.ExportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class ExportOrderResponse {
    private Long id;
    private String orderCode;
    private Long warehouseId;
    private String warehouseName;
    private String createdByName;
    private String approvedByName;
    private ExportStatus status;
    private String note;
    private String recipient;
    private LocalDateTime createdAt;
    private List<DetailItem> details;

    @Data @Builder
    public static class DetailItem {
        private Long id;
        private Long batchId;
        private String batchNumber;
        private LocalDate expiryDate;
        private Long medicineId;
        private String medicineName;
        private Integer quantity;
    }
}

package com.pharma.wms.dto.response;

import com.pharma.wms.enums.ImportStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class ImportOrderResponse {
    private Long id;
    private String orderCode;
    private Long warehouseId;
    private String warehouseName;
    private String supplierName;
    private String createdByName;
    private String approvedByName;
    private ImportStatus status;
    private String note;
    private String rejectReason;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private List<DetailItem> details;

    @Data @Builder
    public static class DetailItem {
        private Long id;
        private Long medicineId;
        private String medicineName;
        private String medicineCode;
        private String batchNumber;
        private LocalDate manufactureDate;
        private LocalDate expiryDate;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}

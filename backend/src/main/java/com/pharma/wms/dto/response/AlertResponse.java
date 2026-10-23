package com.pharma.wms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class AlertResponse {
    private List<LowStockAlert> lowStockAlerts;
    private List<NearExpiryAlert> nearExpiryAlerts;
    private int totalLowStock;
    private int totalNearExpiry;

    @Data @Builder
    public static class LowStockAlert {
        private Long medicineId;
        private String medicineName;
        private String medicineCode;
        private Integer currentStock;
        private Integer minStockLevel;
        private Integer deficit; // Thiếu bao nhiêu
    }

    @Data @Builder
    public static class NearExpiryAlert {
        private Long batchId;
        private String batchNumber;
        private Long medicineId;
        private String medicineName;
        private Integer quantity;
        private String expiryDate;
        private Long daysUntilExpiry;
    }
}

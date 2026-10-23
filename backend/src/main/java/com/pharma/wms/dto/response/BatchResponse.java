package com.pharma.wms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class BatchResponse {
    private Long id;
    private String batchNumber;
    private Long medicineId;
    private String medicineName;
    private String medicineCode;
    private Long warehouseId;
    private String warehouseName;
    private String supplierName;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private Integer quantity;
    private Integer initialQuantity;
    private BigDecimal importPrice;
    private boolean active;
    private boolean expired;
    private boolean nearExpiry;
    private Long daysUntilExpiry;
    private LocalDateTime createdAt;
}

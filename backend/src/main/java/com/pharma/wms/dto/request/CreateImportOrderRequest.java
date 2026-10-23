package com.pharma.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateImportOrderRequest {

    @NotNull(message = "Kho nhập hàng không được để trống")
    private Long warehouseId;

    private Long supplierId;

    private String note;

    @NotNull(message = "Chi tiết nhập kho không được để trống")
    private List<ImportDetailItem> details;

    @Data
    public static class ImportDetailItem {
        @NotNull
        private Long medicineId;

        @NotBlank(message = "Số lô không được để trống")
        private String batchNumber;

        @NotNull
        private LocalDate manufactureDate;

        @NotNull
        private LocalDate expiryDate;

        @NotNull @Positive
        private Integer quantity;

        private BigDecimal unitPrice;
    }
}

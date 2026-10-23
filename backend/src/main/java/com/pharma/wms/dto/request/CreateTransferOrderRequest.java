package com.pharma.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class CreateTransferOrderRequest {

    @NotNull
    private Long sourceWarehouseId;

    @NotNull
    private Long destWarehouseId;

    private String note;

    @NotNull
    private List<TransferDetailItem> details;

    @Data
    public static class TransferDetailItem {
        @NotNull
        private Long batchId; // Lô cụ thể ở kho nguồn

        @NotNull @Positive
        private Integer quantity;
    }
}

package com.pharma.wms.controller;

import com.pharma.wms.dto.request.CreateTransferOrderRequest;
import com.pharma.wms.entity.TransferOrder;
import com.pharma.wms.enums.TransferStatus;
import com.pharma.wms.service.impl.TransferOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfer-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transfer Orders", description = "Chuyển kho giữa chi nhánh: PENDING→IN_TRANSIT→DELIVERED")
public class TransferOrderController {

    private final TransferOrderService transferOrderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Tạo phiếu chuyển kho")
    public ResponseEntity<TransferOrder> create(@Valid @RequestBody CreateTransferOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferOrderService.createTransferOrder(request));
    }

    @GetMapping
    @Operation(summary = "Danh sách phiếu chuyển kho")
    public ResponseEntity<Page<TransferOrder>> list(
        @RequestParam(required = false) Long warehouseId,
        @RequestParam(required = false) TransferStatus status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(transferOrderService.getTransferOrders(warehouseId, status, pageable));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Gửi hàng (PENDING→IN_TRANSIT) - trừ tồn kho nguồn")
    public ResponseEntity<TransferOrder> send(@PathVariable Long id) {
        return ResponseEntity.ok(transferOrderService.sendTransfer(id));
    }

    @PostMapping("/{id}/confirm-delivery")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Xác nhận nhận hàng (IN_TRANSIT→DELIVERED) - cộng tồn kho đích")
    public ResponseEntity<TransferOrder> confirmDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(transferOrderService.confirmDelivery(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Hủy phiếu chuyển kho")
    public ResponseEntity<TransferOrder> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(transferOrderService.cancelTransfer(id));
    }
}

package com.pharma.wms.controller;

import com.pharma.wms.dto.request.CreateExportOrderRequest;
import com.pharma.wms.dto.response.ExportOrderResponse;
import com.pharma.wms.enums.ExportStatus;
import com.pharma.wms.service.impl.ExportOrderService;
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
@RequestMapping("/api/export-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Export Orders", description = "Quản lý phiếu xuất kho - FEFO")
public class ExportOrderController {

    private final ExportOrderService exportOrderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(
        summary = "Tạo phiếu xuất kho (FEFO tự động)",
        description = "Hệ thống tự động chọn lô có hạn dùng gần nhất (FEFO). Không cần chỉ định lô thủ công."
    )
    public ResponseEntity<ExportOrderResponse> create(@Valid @RequestBody CreateExportOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exportOrderService.createExportOrder(request));
    }

    @GetMapping
    @Operation(summary = "Danh sách phiếu xuất")
    public ResponseEntity<Page<ExportOrderResponse>> list(
        @RequestParam(required = false) Long warehouseId,
        @RequestParam(required = false) ExportStatus status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(exportOrderService.getExportOrders(warehouseId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phiếu xuất")
    public ResponseEntity<ExportOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(exportOrderService.getById(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Duyệt phiếu xuất kho")
    public ResponseEntity<ExportOrderResponse> approve(
        @PathVariable Long id,
        @RequestParam boolean approved,
        @RequestParam(required = false) String rejectReason
    ) {
        return ResponseEntity.ok(exportOrderService.approveExportOrder(id, approved, rejectReason));
    }
}

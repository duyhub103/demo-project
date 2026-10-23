package com.pharma.wms.controller;

import com.pharma.wms.dto.request.CreateImportOrderRequest;
import com.pharma.wms.dto.response.ImportOrderResponse;
import com.pharma.wms.enums.ImportStatus;
import com.pharma.wms.service.impl.ImportOrderService;
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
@RequestMapping("/api/import-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Import Orders", description = "Quản lý phiếu nhập kho")
public class ImportOrderController {

    private final ImportOrderService importOrderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Tạo phiếu nhập kho")
    public ResponseEntity<ImportOrderResponse> create(@Valid @RequestBody CreateImportOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(importOrderService.createImportOrder(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Danh sách phiếu nhập")
    public ResponseEntity<Page<ImportOrderResponse>> list(
        @RequestParam(required = false) Long warehouseId,
        @RequestParam(required = false) ImportStatus status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(importOrderService.getImportOrders(warehouseId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phiếu nhập")
    public ResponseEntity<ImportOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(importOrderService.getById(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Duyệt phiếu nhập kho - tự động tạo lô và cộng tồn")
    public ResponseEntity<ImportOrderResponse> approve(
        @PathVariable Long id,
        @RequestParam boolean approved,
        @RequestParam(required = false) String rejectReason
    ) {
        return ResponseEntity.ok(importOrderService.approveImportOrder(id, approved, rejectReason));
    }
}

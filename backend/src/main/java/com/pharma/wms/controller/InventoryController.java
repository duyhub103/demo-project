package com.pharma.wms.controller;

import com.pharma.wms.dto.response.AlertResponse;
import com.pharma.wms.entity.InventoryCheck;
import com.pharma.wms.service.impl.AlertService;
import com.pharma.wms.service.impl.InventoryCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inventory & Alerts", description = "Kiểm kê kho và cảnh báo")
public class InventoryController {

    private final InventoryCheckService inventoryCheckService;
    private final AlertService alertService;

    @GetMapping("/alerts")
    @Operation(summary = "Cảnh báo: tồn thấp và lô cận hạn")
    public ResponseEntity<AlertResponse> getAlerts(@RequestParam Long warehouseId) {
        return ResponseEntity.ok(alertService.getAlerts(warehouseId));
    }

    @PostMapping("/checks")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Tạo phiếu kiểm kê - snapshot tồn hệ thống")
    public ResponseEntity<InventoryCheck> createCheck(
        @RequestParam Long warehouseId,
        @RequestParam(required = false) String note
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inventoryCheckService.createInventoryCheck(warehouseId, note));
    }

    @GetMapping("/checks")
    @Operation(summary = "Danh sách phiếu kiểm kê")
    public ResponseEntity<Page<InventoryCheck>> listChecks(
        @RequestParam(required = false) Long warehouseId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryCheckService.getInventoryChecks(warehouseId, pageable));
    }

    @GetMapping("/checks/{id}")
    @Operation(summary = "Chi tiết phiếu kiểm kê")
    public ResponseEntity<InventoryCheck> getCheck(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryCheckService.getById(id));
    }

    @PutMapping("/checks/{id}/actual-quantities")
    @Operation(summary = "Nhập số lượng thực đếm (batchId -> actualQty)")
    public ResponseEntity<InventoryCheck> updateActual(
        @PathVariable Long id,
        @RequestBody Map<Long, Integer> batchActualQty
    ) {
        return ResponseEntity.ok(inventoryCheckService.updateActualQuantities(id, batchActualQty));
    }

    @PostMapping("/checks/{id}/approve")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Duyệt kiểm kê và điều chỉnh tồn kho")
    public ResponseEntity<InventoryCheck> approve(
        @PathVariable Long id,
        @RequestParam boolean approved
    ) {
        return ResponseEntity.ok(inventoryCheckService.approveInventoryCheck(id, approved));
    }
}

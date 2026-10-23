package com.pharma.wms.controller;

import com.pharma.wms.dto.response.BatchResponse;
import com.pharma.wms.entity.Batch;
import com.pharma.wms.entity.Medicine;
import com.pharma.wms.entity.Supplier;
import com.pharma.wms.entity.Warehouse;
import com.pharma.wms.repository.BatchRepository;
import com.pharma.wms.repository.MedicineRepository;
import com.pharma.wms.repository.SupplierRepository;
import com.pharma.wms.repository.WarehouseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Catalog", description = "Danh mục thuốc, nhà cung cấp, kho, lô hàng")
public class CatalogController {

    private final MedicineRepository medicineRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final BatchRepository batchRepository;

    @Value("${app.warehouse.expiry-warning-days:90}")
    private int expiryWarningDays;

    // ===== MEDICINES =====
    @GetMapping("/medicines")
    @Operation(summary = "Danh sách thuốc (có tìm kiếm)")
    public ResponseEntity<Page<Medicine>> getMedicines(
        @RequestParam(required = false) String keyword,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(medicineRepository.searchMedicines(keyword, pageable));
    }

    @GetMapping("/medicines/{id}")
    public ResponseEntity<Medicine> getMedicine(@PathVariable Long id) {
        return ResponseEntity.ok(medicineRepository.findById(id).orElseThrow());
    }

    @PostMapping("/medicines")
    @Operation(summary = "Thêm thuốc mới")
    public ResponseEntity<Medicine> createMedicine(@RequestBody Medicine medicine) {
        return ResponseEntity.ok(medicineRepository.save(medicine));
    }

    @PutMapping("/medicines/{id}")
    @Operation(summary = "Cập nhật thông tin thuốc")
    public ResponseEntity<Medicine> updateMedicine(@PathVariable Long id, @RequestBody Medicine updated) {
        Medicine m = medicineRepository.findById(id).orElseThrow();
        m.setName(updated.getName());
        m.setActiveIngredient(updated.getActiveIngredient());
        m.setDosageForm(updated.getDosageForm());
        m.setUnit(updated.getUnit());
        m.setMinStockLevel(updated.getMinStockLevel());
        m.setUnitPrice(updated.getUnitPrice());
        return ResponseEntity.ok(medicineRepository.save(m));
    }

    // ===== SUPPLIERS =====
    @GetMapping("/suppliers")
    public ResponseEntity<List<Supplier>> getSuppliers() {
        return ResponseEntity.ok(supplierRepository.findAll());
    }

    @PostMapping("/suppliers")
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier) {
        return ResponseEntity.ok(supplierRepository.save(supplier));
    }

    // ===== WAREHOUSES =====
    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> getWarehouses() {
        return ResponseEntity.ok(warehouseRepository.findByActiveTrue());
    }

    @PostMapping("/warehouses")
    public ResponseEntity<Warehouse> createWarehouse(@RequestBody Warehouse warehouse) {
        return ResponseEntity.ok(warehouseRepository.save(warehouse));
    }

    // ===== BATCHES =====
    @GetMapping("/batches")
    @Operation(summary = "Tồn kho hiện tại theo lô")
    public ResponseEntity<List<BatchResponse>> getBatches(
        @RequestParam Long warehouseId,
        @RequestParam(required = false) Long medicineId
    ) {
        List<Batch> batches;
        if (medicineId != null) {
            batches = batchRepository.findByWarehouseAndMedicine(warehouseId, medicineId);
        } else {
            batches = batchRepository.findByWarehouseIdAndActiveTrue(warehouseId);
        }

        LocalDate today = LocalDate.now();
        return ResponseEntity.ok(batches.stream()
            .map(b -> BatchResponse.builder()
                .id(b.getId())
                .batchNumber(b.getBatchNumber())
                .medicineId(b.getMedicine().getId())
                .medicineName(b.getMedicine().getName())
                .medicineCode(b.getMedicine().getCode())
                .warehouseId(b.getWarehouse().getId())
                .warehouseName(b.getWarehouse().getName())
                .supplierName(b.getSupplier() != null ? b.getSupplier().getName() : null)
                .manufactureDate(b.getManufactureDate())
                .expiryDate(b.getExpiryDate())
                .quantity(b.getQuantity())
                .initialQuantity(b.getInitialQuantity())
                .importPrice(b.getImportPrice())
                .active(b.isActive())
                .expired(b.isExpired())
                .nearExpiry(b.isNearExpiry(expiryWarningDays))
                .daysUntilExpiry(ChronoUnit.DAYS.between(today, b.getExpiryDate()))
                .createdAt(b.getCreatedAt())
                .build())
            .toList()
        );
    }
}

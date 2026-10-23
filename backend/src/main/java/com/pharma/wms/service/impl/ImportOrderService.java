package com.pharma.wms.service.impl;

import com.pharma.wms.dto.request.CreateImportOrderRequest;
import com.pharma.wms.dto.response.ImportOrderResponse;
import com.pharma.wms.entity.*;
import com.pharma.wms.enums.ImportStatus;
import com.pharma.wms.exception.BusinessException;
import com.pharma.wms.exception.ResourceNotFoundException;
import com.pharma.wms.repository.*;
import com.pharma.wms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportOrderService {

    private final ImportOrderRepository importOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;

    @Transactional
    public ImportOrderResponse createImportOrder(CreateImportOrderRequest request) {
        UserPrincipal principal = getCurrentUser();

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
            .orElseThrow(() -> new ResourceNotFoundException("Kho hàng", request.getWarehouseId()));

        // Kiểm soát phạm vi: staff chỉ được tạo phiếu cho kho của mình
        if (principal.getWarehouseId() != null && !principal.getWarehouseId().equals(request.getWarehouseId())) {
            throw new BusinessException("Bạn không có quyền tạo phiếu nhập cho kho này");
        }

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp", request.getSupplierId()));
        }

        User currentUser = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        ImportOrder order = ImportOrder.builder()
            .orderCode(generateOrderCode("NK"))
            .warehouse(warehouse)
            .supplier(supplier)
            .createdBy(currentUser)
            .status(ImportStatus.PENDING) // Tự động chuyển sang PENDING để quản lý duyệt
            .note(request.getNote())
            .build();

        List<ImportOrderDetail> details = new ArrayList<>();
        for (CreateImportOrderRequest.ImportDetailItem item : request.getDetails()) {
            Medicine medicine = medicineRepository.findById(item.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Thuốc", item.getMedicineId()));

            // Validate ngày
            if (item.getExpiryDate().isBefore(item.getManufactureDate())) {
                throw new BusinessException("Hạn sử dụng phải sau ngày sản xuất cho thuốc: " + medicine.getName());
            }
            if (item.getExpiryDate().isBefore(java.time.LocalDate.now())) {
                throw new BusinessException("Không thể nhập lô đã hết hạn: " + item.getBatchNumber());
            }

            ImportOrderDetail detail = ImportOrderDetail.builder()
                .importOrder(order)
                .medicine(medicine)
                .batchNumber(item.getBatchNumber())
                .manufactureDate(item.getManufactureDate())
                .expiryDate(item.getExpiryDate())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build();
            details.add(detail);
        }
        order.setDetails(details);

        ImportOrder saved = importOrderRepository.save(order);
        log.info("Tạo phiếu nhập {} bởi {}", saved.getOrderCode(), principal.getUsername());
        return mapToResponse(saved);
    }

    @Transactional
    public ImportOrderResponse approveImportOrder(Long orderId, boolean approved, String rejectReason) {
        ImportOrder order = importOrderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Phiếu nhập", orderId));

        if (order.getStatus() != ImportStatus.PENDING) {
            throw new BusinessException("Phiếu nhập không ở trạng thái chờ duyệt");
        }

        UserPrincipal principal = getCurrentUser();
        User approver = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        if (approved) {
            order.setStatus(ImportStatus.APPROVED);
            order.setApprovedBy(approver);
            order.setApprovedAt(LocalDateTime.now());

            // Tự động tạo/cập nhật lô hàng và cộng tồn
            for (ImportOrderDetail detail : order.getDetails()) {
                Batch batch = Batch.builder()
                    .batchNumber(detail.getBatchNumber())
                    .medicine(detail.getMedicine())
                    .warehouse(order.getWarehouse())
                    .supplier(order.getSupplier())
                    .manufactureDate(detail.getManufactureDate())
                    .expiryDate(detail.getExpiryDate())
                    .quantity(detail.getQuantity())
                    .initialQuantity(detail.getQuantity())
                    .importPrice(detail.getUnitPrice())
                    .active(true)
                    .build();
                Batch savedBatch = batchRepository.save(batch);
                detail.setBatch(savedBatch);
                log.info("Tạo lô {} - {} SL tại kho {}", 
                    batch.getBatchNumber(), batch.getQuantity(), order.getWarehouse().getName());
            }
        } else {
            order.setStatus(ImportStatus.REJECTED);
            order.setRejectReason(rejectReason);
        }

        return mapToResponse(importOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public Page<ImportOrderResponse> getImportOrders(Long warehouseId, ImportStatus status, Pageable pageable) {
        return importOrderRepository.findByWarehouseAndStatus(warehouseId, status, pageable)
            .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ImportOrderResponse getById(Long id) {
        return mapToResponse(importOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Phiếu nhập", id)));
    }

    private ImportOrderResponse mapToResponse(ImportOrder order) {
        List<ImportOrderResponse.DetailItem> detailItems = order.getDetails().stream()
            .map(d -> ImportOrderResponse.DetailItem.builder()
                .id(d.getId())
                .medicineId(d.getMedicine().getId())
                .medicineName(d.getMedicine().getName())
                .medicineCode(d.getMedicine().getCode())
                .batchNumber(d.getBatchNumber())
                .manufactureDate(d.getManufactureDate())
                .expiryDate(d.getExpiryDate())
                .quantity(d.getQuantity())
                .unitPrice(d.getUnitPrice())
                .build())
            .toList();

        return ImportOrderResponse.builder()
            .id(order.getId())
            .orderCode(order.getOrderCode())
            .warehouseId(order.getWarehouse().getId())
            .warehouseName(order.getWarehouse().getName())
            .supplierName(order.getSupplier() != null ? order.getSupplier().getName() : null)
            .createdByName(order.getCreatedBy().getFullName())
            .approvedByName(order.getApprovedBy() != null ? order.getApprovedBy().getFullName() : null)
            .status(order.getStatus())
            .note(order.getNote())
            .rejectReason(order.getRejectReason())
            .approvedAt(order.getApprovedAt())
            .createdAt(order.getCreatedAt())
            .details(detailItems)
            .build();
    }

    private String generateOrderCode(String prefix) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        return prefix + "-" + timestamp;
    }

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

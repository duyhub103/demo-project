package com.pharma.wms.service.impl;

import com.pharma.wms.dto.request.CreateExportOrderRequest;
import com.pharma.wms.dto.response.ExportOrderResponse;
import com.pharma.wms.entity.*;
import com.pharma.wms.enums.ExportStatus;
import com.pharma.wms.exception.BusinessException;
import com.pharma.wms.exception.ResourceNotFoundException;
import com.pharma.wms.repository.*;
import com.pharma.wms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportOrderService {

    private final ExportOrderRepository exportOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;

    @Value("${app.warehouse.expiry-warning-days:90}")
    private int expiryWarningDays;

    /**
     * Tạo phiếu xuất kho - áp dụng cơ chế FEFO.
     * Hệ thống tự động chọn lô có hạn dùng gần nhất, không cho nhân viên chọn tay.
     * Một phiếu xuất có thể lấy từ nhiều lô.
     */
    @Transactional
    public ExportOrderResponse createExportOrder(CreateExportOrderRequest request) {
        UserPrincipal principal = getCurrentUser();

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
            .orElseThrow(() -> new ResourceNotFoundException("Kho hàng", request.getWarehouseId()));

        if (principal.getWarehouseId() != null && !principal.getWarehouseId().equals(request.getWarehouseId())) {
            throw new BusinessException("Bạn không có quyền tạo phiếu xuất cho kho này");
        }

        User currentUser = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        ExportOrder order = ExportOrder.builder()
            .orderCode(generateOrderCode("XK"))
            .warehouse(warehouse)
            .createdBy(currentUser)
            .status(ExportStatus.PENDING)
            .note(request.getNote())
            .recipient(request.getRecipient())
            .build();

        List<ExportOrderDetail> details = new ArrayList<>();

        for (CreateExportOrderRequest.ExportDetailItem item : request.getDetails()) {
            Medicine medicine = medicineRepository.findById(item.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Thuốc", item.getMedicineId()));

            // === CƠ CHẾ FEFO: Tự động chọn lô ===
            List<ExportOrderDetail> fefoDetails = applyFEFO(order, medicine, item.getQuantity(), warehouse);
            details.addAll(fefoDetails);
        }

        order.setDetails(details);
        ExportOrder saved = exportOrderRepository.save(order);
        log.info("Tạo phiếu xuất {} theo FEFO bởi {}", saved.getOrderCode(), principal.getUsername());
        return mapToResponse(saved);
    }

    /**
     * Logic FEFO cốt lõi:
     * 1. Lấy các lô sắp xếp theo hạn dùng ASC (hết hạn gần nhất trước)
     * 2. Lần lượt lấy từ từng lô cho đến đủ số lượng
     * 3. Dùng Pessimistic Locking khi trừ tồn để tránh race condition
     */
    private List<ExportOrderDetail> applyFEFO(ExportOrder order, Medicine medicine, int requiredQuantity, Warehouse warehouse) {
        // Kiểm tra tổng tồn trước (không lock, chỉ để báo lỗi nhanh)
        Integer totalStock = batchRepository.getTotalStock(medicine.getId(), warehouse.getId(), LocalDate.now());
        if (totalStock == null || totalStock < requiredQuantity) {
            throw new BusinessException(String.format(
                "Không đủ tồn kho cho thuốc '%s'. Cần: %d, Hiện có: %d",
                medicine.getName(), requiredQuantity, totalStock == null ? 0 : totalStock
            ));
        }

        // Lấy danh sách lô theo FEFO (hạn dùng gần nhất trước)
        List<Batch> fefoSortedBatches = batchRepository.findAvailableBatchesForFEFO(
            medicine.getId(), warehouse.getId(), LocalDate.now()
        );

        List<ExportOrderDetail> exportDetails = new ArrayList<>();
        int remaining = requiredQuantity;

        for (Batch batch : fefoSortedBatches) {
            if (remaining <= 0) break;

            // Pessimistic Locking: khóa row trước khi trừ tồn
            Batch lockedBatch = batchRepository.findByIdWithLock(batch.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lô hàng", batch.getId()));

            // Double-check sau khi lock
            if (lockedBatch.getQuantity() <= 0 || lockedBatch.isExpired()) continue;

            int takeQuantity = Math.min(remaining, lockedBatch.getQuantity());

            // Trừ tồn
            lockedBatch.setQuantity(lockedBatch.getQuantity() - takeQuantity);
            batchRepository.save(lockedBatch);

            ExportOrderDetail detail = ExportOrderDetail.builder()
                .exportOrder(order)
                .batch(lockedBatch)
                .medicine(medicine)
                .quantity(takeQuantity)
                .build();
            exportDetails.add(detail);

            remaining -= takeQuantity;
            log.info("FEFO: lấy {} từ lô {} (hạn: {})", takeQuantity, lockedBatch.getBatchNumber(), lockedBatch.getExpiryDate());
        }

        if (remaining > 0) {
            throw new BusinessException("Không đủ tồn kho hợp lệ cho thuốc: " + medicine.getName());
        }

        return exportDetails;
    }

    @Transactional
    public ExportOrderResponse approveExportOrder(Long orderId, boolean approved, String rejectReason) {
        ExportOrder order = exportOrderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Phiếu xuất", orderId));

        if (order.getStatus() != ExportStatus.PENDING) {
            throw new BusinessException("Phiếu xuất không ở trạng thái chờ duyệt");
        }

        UserPrincipal principal = getCurrentUser();
        User approver = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        if (approved) {
            order.setStatus(ExportStatus.COMPLETED);
            order.setApprovedBy(approver);
            order.setApprovedAt(LocalDateTime.now());
        } else {
            // Hoàn trả tồn kho nếu từ chối
            for (ExportOrderDetail detail : order.getDetails()) {
                Batch batch = batchRepository.findByIdWithLock(detail.getBatch().getId()).orElseThrow();
                batch.setQuantity(batch.getQuantity() + detail.getQuantity());
                batchRepository.save(batch);
            }
            order.setStatus(ExportStatus.REJECTED);
        }

        return mapToResponse(exportOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public Page<ExportOrderResponse> getExportOrders(Long warehouseId, ExportStatus status, Pageable pageable) {
        return exportOrderRepository.findByWarehouseAndStatus(warehouseId, status, pageable)
            .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ExportOrderResponse getById(Long id) {
        return mapToResponse(exportOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Phiếu xuất", id)));
    }

    private ExportOrderResponse mapToResponse(ExportOrder order) {
        List<ExportOrderResponse.DetailItem> detailItems = order.getDetails().stream()
            .map(d -> ExportOrderResponse.DetailItem.builder()
                .id(d.getId())
                .batchId(d.getBatch().getId())
                .batchNumber(d.getBatch().getBatchNumber())
                .expiryDate(d.getBatch().getExpiryDate())
                .medicineId(d.getMedicine().getId())
                .medicineName(d.getMedicine().getName())
                .quantity(d.getQuantity())
                .build())
            .toList();

        return ExportOrderResponse.builder()
            .id(order.getId())
            .orderCode(order.getOrderCode())
            .warehouseId(order.getWarehouse().getId())
            .warehouseName(order.getWarehouse().getName())
            .createdByName(order.getCreatedBy().getFullName())
            .approvedByName(order.getApprovedBy() != null ? order.getApprovedBy().getFullName() : null)
            .status(order.getStatus())
            .note(order.getNote())
            .recipient(order.getRecipient())
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

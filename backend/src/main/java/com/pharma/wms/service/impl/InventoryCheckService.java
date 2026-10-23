package com.pharma.wms.service.impl;

import com.pharma.wms.entity.InventoryCheck;
import com.pharma.wms.entity.InventoryCheckDetail;
import com.pharma.wms.entity.Batch;
import com.pharma.wms.entity.User;
import com.pharma.wms.entity.Warehouse;
import com.pharma.wms.enums.InventoryStatus;
import com.pharma.wms.exception.BusinessException;
import com.pharma.wms.exception.ResourceNotFoundException;
import com.pharma.wms.repository.BatchRepository;
import com.pharma.wms.repository.InventoryCheckRepository;
import com.pharma.wms.repository.UserRepository;
import com.pharma.wms.repository.WarehouseRepository;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryCheckService {

    private final InventoryCheckRepository inventoryCheckRepository;
    private final WarehouseRepository warehouseRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    /**
     * Tạo phiếu kiểm kê - tự động snapshot tồn hệ thống hiện tại
     */
    @Transactional
    public InventoryCheck createInventoryCheck(Long warehouseId, String note) {
        UserPrincipal principal = getCurrentUser();
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new ResourceNotFoundException("Kho hàng", warehouseId));
        User currentUser = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        InventoryCheck check = InventoryCheck.builder()
            .checkCode(generateCode())
            .warehouse(warehouse)
            .createdBy(currentUser)
            .status(InventoryStatus.IN_PROGRESS)
            .note(note)
            .build();

        // Snapshot tồn hệ thống tại thời điểm tạo
        List<Batch> batches = batchRepository.findByWarehouseIdAndActiveTrue(warehouseId);
        List<InventoryCheckDetail> details = new ArrayList<>();
        for (Batch batch : batches) {
            InventoryCheckDetail detail = InventoryCheckDetail.builder()
                .inventoryCheck(check)
                .batch(batch)
                .medicine(batch.getMedicine())
                .systemQuantity(batch.getQuantity())
                .build();
            details.add(detail);
        }
        check.setDetails(details);

        return inventoryCheckRepository.save(check);
    }

    /**
     * Nhập số lượng thực đếm
     */
    @Transactional
    public InventoryCheck updateActualQuantities(Long checkId, Map<Long, Integer> batchActualQty) {
        InventoryCheck check = inventoryCheckRepository.findById(checkId)
            .orElseThrow(() -> new ResourceNotFoundException("Phiếu kiểm kê", checkId));

        if (check.getStatus() != InventoryStatus.IN_PROGRESS) {
            throw new BusinessException("Phiếu kiểm kê không ở trạng thái đang thực hiện");
        }

        for (InventoryCheckDetail detail : check.getDetails()) {
            if (batchActualQty.containsKey(detail.getBatch().getId())) {
                detail.setActualQuantity(batchActualQty.get(detail.getBatch().getId()));
            }
        }
        check.setStatus(InventoryStatus.PENDING_APPROVAL);
        return inventoryCheckRepository.save(check);
    }

    /**
     * Quản lý kho duyệt điều chỉnh tồn kho
     */
    @Transactional
    public InventoryCheck approveInventoryCheck(Long checkId, boolean approved) {
        InventoryCheck check = inventoryCheckRepository.findById(checkId)
            .orElseThrow(() -> new ResourceNotFoundException("Phiếu kiểm kê", checkId));

        if (check.getStatus() != InventoryStatus.PENDING_APPROVAL) {
            throw new BusinessException("Phiếu kiểm kê chưa sẵn sàng để duyệt");
        }

        UserPrincipal principal = getCurrentUser();
        User approver = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        if (approved) {
            // Điều chỉnh tồn kho theo số liệu thực tế
            for (InventoryCheckDetail detail : check.getDetails()) {
                if (detail.getActualQuantity() != null) {
                    Batch batch = batchRepository.findByIdWithLock(detail.getBatch().getId()).orElseThrow();
                    int diff = detail.getActualQuantity() - detail.getSystemQuantity();
                    if (diff != 0) {
                        batch.setQuantity(detail.getActualQuantity());
                        batchRepository.save(batch);
                        log.info("Điều chỉnh tồn lô {}: {} -> {} (chênh lệch: {})",
                            batch.getBatchNumber(), detail.getSystemQuantity(), detail.getActualQuantity(), diff);
                    }
                }
            }
            check.setStatus(InventoryStatus.APPROVED);
            check.setApprovedBy(approver);
            check.setApprovedAt(LocalDateTime.now());
        } else {
            check.setStatus(InventoryStatus.REJECTED);
        }

        return inventoryCheckRepository.save(check);
    }

    @Transactional(readOnly = true)
    public Page<InventoryCheck> getInventoryChecks(Long warehouseId, Pageable pageable) {
        return inventoryCheckRepository.findByWarehouse(warehouseId, pageable);
    }

    @Transactional(readOnly = true)
    public InventoryCheck getById(Long id) {
        return inventoryCheckRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Phiếu kiểm kê", id));
    }

    private String generateCode() {
        return "KK-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
    }

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

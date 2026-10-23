package com.pharma.wms.service.impl;

import com.pharma.wms.dto.request.CreateTransferOrderRequest;
import com.pharma.wms.entity.*;
import com.pharma.wms.enums.TransferStatus;
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
public class TransferOrderService {

    private final TransferOrderRepository transferOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransferOrder createTransferOrder(CreateTransferOrderRequest request) {
        UserPrincipal principal = getCurrentUser();

        if (request.getSourceWarehouseId().equals(request.getDestWarehouseId())) {
            throw new BusinessException("Kho nguồn và kho đích không được trùng nhau");
        }

        Warehouse source = warehouseRepository.findById(request.getSourceWarehouseId())
            .orElseThrow(() -> new ResourceNotFoundException("Kho nguồn", request.getSourceWarehouseId()));
        Warehouse dest = warehouseRepository.findById(request.getDestWarehouseId())
            .orElseThrow(() -> new ResourceNotFoundException("Kho đích", request.getDestWarehouseId()));

        User currentUser = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        TransferOrder order = TransferOrder.builder()
            .orderCode(generateOrderCode("CK"))
            .sourceWarehouse(source)
            .destWarehouse(dest)
            .createdBy(currentUser)
            .status(TransferStatus.PENDING)
            .note(request.getNote())
            .build();

        List<TransferOrderDetail> details = new ArrayList<>();
        for (CreateTransferOrderRequest.TransferDetailItem item : request.getDetails()) {
            Batch sourceBatch = batchRepository.findById(item.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Lô hàng", item.getBatchId()));

            if (!sourceBatch.getWarehouse().getId().equals(source.getId())) {
                throw new BusinessException("Lô " + sourceBatch.getBatchNumber() + " không thuộc kho nguồn");
            }
            if (sourceBatch.getQuantity() < item.getQuantity()) {
                throw new BusinessException("Lô " + sourceBatch.getBatchNumber() + " không đủ số lượng để chuyển");
            }
            if (sourceBatch.isExpired()) {
                throw new BusinessException("Không được chuyển lô đã hết hạn: " + sourceBatch.getBatchNumber());
            }

            TransferOrderDetail detail = TransferOrderDetail.builder()
                .transferOrder(order)
                .sourceBatch(sourceBatch)
                .medicine(sourceBatch.getMedicine())
                .quantity(item.getQuantity())
                .batchNumber(sourceBatch.getBatchNumber())
                .expiryDate(sourceBatch.getExpiryDate())
                .manufactureDate(sourceBatch.getManufactureDate())
                .build();
            details.add(detail);
        }
        order.setDetails(details);
        return transferOrderRepository.save(order);
    }

    /**
     * Gửi hàng: PENDING -> IN_TRANSIT
     * Trừ tồn ở kho nguồn ngay khi gửi
     */
    @Transactional
    public TransferOrder sendTransfer(Long orderId) {
        TransferOrder order = transferOrderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Phiếu chuyển", orderId));

        if (order.getStatus() != TransferStatus.PENDING) {
            throw new BusinessException("Phiếu chuyển không ở trạng thái PENDING");
        }

        // Trừ tồn kho nguồn với Pessimistic Lock
        for (TransferOrderDetail detail : order.getDetails()) {
            Batch locked = batchRepository.findByIdWithLock(detail.getSourceBatch().getId()).orElseThrow();
            if (locked.getQuantity() < detail.getQuantity()) {
                throw new BusinessException("Lô " + locked.getBatchNumber() + " không đủ số lượng để chuyển");
            }
            locked.setQuantity(locked.getQuantity() - detail.getQuantity());
            batchRepository.save(locked);
        }

        order.setStatus(TransferStatus.IN_TRANSIT);
        order.setSentAt(LocalDateTime.now());
        log.info("Chuyển kho {} sang IN_TRANSIT, đã trừ tồn kho nguồn", order.getOrderCode());
        return transferOrderRepository.save(order);
    }

    /**
     * Xác nhận nhận hàng: IN_TRANSIT -> DELIVERED
     * Cộng tồn ở kho đích, tạo lô mới nếu cần
     */
    @Transactional
    public TransferOrder confirmDelivery(Long orderId) {
        TransferOrder order = transferOrderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Phiếu chuyển", orderId));

        if (order.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new BusinessException("Phiếu chuyển không ở trạng thái IN_TRANSIT");
        }

        UserPrincipal principal = getCurrentUser();
        User confirmer = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        // Kiểm soát phạm vi: chỉ người ở kho đích mới được xác nhận
        if (principal.getWarehouseId() != null && !principal.getWarehouseId().equals(order.getDestWarehouse().getId())) {
            throw new BusinessException("Bạn không có quyền xác nhận nhận hàng cho kho này");
        }

        for (TransferOrderDetail detail : order.getDetails()) {
            // Tìm lô tương ứng ở kho đích hoặc tạo mới
            Batch destBatch = Batch.builder()
                .batchNumber(detail.getBatchNumber())
                .medicine(detail.getMedicine())
                .warehouse(order.getDestWarehouse())
                .supplier(detail.getSourceBatch().getSupplier())
                .manufactureDate(detail.getManufactureDate())
                .expiryDate(detail.getExpiryDate())
                .quantity(detail.getQuantity())
                .initialQuantity(detail.getQuantity())
                .importPrice(detail.getSourceBatch().getImportPrice())
                .active(true)
                .build();
            Batch saved = batchRepository.save(destBatch);
            detail.setDestBatch(saved);
        }

        order.setStatus(TransferStatus.DELIVERED);
        order.setConfirmedBy(confirmer);
        order.setDeliveredAt(LocalDateTime.now());
        log.info("Chuyển kho {} DELIVERED, đã cộng tồn kho đích", order.getOrderCode());
        return transferOrderRepository.save(order);
    }

    @Transactional
    public TransferOrder cancelTransfer(Long orderId) {
        TransferOrder order = transferOrderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Phiếu chuyển", orderId));

        if (order.getStatus() == TransferStatus.DELIVERED) {
            throw new BusinessException("Không thể hủy phiếu đã giao hàng");
        }

        // Nếu đang IN_TRANSIT, hoàn trả tồn kho nguồn
        if (order.getStatus() == TransferStatus.IN_TRANSIT) {
            for (TransferOrderDetail detail : order.getDetails()) {
                Batch locked = batchRepository.findByIdWithLock(detail.getSourceBatch().getId()).orElseThrow();
                locked.setQuantity(locked.getQuantity() + detail.getQuantity());
                batchRepository.save(locked);
            }
        }

        order.setStatus(TransferStatus.CANCELLED);
        return transferOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Page<TransferOrder> getTransferOrders(Long warehouseId, TransferStatus status, Pageable pageable) {
        return transferOrderRepository.findByWarehouseAndStatus(warehouseId, status, pageable);
    }

    private String generateOrderCode(String prefix) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        return prefix + "-" + timestamp;
    }

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

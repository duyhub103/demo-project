package com.pharma.wms.service.impl;

import com.pharma.wms.dto.response.AlertResponse;
import com.pharma.wms.entity.Batch;
import com.pharma.wms.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final BatchRepository batchRepository;

    @Value("${app.warehouse.expiry-warning-days:90}")
    private int expiryWarningDays;

    @Transactional(readOnly = true)
    public AlertResponse getAlerts(Long warehouseId) {
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(expiryWarningDays);

        // Cảnh báo tồn thấp
        List<Object[]> lowStockRaw = batchRepository.findLowStockMedicines(warehouseId, today);
        List<AlertResponse.LowStockAlert> lowStockAlerts = lowStockRaw.stream()
            .map(row -> AlertResponse.LowStockAlert.builder()
                .medicineId((Long) row[0])
                .medicineName((String) row[1])
                .medicineCode((String) row[2])
                .currentStock(((Number) row[4]).intValue())
                .minStockLevel(((Number) row[3]).intValue())
                .deficit(((Number) row[3]).intValue() - ((Number) row[4]).intValue())
                .build())
            .toList();

        // Cảnh báo cận hạn
        List<Batch> nearExpiryBatches = batchRepository.findNearExpiryBatches(warehouseId, today, warningDate);
        List<AlertResponse.NearExpiryAlert> nearExpiryAlerts = nearExpiryBatches.stream()
            .map(batch -> AlertResponse.NearExpiryAlert.builder()
                .batchId(batch.getId())
                .batchNumber(batch.getBatchNumber())
                .medicineId(batch.getMedicine().getId())
                .medicineName(batch.getMedicine().getName())
                .quantity(batch.getQuantity())
                .expiryDate(batch.getExpiryDate().toString())
                .daysUntilExpiry(ChronoUnit.DAYS.between(today, batch.getExpiryDate()))
                .build())
            .toList();

        return AlertResponse.builder()
            .lowStockAlerts(lowStockAlerts)
            .nearExpiryAlerts(nearExpiryAlerts)
            .totalLowStock(lowStockAlerts.size())
            .totalNearExpiry(nearExpiryAlerts.size())
            .build();
    }
}

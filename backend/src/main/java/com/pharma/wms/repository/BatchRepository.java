package com.pharma.wms.repository;

import com.pharma.wms.entity.Batch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    /**
     * FEFO Query: Lấy các lô của thuốc tại kho, sắp xếp theo hạn dùng gần nhất trước (FEFO),
     * loại lô hết hạn và lô hết hàng.
     * Đây là truy vấn cốt lõi của hệ thống FEFO.
     */
    @Query("""
        SELECT b FROM Batch b
        WHERE b.medicine.id = :medicineId
          AND b.warehouse.id = :warehouseId
          AND b.quantity > 0
          AND b.expiryDate > :today
          AND b.active = true
        ORDER BY b.expiryDate ASC
    """)
    List<Batch> findAvailableBatchesForFEFO(
        @Param("medicineId") Long medicineId,
        @Param("warehouseId") Long warehouseId,
        @Param("today") LocalDate today
    );

    /**
     * Pessimistic Locking - Dùng khi trừ tồn kho để tránh race condition.
     * SELECT ... FOR UPDATE: khóa row cho đến khi transaction hoàn tất.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Batch b WHERE b.id = :id")
    Optional<Batch> findByIdWithLock(@Param("id") Long id);

    /**
     * Cảnh báo lô cận hạn: lấy các lô sẽ hết hạn trong warningDays ngày tới
     */
    @Query("""
        SELECT b FROM Batch b
        WHERE b.warehouse.id = :warehouseId
          AND b.quantity > 0
          AND b.active = true
          AND b.expiryDate > :today
          AND b.expiryDate <= :warningDate
        ORDER BY b.expiryDate ASC
    """)
    List<Batch> findNearExpiryBatches(
        @Param("warehouseId") Long warehouseId,
        @Param("today") LocalDate today,
        @Param("warningDate") LocalDate warningDate
    );

    /**
     * Lấy tổng tồn theo thuốc tại kho
     */
    @Query("""
        SELECT COALESCE(SUM(b.quantity), 0) FROM Batch b
        WHERE b.medicine.id = :medicineId
          AND b.warehouse.id = :warehouseId
          AND b.active = true
          AND b.expiryDate > :today
    """)
    Integer getTotalStock(
        @Param("medicineId") Long medicineId,
        @Param("warehouseId") Long warehouseId,
        @Param("today") LocalDate today
    );

    /**
     * Cảnh báo tồn thấp: thuốc có tồn < mức tối thiểu
     */
    @Query("""
        SELECT b.medicine.id, b.medicine.name, b.medicine.code,
               b.medicine.minStockLevel, SUM(b.quantity) as totalStock
        FROM Batch b
        WHERE b.warehouse.id = :warehouseId
          AND b.active = true
          AND b.expiryDate > :today
        GROUP BY b.medicine.id, b.medicine.name, b.medicine.code, b.medicine.minStockLevel
        HAVING SUM(b.quantity) < b.medicine.minStockLevel
    """)
    List<Object[]> findLowStockMedicines(
        @Param("warehouseId") Long warehouseId,
        @Param("today") LocalDate today
    );

    List<Batch> findByWarehouseIdAndActiveTrue(Long warehouseId);

    @Query("SELECT b FROM Batch b WHERE b.warehouse.id = :warehouseId AND b.medicine.id = :medicineId AND b.active = true ORDER BY b.expiryDate ASC")
    List<Batch> findByWarehouseAndMedicine(@Param("warehouseId") Long warehouseId, @Param("medicineId") Long medicineId);
}

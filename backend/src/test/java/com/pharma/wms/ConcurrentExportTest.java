package com.pharma.wms;

import com.pharma.wms.entity.*;
import com.pharma.wms.enums.WarehouseType;
import com.pharma.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test mô phỏng nhiều request đồng thời xuất kho cùng 1 lô.
 * Chứng minh Pessimistic Locking ngăn được tình trạng âm kho.
 *
 * Kịch bản: 10 thread cùng xuất 20 đơn vị từ lô chỉ có 100 đơn vị
 * -> Tổng xuất = 200 > 100 -> Phải chặn được, kết quả tồn kho >= 0
 */
@SpringBootTest
@ActiveProfiles("test")
public class ConcurrentExportTest {

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    private Batch testBatch;
    private static final int INITIAL_STOCK = 100;
    private static final int EXPORT_PER_THREAD = 20;
    private static final int THREAD_COUNT = 10; // 10 threads * 20 = 200 > 100

    @BeforeEach
    @Transactional
    void setUp() {
        // Tạo kho và thuốc test
        Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
            .name("Test Warehouse")
            .code("TEST-" + System.currentTimeMillis())
            .type(WarehouseType.CENTRAL)
            .active(true)
            .build());

        Medicine medicine = medicineRepository.save(Medicine.builder()
            .name("Test Medicine")
            .code("TEST-MED-" + System.currentTimeMillis())
            .unit("Viên")
            .minStockLevel(0)
            .active(true)
            .build());

        testBatch = batchRepository.save(Batch.builder()
            .batchNumber("TEST-BATCH-CONCURRENT")
            .medicine(medicine)
            .warehouse(warehouse)
            .expiryDate(LocalDate.now().plusYears(2))
            .manufactureDate(LocalDate.now().minusMonths(6))
            .quantity(INITIAL_STOCK)
            .initialQuantity(INITIAL_STOCK)
            .importPrice(BigDecimal.TEN)
            .active(true)
            .build());
    }

    @Test
    @DisplayName("Pessimistic Locking - 10 thread đồng thời trừ tồn, tồn kho không được âm")
    void testConcurrentExportWithPessimisticLocking() throws InterruptedException {
        int numThreads = THREAD_COUNT;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1); // Đồng bộ start
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Chờ tất cả thread ready
                    tryExport(threadId, EXPORT_PER_THREAD, successCount, failCount, results);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Bắn tín hiệu start đồng thời
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Kiểm tra kết quả
        Batch finalBatch = batchRepository.findById(testBatch.getId()).orElseThrow();
        int finalStock = finalBatch.getQuantity();
        int totalExported = successCount.get() * EXPORT_PER_THREAD;

        System.out.println("\n========== KẾT QUẢ TEST ĐỒNG THỜI ==========");
        System.out.println("Tồn ban đầu: " + INITIAL_STOCK);
        System.out.println("Số thread: " + numThreads + " (mỗi thread xuất " + EXPORT_PER_THREAD + ")");
        System.out.println("Tổng cần xuất: " + (numThreads * EXPORT_PER_THREAD));
        System.out.println("Thành công: " + successCount.get() + " thread");
        System.out.println("Thất bại (bị chặn): " + failCount.get() + " thread");
        System.out.println("Tổng đã xuất: " + totalExported);
        System.out.println("Tồn kho cuối: " + finalStock);
        System.out.println("Kiểm tra: " + INITIAL_STOCK + " - " + totalExported + " = " + (INITIAL_STOCK - totalExported));
        results.forEach(r -> System.out.println("  " + r));
        System.out.println("==============================================\n");

        // Assertions quan trọng:
        assertThat(finalStock).isGreaterThanOrEqualTo(0)
            .as("Tồn kho KHÔNG ĐƯỢC ÂM dù có nhiều request đồng thời");

        assertThat(finalStock).isEqualTo(INITIAL_STOCK - totalExported)
            .as("Tồn kho phải chính xác: ban đầu - đã xuất thành công");

        assertThat(successCount.get()).isLessThanOrEqualTo(INITIAL_STOCK / EXPORT_PER_THREAD)
            .as("Số thread thành công không vượt quá số lần có thể xuất");
    }

    private void tryExport(int threadId, int qty, AtomicInteger successCount, AtomicInteger failCount, List<String> results) {
        try {
            // Sử dụng Spring transaction mới cho mỗi thread
            batchRepository.findByIdWithLock(testBatch.getId()).ifPresent(locked -> {
                if (locked.getQuantity() >= qty) {
                    locked.setQuantity(locked.getQuantity() - qty);
                    batchRepository.save(locked);
                    successCount.incrementAndGet();
                    results.add(String.format("Thread-%d: THÀNH CÔNG - xuất %d (còn %d)", threadId, qty, locked.getQuantity()));
                } else {
                    failCount.incrementAndGet();
                    results.add(String.format("Thread-%d: BỊ CHẶN - tồn chỉ còn %d < %d", threadId, locked.getQuantity(), qty));
                }
            });
        } catch (Exception e) {
            failCount.incrementAndGet();
            results.add(String.format("Thread-%d: LỖI - %s", threadId, e.getMessage()));
        }
    }
}

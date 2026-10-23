package com.pharma.wms.config;

import com.pharma.wms.entity.*;
import com.pharma.wms.enums.Role;
import com.pharma.wms.enums.WarehouseType;
import com.pharma.wms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Khởi tạo dữ liệu mẫu khi ứng dụng chạy lần đầu.
 * Tạo các tài khoản admin, kho tổng, kho chi nhánh, và dữ liệu mẫu.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Dữ liệu đã tồn tại, bỏ qua khởi tạo.");
            return;
        }

        log.info("=== Khởi tạo dữ liệu mẫu Pharma WMS ===");

        // === Tạo kho ===
        Warehouse khoTong = warehouseRepository.save(Warehouse.builder()
            .name("Kho Tổng Hà Nội")
            .code("KT-HN")
            .type(WarehouseType.CENTRAL)
            .address("123 Đường Láng, Đống Đa, Hà Nội")
            .phone("024-3456-7890")
            .active(true)
            .build());

        Warehouse khoCN1 = warehouseRepository.save(Warehouse.builder()
            .name("Kho Chi Nhánh TP.HCM")
            .code("KCN-HCM")
            .type(WarehouseType.BRANCH)
            .address("456 Nguyễn Văn Linh, Quận 7, TP.HCM")
            .phone("028-9876-5432")
            .active(true)
            .build());

        Warehouse khoCN2 = warehouseRepository.save(Warehouse.builder()
            .name("Kho Chi Nhánh Đà Nẵng")
            .code("KCN-DN")
            .type(WarehouseType.BRANCH)
            .address("789 Lê Duẩn, Hải Châu, Đà Nẵng")
            .phone("0236-123-4567")
            .active(true)
            .build());

        log.info("Tạo {} kho thành công", 3);

        // === Tạo người dùng ===
        userRepository.save(User.builder()
            .username("admin")
            .password(passwordEncoder.encode("Admin@123"))
            .fullName("Quản trị viên Hệ thống")
            .email("admin@pharmawms.vn")
            .role(Role.ADMIN)
            .warehouse(null) // Admin = toàn hệ thống
            .active(true)
            .build());

        userRepository.save(User.builder()
            .username("manager_hn")
            .password(passwordEncoder.encode("Manager@123"))
            .fullName("Nguyễn Văn Quản")
            .email("manager.hn@pharmawms.vn")
            .role(Role.WAREHOUSE_MANAGER)
            .warehouse(khoTong)
            .active(true)
            .build());

        userRepository.save(User.builder()
            .username("staff_hn")
            .password(passwordEncoder.encode("Staff@123"))
            .fullName("Trần Thị Nhân Viên")
            .email("staff.hn@pharmawms.vn")
            .role(Role.WAREHOUSE_STAFF)
            .warehouse(khoTong)
            .active(true)
            .build());

        userRepository.save(User.builder()
            .username("manager_hcm")
            .password(passwordEncoder.encode("Manager@123"))
            .fullName("Lê Văn Nam")
            .email("manager.hcm@pharmawms.vn")
            .role(Role.WAREHOUSE_MANAGER)
            .warehouse(khoCN1)
            .active(true)
            .build());

        userRepository.save(User.builder()
            .username("staff_hcm")
            .password(passwordEncoder.encode("Staff@123"))
            .fullName("Phạm Thị Lan")
            .email("staff.hcm@pharmawms.vn")
            .role(Role.WAREHOUSE_STAFF)
            .warehouse(khoCN1)
            .active(true)
            .build());

        log.info("Tạo 5 tài khoản người dùng thành công");

        // === Tạo nhà cung cấp ===
        supplierRepository.save(Supplier.builder()
            .name("Công ty Dược phẩm Trung ương 1")
            .code("NCC-TW1")
            .address("170 Lê Lợi, Hoàn Kiếm, Hà Nội")
            .phone("024-3825-1234")
            .email("info@duoctrunguong1.vn")
            .contactPerson("Nguyễn Dược Sĩ")
            .active(true)
            .build());

        Supplier ncc2 = supplierRepository.save(Supplier.builder()
            .name("Imexpharm Corporation")
            .code("NCC-IMP")
            .address("4/6 Hoàng Diệu, Sa Đéc, Đồng Tháp")
            .phone("0277-378-6268")
            .email("info@imexpharm.com")
            .active(true)
            .build());

        supplierRepository.save(Supplier.builder()
            .name("Dược phẩm OPV")
            .code("NCC-OPV")
            .address("Km9, QL1A, Đông Anh, Hà Nội")
            .phone("024-3882-0008")
            .email("info@opv.vn")
            .active(true)
            .build());

        log.info("Tạo 3 nhà cung cấp thành công");

        // === Tạo danh mục thuốc ===
        Medicine medPcm = medicineRepository.save(Medicine.builder()
            .name("Paracetamol 500mg")
            .code("MED-PCM500")
            .activeIngredient("Paracetamol")
            .dosageForm("Viên nén")
            .unit("Viên")
            .concentration("500mg")
            .manufacturer("Imexpharm")
            .minStockLevel(1000)
            .unitPrice(new BigDecimal("500"))
            .active(true)
            .build());

        Medicine medAmx = medicineRepository.save(Medicine.builder()
            .name("Amoxicillin 500mg")
            .code("MED-AMX500")
            .activeIngredient("Amoxicillin")
            .dosageForm("Viên nang")
            .unit("Viên")
            .concentration("500mg")
            .manufacturer("OPV")
            .minStockLevel(500)
            .unitPrice(new BigDecimal("3500"))
            .active(true)
            .build());

        Medicine medVitc = medicineRepository.save(Medicine.builder()
            .name("Vitamin C 1000mg")
            .code("MED-VITC")
            .activeIngredient("Acid Ascorbic")
            .dosageForm("Viên sủi")
            .unit("Viên")
            .concentration("1000mg")
            .minStockLevel(2000)
            .unitPrice(new BigDecimal("2000"))
            .active(true)
            .build());

        medicineRepository.save(Medicine.builder()
            .name("Metformin 850mg")
            .code("MED-MET850")
            .activeIngredient("Metformin HCl")
            .dosageForm("Viên nén bao phim")
            .unit("Viên")
            .concentration("850mg")
            .minStockLevel(300)
            .unitPrice(new BigDecimal("4000"))
            .active(true)
            .build());

        medicineRepository.save(Medicine.builder()
            .name("Omeprazole 20mg")
            .code("MED-OMP20")
            .activeIngredient("Omeprazole")
            .dosageForm("Viên nang cứng")
            .unit("Viên")
            .concentration("20mg")
            .minStockLevel(200)
            .unitPrice(new BigDecimal("6000"))
            .active(true)
            .build());

        medicineRepository.save(Medicine.builder()
            .name("Losartan 50mg")
            .code("MED-LOS50")
            .activeIngredient("Losartan Potassium")
            .dosageForm("Viên nén bao phim")
            .unit("Viên")
            .concentration("50mg")
            .minStockLevel(150)
            .unitPrice(new BigDecimal("8500"))
            .active(true)
            .build());

        log.info("Tạo 6 loại thuốc thành công");

        // === Tạo các lô thuốc mẫu để demo ngay cơ chế FEFO và cảnh báo cận hạn ===
        LocalDate today = LocalDate.now();

        // Paracetamol có 3 lô với 3 hạn dùng khác nhau tại Kho Tổng Hà Nội
        batchRepository.save(Batch.builder()
            .batchNumber("LOT-PCM-2024A")
            .medicine(medPcm)
            .warehouse(khoTong)
            .supplier(ncc2)
            .manufactureDate(today.minusMonths(5))
            .expiryDate(today.plusDays(40)) // Cận hạn trong 40 ngày (CẢNH BÁO VÀNG FEFO)
            .quantity(500)
            .initialQuantity(500)
            .importPrice(new BigDecimal("450"))
            .build());

        batchRepository.save(Batch.builder()
            .batchNumber("LOT-PCM-2024B")
            .medicine(medPcm)
            .warehouse(khoTong)
            .supplier(ncc2)
            .manufactureDate(today.minusMonths(2))
            .expiryDate(today.plusMonths(6)) // HSD sau 6 tháng
            .quantity(1000)
            .initialQuantity(1000)
            .importPrice(new BigDecimal("460"))
            .build());

        batchRepository.save(Batch.builder()
            .batchNumber("LOT-PCM-2024C")
            .medicine(medPcm)
            .warehouse(khoTong)
            .supplier(ncc2)
            .manufactureDate(today.minusDays(10))
            .expiryDate(today.plusYears(1)) // HSD sau 1 năm
            .quantity(2000)
            .initialQuantity(2000)
            .importPrice(new BigDecimal("470"))
            .build());

        // Amoxicillin có lô cận hạn gấp (20 ngày - CẢNH BÁO ĐỎ)
        batchRepository.save(Batch.builder()
            .batchNumber("LOT-AMX-2024A")
            .medicine(medAmx)
            .warehouse(khoTong)
            .supplier(ncc2)
            .manufactureDate(today.minusMonths(10))
            .expiryDate(today.plusDays(20)) // Cận hạn 20 ngày
            .quantity(300)
            .initialQuantity(300)
            .importPrice(new BigDecimal("3200"))
            .build());

        batchRepository.save(Batch.builder()
            .batchNumber("LOT-AMX-2024B")
            .medicine(medAmx)
            .warehouse(khoTong)
            .supplier(ncc2)
            .manufactureDate(today.minusMonths(1))
            .expiryDate(today.plusMonths(14))
            .quantity(1500)
            .initialQuantity(1500)
            .importPrice(new BigDecimal("3300"))
            .build());

        // Vitamin C tồn kho thấp (800 viên < minStockLevel 2000)
        batchRepository.save(Batch.builder()
            .batchNumber("LOT-VITC-2024A")
            .medicine(medVitc)
            .warehouse(khoTong)
            .supplier(ncc2)
            .manufactureDate(today.minusMonths(3))
            .expiryDate(today.plusMonths(10))
            .quantity(800)
            .initialQuantity(1000)
            .importPrice(new BigDecimal("1800"))
            .build());

        log.info("Khởi tạo 6 lô thuốc mẫu cho cơ chế FEFO thành công");
        log.info("=== Khởi tạo dữ liệu hoàn tất ===");
        log.info("=== TÀI KHOẢN DEMO ===");
        log.info("Admin: admin / Admin@123");
        log.info("Quản lý kho HN: manager_hn / Manager@123");
        log.info("Nhân viên kho HN: staff_hn / Staff@123");
        log.info("Quản lý kho HCM: manager_hcm / Manager@123");
        log.info("Nhân viên kho HCM: staff_hcm / Staff@123");
    }
}

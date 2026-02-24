package com.example.medaiassistant.drg.catalog;

import com.example.medaiassistant.model.Drg;
import com.example.medaiassistant.repository.DrgRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * DrgCatalog不可变快照测试类
 * 
 * 按照TDD红-绿-重构流程实现不可变快照功能
 * 
 * @author MedAiAssistant Team
 * @version 1.0
 * @since 2025-10-23
 */
class DrgCatalogTest {

    private ClobParser clobParser;
    private DrgCatalogLoader drgCatalogLoader;
    
    @Mock
    private DrgRepository drgRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clobParser = new ClobParser();
        
        // 模拟数据库返回的数据，包含没有主要手术的情况
        List<Drg> mockDrgs = Arrays.asList(
            createMockDrg(1L, "DRG001", "心房颤动DRG", 
                         "I48.000 阵发性心房颤动", 
                         "37.9000x001 经皮左心耳封堵术"),
            createMockDrg(2L, "DRG002", "高血压DRG", 
                         "I10.x00 原发性高血压", 
                         "88.7901 冠状动脉造影"),
            createMockDrg(3L, "DRG003", "糖尿病DRG", 
                         "E11.900 2型糖尿病", 
                         null), // 没有主要手术
            createMockDrg(4L, "DRG004", "肺炎DRG", 
                         "J18.900 肺炎", 
                         "/") // 主要手术为"/"
        );
        
        when(drgRepository.findAll()).thenReturn(mockDrgs);
        
        drgCatalogLoader = new DrgCatalogLoader(clobParser, drgRepository);
    }
    
    private Drg createMockDrg(Long id, String code, String name, String diagnoses, String procedures) {
        Drg drg = new Drg();
        drg.setDrgId(id);
        drg.setDrgCode(code);
        drg.setDrgName(name);
        drg.setMainDiagnoses(diagnoses);
        drg.setMainProcedures(procedures);
        drg.setWeight(BigDecimal.valueOf(1.5));
        drg.setInsurancePayment(BigDecimal.valueOf(10000));
        return drg;
    }

    // ==================== 红阶段：编写会失败的测试用例 ====================

    /**
     * 测试快照不可变性 - 应该抛出异常
     * 
     * 红阶段：这个测试应该失败，因为我们还没有实现不可变性保护
     */
    @Test
    void shouldThrowExceptionWhenModifyingCatalog() {
        // 给定一个已构建的DrgCatalog
        DrgCatalog catalog = drgCatalogLoader.loadCatalog();
        
        // 当尝试修改快照内容时
        // 那么应该抛出UnsupportedOperationException
        assertThatThrownBy(() -> catalog.getDrgRecords().add(new DrgParsedRecord("TEST001", List.of(), List.of())))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * 测试并发读取安全 - 应该没有并发修改异常
     * 
     * 红阶段：这个测试应该验证并发读取的安全性
     */
    @Test
    void shouldBeThreadSafeForConcurrentReads() throws InterruptedException, ExecutionException {
        // 给定一个DrgCatalog
        DrgCatalog catalog = drgCatalogLoader.loadCatalog();
        
        // 当多个线程同时读取时
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            futures.add(executor.submit(() -> {
                catalog.getDrgRecords().forEach(record -> {
                    assertThat(record.getDrgId()).isNotNull();
                });
                return true;
            }));
        }
        
        // 那么所有线程都应该成功完成
        for (Future<Boolean> future : futures) {
            assertThat(future.get()).isTrue();
        }
        
        executor.shutdown();
    }

    /**
     * 测试版本标识功能
     * 
     * 红阶段：验证版本标识的正确性
     */
    @Test
    void shouldHaveVersionIdentifier() {
        // 给定一个已加载的目录
        DrgCatalog catalog = drgCatalogLoader.loadCatalog();
        
        // 那么目录应该有版本标识
        assertThat(catalog.getVersion()).isNotNull();
        assertThat(catalog.getVersion()).isNotEmpty();
    }

    /**
     * 测试原子替换机制
     * 
     * 绿阶段：验证原子替换的正确性
     */
    @Test
    void shouldAtomicallyReplaceCatalog() throws InterruptedException {
        // 给定一个初始的DrgCatalog
        String initialVersion = drgCatalogLoader.getVersion();
        
        // 等待确保时间戳不同
        Thread.sleep(10);
        
        // 当执行刷新时
        DrgCatalog newCatalog = drgCatalogLoader.reload();
        String newVersion = drgCatalogLoader.getVersion();
        
        // 那么版本应该更新，且当前目录指向新版本
        assertThat(newVersion).isNotEqualTo(initialVersion);
        assertThat(drgCatalogLoader.getCurrentCatalog()).isEqualTo(newCatalog);
    }

    /**
     * 测试刷新期间的读取一致性
     * 
     * 红阶段：验证刷新期间读取的一致性
     */
    @Test
    void shouldMaintainReadConsistencyDuringRefresh() throws InterruptedException {
        // 给定一个正在刷新的场景
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch refreshCompleted = new CountDownLatch(1);
        
        // 模拟刷新过程
        Thread refreshThread = new Thread(() -> {
            refreshStarted.countDown();
            // 模拟刷新延迟
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            drgCatalogLoader.reload();
            refreshCompleted.countDown();
        });
        
        // 当刷新过程中有读取操作时
        refreshThread.start();
        refreshStarted.await();
        
        // 读取操作应该不受影响
        DrgCatalog readingCatalog = drgCatalogLoader.getCurrentCatalog();
        assertThat(readingCatalog).isNotNull();
        
        refreshCompleted.await();
        // 刷新完成后应该使用新版本
        assertThat(drgCatalogLoader.getCurrentCatalog()).isNotEqualTo(readingCatalog);
    }

    /**
     * 测试记录数量正确性
     * 
     * 红阶段：验证记录数量的正确性
     */
    @Test
    void shouldHaveCorrectRecordCount() {
        // 给定一个已加载的目录
        DrgCatalog catalog = drgCatalogLoader.loadCatalog();
        
        // 那么记录数量应该正确（现在有4条记录）
        assertThat(catalog.getRecordCount()).isEqualTo(4);
        assertThat(catalog.getDrgRecords()).hasSize(4);
    }

    /**
     * 测试诊断和手术数据完整性
     * 
     * 红阶段：验证数据完整性
     */
    @Test
    void shouldHaveCompleteDiagnosisAndProcedureData() {
        // 给定一个已加载的目录
        DrgCatalog catalog = drgCatalogLoader.loadCatalog();
        
        // 那么应该包含完整的诊断和手术数据
        List<DrgParsedRecord> records = catalog.getDrgRecords();
        assertThat(records).isNotEmpty();
        
        for (DrgParsedRecord record : records) {
            assertThat(record.getDrgId()).isNotNull();
            assertThat(record.getDiagnoses()).isNotNull();
            assertThat(record.getProcedures()).isNotNull();
        }
    }
}

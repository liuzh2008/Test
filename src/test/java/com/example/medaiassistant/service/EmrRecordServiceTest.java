package com.example.medaiassistant.service;

import com.example.medaiassistant.dto.EmrRecordListDTO;
import com.example.medaiassistant.dto.EmrRecordContentDTO;
import com.example.medaiassistant.repository.EmrRecordRepository;
import com.example.medaiassistant.repository.EmrContentRepository;
import com.example.medaiassistant.model.EmrContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * EmrRecordService单元测试类
 * 测试新添加的两个业务逻辑方法
 * 
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class EmrRecordServiceTest {

    @Mock
    private EmrRecordRepository emrRecordRepository;

    @Mock
    private EmrContentRepository emrContentRepository;

    @InjectMocks
    private EmrRecordService emrRecordService;

    private Object[] sampleResult1;
    private Object[] sampleResult2;

    @BeforeEach
    void setUp() {
        // 设置测试数据 - EmrContent 查询返回 Object[] 格式: id, docTypeName, docTitleTime
        sampleResult1 = new Object[]{1L, "入院记录", new Timestamp(System.currentTimeMillis())};
        sampleResult2 = new Object[]{2L, "病程记录", new Timestamp(System.currentTimeMillis())};
    }

    /**
     * 测试获取病历记录列表 - 正常情况
     */
    @Test
    void getEmrRecordListByPatientId_ShouldReturnDTOList_WhenDataExists() {
        // 准备
        String patientId = "PAT001";
        List<Object[]> repositoryResults = Arrays.asList(sampleResult1, sampleResult2);
        when(emrContentRepository.findByPatientIdAndDeleteMarkZero(patientId)).thenReturn(repositoryResults);

        // 执行
        List<EmrRecordListDTO> result = emrRecordService.getEmrRecordListByPatientId(patientId);

        // 验证
        assertNotNull(result);
        assertEquals(2, result.size());
        
        EmrRecordListDTO firstRecord = result.get(0);
        assertEquals("1", firstRecord.getID());
        assertEquals("入院记录", firstRecord.getDOC_TYPE_NAME());
        assertNotNull(firstRecord.getDOC_TITLE_TIME());
        
        EmrRecordListDTO secondRecord = result.get(1);
        assertEquals("2", secondRecord.getID());
        assertEquals("病程记录", secondRecord.getDOC_TYPE_NAME());
        assertNotNull(secondRecord.getDOC_TITLE_TIME());
    }

    /**
     * 测试获取病历记录列表 - 病人ID不存在时返回空列表
     */
    @Test
    void getEmrRecordListByPatientId_ShouldReturnEmptyList_WhenNoDataExists() {
        // 准备
        String patientId = "PAT999";
        when(emrContentRepository.findByPatientIdAndDeleteMarkZero(patientId)).thenReturn(Collections.emptyList());

        // 执行
        List<EmrRecordListDTO> result = emrRecordService.getEmrRecordListByPatientId(patientId);

        // 验证
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * 测试获取病历记录列表 - Repository异常时返回空列表
     */
    @Test
    void getEmrRecordListByPatientId_ShouldReturnEmptyList_WhenRepositoryThrowsException() {
        // 准备
        String patientId = "PAT001";
        when(emrContentRepository.findByPatientIdAndDeleteMarkZero(patientId))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // 执行
        List<EmrRecordListDTO> result = emrRecordService.getEmrRecordListByPatientId(patientId);

        // 验证
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * 测试获取病历记录列表 - 数据转换异常时返回空列表
     */
    @Test
    void getEmrRecordListByPatientId_ShouldReturnEmptyList_WhenDataConversionFails() {
        // 准备
        String patientId = "PAT001";
        // 模拟数据格式错误的情况
        Object[] invalidResult = new Object[]{123, 456, "invalid-date"};
        List<Object[]> repositoryResults = Collections.singletonList(invalidResult);
        when(emrContentRepository.findByPatientIdAndDeleteMarkZero(patientId)).thenReturn(repositoryResults);

        // 执行
        List<EmrRecordListDTO> result = emrRecordService.getEmrRecordListByPatientId(patientId);

        // 验证
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * 测试获取病历记录内容 - 正常情况
     */
    @Test
    void getEmrRecordContentById_ShouldReturnContentDTO_WhenRecordExists() {
        // 准备
        String recordId = "1";
        String expectedContent = "这是病历记录的具体内容...";
        when(emrContentRepository.findContentById(1L)).thenReturn(expectedContent);

        // 执行
        EmrRecordContentDTO result = emrRecordService.getEmrRecordContentById(recordId);

        // 验证
        assertNotNull(result);
        assertEquals(expectedContent, result.getCONTENT());
    }

    /**
     * 测试获取病历记录内容 - 记录ID不存在时返回空内容
     */
    @Test
    void getEmrRecordContentById_ShouldReturnEmptyContent_WhenRecordNotExists() {
        // 准备
        String recordId = "999";
        when(emrContentRepository.findContentById(999L)).thenReturn(null);

        // 执行
        EmrRecordContentDTO result = emrRecordService.getEmrRecordContentById(recordId);

        // 验证
        assertNotNull(result);
        assertEquals("", result.getCONTENT());
    }

    /**
     * 测试获取病历记录内容 - Repository异常时返回空内容
     */
    @Test
    void getEmrRecordContentById_ShouldReturnEmptyContent_WhenRepositoryThrowsException() {
        // 准备
        String recordId = "1";
        when(emrContentRepository.findContentById(1L))
                .thenThrow(new RuntimeException("数据库查询失败"));

        // 执行
        EmrRecordContentDTO result = emrRecordService.getEmrRecordContentById(recordId);

        // 验证
        assertNotNull(result);
        assertEquals("", result.getCONTENT());
    }

    /**
     * 测试获取病历记录内容 - 空记录ID处理
     */
    @Test
    void getEmrRecordContentById_ShouldHandleEmptyRecordId() {
        // 准备
        String recordId = "";
        // 空ID会导致NumberFormatException，无需mock

        // 执行
        EmrRecordContentDTO result = emrRecordService.getEmrRecordContentById(recordId);

        // 验证
        assertNotNull(result);
        assertEquals("", result.getCONTENT());
    }

    /**
     * 测试获取病历记录内容 - 空内容处理
     */
    @Test
    void getEmrRecordContentById_ShouldHandleEmptyContent() {
        // 准备
        String recordId = "1";
        when(emrContentRepository.findContentById(1L)).thenReturn("");

        // 执行
        EmrRecordContentDTO result = emrRecordService.getEmrRecordContentById(recordId);

        // 验证
        assertNotNull(result);
        assertEquals("", result.getCONTENT());
    }

    /**
     * 测试获取病历记录列表 - 边界情况：单个记录
     */
    @Test
    void getEmrRecordListByPatientId_ShouldHandleSingleRecord() {
        // 准备
        String patientId = "PAT001";
        List<Object[]> repositoryResults = Collections.singletonList(sampleResult1);
        when(emrContentRepository.findByPatientIdAndDeleteMarkZero(patientId)).thenReturn(repositoryResults);

        // 执行
        List<EmrRecordListDTO> result = emrRecordService.getEmrRecordListByPatientId(patientId);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getID());
        assertEquals("入院记录", result.get(0).getDOC_TYPE_NAME());
    }

    /**
     * 测试获取病历记录列表 - 边界情况：空病人ID
     */
    @Test
    void getEmrRecordListByPatientId_ShouldHandleEmptyPatientId() {
        // 准备
        String patientId = "";
        when(emrContentRepository.findByPatientIdAndDeleteMarkZero(patientId)).thenReturn(Collections.emptyList());

        // 执行
        List<EmrRecordListDTO> result = emrRecordService.getEmrRecordListByPatientId(patientId);

        // 验证
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

package com.example.medaiassistant.service;

import lombok.Data;

/**
 * 夜间同步结果统计类
 * 
 * <p>用于记录夜间定时同步任务（NightlySyncService）的执行结果，包括：</p>
 * <ul>
 *   <li>整体执行状态（成功/失败）</li>
 *   <li>执行耗时</li>
 *   <li>病人列表同步统计（按科室）</li>
 *   <li>化验结果同步统计（按病人）</li>
 *   <li>检查结果同步统计（按病人）</li>
 *   <li>EMR病历同步统计（按病人）</li>
 *   <li>医嘱同步统计（按病人）</li>
 * </ul>
 * 
 * <p><strong>使用场景</strong>：</p>
 * <pre>{@code
 * NightlySyncResult result = new NightlySyncResult();
 * result.setSuccess(true);
 * result.setDurationMs(60000L);
 * result.setPatientSyncSuccessDepts(2);
 * result.setLabSyncSuccess(25);
 * result.setOrderSyncSuccess(20);
 * // ...
 * }</pre>
 *
 * @author System
 * @version 1.1
 * @since 2026-01-13
 * @see NightlySyncService
 */
@Data
public class NightlySyncResult {

    /**
     * 同步任务是否成功完成
     * <p>true: 所有步骤执行完成（可能有部分失败）</p>
     * <p>false: 任务执行过程中发生严重异常</p>
     */
    private boolean success;

    /**
     * 错误信息
     * <p>当success=false时，记录导致任务失败的异常信息</p>
     */
    private String errorMessage;

    /**
     * 任务总执行时长（毫秒）
     */
    private long durationMs;

    // ========== 病人列表同步统计 ==========

    /**
     * 病人列表同步成功的科室数量
     */
    private int patientSyncSuccessDepts;

    /**
     * 病人列表同步失败的科室数量
     */
    private int patientSyncFailedDepts;

    /**
     * 在院病人总数（用于后续同步的基数）
     */
    private int totalPatients;

    // ========== 化验结果同步统计 ==========

    /**
     * 化验结果同步成功的病人数量
     */
    private int labSyncSuccess;

    /**
     * 化验结果同步失败的病人数量
     */
    private int labSyncFailed;

    // ========== 检查结果同步统计 ==========

    /**
     * 检查结果同步成功的病人数量
     */
    private int examSyncSuccess;

    /**
     * 检查结果同步失败的病人数量
     */
    private int examSyncFailed;

    // ========== EMR病历同步统计 ==========

    /**
     * EMR病历同步成功的病人数量
     */
    private int emrSyncSuccess;

    /**
     * EMR病历同步失败的病人数量
     */
    private int emrSyncFailed;

    // ========== 医嘱同步统计 ==========

    /**
     * 医嘱同步成功的病人数量
     */
    private int orderSyncSuccess;

    /**
     * 医嘱同步失败的病人数量
     */
    private int orderSyncFailed;
}

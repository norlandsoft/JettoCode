package com.jettech.code.service;

import com.jettech.code.dto.ScanProgressMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 扫描进度推送服务
 * 通过 WebSocket 实时推送扫描进度
 */
@Service
public class ScanProgressService {

    private static final Logger logger = LoggerFactory.getLogger(ScanProgressService.class);

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 推送进度更新
     */
    public void notifyProgress(Long scanId, String type, String message,
                               int progress, int completed, int total) {
        if (messagingTemplate == null) {
            return;
        }

        ScanProgressMessage progressMessage;

        switch (type) {
            case ScanProgressMessage.TYPE_PHASE:
                progressMessage = ScanProgressMessage.phase(scanId, message);
                break;
            case ScanProgressMessage.TYPE_COMPLETED:
                progressMessage = ScanProgressMessage.completed(scanId, 0, progress);
                break;
            case ScanProgressMessage.TYPE_CANCELLED:
                progressMessage = ScanProgressMessage.cancelled(scanId, message);
                break;
            case ScanProgressMessage.TYPE_ERROR:
                progressMessage = ScanProgressMessage.error(scanId, message);
                break;
            default:
                progressMessage = ScanProgressMessage.progress(scanId, progress, completed, total);
        }

        sendProgress(scanId, progressMessage);
    }

    /**
     * 推送任务完成消息
     */
    public void notifyTaskComplete(Long scanId, Long taskId, String serviceName,
                                   String checkItemName, int issueCount, String status) {
        if (messagingTemplate == null) {
            return;
        }

        ScanProgressMessage message = ScanProgressMessage.taskComplete(
                scanId, taskId, serviceName, checkItemName, issueCount, status);

        sendProgress(scanId, message);
        logger.debug("Task complete notification sent: scan={}, task={}, issues={}",
                scanId, taskId, issueCount);
    }

    /**
     * 推送扫描完成消息
     */
    public void notifyScanComplete(Long scanId, int totalIssues, int totalTasks) {
        if (messagingTemplate == null) {
            return;
        }

        ScanProgressMessage message = ScanProgressMessage.completed(scanId, totalIssues, 100);
        sendProgress(scanId, message);
        logger.info("Scan complete notification sent: scan={}, issues={}", scanId, totalIssues);
    }

    /**
     * 推送扫描取消消息
     */
    public void notifyScanCancelled(Long scanId, String reason) {
        if (messagingTemplate == null) {
            return;
        }

        ScanProgressMessage message = ScanProgressMessage.cancelled(scanId, reason);
        sendProgress(scanId, message);
        logger.info("Scan cancelled notification sent: scan={}", scanId);
    }

    /**
     * 发送进度消息到 WebSocket
     */
    private void sendProgress(Long scanId, ScanProgressMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/scan/" + scanId, message);
        } catch (Exception e) {
            logger.error("Failed to send progress message: {}", e.getMessage());
        }
    }
}

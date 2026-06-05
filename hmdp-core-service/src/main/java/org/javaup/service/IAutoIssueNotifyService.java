package org.javaup.service;


public interface IAutoIssueNotifyService {
    
    void sendAutoIssueNotify(Long voucherId, Long userId, Long orderId);
}
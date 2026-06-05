package org.javaup.service;

import org.javaup.entity.RollbackFailureLog;

public interface IRollbackAlertService {

    void sendRollbackAlert(RollbackFailureLog log);
}
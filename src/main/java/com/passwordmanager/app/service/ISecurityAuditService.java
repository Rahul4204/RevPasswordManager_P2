package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.AuditReport;

public interface ISecurityAuditService {
    AuditReport generateReport(Long userId, int oldPasswordDays);
}

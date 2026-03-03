package com.passwordmanager.app.dto;

import java.util.ArrayList;
import java.util.List;

public class AuditReport {
    private int totalEntries;
    private int securityScore = 100;
    private List<AuditItem> weakPasswords = new ArrayList<>();
    private List<AuditItem> reusedPasswords = new ArrayList<>();
    private List<AuditItem> oldPasswords = new ArrayList<>();

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int t) {
        this.totalEntries = t;
    }

    public int getSecurityScore() {
        return securityScore;
    }

    public void setSecurityScore(int s) {
        this.securityScore = Math.max(0, Math.min(100, s));
    }

    public List<AuditItem> getWeakPasswords() {
        return weakPasswords;
    }

    public List<AuditItem> getReusedPasswords() {
        return reusedPasswords;
    }

    public List<AuditItem> getOldPasswords() {
        return oldPasswords;
    }
}

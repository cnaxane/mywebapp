package model;

import java.time.LocalDateTime;

public class AuditLog {
    private int logId;
    private Integer loanId;
    private String actionType;
    private String actionDetails;
    private LocalDateTime performedAt;

    public AuditLog() {
    }

    public AuditLog(int logId, Integer loanId, String actionType, String actionDetails, LocalDateTime performedAt) {
        this.logId = logId;
        this.loanId = loanId;
        this.actionType = actionType;
        this.actionDetails = actionDetails;
        this.performedAt = performedAt;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public Integer getLoanId() {
        return loanId;
    }

    public void setLoanId(Integer loanId) {
        this.loanId = loanId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionDetails() {
        return actionDetails;
    }

    public void setActionDetails(String actionDetails) {
        this.actionDetails = actionDetails;
    }

    public LocalDateTime getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(LocalDateTime performedAt) {
        this.performedAt = performedAt;
    }
}

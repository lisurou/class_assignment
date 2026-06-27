package org.example.classAssignment.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrepareSpaceOperationLog {
    private Long logId;
    private Long prepareSpaceId;
    private String accountId;
    private String operationType;
    private String operationTarget;
    private String targetId;
    private String detail;
    private LocalDateTime createdAt;
}

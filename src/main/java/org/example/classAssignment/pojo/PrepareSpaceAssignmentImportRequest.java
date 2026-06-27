package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class PrepareSpaceAssignmentImportRequest {
    private String accountId;
    private String assignmentId;
    private Long targetFolderId;
}


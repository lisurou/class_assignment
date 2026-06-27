package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class PrepareSpaceMaterialImportRequest {
    private String accountId;
    private String category;
    private String mode;
    private String itemType;
    private Long itemId;
    private Long targetFolderId;
    private Long targetParentFolderId;
}


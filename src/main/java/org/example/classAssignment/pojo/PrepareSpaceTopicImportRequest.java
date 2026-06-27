package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class PrepareSpaceTopicImportRequest {
    private String accountId;
    private String topicId;
    private Boolean includeReplies;
    private Long targetFolderId;
}


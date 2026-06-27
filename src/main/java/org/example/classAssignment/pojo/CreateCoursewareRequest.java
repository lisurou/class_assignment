package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class CreateCoursewareRequest {
    private String accountId;
    private Long folderId;
    private String title;
    private String itemType;
    private String visibility;
    private String url;
}


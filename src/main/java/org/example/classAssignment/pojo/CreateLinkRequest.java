package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class CreateLinkRequest {
    private String accountId;
    private Long folderId;
    private String title;
    private String url;
}

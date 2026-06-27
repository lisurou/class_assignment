package org.example.classAssignment.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Courseware {
    private Long coursewareId;
    private Long folderId;
    private String title;
    private String itemType;
    private String visibility;
    private String url;
    private String originalName;
    private String storedName;
    private String relativePath;
    private Long size;
    private String contentType;
    private String createdBy;
    private LocalDateTime createdAt;
}


package org.example.classAssignment.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialAttachment {
    private Long attachmentId;
    private String courseId;
    private String category;
    private Long folderId;
    private String originalName;
    private String storedName;
    private String relativePath;
    private Long size;
    private String contentType;
    private String createdBy;
    private LocalDateTime createdAt;
}

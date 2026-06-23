package org.example.classAssignment.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialFolder {
    private Long folderId;
    private String courseId;
    private String category;
    private Long parentId;
    private String name;
    private String createdBy;
    private LocalDateTime createdAt;
}

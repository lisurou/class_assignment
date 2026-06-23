package org.example.classAssignment.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialLink {
    private Long linkId;
    private String courseId;
    private String category;
    private Long folderId;
    private String title;
    private String url;
    private String createdBy;
    private LocalDateTime createdAt;
}

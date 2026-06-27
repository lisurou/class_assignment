package org.example.classAssignment.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrepareSpace {
    private Long prepareSpaceId;
    private String name;
    private String spaceType;
    private String ownerId;
    private String courseId;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

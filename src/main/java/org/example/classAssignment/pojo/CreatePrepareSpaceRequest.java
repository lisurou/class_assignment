package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class CreatePrepareSpaceRequest {
    private String ownerId;
    private String name;
    private String spaceType;
    private String courseId;
    private String description;
}

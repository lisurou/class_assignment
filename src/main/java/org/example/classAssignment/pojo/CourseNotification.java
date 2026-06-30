package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class CourseNotification {
    private Long id;
    private String accountId;
    private String courseId;
    private String assignmentId;
    private String type;
    private String title;
    private String content;
    private String senderName;
    private Boolean readStatus;
    private String createdAt;
}

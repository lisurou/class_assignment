package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class Assignment {
    String accountId;
    String id;
    String assignmentId;
    String studentName;
    String studentId;
    String title;
    String deadline;
    String assignmentType;
    String content;
    Integer totalScore;
    String submit;
    String correct;
    Integer score;
    String submitContent;
    String fileName;
    String fileStoredName;
    Long fileSize;
    String fileContentType;
    String fileDownloadUrl;
    Boolean aiEnabled;
    Integer aiScore;
    String aiComment;
    Integer correctedCount;
    Integer pendingCount;
    Integer missingCount;
}

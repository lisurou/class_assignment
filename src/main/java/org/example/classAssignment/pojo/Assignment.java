package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class Assignment {
    String accountId;
    String id;
    String assignmentId;
    Long folderId;
    String submissionId;
    String submittedAt;
    String studentName;
    String studentId;
    String title;
    String publishTime;
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
    String attachmentName;
    String attachmentStoredName;
    Long attachmentSize;
    String attachmentContentType;
    String attachmentDownloadUrl;
    Boolean aiEnabled;
    Integer aiScore;
    String aiComment;
    String teacherComment;
    Integer correctedCount;
    Integer pendingCount;
    Integer missingCount;
}

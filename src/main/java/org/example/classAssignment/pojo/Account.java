package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class Account {
    private String accountId;
    private String phone;
    private String password;
    private String identity;
    private String name;
    private String school;
    private String studentId;
    private String college;
    private String major;
    private String classes;
    private String grade;
    private String enrollment;
    private String email;
    private String WeChat;
    private String learned;
    private String taught;
    private Boolean homeworkDue;
    private Boolean coursePrivateMessage;
    private Boolean topicReminder;
    private String top;
    private String archivedLearned;     // 学生归档的课程ID（逗号分隔）
    private String archivedTaught;      // 教师归档的课程ID（逗号分隔）
    private String avatarStoredName;
    private String avatarUrl;
}

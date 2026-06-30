package org.example.classAssignment.pojo;

import lombok.Data;

import java.util.List;

@Data
public class Result {
    private Boolean success;
    private String message;
    private Account account;
    private List<Account> accounts;
    private List<Course> learned;
    private List<Course> taught;
    private List<Course> top;
    private Courseware courseware;
    private List<Courseware> coursewares;
    private List<Assignment> assignments;
    private Course course;
    private Assignment assignment;
    private Topic topic;
    private List<Topic> topics;
    private Reply reply;
    private List<Reply> replies;
    private List<CourseNotification> notifications;
}

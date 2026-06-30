package org.example.classAssignment.service;

import org.example.classAssignment.pojo.Account;
import org.example.classAssignment.pojo.Assignment;
import org.example.classAssignment.pojo.Course;
import org.example.classAssignment.pojo.Result;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


public interface AccountServiceInterface {
    Account findByAccountId(String accountId);

    Account findByPhone(String phone);

    Boolean saveAccount(Account account);

    Boolean authenticate(String phone, String password);

    Boolean updateIdentity(Account account);

    Boolean updatePhone(Account account);

    Boolean updatePassword(Account account);

    Result updateAvatar(String accountId, MultipartFile file);

    Boolean updateBasicInformation(Account account);

    Course findByCourseId(String id);

    Boolean updateLearned(String accountId, String learned);

    String findLearned(String accountId);

    String findTaught(String accountId);

    Boolean updateTaught(String accountId, String taught);

    Boolean insertCourse(Course course);

    String findTop(String accountId);

    Boolean updateTop(String accountId, String top);

    Result findAssignment(String accountId, String id);
    Result updateAssignment(String accountId, String id, String assignmentId, String submitContent, MultipartFile file);
    Result deleteAssignmentSubmissionFile(String accountId, String id, String assignmentId);
    Result findSubmitAssignment(String accountId,String id,String assignmentId);
    Result updateScore(Integer score,String teacherComment,String accountId,String id,String assignmentId);
    Result insertAssignments(String accountIdNull,String id,Assignment assignment, MultipartFile file);
    Result updateCourseAssignment(String id, String assignmentId, Assignment assignment, MultipartFile file, Boolean removeAttachment);
    Result deleteCourseAssignment(String id, String assignmentId);
    Result toggleAssignmentAi(String id, String assignmentId, Boolean aiEnabled);
    Result getCourseMembers(String courseId);
    Result getNotifications(String accountId);
    Result markNotificationAsRead(Long notificationId, String accountId);
    Result markAllNotificationsAsRead(String accountId);
    Result sendAssignmentReminder(String courseId, String assignmentId, String targetAccountId, String teacherAccountId);
    Boolean findByAssignmentId(String assignmentId);
    String findStudents(String id);
    Boolean updateStudents(String students,String id);
    void syncCourseAssignmentsForStudent(String accountId, String id);
    Resource loadAssignmentFile(String accountId, String id, String assignmentId) throws IOException;
    Assignment getAssignmentFileMeta(String accountId, String id, String assignmentId);
    Resource loadAssignmentResource(String id, String assignmentId) throws IOException;
    Assignment getAssignmentResourceMeta(String id, String assignmentId);
    Resource loadAccountAvatar(String accountId) throws IOException;
}

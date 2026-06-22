package org.example.classAssignment.service;

import org.example.classAssignment.pojo.Account;
import org.example.classAssignment.pojo.Assignment;
import org.example.classAssignment.pojo.Course;
import org.example.classAssignment.pojo.Result;


public interface AccountServiceInterface {
    Account findByAccountId(String accountId);

    Account findByPhone(String phone);

    Boolean saveAccount(Account account);

    Boolean authenticate(String phone, String password);

    Boolean updateIdentity(Account account);

    Boolean updatePhone(Account account);

    Boolean updatePassword(Account account);

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
    Result updateAssignment(String accountId, String id, String assignmentId,String submitContent);
    Result findSubmitAssignment(String accountId,String id,String assignmentId);
    Result updateScore(Integer score,String accountId,String id,String assignmentId);
    Result insertAssignments(String accountIdNull,String id,Assignment assignment);
    Boolean findByAssignmentId(String assignmentId);
    String findStudents(String id);
    Boolean updateStudents(String students,String id);
}

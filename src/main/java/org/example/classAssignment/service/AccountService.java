package org.example.classAssignment.service;

import org.example.classAssignment.mapper.AccountMapper;
import org.example.classAssignment.pojo.Account;
import org.example.classAssignment.pojo.Assignment;
import org.example.classAssignment.pojo.Course;
import org.example.classAssignment.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class AccountService implements AccountServiceInterface {
    @Autowired
    private AccountMapper accountMapper;

    @Override
    public Account findByAccountId(String accountId) {
        return accountMapper.findByAccountId(accountId);
    }

    @Override
    public Account findByPhone(String phone) {
        return accountMapper.findByPhone(phone);
    }

    @Override
    public Boolean saveAccount(Account account) {
        if ("学生".equals(account.getIdentity())) {
            return accountMapper.insertStudentAccount(account);
        } else {
            return accountMapper.insertTeacherAccount(account);
        }
    }

    @Override
    public Boolean authenticate(String phone, String password) {
        Account user = accountMapper.findByPhone(phone);
        return user != null && user.getPassword().equals(password);
    }

    @Override
    public Boolean updateIdentity(Account account) {
        return accountMapper.updateIdentity(account) >= 0;
    }

    @Override
    public Boolean updatePhone(Account account) {
        return accountMapper.updatePhone(account) >= 0;
    }

    @Override
    public Boolean updatePassword(Account account) {
        return accountMapper.updatePassword(account) >= 0;
    }

    @Override
    public Boolean updateBasicInformation(Account account) {
        return accountMapper.updateBasicInformation(account) >= 0;
    }

    @Override
    public Course findByCourseId(String id) {
        return accountMapper.findByCourseId(id);
    }

    @Override
    public Boolean updateLearned(String accountId, String learned) {
        return accountMapper.updateLearned(accountId, learned);
    }

    @Override
    public String findLearned(String accountId) {
        return accountMapper.findLearned(accountId);
    }

    @Override
    public String findTaught(String accountId) {
        return accountMapper.findTaught(accountId);
    }

    @Override
    public Boolean updateTaught(String accountId, String taught) {
        return accountMapper.updateTaught(accountId, taught);
    }

    @Override
    public Boolean insertCourse(Course course) {
        return accountMapper.insertCourse(course);
    }

    @Override
    public String findTop(String accountId) {
        return accountMapper.findTop(accountId);
    }

    @Override
    public Boolean updateTop(String accountId, String top) {
        return accountMapper.updateTop(accountId, top);
    }

    //点击我学的课程卡片，展示我的作业
    @Override
    public Result findAssignment(String accountId, String id) {
        Result result = new Result();
        try {
            List<Assignment> assignments = accountMapper.findAssignment(accountId, id);
            Course course = accountMapper.findByCourseId(id);
            result.setSuccess(true);
            result.setMessage("作业成功展示");
            result.setCourse(course);
            result.setAssignments(assignments);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("作业展示失败：" + e.getMessage());
        }
        return result;
    }

    //提交作业
    @Override
    public Result updateAssignment(String accountId, String id, String assignmentId, String assignmentContent) {
        Result result = new Result();
        try {
            if (accountMapper.updateAssignment(accountId, id, assignmentId, assignmentContent) && accountMapper.updateSubmit(accountId, id, assignmentId, "已提交")) {
                Assignment assignment = accountMapper.findAssignmentById(assignmentId);
                result.setSuccess(true);
                result.setMessage("作业提交成功");
                result.setAssignment(assignment);
            } else {
                result.setSuccess(false);
                result.setMessage("作业提交失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("作业提交失败：" + e.getMessage());
        }
        return result;
    }

    //点击我教的课程卡片，展示已经提交的作业
    @Override
    public Result findSubmitAssignment(String accountId, String id, String assignmentId) {
        Result result = new Result();
        try {
            List<Assignment> assignments = accountMapper.findSubmitAssignment(id, assignmentId, "已提交");
            result.setSuccess(true);
            result.setMessage("成功查看已提交作业");
            result.setAssignments(assignments);
            System.out.println(assignments);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("无法查看已提交的作业：" + e.getMessage());
        }
        return result;
    }

    //批改作业
    @Override
    public Result updateScore(Integer score, String accountId, String id, String assignmentId) {
        Result result = new Result();
        try {
            if (accountMapper.updateScore(score, accountId, id, assignmentId)) {
                List<Assignment> assignments = accountMapper.findSubmitAssignment(id, assignmentId, "已提交");
                result.setSuccess(true);
                result.setMessage("批改作业成功");
                result.setAssignments(assignments);
            } else {
                result.setSuccess(false);
                result.setMessage("批改作业失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("批改作业失败：" + e.getMessage());
        }
        return result;
    }

    //发布作业，将作业填充到对应的账号，课程
    @Override
    public Result insertAssignments(String accountIdNull, String id, Assignment assignment) {
        Result result = new Result();
        try {
            //生成一个不重复的作业码
            String randomCourseCode = generateRandomCourseCode();
            //确保生成的作业码为新
            while (findByAssignmentId(randomCourseCode)) {
                randomCourseCode = generateRandomCourseCode();
            }
            //取出课程中的所有账号
            String accountIds = findStudents(id);
            accountMapper.insertAssignment(accountIdNull, id, randomCourseCode, assignment.getTitle(), assignment.getDeadline()
                    , assignment.getAssignmentType(), assignment.getContent(), assignment.getTotalScore());
            if (accountIds != null) {
                String[] accountIdArr = accountIds.split(",");
                for (int i = 0; i < accountIdArr.length; i++) {
                    String accountId = accountIdArr[i];
                    accountMapper.insertAssignment(accountId, id, randomCourseCode, assignment.getTitle(), assignment.getDeadline()
                            , assignment.getAssignmentType(), assignment.getContent(), assignment.getTotalScore());
                }
            }

            result.setSuccess(true);
            result.setMessage("发布作业成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("发布作业失败：" + e.getMessage());
        }
        return result;
    }

    //生成6位随机字符串（包含ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789）,课程码
    private String generateRandomCourseCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    @Override
    public Boolean findByAssignmentId(String assignmentId) {
        return accountMapper.findByAssignmentId(assignmentId) != null;
    }

    //获取课程的students信息
    @Override
    public String findStudents(String id) {
        return accountMapper.findStudents(id);
    }
    //加入课程时，更新课程的students信息
    @Override
    public Boolean updateStudents(String students, String id) {
        return accountMapper.updateStudents(students, id);
    }
}

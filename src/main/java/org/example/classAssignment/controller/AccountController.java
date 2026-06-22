package org.example.classAssignment.controller;

import org.example.classAssignment.pojo.*;
import org.example.classAssignment.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;


@RestController
//@Api(tags = "课堂派接口")
public class AccountController {
    @Autowired
    private AccountService accountService;

    // 生成11位随机字符串（包含a-z和0-9），账号
    private String generateRandomAccount() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(11);
        for (int i = 0; i < 11; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
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

    private List<Course> getCourses(String courseIds) {
        if (courseIds == null || courseIds.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Course> courses = new ArrayList<>();
        String[] ids = courseIds.split(",");
        for (String id : ids) {
            if (!id.trim().isEmpty()) { // 跳过空字符串元素
                Course course = accountService.findByCourseId(id);
                if (course != null) {
                    courses.add(course);
                }
            }
        }
        return courses;
    }

    private List<Course> updateLearnedCourses(String learnedCourses) {
        return getCourses(learnedCourses);
    }

    private List<Course> updateTaughtCourses(String taughtCourses) {
        return getCourses(taughtCourses);
    }

    private List<Course> updateTopCourses(String topCourses) {
        return getCourses(topCourses);
    }

    // 从逗号分隔的字符串中移除指定id
    private String removeCourseId(String original, String targetId) {
        if (original == null || original.isEmpty()) {
            return "";
        }
        List<String> ids = Arrays.asList(original.split(","));
        List<String> newIds = ids.stream()
                .filter(id -> !id.equals(targetId))
                .collect(Collectors.toList());
        return String.join(",", newIds);
    }

    @PostMapping("/top")
//    @ApiOperation("置顶课程")
//    public Result updateTop(@ApiParam("请求参数") @RequestBody Map<String, Object> map) {
    public Result updateTop( @RequestBody Map<String, Object> map) {
        Result result = new Result();
        try {
            String accountId = map.get("accountId").toString();
            String id = map.get("id").toString();
            String topCourses = accountService.findTop(accountId);
            String newTopCourses = appendId(topCourses, id); // 使用工具方法
            if (accountService.updateTop(accountId, newTopCourses)) {
                result.setSuccess(true);
                result.setMessage("顶置成功");
                List<Course> top;
                top = updateTopCourses(topCourses);
                result.setTop(top);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("顶置失败");
        }
        return result;
    }

    @PostMapping("/cancel-top")
//    @ApiOperation("取消置顶课程")
//    public Result cancelTop(@ApiParam("请求参数") @RequestBody Map<String, Object> dataMap) {
    public Result cancelTop(@RequestBody Map<String, Object> dataMap) {
        Result result = new Result();
        try {
            String accountId = dataMap.get("accountId").toString();
            String courseId = dataMap.get("id").toString();

            Account account = accountService.findByAccountId(accountId);
            String topCourses = account.getTop();
            // 处理字符串，移除目标id（注意处理空值和边界情况）
            String newTopCourses = removeCourseId(topCourses, courseId);
            account.setTop(newTopCourses);
            accountService.updateTop(accountId, newTopCourses); // 更新数据库

            // 2. 返回更新后的置顶课程列表
            List<Course> top = updateTopCourses(topCourses);
            result.setTop(top);
            result.setSuccess(true);
            result.setMessage("取消置顶成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("取消置顶失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/login")
//    @ApiOperation("用户登录")
//    public Result login(@ApiParam("登录信息") @RequestBody Account account) {
    public Result login( @RequestBody Account account) {
        Result result = new Result();
        try {
            String phone = account.getPhone();
            String password = account.getPassword();
            List<Course> learned;
            List<Course> taught;
            List<Course> top;
            if (accountService.authenticate(phone, password)) {
                Account returnAccount = accountService.findByPhone(phone);
                String learnedCourses = returnAccount.getLearned();
                String taughtCourses = returnAccount.getTaught();
                String topCourses = returnAccount.getTop();
                learned = updateLearnedCourses(learnedCourses);
                taught = updateTaughtCourses(taughtCourses);
                top = updateTopCourses(topCourses);
                result.setLearned(learned);
                result.setTaught(taught);
                result.setTop(top);
                result.setSuccess(true);
                result.setMessage("登陆成功");
                result.setAccount(returnAccount);
            } else {
                result.setSuccess(false);
                result.setMessage("登录失败：用户名或密码错误");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("登录失败:" + e.getMessage());
        }
        return result;
    }

    //注册接口
    @PostMapping("/register")
//    @ApiOperation("用户注册")
//    public Result register(@ApiParam("注册信息") @RequestBody Account account) {
    public Result register( @RequestBody Account account) {
        Result result = new Result();
        try {
            if (account.getPhone() == null || account.getPassword() == null || account.getName() == null || account.getSchool() == null) {
                result.setSuccess(false);
                result.setMessage("手机号，密码，姓名，学校不能为空");
                return result;
            }
            //检查账号是否已经存在
            if (accountService.findByPhone(account.getPhone()) != null) {
                result.setSuccess(false);
                result.setMessage("手机号已存在");
                return result;
            }
            //设置默认角色老师
            if (account.getIdentity() == null) {
                account.setIdentity("老师");
            }
            // 根据身份选择性填充数据
            if ("学生".equals(account.getIdentity())) {
                if (account.getStudentId() == null) {
                    result.setSuccess(false);
                    result.setMessage("学生身份注册时学号不能为空");
                    return result;
                }
            }
            // 自动生成account（若为空）
            if (account.getAccountId() == null) {
                String randomAccount = generateRandomAccount();
                // 确保生成的account唯一（防止极端情况下重复）
                while (accountService.findByAccountId(randomAccount) != null) {
                    randomAccount = generateRandomAccount();
                }
                account.setAccountId(randomAccount);
            }
            //保存用户
            accountService.saveAccount(account);
            result.setSuccess(true);
            result.setMessage("注册成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("注册失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/change-identity")
//    @ApiOperation("修改用户身份")
//    public Result changeIdentity(@ApiParam("用户信息") @RequestBody Account account) {
    public Result changeIdentity(@RequestBody Account account) {
        Result result = new Result();
        try {
            if (accountService.updateIdentity(account)) {
                Account accountResult = accountService.findByAccountId(account.getAccountId());
                result.setAccount(accountResult);
                result.setSuccess(true);
                result.setMessage("角色变更成功");
            } else {
                result.setSuccess(false);
                result.setMessage("角色变更失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("角色变更失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/change-phone")
//    @ApiOperation("修改手机号")
//    public Result changePhone(@ApiParam("用户信息") @RequestBody Account account) {
    public Result changePhone( @RequestBody Account account) {
        Result result = new Result();
        if (accountService.updatePhone(account)) {
            result.setSuccess(true);
            result.setMessage("手机号更换成功");
        } else {
            result.setSuccess(false);
            result.setMessage("手机号更换失败");
        }
        return result;
    }

    @PostMapping("/change-password")
//    @ApiOperation("修改密码")
//    public Result changePassword(@ApiParam("用户信息") @RequestBody Account account) {
    public Result changePassword(@RequestBody Account account) {
        Result result = new Result();
        try {
            if (accountService.updatePassword(account)) {
                result.setSuccess(true);
                result.setMessage("密码修改成功");
            } else {
                result.setSuccess(false);
                result.setMessage("密码修改失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("密码修改失败" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/change-basic-information")
//    @ApiOperation("修改基本信息")
//    public Result changeBasicInformation(@ApiParam("用户信息") @RequestBody Account account) {
    public Result changeBasicInformation( @RequestBody Account account) {
        Result result = new Result();
        try {
            if (accountService.updateBasicInformation(account)) {
                result.setSuccess(true);
                result.setMessage("基础信息变更成功");
            } else {
                result.setSuccess(false);
                result.setMessage("基础信息变更失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("基础信息变更失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/create-course")
//    @ApiOperation("创建课程")
//    public Result createCourse(@ApiParam("课程信息") @RequestBody CourseRequest request) {
    public Result createCourse( @RequestBody CourseRequest request) {
        Result result = new Result();
        try {
            String accountId = request.getAccountId();
            Course course = request.getCourse();
            //生成一个不重复的课程码，将课程插入到课程表中，更新我教的
            String randomCourseCode = generateRandomCourseCode();
            //确保生成的课程码唯一
            while (accountService.findByCourseId(randomCourseCode) != null) {
                randomCourseCode = generateRandomCourseCode();
            }
            course.setId(randomCourseCode);
            String stringTaught = accountService.findTaught(accountId);
            String newStringTaught = appendId(stringTaught, randomCourseCode); // 使用工具方法
            List<Course> taught = updateTaughtCourses(newStringTaught);
            if (accountService.insertCourse(course)) {
                accountService.updateTaught(accountId, newStringTaught);
                result.setTaught(taught);
                result.setSuccess(true);
                result.setMessage("成功创建课程");
            } else {
                result.setSuccess(false);
                result.setMessage("创建课程失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("创建课程失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/join-course")
//    @ApiOperation("加入课程")
//    public Result joinCourse(@ApiParam("加入课程信息") @RequestBody Map<String, Object> dataMap) {
    public Result joinCourse( @RequestBody Map<String, Object> dataMap) {
        Result result = new Result();
        try {
            String accountId = dataMap.get("accountId").toString();
            String id = dataMap.get("id").toString();

            //查询当前账户的我学的，处理null情况
            String stringLearned = accountService.findLearned(accountId);
            // 查询当前账户的我教的，处理null情况
            String stringTaught = accountService.findTaught(accountId);

            // 处理null值，转为空字符串
            stringLearned = stringLearned == null ? "" : stringLearned;
            stringTaught = stringTaught == null ? "" : stringTaught;

            // 检查课程是否已经在我学的课程列表中
            if (stringLearned.contains(id)) {
                result.setSuccess(false);
                result.setMessage("该课程已经在您学习的课程列表中，加入失败");
                return result;
            }
            if (stringTaught.contains(id)) {
                result.setSuccess(false);
                result.setMessage("该课程已经在您教的课程列表中，加入失败");
                return result;
            }
            //把id插入到我学的里面
            String newStringLearned = appendId(stringLearned, id);
            //在课程的学生添加新的账号
            String students=accountService.findStudents(id);
           //处理null值
            String existingStudents = (students == null) ? "" : students;
            List<String> studentList = new ArrayList<>();
            if (!existingStudents.isEmpty()) {
                studentList.addAll(Arrays.asList(existingStudents.split(",")));
            }
            if (!studentList.contains(accountId)) {
                studentList.add(accountId);
            }
            String updatedStudents = String.join(",", studentList);
           if(!accountService.updateStudents(updatedStudents,id)){
               result.setSuccess(false);
               result.setMessage("加入课程失败" );
               return result;
           }
            if (accountService.findByCourseId(id) == null) {
                result.setSuccess(false);
                result.setMessage("不存在此课程");
            } else {
                accountService.updateLearned(accountId, newStringLearned);
                //查询课程详细信息，并返回给前端进行渲染
                List<Course> learned = updateLearnedCourses(newStringLearned);
                result.setLearned(learned);
                result.setSuccess(true);
                result.setMessage("成功加入课程");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("加入课程失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/assignment-details")
//    @ApiOperation("获取作业详情")
//    public Result assignmentDetails(@ApiParam("请求参数") @RequestBody CourseAndAccount request) {
    public Result assignmentDetails(@RequestBody CourseAndAccount request) {
     String accountId = request.getAccountId();
     String id = request.getId();
     return accountService.findAssignment(accountId,id);
    }
    @PostMapping("/assignment-submit")
//    @ApiOperation("提交作业")
//    public Result assignmentSubmit(@ApiParam("提交作业信息") @RequestBody CourseAndAccount request) {
    public Result assignmentSubmit( @RequestBody CourseAndAccount request) {
        String accountId=request.getAccountId();
        String id=request.getId();
        String assignmentId=request.getAssignmentId();
        String submitContent=request.getSubmitContent();
        return accountService.updateAssignment(accountId,id,assignmentId,submitContent);
    }
    @PostMapping("/check-assignment-submit")
//    @ApiOperation("查看已提交作业")
//    public Result checkAssignmentSubmit(@ApiParam("请求参数") @RequestBody CourseAndAccount request) {
    public Result checkAssignmentSubmit( @RequestBody CourseAndAccount request) {
        String id=request.getId();
        String assignmentId=request.getAssignmentId();
        String accountId=request.getAccountId();
        return accountService.findSubmitAssignment(accountId,id,assignmentId);
    }
    @PostMapping("/correct-assignment")
//    @ApiOperation("批改作业")
//    public Result correctAssignment(@ApiParam("批改信息") @RequestBody CourseAndAccount request) {
    public Result correctAssignment( @RequestBody CourseAndAccount request) {
     String accountId=request.getAccountId();
     String id=request.getId();
     String assignmentId=request.getAssignmentId();
     Integer score=request.getScore();
     return accountService.updateScore(score,accountId,id,assignmentId);
    }
    @PostMapping("/release-assignment")
//    @ApiOperation("发布作业")
//    public Result releaseAssignment(@ApiParam("作业信息") @RequestBody CourseAndAccount request) {
    public Result releaseAssignment(@RequestBody CourseAndAccount request) {
        String id=request.getId();
        String accountIdNull=request.getAccountId();
        Assignment assignment=request.getAssignment();
        return accountService.insertAssignments(accountIdNull,id,assignment);
    }
    // 处理字符串拼接，避免空字符串和多余逗号
    private String appendId(String original, String newId) {
        if (original == null || original.trim().isEmpty()) {
            return newId;
        }
        if (original.contains(newId)) { // 避免重复添加
            return original;
        }
        return original + "," + newId;
    }
}

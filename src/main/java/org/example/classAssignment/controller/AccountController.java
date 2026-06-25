package org.example.classAssignment.controller;

import org.example.classAssignment.pojo.*;
import org.example.classAssignment.service.AccountService;
import org.example.classAssignment.mapper.AccountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


@RestController
//@Api(tags = "课堂派接口")
public class AccountController {
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private AccountMapper accountMapper;

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
            if (!id.trim().isEmpty()) {
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
    public Result updateTop(@RequestBody Map<String, Object> map) {
        Result result = new Result();
        try {
            String accountId = map.get("accountId").toString();
            String id = map.get("id").toString();
            String topCourses = accountService.findTop(accountId);
            String newTopCourses = appendId(topCourses, id);
            if (accountService.updateTop(accountId, newTopCourses)) {
                result.setSuccess(true);
                result.setMessage("顶置成功");
                List<Course> top;
                top = updateTopCourses(newTopCourses);
                result.setTop(top);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("顶置失败");
        }
        return result;
    }

    @PostMapping("/cancel-top")
    public Result cancelTop(@RequestBody Map<String, Object> dataMap) {
        Result result = new Result();
        try {
            String accountId = dataMap.get("accountId").toString();
            String courseId = dataMap.get("id").toString();

            Account account = accountService.findByAccountId(accountId);
            String topCourses = account.getTop();
            String newTopCourses = removeCourseId(topCourses, courseId);
            account.setTop(newTopCourses);
            accountService.updateTop(accountId, newTopCourses);

            // 2. 返回更新后的置顶课程列表
            List<Course> top = updateTopCourses(newTopCourses);
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
    public Result login(@RequestBody Account account) {
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

    @PostMapping("/register")
    public Result register(@RequestBody Account account) {
        Result result = new Result();
        try {
            if (account.getPhone() == null || account.getPassword() == null || account.getName() == null || account.getSchool() == null) {
                result.setSuccess(false);
                result.setMessage("手机号，密码，姓名，学校不能为空");
                return result;
            }
            if (accountService.findByPhone(account.getPhone()) != null) {
                result.setSuccess(false);
                result.setMessage("手机号已存在");
                return result;
            }
            if (account.getIdentity() == null) {
                account.setIdentity("老师");
            }
            if ("学生".equals(account.getIdentity())) {
                if (account.getStudentId() == null) {
                    result.setSuccess(false);
                    result.setMessage("学生身份注册时学号不能为空");
                    return result;
                }
            }
            if (account.getAccountId() == null) {
                String randomAccount = generateRandomAccount();
                while (accountService.findByAccountId(randomAccount) != null) {
                    randomAccount = generateRandomAccount();
                }
                account.setAccountId(randomAccount);
            }
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
    public Result changePhone(@RequestBody Account account) {
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
    public Result changeBasicInformation(@RequestBody Account account) {
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
    public Result createCourse(@RequestBody CourseRequest request) {
        Result result = new Result();
        try {
            String accountId = request.getAccountId();
            Course course = request.getCourse();
            String randomCourseCode = generateRandomCourseCode();
            while (accountService.findByCourseId(randomCourseCode) != null) {
                randomCourseCode = generateRandomCourseCode();
            }
            course.setId(randomCourseCode);
            String stringTaught = accountService.findTaught(accountId);
            String newStringTaught = appendId(stringTaught, randomCourseCode);
            if (accountService.insertCourse(course)) {
                accountService.updateTaught(accountId, newStringTaught);
                List<Course> taught = updateTaughtCourses(newStringTaught);
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
    public Result joinCourse(@RequestBody Map<String, Object> dataMap) {
        Result result = new Result();
        try {
            String accountId = dataMap.get("accountId").toString();
            String id = dataMap.get("id").toString();

            String stringLearned = accountService.findLearned(accountId);
            String stringTaught = accountService.findTaught(accountId);

            stringLearned = stringLearned == null ? "" : stringLearned;
            stringTaught = stringTaught == null ? "" : stringTaught;

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
            String newStringLearned = appendId(stringLearned, id);
            String students=accountService.findStudents(id);
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
                accountService.syncCourseAssignmentsForStudent(accountId, id);
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
    public Result assignmentDetails(@RequestBody CourseAndAccount request) {
        String accountId = request.getAccountId();
        String id = request.getId();
        return accountService.findAssignment(accountId,id);
    }

    @PostMapping("/assignment-submit")
//    @ApiOperation("提交作业")
//    public Result assignmentSubmit(@ApiParam("提交作业信息") @RequestBody CourseAndAccount request) {
    public Result assignmentSubmit(
            @RequestParam("accountId") String accountId,
            @RequestParam("id") String id,
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam(value = "submitContent", required = false) String submitContent,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return accountService.updateAssignment(accountId, id, assignmentId, submitContent, file);
    }

    @DeleteMapping("/assignment-submission-file")
    public Result deleteAssignmentSubmissionFile(
            @RequestParam("accountId") String accountId,
            @RequestParam("id") String id,
            @RequestParam("assignmentId") String assignmentId
    ) {
        return accountService.deleteAssignmentSubmissionFile(accountId, id, assignmentId);
    }

    @PostMapping("/check-assignment-submit")
    public Result checkAssignmentSubmit(@RequestBody CourseAndAccount request) {
        String id=request.getId();
        String assignmentId=request.getAssignmentId();
        String accountId=request.getAccountId();
        return accountService.findSubmitAssignment(accountId,id,assignmentId);
    }

    @PostMapping("/correct-assignment")
    public Result correctAssignment(@RequestBody CourseAndAccount request) {
        String accountId=request.getAccountId();
        String id=request.getId();
        String assignmentId=request.getAssignmentId();
        Integer score=request.getScore();
        return accountService.updateScore(score,accountId,id,assignmentId);
    }

    @PostMapping("/release-assignment")
    public Result releaseAssignment(@RequestBody CourseAndAccount request) {
        String id=request.getId();
        String accountIdNull=request.getAccountId();
        Assignment assignment=request.getAssignment();
        return accountService.insertAssignments(accountIdNull,id,assignment);
    }

    @PostMapping("/update-course-assignment")
    public Result updateCourseAssignment(@RequestBody CourseAndAccount request) {
        return accountService.updateCourseAssignment(request.getId(), request.getAssignmentId(), request.getAssignment());
    }

    @PostMapping("/delete-course-assignment")
    public Result deleteCourseAssignment(@RequestBody CourseAndAccount request) {
        return accountService.deleteCourseAssignment(request.getId(), request.getAssignmentId());
    }

    @PostMapping("/toggle-assignment-ai")
    public Result toggleAssignmentAi(@RequestBody CourseAndAccount request) {
        Boolean aiEnabled = request.getAssignment() == null ? null : request.getAssignment().getAiEnabled();
        return accountService.toggleAssignmentAi(request.getId(), request.getAssignmentId(), aiEnabled);
    }

    @GetMapping("/assignment-file")
    public ResponseEntity<Resource> downloadAssignmentFile(
            @RequestParam("accountId") String accountId,
            @RequestParam("id") String id,
            @RequestParam("assignmentId") String assignmentId
    ) throws IOException {
        Assignment fileMeta = accountService.getAssignmentFileMeta(accountId, id, assignmentId);
        Resource resource = accountService.loadAssignmentFile(accountId, id, assignmentId);
        if (resource == null || fileMeta == null || fileMeta.getFileName() == null) {
            return ResponseEntity.notFound().build();
        }
        String encodedFileName = URLEncoder.encode(fileMeta.getFileName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // 处理字符串拼接，避免空字符串和多余逗号
    private String appendId(String original, String newId) {
        if (original == null || original.trim().isEmpty()) {
            return newId;
        }
        if (original.contains(newId)) {
            return original;
        }
        return original + "," + newId;
    }

    // 学生归档课程（简单版本，直接归档）
    @PostMapping("/archive-course")
    public Result archiveCourse(@RequestBody Map<String, Object> dataMap) {
        Result result = new Result();
        try {
            String accountId = dataMap.get("accountId").toString();
            String courseId = dataMap.get("courseId").toString();
            String archiveType = dataMap.get("archiveType").toString();
            
            String archivedAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            
            accountMapper.updateCourseArchiveStatus(courseId, accountId, archivedAt);
            
            if ("student".equals(archiveType)) {
                String archivedLearned = accountMapper.findArchivedLearned(accountId);
                archivedLearned = archivedLearned == null ? "" : archivedLearned;
                String newArchivedLearned = appendId(archivedLearned, courseId);
                accountMapper.updateArchivedLearned(accountId, newArchivedLearned);
                
                String learned = accountMapper.findLearned(accountId);
                learned = learned == null ? "" : learned;
                String newLearned = removeCourseId(learned, courseId);
                accountMapper.updateLearned(accountId, newLearned);
                
                String top = accountMapper.findTop(accountId);
                top = top == null ? "" : top;
                String newTop = removeCourseId(top, courseId);
                accountMapper.updateTop(accountId, newTop);
            } else if ("teacher_self".equals(archiveType)) {
                String archivedTaught = accountMapper.findArchivedTaught(accountId);
                archivedTaught = archivedTaught == null ? "" : archivedTaught;
                String newArchivedTaught = appendId(archivedTaught, courseId);
                accountMapper.updateArchivedTaught(accountId, newArchivedTaught);
                
                String taught = accountMapper.findTaught(accountId);
                taught = taught == null ? "" : taught;
                String newTaught = removeCourseId(taught, courseId);
                accountMapper.updateTaught(accountId, newTaught);
            } else if ("teacher_student".equals(archiveType)) {
                String students = accountMapper.findStudents(courseId);
                if (students != null && !students.isEmpty()) {
                    String[] studentIds = students.split(",");
                    for (String studentId : studentIds) {
                        if (!studentId.trim().isEmpty()) {
                            String studentArchivedLearned = accountMapper.findArchivedLearned(studentId);
                            studentArchivedLearned = studentArchivedLearned == null ? "" : studentArchivedLearned;
                            String newStudentArchivedLearned = appendId(studentArchivedLearned, courseId);
                            accountMapper.updateArchivedLearned(studentId, newStudentArchivedLearned);
                            
                            String studentLearned = accountMapper.findLearned(studentId);
                            studentLearned = studentLearned == null ? "" : studentLearned;
                            String newStudentLearned = removeCourseId(studentLearned, courseId);
                            accountMapper.updateLearned(studentId, newStudentLearned);
                            
                            String studentTop = accountMapper.findTop(studentId);
                            studentTop = studentTop == null ? "" : studentTop;
                            String newStudentTop = removeCourseId(studentTop, courseId);
                            accountMapper.updateTop(studentId, newStudentTop);
                        }
                    }
                }
            }
            
            result.setSuccess(true);
            result.setMessage("归档成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("归档失败：" + e.getMessage());
        }
        return result;
    }

    // 批量归档教师的所有课程
    @PostMapping("/archive-all-courses")
    public Result archiveAllCourses(@RequestBody Map<String, Object> dataMap) {
        Result result = new Result();
        try {
            String accountId = dataMap.get("accountId").toString();
            String archiveType = dataMap.get("archiveType").toString();
            
            String archivedAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            
            if ("teacher".equals(archiveType)) {
                String taught = accountMapper.findTaught(accountId);
                if (taught != null && !taught.isEmpty()) {
                    String[] courseIds = taught.split(",");
                    for (String courseId : courseIds) {
                        if (!courseId.trim().isEmpty()) {
                            accountMapper.updateCourseArchiveStatus(courseId, accountId, archivedAt);
                            
                            String archivedTaught = accountMapper.findArchivedTaught(accountId);
                            archivedTaught = archivedTaught == null ? "" : archivedTaught;
                            String newArchivedTaught = appendId(archivedTaught, courseId);
                            accountMapper.updateArchivedTaught(accountId, newArchivedTaught);
                        }
                    }
                    accountMapper.updateTaught(accountId, "");
                }
            } else if ("student".equals(archiveType)) {
                String learned = accountMapper.findLearned(accountId);
                if (learned != null && !learned.isEmpty()) {
                    String[] courseIds = learned.split(",");
                    for (String courseId : courseIds) {
                        if (!courseId.trim().isEmpty()) {
                            accountMapper.updateCourseArchiveStatus(courseId, accountId, archivedAt);
                            
                            String archivedLearned = accountMapper.findArchivedLearned(accountId);
                            archivedLearned = archivedLearned == null ? "" : archivedLearned;
                            String newArchivedLearned = appendId(archivedLearned, courseId);
                            accountMapper.updateArchivedLearned(accountId, newArchivedLearned);
                        }
                    }
                    accountMapper.updateLearned(accountId, "");
                    accountMapper.updateTop(accountId, "");
                }
            }
            
            result.setSuccess(true);
            result.setMessage("批量归档成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("批量归档失败：" + e.getMessage());
        }
        return result;
    }

    // 取消归档（恢复课程）
    @PostMapping("/unarchive-course")
    public Result unarchiveCourse(@RequestBody Map<String, Object> dataMap) {
        Result result = new Result();
        try {
            String accountId = dataMap.get("accountId").toString();
            String courseId = dataMap.get("courseId").toString();
            String restoreType = dataMap.get("restoreType").toString();
            
            accountMapper.updateCourseArchiveStatus(courseId, null, null);
            
            if ("learned".equals(restoreType)) {
                String archivedLearned = accountMapper.findArchivedLearned(accountId);
                archivedLearned = archivedLearned == null ? "" : archivedLearned;
                String newArchivedLearned = removeCourseId(archivedLearned, courseId);
                accountMapper.updateArchivedLearned(accountId, newArchivedLearned);
                
                String learned = accountMapper.findLearned(accountId);
                learned = learned == null ? "" : learned;
                String newLearned = appendId(learned, courseId);
                accountMapper.updateLearned(accountId, newLearned);
            } else if ("taught".equals(restoreType)) {
                String archivedTaught = accountMapper.findArchivedTaught(accountId);
                archivedTaught = archivedTaught == null ? "" : archivedTaught;
                String newArchivedTaught = removeCourseId(archivedTaught, courseId);
                accountMapper.updateArchivedTaught(accountId, newArchivedTaught);
                
                String taught = accountMapper.findTaught(accountId);
                taught = taught == null ? "" : taught;
                String newTaught = appendId(taught, courseId);
                accountMapper.updateTaught(accountId, newTaught);
            }
            
            result.setSuccess(true);
            result.setMessage("取消归档成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("取消归档失败：" + e.getMessage());
        }
        return result;
    }

    // 获取归档的课程列表
    @PostMapping("/get-archived-courses")
    public Result getArchivedCourses(@RequestBody Map<String, String> dataMap) {
        Result result = new Result();
        try {
            String accountId = dataMap.get("accountId").toString();
            String courseType = dataMap.get("courseType").toString();
            
            List<Course> archivedCourses = new ArrayList<>();
            
            if ("learned".equals(courseType)) {
                String archivedLearned = accountMapper.findArchivedLearned(accountId);
                if (archivedLearned != null && !archivedLearned.isEmpty()) {
                    archivedCourses = getCourses(archivedLearned);
                }
            } else if ("taught".equals(courseType)) {
                String archivedTaught = accountMapper.findArchivedTaught(accountId);
                if (archivedTaught != null && !archivedTaught.isEmpty()) {
                    archivedCourses = getCourses(archivedTaught);
                }
            }
            
            result.setSuccess(true);
            result.setMessage("获取成功");
            if ("taught".equals(courseType)) {
                result.setTaught(archivedCourses);
            } else {
                result.setLearned(archivedCourses);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败：" + e.getMessage());
        }
        return result;
    }
}

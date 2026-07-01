package org.example.classAssignment.service;

import org.example.classAssignment.mapper.AccountMapper;
import org.example.classAssignment.pojo.Account;
import org.example.classAssignment.pojo.Assignment;
import org.example.classAssignment.pojo.Course;
import org.example.classAssignment.pojo.CourseNotification;
import org.example.classAssignment.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AccountService implements AccountServiceInterface {
    private static final String FILE_META_NAME = "meta.properties";
    private final Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads", "assignments");
    private final Path assignmentResourceRoot = Paths.get(System.getProperty("user.dir"), "uploads", "assignment-resources");
    private final Path avatarRoot = Paths.get(System.getProperty("user.dir"), "uploads", "avatars");

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AiEvaluationService aiEvaluationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureFeatureSchema() {
        jdbcTemplate.execute("create table if not exists course_notification (" +
                "id bigint primary key auto_increment, " +
                "account_id varchar(64) not null, " +
                "course_id varchar(64), " +
                "assignment_id varchar(64), " +
                "type varchar(32) not null, " +
                "title varchar(255) not null, " +
                "content text, " +
                "sender_name varchar(128), " +
                "read_status tinyint(1) default 0, " +
                "created_at datetime default current_timestamp" +
                ")");
        jdbcTemplate.execute("create table if not exists assignment_resource (" +
                "course_id varchar(64) not null, " +
                "assignment_id varchar(64) not null, " +
                "file_name varchar(255), " +
                "file_stored_name varchar(255), " +
                "file_size bigint, " +
                "file_content_type varchar(255), " +
                "created_at datetime default current_timestamp, " +
                "primary key(course_id, assignment_id)" +
                ")");
        ensureColumnExists("course", "account_id", "varchar(64) null");
        ensureColumnExists("account", "avatar_stored_name", "varchar(255) null");
        ensureColumnExists("asssignment", "teacher_comment", "text null");
        ensureColumnExists("asssignment", "publish_time", "varchar(64) null");
        normalizeExistingAssignmentPublishTime();
        migrateCourseTeacherToAccountId();
        dropColumnIfExists("course", "teacher");
        normalizeCourseRelationshipData();
        dropColumnIfExists("course", "number");
    }

    private void ensureColumnExists(String tableName, String columnName, String columnDefinition) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns " +
                        "where table_schema = database() and table_name = ? and column_name = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute(String.format(
                "alter table %s add column %s %s",
                tableName,
                columnName,
                columnDefinition
        ));
    }

    private void dropColumnIfExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns " +
                        "where table_schema = database() and table_name = ? and column_name = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count <= 0) {
            return;
        }
        jdbcTemplate.execute(String.format("alter table %s drop column %s", tableName, columnName));
    }

    private void migrateCourseTeacherToAccountId() {
        Integer teacherColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns " +
                        "where table_schema = database() and table_name = 'course' and column_name = 'teacher'",
                Integer.class
        );
        if (teacherColumnCount == null || teacherColumnCount <= 0) {
            return;
        }
        jdbcTemplate.execute(
                "update course c " +
                        "left join account a on a.name = c.teacher and a.identity = '老师' " +
                        "set c.account_id = coalesce(c.account_id, a.account_id) " +
                        "where (c.account_id is null or c.account_id = '') and c.teacher is not null and c.teacher <> ''"
        );
    }

    private void normalizeExistingAssignmentPublishTime() {
        jdbcTemplate.execute(
                "update asssignment " +
                        "set publish_time = case " +
                        "when deadline is not null and deadline <> '' then deadline " +
                        "else date_format(now(), '%Y-%m-%d %H:%i') end " +
                        "where (publish_time is null or publish_time = '') " +
                        "and exists (" +
                        "select 1 from course c " +
                        "where c.id = asssignment.id " +
                        "and (" +
                        "asssignment.account_id <> c.account_id " +
                        "or (asssignment.submit is not null and asssignment.submit <> '') " +
                        "or (asssignment.correct is not null and asssignment.correct <> '')" +
                        ")" +
                        ")"
        );
    }

    private void normalizeCourseRelationshipData() {
        List<Map<String, Object>> accountRows = jdbcTemplate.queryForList(
                "select account_id, identity, learned, taught, top, archived_learned, archived_taught from account"
        );
        List<Map<String, Object>> courseRows = jdbcTemplate.queryForList(
                "select id, account_id, students from course"
        );

        Set<String> validCourseIds = new LinkedHashSet<>();
        Set<String> validStudentIds = new LinkedHashSet<>();
        Map<String, String> courseOwnerMap = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> courseStudentsMap = new LinkedHashMap<>();

        for (Map<String, Object> courseRow : courseRows) {
            String courseId = toSafeString(courseRow.get("id"));
            if (courseId == null) {
                continue;
            }
            validCourseIds.add(courseId);
            courseOwnerMap.put(courseId, toSafeString(courseRow.get("account_id")));
        }

        for (Map<String, Object> accountRow : accountRows) {
            String accountId = toSafeString(accountRow.get("account_id"));
            String identity = toSafeString(accountRow.get("identity"));
            if (accountId != null && "学生".equals(identity)) {
                validStudentIds.add(accountId);
            }
        }

        for (Map<String, Object> courseRow : courseRows) {
            String courseId = toSafeString(courseRow.get("id"));
            if (courseId == null) {
                continue;
            }
            LinkedHashSet<String> normalizedStudents = normalizeIdSet(courseRow.get("students"), validStudentIds);
            courseStudentsMap.put(courseId, normalizedStudents);
        }

        for (Map<String, Object> accountRow : accountRows) {
            String accountId = toSafeString(accountRow.get("account_id"));
            String identity = toSafeString(accountRow.get("identity"));
            if (accountId == null) {
                continue;
            }

            if ("学生".equals(identity)) {
                LinkedHashSet<String> learnedSet = normalizeIdSet(accountRow.get("learned"), validCourseIds);
                LinkedHashSet<String> archivedLearnedSet = normalizeIdSet(accountRow.get("archived_learned"), validCourseIds);

                // 已归档课程不应再出现在激活课程列表中
                learnedSet.removeAll(archivedLearnedSet);

                for (Map.Entry<String, LinkedHashSet<String>> entry : courseStudentsMap.entrySet()) {
                    if (!entry.getValue().contains(accountId)) {
                        continue;
                    }
                    String courseId = entry.getKey();
                    if (!learnedSet.contains(courseId) && !archivedLearnedSet.contains(courseId)) {
                        learnedSet.add(courseId);
                    }
                }

                for (String courseId : learnedSet) {
                    courseStudentsMap.computeIfAbsent(courseId, key -> new LinkedHashSet<>()).add(accountId);
                }
                for (String courseId : archivedLearnedSet) {
                    courseStudentsMap.computeIfAbsent(courseId, key -> new LinkedHashSet<>()).add(accountId);
                }

                LinkedHashSet<String> topSet = normalizeIdSet(accountRow.get("top"), validCourseIds);
                topSet.retainAll(learnedSet);

                updateAccountCourseColumnsIfChanged(
                        accountId,
                        accountRow.get("learned"),
                        joinIds(learnedSet),
                        accountRow.get("archived_learned"),
                        joinIds(archivedLearnedSet),
                        accountRow.get("taught"),
                        null,
                        accountRow.get("archived_taught"),
                        null,
                        accountRow.get("top"),
                        joinIds(topSet)
                );
                continue;
            }

            if ("老师".equals(identity)) {
                LinkedHashSet<String> taughtSet = normalizeIdSet(accountRow.get("taught"), validCourseIds);
                LinkedHashSet<String> archivedTaughtSet = normalizeIdSet(accountRow.get("archived_taught"), validCourseIds);
                taughtSet.removeAll(archivedTaughtSet);

                for (Map.Entry<String, String> entry : courseOwnerMap.entrySet()) {
                    if (!Objects.equals(accountId, entry.getValue())) {
                        continue;
                    }
                    String courseId = entry.getKey();
                    if (!taughtSet.contains(courseId) && !archivedTaughtSet.contains(courseId)) {
                        taughtSet.add(courseId);
                    }
                }

                LinkedHashSet<String> topSet = normalizeIdSet(accountRow.get("top"), validCourseIds);
                topSet.retainAll(taughtSet);

                updateAccountCourseColumnsIfChanged(
                        accountId,
                        accountRow.get("learned"),
                        null,
                        accountRow.get("archived_learned"),
                        null,
                        accountRow.get("taught"),
                        joinIds(taughtSet),
                        accountRow.get("archived_taught"),
                        joinIds(archivedTaughtSet),
                        accountRow.get("top"),
                        joinIds(topSet)
                );
            }
        }

        for (Map<String, Object> courseRow : courseRows) {
            String courseId = toSafeString(courseRow.get("id"));
            if (courseId == null) {
                continue;
            }
            LinkedHashSet<String> studentsSet = courseStudentsMap.getOrDefault(courseId, new LinkedHashSet<>());
            String nextStudents = joinIds(studentsSet);
            String currentStudents = emptyToNull(toSafeString(courseRow.get("students")));
            if (!Objects.equals(currentStudents, nextStudents)) {
                jdbcTemplate.update(
                        "update course set students=? where id=?",
                        nextStudents,
                        courseId
                );
            }
        }
    }

    private void updateAccountCourseColumnsIfChanged(
            String accountId,
            Object currentLearned,
            String nextLearned,
            Object currentArchivedLearned,
            String nextArchivedLearned,
            Object currentTaught,
            String nextTaught,
            Object currentArchivedTaught,
            String nextArchivedTaught,
            Object currentTop,
            String nextTop
    ) {
        String normalizedCurrentLearned = emptyToNull(toSafeString(currentLearned));
        String normalizedCurrentArchivedLearned = emptyToNull(toSafeString(currentArchivedLearned));
        String normalizedCurrentTaught = emptyToNull(toSafeString(currentTaught));
        String normalizedCurrentArchivedTaught = emptyToNull(toSafeString(currentArchivedTaught));
        String normalizedCurrentTop = emptyToNull(toSafeString(currentTop));

        String finalLearned = nextLearned == null ? normalizedCurrentLearned : nextLearned;
        String finalArchivedLearned = nextArchivedLearned == null ? normalizedCurrentArchivedLearned : nextArchivedLearned;
        String finalTaught = nextTaught == null ? normalizedCurrentTaught : nextTaught;
        String finalArchivedTaught = nextArchivedTaught == null ? normalizedCurrentArchivedTaught : nextArchivedTaught;
        String finalTop = nextTop == null ? normalizedCurrentTop : nextTop;

        if (Objects.equals(normalizedCurrentLearned, finalLearned)
                && Objects.equals(normalizedCurrentArchivedLearned, finalArchivedLearned)
                && Objects.equals(normalizedCurrentTaught, finalTaught)
                && Objects.equals(normalizedCurrentArchivedTaught, finalArchivedTaught)
                && Objects.equals(normalizedCurrentTop, finalTop)) {
            return;
        }

        jdbcTemplate.update(
                "update account set learned=?, archived_learned=?, taught=?, archived_taught=?, top=? where account_id=?",
                finalLearned,
                finalArchivedLearned,
                finalTaught,
                finalArchivedTaught,
                finalTop,
                accountId
        );
    }

    private LinkedHashSet<String> normalizeIdSet(Object rawValue, Set<String> allowedIds) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String value = toSafeString(rawValue);
        if (value == null) {
            return result;
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            String normalized = emptyToNull(part == null ? null : part.trim());
            if (normalized == null) {
                continue;
            }
            if (allowedIds != null && !allowedIds.contains(normalized)) {
                continue;
            }
            result.add(normalized);
        }
        return result;
    }

    private String joinIds(LinkedHashSet<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return String.join(",", ids);
    }

    private String toSafeString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public Account findByAccountId(String accountId) {
        Account account = accountMapper.findByAccountId(accountId);
        enrichAccountAvatar(account);
        return account;
    }

    @Override
    public Account findByPhone(String phone) {
        Account account = accountMapper.findByPhone(phone);
        enrichAccountAvatar(account);
        return account;
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
    public Result updateAvatar(String accountId, MultipartFile file) {
        Result result = new Result();
        try {
            if (accountId == null || accountId.isBlank()) {
                result.setSuccess(false);
                result.setMessage("账号不能为空");
                return result;
            }
            if (file == null || file.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("请选择头像图片");
                return result;
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                result.setSuccess(false);
                result.setMessage("头像仅支持图片格式");
                return result;
            }
            String storedName = storeAccountAvatar(accountId, file);
            accountMapper.updateAvatarStoredName(accountId, storedName);
            result.setSuccess(true);
            result.setMessage("头像更新成功");
            result.setAccount(findByAccountId(accountId));
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("头像更新失败：" + e.getMessage());
        }
        return result;
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
            Account account = accountMapper.findByAccountId(accountId);
            List<Assignment> assignments;
            if (account != null && "老师".equals(account.getIdentity())) {
                assignments = accountMapper.findCourseAssignments(id);
            } else {
                assignments = accountMapper.findAssignment(accountId, id);
            }
            if (account != null && "学生".equals(account.getIdentity()) && assignments != null) {
                List<Assignment> visibleAssignments = new ArrayList<>();
                for (Assignment assignment : assignments) {
                    if (assignment == null) {
                        continue;
                    }
                    String deadline = assignment.getDeadline();
                    if (deadline != null && !deadline.trim().isEmpty() && !"待发布".equals(deadline)) {
                        visibleAssignments.add(assignment);
                    }
                }
                assignments = visibleAssignments;
            }
            enrichAssignmentsWithFileMeta(assignments);
            enrichAssignmentsWithResourceMeta(assignments);
            Course course = findByCourseId(id);
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
    public Result updateAssignment(String accountId, String id, String assignmentId, String assignmentContent, MultipartFile file) {
        Result result = new Result();
        try {
            Assignment currentAssignment = loadSingleAssignment(accountId, id, assignmentId);
            if (isAssignmentDeadlinePassed(currentAssignment)) {
                result.setSuccess(false);
                result.setMessage("已超过作业截止时间，不能再提交");
                result.setAssignment(currentAssignment);
                return result;
            }
            storeAssignmentFile(accountId, id, assignmentId, file);
            if (accountMapper.updateAssignment(accountId, id, assignmentId, assignmentContent) && accountMapper.updateSubmit(accountId, id, assignmentId, "已提交")) {
                Assignment updatedAssignment = loadSingleAssignment(accountId, id, assignmentId);
                runAiReviewIfEnabled(updatedAssignment);
                Assignment assignment = loadSingleAssignment(accountId, id, assignmentId);
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

    @Override
    public Result deleteAssignmentSubmissionFile(String accountId, String id, String assignmentId) {
        Result result = new Result();
        try {
            clearAssignmentSubmissionFile(accountId, id, assignmentId);
            Assignment assignment = loadSingleAssignment(accountId, id, assignmentId);
            result.setSuccess(true);
            result.setMessage("附件删除成功");
            result.setAssignment(assignment);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("附件删除失败：" + e.getMessage());
        }
        return result;
    }

    //点击我教的课程卡片，展示已经提交的作业
    @Override
    public Result findSubmitAssignment(String accountId, String id, String assignmentId) {
        Result result = new Result();
        try {
            List<Assignment> assignments = accountMapper.findAssignmentSubmissions(id, assignmentId);
            enrichAssignmentsWithFileMeta(assignments);
            enrichAssignmentsWithResourceMeta(assignments);
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
    public Result updateScore(Integer score, String teacherComment, String accountId, String id, String assignmentId) {
        Result result = new Result();
        try {
            if (accountMapper.updateScore(score, teacherComment, accountId, id, assignmentId) > 0) {
                List<Assignment> assignments = accountMapper.findAssignmentSubmissions(id, assignmentId);
                enrichAssignmentsWithFileMeta(assignments);
                enrichAssignmentsWithResourceMeta(assignments);
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
    public Result insertAssignments(String accountIdNull, String id, Assignment assignment, MultipartFile file) {
        Result result = new Result();
        try {
            String scheduleValidationMessage = validateAssignmentSchedule(assignment);
            if (scheduleValidationMessage != null) {
                result.setSuccess(false);
                result.setMessage(scheduleValidationMessage);
                return result;
            }
            //生成一个不重复的作业码
            String randomCourseCode = generateRandomCourseCode();
            //确保生成的作业码为新
            while (findByAssignmentId(randomCourseCode)) {
                randomCourseCode = generateRandomCourseCode();
            }
            //取出课程中的所有账号
            String accountIds = findStudents(id);
            accountMapper.insertAssignment(accountIdNull, id, randomCourseCode, assignment.getTitle(), assignment.getPublishTime(), assignment.getDeadline()
                    , assignment.getAssignmentType(), assignment.getContent(), assignment.getTotalScore(), assignment.getAiEnabled());
            if (assignment.getPublishTime() != null && !assignment.getPublishTime().isBlank() && accountIds != null) {
                String[] accountIdArr = accountIds.split(",");
                for (int i = 0; i < accountIdArr.length; i++) {
                    String accountId = accountIdArr[i];
                    if (accountId == null || accountId.isBlank()) {
                        continue;
                    }
                    accountMapper.insertAssignment(accountId, id, randomCourseCode, assignment.getTitle(), assignment.getPublishTime(), assignment.getDeadline()
                            , assignment.getAssignmentType(), assignment.getContent(), assignment.getTotalScore(), assignment.getAiEnabled());
                    createNotificationForPublishedAssignment(accountId, id, randomCourseCode, assignment.getTitle(), accountIdNull);
                }
            }
            storeAssignmentResource(id, randomCourseCode, file);

            result.setSuccess(true);
            result.setMessage("发布作业成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("发布作业失败：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Result updateCourseAssignment(String id, String assignmentId, Assignment assignment, MultipartFile file, Boolean removeAttachment) {
        Result result = new Result();
        try {
            String scheduleValidationMessage = validateAssignmentSchedule(assignment);
            if (scheduleValidationMessage != null) {
                result.setSuccess(false);
                result.setMessage(scheduleValidationMessage);
                return result;
            }
            boolean updated = accountMapper.updateCourseAssignment(
                    id,
                    assignmentId,
                    assignment.getTitle(),
                    assignment.getPublishTime(),
                    assignment.getDeadline(),
                    assignment.getAssignmentType(),
                    assignment.getContent(),
                    assignment.getTotalScore(),
                    assignment.getAiEnabled()
            );
            if (Boolean.TRUE.equals(removeAttachment)) {
                clearAssignmentResource(id, assignmentId);
            }
            if (file != null && !file.isEmpty()) {
                storeAssignmentResource(id, assignmentId, file);
            }
            if (assignment.getPublishTime() != null && !assignment.getPublishTime().isBlank()) {
                distributePublishedAssignmentToStudents(id, assignmentId, assignment);
            }
            result.setSuccess(updated);
            result.setMessage(updated ? "作业更新成功" : "作业更新失败");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("作业更新失败：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Result deleteCourseAssignment(String id, String assignmentId) {
        Result result = new Result();
        try {
            boolean deleted = accountMapper.deleteCourseAssignment(id, assignmentId);
            deleteAssignmentDirectory(id, assignmentId);
            result.setSuccess(deleted);
            result.setMessage(deleted ? "作业删除成功" : "作业删除失败");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("作业删除失败：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Result toggleAssignmentAi(String id, String assignmentId, Boolean aiEnabled) {
        Result result = new Result();
        try {
            boolean updated = accountMapper.updateAssignmentAiEnabled(id, assignmentId, aiEnabled);
            if (!updated) {
                result.setSuccess(false);
                result.setMessage("AI预批阅设置失败");
                return result;
            }
            if (Boolean.TRUE.equals(aiEnabled)) {
                rerunAiReviewForAssignment(id, assignmentId);
            } else {
                accountMapper.clearAssignmentAiReview(id, assignmentId);
            }
            result.setSuccess(true);
            result.setMessage(Boolean.TRUE.equals(aiEnabled) ? "已开启AI预批阅" : "已关闭AI预批阅");
            return result;
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("AI预批阅设置失败：" + e.getMessage());
            return result;
        }
    }

    @Override
    public Result getCourseMembers(String courseId) {
        Result result = new Result();
        try {
            List<Account> accounts = accountMapper.findCourseMembers(courseId);
            result.setSuccess(true);
            result.setMessage("课程成员获取成功");
            result.setAccounts(accounts);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("课程成员获取失败：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Result getNotifications(String accountId) {
        Result result = new Result();
        try {
            List<CourseNotification> notifications = accountMapper.findNotificationsByAccountId(accountId);
            result.setSuccess(true);
            result.setMessage("通知获取成功");
            result.setNotifications(notifications);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("通知获取失败：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Result markNotificationAsRead(Long notificationId, String accountId) {
        Result result = new Result();
        try {
            accountMapper.markNotificationAsRead(notificationId, accountId);
            result.setSuccess(true);
            result.setMessage("通知已读成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("通知已读失败：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Result markAllNotificationsAsRead(String accountId) {
        Result result = new Result();
        try {
            accountMapper.markAllNotificationsAsRead(accountId);
            result.setSuccess(true);
            result.setMessage("全部通知已读成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("全部通知已读失败：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Result sendAssignmentReminder(String courseId, String assignmentId, String targetAccountId, String teacherAccountId) {
        Result result = new Result();
        try {
            Assignment assignment = loadSingleAssignment(targetAccountId, courseId, assignmentId);
            if (assignment == null) {
                result.setSuccess(false);
                result.setMessage("未找到需要催交的作业");
                return result;
            }
            Course course = findByCourseId(courseId);
            Account teacher = findByAccountId(teacherAccountId);
            String title = assignment.getTitle() == null ? "未命名作业" : assignment.getTitle();
            String courseName = course == null ? "课程" : course.getName();
            String senderName = teacher == null ? "老师" : teacher.getName();
            accountMapper.insertNotification(
                    targetAccountId,
                    courseId,
                    assignmentId,
                    "remind",
                    "催交作业",
                    String.format("%s 提醒你尽快提交《%s》课程中的作业《%s》", senderName, courseName, title),
                    senderName,
                    Boolean.FALSE
            );
            result.setSuccess(true);
            result.setMessage("催交成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("催交失败：" + e.getMessage());
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

    @Override
    public void syncCourseAssignmentsForStudent(String accountId, String id) {
        if (accountId == null || accountId.trim().isEmpty() || id == null || id.trim().isEmpty()) {
            return;
        }
        List<Assignment> courseAssignments = accountMapper.findCourseAssignments(id);
        if (courseAssignments == null || courseAssignments.isEmpty()) {
            return;
        }
        List<Assignment> existingAssignments = accountMapper.findAssignment(accountId, id);
        Set<String> existingAssignmentIds = new HashSet<>();
        if (existingAssignments != null) {
            for (Assignment existingAssignment : existingAssignments) {
                if (existingAssignment != null && existingAssignment.getAssignmentId() != null) {
                    existingAssignmentIds.add(existingAssignment.getAssignmentId());
                }
            }
        }
        for (Assignment assignment : courseAssignments) {
            if (assignment == null || assignment.getAssignmentId() == null) {
                continue;
            }
            if (assignment.getPublishTime() == null || assignment.getPublishTime().isBlank()) {
                continue;
            }
            if (existingAssignmentIds.contains(assignment.getAssignmentId())) {
                continue;
            }
            Integer count = accountMapper.countAssignmentByStudent(accountId, id, assignment.getAssignmentId());
            if (count != null && count > 0) {
                continue;
            }
            accountMapper.insertAssignment(
                    accountId,
                    id,
                    assignment.getAssignmentId(),
                    assignment.getTitle(),
                    assignment.getPublishTime(),
                    assignment.getDeadline(),
                    assignment.getAssignmentType(),
                    assignment.getContent(),
                    assignment.getTotalScore(),
                    assignment.getAiEnabled()
            );
        }
    }

    private void distributePublishedAssignmentToStudents(String courseId, String assignmentId, Assignment assignment) {
        String studentIds = findStudents(courseId);
        if (studentIds == null || studentIds.isBlank()) {
            return;
        }
        Course course = accountMapper.findByCourseId(courseId);
        String teacherAccountId = course == null ? null : course.getAccountId();
        for (String accountId : studentIds.split(",")) {
            if (accountId == null || accountId.isBlank()) {
                continue;
            }
            Integer count = accountMapper.countAssignmentByStudent(accountId, courseId, assignmentId);
            if (count != null && count > 0) {
                continue;
            }
            accountMapper.insertAssignment(
                    accountId,
                    courseId,
                    assignmentId,
                    assignment.getTitle(),
                    assignment.getPublishTime(),
                    assignment.getDeadline(),
                    assignment.getAssignmentType(),
                    assignment.getContent(),
                    assignment.getTotalScore(),
                    assignment.getAiEnabled()
            );
            if (teacherAccountId != null && !teacherAccountId.isBlank()) {
                createNotificationForPublishedAssignment(accountId, courseId, assignmentId, assignment.getTitle(), teacherAccountId);
            }
        }
    }

    @Override
    public Resource loadAssignmentFile(String accountId, String id, String assignmentId) throws IOException {
        Assignment meta = getAssignmentFileMeta(accountId, id, assignmentId);
        if (meta == null || meta.getFileName() == null) {
            return null;
        }
        try {
            String storedName = meta.getFileStoredName();
            if (storedName == null || storedName.isBlank()) {
                storedName = readFileMeta(getSubmissionDirectory(accountId, id, assignmentId)).getProperty("storedName");
            }
            if (storedName == null || storedName.isBlank()) {
                return null;
            }
            Path targetFile = getSubmissionDirectory(accountId, id, assignmentId).resolve(storedName);
            if (!Files.exists(targetFile)) {
                return null;
            }
            return new UrlResource(targetFile.toUri());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public Assignment getAssignmentFileMeta(String accountId, String id, String assignmentId) {
        try {
            return readAssignmentFileMeta(accountId, id, assignmentId);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Resource loadAssignmentResource(String id, String assignmentId) throws IOException {
        Assignment meta = getAssignmentResourceMeta(id, assignmentId);
        if (meta == null || meta.getAttachmentName() == null) {
            return null;
        }
        try {
            String storedName = meta.getAttachmentStoredName();
            if (storedName == null || storedName.isBlank()) {
                return null;
            }
            Path targetFile = getAssignmentResourceDirectory(id, assignmentId).resolve(storedName);
            if (!Files.exists(targetFile)) {
                return null;
            }
            return new UrlResource(targetFile.toUri());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public Assignment getAssignmentResourceMeta(String id, String assignmentId) {
        try {
            return readAssignmentResourceMeta(id, assignmentId);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Resource loadAccountAvatar(String accountId) throws IOException {
        Account account = accountMapper.findByAccountId(accountId);
        if (account == null || account.getAvatarStoredName() == null || account.getAvatarStoredName().isBlank()) {
            return null;
        }
        Path targetFile = getAccountAvatarDirectory(accountId).resolve(account.getAvatarStoredName());
        if (!Files.exists(targetFile)) {
            return null;
        }
        try {
            return new UrlResource(targetFile.toUri());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private void enrichAssignmentsWithFileMeta(List<Assignment> assignments) {
        if (assignments == null) {
            return;
        }
        for (Assignment assignment : assignments) {
            if (assignment == null) {
                continue;
            }
            if (assignment.getFileName() != null && !assignment.getFileName().isBlank()) {
                assignment.setFileDownloadUrl(String.format("http://localhost:8080/assignment-file?accountId=%s&id=%s&assignmentId=%s",
                        assignment.getAccountId(), assignment.getId(), assignment.getAssignmentId()));
                continue;
            }
            try {
                Assignment fileMeta = readAssignmentFileMeta(assignment.getAccountId(), assignment.getId(), assignment.getAssignmentId());
                if (fileMeta != null) {
                    assignment.setFileName(fileMeta.getFileName());
                    assignment.setFileStoredName(fileMeta.getFileStoredName());
                    assignment.setFileSize(fileMeta.getFileSize());
                    assignment.setFileContentType(fileMeta.getFileContentType());
                    assignment.setFileDownloadUrl(fileMeta.getFileDownloadUrl());
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void enrichAssignmentsWithResourceMeta(List<Assignment> assignments) {
        if (assignments == null) {
            return;
        }
        for (Assignment assignment : assignments) {
            if (assignment == null || assignment.getId() == null || assignment.getAssignmentId() == null) {
                continue;
            }
            try {
                Assignment resourceMeta = readAssignmentResourceMeta(assignment.getId(), assignment.getAssignmentId());
                if (resourceMeta == null) {
                    continue;
                }
                assignment.setAttachmentName(resourceMeta.getAttachmentName());
                assignment.setAttachmentStoredName(resourceMeta.getAttachmentStoredName());
                assignment.setAttachmentSize(resourceMeta.getAttachmentSize());
                assignment.setAttachmentContentType(resourceMeta.getAttachmentContentType());
                assignment.setAttachmentDownloadUrl(resourceMeta.getAttachmentDownloadUrl());
            } catch (IOException ignored) {
            }
        }
    }

    private Assignment loadSingleAssignment(String accountId, String id, String assignmentId) throws IOException {
        List<Assignment> assignments = accountMapper.findAssignment(accountId, id);
        if (assignments == null) {
            return null;
        }
        for (Assignment assignment : assignments) {
            if (assignment != null && Objects.equals(assignment.getAssignmentId(), assignmentId)) {
                if (assignment.getFileName() != null && !assignment.getFileName().isBlank()) {
                    assignment.setFileDownloadUrl(String.format("http://localhost:8080/assignment-file?accountId=%s&id=%s&assignmentId=%s",
                            accountId, id, assignmentId));
                } else {
                    Assignment fileMeta = readAssignmentFileMeta(accountId, id, assignmentId);
                    if (fileMeta != null) {
                        assignment.setFileName(fileMeta.getFileName());
                        assignment.setFileStoredName(fileMeta.getFileStoredName());
                        assignment.setFileSize(fileMeta.getFileSize());
                        assignment.setFileContentType(fileMeta.getFileContentType());
                        assignment.setFileDownloadUrl(fileMeta.getFileDownloadUrl());
                    }
                }
                return assignment;
            }
        }
        return null;
    }

    private boolean isAssignmentDeadlinePassed(Assignment assignment) {
        if (assignment == null) {
            return false;
        }
        LocalDateTime deadlineTime = parseAssignmentDateTime(assignment.getDeadline());
        if (deadlineTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(deadlineTime);
    }

    private String validateAssignmentSchedule(Assignment assignment) {
        if (assignment == null) {
            return null;
        }
        LocalDateTime publishTime = parseAssignmentDateTime(assignment.getPublishTime());
        LocalDateTime deadlineTime = parseAssignmentDateTime(assignment.getDeadline());
        if (publishTime != null && deadlineTime != null && deadlineTime.isBefore(publishTime)) {
            return "截止日期不能小于发布日期";
        }
        return null;
    }

    private LocalDateTime parseAssignmentDateTime(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || "暂无".equals(normalized) || "待发布".equals(normalized)) {
            return null;
        }
        normalized = normalized.replace('/', '-');
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (Exception ignored) {
            }
        }
        try {
            return LocalDateTime.parse(normalized.replace('T', ' ').substring(0, Math.min(normalized.length(), 19)),
                    DateTimeFormatter.ofPattern(normalized.length() >= 19 ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd HH:mm"));
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(normalized).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception ignored) {
        }
        return null;
    }

    private void storeAssignmentFile(String accountId, String id, String assignmentId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return;
        }
        Path submissionDirectory = getSubmissionDirectory(accountId, id, assignmentId);
        Files.createDirectories(submissionDirectory);
        clearExistingSubmissionFiles(submissionDirectory);

        String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }
        String storedName = UUID.randomUUID() + extension;
        Path targetFile = submissionDirectory.resolve(storedName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        Properties properties = new Properties();
        properties.setProperty("storedName", storedName);
        properties.setProperty("originalName", originalFilename);
        properties.setProperty("fileSize", String.valueOf(file.getSize()));
        properties.setProperty("contentType", file.getContentType() == null ? "" : file.getContentType());
        try (OutputStream outputStream = Files.newOutputStream(submissionDirectory.resolve(FILE_META_NAME))) {
            properties.store(outputStream, "assignment file meta");
        }

        try {
            accountMapper.updateAssignmentFileMeta(
                    accountId,
                    id,
                    assignmentId,
                    originalFilename,
                    storedName,
                    file.getSize(),
                    file.getContentType()
            );
        } catch (Exception ignored) {
        }
    }

    private void clearAssignmentSubmissionFile(String accountId, String id, String assignmentId) throws IOException {
        Path submissionDirectory = getSubmissionDirectory(accountId, id, assignmentId);
        clearExistingSubmissionFiles(submissionDirectory);
        Files.deleteIfExists(submissionDirectory.resolve(FILE_META_NAME));
        Files.deleteIfExists(submissionDirectory);
        accountMapper.clearAssignmentFileMeta(accountId, id, assignmentId);
    }

    private void storeAssignmentResource(String id, String assignmentId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return;
        }
        Path resourceDirectory = getAssignmentResourceDirectory(id, assignmentId);
        Files.createDirectories(resourceDirectory);
        clearExistingSubmissionFiles(resourceDirectory);

        String originalFilename = Paths.get(Objects.requireNonNullElse(file.getOriginalFilename(), "assignment-file")).getFileName().toString();
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }
        String storedName = UUID.randomUUID() + extension;
        Path targetFile = resourceDirectory.resolve(storedName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
        accountMapper.upsertAssignmentResource(id, assignmentId, originalFilename, storedName, file.getSize(), file.getContentType());
    }

    private void clearAssignmentResource(String id, String assignmentId) throws IOException {
        Path resourceDirectory = getAssignmentResourceDirectory(id, assignmentId);
        clearExistingSubmissionFiles(resourceDirectory);
        Files.deleteIfExists(resourceDirectory);
        accountMapper.deleteAssignmentResource(id, assignmentId);
    }

    private void deleteAssignmentDirectory(String id, String assignmentId) throws IOException {
        Path assignmentDirectory = uploadRoot.resolve(id).resolve(assignmentId);
        if (!Files.exists(assignmentDirectory)) {
            clearAssignmentResource(id, assignmentId);
            return;
        }
        Files.walk(assignmentDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        clearAssignmentResource(id, assignmentId);
    }

    public void deleteCourseCompletely(String courseId) throws IOException {
        if (courseId == null || courseId.isBlank()) {
            return;
        }
        accountMapper.deleteNotificationsByCourseId(courseId);
        accountMapper.deleteAssignmentResourcesByCourseId(courseId);
        accountMapper.deleteAssignmentsByCourseId(courseId);
        accountMapper.deleteCourseById(courseId);
        deleteDirectoryRecursively(uploadRoot.resolve(courseId));
        deleteDirectoryRecursively(assignmentResourceRoot.resolve(courseId));
    }

    private void clearExistingSubmissionFiles(Path submissionDirectory) throws IOException {
        if (!Files.exists(submissionDirectory)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(submissionDirectory)) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }
    }

    private void deleteDirectoryRecursively(Path targetDirectory) throws IOException {
        if (targetDirectory == null || !Files.exists(targetDirectory)) {
            return;
        }
        Files.walk(targetDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
    }

    private Assignment readAssignmentFileMeta(String accountId, String id, String assignmentId) throws IOException {
        if (accountId == null || id == null || assignmentId == null) {
            return null;
        }
        Assignment dbMeta = null;
        try {
            dbMeta = accountMapper.findAssignmentFileMeta(accountId, id, assignmentId);
        } catch (Exception ignored) {
        }
        if (dbMeta != null && dbMeta.getFileName() != null && !dbMeta.getFileName().isBlank() && dbMeta.getFileStoredName() != null && !dbMeta.getFileStoredName().isBlank()) {
            Assignment assignment = new Assignment();
            assignment.setFileName(dbMeta.getFileName());
            assignment.setFileStoredName(dbMeta.getFileStoredName());
            assignment.setFileSize(dbMeta.getFileSize());
            assignment.setFileContentType(dbMeta.getFileContentType());
            assignment.setFileDownloadUrl(String.format("http://localhost:8080/assignment-file?accountId=%s&id=%s&assignmentId=%s", accountId, id, assignmentId));
            return assignment;
        }
        Path submissionDirectory = getSubmissionDirectory(accountId, id, assignmentId);
        if (!Files.exists(submissionDirectory)) {
            return null;
        }
        Properties properties = readFileMeta(submissionDirectory);
        String originalName = properties.getProperty("originalName");
        if (originalName == null || originalName.isBlank()) {
            return null;
        }
        Assignment assignment = new Assignment();
        assignment.setFileName(originalName);
        assignment.setFileStoredName(properties.getProperty("storedName"));
        String fileSize = properties.getProperty("fileSize");
        if (fileSize != null && !fileSize.isBlank()) {
            assignment.setFileSize(Long.parseLong(fileSize));
        }
        assignment.setFileContentType(properties.getProperty("contentType"));
        assignment.setFileDownloadUrl(String.format("http://localhost:8080/assignment-file?accountId=%s&id=%s&assignmentId=%s", accountId, id, assignmentId));
        return assignment;
    }

    private Properties readFileMeta(Path submissionDirectory) throws IOException {
        Path metaFile = submissionDirectory.resolve(FILE_META_NAME);
        Properties properties = new Properties();
        if (!Files.exists(metaFile)) {
            return properties;
        }
        try (InputStream inputStream = Files.newInputStream(metaFile)) {
            properties.load(inputStream);
        }
        return properties;
    }

    private Path getSubmissionDirectory(String accountId, String id, String assignmentId) {
        return uploadRoot.resolve(id).resolve(assignmentId).resolve(accountId);
    }

    private Path getAssignmentResourceDirectory(String id, String assignmentId) {
        return assignmentResourceRoot.resolve(id).resolve(assignmentId);
    }

    private Path getAccountAvatarDirectory(String accountId) {
        return avatarRoot.resolve(accountId);
    }

    private Assignment readAssignmentResourceMeta(String id, String assignmentId) throws IOException {
        if (id == null || assignmentId == null) {
            return null;
        }
        Assignment dbMeta = accountMapper.findAssignmentResource(id, assignmentId);
        if (dbMeta == null || dbMeta.getAttachmentName() == null || dbMeta.getAttachmentStoredName() == null) {
            return null;
        }
        Assignment assignment = new Assignment();
        assignment.setAttachmentName(dbMeta.getAttachmentName());
        assignment.setAttachmentStoredName(dbMeta.getAttachmentStoredName());
        assignment.setAttachmentSize(dbMeta.getAttachmentSize());
        assignment.setAttachmentContentType(dbMeta.getAttachmentContentType());
        assignment.setAttachmentDownloadUrl(String.format("http://localhost:8080/assignment-resource?id=%s&assignmentId=%s", id, assignmentId));
        return assignment;
    }

    private void createNotificationForPublishedAssignment(String studentAccountId, String courseId, String assignmentId, String assignmentTitle, String teacherAccountId) {
        Course course = findByCourseId(courseId);
        Account teacher = findByAccountId(teacherAccountId);
        String senderName = teacher == null ? "老师" : teacher.getName();
        String courseName = course == null ? "课程" : course.getName();
        accountMapper.insertNotification(
                studentAccountId,
                courseId,
                assignmentId,
                "assignment",
                "新作业通知",
                String.format("%s 在《%s》中发布了作业《%s》", senderName, courseName, assignmentTitle == null ? "未命名作业" : assignmentTitle),
                senderName,
                Boolean.FALSE
        );
    }

    private void enrichAccountAvatar(Account account) {
        if (account == null) {
            return;
        }
        String storedName = account.getAvatarStoredName();
        if (storedName == null || storedName.isBlank()) {
            account.setAvatarUrl(null);
            return;
        }
        Path targetFile = getAccountAvatarDirectory(account.getAccountId()).resolve(storedName);
        if (!Files.exists(targetFile)) {
            account.setAvatarUrl(null);
            return;
        }
        account.setAvatarUrl("http://localhost:8080/account-avatar/" + account.getAccountId() + "?t=" + System.currentTimeMillis());
    }

    private String storeAccountAvatar(String accountId, MultipartFile file) throws IOException {
        Path avatarDirectory = getAccountAvatarDirectory(accountId);
        Files.createDirectories(avatarDirectory);
        clearExistingSubmissionFiles(avatarDirectory);

        String originalFilename = Paths.get(Objects.requireNonNullElse(file.getOriginalFilename(), "avatar-image")).getFileName().toString();
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }
        String storedName = UUID.randomUUID() + extension;
        Path targetFile = avatarDirectory.resolve(storedName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return storedName;
    }

    private void runAiReviewIfEnabled(Assignment assignment) {
        if (assignment == null || !Boolean.TRUE.equals(assignment.getAiEnabled())) {
            return;
        }
        String submitContent = assignment.getSubmitContent();
        if ((submitContent == null || submitContent.isBlank()) && (assignment.getFileName() == null || assignment.getFileName().isBlank())) {
            return;
        }
        AiEvaluationService.AiEvaluationResult aiResult = aiEvaluationService.evaluate(
                assignment.getTitle(),
                assignment.getContent(),
                assignment.getTotalScore() == null ? 100 : assignment.getTotalScore(),
                buildAiSubmissionText(assignment)
        );
        if (aiResult == null) {
            return;
        }
        accountMapper.updateAssignmentAiReview(
                assignment.getAccountId(),
                assignment.getId(),
                assignment.getAssignmentId(),
                aiResult.getScore(),
                aiResult.getComment()
        );
    }

    private void rerunAiReviewForAssignment(String id, String assignmentId) {
        List<Assignment> submissions = accountMapper.findAssignmentSubmissions(id, assignmentId);
        if (submissions == null) {
            return;
        }
        enrichAssignmentsWithFileMeta(submissions);
        for (Assignment submission : submissions) {
            if (submission == null || submission.getAccountId() == null) {
                continue;
            }
            if (!"已提交".equals(submission.getSubmit())) {
                continue;
            }
            submission.setAiEnabled(true);
            runAiReviewIfEnabled(submission);
        }
    }

    private String buildAiSubmissionText(Assignment assignment) {
        StringBuilder builder = new StringBuilder();
        if (assignment.getSubmitContent() != null && !assignment.getSubmitContent().isBlank()) {
            builder.append("学生留言：").append(assignment.getSubmitContent());
        }
        if (assignment.getFileName() != null && !assignment.getFileName().isBlank()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("附件名称：").append(assignment.getFileName());
        }
        return builder.toString();
    }
}

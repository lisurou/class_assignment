package org.example.classAssignment.service;

import org.example.classAssignment.mapper.AccountMapper;
import org.example.classAssignment.pojo.Account;
import org.example.classAssignment.pojo.Assignment;
import org.example.classAssignment.pojo.Course;
import org.example.classAssignment.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class AccountService implements AccountServiceInterface {
    private static final String FILE_META_NAME = "meta.properties";
    private final Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads", "assignments");

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
            Account account = accountMapper.findByAccountId(accountId);
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
    public Result updateAssignment(String accountId, String id, String assignmentId, String assignmentContent, MultipartFile file) {
        Result result = new Result();
        try {
            storeAssignmentFile(accountId, id, assignmentId, file);
            if (accountMapper.updateAssignment(accountId, id, assignmentId, assignmentContent) && accountMapper.updateSubmit(accountId, id, assignmentId, "已提交")) {
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

    //点击我教的课程卡片，展示已经提交的作业
    @Override
    public Result findSubmitAssignment(String accountId, String id, String assignmentId) {
        Result result = new Result();
        try {
            List<Assignment> assignments = accountMapper.findAssignmentSubmissions(id, assignmentId);
            enrichAssignmentsWithFileMeta(assignments);
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
                List<Assignment> assignments = accountMapper.findAssignmentSubmissions(id, assignmentId);
                enrichAssignmentsWithFileMeta(assignments);
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

    @Override
    public Result updateCourseAssignment(String id, String assignmentId, Assignment assignment) {
        Result result = new Result();
        try {
            boolean updated = accountMapper.updateCourseAssignment(
                    id,
                    assignmentId,
                    assignment.getTitle(),
                    assignment.getDeadline(),
                    assignment.getAssignmentType(),
                    assignment.getContent(),
                    assignment.getTotalScore()
            );
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
            result.setSuccess(deleted);
            result.setMessage(deleted ? "作业删除成功" : "作业删除失败");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("作业删除失败：" + e.getMessage());
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
                    assignment.getDeadline(),
                    assignment.getAssignmentType(),
                    assignment.getContent(),
                    assignment.getTotalScore()
            );
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
}

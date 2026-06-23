package org.example.classAssignment.service;

import org.example.classAssignment.mapper.MaterialMapper;
import org.example.classAssignment.pojo.Account;
import org.example.classAssignment.pojo.CreateFolderRequest;
import org.example.classAssignment.pojo.CreateLinkRequest;
import org.example.classAssignment.pojo.MaterialAttachment;
import org.example.classAssignment.pojo.MaterialCategory;
import org.example.classAssignment.pojo.MaterialFolder;
import org.example.classAssignment.pojo.MaterialLink;
import org.example.classAssignment.pojo.MaterialResult;
import org.example.classAssignment.pojo.MoveFolderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class MaterialService {
    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private AccountService accountService;

    @Value("${materials.storage-path:uploads/materials}")
    private String storagePath;

    public MaterialResult listFolders(String courseId, String accountId, String category) {
        MaterialResult result = new MaterialResult();
        try {
            assertCourseReadable(accountId, courseId);
            String normalizedCategory = MaterialCategory.normalize(category);
            List<MaterialFolder> folders = materialMapper.findFoldersByCourseId(courseId, normalizedCategory);
            result.setFolders(folders);
            result.setSuccess(true);
            result.setMessage("资料文件夹获取成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("资料文件夹获取失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult createFolder(String courseId, CreateFolderRequest request) {
        MaterialResult result = new MaterialResult();
        try {
            assertCourseTeacher(request.getAccountId(), courseId);
            String normalizedCategory = MaterialCategory.normalize(request.getCategory());
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("文件夹名称不能为空");
            }
            Long parentId = request.getParentId();
            if (parentId != null) {
                MaterialFolder parent = materialMapper.findFolderById(courseId, parentId);
                if (parent == null) {
                    throw new IllegalArgumentException("父文件夹不存在");
                }
                assertFolderCategory(parent, normalizedCategory);
            }

            MaterialFolder folder = new MaterialFolder();
            folder.setCourseId(courseId);
            folder.setCategory(normalizedCategory);
            folder.setParentId(parentId);
            folder.setName(request.getName().trim());
            folder.setCreatedBy(request.getAccountId());
            if (!Boolean.TRUE.equals(materialMapper.insertFolder(folder))) {
                throw new IllegalStateException("创建文件夹失败");
            }
            result.setFolder(folder);
            result.setSuccess(true);
            result.setMessage("创建文件夹成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("创建文件夹失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult moveFolder(String courseId, Long folderId, MoveFolderRequest request) {
        MaterialResult result = new MaterialResult();
        try {
            assertCourseTeacher(request.getAccountId(), courseId);
            MaterialFolder folder = materialMapper.findFolderById(courseId, folderId);
            if (folder == null) {
                throw new IllegalArgumentException("目标文件夹不存在");
            }

            Long newParentId = request.getNewParentId();
            if (newParentId != null) {
                if (Objects.equals(newParentId, folderId)) {
                    throw new IllegalArgumentException("不能移动到自身");
                }
                MaterialFolder newParent = materialMapper.findFolderById(courseId, newParentId);
                if (newParent == null) {
                    throw new IllegalArgumentException("新的父文件夹不存在");
                }
                assertFolderCategory(newParent, folder.getCategory());

                List<MaterialFolder> all = materialMapper.findFoldersByCourseId(courseId, folder.getCategory());
                Set<Long> descendants = collectDescendantFolderIds(all, folderId);
                if (descendants.contains(newParentId)) {
                    throw new IllegalArgumentException("不能移动到子文件夹中");
                }
            }

            int updated = materialMapper.updateFolderParent(courseId, folderId, newParentId);
            if (updated <= 0) {
                throw new IllegalStateException("移动文件夹失败");
            }
            folder.setParentId(newParentId);
            result.setFolder(folder);
            result.setSuccess(true);
            result.setMessage("移动文件夹成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("移动文件夹失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult deleteFolder(String courseId, Long folderId, String accountId) {
        MaterialResult result = new MaterialResult();
        try {
            assertCourseTeacher(accountId, courseId);
            MaterialFolder folder = materialMapper.findFolderById(courseId, folderId);
            if (folder == null) {
                throw new IllegalArgumentException("文件夹不存在");
            }

            List<MaterialFolder> allFolders = materialMapper.findFoldersByCourseId(courseId, folder.getCategory());
            List<Long> deleteOrder = collectPostOrderFolderIds(allFolders, folderId);

            for (Long id : deleteOrder) {
                deleteFolderContent(courseId, id);
                materialMapper.deleteFolder(courseId, id);
            }

            result.setSuccess(true);
            result.setMessage("删除文件夹成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除文件夹失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult listAttachments(String courseId, String accountId, String category) {
        MaterialResult result = new MaterialResult();
        try {
            assertCourseReadable(accountId, courseId);
            String normalizedCategory = MaterialCategory.normalize(category);
            List<MaterialAttachment> attachments = materialMapper.findAttachmentsByCourseId(courseId, normalizedCategory);
            result.setAttachments(attachments);
            result.setSuccess(true);
            result.setMessage("附件资料获取成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("附件资料获取失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult uploadAttachment(String courseId, String accountId, String category, Long folderId, MultipartFile file) {
        MaterialResult result = new MaterialResult();
        try {
            assertCourseTeacher(accountId, courseId);
            String normalizedCategory = MaterialCategory.normalize(category);
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("文件不能为空");
            }
            if (folderId != null) {
                MaterialFolder folder = materialMapper.findFolderById(courseId, folderId);
                if (folder == null) {
                    throw new IllegalArgumentException("文件夹不存在");
                }
                assertFolderCategory(folder, normalizedCategory);
            }

            String originalName = sanitizeFileName(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
            String storedName = UUID.randomUUID() + "_" + originalName;
            String relativePath = courseId + "/" + normalizedCategory + "/" + storedName;

            Path root = getStorageRoot();
            Path courseDir = root.resolve(courseId).resolve(normalizedCategory).normalize();
            Files.createDirectories(courseDir);

            Path target = courseDir.resolve(storedName).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalStateException("非法文件路径");
            }
            file.transferTo(target);

            MaterialAttachment attachment = new MaterialAttachment();
            attachment.setCourseId(courseId);
            attachment.setCategory(normalizedCategory);
            attachment.setFolderId(folderId);
            attachment.setOriginalName(originalName);
            attachment.setStoredName(storedName);
            attachment.setRelativePath(relativePath);
            attachment.setSize(file.getSize());
            attachment.setContentType(file.getContentType());
            attachment.setCreatedBy(accountId);
            if (!Boolean.TRUE.equals(materialMapper.insertAttachment(attachment))) {
                Files.deleteIfExists(target);
                throw new IllegalStateException("保存附件记录失败");
            }

            result.setAttachment(attachment);
            result.setSuccess(true);
            result.setMessage("上传附件成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("上传附件失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult deleteAttachment(String courseId, Long attachmentId, String accountId) {
        MaterialResult result = new MaterialResult();
        try {
            assertCourseTeacher(accountId, courseId);
            MaterialAttachment attachment = materialMapper.findAttachmentById(courseId, attachmentId);
            if (attachment == null) {
                throw new IllegalArgumentException("附件不存在");
            }

            deletePhysicalFile(attachment.getRelativePath());
            materialMapper.deleteAttachment(courseId, attachmentId);

            result.setSuccess(true);
            result.setMessage("删除附件成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除附件失败：" + e.getMessage());
        }
        return result;
    }

    public ResponseEntity<Resource> downloadAttachment(String courseId, Long attachmentId, String accountId) {
        assertCourseReadable(accountId, courseId);
        MaterialAttachment attachment = materialMapper.findAttachmentById(courseId, attachmentId);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        Path root = getStorageRoot();
        Path filePath = root.resolve(attachment.getRelativePath()).normalize();
        if (!filePath.startsWith(root) || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String downloadName = encodeFileName(attachment.getOriginalName());
        MediaType contentType = attachment.getContentType() == null || attachment.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(attachment.getContentType());

        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + downloadName)
                .body(resource);
    }

    public MaterialResult listLinks(String courseId, String accountId, String category) {
        MaterialResult result = new MaterialResult();
        try {
            assertCourseReadable(accountId, courseId);
            String normalizedCategory = MaterialCategory.normalize(category);
            List<MaterialLink> links = materialMapper.findLinksByCourseId(courseId, normalizedCategory);
            result.setLinks(links);
            result.setSuccess(true);
            result.setMessage("外链资料获取成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("外链资料获取失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult createLink(String courseId, CreateLinkRequest request) {
        MaterialResult result = new MaterialResult();
        try {
            assertCourseTeacher(request.getAccountId(), courseId);
            String normalizedCategory = MaterialCategory.normalize(request.getCategory());
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("标题不能为空");
            }
            if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
                throw new IllegalArgumentException("链接不能为空");
            }
            if (!isValidUrl(request.getUrl().trim())) {
                throw new IllegalArgumentException("链接格式不正确");
            }
            if (request.getFolderId() != null) {
                MaterialFolder folder = materialMapper.findFolderById(courseId, request.getFolderId());
                if (folder == null) {
                    throw new IllegalArgumentException("文件夹不存在");
                }
                assertFolderCategory(folder, normalizedCategory);
            }

            MaterialLink link = new MaterialLink();
            link.setCourseId(courseId);
            link.setCategory(normalizedCategory);
            link.setFolderId(request.getFolderId());
            link.setTitle(request.getTitle().trim());
            link.setUrl(request.getUrl().trim());
            link.setCreatedBy(request.getAccountId());
            if (!Boolean.TRUE.equals(materialMapper.insertLink(link))) {
                throw new IllegalStateException("创建外链失败");
            }

            result.setLink(link);
            result.setSuccess(true);
            result.setMessage("创建外链成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("创建外链失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult deleteLink(String courseId, Long linkId, String accountId) {
        MaterialResult result = new MaterialResult();
        try {
            assertCourseTeacher(accountId, courseId);
            MaterialLink link = materialMapper.findLinkById(courseId, linkId);
            if (link == null) {
                throw new IllegalArgumentException("外链不存在");
            }
            materialMapper.deleteLink(courseId, linkId);
            result.setSuccess(true);
            result.setMessage("删除外链成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除外链失败：" + e.getMessage());
        }
        return result;
    }

    private void deleteFolderContent(String courseId, Long folderId) {
        MaterialFolder folder = materialMapper.findFolderById(courseId, folderId);
        if (folder == null) {
            return;
        }
        List<MaterialAttachment> attachments = materialMapper.findAttachmentsByFolder(courseId, folderId);
        for (MaterialAttachment attachment : attachments) {
            assertAttachmentCategory(attachment, folder.getCategory());
            deletePhysicalFile(attachment.getRelativePath());
            materialMapper.deleteAttachment(courseId, attachment.getAttachmentId());
        }

        List<MaterialLink> links = materialMapper.findLinksByFolder(courseId, folderId);
        for (MaterialLink link : links) {
            assertLinkCategory(link, folder.getCategory());
            materialMapper.deleteLink(courseId, link.getLinkId());
        }
    }

    private Path getStorageRoot() {
        Path root = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建存储目录：" + root);
        }
        return root;
    }

    private void deletePhysicalFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path root = getStorageRoot();
        Path filePath = root.resolve(relativePath).normalize();
        if (!filePath.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    private void assertCourseTeacher(String accountId, String courseId) {
        assertCourseExists(courseId);
        Account account = accountService.findByAccountId(accountId);
        if (account == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        if (!"老师".equals(account.getIdentity())) {
            throw new IllegalArgumentException("无权限：仅教师可操作");
        }
        if (!containsCourseId(account.getTaught(), courseId)) {
            throw new IllegalArgumentException("无权限：非该课程教师");
        }
    }

    private void assertCourseReadable(String accountId, String courseId) {
        assertCourseExists(courseId);
        Account account = accountService.findByAccountId(accountId);
        if (account == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        if ("老师".equals(account.getIdentity())) {
            if (!containsCourseId(account.getTaught(), courseId)) {
                throw new IllegalArgumentException("无权限：非该课程教师");
            }
            return;
        }
        if ("学生".equals(account.getIdentity())) {
            if (!containsCourseId(account.getLearned(), courseId)) {
                throw new IllegalArgumentException("无权限：非该课程学生");
            }
            return;
        }
        throw new IllegalArgumentException("无权限：未知身份");
    }

    private void assertCourseExists(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("课程ID不能为空");
        }
        if (accountService.findByCourseId(courseId) == null) {
            throw new IllegalArgumentException("课程不存在");
        }
    }

    private boolean containsCourseId(String ids, String courseId) {
        if (ids == null || ids.isBlank()) {
            return false;
        }
        String[] parts = ids.split(",");
        for (String part : parts) {
            if (courseId.equals(part)) {
                return true;
            }
        }
        return false;
    }

    private String sanitizeFileName(String filename) {
        String base = Paths.get(filename).getFileName().toString();
        base = base.replaceAll("[\\\\/]+", "_");
        base = base.replaceAll("[\\r\\n\\t]", "_");
        if (base.isBlank()) {
            return "file";
        }
        return base;
    }

    private String encodeFileName(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean isValidUrl(String url) {
        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private void assertFolderCategory(MaterialFolder folder, String category) {
        if (!category.equals(folder.getCategory())) {
            throw new IllegalArgumentException("文件夹不属于当前资料分区");
        }
    }

    private void assertAttachmentCategory(MaterialAttachment attachment, String category) {
        if (!category.equals(attachment.getCategory())) {
            throw new IllegalArgumentException("附件不属于当前资料分区");
        }
    }

    private void assertLinkCategory(MaterialLink link, String category) {
        if (!category.equals(link.getCategory())) {
            throw new IllegalArgumentException("外链不属于当前资料分区");
        }
    }

    private Set<Long> collectDescendantFolderIds(List<MaterialFolder> folders, Long rootId) {
        Map<Long, List<Long>> childrenMap = buildChildrenMap(folders);
        Set<Long> visited = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootId);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            List<Long> children = childrenMap.get(current);
            if (children == null) {
                continue;
            }
            for (Long child : children) {
                if (visited.add(child)) {
                    stack.push(child);
                }
            }
        }
        return visited;
    }

    private List<Long> collectPostOrderFolderIds(List<MaterialFolder> folders, Long rootId) {
        Map<Long, List<Long>> childrenMap = buildChildrenMap(folders);
        List<Long> postOrder = new ArrayList<>();
        postOrderDfs(childrenMap, rootId, postOrder, new HashSet<>());
        return postOrder;
    }

    private void postOrderDfs(Map<Long, List<Long>> childrenMap, Long currentId, List<Long> out, Set<Long> visiting) {
        if (!visiting.add(currentId)) {
            return;
        }
        List<Long> children = childrenMap.get(currentId);
        if (children != null) {
            for (Long child : children) {
                postOrderDfs(childrenMap, child, out, visiting);
            }
        }
        out.add(currentId);
    }

    private Map<Long, List<Long>> buildChildrenMap(List<MaterialFolder> folders) {
        Map<Long, List<Long>> map = new HashMap<>();
        for (MaterialFolder folder : folders) {
            Long parentId = folder.getParentId();
            if (parentId == null || folder.getFolderId() == null) {
                continue;
            }
            map.computeIfAbsent(parentId, k -> new ArrayList<>()).add(folder.getFolderId());
        }
        return map;
    }
}

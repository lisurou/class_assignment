package org.example.classAssignment.service;

import org.example.classAssignment.mapper.AccountMapper;
import org.example.classAssignment.mapper.MaterialMapper;
import org.example.classAssignment.mapper.PrepareSpaceContentMapper;
import org.example.classAssignment.mapper.PrepareSpaceMapper;
import org.example.classAssignment.mapper.TopicMapper;
import org.example.classAssignment.pojo.Assignment;
import org.example.classAssignment.pojo.Courseware;
import org.example.classAssignment.pojo.CreateFolderRequest;
import org.example.classAssignment.pojo.CreateCoursewareRequest;
import org.example.classAssignment.pojo.CreateLinkRequest;
import org.example.classAssignment.pojo.CourseAndAccount;
import org.example.classAssignment.pojo.MaterialAttachment;
import org.example.classAssignment.pojo.MaterialCategory;
import org.example.classAssignment.pojo.MaterialFolder;
import org.example.classAssignment.pojo.MaterialLink;
import org.example.classAssignment.pojo.MaterialResult;
import org.example.classAssignment.pojo.MoveFolderRequest;
import org.example.classAssignment.pojo.PrepareSpace;
import org.example.classAssignment.pojo.PrepareSpaceAssignmentImportRequest;
import org.example.classAssignment.pojo.PrepareSpaceMember;
import org.example.classAssignment.pojo.PrepareSpaceOperationLog;
import org.example.classAssignment.pojo.PrepareSpaceMaterialImportRequest;
import org.example.classAssignment.pojo.PrepareSpaceTopicImportRequest;
import org.example.classAssignment.pojo.Reply;
import org.example.classAssignment.pojo.Result;
import org.example.classAssignment.pojo.Topic;
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
public class PrepareSpaceContentService {
    private static final String MODULE_MATERIAL = "MATERIAL";
    private static final String MODULE_ASSIGNMENT = "ASSIGNMENT";
    private static final String MODULE_TOPIC = "TOPIC";
    private static final String MODULE_COURSEWARE = "COURSEWARE";

    @Autowired
    private PrepareSpaceMapper prepareSpaceMapper;

    @Autowired
    private PrepareSpaceContentMapper prepareSpaceContentMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Value("${prepare-space.storage-path:uploads/prepare-spaces}")
    private String storagePath;

    @Value("${materials.storage-path:uploads/materials}")
    private String materialsStoragePath;

    public MaterialResult listFolders(Long spaceId, String accountId, String category) {
        String normalizedCategory = MaterialCategory.normalize(category);
        return listFoldersInternal(spaceId, accountId, MODULE_MATERIAL, normalizedCategory, "资料文件夹");
    }

    public MaterialResult createFolder(Long spaceId, CreateFolderRequest request) {
        String normalizedCategory = MaterialCategory.normalize(request.getCategory());
        return createFolderInternal(spaceId, MODULE_MATERIAL, request, normalizedCategory, "资料文件夹");
    }

    public MaterialResult moveFolder(Long spaceId, Long folderId, MoveFolderRequest request) {
        return moveFolderInternal(spaceId, MODULE_MATERIAL, folderId, request, "资料文件夹");
    }

    public MaterialResult deleteFolder(Long spaceId, Long folderId, String accountId) {
        return deleteFolderInternal(spaceId, MODULE_MATERIAL, folderId, accountId, "资料文件夹");
    }

    public MaterialResult listCoursewareFolders(Long spaceId, String accountId) {
        return listFoldersInternal(spaceId, accountId, MODULE_COURSEWARE, null, "互动课件文件夹");
    }

    public MaterialResult createCoursewareFolder(Long spaceId, CreateFolderRequest request) {
        return createFolderInternal(spaceId, MODULE_COURSEWARE, request, null, "互动课件文件夹");
    }

    public MaterialResult moveCoursewareFolder(Long spaceId, Long folderId, MoveFolderRequest request) {
        return moveFolderInternal(spaceId, MODULE_COURSEWARE, folderId, request, "互动课件文件夹");
    }

    public MaterialResult deleteCoursewareFolder(Long spaceId, Long folderId, String accountId) {
        return deleteFolderInternal(spaceId, MODULE_COURSEWARE, folderId, accountId, "互动课件文件夹");
    }

    public Result listCoursewares(Long spaceId, String accountId, Long folderId, String keyword) {
        Result result = new Result();
        try {
            assertSpaceVisible(spaceId, accountId);
            List<Courseware> coursewares = prepareSpaceContentMapper.findCoursewares(spaceId, accountId, folderId, keyword);
            result.setCoursewares(coursewares);
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取互动课件失败：" + e.getMessage());
        }
        return result;
    }

    public Result createCourseware(Long spaceId, CreateCoursewareRequest request) {
        Result result = new Result();
        try {
            assertSpaceWritable(spaceId, request.getAccountId());
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("标题不能为空");
            }
            if (request.getItemType() == null || request.getItemType().trim().isEmpty()) {
                throw new IllegalArgumentException("类型不能为空");
            }
            String visibility = request.getVisibility() == null || request.getVisibility().isBlank()
                    ? "ALL_MEMBERS"
                    : request.getVisibility().trim();
            if (!"ALL_MEMBERS".equals(visibility) && !"ONLY_ME".equals(visibility)) {
                throw new IllegalArgumentException("visibility 不合法，仅支持 ALL_MEMBERS/ONLY_ME");
            }
            if (request.getUrl() != null && !request.getUrl().isBlank() && !isValidUrl(request.getUrl().trim())) {
                throw new IllegalArgumentException("链接格式不正确");
            }
            if (request.getFolderId() != null) {
                MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, MODULE_COURSEWARE, request.getFolderId());
                if (folder == null) {
                    throw new IllegalArgumentException("文件夹不存在");
                }
            }

            Courseware courseware = new Courseware();
            courseware.setFolderId(request.getFolderId());
            courseware.setTitle(request.getTitle().trim());
            courseware.setItemType(request.getItemType().trim());
            courseware.setVisibility(visibility);
            courseware.setUrl(request.getUrl() == null ? null : request.getUrl().trim());
            courseware.setCreatedBy(request.getAccountId());
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertCourseware(spaceId, request.getAccountId(), courseware))) {
                throw new IllegalStateException("创建失败");
            }
            insertLog(spaceId, request.getAccountId(), "创建", "互动课件", String.valueOf(courseware.getCoursewareId()), "创建互动课件：" + courseware.getTitle());
            result.setCourseware(courseware);
            result.setSuccess(true);
            result.setMessage("创建成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("创建失败：" + e.getMessage());
        }
        return result;
    }

    public Result deleteCourseware(Long spaceId, Long coursewareId, String accountId) {
        Result result = new Result();
        try {
            assertSpaceWritable(spaceId, accountId);
            Courseware courseware = prepareSpaceContentMapper.findCoursewareById(spaceId, coursewareId);
            if (courseware == null) {
                throw new IllegalArgumentException("互动课件不存在");
            }
            prepareSpaceContentMapper.deleteCourseware(spaceId, coursewareId);
            insertLog(spaceId, accountId, "删除", "互动课件", String.valueOf(coursewareId), "删除互动课件：" + courseware.getTitle());
            result.setSuccess(true);
            result.setMessage("删除成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult listTopicFolders(Long spaceId, String accountId) {
        return listFoldersInternal(spaceId, accountId, MODULE_TOPIC, null, "话题文件夹");
    }

    public MaterialResult createTopicFolder(Long spaceId, CreateFolderRequest request) {
        return createFolderInternal(spaceId, MODULE_TOPIC, request, null, "话题文件夹");
    }

    public MaterialResult moveTopicFolder(Long spaceId, Long folderId, MoveFolderRequest request) {
        return moveFolderInternal(spaceId, MODULE_TOPIC, folderId, request, "话题文件夹");
    }

    public MaterialResult deleteTopicFolder(Long spaceId, Long folderId, String accountId) {
        return deleteFolderInternal(spaceId, MODULE_TOPIC, folderId, accountId, "话题文件夹");
    }

    public MaterialResult listAssignmentFolders(Long spaceId, String accountId) {
        return listFoldersInternal(spaceId, accountId, MODULE_ASSIGNMENT, null, "作业文件夹");
    }

    public MaterialResult createAssignmentFolder(Long spaceId, CreateFolderRequest request) {
        return createFolderInternal(spaceId, MODULE_ASSIGNMENT, request, null, "作业文件夹");
    }

    public MaterialResult moveAssignmentFolder(Long spaceId, Long folderId, MoveFolderRequest request) {
        return moveFolderInternal(spaceId, MODULE_ASSIGNMENT, folderId, request, "作业文件夹");
    }

    public MaterialResult deleteAssignmentFolder(Long spaceId, Long folderId, String accountId) {
        return deleteFolderInternal(spaceId, MODULE_ASSIGNMENT, folderId, accountId, "作业文件夹");
    }

    private MaterialResult listFoldersInternal(Long spaceId, String accountId, String module, String category, String targetName) {
        MaterialResult result = new MaterialResult();
        try {
            assertSpaceVisible(spaceId, accountId);
            List<MaterialFolder> folders = prepareSpaceContentMapper.findFolders(spaceId, module, category);
            result.setFolders(folders);
            result.setSuccess(true);
            result.setMessage(targetName + "获取成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage(targetName + "获取失败：" + e.getMessage());
        }
        return result;
    }

    private MaterialResult createFolderInternal(Long spaceId, String module, CreateFolderRequest request, String category, String targetName) {
        MaterialResult result = new MaterialResult();
        try {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("文件夹名称不能为空");
            }
            assertSpaceWritable(spaceId, request.getAccountId());
            Long parentId = request.getParentId();
            if (parentId != null) {
                MaterialFolder parent = prepareSpaceContentMapper.findFolderById(spaceId, module, parentId);
                if (parent == null) {
                    throw new IllegalArgumentException("父文件夹不存在");
                }
            }

            MaterialFolder folder = new MaterialFolder();
            folder.setCategory(category);
            folder.setParentId(parentId);
            folder.setName(request.getName().trim());
            folder.setCreatedBy(request.getAccountId());
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertFolder(spaceId, module, request.getAccountId(), folder))) {
                throw new IllegalStateException("创建文件夹失败");
            }
            insertLog(spaceId, request.getAccountId(), "创建", targetName, String.valueOf(folder.getFolderId()), "创建文件夹：" + folder.getName());
            result.setFolder(folder);
            result.setSuccess(true);
            result.setMessage("创建文件夹成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("创建文件夹失败：" + e.getMessage());
        }
        return result;
    }

    private MaterialResult moveFolderInternal(Long spaceId, String module, Long folderId, MoveFolderRequest request, String targetName) {
        MaterialResult result = new MaterialResult();
        try {
            assertSpaceWritable(spaceId, request.getAccountId());
            MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, module, folderId);
            if (folder == null) {
                throw new IllegalArgumentException("目标文件夹不存在");
            }
            Long newParentId = request.getNewParentId();
            if (newParentId != null) {
                if (Objects.equals(newParentId, folderId)) {
                    throw new IllegalArgumentException("不能移动到自身");
                }
                MaterialFolder newParent = prepareSpaceContentMapper.findFolderById(spaceId, module, newParentId);
                if (newParent == null) {
                    throw new IllegalArgumentException("新的父文件夹不存在");
                }
                List<MaterialFolder> all = prepareSpaceContentMapper.findFolders(spaceId, module, module.equals(MODULE_MATERIAL) ? folder.getCategory() : null);
                Set<Long> descendants = collectDescendantFolderIds(all, folderId);
                if (descendants.contains(newParentId)) {
                    throw new IllegalArgumentException("不能移动到子文件夹中");
                }
            }

            int updated = prepareSpaceContentMapper.updateFolderParent(spaceId, module, folderId, newParentId);
            if (updated <= 0) {
                throw new IllegalStateException("移动文件夹失败");
            }
            folder.setParentId(newParentId);
            insertLog(spaceId, request.getAccountId(), "移动", targetName, String.valueOf(folderId), "移动文件夹");
            result.setFolder(folder);
            result.setSuccess(true);
            result.setMessage("移动文件夹成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("移动文件夹失败：" + e.getMessage());
        }
        return result;
    }

    private MaterialResult deleteFolderInternal(Long spaceId, String module, Long folderId, String accountId, String targetName) {
        MaterialResult result = new MaterialResult();
        try {
            assertSpaceWritable(spaceId, accountId);
            MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, module, folderId);
            if (folder == null) {
                throw new IllegalArgumentException("文件夹不存在");
            }
            List<MaterialFolder> allFolders = prepareSpaceContentMapper.findFolders(spaceId, module, module.equals(MODULE_MATERIAL) ? folder.getCategory() : null);
            List<Long> deleteOrder = collectPostOrderFolderIds(allFolders, folderId);
            for (Long id : deleteOrder) {
                deleteFolderContent(spaceId, module, id, accountId);
                prepareSpaceContentMapper.deleteFolder(spaceId, module, id);
            }
            insertLog(spaceId, accountId, "删除", targetName, String.valueOf(folderId), "删除文件夹");
            result.setSuccess(true);
            result.setMessage("删除文件夹成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除文件夹失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult listAttachments(Long spaceId, String accountId, String category) {
        MaterialResult result = new MaterialResult();
        try {
            assertSpaceVisible(spaceId, accountId);
            String normalizedCategory = MaterialCategory.normalize(category);
            List<MaterialAttachment> attachments = prepareSpaceContentMapper.findMaterialAttachments(spaceId, normalizedCategory, null, null);
            result.setAttachments(attachments);
            result.setSuccess(true);
            result.setMessage("附件资料获取成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("附件资料获取失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult uploadAttachment(Long spaceId, String accountId, String category, Long folderId, MultipartFile file) {
        MaterialResult result = new MaterialResult();
        try {
            assertSpaceWritable(spaceId, accountId);
            String normalizedCategory = MaterialCategory.normalize(category);
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("文件不能为空");
            }
            if (folderId != null) {
                MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, MODULE_MATERIAL, folderId);
                if (folder == null) {
                    throw new IllegalArgumentException("文件夹不存在");
                }
                if (!normalizedCategory.equals(folder.getCategory())) {
                    throw new IllegalArgumentException("资料分区不匹配");
                }
            }

            String originalName = sanitizeFileName(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
            String storedName = UUID.randomUUID() + "_" + originalName;
            String relativePath = spaceId + "/materials/" + storedName;

            Path root = getStorageRoot();
            Path spaceDir = root.resolve(String.valueOf(spaceId)).resolve("materials").normalize();
            Files.createDirectories(spaceDir);

            Path target = spaceDir.resolve(storedName).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalStateException("非法文件路径");
            }
            file.transferTo(target);

            MaterialAttachment attachment = new MaterialAttachment();
            attachment.setCategory(normalizedCategory);
            attachment.setFolderId(folderId);
            attachment.setOriginalName(originalName);
            attachment.setStoredName(storedName);
            attachment.setRelativePath(relativePath);
            attachment.setSize(file.getSize());
            attachment.setContentType(file.getContentType());
            attachment.setCreatedBy(accountId);
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertMaterialAttachment(spaceId, accountId, attachment))) {
                Files.deleteIfExists(target);
                throw new IllegalStateException("保存附件记录失败");
            }

            insertLog(spaceId, accountId, "上传", "资料附件", String.valueOf(attachment.getAttachmentId()), "上传附件：" + attachment.getOriginalName());
            result.setAttachment(attachment);
            result.setSuccess(true);
            result.setMessage("上传附件成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("上传附件失败：" + e.getMessage());
        }
        return result;
    }

    public ResponseEntity<Resource> downloadAttachment(Long spaceId, Long attachmentId, String accountId) {
        assertSpaceVisible(spaceId, accountId);
        MaterialAttachment attachment = prepareSpaceContentMapper.findMaterialAttachmentById(spaceId, attachmentId);
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

    public MaterialResult deleteAttachment(Long spaceId, Long attachmentId, String accountId) {
        MaterialResult result = new MaterialResult();
        try {
            assertSpaceWritable(spaceId, accountId);
            MaterialAttachment attachment = prepareSpaceContentMapper.findMaterialAttachmentById(spaceId, attachmentId);
            if (attachment == null) {
                throw new IllegalArgumentException("附件不存在");
            }
            deletePhysicalFile(attachment.getRelativePath());
            prepareSpaceContentMapper.deleteMaterialAttachment(spaceId, attachmentId);
            insertLog(spaceId, accountId, "删除", "资料附件", String.valueOf(attachmentId), "删除附件：" + attachment.getOriginalName());
            result.setSuccess(true);
            result.setMessage("删除附件成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除附件失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult listLinks(Long spaceId, String accountId, String category) {
        MaterialResult result = new MaterialResult();
        try {
            assertSpaceVisible(spaceId, accountId);
            String normalizedCategory = MaterialCategory.normalize(category);
            List<MaterialLink> links = prepareSpaceContentMapper.findMaterialLinks(spaceId, normalizedCategory, null, null);
            result.setLinks(links);
            result.setSuccess(true);
            result.setMessage("外链资料获取成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("外链资料获取失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult createLink(Long spaceId, CreateLinkRequest request) {
        MaterialResult result = new MaterialResult();
        try {
            assertSpaceWritable(spaceId, request.getAccountId());
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
                MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, MODULE_MATERIAL, request.getFolderId());
                if (folder == null) {
                    throw new IllegalArgumentException("文件夹不存在");
                }
                if (!normalizedCategory.equals(folder.getCategory())) {
                    throw new IllegalArgumentException("资料分区不匹配");
                }
            }

            MaterialLink link = new MaterialLink();
            link.setCategory(normalizedCategory);
            link.setFolderId(request.getFolderId());
            link.setTitle(request.getTitle().trim());
            link.setUrl(request.getUrl().trim());
            link.setCreatedBy(request.getAccountId());
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertMaterialLink(spaceId, request.getAccountId(), link))) {
                throw new IllegalStateException("创建外链失败");
            }

            insertLog(spaceId, request.getAccountId(), "创建", "资料外链", String.valueOf(link.getLinkId()), "创建外链：" + link.getTitle());
            result.setLink(link);
            result.setSuccess(true);
            result.setMessage("创建外链成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("创建外链失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult deleteLink(Long spaceId, Long linkId, String accountId) {
        MaterialResult result = new MaterialResult();
        try {
            assertSpaceWritable(spaceId, accountId);
            MaterialLink link = prepareSpaceContentMapper.findMaterialLinkById(spaceId, linkId);
            if (link == null) {
                throw new IllegalArgumentException("外链不存在");
            }
            prepareSpaceContentMapper.deleteMaterialLink(spaceId, linkId);
            insertLog(spaceId, accountId, "删除", "资料外链", String.valueOf(linkId), "删除外链：" + link.getTitle());
            result.setSuccess(true);
            result.setMessage("删除外链成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除外链失败：" + e.getMessage());
        }
        return result;
    }

    public MaterialResult importMaterialsFromCourse(Long spaceId, PrepareSpaceMaterialImportRequest request) {
        MaterialResult result = new MaterialResult();
        try {
            if (request.getAccountId() == null || request.getAccountId().isBlank()) {
                throw new IllegalArgumentException("accountId 不能为空");
            }
            PrepareSpace space = assertSpaceWritable(spaceId, request.getAccountId());
            if (space.getCourseId() == null || space.getCourseId().isBlank()) {
                throw new IllegalArgumentException("备课区未关联课程");
            }
            String courseId = space.getCourseId();
            String normalizedCategory = MaterialCategory.normalize(request.getCategory());
            String mode = request.getMode() == null || request.getMode().isBlank() ? "IMPORT" : request.getMode().trim().toUpperCase();
            if (!"IMPORT".equals(mode)) {
                throw new IllegalArgumentException("暂不支持 MOVE");
            }
            if (request.getItemType() == null || request.getItemType().isBlank()) {
                throw new IllegalArgumentException("itemType 不能为空");
            }
            if (request.getItemId() == null) {
                throw new IllegalArgumentException("itemId 不能为空");
            }
            String itemType = request.getItemType().trim().toUpperCase();
            if (request.getTargetFolderId() != null) {
                MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, MODULE_MATERIAL, request.getTargetFolderId());
                if (folder == null) {
                    throw new IllegalArgumentException("目标文件夹不存在");
                }
                if (!normalizedCategory.equals(folder.getCategory())) {
                    throw new IllegalArgumentException("资料分区不匹配");
                }
            }
            if (request.getTargetParentFolderId() != null) {
                MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, MODULE_MATERIAL, request.getTargetParentFolderId());
                if (folder == null) {
                    throw new IllegalArgumentException("目标父文件夹不存在");
                }
                if (!normalizedCategory.equals(folder.getCategory())) {
                    throw new IllegalArgumentException("资料分区不匹配");
                }
            }

            if ("ATTACHMENT".equals(itemType)) {
                MaterialAttachment imported = importCourseAttachment(spaceId, courseId, normalizedCategory, request.getItemId(), request.getTargetFolderId(), request.getAccountId());
                result.setAttachment(imported);
                result.setSuccess(true);
                result.setMessage("导入附件成功");
                return result;
            }
            if ("LINK".equals(itemType)) {
                MaterialLink imported = importCourseLink(spaceId, courseId, normalizedCategory, request.getItemId(), request.getTargetFolderId(), request.getAccountId());
                result.setLink(imported);
                result.setSuccess(true);
                result.setMessage("导入外链成功");
                return result;
            }
            if ("FOLDER".equals(itemType)) {
                importCourseFolder(spaceId, courseId, normalizedCategory, request.getItemId(), request.getTargetParentFolderId(), request.getAccountId());
                result.setSuccess(true);
                result.setMessage("导入文件夹成功");
                return result;
            }
            throw new IllegalArgumentException("itemType 不合法，仅支持 FOLDER/ATTACHMENT/LINK");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("导入失败：" + e.getMessage());
        }
        return result;
    }

    public Result importAssignmentFromCourse(Long spaceId, PrepareSpaceAssignmentImportRequest request) {
        Result result = new Result();
        try {
            if (request.getAccountId() == null || request.getAccountId().isBlank()) {
                throw new IllegalArgumentException("accountId 不能为空");
            }
            PrepareSpace space = assertSpaceWritable(spaceId, request.getAccountId());
            if (space.getCourseId() == null || space.getCourseId().isBlank()) {
                throw new IllegalArgumentException("备课区未关联课程");
            }
            if (request.getAssignmentId() == null || request.getAssignmentId().isBlank()) {
                throw new IllegalArgumentException("assignmentId 不能为空");
            }
            if (request.getTargetFolderId() != null) {
                MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, MODULE_ASSIGNMENT, request.getTargetFolderId());
                if (folder == null) {
                    throw new IllegalArgumentException("目标文件夹不存在");
                }
            }
            String courseId = space.getCourseId();
            List<Assignment> courseAssignments = accountMapper.findCourseAssignments(courseId);
            Assignment source = null;
            for (Assignment a : courseAssignments) {
                if (request.getAssignmentId().equals(a.getAssignmentId())) {
                    source = a;
                    break;
                }
            }
            if (source == null) {
                throw new IllegalArgumentException("课程作业不存在");
            }
            String newAssignmentId = UUID.randomUUID().toString().replace("-", "");
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertAssignment(newAssignmentId, spaceId, request.getTargetFolderId(), source.getTitle(), source.getDeadline(), source.getContent(), source.getTotalScore(), request.getAccountId()))) {
                throw new IllegalStateException("导入失败");
            }
            prepareSpaceContentMapper.insertImportLog(spaceId, MODULE_ASSIGNMENT, "COURSE", courseId, "COURSE_ASSIGNMENT", request.getAssignmentId(), newAssignmentId, "IMPORT", request.getAccountId());
            insertLog(spaceId, request.getAccountId(), "导入", "作业", newAssignmentId, "从课程导入作业：" + source.getTitle());
            result.setSuccess(true);
            result.setMessage("导入作业成功");
            result.setAssignment(prepareSpaceContentMapper.findAssignmentById(spaceId, newAssignmentId));
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("导入作业失败：" + e.getMessage());
        }
        return result;
    }

    public Result importTopicFromCourse(Long spaceId, PrepareSpaceTopicImportRequest request) {
        Result result = new Result();
        try {
            if (request.getAccountId() == null || request.getAccountId().isBlank()) {
                throw new IllegalArgumentException("accountId 不能为空");
            }
            PrepareSpace space = assertSpaceWritable(spaceId, request.getAccountId());
            if (space.getCourseId() == null || space.getCourseId().isBlank()) {
                throw new IllegalArgumentException("备课区未关联课程");
            }
            if (request.getTopicId() == null || request.getTopicId().isBlank()) {
                throw new IllegalArgumentException("topicId 不能为空");
            }
            if (request.getTargetFolderId() != null) {
                MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, MODULE_TOPIC, request.getTargetFolderId());
                if (folder == null) {
                    throw new IllegalArgumentException("目标文件夹不存在");
                }
            }
            String courseId = space.getCourseId();
            Topic source = topicMapper.findByTopicId(request.getTopicId());
            if (source == null) {
                throw new IllegalArgumentException("课程话题不存在");
            }
            if (source.getCourseId() != null && !courseId.equals(source.getCourseId())) {
                throw new IllegalArgumentException("课程话题不属于该备课区关联课程");
            }
            boolean includeReplies = Boolean.TRUE.equals(request.getIncludeReplies());
            List<Reply> replies = includeReplies ? topicMapper.findRepliesByTopicId(request.getTopicId()) : List.of();
            String newTopicId = UUID.randomUUID().toString().replace("-", "");
            int replyCount = includeReplies ? replies.size() : 0;
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertTopic(newTopicId, spaceId, request.getTargetFolderId(), source.getAuthorId(), source.getAuthorName(), source.getTitle(), source.getContent(), source.getIsAnonymous(), source.getIsPinned(), source.getIsLocked(), replyCount))) {
                throw new IllegalStateException("导入失败");
            }
            if (includeReplies) {
                for (Reply r : replies) {
                    String newReplyId = UUID.randomUUID().toString().replace("-", "");
                    prepareSpaceContentMapper.insertReply(newReplyId, spaceId, newTopicId, r.getAuthorId(), r.getAuthorName(), r.getContent(), r.getIsAnonymous());
                }
            }
            prepareSpaceContentMapper.insertImportLog(spaceId, MODULE_TOPIC, "COURSE", courseId, "COURSE_TOPIC", request.getTopicId(), newTopicId, "IMPORT", request.getAccountId());
            insertLog(spaceId, request.getAccountId(), "导入", "话题", newTopicId, "从课程导入话题：" + source.getTitle());
            result.setSuccess(true);
            result.setMessage("导入话题成功");
            result.setTopic(prepareSpaceContentMapper.findTopicById(spaceId, newTopicId));
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("导入话题失败：" + e.getMessage());
        }
        return result;
    }

    private PrepareSpace assertSpaceVisible(Long spaceId, String accountId) {
        if (spaceId == null) {
            throw new IllegalArgumentException("备课区ID不能为空");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("账号不能为空");
        }
        PrepareSpace space = prepareSpaceMapper.findPrepareSpaceById(spaceId);
        if (space == null || !"正常".equals(space.getStatus())) {
            throw new IllegalArgumentException("备课区不存在");
        }
        if (accountId.equals(space.getOwnerId())) {
            return space;
        }
        PrepareSpaceMember member = prepareSpaceMapper.findMemberByAccountId(spaceId, accountId);
        if (member == null) {
            throw new IllegalArgumentException("无权限访问该备课区");
        }
        return space;
    }

    private PrepareSpace assertSpaceWritable(Long spaceId, String accountId) {
        PrepareSpace space = assertSpaceVisible(spaceId, accountId);
        if (accountId.equals(space.getOwnerId())) {
            return space;
        }
        PrepareSpaceMember member = prepareSpaceMapper.findMemberByAccountId(spaceId, accountId);
        if (member == null) {
            throw new IllegalArgumentException("无权限访问该备课区");
        }
        String role = member.getRole();
        if (!"owner".equals(role) && !"admin".equals(role) && !"editor".equals(role)) {
            throw new IllegalArgumentException("无权限：仅 owner/admin/editor 可操作");
        }
        return space;
    }

    public Result listTopics(Long spaceId, String accountId) {
        Result result = new Result();
        try {
            assertSpaceVisible(spaceId, accountId);
            List<Topic> topics = prepareSpaceContentMapper.findTopics(spaceId);
            result.setSuccess(true);
            result.setTopics(topics);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取话题列表失败: " + e.getMessage());
        }
        return result;
    }

    public Result getTopicDetail(Long spaceId, String accountId, String topicId) {
        Result result = new Result();
        try {
            assertSpaceVisible(spaceId, accountId);
            Topic topic = prepareSpaceContentMapper.findTopicById(spaceId, topicId);
            if (topic == null) {
                result.setSuccess(false);
                result.setMessage("话题不存在");
                return result;
            }
            List<Reply> replies = prepareSpaceContentMapper.findReplies(spaceId, topicId);
            result.setSuccess(true);
            result.setTopic(topic);
            result.setReplies(replies);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取话题详情失败: " + e.getMessage());
        }
        return result;
    }

    public Result createTopic(Long spaceId, Map<String, Object> map) {
        Result result = new Result();
        try {
            String authorId = requiredString(map.get("authorId"), "authorId");
            PrepareSpace space = assertSpaceWritable(spaceId, authorId);
            String authorName = requiredString(map.get("authorName"), "authorName");
            String title = requiredString(map.get("title"), "title");
            String content = requiredString(map.get("content"), "content");
            Boolean isAnonymous = map.get("isAnonymous") != null && Boolean.parseBoolean(map.get("isAnonymous").toString());
            Long folderId = map.get("folderId") == null ? null : Long.parseLong(map.get("folderId").toString());
            if (folderId != null) {
                MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, MODULE_TOPIC, folderId);
                if (folder == null) {
                    throw new IllegalArgumentException("文件夹不存在");
                }
            }

            String topicId = UUID.randomUUID().toString().replace("-", "");
            String displayName = isAnonymous ? "匿名用户" : authorName;
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertTopic(topicId, spaceId, folderId, authorId, displayName, title, content, isAnonymous, false, false, 0))) {
                throw new IllegalStateException("话题创建失败");
            }
            insertLog(space.getPrepareSpaceId(), authorId, "创建", "话题", topicId, "创建话题：" + title);
            Topic topic = prepareSpaceContentMapper.findTopicById(spaceId, topicId);
            result.setSuccess(true);
            result.setMessage("话题创建成功");
            result.setTopic(topic);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("话题创建失败: " + e.getMessage());
        }
        return result;
    }

    public Result updateTopic(Long spaceId, Map<String, Object> map) {
        Result result = new Result();
        try {
            String topicId = requiredString(map.get("topicId"), "topicId");
            String authorId = requiredString(map.get("authorId"), "authorId");
            assertSpaceWritable(spaceId, authorId);
            Topic topic = prepareSpaceContentMapper.findTopicById(spaceId, topicId);
            if (topic == null) {
                result.setSuccess(false);
                result.setMessage("话题不存在");
                return result;
            }
            boolean canEdit = authorId.equals(topic.getAuthorId()) || isAdminOrOwner(spaceId, authorId);
            if (!canEdit) {
                result.setSuccess(false);
                result.setMessage("无权修改此话题");
                return result;
            }
            String title = requiredString(map.get("title"), "title");
            String content = requiredString(map.get("content"), "content");
            if (prepareSpaceContentMapper.updateTopic(spaceId, topicId, title, content) <= 0) {
                throw new IllegalStateException("话题修改失败");
            }
            insertLog(spaceId, authorId, "编辑", "话题", topicId, "修改话题");
            result.setSuccess(true);
            result.setMessage("话题修改成功");
            result.setTopic(prepareSpaceContentMapper.findTopicById(spaceId, topicId));
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("话题修改失败: " + e.getMessage());
        }
        return result;
    }

    public Result deleteTopic(Long spaceId, Map<String, Object> map) {
        Result result = new Result();
        try {
            String topicId = requiredString(map.get("topicId"), "topicId");
            String operatorId = requiredString(map.get("authorId"), "authorId");
            assertSpaceWritable(spaceId, operatorId);
            Topic topic = prepareSpaceContentMapper.findTopicById(spaceId, topicId);
            if (topic == null) {
                result.setSuccess(false);
                result.setMessage("话题不存在");
                return result;
            }
            boolean canDelete = operatorId.equals(topic.getAuthorId()) || isAdminOrOwner(spaceId, operatorId);
            if (!canDelete) {
                result.setSuccess(false);
                result.setMessage("无权删除此话题");
                return result;
            }
            List<Reply> replies = prepareSpaceContentMapper.findReplies(spaceId, topicId);
            for (Reply reply : replies) {
                prepareSpaceContentMapper.deleteReply(spaceId, reply.getReplyId());
            }
            prepareSpaceContentMapper.deleteTopic(spaceId, topicId);
            insertLog(spaceId, operatorId, "删除", "话题", topicId, "删除话题");
            result.setSuccess(true);
            result.setMessage("话题删除成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("话题删除失败: " + e.getMessage());
        }
        return result;
    }

    public Result pinTopic(Long spaceId, Map<String, Object> map) {
        Result result = new Result();
        try {
            String topicId = requiredString(map.get("topicId"), "topicId");
            String accountId = requiredString(map.get("accountId"), "accountId");
            assertSpaceManageable(spaceId, accountId);
            Boolean isPinned = map.get("isPinned") != null && Boolean.parseBoolean(map.get("isPinned").toString());
            if (prepareSpaceContentMapper.updateTopicPin(spaceId, topicId, isPinned) <= 0) {
                throw new IllegalStateException("操作失败");
            }
            insertLog(spaceId, accountId, "置顶", "话题", topicId, isPinned ? "置顶话题" : "取消置顶");
            result.setSuccess(true);
            result.setMessage(isPinned ? "话题置顶成功" : "取消置顶成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("操作失败: " + e.getMessage());
        }
        return result;
    }

    public Result lockTopic(Long spaceId, Map<String, Object> map) {
        Result result = new Result();
        try {
            String topicId = requiredString(map.get("topicId"), "topicId");
            String accountId = requiredString(map.get("accountId"), "accountId");
            assertSpaceManageable(spaceId, accountId);
            Boolean isLocked = map.get("isLocked") != null && Boolean.parseBoolean(map.get("isLocked").toString());
            if (prepareSpaceContentMapper.updateTopicLock(spaceId, topicId, isLocked) <= 0) {
                throw new IllegalStateException("操作失败");
            }
            insertLog(spaceId, accountId, "锁定", "话题", topicId, isLocked ? "锁定话题" : "解锁话题");
            result.setSuccess(true);
            result.setMessage(isLocked ? "话题已锁定" : "话题已解锁");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("操作失败: " + e.getMessage());
        }
        return result;
    }

    public Result addReply(Long spaceId, Map<String, Object> map) {
        Result result = new Result();
        try {
            String topicId = requiredString(map.get("topicId"), "topicId");
            String authorId = requiredString(map.get("authorId"), "authorId");
            assertSpaceVisible(spaceId, authorId);
            Topic topic = prepareSpaceContentMapper.findTopicById(spaceId, topicId);
            if (topic == null) {
                result.setSuccess(false);
                result.setMessage("话题不存在");
                return result;
            }
            if (Boolean.TRUE.equals(topic.getIsLocked())) {
                result.setSuccess(false);
                result.setMessage("话题已锁定，无法回复");
                return result;
            }
            String authorName = requiredString(map.get("authorName"), "authorName");
            String content = requiredString(map.get("content"), "content");
            Boolean isAnonymous = map.get("isAnonymous") != null && Boolean.parseBoolean(map.get("isAnonymous").toString());
            String displayName = isAnonymous ? "匿名用户" : authorName;
            String replyId = UUID.randomUUID().toString().replace("-", "");
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertReply(replyId, spaceId, topicId, authorId, displayName, content, isAnonymous))) {
                throw new IllegalStateException("回复失败");
            }
            prepareSpaceContentMapper.incrementReplyCount(spaceId, topicId);
            insertLog(spaceId, authorId, "回复", "话题", topicId, "回复话题");
            Reply reply = prepareSpaceContentMapper.findReplyById(spaceId, replyId);
            result.setSuccess(true);
            result.setMessage("回复成功");
            result.setReply(reply);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("回复失败: " + e.getMessage());
        }
        return result;
    }

    public Result deleteReply(Long spaceId, Map<String, Object> map) {
        Result result = new Result();
        try {
            String replyId = requiredString(map.get("replyId"), "replyId");
            String operatorId = requiredString(map.get("authorId"), "authorId");
            assertSpaceWritable(spaceId, operatorId);
            Reply reply = prepareSpaceContentMapper.findReplyById(spaceId, replyId);
            if (reply == null) {
                result.setSuccess(false);
                result.setMessage("回复不存在");
                return result;
            }
            boolean canDelete = operatorId.equals(reply.getAuthorId()) || isAdminOrOwner(spaceId, operatorId);
            if (!canDelete) {
                result.setSuccess(false);
                result.setMessage("无权删除此回复");
                return result;
            }
            prepareSpaceContentMapper.deleteReply(spaceId, replyId);
            prepareSpaceContentMapper.decrementReplyCount(spaceId, reply.getTopicId());
            insertLog(spaceId, operatorId, "删除", "回复", replyId, "删除回复");
            result.setSuccess(true);
            result.setMessage("回复删除成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("回复删除失败: " + e.getMessage());
        }
        return result;
    }

    public Result listAssignments(Long spaceId, String accountId) {
        Result result = new Result();
        try {
            assertSpaceVisible(spaceId, accountId);
            List<Assignment> assignments = prepareSpaceContentMapper.findAssignments(spaceId);
            result.setSuccess(true);
            result.setAssignments(assignments);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("作业展示失败：" + e.getMessage());
        }
        return result;
    }

    public Result createAssignment(Long spaceId, Map<String, Object> map) {
        Result result = new Result();
        try {
            String accountId = requiredString(map.get("accountId"), "accountId");
            assertSpaceWritable(spaceId, accountId);
            String title = requiredString(map.get("title"), "title");
            String deadline = map.get("deadline") == null ? null : map.get("deadline").toString();
            String content = map.get("content") == null ? "" : map.get("content").toString();
            Integer totalScore = map.get("totalScore") == null ? null : Integer.parseInt(map.get("totalScore").toString());
            Long folderId = map.get("folderId") == null ? null : Long.parseLong(map.get("folderId").toString());
            if (folderId != null) {
                MaterialFolder folder = prepareSpaceContentMapper.findFolderById(spaceId, MODULE_ASSIGNMENT, folderId);
                if (folder == null) {
                    throw new IllegalArgumentException("文件夹不存在");
                }
            }
            String assignmentId = UUID.randomUUID().toString().replace("-", "");
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertAssignment(assignmentId, spaceId, folderId, title, deadline, content, totalScore, accountId))) {
                throw new IllegalStateException("发布作业失败");
            }
            insertLog(spaceId, accountId, "创建", "作业", assignmentId, "创建作业：" + title);
            result.setSuccess(true);
            result.setMessage("作业创建成功");
            result.setAssignment(prepareSpaceContentMapper.findAssignmentById(spaceId, assignmentId));
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("作业创建失败：" + e.getMessage());
        }
        return result;
    }

    public Result deleteAssignment(Long spaceId, String accountId, String assignmentId) {
        Result result = new Result();
        try {
            assertSpaceWritable(spaceId, accountId);
            if (prepareSpaceContentMapper.deleteAssignment(spaceId, assignmentId) <= 0) {
                throw new IllegalStateException("删除作业失败");
            }
            insertLog(spaceId, accountId, "删除", "作业", assignmentId, "删除作业");
            result.setSuccess(true);
            result.setMessage("删除作业成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除作业失败：" + e.getMessage());
        }
        return result;
    }

    public Result submitAssignmentText(Long spaceId, CourseAndAccount request) {
        Result result = new Result();
        try {
            assertSpaceVisible(spaceId, request.getAccountId());
            String assignmentId = request.getAssignmentId();
            if (assignmentId == null || assignmentId.isBlank()) {
                throw new IllegalArgumentException("assignmentId 不能为空");
            }
            String submissionId = UUID.randomUUID().toString().replace("-", "");
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertAssignmentSubmission(submissionId, spaceId, assignmentId, request.getAccountId(),
                    request.getSubmitContent(), null, null, null, null))) {
                throw new IllegalStateException("提交失败");
            }
            insertLog(spaceId, request.getAccountId(), "提交", "作业", assignmentId, "提交作业");
            result.setSuccess(true);
            result.setMessage("作业提交成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("作业提交失败：" + e.getMessage());
        }
        return result;
    }

    public Result submitAssignmentWithFile(Long spaceId, String accountId, String assignmentId, String submitContent, MultipartFile file) {
        Result result = new Result();
        try {
            assertSpaceVisible(spaceId, accountId);
            if (assignmentId == null || assignmentId.isBlank()) {
                throw new IllegalArgumentException("assignmentId 不能为空");
            }
            String submissionId = UUID.randomUUID().toString().replace("-", "");
            String originalName = file == null ? null : sanitizeFileName(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
            String storedName = originalName == null ? null : UUID.randomUUID() + "_" + originalName;
            String relativePath = storedName == null ? null : spaceId + "/assignments/" + assignmentId + "/" + submissionId + "/" + storedName;

            if (file != null && !file.isEmpty()) {
                Path root = getStorageRoot();
                Path dir = root.resolve(String.valueOf(spaceId)).resolve("assignments").resolve(assignmentId).resolve(submissionId).normalize();
                Files.createDirectories(dir);
                Path target = dir.resolve(storedName).normalize();
                if (!target.startsWith(root)) {
                    throw new IllegalStateException("非法文件路径");
                }
                file.transferTo(target);
            }

            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertAssignmentSubmission(submissionId, spaceId, assignmentId, accountId,
                    submitContent, originalName, storedName, file == null ? null : file.getSize(), file == null ? null : file.getContentType()))) {
                throw new IllegalStateException("提交失败");
            }
            insertLog(spaceId, accountId, "提交", "作业", assignmentId, "提交作业");
            result.setSuccess(true);
            result.setMessage("作业提交成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("作业提交失败：" + e.getMessage());
        }
        return result;
    }

    public ResponseEntity<Resource> downloadSubmissionFile(Long spaceId, String accountId, String submissionId) {
        assertSpaceVisible(spaceId, accountId);
        Assignment submission = prepareSpaceContentMapper.findSubmissionById(spaceId, submissionId);
        if (submission == null || submission.getFileStoredName() == null || submission.getFileStoredName().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Path root = getStorageRoot();
        Path filePath = root.resolve(String.valueOf(spaceId)).resolve("assignments").normalize();
        Path found = null;
        try {
            if (Files.exists(filePath)) {
                found = Files.walk(filePath)
                        .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().equals(submission.getFileStoredName()))
                        .findFirst()
                        .orElse(null);
            }
        } catch (IOException ignored) {
        }
        if (found == null || !found.startsWith(root) || !Files.exists(found)) {
            return ResponseEntity.notFound().build();
        }

        String downloadName = encodeFileName(Objects.requireNonNullElse(submission.getFileName(), submission.getFileStoredName()));
        MediaType contentType = submission.getFileContentType() == null || submission.getFileContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(submission.getFileContentType());
        Resource resource = new FileSystemResource(found);
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + downloadName)
                .body(resource);
    }

    private void deleteFolderContent(Long spaceId, String module, Long folderId, String accountId) {
        if (MODULE_MATERIAL.equals(module)) {
            deleteMaterialFolderContent(spaceId, folderId, accountId);
            return;
        }
        if (MODULE_COURSEWARE.equals(module)) {
            List<Courseware> coursewares = prepareSpaceContentMapper.findCoursewaresByFolder(spaceId, folderId);
            for (Courseware courseware : coursewares) {
                insertLog(spaceId, accountId, "删除", "互动课件",
                        String.valueOf(courseware.getCoursewareId()), "删除互动课件：" + courseware.getTitle());
            }
            prepareSpaceContentMapper.deleteCoursewareByFolder(spaceId, folderId);
            return;
        }
        if (MODULE_TOPIC.equals(module)) {
            List<Topic> topics = prepareSpaceContentMapper.findTopicsByFolder(spaceId, folderId);
            for (Topic topic : topics) {
                insertLog(spaceId, accountId, "删除", "话题", topic.getTopicId(), "删除话题：" + topic.getTitle());
            }
            prepareSpaceContentMapper.deleteTopicsByFolder(spaceId, folderId);
            return;
        }
        if (MODULE_ASSIGNMENT.equals(module)) {
            List<Assignment> assignments = prepareSpaceContentMapper.findAssignmentsByFolder(spaceId, folderId);
            for (Assignment assignment : assignments) {
                if (assignment.getAssignmentId() != null) {
                    insertLog(spaceId, accountId, "删除", "作业", assignment.getAssignmentId(), "删除作业：" + assignment.getTitle());
                    prepareSpaceContentMapper.deleteAssignmentSubmissionsByAssignment(spaceId, assignment.getAssignmentId());
                    prepareSpaceContentMapper.deleteAssignment(spaceId, assignment.getAssignmentId());
                }
            }
        }
    }

    private void deleteMaterialFolderContent(Long spaceId, Long folderId, String accountId) {
        List<MaterialAttachment> attachments = prepareSpaceContentMapper.findMaterialAttachmentsByFolder(spaceId, folderId);
        for (MaterialAttachment attachment : attachments) {
            insertLog(spaceId, accountId, "删除", "资料附件",
                    String.valueOf(attachment.getAttachmentId()), "删除附件：" + attachment.getOriginalName());
            deletePhysicalFile(attachment.getRelativePath());
            prepareSpaceContentMapper.deleteMaterialAttachment(spaceId, attachment.getAttachmentId());
        }
        List<MaterialLink> links = prepareSpaceContentMapper.findMaterialLinksByFolder(spaceId, folderId);
        for (MaterialLink link : links) {
            insertLog(spaceId, accountId, "删除", "资料外链",
                    String.valueOf(link.getLinkId()), "删除外链：" + link.getTitle());
            prepareSpaceContentMapper.deleteMaterialLink(spaceId, link.getLinkId());
        }
    }

    private MaterialAttachment importCourseAttachment(Long spaceId,
                                                      String courseId,
                                                      String category,
                                                      Long sourceAttachmentId,
                                                      Long targetFolderId,
                                                      String operatorId) throws IOException {
        MaterialAttachment source = materialMapper.findAttachmentById(courseId, sourceAttachmentId);
        if (source == null) {
            throw new IllegalArgumentException("课程附件不存在");
        }
        if (source.getCategory() != null && !category.equals(source.getCategory())) {
            throw new IllegalArgumentException("资料分区不匹配");
        }

        Path courseRoot = getMaterialsStorageRoot();
        Path sourcePath = courseRoot.resolve(source.getRelativePath()).normalize();
        if (!sourcePath.startsWith(courseRoot) || !Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
            throw new IllegalArgumentException("课程附件文件不存在");
        }

        String safeOriginalName = sanitizeFileName(Objects.requireNonNullElse(source.getOriginalName(), "file"));
        String storedName = UUID.randomUUID() + "_" + safeOriginalName;
        String relativePath = spaceId + "/materials/" + storedName;

        Path root = getStorageRoot();
        Path spaceDir = root.resolve(String.valueOf(spaceId)).resolve("materials").normalize();
        Files.createDirectories(spaceDir);
        Path targetPath = spaceDir.resolve(storedName).normalize();
        if (!targetPath.startsWith(root)) {
            throw new IllegalStateException("非法文件路径");
        }
        Files.copy(sourcePath, targetPath);

        MaterialAttachment attachment = new MaterialAttachment();
        attachment.setCategory(category);
        attachment.setFolderId(targetFolderId);
        attachment.setOriginalName(safeOriginalName);
        attachment.setStoredName(storedName);
        attachment.setRelativePath(relativePath);
        attachment.setSize(source.getSize());
        attachment.setContentType(source.getContentType());
        attachment.setCreatedBy(operatorId);
        if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertMaterialAttachment(spaceId, operatorId, attachment))) {
            Files.deleteIfExists(targetPath);
            throw new IllegalStateException("保存附件记录失败");
        }

        prepareSpaceContentMapper.insertImportLog(spaceId, MODULE_MATERIAL, "COURSE", courseId, "COURSE_MATERIAL_ATTACHMENT",
                String.valueOf(sourceAttachmentId), String.valueOf(attachment.getAttachmentId()), "IMPORT", operatorId);
        insertLog(spaceId, operatorId, "导入", "资料附件", String.valueOf(attachment.getAttachmentId()), "从课程导入附件：" + safeOriginalName);
        return attachment;
    }

    private MaterialLink importCourseLink(Long spaceId,
                                          String courseId,
                                          String category,
                                          Long sourceLinkId,
                                          Long targetFolderId,
                                          String operatorId) {
        MaterialLink source = materialMapper.findLinkById(courseId, sourceLinkId);
        if (source == null) {
            throw new IllegalArgumentException("课程外链不存在");
        }
        if (source.getCategory() != null && !category.equals(source.getCategory())) {
            throw new IllegalArgumentException("资料分区不匹配");
        }

        MaterialLink link = new MaterialLink();
        link.setCategory(category);
        link.setFolderId(targetFolderId);
        link.setTitle(source.getTitle());
        link.setUrl(source.getUrl());
        link.setCreatedBy(operatorId);
        if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertMaterialLink(spaceId, operatorId, link))) {
            throw new IllegalStateException("保存外链记录失败");
        }
        prepareSpaceContentMapper.insertImportLog(spaceId, MODULE_MATERIAL, "COURSE", courseId, "COURSE_MATERIAL_LINK",
                String.valueOf(sourceLinkId), String.valueOf(link.getLinkId()), "IMPORT", operatorId);
        insertLog(spaceId, operatorId, "导入", "资料外链", String.valueOf(link.getLinkId()), "从课程导入外链：" + link.getTitle());
        return link;
    }

    private void importCourseFolder(Long spaceId,
                                    String courseId,
                                    String category,
                                    Long sourceFolderId,
                                    Long targetParentFolderId,
                                    String operatorId) throws IOException {
        MaterialFolder sourceRoot = materialMapper.findFolderById(courseId, sourceFolderId);
        if (sourceRoot == null) {
            throw new IllegalArgumentException("课程文件夹不存在");
        }
        if (sourceRoot.getCategory() != null && !category.equals(sourceRoot.getCategory())) {
            throw new IllegalArgumentException("资料分区不匹配");
        }

        List<MaterialFolder> allCourseFolders = materialMapper.findFoldersByCourseId(courseId, category);
        Set<Long> descendants = collectDescendantFolderIds(allCourseFolders, sourceFolderId);
        Set<Long> subtree = new HashSet<>(descendants);
        subtree.add(sourceFolderId);

        Map<Long, MaterialFolder> folderMap = new HashMap<>();
        for (MaterialFolder f : allCourseFolders) {
            if (f.getFolderId() != null) {
                folderMap.put(f.getFolderId(), f);
            }
        }
        Map<Long, List<Long>> childrenMap = buildChildrenMap(allCourseFolders);

        Map<Long, Long> idMapping = new HashMap<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(sourceFolderId);
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            if (!subtree.contains(currentId)) {
                continue;
            }
            MaterialFolder current = folderMap.get(currentId);
            if (current == null) {
                continue;
            }
            Long newParentId;
            if (Objects.equals(currentId, sourceFolderId)) {
                newParentId = targetParentFolderId;
            } else {
                newParentId = idMapping.get(current.getParentId());
            }
            MaterialFolder newFolder = new MaterialFolder();
            newFolder.setCategory(category);
            newFolder.setParentId(newParentId);
            newFolder.setName(current.getName());
            newFolder.setCreatedBy(operatorId);
            if (!Boolean.TRUE.equals(prepareSpaceContentMapper.insertFolder(spaceId, MODULE_MATERIAL, operatorId, newFolder))) {
                throw new IllegalStateException("导入文件夹失败");
            }
            idMapping.put(currentId, newFolder.getFolderId());
            prepareSpaceContentMapper.insertImportLog(spaceId, MODULE_MATERIAL, "COURSE", courseId, "COURSE_MATERIAL_FOLDER",
                    String.valueOf(currentId), String.valueOf(newFolder.getFolderId()), "IMPORT", operatorId);

            List<Long> children = childrenMap.get(currentId);
            if (children != null) {
                for (Long child : children) {
                    if (subtree.contains(child)) {
                        queue.add(child);
                    }
                }
            }
        }

        List<MaterialAttachment> courseAttachments = materialMapper.findAttachmentsByCourseId(courseId, category);
        for (MaterialAttachment att : courseAttachments) {
            Long fid = att.getFolderId();
            if (fid != null && subtree.contains(fid)) {
                importCourseAttachment(spaceId, courseId, category, att.getAttachmentId(), idMapping.get(fid), operatorId);
            }
        }
        List<MaterialLink> courseLinks = materialMapper.findLinksByCourseId(courseId, category);
        for (MaterialLink l : courseLinks) {
            Long fid = l.getFolderId();
            if (fid != null && subtree.contains(fid)) {
                importCourseLink(spaceId, courseId, category, l.getLinkId(), idMapping.get(fid), operatorId);
            }
        }
        insertLog(spaceId, operatorId, "导入", "资料文件夹", String.valueOf(sourceFolderId), "从课程导入文件夹：" + sourceRoot.getName());
    }

    private Path getMaterialsStorageRoot() {
        Path root = Paths.get(materialsStoragePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建存储目录：" + root);
        }
        return root;
    }

    private void insertLog(Long spaceId, String accountId, String type, String target, String targetId, String detail) {
        PrepareSpaceOperationLog log = new PrepareSpaceOperationLog();
        log.setPrepareSpaceId(spaceId);
        log.setAccountId(accountId);
        log.setOperationType(type);
        log.setOperationTarget(target);
        log.setTargetId(targetId);
        log.setDetail(detail);
        prepareSpaceMapper.insertLog(log);
    }

    private boolean isAdminOrOwner(Long spaceId, String accountId) {
        PrepareSpace space = prepareSpaceMapper.findPrepareSpaceById(spaceId);
        if (space != null && accountId.equals(space.getOwnerId())) {
            return true;
        }
        PrepareSpaceMember member = prepareSpaceMapper.findMemberByAccountId(spaceId, accountId);
        return member != null && ("admin".equals(member.getRole()) || "owner".equals(member.getRole()));
    }

    private void assertSpaceManageable(Long spaceId, String accountId) {
        PrepareSpace space = assertSpaceVisible(spaceId, accountId);
        if (accountId.equals(space.getOwnerId())) {
            return;
        }
        PrepareSpaceMember member = prepareSpaceMapper.findMemberByAccountId(spaceId, accountId);
        if (member == null || (!"admin".equals(member.getRole()) && !"owner".equals(member.getRole()))) {
            throw new IllegalArgumentException("无权限管理该备课区");
        }
    }

    private String requiredString(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        String str = value.toString();
        if (str.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return str;
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

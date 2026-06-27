package org.example.classAssignment.controller;

import org.example.classAssignment.pojo.CourseAndAccount;
import org.example.classAssignment.pojo.CreateCoursewareRequest;
import org.example.classAssignment.pojo.CreateFolderRequest;
import org.example.classAssignment.pojo.CreateLinkRequest;
import org.example.classAssignment.pojo.MaterialResult;
import org.example.classAssignment.pojo.MoveFolderRequest;
import org.example.classAssignment.pojo.PrepareSpaceAssignmentImportRequest;
import org.example.classAssignment.pojo.PrepareSpaceMaterialImportRequest;
import org.example.classAssignment.pojo.PrepareSpaceTopicImportRequest;
import org.example.classAssignment.pojo.Result;
import org.example.classAssignment.service.PrepareSpaceContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/prepare-spaces/{spaceId}")
public class PrepareSpaceContentController {
    @Autowired
    private PrepareSpaceContentService prepareSpaceContentService;

    @GetMapping("/materials/folders")
    public MaterialResult listMaterialFolders(@PathVariable Long spaceId,
                                              @RequestParam String accountId,
                                              @RequestParam String category) {
        return prepareSpaceContentService.listFolders(spaceId, accountId, category);
    }

    @PostMapping("/materials/folders")
    public MaterialResult createMaterialFolder(@PathVariable Long spaceId,
                                               @RequestBody CreateFolderRequest request) {
        return prepareSpaceContentService.createFolder(spaceId, request);
    }

    @PutMapping("/materials/folders/{folderId}/move")
    public MaterialResult moveMaterialFolder(@PathVariable Long spaceId,
                                             @PathVariable Long folderId,
                                             @RequestBody MoveFolderRequest request) {
        return prepareSpaceContentService.moveFolder(spaceId, folderId, request);
    }

    @DeleteMapping("/materials/folders/{folderId}")
    public MaterialResult deleteMaterialFolder(@PathVariable Long spaceId,
                                               @PathVariable Long folderId,
                                               @RequestParam String accountId) {
        return prepareSpaceContentService.deleteFolder(spaceId, folderId, accountId);
    }

    @GetMapping("/courseware/folders")
    public MaterialResult listCoursewareFolders(@PathVariable Long spaceId,
                                                @RequestParam String accountId) {
        return prepareSpaceContentService.listCoursewareFolders(spaceId, accountId);
    }

    @PostMapping("/courseware/folders")
    public MaterialResult createCoursewareFolder(@PathVariable Long spaceId,
                                                 @RequestBody CreateFolderRequest request) {
        return prepareSpaceContentService.createCoursewareFolder(spaceId, request);
    }

    @PutMapping("/courseware/folders/{folderId}/move")
    public MaterialResult moveCoursewareFolder(@PathVariable Long spaceId,
                                               @PathVariable Long folderId,
                                               @RequestBody MoveFolderRequest request) {
        return prepareSpaceContentService.moveCoursewareFolder(spaceId, folderId, request);
    }

    @DeleteMapping("/courseware/folders/{folderId}")
    public MaterialResult deleteCoursewareFolder(@PathVariable Long spaceId,
                                                 @PathVariable Long folderId,
                                                 @RequestParam String accountId) {
        return prepareSpaceContentService.deleteCoursewareFolder(spaceId, folderId, accountId);
    }

    @GetMapping("/courseware")
    public Result listCoursewares(@PathVariable Long spaceId,
                                  @RequestParam String accountId,
                                  @RequestParam(required = false) Long folderId,
                                  @RequestParam(required = false) String keyword) {
        return prepareSpaceContentService.listCoursewares(spaceId, accountId, folderId, keyword);
    }

    @PostMapping("/courseware")
    public Result createCourseware(@PathVariable Long spaceId,
                                   @RequestBody CreateCoursewareRequest request) {
        return prepareSpaceContentService.createCourseware(spaceId, request);
    }

    @DeleteMapping("/courseware/{coursewareId}")
    public Result deleteCourseware(@PathVariable Long spaceId,
                                   @PathVariable Long coursewareId,
                                   @RequestParam String accountId) {
        return prepareSpaceContentService.deleteCourseware(spaceId, coursewareId, accountId);
    }

    @GetMapping("/materials/attachments")
    public MaterialResult listMaterialAttachments(@PathVariable Long spaceId,
                                                  @RequestParam String accountId,
                                                  @RequestParam String category) {
        return prepareSpaceContentService.listAttachments(spaceId, accountId, category);
    }

    @PostMapping("/materials/attachments")
    public MaterialResult uploadMaterialAttachment(@PathVariable Long spaceId,
                                                   @RequestParam String accountId,
                                                   @RequestParam String category,
                                                   @RequestParam(required = false) Long folderId,
                                                   @RequestParam("file") MultipartFile file) {
        return prepareSpaceContentService.uploadAttachment(spaceId, accountId, category, folderId, file);
    }

    @GetMapping("/materials/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadMaterialAttachment(@PathVariable Long spaceId,
                                                               @PathVariable Long attachmentId,
                                                               @RequestParam String accountId) {
        return prepareSpaceContentService.downloadAttachment(spaceId, attachmentId, accountId);
    }

    @DeleteMapping("/materials/attachments/{attachmentId}")
    public MaterialResult deleteMaterialAttachment(@PathVariable Long spaceId,
                                                   @PathVariable Long attachmentId,
                                                   @RequestParam String accountId) {
        return prepareSpaceContentService.deleteAttachment(spaceId, attachmentId, accountId);
    }

    @GetMapping("/materials/links")
    public MaterialResult listMaterialLinks(@PathVariable Long spaceId,
                                            @RequestParam String accountId,
                                            @RequestParam String category) {
        return prepareSpaceContentService.listLinks(spaceId, accountId, category);
    }

    @PostMapping("/materials/links")
    public MaterialResult createMaterialLink(@PathVariable Long spaceId,
                                             @RequestBody CreateLinkRequest request) {
        return prepareSpaceContentService.createLink(spaceId, request);
    }

    @DeleteMapping("/materials/links/{linkId}")
    public MaterialResult deleteMaterialLink(@PathVariable Long spaceId,
                                             @PathVariable Long linkId,
                                             @RequestParam String accountId) {
        return prepareSpaceContentService.deleteLink(spaceId, linkId, accountId);
    }

    @PostMapping("/materials/import/from-course")
    public MaterialResult importMaterialsFromCourse(@PathVariable Long spaceId,
                                                    @RequestBody PrepareSpaceMaterialImportRequest request) {
        return prepareSpaceContentService.importMaterialsFromCourse(spaceId, request);
    }

    @GetMapping("/assignments")
    public Result listAssignments(@PathVariable Long spaceId, @RequestParam String accountId) {
        return prepareSpaceContentService.listAssignments(spaceId, accountId);
    }

    @GetMapping("/assignments/folders")
    public MaterialResult listAssignmentFolders(@PathVariable Long spaceId,
                                                @RequestParam String accountId) {
        return prepareSpaceContentService.listAssignmentFolders(spaceId, accountId);
    }

    @PostMapping("/assignments/folders")
    public MaterialResult createAssignmentFolder(@PathVariable Long spaceId,
                                                 @RequestBody CreateFolderRequest request) {
        return prepareSpaceContentService.createAssignmentFolder(spaceId, request);
    }

    @PutMapping("/assignments/folders/{folderId}/move")
    public MaterialResult moveAssignmentFolder(@PathVariable Long spaceId,
                                               @PathVariable Long folderId,
                                               @RequestBody MoveFolderRequest request) {
        return prepareSpaceContentService.moveAssignmentFolder(spaceId, folderId, request);
    }

    @DeleteMapping("/assignments/folders/{folderId}")
    public MaterialResult deleteAssignmentFolder(@PathVariable Long spaceId,
                                                 @PathVariable Long folderId,
                                                 @RequestParam String accountId) {
        return prepareSpaceContentService.deleteAssignmentFolder(spaceId, folderId, accountId);
    }

    @PostMapping("/assignments/submit")
    public Result submitAssignment(@PathVariable Long spaceId, @RequestBody CourseAndAccount request) {
        return prepareSpaceContentService.submitAssignmentText(spaceId, request);
    }

    @GetMapping("/topics")
    public Result listTopics(@PathVariable Long spaceId, @RequestParam String accountId) {
        return prepareSpaceContentService.listTopics(spaceId, accountId);
    }

    @GetMapping("/topics/folders")
    public MaterialResult listTopicFolders(@PathVariable Long spaceId,
                                           @RequestParam String accountId) {
        return prepareSpaceContentService.listTopicFolders(spaceId, accountId);
    }

    @PostMapping("/topics/folders")
    public MaterialResult createTopicFolder(@PathVariable Long spaceId,
                                            @RequestBody CreateFolderRequest request) {
        return prepareSpaceContentService.createTopicFolder(spaceId, request);
    }

    @PutMapping("/topics/folders/{folderId}/move")
    public MaterialResult moveTopicFolder(@PathVariable Long spaceId,
                                          @PathVariable Long folderId,
                                          @RequestBody MoveFolderRequest request) {
        return prepareSpaceContentService.moveTopicFolder(spaceId, folderId, request);
    }

    @DeleteMapping("/topics/folders/{folderId}")
    public MaterialResult deleteTopicFolder(@PathVariable Long spaceId,
                                            @PathVariable Long folderId,
                                            @RequestParam String accountId) {
        return prepareSpaceContentService.deleteTopicFolder(spaceId, folderId, accountId);
    }

    @PostMapping("/topics/create")
    public Result createTopic(@PathVariable Long spaceId, @RequestBody Map<String, Object> map) {
        return prepareSpaceContentService.createTopic(spaceId, map);
    }

    @PostMapping("/topics/detail")
    public Result getTopicDetail(@PathVariable Long spaceId, @RequestParam String accountId, @RequestBody Map<String, String> map) {
        return prepareSpaceContentService.getTopicDetail(spaceId, accountId, map.get("topicId"));
    }

    @PostMapping("/topics/update")
    public Result updateTopic(@PathVariable Long spaceId, @RequestBody Map<String, Object> map) {
        return prepareSpaceContentService.updateTopic(spaceId, map);
    }

    @PostMapping("/topics/delete")
    public Result deleteTopic(@PathVariable Long spaceId, @RequestBody Map<String, Object> map) {
        return prepareSpaceContentService.deleteTopic(spaceId, map);
    }

    @PostMapping("/topics/pin")
    public Result pinTopic(@PathVariable Long spaceId, @RequestBody Map<String, Object> map) {
        return prepareSpaceContentService.pinTopic(spaceId, map);
    }

    @PostMapping("/topics/lock")
    public Result lockTopic(@PathVariable Long spaceId, @RequestBody Map<String, Object> map) {
        return prepareSpaceContentService.lockTopic(spaceId, map);
    }

    @PostMapping("/topics/reply/add")
    public Result addReply(@PathVariable Long spaceId, @RequestBody Map<String, Object> map) {
        return prepareSpaceContentService.addReply(spaceId, map);
    }

    @PostMapping("/topics/reply/delete")
    public Result deleteReply(@PathVariable Long spaceId, @RequestBody Map<String, Object> map) {
        return prepareSpaceContentService.deleteReply(spaceId, map);
    }

    @PostMapping("/topics/import/from-course")
    public Result importTopicFromCourse(@PathVariable Long spaceId,
                                        @RequestBody PrepareSpaceTopicImportRequest request) {
        return prepareSpaceContentService.importTopicFromCourse(spaceId, request);
    }

    @PostMapping("/assignments/create")
    public Result createAssignment(@PathVariable Long spaceId, @RequestBody Map<String, Object> map) {
        map.putIfAbsent("spaceId", spaceId);
        return prepareSpaceContentService.createAssignment(spaceId, map);
    }

    @DeleteMapping("/assignments/{assignmentId}")
    public Result deleteAssignment(@PathVariable Long spaceId,
                                   @PathVariable String assignmentId,
                                   @RequestParam String accountId) {
        return prepareSpaceContentService.deleteAssignment(spaceId, accountId, assignmentId);
    }

    @PostMapping("/assignments/import/from-course")
    public Result importAssignmentFromCourse(@PathVariable Long spaceId,
                                             @RequestBody PrepareSpaceAssignmentImportRequest request) {
        return prepareSpaceContentService.importAssignmentFromCourse(spaceId, request);
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    public Result submitAssignmentWithFile(@PathVariable Long spaceId,
                                           @PathVariable String assignmentId,
                                           @RequestParam String accountId,
                                           @RequestParam(required = false) String submitContent,
                                           @RequestParam(required = false) MultipartFile file) {
        return prepareSpaceContentService.submitAssignmentWithFile(spaceId, accountId, assignmentId, submitContent, file);
    }

    @GetMapping("/assignments/submissions/{submissionId}/download")
    public ResponseEntity<Resource> downloadSubmissionFile(@PathVariable Long spaceId,
                                                           @PathVariable String submissionId,
                                                           @RequestParam String accountId) {
        return prepareSpaceContentService.downloadSubmissionFile(spaceId, accountId, submissionId);
    }
}

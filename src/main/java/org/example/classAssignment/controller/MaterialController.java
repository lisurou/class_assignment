package org.example.classAssignment.controller;

import org.example.classAssignment.pojo.CreateFolderRequest;
import org.example.classAssignment.pojo.CreateLinkRequest;
import org.example.classAssignment.pojo.MaterialResult;
import org.example.classAssignment.pojo.MoveFolderRequest;
import org.example.classAssignment.service.MaterialService;
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

@RestController
@RequestMapping("/courses/{courseId}/materials")
public class MaterialController {
    @Autowired
    private MaterialService materialService;

    @GetMapping("/folders")
    public MaterialResult listFolders(@PathVariable String courseId, @RequestParam String accountId, @RequestParam String category) {
        return materialService.listFolders(courseId, accountId, category);
    }

    @PostMapping("/folders")
    public MaterialResult createFolder(@PathVariable String courseId, @RequestBody CreateFolderRequest request) {
        return materialService.createFolder(courseId, request);
    }

    @PutMapping("/folders/{folderId}/move")
    public MaterialResult moveFolder(@PathVariable String courseId, @PathVariable Long folderId, @RequestBody MoveFolderRequest request) {
        return materialService.moveFolder(courseId, folderId, request);
    }

    @DeleteMapping("/folders/{folderId}")
    public MaterialResult deleteFolder(@PathVariable String courseId, @PathVariable Long folderId, @RequestParam String accountId) {
        return materialService.deleteFolder(courseId, folderId, accountId);
    }

    @GetMapping("/attachments")
    public MaterialResult listAttachments(@PathVariable String courseId, @RequestParam String accountId, @RequestParam String category) {
        return materialService.listAttachments(courseId, accountId, category);
    }

    @PostMapping("/attachments")
    public MaterialResult uploadAttachment(
            @PathVariable String courseId,
            @RequestParam String accountId,
            @RequestParam String category,
            @RequestParam(required = false) Long folderId,
            @RequestParam("file") MultipartFile file
    ) {
        return materialService.uploadAttachment(courseId, accountId, category, folderId, file);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable String courseId,
            @PathVariable Long attachmentId,
            @RequestParam String accountId
    ) {
        return materialService.downloadAttachment(courseId, attachmentId, accountId);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public MaterialResult deleteAttachment(
            @PathVariable String courseId,
            @PathVariable Long attachmentId,
            @RequestParam String accountId
    ) {
        return materialService.deleteAttachment(courseId, attachmentId, accountId);
    }

    @GetMapping("/links")
    public MaterialResult listLinks(@PathVariable String courseId, @RequestParam String accountId, @RequestParam String category) {
        return materialService.listLinks(courseId, accountId, category);
    }

    @PostMapping("/links")
    public MaterialResult createLink(@PathVariable String courseId, @RequestBody CreateLinkRequest request) {
        return materialService.createLink(courseId, request);
    }

    @DeleteMapping("/links/{linkId}")
    public MaterialResult deleteLink(@PathVariable String courseId, @PathVariable Long linkId, @RequestParam String accountId) {
        return materialService.deleteLink(courseId, linkId, accountId);
    }
}

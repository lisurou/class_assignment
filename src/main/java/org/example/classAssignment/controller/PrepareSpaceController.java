package org.example.classAssignment.controller;

import org.example.classAssignment.pojo.AddPrepareMemberRequest;
import org.example.classAssignment.pojo.BatchRemovePrepareMembersRequest;
import org.example.classAssignment.pojo.CreatePrepareSpaceRequest;
import org.example.classAssignment.pojo.PrepareSpaceResult;
import org.example.classAssignment.pojo.TransferPrepareSpaceOwnerRequest;
import org.example.classAssignment.pojo.UpdatePrepareMemberRequest;
import org.example.classAssignment.pojo.UpdatePrepareSpaceRequest;
import org.example.classAssignment.service.PrepareSpaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/prepare-spaces")
public class PrepareSpaceController {
    @Autowired
    private PrepareSpaceService prepareSpaceService;

    @PostMapping
    public PrepareSpaceResult createPrepareSpace(@RequestBody CreatePrepareSpaceRequest request) {
        return prepareSpaceService.createPrepareSpace(request);
    }

    @GetMapping
    public PrepareSpaceResult listPrepareSpaces(@RequestParam String accountId,
                                                @RequestParam(required = false) String type) {
        return prepareSpaceService.listPrepareSpaces(accountId, type);
    }

    @GetMapping("/{spaceId}")
    public PrepareSpaceResult getPrepareSpaceDetail(@PathVariable Long spaceId,
                                                    @RequestParam String accountId) {
        return prepareSpaceService.getPrepareSpaceDetail(spaceId, accountId);
    }

    @PutMapping("/{spaceId}")
    public PrepareSpaceResult updatePrepareSpace(@PathVariable Long spaceId,
                                                 @RequestBody UpdatePrepareSpaceRequest request) {
        return prepareSpaceService.updatePrepareSpace(spaceId, request);
    }

    @DeleteMapping("/{spaceId}")
    public PrepareSpaceResult deletePrepareSpace(@PathVariable Long spaceId,
                                                 @RequestParam String accountId) {
        return prepareSpaceService.deletePrepareSpace(spaceId, accountId);
    }

    @GetMapping("/{spaceId}/members")
    public PrepareSpaceResult listMembers(@PathVariable Long spaceId,
                                          @RequestParam String accountId,
                                          @RequestParam(required = false) String keyword) {
        return prepareSpaceService.listMembers(spaceId, accountId, keyword);
    }

    @PostMapping("/{spaceId}/members")
    public PrepareSpaceResult addMember(@PathVariable Long spaceId,
                                        @RequestBody AddPrepareMemberRequest request) {
        return prepareSpaceService.addMember(spaceId, request);
    }

    @PutMapping("/{spaceId}/members/{memberId}")
    public PrepareSpaceResult updateMember(@PathVariable Long spaceId,
                                           @PathVariable Long memberId,
                                           @RequestBody UpdatePrepareMemberRequest request) {
        return prepareSpaceService.updateMember(spaceId, memberId, request);
    }

    @PutMapping("/{spaceId}/members/transfer-owner")
    public PrepareSpaceResult transferOwner(@PathVariable Long spaceId,
                                            @RequestBody TransferPrepareSpaceOwnerRequest request) {
        return prepareSpaceService.transferOwner(spaceId, request);
    }

    @DeleteMapping("/{spaceId}/members/{memberId}")
    public PrepareSpaceResult removeMember(@PathVariable Long spaceId,
                                           @PathVariable Long memberId,
                                           @RequestParam String accountId) {
        return prepareSpaceService.removeMember(spaceId, memberId, accountId);
    }

    @PostMapping("/{spaceId}/members/batch-remove")
    public PrepareSpaceResult batchRemoveMembers(@PathVariable Long spaceId,
                                                 @RequestBody BatchRemovePrepareMembersRequest request) {
        return prepareSpaceService.batchRemoveMembers(spaceId, request);
    }

    @GetMapping("/{spaceId}/logs")
    public PrepareSpaceResult listLogs(@PathVariable Long spaceId,
                                       @RequestParam String accountId,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String keyword) {
        return prepareSpaceService.listLogs(spaceId, accountId, category, keyword);
    }
}

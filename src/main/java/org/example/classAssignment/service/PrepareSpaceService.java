package org.example.classAssignment.service;

import org.example.classAssignment.mapper.PrepareSpaceContentMapper;
import org.example.classAssignment.mapper.PrepareSpaceMapper;
import org.example.classAssignment.pojo.Account;
import org.example.classAssignment.pojo.AddPrepareMemberRequest;
import org.example.classAssignment.pojo.BatchRemovePrepareMembersRequest;
import org.example.classAssignment.pojo.CreatePrepareSpaceRequest;
import org.example.classAssignment.pojo.PrepareSpace;
import org.example.classAssignment.pojo.PrepareSpaceMember;
import org.example.classAssignment.pojo.PrepareSpaceOperationLog;
import org.example.classAssignment.pojo.PrepareSpaceResult;
import org.example.classAssignment.pojo.TransferPrepareSpaceOwnerRequest;
import org.example.classAssignment.pojo.UpdatePrepareMemberRequest;
import org.example.classAssignment.pojo.UpdatePrepareSpaceRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class PrepareSpaceService {
    @Autowired
    private PrepareSpaceMapper prepareSpaceMapper;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PrepareSpaceContentMapper prepareSpaceContentMapper;

    @Value("${prepare-space.storage-path:uploads/prepare-spaces}")
    private String storagePath;

    public PrepareSpaceResult createPrepareSpace(CreatePrepareSpaceRequest request) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            validateCreateRequest(request);
            String normalizedSpaceType = normalizeSpaceType(request.getSpaceType());
            assertTeacher(request.getOwnerId());

            PrepareSpace prepareSpace = new PrepareSpace();
            prepareSpace.setName(request.getName().trim());
            prepareSpace.setSpaceType(normalizedSpaceType);
            prepareSpace.setOwnerId(request.getOwnerId());
            prepareSpace.setCourseId(request.getCourseId().trim());
            prepareSpace.setDescription(trimToNull(request.getDescription()));
            prepareSpace.setStatus("正常");
            if (!Boolean.TRUE.equals(prepareSpaceMapper.insertPrepareSpace(prepareSpace))) {
                throw new IllegalStateException("创建备课区失败");
            }

            PrepareSpaceMember ownerMember = new PrepareSpaceMember();
            ownerMember.setPrepareSpaceId(prepareSpace.getPrepareSpaceId());
            ownerMember.setAccountId(request.getOwnerId());
            ownerMember.setRole("owner");
            ownerMember.setStatus("正常");
            prepareSpaceMapper.insertMember(ownerMember);

            insertLog(prepareSpace.getPrepareSpaceId(), request.getOwnerId(), "创建", "备课区",
                    String.valueOf(prepareSpace.getPrepareSpaceId()), "创建备课区：" + prepareSpace.getName());

            result.setPrepareSpace(prepareSpace);
            result.setSuccess(true);
            result.setMessage("创建备课区成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("创建备课区失败：" + e.getMessage());
        }
        return result;
    }

    public PrepareSpaceResult listPrepareSpaces(String accountId, String type) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            assertAccountExists(accountId);
            List<PrepareSpace> spaces = prepareSpaceMapper.findVisibleSpaces(accountId);
            if (type != null && !type.isBlank() && !"全部".equals(type.trim())) {
                String normalizedType = normalizeSpaceType(type);
                spaces = filterByType(spaces, normalizedType);
            } else {
                spaces = deduplicate(spaces);
            }
            result.setPrepareSpaces(spaces);
            result.setSuccess(true);
            result.setMessage("备课区列表获取成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("备课区列表获取失败：" + e.getMessage());
        }
        return result;
    }

    public PrepareSpaceResult getPrepareSpaceDetail(Long spaceId, String accountId) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            PrepareSpace prepareSpace = assertVisibleSpace(spaceId, accountId);
            result.setPrepareSpace(prepareSpace);
            result.setSuccess(true);
            result.setMessage("备课区详情获取成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("备课区详情获取失败：" + e.getMessage());
        }
        return result;
    }

    public PrepareSpaceResult updatePrepareSpace(Long spaceId, UpdatePrepareSpaceRequest request) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            if (request.getAccountId() == null || request.getAccountId().isBlank()) {
                throw new IllegalArgumentException("操作账号不能为空");
            }
            PrepareSpace prepareSpace = assertManageableSpace(spaceId, request.getAccountId());
            String name = request.getName() == null || request.getName().isBlank() ? prepareSpace.getName() : request.getName().trim();
            String description = request.getDescription() == null ? prepareSpace.getDescription() : trimToNull(request.getDescription());
            int updated = prepareSpaceMapper.updatePrepareSpace(spaceId, name, description);
            if (updated <= 0) {
                throw new IllegalStateException("更新备课区失败");
            }
            insertLog(spaceId, request.getAccountId(), "编辑", "备课区", String.valueOf(spaceId), "更新备课区信息");
            result.setPrepareSpace(prepareSpaceMapper.findPrepareSpaceById(spaceId));
            result.setSuccess(true);
            result.setMessage("更新备课区成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("更新备课区失败：" + e.getMessage());
        }
        return result;
    }

    @Transactional
    public PrepareSpaceResult deletePrepareSpace(Long spaceId, String accountId) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            PrepareSpace prepareSpace = assertOwnerSpace(spaceId, accountId);
            cleanupPrepareSpaceData(spaceId);
            prepareSpaceMapper.removeAllMembers(spaceId);
            insertLog(spaceId, accountId, "删除", "备课区", String.valueOf(spaceId), "删除备课区：" + prepareSpace.getName());
            int updated = prepareSpaceMapper.deletePrepareSpace(spaceId);
            if (updated <= 0) {
                throw new IllegalStateException("删除备课区失败");
            }
            deletePrepareSpaceFiles(spaceId);
            result.setSuccess(true);
            result.setMessage("删除备课区成功");
        } catch (Exception e) {
            if (TransactionAspectSupport.currentTransactionStatus() != null) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            result.setSuccess(false);
            result.setMessage("删除备课区失败：" + e.getMessage());
        }
        return result;
    }

    public PrepareSpaceResult listMembers(Long spaceId, String accountId, String keyword) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            PrepareSpace prepareSpace = assertVisibleSpace(spaceId, accountId);
            result.setPrepareSpace(prepareSpace);
            result.setMembers(loadMembers(spaceId, keyword));
            result.setSuccess(true);
            result.setMessage("备课区成员获取成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("备课区成员获取失败：" + e.getMessage());
        }
        return result;
    }

    public PrepareSpaceResult addMember(Long spaceId, AddPrepareMemberRequest request) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            assertManageableSpace(spaceId, request.getAccountId());
            if (request.getMemberAccountId() == null || request.getMemberAccountId().isBlank()) {
                throw new IllegalArgumentException("新增成员账号不能为空");
            }
            String role = normalizeRole(request.getRole(), false);
            Account memberAccount = assertAccountExists(request.getMemberAccountId());
            if (!"老师".equals(memberAccount.getIdentity())) {
                throw new IllegalArgumentException("备课区成员当前仅支持教师");
            }
            PrepareSpaceMember existing = prepareSpaceMapper.findMemberByAccountId(spaceId, request.getMemberAccountId());
            if (existing != null) {
                throw new IllegalArgumentException("该成员已在备课区中");
            }

            PrepareSpaceMember member = new PrepareSpaceMember();
            member.setPrepareSpaceId(spaceId);
            member.setAccountId(request.getMemberAccountId());
            member.setRole(role);
            member.setStatus("正常");
            if (!Boolean.TRUE.equals(prepareSpaceMapper.insertMember(member))) {
                throw new IllegalStateException("添加成员失败");
            }
            insertLog(spaceId, request.getAccountId(), "添加成员", "成员", request.getMemberAccountId(),
                    "添加成员：" + request.getMemberAccountId() + "，角色：" + role);
            result.setMembers(loadMembers(spaceId, null));
            result.setSuccess(true);
            result.setMessage("添加成员成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("添加成员失败：" + e.getMessage());
        }
        return result;
    }

    public PrepareSpaceResult updateMember(Long spaceId, Long memberId, UpdatePrepareMemberRequest request) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            assertManageableSpace(spaceId, request.getAccountId());
            PrepareSpaceMember member = prepareSpaceMapper.findMemberById(memberId);
            if (member == null || !Objects.equals(member.getPrepareSpaceId(), spaceId) || !"正常".equals(member.getStatus())) {
                throw new IllegalArgumentException("成员不存在");
            }
            if ("owner".equals(member.getRole())) {
                throw new IllegalArgumentException("创建者角色不允许修改");
            }
            String role = normalizeRole(request.getRole(), false);
            int updated = prepareSpaceMapper.updateMemberRole(spaceId, memberId, role);
            if (updated <= 0) {
                throw new IllegalStateException("更新成员角色失败");
            }
            insertLog(spaceId, request.getAccountId(), "修改成员", "成员", member.getAccountId(),
                    "更新成员角色为：" + role);
            result.setMembers(loadMembers(spaceId, null));
            result.setSuccess(true);
            result.setMessage("更新成员角色成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("更新成员角色失败：" + e.getMessage());
        }
        return result;
    }

    @Transactional
    public PrepareSpaceResult transferOwner(Long spaceId, TransferPrepareSpaceOwnerRequest request) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            if (request == null || request.getAccountId() == null || request.getAccountId().isBlank()) {
                throw new IllegalArgumentException("操作账号不能为空");
            }
            PrepareSpace prepareSpace = assertOwnerSpace(spaceId, request.getAccountId());
            if (request.getTargetMemberId() == null) {
                throw new IllegalArgumentException("目标成员ID不能为空");
            }
            PrepareSpaceMember targetMember = prepareSpaceMapper.findMemberById(request.getTargetMemberId());
            if (targetMember == null || !Objects.equals(targetMember.getPrepareSpaceId(), spaceId) || !"正常".equals(targetMember.getStatus())) {
                throw new IllegalArgumentException("目标成员不存在");
            }
            if ("owner".equals(targetMember.getRole())) {
                throw new IllegalArgumentException("目标成员已经是组长");
            }
            if (prepareSpace.getOwnerId().equals(targetMember.getAccountId())) {
                throw new IllegalArgumentException("不能转让给自己");
            }

            String previousOwnerRole = request.getPreviousOwnerRole() == null || request.getPreviousOwnerRole().isBlank()
                    ? "admin"
                    : normalizeRole(request.getPreviousOwnerRole(), false);
            PrepareSpaceMember ownerMember = prepareSpaceMapper.findMemberByAccountId(spaceId, prepareSpace.getOwnerId());
            if (ownerMember == null || !"正常".equals(ownerMember.getStatus())) {
                throw new IllegalStateException("当前组长成员记录不存在");
            }

            if (prepareSpaceMapper.updatePrepareSpaceOwner(spaceId, targetMember.getAccountId()) <= 0) {
                throw new IllegalStateException("更新备课区组长失败");
            }
            if (prepareSpaceMapper.updateMemberRoleByAccountId(spaceId, prepareSpace.getOwnerId(), previousOwnerRole) <= 0) {
                throw new IllegalStateException("更新原组长角色失败");
            }
            if (prepareSpaceMapper.updateMemberRole(spaceId, targetMember.getMemberId(), "owner") <= 0) {
                throw new IllegalStateException("更新新组长角色失败");
            }

            insertLog(spaceId, request.getAccountId(), "转让组长", "成员", targetMember.getAccountId(),
                    "将组长转让给：" + targetMember.getAccountId() + "，原组长角色变更为：" + previousOwnerRole);
            result.setPrepareSpace(prepareSpaceMapper.findPrepareSpaceById(spaceId));
            result.setMembers(loadMembers(spaceId, null));
            result.setSuccess(true);
            result.setMessage("组长转让成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("组长转让失败：" + e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return result;
    }

    public PrepareSpaceResult removeMember(Long spaceId, Long memberId, String accountId) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            assertManageableSpace(spaceId, accountId);
            PrepareSpaceMember member = prepareSpaceMapper.findMemberById(memberId);
            if (member == null || !Objects.equals(member.getPrepareSpaceId(), spaceId) || !"正常".equals(member.getStatus())) {
                throw new IllegalArgumentException("成员不存在");
            }
            if ("owner".equals(member.getRole())) {
                throw new IllegalArgumentException("不能移除创建者");
            }
            int updated = prepareSpaceMapper.removeMember(spaceId, memberId);
            if (updated <= 0) {
                throw new IllegalStateException("移除成员失败");
            }
            insertLog(spaceId, accountId, "移除成员", "成员", member.getAccountId(), "移除成员：" + member.getAccountId());
            result.setMembers(loadMembers(spaceId, null));
            result.setSuccess(true);
            result.setMessage("移除成员成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("移除成员失败：" + e.getMessage());
        }
        return result;
    }

    public PrepareSpaceResult batchRemoveMembers(Long spaceId, BatchRemovePrepareMembersRequest request) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            if (request == null || request.getAccountId() == null || request.getAccountId().isBlank()) {
                throw new IllegalArgumentException("操作账号不能为空");
            }
            assertManageableSpace(spaceId, request.getAccountId());
            if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
                throw new IllegalArgumentException("成员ID列表不能为空");
            }
            Set<Long> visited = new HashSet<>();
            int removedCount = 0;
            for (Long memberId : request.getMemberIds()) {
                if (memberId == null || !visited.add(memberId)) {
                    continue;
                }
                PrepareSpaceMember member = prepareSpaceMapper.findMemberById(memberId);
                if (member == null || !Objects.equals(member.getPrepareSpaceId(), spaceId) || !"正常".equals(member.getStatus())) {
                    continue;
                }
                if ("owner".equals(member.getRole())) {
                    throw new IllegalArgumentException("批量移除中包含创建者，无法继续");
                }
                if (prepareSpaceMapper.removeMember(spaceId, memberId) > 0) {
                    removedCount++;
                    insertLog(spaceId, request.getAccountId(), "移除成员", "成员", member.getAccountId(),
                            "批量移除成员：" + member.getAccountId());
                }
            }
            result.setMembers(loadMembers(spaceId, null));
            result.setSuccess(true);
            result.setMessage(removedCount > 0 ? "批量移除成员成功" : "没有可移除的成员");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("批量移除成员失败：" + e.getMessage());
        }
        return result;
    }

    public PrepareSpaceResult listLogs(Long spaceId, String accountId, String category, String keyword) {
        PrepareSpaceResult result = new PrepareSpaceResult();
        try {
            assertManageableSpace(spaceId, accountId);
            List<PrepareSpaceOperationLog> logs = prepareSpaceMapper.findLogsBySpaceId(spaceId);
            result.setLogs(filterLogs(logs, category, keyword));
            result.setSuccess(true);
            result.setMessage("操作记录获取成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("操作记录获取失败：" + e.getMessage());
        }
        return result;
    }

    private void validateCreateRequest(CreatePrepareSpaceRequest request) {
        if (request.getOwnerId() == null || request.getOwnerId().isBlank()) {
            throw new IllegalArgumentException("创建者账号不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("备课区名称不能为空");
        }
        if (request.getCourseId() == null || request.getCourseId().isBlank()) {
            throw new IllegalArgumentException("关联课程ID不能为空");
        }
    }

    private PrepareSpace assertVisibleSpace(Long spaceId, String accountId) {
        PrepareSpace prepareSpace = assertSpaceExists(spaceId);
        assertAccountExists(accountId);
        if (accountId.equals(prepareSpace.getOwnerId())) {
            return prepareSpace;
        }
        PrepareSpaceMember member = prepareSpaceMapper.findMemberByAccountId(spaceId, accountId);
        if (member == null) {
            throw new IllegalArgumentException("无权限访问该备课区");
        }
        return prepareSpace;
    }

    private PrepareSpace assertManageableSpace(Long spaceId, String accountId) {
        PrepareSpace prepareSpace = assertVisibleSpace(spaceId, accountId);
        if (accountId.equals(prepareSpace.getOwnerId())) {
            return prepareSpace;
        }
        PrepareSpaceMember member = prepareSpaceMapper.findMemberByAccountId(spaceId, accountId);
        if (member == null || (!"admin".equals(member.getRole()) && !"owner".equals(member.getRole()))) {
            throw new IllegalArgumentException("无权限管理该备课区");
        }
        return prepareSpace;
    }

    private PrepareSpace assertOwnerSpace(Long spaceId, String accountId) {
        PrepareSpace prepareSpace = assertSpaceExists(spaceId);
        if (!accountId.equals(prepareSpace.getOwnerId())) {
            throw new IllegalArgumentException("仅创建者可删除备课区");
        }
        return prepareSpace;
    }

    private PrepareSpace assertSpaceExists(Long spaceId) {
        if (spaceId == null) {
            throw new IllegalArgumentException("备课区ID不能为空");
        }
        PrepareSpace prepareSpace = prepareSpaceMapper.findPrepareSpaceById(spaceId);
        if (prepareSpace == null || !"正常".equals(prepareSpace.getStatus())) {
            throw new IllegalArgumentException("备课区不存在");
        }
        return prepareSpace;
    }

    private void assertTeacher(String accountId) {
        Account account = assertAccountExists(accountId);
        if (!"老师".equals(account.getIdentity())) {
            throw new IllegalArgumentException("仅教师可创建备课区");
        }
    }

    private Account assertAccountExists(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("账号不能为空");
        }
        Account account = accountService.findByAccountId(accountId);
        if (account == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        return account;
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

    private void cleanupPrepareSpaceData(Long spaceId) {
        prepareSpaceContentMapper.deleteAllAssignmentSubmissionsBySpaceId(spaceId);
        prepareSpaceContentMapper.deleteAllAssignmentsBySpaceId(spaceId);
        prepareSpaceContentMapper.deleteAllResourcesBySpaceId(spaceId);
        prepareSpaceContentMapper.deleteAllTopicsBySpaceId(spaceId);
        prepareSpaceContentMapper.deleteAllImportLogsBySpaceId(spaceId);
        prepareSpaceContentMapper.deleteAllFoldersBySpaceId(spaceId);
    }

    private void deletePrepareSpaceFiles(Long spaceId) {
        Path root = Paths.get(storagePath).toAbsolutePath().normalize();
        Path spaceDir = root.resolve(String.valueOf(spaceId)).normalize();
        if (!spaceDir.startsWith(root) || !Files.exists(spaceDir)) {
            return;
        }
        try (var stream = Files.walk(spaceDir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new IllegalStateException("删除备课区文件失败：" + path, e);
                        }
                    });
        } catch (IOException e) {
            throw new IllegalStateException("删除备课区文件失败", e);
        }
    }

    private String normalizeSpaceType(String type) {
        if (type == null || type.isBlank() || "全部".equals(type.trim())) {
            throw new IllegalArgumentException("备课区类型不能为空");
        }
        String normalized = type.trim();
        if (!"个人".equals(normalized) && !"小组".equals(normalized)) {
            throw new IllegalArgumentException("备课区类型仅支持：个人、小组");
        }
        return normalized;
    }

    private String normalizeRole(String role, boolean allowOwner) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("成员角色不能为空");
        }
        String normalized = role.trim();
        if ("管理员".equals(normalized)) {
            return "admin";
        }
        if ("成员".equals(normalized) || "member".equalsIgnoreCase(normalized)) {
            return "editor";
        }
        if ("只读".equals(normalized)) {
            return "viewer";
        }
        if ("admin".equalsIgnoreCase(normalized)) {
            return "admin";
        }
        if ("editor".equalsIgnoreCase(normalized)) {
            return "editor";
        }
        if ("viewer".equalsIgnoreCase(normalized)) {
            return "viewer";
        }
        if (allowOwner && "owner".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("成员角色仅支持：admin、editor、viewer");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<PrepareSpace> filterByType(List<PrepareSpace> spaces, String type) {
        List<PrepareSpace> filtered = new ArrayList<>();
        for (PrepareSpace space : deduplicate(spaces)) {
            if (type.equals(space.getSpaceType())) {
                filtered.add(space);
            }
        }
        return filtered;
    }

    private List<PrepareSpace> deduplicate(List<PrepareSpace> spaces) {
        Map<Long, PrepareSpace> map = new LinkedHashMap<>();
        for (PrepareSpace space : spaces) {
            map.put(space.getPrepareSpaceId(), space);
        }
        return new ArrayList<>(map.values());
    }

    private List<PrepareSpaceMember> loadMembers(Long spaceId, String keyword) {
        List<PrepareSpaceMember> source = prepareSpaceMapper.findMembersBySpaceId(spaceId);
        List<PrepareSpaceMember> members = new ArrayList<>();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        for (PrepareSpaceMember member : source) {
            Account account = accountService.findByAccountId(member.getAccountId());
            if (account != null) {
                member.setName(account.getName());
                member.setIdentity(account.getIdentity());
            }
            if (normalizedKeyword.isEmpty() || memberMatchesKeyword(member, normalizedKeyword)) {
                members.add(member);
            }
        }
        return members;
    }

    private boolean memberMatchesKeyword(PrepareSpaceMember member, String keyword) {
        return containsIgnoreCase(member.getAccountId(), keyword)
                || containsIgnoreCase(member.getName(), keyword)
                || containsIgnoreCase(member.getIdentity(), keyword)
                || containsIgnoreCase(member.getRole(), keyword);
    }

    private List<PrepareSpaceOperationLog> filterLogs(List<PrepareSpaceOperationLog> logs, String category, String keyword) {
        List<PrepareSpaceOperationLog> filtered = new ArrayList<>();
        String normalizedCategory = normalizeLogCategory(category);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        for (PrepareSpaceOperationLog log : logs) {
            if (!matchesLogCategory(log, normalizedCategory)) {
                continue;
            }
            if (!normalizedKeyword.isEmpty() && !matchesLogKeyword(log, normalizedKeyword)) {
                continue;
            }
            filtered.add(log);
        }
        return filtered;
    }

    private String normalizeLogCategory(String category) {
        if (category == null || category.isBlank() || "全部".equals(category.trim()) || "all".equalsIgnoreCase(category.trim())) {
            return "ALL";
        }
        String normalized = category.trim();
        if ("文件编辑".equals(normalized) || "file_edit".equalsIgnoreCase(normalized)) {
            return "FILE_EDIT";
        }
        if ("文件删除".equals(normalized) || "file_delete".equalsIgnoreCase(normalized)) {
            return "FILE_DELETE";
        }
        if ("成员变动".equals(normalized) || "member_change".equalsIgnoreCase(normalized)) {
            return "MEMBER_CHANGE";
        }
        throw new IllegalArgumentException("日志分类仅支持：全部、文件编辑、文件删除、成员变动");
    }

    private boolean matchesLogCategory(PrepareSpaceOperationLog log, String category) {
        return switch (category) {
            case "FILE_EDIT" -> isFileEditLog(log);
            case "FILE_DELETE" -> isFileDeleteLog(log);
            case "MEMBER_CHANGE" -> isMemberChangeLog(log);
            default -> true;
        };
    }

    private boolean isFileEditLog(PrepareSpaceOperationLog log) {
        if (isMemberChangeLog(log) || "备课区".equals(log.getOperationTarget())) {
            return false;
        }
        return !"删除".equals(log.getOperationType());
    }

    private boolean isFileDeleteLog(PrepareSpaceOperationLog log) {
        return !isMemberChangeLog(log)
                && !"备课区".equals(log.getOperationTarget())
                && "删除".equals(log.getOperationType());
    }

    private boolean isMemberChangeLog(PrepareSpaceOperationLog log) {
        return "成员".equals(log.getOperationTarget())
                || containsIgnoreCase(log.getOperationType(), "成员")
                || containsIgnoreCase(log.getDetail(), "成员");
    }

    private boolean matchesLogKeyword(PrepareSpaceOperationLog log, String keyword) {
        return containsIgnoreCase(log.getAccountId(), keyword)
                || containsIgnoreCase(log.getOperationType(), keyword)
                || containsIgnoreCase(log.getOperationTarget(), keyword)
                || containsIgnoreCase(log.getTargetId(), keyword)
                || containsIgnoreCase(log.getDetail(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}

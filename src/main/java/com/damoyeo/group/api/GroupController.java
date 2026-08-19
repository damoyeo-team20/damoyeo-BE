package com.damoyeo.group.api;

import com.damoyeo.common.auth.CurrentUserProvider;
import com.damoyeo.group.dto.CreateGroupRequest;
import com.damoyeo.group.dto.GroupDetailResponse;
import com.damoyeo.group.dto.GroupSummaryResponse;
import com.damoyeo.group.service.GroupService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final CurrentUserProvider currentUserProvider;
    private final GroupService groupService;

    public GroupController(CurrentUserProvider currentUserProvider, GroupService groupService) {
        this.currentUserProvider = currentUserProvider;
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupDetailResponse> create(@Valid @RequestBody CreateGroupRequest request) {
        GroupDetailResponse response = groupService.create(currentUserProvider.getCurrentUserId(), request);
        return ResponseEntity.created(URI.create("/api/groups/" + response.id())).body(response);
    }

    @GetMapping
    public List<GroupSummaryResponse> findMyGroups() {
        return groupService.findMyGroups(currentUserProvider.getCurrentUserId());
    }

    @GetMapping("/{groupId}")
    public GroupDetailResponse findDetail(@PathVariable long groupId) {
        return groupService.findDetail(currentUserProvider.getCurrentUserId(), groupId);
    }
}

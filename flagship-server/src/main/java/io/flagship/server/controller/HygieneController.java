package io.flagship.server.controller;

import io.flagship.server.service.FlagHygieneService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/hygiene")
public class HygieneController {

    private final FlagHygieneService hygieneService;

    public HygieneController(FlagHygieneService hygieneService) {
        this.hygieneService = hygieneService;
    }

    @GetMapping
    @PreAuthorize("@projectSec.hasRole(#projectId, 'VIEWER')")
    public ResponseEntity<List<FlagHygieneService.StaleFlagReport>> getStaleFlags(@PathVariable String projectId) {
        return ResponseEntity.ok(hygieneService.getStaleFlags(projectId));
    }
}

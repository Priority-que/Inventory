package com.xixi.agent.controller;

import com.xixi.agent.dto.RagKnowledgeImportRequest;
import com.xixi.agent.dto.RagSearchRequest;
import com.xixi.agent.service.PythonWorkflowProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/rag")
@RequiredArgsConstructor
public class AgentRagController {
    private final PythonWorkflowProxyService pythonWorkflowProxyService;

    @PostMapping("/import")
    public ResponseEntity<String> importKnowledge(@RequestBody RagKnowledgeImportRequest request,
                                                  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                                  String authorization) {
        return pythonWorkflowProxyService.importRagKnowledge(request, authorization);
    }

    @PostMapping("/search")
    public ResponseEntity<String> search(@RequestBody RagSearchRequest request,
                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                         String authorization) {
        return pythonWorkflowProxyService.searchRagKnowledge(request, authorization);
    }
}

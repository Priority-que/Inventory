package com.xixi.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "AI知识库", description = "AI知识库接口")
public class AgentRagController {
    private final PythonWorkflowProxyService pythonWorkflowProxyService;

    @Operation(summary = "导入RAG知识库", operationId = "importKnowledge")
    @PostMapping("/import")
    public ResponseEntity<String> importKnowledge(@RequestBody RagKnowledgeImportRequest request,
                                                  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                                  String authorization) {
        return pythonWorkflowProxyService.importRagKnowledge(request, authorization);
    }

    @Operation(summary = "检索RAG知识库", operationId = "search")
    @PostMapping("/search")
    public ResponseEntity<String> search(@RequestBody RagSearchRequest request,
                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                         String authorization) {
        return pythonWorkflowProxyService.searchRagKnowledge(request, authorization);
    }
}


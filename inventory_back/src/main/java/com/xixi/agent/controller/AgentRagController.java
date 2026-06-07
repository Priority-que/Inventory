package com.xixi.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.xixi.agent.dto.RagKnowledgeImportRequest;
import com.xixi.agent.dto.RagSearchRequest;
import com.xixi.agent.service.PythonWorkflowProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "上传RAG知识库文件", operationId = "uploadKnowledgeFile")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadKnowledge(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "docCode", required = false) String docCode,
                                                  @RequestParam(value = "title", required = false) String title,
                                                  @RequestParam(value = "docType", required = false) String docType,
                                                  @RequestParam(value = "bizIntent", required = false) String bizIntent,
                                                  @RequestParam(value = "sourcePath", required = false) String sourcePath,
                                                  @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                                  String authorization) {
        return pythonWorkflowProxyService.uploadRagKnowledge(
                file,
                docCode,
                title,
                docType,
                bizIntent,
                sourcePath,
                authorization
        );
    }

    @Operation(summary = "检索RAG知识库", operationId = "search")
    @PostMapping("/search")
    public ResponseEntity<String> search(@RequestBody RagSearchRequest request,
                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                         String authorization) {
        return pythonWorkflowProxyService.searchRagKnowledge(request, authorization);
    }
}


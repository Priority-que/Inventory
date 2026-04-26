package com.xixi.agent.controller;

import com.xixi.agent.dto.RagKnowledgeImportRequest;
import com.xixi.agent.dto.RagSearchRequest;
import com.xixi.agent.service.AgentRagService;
import com.xixi.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/rag")
@RequiredArgsConstructor
public class AgentRagController {
    private final AgentRagService agentRagService;

    @PostMapping("/import")
    public Result importKnowledge(@RequestBody RagKnowledgeImportRequest request) {
        return Result.success(agentRagService.importKnowledge(request));
    }

    @PostMapping("/search")
    public Result search(@RequestBody RagSearchRequest request) {
        return Result.success(agentRagService.search(request));
    }
}

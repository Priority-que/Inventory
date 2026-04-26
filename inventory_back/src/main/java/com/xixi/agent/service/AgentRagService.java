package com.xixi.agent.service;

import com.xixi.agent.dto.RagKnowledgeImportRequest;
import com.xixi.agent.dto.RagSearchRequest;
import com.xixi.agent.vo.RagImportResultVO;
import com.xixi.agent.vo.RagSearchResultVO;

import java.util.List;

public interface AgentRagService {
    RagImportResultVO importKnowledge(RagKnowledgeImportRequest request);

    List<RagSearchResultVO> search(RagSearchRequest request);

    List<RagSearchResultVO> search(String query, String bizIntent, Integer topK);
}

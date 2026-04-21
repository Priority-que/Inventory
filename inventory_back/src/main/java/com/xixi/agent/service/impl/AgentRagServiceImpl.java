package com.xixi.agent.service.impl;

import com.xixi.agent.dto.RagKnowledgeImportRequest;
import com.xixi.agent.dto.RagSearchRequest;
import com.xixi.agent.service.AgentRagService;
import com.xixi.agent.vo.RagImportResultVO;
import com.xixi.agent.vo.RagSearchResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AgentRagServiceImpl implements AgentRagService {
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 80;
    private static final int DEFAULT_TOP_K = 4;
    private static final int MAX_TOP_K = 10;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.45D;
    private static final Pattern DOC_CODE_PATTERN = Pattern.compile("^[A-Z0-9_\\-]{3,64}$");

    private final ObjectProvider<VectorStore> vectorStoreProvider;

    @Override
    public RagImportResultVO importKnowledge(RagKnowledgeImportRequest request) {
        validateImportRequest(request);

        String docCode = request.getDocCode().trim();
        String title = request.getTitle().trim();
        String docType = defaultIfBlank(request.getDocType(), "BUSINESS_RULE");
        String bizIntent = defaultIfBlank(request.getBizIntent(), "COMMON");
        String sourcePath = defaultIfBlank(request.getSourcePath(), "manual");
        VectorStore vectorStore = getVectorStore();

        // 同一个 docCode 重复导入时，先删除旧分片，避免召回到过期规则。
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.eq("docCode", docCode).build());

        List<String> chunks = splitContent(request.getContent());
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            int chunkNo = i + 1;
            String content = chunks.get(i);
            String id = buildChunkId(docCode, chunkNo, content);

            Document document = Document.builder()
                    .id(id)
                    .text(content)
                    .metadata(Map.of(
                            "docCode", docCode,
                            "title", title,
                            "docType", docType,
                            "bizIntent", bizIntent,
                            "sourcePath", sourcePath,
                            "chunkNo", chunkNo
                    ))
                    .build();
            documents.add(document);
        }

        vectorStore.add(documents);

        RagImportResultVO result = new RagImportResultVO();
        result.setDocCode(docCode);
        result.setTitle(title);
        result.setBizIntent(bizIntent);
        result.setChunkCount(documents.size());
        result.setMessage("知识导入成功");
        return result;
    }

    @Override
    public List<RagSearchResultVO> search(RagSearchRequest request) {
        if (request == null) {
            return List.of();
        }
        return search(request.getQuery(), request.getBizIntent(), request.getTopK());
    }

    @Override
    public List<RagSearchResultVO> search(String query, String bizIntent, Integer topK) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        VectorStore vectorStore = getVectorStore();

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query.trim())
                .topK(normalizeTopK(topK))
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD);

        Filter.Expression filterExpression = buildIntentFilter(bizIntent);
        if (filterExpression != null) {
            builder.filterExpression(filterExpression);
        }

        List<Document> documents = vectorStore.similaritySearch(builder.build());
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(this::toSearchResult)
                .toList();
    }

    private void validateImportRequest(RagKnowledgeImportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("导入请求不能为空");
        }
        if (!StringUtils.hasText(request.getDocCode())) {
            throw new IllegalArgumentException("docCode不能为空");
        }
        if (!DOC_CODE_PATTERN.matcher(request.getDocCode().trim()).matches()) {
            throw new IllegalArgumentException("docCode只能包含大写字母、数字、下划线和中划线，长度3到64位");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("title不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("content不能为空");
        }
    }

    private Filter.Expression buildIntentFilter(String bizIntent) {
        if (!StringUtils.hasText(bizIntent)) {
            return null;
        }

        String intent = bizIntent.trim();
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        return builder.or(
                builder.eq("bizIntent", "COMMON"),
                builder.eq("bizIntent", intent)
        ).build();
    }

    private List<String> splitContent(String content) {
        String text = content.replace("\r\n", "\n").trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            int splitEnd = findSplitPosition(text, start, end);
            String chunk = text.substring(start, splitEnd).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (splitEnd >= text.length()) {
                break;
            }
            int nextStart = splitEnd - CHUNK_OVERLAP;
            start = nextStart <= start ? splitEnd : nextStart;
        }

        return chunks;
    }

    private int findSplitPosition(String text, int start, int end) {
        if (end >= text.length()) {
            return text.length();
        }

        int minSplit = start + CHUNK_SIZE / 2;
        int newline = text.lastIndexOf('\n', end);
        if (newline > minSplit) {
            return newline;
        }

        for (int i = end - 1; i > minSplit; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '；' || c == ';' || c == '.') {
                return i + 1;
            }
        }

        return end;
    }

    private RagSearchResultVO toSearchResult(Document document) {
        RagSearchResultVO vo = new RagSearchResultVO();
        vo.setId(document.getId());
        vo.setContent(document.getText());
        vo.setScore(document.getScore());
        vo.setDocCode(getMetadata(document, "docCode"));
        vo.setTitle(getMetadata(document, "title"));
        vo.setDocType(getMetadata(document, "docType"));
        vo.setBizIntent(getMetadata(document, "bizIntent"));
        vo.setSourcePath(getMetadata(document, "sourcePath"));
        vo.setChunkNo(getIntegerMetadata(document, "chunkNo"));
        return vo;
    }

    private String getMetadata(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value == null ? null : value.toString();
    }

    private Integer getIntegerMetadata(Document document, String key) {
        Object value = document.getMetadata().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private String buildChunkId(String docCode, int chunkNo, String content) {
        return docCode + ":" + chunkNo + ":" + sha256(content).substring(0, 16);
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前JDK不支持SHA-256", e);
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private VectorStore getVectorStore() {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new IllegalStateException("RAG向量存储不可用");
        }
        return vectorStore;
    }
}

package com.xixi.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.xixi.agent.dto.RagKnowledgeImportRequest;
import com.xixi.agent.dto.RagSearchRequest;
import com.xixi.agent.service.AgentRagService;
import com.xixi.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/agent/rag")
@RequiredArgsConstructor
@Tag(name = "AI知识库", description = "AI知识库接口")
public class AgentRagController {
    private static final long MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
    private static final Set<String> SUPPORTED_TEXT_EXTENSIONS = Set.of("txt", "md", "markdown", "json", "csv");
    private static final Pattern INVALID_DOC_CODE_CHARS = Pattern.compile("[^A-Z0-9_-]+");

    private final AgentRagService agentRagService;

    @Operation(summary = "导入RAG知识库", operationId = "importKnowledge")
    @PostMapping("/import")
    public Result importKnowledge(@RequestBody RagKnowledgeImportRequest request) {
        return Result.success(agentRagService.importKnowledge(request));
    }

    @Operation(summary = "上传RAG知识库文件", operationId = "uploadKnowledgeFile")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadKnowledge(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "docCode", required = false) String docCode,
                                  @RequestParam(value = "title", required = false) String title,
                                  @RequestParam(value = "docType", required = false) String docType,
                                  @RequestParam(value = "bizIntent", required = false) String bizIntent,
                                  @RequestParam(value = "sourcePath", required = false) String sourcePath) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的知识文件");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            return Result.error("知识文件不能超过5MB");
        }

        String originalFilename = safeFilename(file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        if (!SUPPORTED_TEXT_EXTENSIONS.contains(extension)) {
            return Result.error("仅支持 txt、md、markdown、json、csv 文本类知识文件");
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        content = stripUtf8Bom(content).trim();
        if (content.isBlank()) {
            return Result.error("知识文件内容不能为空");
        }

        RagKnowledgeImportRequest request = new RagKnowledgeImportRequest();
        request.setDocCode(defaultIfBlank(docCode, buildDocCode(originalFilename)));
        request.setTitle(defaultIfBlank(title, buildTitle(originalFilename)));
        request.setDocType(docType);
        request.setBizIntent(bizIntent);
        request.setSourcePath(defaultIfBlank(sourcePath, originalFilename));
        request.setContent(content);
        return Result.success(agentRagService.importKnowledge(request));
    }

    @Operation(summary = "检索RAG知识库", operationId = "search")
    @PostMapping("/search")
    public Result search(@RequestBody RagSearchRequest request) {
        return Result.success(agentRagService.search(request));
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "knowledge.txt";
        }
        String normalized = originalFilename.replace("\\", "/");
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private String getExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String buildTitle(String filename) {
        int index = filename.lastIndexOf('.');
        String title = index > 0 ? filename.substring(0, index) : filename;
        return title.isBlank() ? "知识文件" : title.trim();
    }

    private String buildDocCode(String filename) {
        String base = buildTitle(filename)
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_');
        String docCode = INVALID_DOC_CODE_CHARS.matcher(base).replaceAll("_");
        docCode = docCode.replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        if (docCode.length() > 60) {
            docCode = docCode.substring(0, 60);
        }
        if (docCode.length() < 3) {
            docCode = "DOC_" + docCode;
        }
        if (docCode.length() < 3) {
            docCode = "DOC";
        }
        return docCode;
    }

    private String stripUtf8Bom(String content) {
        if (content != null && content.startsWith("\uFEFF")) {
            return content.substring(1);
        }
        return content;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}


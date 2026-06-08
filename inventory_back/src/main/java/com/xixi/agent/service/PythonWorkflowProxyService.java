package com.xixi.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.dto.WorkflowAgentRequest;
import com.xixi.config.PythonAgentProperties;
import com.xixi.pojo.vo.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PythonWorkflowProxyService {
    private static final String WORKFLOW_EXECUTE_PATH = "/agent/workflow/execute";
    private static final String WORKFLOW_STREAM_PATH = "/agent/workflow/stream";
    private static final String RAG_IMPORT_PATH = "/agent/rag/import";
    private static final String RAG_UPLOAD_PATH = "/agent/rag/upload";
    private static final String RAG_SEARCH_PATH = "/agent/rag/search";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PythonWorkflowProxyService(PythonAgentProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeoutMs());
        requestFactory.setReadTimeout(properties.getTimeoutMs());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public ResponseEntity<String> executeWorkflow(WorkflowAgentRequest request, String authorization) {
        return postJson(WORKFLOW_EXECUTE_PATH, request, authorization);
    }

    public void streamWorkflow(WorkflowAgentRequest request, String authorization, HttpServletResponse response) {
        response.setStatus(HttpStatus.OK.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            restClient.post()
                    .uri(WORKFLOW_STREAM_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .headers(headers -> {
                        if (StringUtils.hasText(authorization)) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorization);
                        }
                    })
                    .body(request)
                    .exchange((clientRequest, clientResponse) -> {
                        try (InputStream responseBody = clientResponse.getBody()) {
                            if (responseBody != null) {
                                copyStream(responseBody, response.getOutputStream());
                            }
                        }
                        return null;
                    });
        } catch (ResourceAccessException ex) {
            writeStreamError(response, "Python Agent 服务不可用：" + ex.getMessage());
        } catch (RestClientException ex) {
            writeStreamError(response, "请求 Python Agent 流式执行失败：" + ex.getMessage());
        }
    }

    public ResponseEntity<String> importRagKnowledge(Object request, String authorization) {
        return postJson(RAG_IMPORT_PATH, request, authorization);
    }

    public ResponseEntity<String> uploadRagKnowledge(MultipartFile file,
                                                     String docCode,
                                                     String title,
                                                     String docType,
                                                     String bizIntent,
                                                     String sourcePath,
                                                     String authorization) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        MediaType fileContentType = StringUtils.hasText(file.getContentType())
                ? MediaType.parseMediaType(file.getContentType())
                : MediaType.TEXT_PLAIN;
        builder.part("file", file.getResource())
                .filename(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "knowledge.txt")
                .contentType(fileContentType);
        addTextPart(builder, "docCode", docCode);
        addTextPart(builder, "title", title);
        addTextPart(builder, "docType", docType);
        addTextPart(builder, "bizIntent", bizIntent);
        addTextPart(builder, "sourcePath", sourcePath);

        try {
            return restClient.post()
                    .uri(RAG_UPLOAD_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .headers(headers -> {
                        if (StringUtils.hasText(authorization)) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorization);
                        }
                    })
                    .body(builder.build())
                    .exchange((clientRequest, clientResponse) -> {
                        InputStream responseBody = clientResponse.getBody();
                        String body = responseBody == null
                                ? ""
                                : StreamUtils.copyToString(responseBody, StandardCharsets.UTF_8);

                        HttpHeaders headers = new HttpHeaders();
                        MediaType contentType = clientResponse.getHeaders().getContentType();
                        headers.setContentType(contentType == null ? MediaType.APPLICATION_JSON : contentType);

                        return ResponseEntity.status(clientResponse.getStatusCode())
                                .headers(headers)
                                .body(body);
                    });
        } catch (ResourceAccessException ex) {
            return badGatewayResponse("Python Agent 服务不可用：" + ex.getMessage());
        } catch (RestClientException | IllegalArgumentException ex) {
            return badGatewayResponse("请求 Python Agent 上传 RAG 文件失败：" + ex.getMessage());
        }
    }

    public ResponseEntity<String> searchRagKnowledge(Object request, String authorization) {
        return postJson(RAG_SEARCH_PATH, request, authorization);
    }

    private void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
            outputStream.flush();
        }
    }

    private void writeStreamError(HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpStatus.OK.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            String payload = objectMapper.writeValueAsString(Map.of("message", message));
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(("event: error\n" + "data: " + payload + "\n\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (Exception ignored) {
        }
    }

    private void addTextPart(MultipartBodyBuilder builder, String name, String value) {
        if (StringUtils.hasText(value)) {
            builder.part(name, value, MediaType.TEXT_PLAIN);
        }
    }

    private ResponseEntity<String> postJson(String path, Object request, String authorization) {
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (StringUtils.hasText(authorization)) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorization);
                        }
                    })
                    .body(request)
                    .exchange((clientRequest, clientResponse) -> {
                        InputStream responseBody = clientResponse.getBody();
                        String body = responseBody == null
                                ? ""
                                : StreamUtils.copyToString(responseBody, StandardCharsets.UTF_8);

                        HttpHeaders headers = new HttpHeaders();
                        MediaType contentType = clientResponse.getHeaders().getContentType();
                        headers.setContentType(contentType == null ? MediaType.APPLICATION_JSON : contentType);

                        return ResponseEntity.status(clientResponse.getStatusCode())
                                .headers(headers)
                                .body(body);
                    });
        } catch (ResourceAccessException ex) {
            return badGatewayResponse("Python Agent 服务不可用：" + ex.getMessage());
        } catch (RestClientException ex) {
            return badGatewayResponse("请求 Python Agent 失败：" + ex.getMessage());
        }
    }

    private ResponseEntity<String> badGatewayResponse(String message) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(Result.error(502, message)));
    }

    private String toJson(Result result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            return "{\"code\":502,\"msg\":\"Python Agent 服务异常\",\"data\":null}";
        }
    }
}

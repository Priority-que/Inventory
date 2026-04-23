package com.xixi.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.dto.WorkflowAgentRequest;
import com.xixi.config.PythonAgentProperties;
import com.xixi.pojo.vo.Result;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class PythonWorkflowProxyService {
    private static final String WORKFLOW_EXECUTE_PATH = "/agent/workflow/execute";

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
        try {
            return restClient.post()
                    .uri(WORKFLOW_EXECUTE_PATH)
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

package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.workflow.state.WorkflowEntity;
import com.xixi.agent.workflow.state.WorkflowStateKeys;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityExtractNode implements NodeAction {
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("PO\\d+");
    private static final Pattern DAYS_PATTERN = Pattern.compile("(最近|近)?(\\d+)\\s*天");
    private static final Pattern SUPPLIER_ID_PATTERN = Pattern.compile("供应商\\s*(\\d+)");

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String message = state.value(WorkflowStateKeys.NORMALIZED_MESSAGE, "").toString();

        WorkflowEntity entity = (WorkflowEntity) state.value(WorkflowStateKeys.ENTITY).orElse(new WorkflowEntity());

        String orderNo = findFirst(ORDER_NO_PATTERN, message, 0);
        if (orderNo != null) {
            entity.setOrderNo(orderNo);
        }

        String days = findFirst(DAYS_PATTERN, message, 2);
        if (days != null) {
            entity.setDays(Integer.parseInt(days));
        } else if (entity.getDays() == null) {
            entity.setDays(30);
        }

        String supplierId = findFirst(SUPPLIER_ID_PATTERN, message, 1);
        if (supplierId != null) {
            entity.setSupplierId(Long.parseLong(supplierId));
        }

        return Map.of(WorkflowStateKeys.ENTITY, entity);
    }

    private String findFirst(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(group);
        }
        return null;
    }
}

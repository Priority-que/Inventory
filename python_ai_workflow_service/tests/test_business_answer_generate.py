import unittest

from app.workflows.nodes.answer_plan import AnswerPlanNode
from app.workflows.nodes.build_answer_card import BuildAnswerCardNode
from app.workflows.nodes.business_answer_generate import BusinessAnswerGenerateNode
from app.workflows.nodes.intent_classify import IntentClassifyNode
from app.workflows.nodes.response_policy import ResponsePolicyNode
from app.workflows.state import WorkflowIntent, WorkflowStateKeys


class FakeLLMClient:
    def __init__(self, text: str = "", configured: bool = False):
        self.text = text
        self.configured = configured

    def is_configured(self) -> bool:
        return self.configured

    async def chat_text(self, system_prompt: str, user_prompt: str, temperature: float = 0.2) -> str:
        return self.text


class BusinessAnswerGenerateNodeTest(unittest.IsolatedAsyncioTestCase):
    async def test_order_owner_reason_fallback_uses_person_and_hides_codes(self):
        node = BusinessAnswerGenerateNode(FakeLLMClient(configured=False))
        result = await node(
            {
                WorkflowStateKeys.MESSAGE: "让哪个采购员跟进？为什么选他？",
                WorkflowStateKeys.ANSWER_CARD: {
                    "intent": "ORDER_DIAGNOSIS",
                    "questionFocus": "OWNER_REASON",
                    "bizType": "PURCHASE_ORDER",
                    "bizKey": "PO2026040022",
                    "conclusion": "PO2026040022 建议让 采购员007（采购侧）跟进。",
                    "reasons": [
                        "当前阶段是“供应商发货 / 仓库到货登记阶段”。",
                        "订单已进入执行中，但目前还没有到货记录。",
                    ],
                    "evidence": [
                        "到货数量为 0.000，如果到货数量为 0，说明仓库暂时没有可登记的到货对象。",
                    ],
                    "nextActions": [
                        "请采购负责人先联系供应商确认发货时间，到货后再通知仓库登记到货。",
                    ],
                    "companionHint": "如果用户继续追问，可以顺着责任人、催办顺序或沟通话术往下拆。",
                },
                WorkflowStateKeys.SELECTED_CONTEXT: {
                    "interactionType": "BUSINESS",
                    "intent": "ORDER_DIAGNOSIS",
                    "questionFocus": "OWNER_REASON",
                    "useLlm": True,
                    "facts": {
                        "orderNo": "PO2026040022",
                        "currentStage": "供应商发货 / 仓库到货登记阶段",
                        "blockReason": "订单已进入执行中，但目前还没有到货记录。",
                        "responsibility": {
                            "ownerRole": "PURCHASER",
                            "ownerRoleName": "采购侧",
                            "ownerUserName": "采购员007",
                            "ownerReason": "当前问题卡在供应商发货前段，仓库暂时没有可登记的到货对象，所以第一责任点在采购侧。",
                        },
                        "nextAction": {
                            "actionText": "请采购负责人先联系供应商确认发货时间，到货后再通知仓库登记到货。",
                        },
                        "evidence": [
                            "采购数量为 117.000，采购数量用于判断到货和入库是否完成。",
                            "到货数量为 0.000，如果到货数量为 0，说明仓库暂时没有可登记的到货对象。",
                        ],
                    },
                },
            }
        )

        answer = result[WorkflowStateKeys.LLM_ANSWER]
        self.assertIn("采购员007", answer)
        self.assertIn("供应商发货", answer)
        self.assertNotIn("PURCHASER", answer)

    async def test_llm_answer_is_sanitized_before_returning(self):
        node = BusinessAnswerGenerateNode(
            FakeLLMClient("建议由 PURCHASER 处理，状态是 IN_PROGRESS，风险是 HIGH。", configured=True)
        )
        result = await node(
            {
                WorkflowStateKeys.MESSAGE: "为什么 PO2026040001 风险这么高？",
                WorkflowStateKeys.SELECTED_CONTEXT: {
                    "interactionType": "BUSINESS",
                    "intent": "WARNING_SCAN",
                    "questionFocus": "SPECIFIC_WARNING_REASON",
                    "useLlm": True,
                    "facts": {},
                    "items": [
                        {
                            "bizNo": "PO2026040001",
                            "riskLevelName": "高风险",
                            "problem": "采购订单待供应商确认超时",
                            "reason": "订单长时间停留在待供应商确认状态。",
                            "suggestOwnerName": "采购侧",
                        }
                    ],
                },
            }
        )

        answer = result[WorkflowStateKeys.LLM_ANSWER]
        self.assertIn("采购侧", answer)
        self.assertIn("执行中", answer)
        self.assertIn("高风险", answer)
        self.assertNotIn("PURCHASER", answer)
        self.assertNotIn("IN_PROGRESS", answer)
        self.assertNotIn("HIGH", answer)

    async def test_supplier_weak_metric_answer_points_to_real_short_board(self):
        node = BusinessAnswerGenerateNode(FakeLLMClient(configured=False))
        result = await node(
            {
                WorkflowStateKeys.MESSAGE: "为什么只有这个？到底差在哪？",
                WorkflowStateKeys.SELECTED_CONTEXT: {
                    "interactionType": "BUSINESS",
                    "intent": "SUPPLIER_SCORE",
                    "questionFocus": "WEAK_METRIC",
                    "useLlm": True,
                    "facts": {
                        "supplierName": "AOP测试供应商001",
                        "score": 65,
                        "level": "一般",
                        "confirmRate": "50.00%",
                        "arrivalCompletionRate": "50.00%",
                        "inboundCompletionRate": "50.00%",
                        "abnormalArrivalRate": "0.00%",
                        "scoreBreakdown": [
                            {"metricName": "确认及时率", "actualScore": 10, "maxScore": 20, "value": "50.00%"},
                            {"metricName": "到货完成率", "actualScore": 15, "maxScore": 30, "value": "50.00%"},
                        ],
                        "weakMetrics": [
                            {
                                "metricName": "确认及时率",
                                "reason": "确认及时率只有 50.00%",
                                "suggestion": "要求供应商缩短确认周期。",
                            },
                            {
                                "metricName": "到货完成率",
                                "reason": "到货完成率只有 50.00%",
                                "suggestion": "跟踪供应商发货计划。",
                            },
                        ],
                    },
                },
            }
        )

        answer = result[WorkflowStateKeys.LLM_ANSWER]
        self.assertIn("确认及时率", answer)
        self.assertIn("到货完成率", answer)
        self.assertIn("50.00%", answer)
        self.assertNotIn("CONFIRM_RATE", answer)

    async def test_supplier_follow_up_focus_prefers_short_board_question(self):
        node = AnswerPlanNode()
        result = await node(
            {
                WorkflowStateKeys.INTERACTION_TYPE: "BUSINESS",
                WorkflowStateKeys.INTENT: WorkflowIntent.SUPPLIER_SCORE.value,
                WorkflowStateKeys.MESSAGE: "为什么只有这个？到底差在哪？",
                WorkflowStateKeys.SUPPLIER_SCORE: {"score": 65},
            }
        )

        plan = result[WorkflowStateKeys.ANSWER_PLAN]
        self.assertEqual(plan["questionFocus"], "WEAK_METRIC")
        self.assertTrue(plan["useLlm"])

    async def test_supplier_follow_up_focus_prefers_action_question(self):
        node = AnswerPlanNode()
        result = await node(
            {
                WorkflowStateKeys.INTERACTION_TYPE: "BUSINESS",
                WorkflowStateKeys.INTENT: WorkflowIntent.SUPPLIER_SCORE.value,
                WorkflowStateKeys.MESSAGE: "那么该怎么提升这个评分表现呢",
                WorkflowStateKeys.SUPPLIER_SCORE: {"score": 65},
            }
        )

        plan = result[WorkflowStateKeys.ANSWER_PLAN]
        self.assertEqual(plan["questionFocus"], "SUPPLIER_ACTION")

    async def test_answer_plan_reuses_only_same_business_scope(self):
        node = AnswerPlanNode()
        result = await node(
            {
                WorkflowStateKeys.INTERACTION_TYPE: "BUSINESS",
                WorkflowStateKeys.INTENT: WorkflowIntent.ORDER_DIAGNOSIS.value,
                WorkflowStateKeys.MESSAGE: "帮我分析 PO2026040023 为什么还没完成",
                WorkflowStateKeys.ENTITY: {"orderNo": "PO2026040023"},
                WorkflowStateKeys.ORDER_DIAGNOSIS: {"orderNo": "PO2026040022"},
                WorkflowStateKeys.ANSWER_PLAN: {
                    "intent": WorkflowIntent.ORDER_DIAGNOSIS.value,
                    "bizType": "PURCHASE_ORDER",
                    "bizKey": "PO2026040022",
                },
            }
        )

        plan = result[WorkflowStateKeys.ANSWER_PLAN]
        self.assertEqual(plan["bizType"], "PURCHASE_ORDER")
        self.assertEqual(plan["bizKey"], "PO2026040023")
        self.assertTrue(plan["needsRefresh"])

    async def test_warning_question_with_order_no_should_not_reuse_order_intent(self):
        node = IntentClassifyNode(FakeLLMClient(configured=False))
        result = await node(
            {
                WorkflowStateKeys.NORMALIZED_MESSAGE: "为什么 PO2026040001 风险这么高？",
                WorkflowStateKeys.ACTIVE_INTENT: WorkflowIntent.ORDER_DIAGNOSIS.value,
            }
        )

        self.assertEqual(result[WorkflowStateKeys.INTENT], WorkflowIntent.WARNING_SCAN.value)

    async def test_response_policy_builds_contextual_social_reply(self):
        policy_node = ResponsePolicyNode()
        policy_result = await policy_node(
            {
                WorkflowStateKeys.MESSAGE: "好的谢谢",
                WorkflowStateKeys.INTERACTION_TYPE: "SOCIAL",
                WorkflowStateKeys.SELECTED_CONTEXT: {
                    "interactionType": "SOCIAL",
                    "intent": "UNKNOWN",
                    "questionFocus": "SOCIAL",
                },
                WorkflowStateKeys.CONVERSATION_MEMORY: {
                    "lastIntent": WorkflowIntent.SUPPLIER_SCORE.value,
                    "lastBizKey": "supplierId=1,days=180",
                },
            }
        )

        answer_node = BusinessAnswerGenerateNode(FakeLLMClient(configured=False))
        answer_result = await answer_node(
            {
                WorkflowStateKeys.SELECTED_CONTEXT: {
                    "interactionType": "SOCIAL",
                    "intent": "UNKNOWN",
                },
                WorkflowStateKeys.RESPONSE_POLICY: policy_result[WorkflowStateKeys.RESPONSE_POLICY],
                WorkflowStateKeys.CONVERSATION_MEMORY: policy_result[WorkflowStateKeys.CONVERSATION_MEMORY],
            }
        )

        answer = answer_result[WorkflowStateKeys.LLM_ANSWER]
        self.assertIn("不客气", answer)
        self.assertIn("供应商", answer)

    async def test_build_answer_card_for_supplier_action(self):
        node = BuildAnswerCardNode()
        result = await node(
            {
                WorkflowStateKeys.SELECTED_CONTEXT: {
                    "intent": "SUPPLIER_SCORE",
                    "questionFocus": "SUPPLIER_ACTION",
                    "bizType": "SUPPLIER",
                    "bizKey": "supplierId=1,days=180",
                    "facts": {
                        "supplierName": "AOP测试供应商001",
                        "score": 65,
                        "level": "一般",
                        "confirmRate": "50.00%",
                        "arrivalCompletionRate": "50.00%",
                        "inboundCompletionRate": "50.00%",
                        "abnormalArrivalRate": "0.00%",
                        "weakMetrics": [
                            {
                                "metricName": "确认及时率",
                                "reason": "确认及时率只有 50.00%",
                                "suggestion": "要求供应商缩短确认周期。",
                            },
                            {
                                "metricName": "到货完成率",
                                "reason": "到货完成率只有 50.00%",
                                "suggestion": "跟踪供应商发货计划。",
                            },
                        ],
                    },
                }
            }
        )

        card = result[WorkflowStateKeys.ANSWER_CARD]
        self.assertEqual(card["questionFocus"], "SUPPLIER_ACTION")
        self.assertIn("重点盯", card["conclusion"])
        self.assertTrue(card["nextActions"])


if __name__ == "__main__":
    unittest.main()

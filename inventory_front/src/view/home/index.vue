<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getStatisticsSummaryApi, type StatisticsSummaryVO } from '@/api/statistics'

const loading = ref(false)
const summary = ref<StatisticsSummaryVO | null>(null)

const stats = computed(() => [
  { label: '待审批采购申请', value: summary.value?.pendingApprovalRequestCount },
  { label: '待供应商确认订单', value: summary.value?.waitConfirmOrderCount },
  { label: '执行中订单', value: summary.value?.inProgressOrderCount },
  { label: '待入库到货单', value: summary.value?.pendingInboundArrivalCount },
  { label: '库存预警', value: summary.value?.inventoryAlertCount },
  { label: '异常到货', value: summary.value?.abnormalArrivalCount },
])

async function loadSummary() {
  loading.value = true
  try {
    summary.value = await getStatisticsSummaryApi()
  } finally {
    loading.value = false
  }
}

onMounted(loadSummary)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">工作台</h2>
        <p class="page-subtitle">查看采购、库存和供应商的关键业务状态</p>
      </div>
    </div>

    <div v-loading="loading" class="stats-grid">
      <el-card v-for="item in stats" :key="item.label" shadow="never">
        <div class="stat-value">{{ item.value ?? '--' }}</div>
        <div class="stat-label">{{ item.label }}</div>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <span>近期事项</span>
      </template>
      <el-empty description="暂无待处理事项" />
    </el-card>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  min-height: 266px;
}

.stat-value {
  color: #111827;
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  margin-top: 8px;
  color: #6b7280;
  font-size: 13px;
}

@media (max-width: 960px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

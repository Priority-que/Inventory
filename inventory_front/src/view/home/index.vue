<script setup lang="ts">
const stats = [
  { label: '待处理采购', value: 0 },
  { label: '待确认到货', value: 0 },
  { label: '库存预警', value: 0 },
  { label: '活跃供应商', value: 0 },
]
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">工作台</h2>
        <p class="page-subtitle">查看采购、库存和供应商的关键业务状态</p>
      </div>
    </div>

    <div class="stats-grid">
      <el-card v-for="item in stats" :key="item.label" shadow="never">
        <div class="stat-value">{{ item.value }}</div>
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

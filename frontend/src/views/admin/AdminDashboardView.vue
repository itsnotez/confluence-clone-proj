<template>
  <div class="admin-page">
    <div class="admin-header">
      <h1>관리자 대시보드</h1>
    </div>

    <div class="admin-content">
      <DxTabPanel :animation-enabled="false">
        <DxItem title="대시보드">
          <template #default>
            <div class="tab-content">
              <div v-if="loading" class="loading-message">로딩 중...</div>
              <div v-else-if="stats">
                <!-- 통계 카드 4개 -->
                <div class="stats-grid">
                  <div class="stat-card">
                    <div class="stat-label">활성 사용자</div>
                    <div class="stat-value">{{ stats.activeUsers }}</div>
                  </div>
                  <div class="stat-card">
                    <div class="stat-label">Space 수</div>
                    <div class="stat-value">{{ stats.totalSpaces }}</div>
                  </div>
                  <div class="stat-card">
                    <div class="stat-label">콘텐츠 수</div>
                    <div class="stat-value">{{ stats.totalContents }}</div>
                  </div>
                  <div class="stat-card">
                    <div class="stat-label">스토리지 사용량</div>
                    <div class="stat-value">{{ formatBytes(stats.storageUsedBytes) }}</div>
                  </div>
                </div>

                <!-- 메일 서버 상태 파이 차트 -->
                <div class="chart-section">
                  <h3 class="chart-title">메일 서버 상태</h3>
                  <DxPieChart
                    :data-source="mailStatusData"
                    palette="Bright"
                    :size="{ width: 400, height: 300 }"
                  >
                    <DxSeries
                      argument-field="status"
                      value-field="count"
                      :label="{ visible: true, connector: { enabled: true } }"
                    />
                  </DxPieChart>
                </div>
              </div>
              <div v-else class="no-data">데이터를 불러올 수 없습니다.</div>
            </div>
          </template>
        </DxItem>

        <DxItem title="감사로그">
          <template #default>
            <div class="tab-content">
              <DxDataGrid
                :data-source="auditLogs"
                :show-borders="true"
                :hover-state-enabled="true"
                :row-alternation-enabled="true"
                height="calc(100vh - 220px)"
              >
                <DxColumn data-field="actorId" caption="수행자" />
                <DxColumn data-field="actionType" caption="액션" />
                <DxColumn data-field="targetType" caption="대상 타입" />
                <DxColumn data-field="targetId" caption="대상 ID" />
                <DxColumn data-field="isAdminAccess" caption="관리자" />
                <DxColumn data-field="createdAt" caption="시각" data-type="datetime" />
                <DxPaging :page-size="20" />
              </DxDataGrid>
            </div>
          </template>
        </DxItem>
      </DxTabPanel>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { DxTabPanel, DxItem } from 'devextreme-vue/tab-panel'
import { DxPieChart, DxSeries } from 'devextreme-vue/pie-chart'
import { DxDataGrid, DxColumn, DxPaging } from 'devextreme-vue/data-grid'

const stats = ref(null)
const auditLogs = ref([])
const auditLogTotal = ref(0)
const loading = ref(false)

const mailStatusData = computed(() => {
  if (!stats.value) return []
  return [
    { status: '정상', count: stats.value.mailAccountsOk },
    { status: '오류', count: stats.value.mailAccountsFailed }
  ]
})

function formatBytes(bytes) {
  if (bytes == null) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

onMounted(async () => {
  loading.value = true
  try {
    const [statsRes, logsRes] = await Promise.all([
      adminApi.getStats(),
      adminApi.getAuditLogs({ page: 0, size: 20 })
    ])
    stats.value = statsRes.data.data
    auditLogs.value = logsRes.data.data.content
    auditLogTotal.value = logsRes.data.data.totalElements
  } catch (e) {
    console.error('관리자 데이터 로딩 실패:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #fafafa;
  padding: 24px;
}

.admin-header {
  margin-bottom: 24px;
}

.admin-header h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.admin-content {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.tab-content {
  padding: 16px 0;
}

.loading-message {
  padding: 40px;
  text-align: center;
  color: #666;
}

.no-data {
  padding: 40px;
  text-align: center;
  color: #999;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: #f5f7fa;
  border: 1px solid #e0e4ea;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a73e8;
}

.chart-section {
  margin-top: 16px;
}

.chart-title {
  margin: 0 0 12px;
  font-size: 16px;
  color: #333;
}
</style>

<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="4" v-for="item in cards" :key="item.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="box-card" style="margin-top: 20px">
      <div slot="header" class="clearfix">
        <span>近 7 天操作日志</span>
      </div>
      <el-table :data="stats.operationLogs7d || []" border>
        <el-table-column label="日期" align="center" prop="date" />
        <el-table-column label="数量" align="center" prop="count" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getOverview } from '@/api/wiki-admin'

export default {
  name: 'WikiAdminDashboard',
  data() {
    return {
      stats: {}
    }
  },
  computed: {
    cards() {
      return [
        { label: '活跃用户', value: this.stats.activeUsers || 0 },
        { label: '正常文档', value: this.stats.totalDocs || 0 },
        { label: '已删文档', value: this.stats.deletedDocs || 0 },
        { label: '24h 操作', value: this.stats.operationLogs24h || 0 },
        { label: '24h 查看', value: this.stats.docViews24h || 0 },
        { label: '24h 修改', value: this.stats.docEdits24h || 0 }
      ]
    }
  },
  created() {
    this.getData()
  },
  methods: {
    getData() {
      getOverview().then(res => {
        this.stats = res || {}
      })
    }
  }
}
</script>

<style scoped>
.stat-card {
  margin-bottom: 16px;
}
.stat-label {
  color: #909399;
  font-size: 13px;
}
.stat-value {
  margin-top: 10px;
  font-size: 24px;
  font-weight: 700;
}
</style>

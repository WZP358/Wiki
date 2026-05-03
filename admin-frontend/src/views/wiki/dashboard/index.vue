<template>
  <div class="app-container wiki-dashboard">
    <el-row :gutter="16" class="mb16">
      <el-col :span="6" v-for="item in cards" :key="item.label">
        <el-card shadow="hover" class="data-card">
          <div class="card-label">{{ item.label }}</div>
          <div class="card-value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>最近更新文档</span>
          </div>
          <el-table :data="latestDocuments" size="small">
            <el-table-column label="文档标题" prop="title" min-width="220" :show-overflow-tooltip="true" />
            <el-table-column label="知识库" prop="kbName" min-width="120" :show-overflow-tooltip="true" />
            <el-table-column label="状态" prop="status" width="90">
              <template slot-scope="scope">
                <el-tag size="mini" :type="scope.row.status === 'published' ? 'success' : 'info'">
                  {{ scope.row.status === "published" ? "已发布" : "草稿" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" prop="updateTime" width="160">
              <template slot-scope="scope">
                <span>{{ parseTime(scope.row.updateTime) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>热门文档</span>
          </div>
          <el-table :data="hotDocuments" size="small">
            <el-table-column label="文档标题" prop="title" min-width="220" :show-overflow-tooltip="true" />
            <el-table-column label="知识库" prop="kbName" min-width="120" :show-overflow-tooltip="true" />
            <el-table-column label="浏览量" prop="viewCount" width="90" />
            <el-table-column label="更新时间" prop="updateTime" width="160">
              <template slot-scope="scope">
                <span>{{ parseTime(scope.row.updateTime) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getAdminDashboard } from "@/api/wiki/workbenchAdmin"

export default {
  name: "WikiDashboard",
  data() {
    return {
      dashboard: {},
      latestDocuments: [],
      hotDocuments: []
    }
  },
  computed: {
    cards() {
      return [
        { label: "知识库", value: this.dashboard.knowledgeBaseCount || 0 },
        { label: "分组", value: this.dashboard.groupCount || 0 },
        { label: "文档总数", value: this.dashboard.documentCount || 0 },
        { label: "已发布文档", value: this.dashboard.publishedDocCount || 0 },
        { label: "草稿文档", value: this.dashboard.draftDocCount || 0 },
        { label: "收藏总数", value: this.dashboard.favoriteCount || 0 },
        { label: "小记总数", value: this.dashboard.noteCount || 0 },
        { label: "评论总数", value: this.dashboard.commentCount || 0 }
      ]
    }
  },
  created() {
    this.getData()
  },
  methods: {
    getData() {
      getAdminDashboard().then(response => {
        this.dashboard = response.data || {}
        this.latestDocuments = this.dashboard.latestDocuments || []
        this.hotDocuments = this.dashboard.hotDocuments || []
      })
    }
  }
}
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}

.data-card {
  min-height: 110px;
}

.card-label {
  color: #909399;
  font-size: 14px;
  margin-bottom: 18px;
}

.card-value {
  color: #303133;
  font-size: 30px;
  font-weight: 600;
  line-height: 1;
}
</style>

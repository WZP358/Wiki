<template>
  <el-card shadow="never" class="box-card">
    <div slot="header" class="reader-header">
      <span>{{ title }}</span>
      <slot name="extra" />
    </div>
    <div v-if="document && document.docId">
      <div class="reader-title">{{ document.title }}</div>
      <div class="reader-meta">
        <el-tag size="mini" type="info">{{ visibilityText(document.visibility) }}</el-tag>
        <el-tag size="mini" :type="document.status === 'published' ? 'success' : 'info'">
          {{ statusText(document.status) }}
        </el-tag>
        <span>{{ document.ownerName || "系统用户" }}</span>
        <span v-if="document.deptName">{{ document.deptName }}</span>
        <span>{{ document.viewCount || 0 }} 次阅读</span>
      </div>
      <div v-if="document.summary" class="reader-summary">{{ document.summary }}</div>
      <div class="reader-content" v-html="document.htmlContent || '<p>-</p>'"></div>
    </div>
    <el-empty v-else :description="emptyText" :image-size="72" />
  </el-card>
</template>

<script>
export default {
  name: "DocumentReader",
  props: {
    title: {
      type: String,
      default: "文档内容"
    },
    document: {
      type: Object,
      default: () => ({})
    },
    emptyText: {
      type: String,
      default: "暂无内容"
    }
  },
  methods: {
    visibilityText(value) {
      return { public: "公开", department: "部门可见", private: "私有" }[value] || value || "-"
    },
    statusText(value) {
      return { draft: "草稿", published: "已发布" }[value] || value || "-"
    }
  }
}
</script>

<style lang="scss" scoped>
.reader-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reader-title {
  margin-bottom: 12px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.reader-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  color: #909399;
  font-size: 13px;
}

.reader-summary {
  margin-bottom: 16px;
  padding: 12px 16px;
  border-left: 4px solid #409eff;
  background: #f4faff;
  color: #606266;
}

.reader-content {
  color: #303133;
  line-height: 1.8;
}

.reader-content ::v-deep pre {
  padding: 12px;
  overflow-x: auto;
  background: #f5f7fa;
}

.reader-content ::v-deep img {
  max-width: 100%;
}
</style>

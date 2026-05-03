<template>
  <el-drawer title="文档详情" :visible.sync="visible" size="620px" append-to-body>
    <div v-loading="loading" class="wiki-view">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文档ID">{{ form.docId || "-" }}</el-descriptions-item>
        <el-descriptions-item label="知识库">{{ form.kbName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="分组">{{ form.groupName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="文档标题" :span="2">{{ form.title || "-" }}</el-descriptions-item>
        <el-descriptions-item label="父文档">{{ form.parentId || 0 }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ form.ownerName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="可见范围">{{ visibilityText(form.visibility) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(form.status) }}</el-descriptions-item>
        <el-descriptions-item label="所属部门">{{ form.deptName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="岗位" :span="2">{{ form.ownerPostNames || "-" }}</el-descriptions-item>
        <el-descriptions-item label="浏览量">{{ form.viewCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="版本号">v{{ form.versionNo || 0 }}</el-descriptions-item>
        <el-descriptions-item label="最近更新" :span="2">{{ parseTime(form.updateTime) || "-" }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">{{ form.summary || "-" }}</el-descriptions-item>
      </el-descriptions>

      <div class="wiki-view__content">
        <div class="wiki-view__title">正文内容</div>
        <div class="wiki-view__body" v-html="form.htmlContent || '<p>-</p>'"></div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import { getAdminWikiDocument } from "@/api/wiki/document"

export default {
  name: "WikiViewDrawer",
  data() {
    return {
      visible: false,
      loading: false,
      form: {}
    }
  },
  methods: {
    open(docId) {
      this.visible = true
      this.loading = true
      getAdminWikiDocument(docId).then(res => {
        this.form = res.data || {}
      }).finally(() => {
        this.loading = false
      })
    },
    visibilityText(value) {
      return { public: "公开", department: "部门可见", private: "仅自己" }[value] || value || "-"
    },
    statusText(value) {
      return { draft: "草稿", published: "已发布" }[value] || value || "-"
    }
  }
}
</script>

<style lang="scss" scoped>
.wiki-view {
  padding: 0 16px 16px;
}

.wiki-view__content {
  margin-top: 20px;
}

.wiki-view__title {
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
}

.wiki-view__body {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  line-height: 1.8;
  color: #303133;
}

.wiki-view__body ::v-deep pre {
  padding: 12px;
  overflow-x: auto;
  background: #f5f7fa;
}

.wiki-view__body ::v-deep img {
  max-width: 100%;
}
</style>

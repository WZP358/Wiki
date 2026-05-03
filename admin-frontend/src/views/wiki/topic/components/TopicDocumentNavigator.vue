<template>
  <div>
    <el-card shadow="never" class="box-card">
      <div slot="header" class="topic-header">
        <span>{{ title }}</span>
        <span class="topic-count">共 {{ documents.length }} 篇</span>
      </div>
      <div
        v-for="(item, index) in documents"
        :key="item.docId"
        class="topic-doc-item"
        :class="{ 'is-active': currentDocId === item.docId }"
        @click="$emit('select', item)"
      >
        <div class="topic-doc-item__title">
          <span class="topic-doc-item__index">{{ index + 1 }}</span>
          <span>{{ item.title }}</span>
        </div>
        <div class="topic-doc-item__meta">
          <span>{{ item.ownerName || "系统用户" }}</span>
          <span>{{ item.viewCount || 0 }} 次阅读</span>
        </div>
      </div>
      <el-empty v-if="!documents.length" description="暂无可阅读文档" :image-size="72" />
    </el-card>

    <el-card shadow="never" class="box-card" v-if="documents.length">
      <div slot="header" class="topic-header">
        <span>阅读导航</span>
        <span class="topic-count">{{ currentIndex + 1 }} / {{ documents.length }}</span>
      </div>
      <div class="topic-nav">
        <el-button :disabled="!previousDocument" @click="$emit('select', previousDocument)">上一篇</el-button>
        <el-button type="primary" :disabled="!nextDocument" @click="$emit('select', nextDocument)">下一篇</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: "TopicDocumentNavigator",
  props: {
    title: {
      type: String,
      default: "专题目录"
    },
    documents: {
      type: Array,
      default: () => []
    },
    currentDocId: {
      type: [Number, String],
      default: undefined
    }
  },
  computed: {
    currentIndex() {
      const index = this.documents.findIndex(item => item.docId === this.currentDocId)
      return index < 0 ? 0 : index
    },
    previousDocument() {
      return this.currentIndex > 0 ? this.documents[this.currentIndex - 1] : null
    },
    nextDocument() {
      return this.currentIndex < this.documents.length - 1 ? this.documents[this.currentIndex + 1] : null
    }
  }
}
</script>

<style lang="scss" scoped>
.topic-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topic-count {
  font-size: 12px;
  color: #909399;
}

.topic-doc-item {
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
}

.topic-doc-item:last-child {
  border-bottom: none;
}

.topic-doc-item.is-active .topic-doc-item__title {
  color: #409eff;
}

.topic-doc-item__title {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  font-weight: 500;
  color: #303133;
}

.topic-doc-item__index {
  width: 20px;
  color: #909399;
  text-align: center;
}

.topic-doc-item__meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.topic-nav {
  display: flex;
  justify-content: space-between;
}
</style>

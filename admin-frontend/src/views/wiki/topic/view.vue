<template>
  <div class="wiki-topic-page app-container">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="8" :lg="7">
        <el-card shadow="never" class="box-card">
          <div slot="header" class="topic-header">
            <span>{{ topic.topicName || "专题文档" }}</span>
          </div>
          <div class="topic-desc">{{ topic.description || "暂无专题描述" }}</div>
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="知识库">{{ topic.kbName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="文档数">{{ (topic.documents || []).length }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ topic.status === "enabled" ? "启用" : "停用" }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <topic-document-navigator
          title="专题目录"
          :documents="topic.documents || []"
          :current-doc-id="currentDoc.docId"
          @select="openDocument"
        />
      </el-col>

      <el-col :xs="24" :sm="24" :md="16" :lg="17">
        <document-reader
          title="专题内容"
          :document="currentDoc"
          empty-text="请从左侧选择专题文档"
        >
          <template #extra>
            <el-button-group>
              <el-button size="mini" :disabled="!previousDocument" @click="openDocument(previousDocument)">上一篇</el-button>
              <el-button size="mini" :disabled="!nextDocument" @click="openDocument(nextDocument)">下一篇</el-button>
              <el-button size="mini" @click="$router.push('/wiki/index')">返回知识库</el-button>
            </el-button-group>
          </template>
        </document-reader>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getTopicVisible } from "@/api/wiki/topic"
import DocumentReader from "../components/DocumentReader"
import TopicDocumentNavigator from "./components/TopicDocumentNavigator"

export default {
  name: "WikiTopicView",
  components: { DocumentReader, TopicDocumentNavigator },
  data() {
    return {
      topic: {},
      currentDoc: {}
    }
  },
  computed: {
    topicDocuments() {
      return this.topic.documents || []
    },
    currentIndex() {
      const index = this.topicDocuments.findIndex(item => item.docId === this.currentDoc.docId)
      return index < 0 ? 0 : index
    },
    previousDocument() {
      return this.currentIndex > 0 ? this.topicDocuments[this.currentIndex - 1] : null
    },
    nextDocument() {
      return this.currentIndex < this.topicDocuments.length - 1 ? this.topicDocuments[this.currentIndex + 1] : null
    }
  },
  created() {
    this.loadTopic()
  },
  methods: {
    loadTopic() {
      getTopicVisible(this.$route.params.topicId).then(response => {
        this.topic = response.data || {}
        const docs = this.topic.documents || []
        const routeDocId = Number(this.$route.query.docId)
        const targetDoc = docs.find(item => item.docId === routeDocId)
        this.currentDoc = targetDoc || (docs.length ? docs[0] : {})
      })
    },
    openDocument(item) {
      this.currentDoc = item || {}
      if (item && item.docId) {
        this.$router.replace({
          path: this.$route.path,
          query: { docId: item.docId }
        })
      }
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

.topic-desc {
  margin-bottom: 16px;
  color: #606266;
  line-height: 1.7;
}
</style>

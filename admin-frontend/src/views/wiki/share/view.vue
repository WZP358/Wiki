<template>
  <div class="wiki-public-page">
    <div class="wiki-public-page__main">
      <el-card shadow="never" class="box-card">
        <div slot="header" class="page-header">
          <span>分享文档</span>
          <el-button size="mini" @click="$router.push('/wiki/index')">返回知识库</el-button>
        </div>
        <div v-if="errorMessage" class="page-error">
          <el-empty :description="errorMessage" :image-size="88" />
        </div>
        <document-reader
          v-else
          title="分享内容"
          :document="document"
          empty-text="正在加载分享内容"
        />
      </el-card>
    </div>
  </div>
</template>

<script>
import { openShare } from "@/api/wiki/share"
import DocumentReader from "../components/DocumentReader"

export default {
  name: "WikiShareView",
  components: { DocumentReader },
  data() {
    return {
      document: {},
      errorMessage: ""
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    loadData() {
      openShare(this.$route.params.shareCode).then(response => {
        this.document = response.data || {}
        this.errorMessage = ""
      }).catch(error => {
        this.document = {}
        this.errorMessage = error && error.message ? error.message : "分享内容无法访问"
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.wiki-public-page {
  min-height: 100vh;
  padding: 24px;
  background: #f5f7fa;
}

.wiki-public-page__main {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-error {
  padding: 24px 0;
}
</style>

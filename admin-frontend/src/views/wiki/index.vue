<template>
  <div class="app-container wiki-portal">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="8" :lg="7">
        <el-card shadow="never" class="box-card">
          <div slot="header" class="clearfix">
            <span>专题推荐</span>
          </div>
          <el-select
            v-model="topicKbId"
            clearable
            size="small"
            placeholder="按知识库筛选专题"
            style="width: 100%; margin-bottom: 12px"
          >
            <el-option v-for="item in topicKbOptions" :key="item.kbId" :label="item.kbName" :value="item.kbId" />
          </el-select>
          <div v-if="filteredTopicList.length">
            <div
              v-for="item in filteredTopicList"
              :key="'topic-' + item.topicId"
              class="wiki-list-item"
              @click="openTopic(item.topicId)"
            >
              <div class="wiki-list-item__title">{{ item.topicName }}</div>
              <div class="wiki-list-item__meta">
                <span>{{ item.kbName || "未归属知识库" }}</span>
                <span>{{ item.docCount || 0 }} 篇文档</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无专题" :image-size="60" />
        </el-card>

        <el-card shadow="never" class="box-card">
          <div slot="header" class="clearfix">
            <span>文档检索</span>
          </div>
          <el-input
            v-model="keyword"
            clearable
            placeholder="请输入标题或正文关键字"
            @keyup.enter.native="handleSearch"
          >
            <el-button slot="append" icon="el-icon-search" @click="handleSearch" />
          </el-input>
          <div class="wiki-tip">这里只提供文档查阅与检索能力。</div>
        </el-card>

        <el-card shadow="never" class="box-card">
          <div slot="header" class="clearfix">
            <span>文档目录</span>
          </div>
          <el-tree
            v-if="treeData.length"
            :data="treeData"
            node-key="docId"
            default-expand-all
            :expand-on-click-node="false"
            :props="{ label: 'title', children: 'children' }"
            @node-click="handleNodeClick"
          />
          <el-empty v-else description="暂无可浏览文档" :image-size="72" />
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="16" :lg="17">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-card shadow="never" class="box-card">
              <div slot="header" class="clearfix">
                <span>最近更新</span>
              </div>
              <div v-if="latestList.length">
                <div
                  v-for="item in latestList"
                  :key="'latest-' + item.docId"
                  class="wiki-list-item"
                  @click="openDocument(item.docId)"
                >
                  <div class="wiki-list-item__title">{{ item.title }}</div>
                  <div class="wiki-list-item__meta">
                    <span>{{ item.ownerName || "系统用户" }}</span>
                    <span>{{ parseTime(item.updateTime) }}</span>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无数据" :image-size="60" />
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-card shadow="never" class="box-card">
              <div slot="header" class="clearfix">
                <span>热门阅读</span>
              </div>
              <div v-if="hotList.length">
                <div
                  v-for="item in hotList"
                  :key="'hot-' + item.docId"
                  class="wiki-list-item"
                  @click="openDocument(item.docId)"
                >
                  <div class="wiki-list-item__title">{{ item.title }}</div>
                  <div class="wiki-list-item__meta">
                    <span>{{ item.viewCount || 0 }} 次阅读</span>
                    <span>{{ item.ownerName || "系统用户" }}</span>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无数据" :image-size="60" />
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" class="box-card">
          <div slot="header" class="clearfix">
            <span>文档概览</span>
          </div>
          <el-row :gutter="16">
            <el-col :xs="12" :sm="6">
              <div class="wiki-stat">
                <div class="wiki-stat__value">{{ stats.totalDocs || 0 }}</div>
                <div class="wiki-stat__label">文档总数</div>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="wiki-stat">
                <div class="wiki-stat__value">{{ stats.publishedDocs || 0 }}</div>
                <div class="wiki-stat__label">已发布</div>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="wiki-stat">
                <div class="wiki-stat__value">{{ stats.draftDocs || 0 }}</div>
                <div class="wiki-stat__label">草稿</div>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="wiki-stat">
                <div class="wiki-stat__value">{{ stats.deptDocs || 0 }}</div>
                <div class="wiki-stat__label">部门可见</div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card shadow="never" class="box-card">
          <div slot="header" class="clearfix wiki-article-header">
            <span>文档内容</span>
            <el-button size="mini" :disabled="!currentDoc.docId" @click="showVersions">版本历史</el-button>
          </div>
          <div v-if="currentDoc.docId">
            <div class="wiki-article-title">{{ currentDoc.title }}</div>
            <div class="wiki-article-meta">
              <el-tag size="mini" type="info">{{ visibilityText(currentDoc.visibility) }}</el-tag>
              <el-tag size="mini" :type="currentDoc.status === 'published' ? 'success' : 'warning'">
                {{ statusText(currentDoc.status) }}
              </el-tag>
              <span>{{ currentDoc.ownerName || "系统用户" }}</span>
              <span v-if="currentDoc.deptName">{{ currentDoc.deptName }}</span>
              <span>{{ currentDoc.viewCount || 0 }} 次阅读</span>
            </div>
            <div v-if="currentDoc.summary" class="wiki-summary">{{ currentDoc.summary }}</div>
            <div class="wiki-content" v-html="currentDoc.htmlContent || '<p>-</p>'"></div>
          </div>
          <el-empty v-else description="请从左侧目录或上方推荐中打开文档" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <el-drawer title="版本历史" :visible.sync="versionOpen" size="40%" append-to-body>
      <el-timeline>
        <el-timeline-item
          v-for="item in versions"
          :key="item.versionId"
          :timestamp="parseTime(item.createTime)"
        >
          <div class="wiki-version__title">v{{ item.versionNo }} {{ item.title }}</div>
          <div class="wiki-version__desc">{{ item.editorName }} · {{ item.commitMessage || "文档更新" }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script>
import {
  getHotWikiDocuments,
  getLatestWikiDocuments,
  getWikiDocument,
  getWikiStats,
  getWikiTree,
  getWikiVersions,
  searchWikiDocuments
} from "@/api/wiki/document"
import { listTopicVisible } from "@/api/wiki/topic"

export default {
  name: "WikiIndex",
  data() {
    return {
      keyword: "",
      flatDocs: [],
      treeData: [],
      latestList: [],
      hotList: [],
      topicList: [],
      topicKbId: undefined,
      stats: {},
      currentDoc: {},
      versionOpen: false,
      versions: []
    }
  },
  computed: {
    topicKbOptions() {
      const map = new Map()
      ;(this.topicList || []).forEach(item => {
        if (item.kbId && !map.has(item.kbId)) {
          map.set(item.kbId, { kbId: item.kbId, kbName: item.kbName || "未归属知识库" })
        }
      })
      return Array.from(map.values())
    },
    filteredTopicList() {
      if (!this.topicKbId) {
        return this.topicList
      }
      return (this.topicList || []).filter(item => item.kbId === this.topicKbId)
    }
  },
  created() {
    this.refreshAll()
  },
  methods: {
    refreshAll() {
      this.loadTree()
      getLatestWikiDocuments().then(res => {
        this.latestList = res.data || []
        if (!this.currentDoc.docId && this.latestList.length) {
          this.openDocument(this.latestList[0].docId)
        }
      })
      getHotWikiDocuments().then(res => {
        this.hotList = res.data || []
      })
      listTopicVisible().then(res => {
        this.topicList = res.data || []
      })
      getWikiStats().then(res => {
        this.stats = res.data || {}
      })
    },
    loadTree() {
      getWikiTree().then(res => {
        this.flatDocs = res.data || []
        this.treeData = this.buildTree(this.flatDocs)
        if (!this.currentDoc.docId && this.flatDocs.length) {
          this.openDocument(this.flatDocs[0].docId)
        }
      })
    },
    buildTree(list) {
      const map = {}
      const roots = []
      list.forEach(item => {
        map[item.docId] = { ...item, children: [] }
      })
      list.forEach(item => {
        const node = map[item.docId]
        if (item.parentId && item.parentId !== 0 && map[item.parentId]) {
          map[item.parentId].children.push(node)
        } else {
          roots.push(node)
        }
      })
      return roots
    },
    handleSearch() {
      if (!this.keyword) {
        this.loadTree()
        return
      }
      searchWikiDocuments(this.keyword).then(res => {
        this.flatDocs = res.data || []
        this.treeData = this.buildTree(this.flatDocs)
        if (this.flatDocs.length) {
          this.openDocument(this.flatDocs[0].docId)
        } else {
          this.currentDoc = {}
        }
      })
    },
    handleNodeClick(data) {
      this.openDocument(data.docId)
    },
    openDocument(docId) {
      getWikiDocument(docId).then(res => {
        this.currentDoc = res.data || {}
      })
    },
    openTopic(topicId) {
      this.$router.push("/wiki/topic/view/" + topicId)
    },
    showVersions() {
      if (!this.currentDoc.docId) {
        return
      }
      getWikiVersions(this.currentDoc.docId).then(res => {
        this.versions = res.data || []
        this.versionOpen = true
      })
    },
    visibilityText(value) {
      return { public: "公开", department: "部门可见", private: "私有" }[value] || value
    },
    statusText(value) {
      return { draft: "草稿", published: "已发布" }[value] || value
    }
  }
}
</script>

<style lang="scss" scoped>
.wiki-portal {
  padding-bottom: 20px;
}

.box-card {
  margin-bottom: 20px;
}

.wiki-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.wiki-list-item {
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
}

.wiki-list-item:last-child {
  border-bottom: none;
}

.wiki-list-item__title {
  margin-bottom: 6px;
  color: #303133;
  font-weight: 500;
}

.wiki-list-item__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.wiki-stat {
  padding: 16px;
  text-align: center;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.wiki-stat__value {
  font-size: 24px;
  line-height: 1.4;
  color: #303133;
}

.wiki-stat__label {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.wiki-article-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.wiki-article-title {
  margin-bottom: 12px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.wiki-article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  color: #909399;
  font-size: 13px;
}

.wiki-summary {
  margin-bottom: 16px;
  padding: 12px 16px;
  border-left: 4px solid #409eff;
  background: #f4faff;
  color: #606266;
}

.wiki-content {
  color: #303133;
  line-height: 1.8;
}

.wiki-content ::v-deep pre {
  padding: 12px;
  overflow-x: auto;
  background: #f5f7fa;
}

.wiki-content ::v-deep img {
  max-width: 100%;
}

.wiki-version__title {
  font-weight: 500;
  color: #303133;
}

.wiki-version__desc {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}
</style>

<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="文档标题/内容" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="知识库ID" prop="kbId">
        <el-input v-model="queryParams.kbId" placeholder="知识库ID" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="可见性" prop="visibility">
        <el-select v-model="queryParams.visibility" placeholder="可见性" clearable style="width: 150px">
          <el-option label="公开" value="PUBLIC" />
          <el-option label="团队" value="TEAM" />
          <el-option label="私有" value="PRIVATE" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="deleted">
        <el-select v-model="queryParams.deleted" placeholder="删除状态" clearable style="width: 150px">
          <el-option label="正常" :value="false" />
          <el-option label="已删除" :value="true" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="single" @click="handleDelete">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-refresh-left" size="mini" :disabled="single" @click="handleRestore">恢复</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="docList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文档ID" align="center" prop="id" width="170" />
      <el-table-column label="标题" align="center" prop="title" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="知识库ID" align="center" prop="kbId" width="170" />
      <el-table-column label="作者ID" align="center" prop="ownerId" width="170" />
      <el-table-column label="可见性" align="center" prop="visibility" width="100" />
      <el-table-column label="发布" align="center" prop="published" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.published ? 'success' : 'info'">{{ scope.row.published ? '已发布' : '草稿' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="deleted" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.deleted ? 'info' : 'success'">{{ scope.row.deleted ? '已删除' : '正常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="浏览量" align="center" prop="viewCount" width="90" />
      <el-table-column label="版本" align="center" prop="versionNo" width="80" />
      <el-table-column label="更新时间" align="center" prop="updatedAt" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updatedAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button v-if="!scope.row.deleted" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
          <el-button v-else size="mini" type="text" icon="el-icon-refresh-left" @click="handleRestore(scope.row)">恢复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { docAction, listDocs } from '@/api/wiki-admin'

export default {
  name: 'WikiAdminDocs',
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      showSearch: true,
      total: 0,
      docList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        keyword: undefined,
        kbId: undefined,
        visibility: undefined,
        deleted: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listDocs(this.queryParams).then(res => {
        this.docList = res.rows
        this.total = res.total
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
    },
    handleDelete(row) {
      const docId = row.id || this.ids[0]
      this.$modal.confirm('确认删除选中的文档吗？').then(() => {
        return docAction({ docId, deleted: true, reason: '后台删除' })
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    handleRestore(row) {
      const docId = row.id || this.ids[0]
      docAction({ docId, deleted: false, reason: '后台恢复' }).then(() => {
        this.$modal.msgSuccess('恢复成功')
        this.getList()
      })
    }
  }
}
</script>

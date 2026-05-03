<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="知识库名称/描述" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="知识库类型" clearable style="width: 160px">
          <el-option label="公司公开" value="COMPANY" />
          <el-option label="团队" value="DEPARTMENT" />
          <el-option label="私有" value="PRIVATE" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="deleted">
        <el-select v-model="queryParams.deleted" placeholder="状态" clearable style="width: 160px">
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

    <el-table v-loading="loading" :data="kbList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="知识库ID" align="center" prop="id" width="170" />
      <el-table-column label="名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" prop="type" width="120" />
      <el-table-column label="创建者ID" align="center" prop="ownerId" width="170" />
      <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="deleted" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.deleted ? 'info' : 'success'">{{ scope.row.deleted ? '已删除' : '正常' }}</el-tag>
        </template>
      </el-table-column>
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
import { listKbs, kbAction } from '@/api/wiki-admin'

export default {
  name: 'WikiAdminKbs',
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      showSearch: true,
      total: 0,
      kbList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        keyword: undefined,
        type: undefined,
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
      listKbs(this.queryParams).then(res => {
        this.kbList = res.rows
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
      const kbId = row.id || this.ids[0]
      this.$modal.confirm('确认删除选中的知识库吗？').then(() => {
        return kbAction({ kbId, deleted: true, reason: '后台删除' })
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    handleRestore(row) {
      const kbId = row.id || this.ids[0]
      kbAction({ kbId, deleted: false, reason: '后台恢复' }).then(() => {
        this.$modal.msgSuccess('恢复成功')
        this.getList()
      })
    }
  }
}
</script>

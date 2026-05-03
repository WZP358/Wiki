<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户ID" prop="userId">
        <el-input v-model="queryParams.userId" placeholder="用户ID" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="操作" prop="action">
        <el-input v-model="queryParams.action" placeholder="操作类型" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="目标类型" prop="targetType">
        <el-input v-model="queryParams.targetType" placeholder="目标类型" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="logList">
      <el-table-column label="日志ID" align="center" prop="id" width="170" />
      <el-table-column label="用户ID" align="center" prop="userId" width="170" />
      <el-table-column label="用户名" align="center" prop="username" width="120" />
      <el-table-column label="操作" align="center" prop="action" width="160" />
      <el-table-column label="目标类型" align="center" prop="targetType" width="120" />
      <el-table-column label="目标ID" align="center" prop="targetId" width="170" />
      <el-table-column label="IP" align="center" prop="ip" width="140" />
      <el-table-column label="详情" align="center" prop="detail" :show-overflow-tooltip="true" />
      <el-table-column label="时间" align="center" prop="createdAt" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createdAt) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listLogs } from '@/api/wiki-admin'

export default {
  name: 'WikiAdminLogs',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      logList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: undefined,
        action: undefined,
        targetType: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listLogs(this.queryParams).then(res => {
        this.logList = res.rows
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
    }
  }
}
</script>

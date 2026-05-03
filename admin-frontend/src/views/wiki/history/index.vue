<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" label-width="68px" v-show="showSearch">
      <el-form-item label="用户账号" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户账号" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="文档标题" prop="docTitle">
        <el-input v-model="queryParams.docTitle" placeholder="请输入文档标题" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="知识库" prop="kbId">
        <el-select v-model="queryParams.kbId" placeholder="请选择知识库" clearable filterable style="width: 240px">
          <el-option v-for="item in kbOptions" :key="item.kbId" :label="item.kbName" :value="item.kbId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['wiki:history:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-delete-solid" size="mini" @click="handleClean" v-hasPermi="['wiki:history:remove']">清空</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="historyList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="记录编号" align="center" prop="historyId" width="110" />
      <el-table-column label="用户账号" align="center" prop="userName" width="140" />
      <el-table-column label="用户昵称" align="center" prop="nickName" width="140" />
      <el-table-column label="文档标题" align="center" prop="docTitle" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="知识库" align="center" prop="kbName" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="文档作者" align="center" prop="ownerName" width="120" />
      <el-table-column label="浏览次数" align="center" prop="browseCount" width="90" />
      <el-table-column label="最近浏览" align="center" prop="updateTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['wiki:history:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import RightToolbar from "@/components/RightToolbar"
import { listKb } from "@/api/wiki/kb"
import { cleanAdminHistory, deleteAdminHistory, listAdminHistories } from "@/api/wiki/workbenchAdmin"

export default {
  name: "WikiHistory",
  components: { RightToolbar },
  data() {
    return {
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      total: 0,
      historyList: [],
      kbOptions: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        docTitle: undefined,
        kbId: undefined
      }
    }
  },
  created() {
    this.getList()
    this.getKbOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listAdminHistories(this.queryParams).then(response => {
        this.historyList = response.rows || []
        this.total = response.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    getKbOptions() {
      listKb({ pageNum: 1, pageSize: 1000, status: "enabled" }).then(response => {
        this.kbOptions = response.rows || []
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.historyId)
      this.multiple = !selection.length
    },
    handleDelete(row) {
      const historyIds = row.historyId || this.ids
      this.$modal.confirm("是否确认删除选中的浏览记录？").then(() => {
        return deleteAdminHistory(historyIds)
      }).then(() => {
        this.$modal.msgSuccess("删除成功")
        this.getList()
      }).catch(() => {})
    },
    handleClean() {
      this.$modal.confirm("是否确认清空全部浏览记录？").then(() => {
        return cleanAdminHistory()
      }).then(() => {
        this.$modal.msgSuccess("清空成功")
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>

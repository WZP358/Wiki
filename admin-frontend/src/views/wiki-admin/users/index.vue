<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="用户名 / 昵称 / 邮箱" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="queryParams.role" placeholder="用户角色" clearable style="width: 160px">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="普通用户" value="USER" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="active">
        <el-select v-model="queryParams.active" placeholder="账号状态" clearable style="width: 160px">
          <el-option label="正常" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate">修改</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户ID" align="center" prop="id" width="170" />
      <el-table-column label="用户名" align="center" prop="username" />
      <el-table-column label="昵称" align="center" prop="nickname" />
      <el-table-column label="邮箱" align="center" prop="email" :show-overflow-tooltip="true" />
      <el-table-column label="手机" align="center" prop="phone" />
      <el-table-column label="团队" align="center" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.pendingAssignment" type="warning">待分配</el-tag>
          <span v-else>{{ teamText(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="角色" align="center" prop="role">
        <template slot-scope="scope">
          <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'info'">{{ scope.row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="active">
        <template slot-scope="scope">
          <el-tag :type="scope.row.active ? 'success' : 'info'">{{ scope.row.active ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updatedAt" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updatedAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="540px" append-to-body>
      <el-form ref="form" :model="form" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="form.role === 'ADMIN'"
          title="系统管理员负责平台治理，不参与普通知识库协作，保存后将清空团队归属。"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 14px"
        />
        <el-form-item v-else label="团队">
          <el-select v-model="form.teamIds" multiple filterable placeholder="请选择一个或多个细分团队" style="width: 100%">
            <el-option v-for="team in leafTeamOptions" :key="team.id" :label="team.name" :value="team.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.active">
            <el-radio :label="true">正常</el-radio>
            <el-radio :label="false">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDepartments, listUsers, updateUser } from '@/api/wiki-admin'

export default {
  name: 'WikiAdminUsers',
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      showSearch: true,
      total: 0,
      userList: [],
      departmentOptions: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        keyword: undefined,
        role: undefined,
        active: undefined
      },
      form: {}
    }
  },
  computed: {
    leafTeamOptions() {
      const parentIds = new Set((this.departmentOptions || []).map(item => item.parentId).filter(Boolean))
      return (this.departmentOptions || []).filter(item => !parentIds.has(item.id))
    }
  },
  created() {
    this.getList()
    this.loadTeams()
  },
  methods: {
    getList() {
      this.loading = true
      listUsers(this.queryParams).then(res => {
        this.userList = res.rows
        this.total = res.total
      }).finally(() => {
        this.loading = false
      })
    },
    loadTeams() {
      listDepartments({ active: true }).then(res => {
        this.departmentOptions = res.rows || []
      })
    },
    teamText(row) {
      if (Array.isArray(row.teamNames) && row.teamNames.length > 0) {
        return row.teamNames.join('、')
      }
      return row.departmentName || '-'
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
    handleUpdate(row) {
      const data = row || this.userList.find(item => item.id === this.ids[0])
      if (!data) {
        this.$modal.msgWarning('请选择要修改的用户')
        return
      }
      this.form = { ...data, teamIds: Array.isArray(data.teamIds) ? data.teamIds.slice() : [] }
      this.open = true
      this.title = '修改用户'
    },
    submitForm() {
      if (this.form.role === 'USER' && (!Array.isArray(this.form.teamIds) || this.form.teamIds.length === 0)) {
        this.$modal.msgWarning('普通用户必须至少分配一个细分团队')
        return
      }
      updateUser({
        userId: this.form.id,
        role: this.form.role,
        teamIds: this.form.role === 'ADMIN' ? [] : (this.form.teamIds || []),
        active: this.form.active
      }).then(() => {
        this.$modal.msgSuccess('修改成功')
        this.open = false
        this.getList()
      })
    },
    cancel() {
      this.open = false
      this.form = {}
    }
  }
}
</script>

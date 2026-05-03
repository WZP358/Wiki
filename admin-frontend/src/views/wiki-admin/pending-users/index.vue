<template>
  <div class="app-container">
    <el-alert
      title="新注册用户默认没有团队和知识库权限，需要管理员分配到一个或多个细分团队后再进入正常协作流程。"
      type="warning"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="用户名 / 昵称 / 邮箱 / 手机号"
          clearable
          style="width: 260px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-finished" size="mini" :disabled="single" @click="handleAssign">
          分配权限
        </el-button>
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
      <el-table-column label="注册时间" align="center" prop="createdAt" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="120">
        <template>
          <el-tag type="warning">待分配</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-finished" @click="handleAssign(scope.row)">分配</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="用户">
          <el-input :value="form.username" disabled />
        </el-form-item>
        <el-form-item label="归属团队" prop="teamIds">
          <el-select
            v-model="form.teamIds"
            multiple
            filterable
            placeholder="请选择一个或多个细分团队"
            style="width: 100%"
          >
            <el-option
              v-for="team in leafTeamOptions"
              :key="team.id"
              :label="team.name"
              :value="team.id"
            />
          </el-select>
        </el-form-item>
        <el-alert
          title="只能选择没有下级团队的细分团队；大团队用于组织归类，不直接分配给用户。"
          type="info"
          :closable="false"
          class="mb8"
        />
        <el-form-item label="可见知识库">
          <el-select v-model="form.kbId" clearable filterable placeholder="可选：同时加入一个知识库" style="width: 100%">
            <el-option
              v-for="kb in kbOptions"
              :key="kb.id"
              :label="kb.name + '（' + kb.type + '）'"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库角色" v-if="form.kbId">
          <el-radio-group v-model="form.memberRole">
            <el-radio label="READER">查看者</el-radio>
            <el-radio label="EDITOR">编辑者</el-radio>
            <el-radio label="ADMIN">管理员</el-radio>
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
import { assignPendingUser, listDepartments, listKbs, listPendingUsers } from '@/api/wiki-admin'

export default {
  name: 'WikiAdminPendingUsers',
  data() {
    return {
      loading: true,
      showSearch: true,
      single: true,
      ids: [],
      total: 0,
      userList: [],
      departmentOptions: [],
      kbOptions: [],
      open: false,
      title: '',
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        keyword: undefined
      },
      form: {},
      rules: {
        teamIds: [{ required: true, type: 'array', min: 1, message: '请选择至少一个细分团队', trigger: 'change' }]
      }
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
    this.loadOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listPendingUsers(this.queryParams).then(res => {
        this.userList = res.rows
        this.total = res.total
      }).finally(() => {
        this.loading = false
      })
    },
    loadOptions() {
      listDepartments({ active: true }).then(res => {
        this.departmentOptions = res.rows || []
      })
      listKbs({ pageNum: 1, pageSize: 1000, deleted: false }).then(res => {
        this.kbOptions = res.rows || []
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
    handleAssign(row) {
      const data = row || this.userList.find(item => item.id === this.ids[0])
      if (!data) return
      this.form = {
        userId: data.id,
        username: data.username,
        teamIds: [],
        kbId: undefined,
        memberRole: 'READER'
      }
      this.open = true
      this.title = '分配新用户权限'
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        assignPendingUser({
          userId: this.form.userId,
          teamIds: this.form.teamIds,
          kbId: this.form.kbId || null,
          memberRole: this.form.kbId ? this.form.memberRole : null
        }).then(() => {
          this.$modal.msgSuccess('分配成功')
          this.open = false
          this.getList()
        })
      })
    },
    cancel() {
      this.open = false
      this.form = {}
    }
  }
}
</script>

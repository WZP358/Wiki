<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="状态" prop="active">
        <el-select v-model="queryParams.active" placeholder="部门状态" clearable style="width: 160px">
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="deptList" row-key="id" default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column label="部门名称" prop="name" min-width="220" />
      <el-table-column label="部门ID" align="center" prop="id" width="170" />
      <el-table-column label="上级部门ID" align="center" prop="parentId" width="170" />
      <el-table-column label="负责人ID" align="center" prop="managerId" width="170" />
      <el-table-column label="状态" align="center" prop="active" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.active ? 'success' : 'info'">{{ scope.row.active ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-switch-button" @click="handleActive(scope.row)">
            {{ scope.row.active ? '停用' : '启用' }}
          </el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="上级部门ID">
          <el-input v-model="form.parentId" placeholder="留空表示顶级部门" />
        </el-form-item>
        <el-form-item label="负责人ID">
          <el-input v-model="form.managerId" placeholder="留空表示无负责人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.description" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { addDepartment, deleteDepartment, listDepartments, setDepartmentActive, updateDepartment } from '@/api/wiki-admin'
import { handleTree } from '@/utils/ruoyi'

export default {
  name: 'WikiAdminDepartments',
  data() {
    return {
      loading: true,
      showSearch: true,
      deptList: [],
      title: '',
      open: false,
      queryParams: {
        active: undefined
      },
      form: {},
      rules: {
        name: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listDepartments(this.queryParams).then(res => {
        this.deptList = handleTree(res.rows, 'id', 'parentId')
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    reset() {
      this.form = {
        id: undefined,
        name: undefined,
        parentId: undefined,
        managerId: undefined,
        description: undefined
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增部门'
    },
    handleUpdate(row) {
      this.form = { ...row }
      this.open = true
      this.title = '修改部门'
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const payload = {
          name: this.form.name,
          parentId: this.form.parentId || null,
          managerId: this.form.managerId || null,
          description: this.form.description
        }
        const req = this.form.id ? updateDepartment(this.form.id, payload) : addDepartment(payload)
        req.then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleActive(row) {
      setDepartmentActive(row.id, !row.active).then(() => {
        this.$modal.msgSuccess('操作成功')
        this.getList()
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除该部门吗？').then(() => {
        return deleteDepartment(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    cancel() {
      this.open = false
      this.reset()
    }
  }
}
</script>

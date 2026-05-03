<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" label-width="68px" v-show="showSearch">
      <el-form-item label="分组名称" prop="folderName">
        <el-input v-model="queryParams.folderName" placeholder="请输入分组名称" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="用户账号" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户账号" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 180px">
          <el-option label="启用" value="enabled" />
          <el-option label="停用" value="disabled" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['wiki:favoriteFolder:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['wiki:favoriteFolder:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['wiki:favoriteFolder:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="folderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="分组编号" align="center" prop="folderId" width="120" />
      <el-table-column label="分组名称" align="center" prop="folderName" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="用户账号" align="center" prop="userName" width="120" />
      <el-table-column label="用户昵称" align="center" prop="nickName" width="120" />
      <el-table-column label="排序" align="center" prop="sortNum" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === 'enabled' ? 'success' : 'info'">
            {{ scope.row.status === "enabled" ? "启用" : "停用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['wiki:favoriteFolder:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['wiki:favoriteFolder:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户编号" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户编号" />
        </el-form-item>
        <el-form-item label="分组名称" prop="folderName">
          <el-input v-model="form.folderName" placeholder="请输入分组名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="enabled">启用</el-radio>
            <el-radio label="disabled">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortNum">
          <el-input-number v-model="form.sortNum" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
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
import RightToolbar from "@/components/RightToolbar"
import { addFavoriteFolder, delFavoriteFolder, getFavoriteFolder, listFavoriteFolder, updateFavoriteFolder } from "@/api/wiki/favoriteFolder"

export default {
  name: "WikiFavoriteFolder",
  components: { RightToolbar },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      folderList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        folderName: undefined,
        userName: undefined,
        status: undefined
      },
      form: {},
      rules: {
        userId: [{ required: true, message: "用户编号不能为空", trigger: "blur" }],
        folderName: [{ required: true, message: "分组名称不能为空", trigger: "blur" }],
        status: [{ required: true, message: "状态不能为空", trigger: "change" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listFavoriteFolder(this.queryParams).then(response => {
        this.folderList = response.rows || []
        this.total = response.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    reset() {
      this.form = {
        folderId: undefined,
        userId: undefined,
        folderName: undefined,
        sortNum: 0,
        status: "enabled",
        remark: undefined
      }
      this.resetForm("form")
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
      this.ids = selection.map(item => item.folderId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增收藏夹分组"
    },
    handleUpdate(row) {
      const folderId = row.folderId || this.ids[0]
      this.reset()
      getFavoriteFolder(folderId).then(response => {
        this.form = response.data || {}
        this.open = true
        this.title = "修改收藏夹分组"
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.folderId ? updateFavoriteFolder(this.form) : addFavoriteFolder(this.form)
        request.then(() => {
          this.$modal.msgSuccess(this.form.folderId ? "修改成功" : "新增成功")
          this.open = false
          this.getList()
        })
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleDelete(row) {
      const folderIds = row.folderId || this.ids
      this.$modal.confirm("是否确认删除选中的收藏夹分组？删除后原有收藏记录会保留，但不再归属该分组。").then(() => {
        return delFavoriteFolder(folderIds)
      }).then(() => {
        this.$modal.msgSuccess("删除成功")
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>

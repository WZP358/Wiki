<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" label-width="68px" v-show="showSearch">
      <el-form-item label="知识库" prop="kbId">
        <el-select v-model="queryParams.kbId" placeholder="请选择知识库" clearable filterable style="width: 240px">
          <el-option v-for="item in kbOptions" :key="item.kbId" :label="item.kbName" :value="item.kbId" />
        </el-select>
      </el-form-item>
      <el-form-item label="分组名称" prop="groupName">
        <el-input v-model="queryParams.groupName" placeholder="请输入分组名称" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['wiki:group:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['wiki:group:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="single" @click="handleDelete" v-hasPermi="['wiki:group:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="groupList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="分组编号" align="center" prop="groupId" width="110" />
      <el-table-column label="知识库" align="center" prop="kbName" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="分组名称" align="center" prop="groupName" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="分组编码" align="center" prop="groupCode" width="140" />
      <el-table-column label="文档数" align="center" prop="docCount" width="90" />
      <el-table-column label="排序" align="center" prop="sortNum" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === 'enabled' ? 'success' : 'info'">
            {{ scope.row.status === "enabled" ? "启用" : "停用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['wiki:group:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['wiki:group:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="知识库" prop="kbId">
          <el-select v-model="form.kbId" placeholder="请选择知识库" filterable style="width: 100%">
            <el-option v-for="item in kbOptions" :key="item.kbId" :label="item.kbName" :value="item.kbId" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组名称" prop="groupName">
          <el-input v-model="form.groupName" placeholder="请输入分组名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="分组编码" prop="groupCode">
          <el-input v-model="form.groupCode" placeholder="请输入分组编码" maxlength="64" />
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
import { listKb } from "@/api/wiki/kb"
import { addGroup, delGroup, getGroup, listGroup, updateGroup } from "@/api/wiki/group"

export default {
  name: "WikiGroup",
  components: { RightToolbar },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      showSearch: true,
      total: 0,
      groupList: [],
      kbOptions: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        kbId: undefined,
        groupName: undefined,
        status: undefined
      },
      form: {},
      rules: {
        kbId: [{ required: true, message: "请选择知识库", trigger: "change" }],
        groupName: [{ required: true, message: "分组名称不能为空", trigger: "blur" }]
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
      listGroup(this.queryParams).then(response => {
        this.groupList = response.rows || []
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
    reset() {
      this.form = {
        groupId: undefined,
        kbId: undefined,
        groupName: undefined,
        groupCode: undefined,
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
      this.ids = selection.map(item => item.groupId)
      this.single = selection.length !== 1
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增分组"
    },
    handleUpdate(row) {
      const groupId = row.groupId || this.ids[0]
      this.reset()
      getGroup(groupId).then(response => {
        this.form = response.data || {}
        this.open = true
        this.title = "修改分组"
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.groupId ? updateGroup(this.form) : addGroup(this.form)
        request.then(() => {
          this.$modal.msgSuccess(this.form.groupId ? "修改成功" : "新增成功")
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const groupId = row.groupId || this.ids[0]
      this.$modal.confirm("是否确认删除该分组？").then(() => {
        return delGroup(groupId)
      }).then(() => {
        this.$modal.msgSuccess("删除成功")
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

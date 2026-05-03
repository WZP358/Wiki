<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" label-width="68px" v-show="showSearch">
      <el-form-item label="用户账号" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户账号" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="小记标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入小记标题" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="归档状态" prop="archived">
        <el-select v-model="queryParams.archived" placeholder="请选择归档状态" clearable style="width: 180px">
          <el-option :value="0" label="未归档" />
          <el-option :value="1" label="已归档" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['wiki:note:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['wiki:note:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['wiki:note:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="noteList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="小记编号" align="center" prop="noteId" width="110" />
      <el-table-column label="用户账号" align="center" prop="userName" width="140" />
      <el-table-column label="用户昵称" align="center" prop="nickName" width="140" />
      <el-table-column label="小记标题" align="center" prop="title" min-width="200" :show-overflow-tooltip="true" />
      <el-table-column label="颜色" align="center" prop="color" width="100" />
      <el-table-column label="置顶" align="center" prop="pinned" width="80">
        <template slot-scope="scope">
          <span>{{ scope.row.pinned === 1 ? "是" : "否" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="归档状态" align="center" prop="archived" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.archived === 1 ? 'success' : 'info'">
            {{ scope.row.archived === 1 ? "已归档" : "未归档" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="归档知识库" align="center" prop="archiveKbName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['wiki:note:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-folder-add" @click="handleArchive(scope.row)" v-if="scope.row.archived !== 1" v-hasPermi="['wiki:note:archive']">归档</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['wiki:note:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户编号" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户编号" />
        </el-form-item>
        <el-form-item label="小记标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入小记标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入小记内容" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="颜色" prop="color">
              <el-select v-model="form.color" placeholder="请选择颜色" style="width: 100%">
                <el-option label="黄色" value="yellow" />
                <el-option label="蓝色" value="blue" />
                <el-option label="绿色" value="green" />
                <el-option label="红色" value="red" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="置顶" prop="pinned">
              <el-radio-group v-model="form.pinned">
                <el-radio :label="1">是</el-radio>
                <el-radio :label="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="归档小记" :visible.sync="archiveOpen" width="520px" append-to-body>
      <el-form ref="archiveForm" :model="archiveForm" :rules="archiveRules" label-width="100px">
        <el-form-item label="归档知识库" prop="archiveKbId">
          <el-select v-model="archiveForm.archiveKbId" placeholder="请选择知识库" filterable style="width: 100%">
            <el-option v-for="item in kbOptions" :key="item.kbId" :label="item.kbName" :value="item.kbId" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitArchive">确 定</el-button>
        <el-button @click="archiveOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import RightToolbar from "@/components/RightToolbar"
import { listKb } from "@/api/wiki/kb"
import { addAdminNote, archiveAdminNote, deleteAdminNote, getAdminNote, listAdminNotes, updateAdminNote } from "@/api/wiki/workbenchAdmin"

export default {
  name: "WikiNote",
  components: { RightToolbar },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      noteList: [],
      kbOptions: [],
      title: "",
      open: false,
      archiveOpen: false,
      currentNoteId: undefined,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        title: undefined,
        archived: undefined
      },
      form: {},
      archiveForm: {
        archiveKbId: undefined
      },
      rules: {
        userId: [{ required: true, message: "用户编号不能为空", trigger: "blur" }],
        title: [{ required: true, message: "小记标题不能为空", trigger: "blur" }]
      },
      archiveRules: {
        archiveKbId: [{ required: true, message: "请选择归档知识库", trigger: "change" }]
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
      listAdminNotes(this.queryParams).then(response => {
        this.noteList = response.rows || []
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
        noteId: undefined,
        userId: undefined,
        title: undefined,
        content: undefined,
        color: "yellow",
        pinned: 0,
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
      this.ids = selection.map(item => item.noteId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增小记"
    },
    handleUpdate(row) {
      this.reset()
      const noteId = row.noteId || this.ids[0]
      getAdminNote(noteId).then(response => {
        this.form = response.data || {}
        this.open = true
        this.title = "修改小记"
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.noteId ? updateAdminNote(this.form) : addAdminNote(this.form)
        request.then(() => {
          this.$modal.msgSuccess(this.form.noteId ? "修改成功" : "新增成功")
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
      const noteIds = row.noteId || this.ids
      this.$modal.confirm("是否确认删除选中的小记记录？").then(() => {
        return deleteAdminNote(noteIds)
      }).then(() => {
        this.$modal.msgSuccess("删除成功")
        this.getList()
      }).catch(() => {})
    },
    handleArchive(row) {
      this.currentNoteId = row.noteId
      this.archiveForm.archiveKbId = undefined
      this.archiveOpen = true
    },
    submitArchive() {
      this.$refs.archiveForm.validate(valid => {
        if (!valid) {
          return
        }
        archiveAdminNote(this.currentNoteId, this.archiveForm.archiveKbId).then(() => {
          this.$modal.msgSuccess("归档成功")
          this.archiveOpen = false
          this.getList()
        })
      })
    }
  }
}
</script>

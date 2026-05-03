<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" label-width="68px" v-show="showSearch">
      <el-form-item label="用户名称" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="文档标题" prop="docTitle">
        <el-input v-model="queryParams.docTitle" placeholder="请输入文档标题" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="知识库" prop="kbId">
        <el-select v-model="queryParams.kbId" placeholder="请选择知识库" clearable filterable style="width: 220px">
          <el-option v-for="item in kbOptions" :key="item.kbId" :label="item.kbName" :value="item.kbId" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option label="正常" value="normal" />
          <el-option label="隐藏" value="hidden" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['wiki:comment:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['wiki:comment:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['wiki:comment:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="commentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="评论编号" align="center" prop="commentId" width="110" />
      <el-table-column label="用户名称" align="center" prop="userName" width="140" />
      <el-table-column label="用户昵称" align="center" prop="nickName" width="140" />
      <el-table-column label="文档标题" align="center" prop="docTitle" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="知识库" align="center" prop="kbName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="评论内容" align="center" prop="content" min-width="260" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === 'normal' ? 'success' : 'info'">
            {{ scope.row.status === "normal" ? "正常" : "隐藏" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="点赞数" align="center" prop="likeCount" width="90" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['wiki:comment:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['wiki:comment:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户编号" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户编号" />
        </el-form-item>
        <el-form-item label="文档编号" prop="docId">
          <el-input v-model="form.docId" placeholder="请输入文档编号" />
        </el-form-item>
        <el-form-item label="上级评论" prop="parentId">
          <el-input v-model="form.parentId" placeholder="没有可留空" />
        </el-form-item>
        <el-form-item label="评论内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入评论内容" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="normal">正常</el-radio>
                <el-radio label="hidden">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="点赞数" prop="likeCount">
              <el-input-number v-model="form.likeCount" controls-position="right" :min="0" />
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
  </div>
</template>

<script>
import RightToolbar from "@/components/RightToolbar"
import { listKb } from "@/api/wiki/kb"
import { addAdminComment, deleteAdminComment, getAdminComment, listAdminComments, updateAdminComment } from "@/api/wiki/workbenchAdmin"

export default {
  name: "WikiComment",
  components: { RightToolbar },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      commentList: [],
      kbOptions: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        docTitle: undefined,
        kbId: undefined,
        status: undefined
      },
      form: {},
      rules: {
        userId: [{ required: true, message: "用户编号不能为空", trigger: "blur" }],
        docId: [{ required: true, message: "文档编号不能为空", trigger: "blur" }],
        content: [{ required: true, message: "评论内容不能为空", trigger: "blur" }]
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
      listAdminComments(this.queryParams).then(response => {
        this.commentList = response.rows || []
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
        commentId: undefined,
        userId: undefined,
        docId: undefined,
        parentId: undefined,
        content: undefined,
        status: "normal",
        likeCount: 0,
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
      this.ids = selection.map(item => item.commentId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增评论"
    },
    handleUpdate(row) {
      const commentId = row.commentId || this.ids[0]
      this.reset()
      getAdminComment(commentId).then(response => {
        this.form = response.data || {}
        this.open = true
        this.title = "修改评论"
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.commentId ? updateAdminComment(this.form) : addAdminComment(this.form)
        request.then(() => {
          this.$modal.msgSuccess(this.form.commentId ? "修改成功" : "新增成功")
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
      const commentIds = row.commentId || this.ids
      this.$modal.confirm("确认删除选中的评论数据项？").then(() => {
        return deleteAdminComment(commentIds)
      }).then(() => {
        this.$modal.msgSuccess("删除成功")
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>

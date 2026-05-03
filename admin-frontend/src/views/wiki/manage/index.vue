<template>
  <div class="app-container">
    <el-form
      ref="queryForm"
      :model="queryParams"
      :inline="true"
      size="small"
      label-width="68px"
      v-show="showSearch"
    >
      <el-form-item label="文档标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入文档标题"
          clearable
          style="width: 220px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="知识库" prop="kbId">
        <el-select
          v-model="queryParams.kbId"
          placeholder="请选择知识库"
          clearable
          filterable
          style="width: 220px"
          @change="handleKbChange"
        >
          <el-option v-for="item in kbOptions" :key="item.kbId" :label="item.kbName" :value="item.kbId" />
        </el-select>
      </el-form-item>
      <el-form-item label="分组" prop="groupId">
        <el-select v-model="queryParams.groupId" placeholder="请选择分组" clearable filterable style="width: 220px">
          <el-option v-for="item in queryGroupOptions" :key="item.groupId" :label="item.groupName" :value="item.groupId" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['wiki:doc:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['wiki:doc:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['wiki:doc:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-refresh-left" size="mini" @click="handleRecycle" v-hasPermi="['wiki:doc:remove']">回收站</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="docList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="文档ID" align="center" prop="docId" width="110" />
      <el-table-column label="文档标题" align="center" prop="title" min-width="220" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <span class="link-type" @click="handleView(scope.row)">{{ scope.row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="知识库" align="center" prop="kbName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="分组" align="center" prop="groupName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="标签" align="center" prop="tagNames" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="父文档" align="center" width="100">
        <template slot-scope="scope">
          <span>{{ scope.row.parentId && scope.row.parentId !== 0 ? scope.row.parentId : "无" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === 'published' ? 'success' : 'info'">
            {{ scope.row.status === "published" ? "已发布" : "草稿" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="作者" align="center" prop="ownerName" width="120" />
      <el-table-column label="浏览量" align="center" prop="viewCount" width="90" />
      <el-table-column label="最近更新" align="center" prop="updateTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['wiki:doc:query']">查看</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['wiki:doc:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['wiki:doc:remove']">删除</el-button>
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

    <el-dialog :title="title" :visible.sync="open" width="980px" append-to-body :before-close="handleDialogClose">
      <el-alert
        v-if="form.docId"
        :title="lockTip"
        :type="editLocked ? 'warning' : 'success'"
        :closable="false"
        show-icon
        class="mb16"
      />

      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="文档标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入文档标题" maxlength="100" :disabled="editLocked" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="知识库" prop="kbId">
              <el-select
                v-model="form.kbId"
                placeholder="请选择知识库"
                filterable
                style="width: 100%"
                :disabled="editLocked"
                @change="handleFormKbChange"
              >
                <el-option v-for="item in kbOptions" :key="item.kbId" :label="item.kbName" :value="item.kbId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分组" prop="groupId">
              <el-select
                v-model="form.groupId"
                placeholder="请选择分组"
                filterable
                style="width: 100%"
                :disabled="editLocked"
              >
                <el-option v-for="item in formGroupOptions" :key="item.groupId" :label="item.groupName" :value="item.groupId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父文档" prop="parentId">
              <el-select
                v-model="form.parentId"
                placeholder="请选择父文档"
                clearable
                filterable
                style="width: 100%"
                :disabled="editLocked"
              >
                <el-option :value="0" label="无父文档" />
                <el-option v-for="item in parentOptions" :key="item.docId" :label="item.title" :value="item.docId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="可见范围" prop="visibility">
              <el-select v-model="form.visibility" placeholder="请选择可见范围" style="width: 100%" :disabled="editLocked">
                <el-option label="公开" value="public" />
                <el-option label="部门可见" value="department" />
                <el-option label="仅自己" value="private" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%" :disabled="editLocked">
                <el-option label="草稿" value="draft" />
                <el-option label="已发布" value="published" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标签" prop="tagIds">
          <document-tag-select v-model="form.tagIds" :kb-id="form.kbId" :disabled="editLocked" />
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请输入文档摘要" :disabled="editLocked" />
        </el-form-item>
        <el-form-item label="提交说明" prop="commitMessage">
          <el-input v-model="form.commitMessage" placeholder="请输入本次变更说明" maxlength="100" :disabled="editLocked" />
        </el-form-item>
        <el-form-item label="Markdown内容" prop="markdownContent">
          <editor v-model="form.markdownContent" :min-height="320" />
          <div v-if="editLocked" class="edit-mask">
            当前文档由 {{ lockInfo.owner || "其他协作者" }} 占用编辑中，暂不可修改。
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="!editLocked" type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">{{ editLocked ? "关 闭" : "取 消" }}</el-button>
      </div>
    </el-dialog>

    <el-dialog title="回收站" :visible.sync="recycleOpen" width="800px" append-to-body>
      <el-table v-loading="recycleLoading" :data="recycleList">
        <el-table-column label="文档ID" align="center" prop="docId" width="110" />
        <el-table-column label="文档标题" align="center" prop="title" min-width="220" :show-overflow-tooltip="true" />
        <el-table-column label="知识库" align="center" prop="kbName" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column label="分组" align="center" prop="groupName" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="最近更新" align="center" prop="updateTime" width="160">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.updateTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="100">
          <template slot-scope="scope">
            <el-button size="mini" type="text" @click="handleRestore(scope.row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <wiki-view-drawer ref="viewRef" />
  </div>
</template>

<script>
import Editor from "@/components/Editor"
import RightToolbar from "@/components/RightToolbar"
import DocumentTagSelect from "./components/DocumentTagSelect"
import WikiViewDrawer from "./view"
import {
  addAdminWikiDocument,
  deleteAdminWikiDocument,
  getAdminWikiDocument,
  getAdminWikiRecycleDocuments,
  listAdminWikiDocumentOptions,
  listAdminWikiDocuments,
  lockAdminWikiDocument,
  restoreAdminWikiDocument,
  unlockAdminWikiDocument,
  updateAdminWikiDocument
} from "@/api/wiki/document"
import { listKb } from "@/api/wiki/kb"
import { listGroupOptions } from "@/api/wiki/group"

export default {
  name: "WikiManage",
  components: { Editor, RightToolbar, DocumentTagSelect, WikiViewDrawer },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      docList: [],
      kbOptions: [],
      queryGroupOptions: [],
      formGroupOptions: [],
      parentSource: [],
      title: "",
      open: false,
      recycleOpen: false,
      recycleLoading: false,
      recycleList: [],
      lockInfo: {
        locked: false,
        owner: "",
        message: ""
      },
      currentLockDocId: undefined,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: undefined,
        kbId: undefined,
        groupId: undefined,
        status: undefined
      },
      form: {},
      rules: {
        title: [{ required: true, message: "文档标题不能为空", trigger: "blur" }],
        kbId: [{ required: true, message: "知识库不能为空", trigger: "change" }],
        groupId: [{ required: true, message: "分组不能为空", trigger: "change" }],
        visibility: [{ required: true, message: "请选择可见范围", trigger: "change" }],
        status: [{ required: true, message: "请选择状态", trigger: "change" }],
        markdownContent: [{ required: true, message: "Markdown内容不能为空", trigger: "blur" }]
      }
    }
  },
  computed: {
    parentOptions() {
      return this.parentSource.filter(item => {
        if (this.form.docId && item.docId === this.form.docId) {
          return false
        }
        if (!this.form.kbId || item.kbId !== this.form.kbId) {
          return false
        }
        const currentGroupId = this.form.groupId || null
        const parentGroupId = item.groupId || null
        return currentGroupId === parentGroupId
      })
    },
    editLocked() {
      return !!(this.form.docId && !this.lockInfo.locked)
    },
    lockTip() {
      if (this.lockInfo.locked) {
        return `当前编辑锁已由你持有，30分钟内其他协作者无法同时修改。`
      }
      return `当前文档正在由 ${this.lockInfo.owner || "其他协作者"} 编辑，请稍后再试。`
    }
  },
  watch: {
    "form.groupId"(value, oldValue) {
      if (value !== oldValue) {
        this.form.parentId = 0
      }
    }
  },
  created() {
    this.getList()
    this.getKbOptions()
    this.getParentOptions()
  },
  beforeDestroy() {
    this.releaseCurrentLock()
  },
  methods: {
    getList() {
      this.loading = true
      listAdminWikiDocuments(this.queryParams).then(response => {
        this.docList = response.rows || []
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
    getParentOptions() {
      listAdminWikiDocumentOptions({ pageNum: 1, pageSize: 1000 }).then(response => {
        this.parentSource = response.data || response.rows || []
      })
    },
    loadQueryGroups(kbId) {
      if (!kbId) {
        this.queryGroupOptions = []
        return
      }
      listGroupOptions(kbId).then(response => {
        this.queryGroupOptions = response.data || []
      })
    },
    loadFormGroups(kbId) {
      if (!kbId) {
        this.formGroupOptions = []
        return
      }
      listGroupOptions(kbId).then(response => {
        this.formGroupOptions = response.data || []
      })
    },
    reset() {
      this.form = {
        docId: undefined,
        kbId: undefined,
        groupId: undefined,
        parentId: 0,
        tagIds: [],
        title: undefined,
        summary: undefined,
        visibility: "public",
        status: "published",
        markdownContent: undefined,
        commitMessage: undefined
      }
      this.formGroupOptions = []
      this.lockInfo = {
        locked: false,
        owner: "",
        message: ""
      }
      this.resetForm("form")
    },
    cancel() {
      this.releaseCurrentLock()
      this.open = false
      this.reset()
    },
    handleDialogClose(done) {
      this.releaseCurrentLock()
      this.reset()
      done()
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.queryGroupOptions = []
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.docId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleKbChange(value) {
      this.queryParams.groupId = undefined
      this.loadQueryGroups(value)
    },
    handleFormKbChange(value) {
      this.form.groupId = undefined
      this.form.parentId = 0
      this.form.tagIds = []
      this.loadFormGroups(value)
    },
    handleAdd() {
      this.releaseCurrentLock()
      this.reset()
      this.open = true
      this.title = "新增文档"
    },
    handleUpdate(row) {
      const docId = row.docId || this.ids[0]
      this.releaseCurrentLock()
      this.reset()
      lockAdminWikiDocument(docId).then(lockResponse => {
        this.lockInfo = lockResponse.data || { locked: false }
        return getAdminWikiDocument(docId)
      }).then(response => {
        this.form = response.data || {}
        this.form.parentId = this.form.parentId || 0
        this.form.tagIds = this.form.tagIds || []
        this.loadFormGroups(this.form.kbId)
        this.currentLockDocId = docId
        this.open = true
        this.title = this.lockInfo.locked ? "修改文档" : "查看协作文档"
        if (!this.lockInfo.locked) {
          this.$modal.msgWarning(this.lockTip)
        }
      }).catch(() => {
        this.releaseCurrentLock()
      })
    },
    submitForm() {
      if (this.editLocked) {
        this.$modal.msgWarning(this.lockTip)
        return
      }
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.docId ? updateAdminWikiDocument(this.form) : addAdminWikiDocument(this.form)
        request.then(() => {
          this.$modal.msgSuccess(this.form.docId ? "修改成功" : "新增成功")
          this.releaseCurrentLock()
          this.open = false
          this.getList()
          this.getParentOptions()
        })
      })
    },
    handleDelete(row) {
      const docIds = row.docId || this.ids
      this.$modal.confirm("确认删除选中的文档吗？").then(() => {
        return deleteAdminWikiDocument(docIds)
      }).then(() => {
        this.$modal.msgSuccess("删除成功")
        this.getList()
        this.getParentOptions()
      }).catch(() => {})
    },
    handleView(row) {
      this.$refs.viewRef.open(row.docId)
    },
    handleRecycle() {
      this.recycleLoading = true
      getAdminWikiRecycleDocuments().then(response => {
        this.recycleList = response.data || []
        this.recycleOpen = true
      }).finally(() => {
        this.recycleLoading = false
      })
    },
    handleRestore(row) {
      restoreAdminWikiDocument(row.docId).then(() => {
        this.$modal.msgSuccess("恢复成功")
        this.handleRecycle()
        this.getList()
        this.getParentOptions()
      })
    },
    releaseCurrentLock() {
      if (!this.currentLockDocId || !this.lockInfo.locked) {
        this.currentLockDocId = undefined
        return Promise.resolve()
      }
      const docId = this.currentLockDocId
      this.currentLockDocId = undefined
      this.lockInfo = {
        locked: false,
        owner: "",
        message: ""
      }
      return unlockAdminWikiDocument(docId).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.mb16 {
  margin-bottom: 16px;
}

.link-type {
  color: #409eff;
  cursor: pointer;
}

.edit-mask {
  margin-top: 8px;
  color: #e6a23c;
  font-size: 12px;
}
</style>

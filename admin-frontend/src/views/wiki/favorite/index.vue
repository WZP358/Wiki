<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" label-width="68px" v-show="showSearch">
      <el-form-item label="用户账号" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户账号" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="文档标题" prop="docTitle">
        <el-input v-model="queryParams.docTitle" placeholder="请输入文档标题" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="知识库" prop="kbId">
        <el-select v-model="queryParams.kbId" placeholder="请选择知识库" clearable filterable style="width: 220px">
          <el-option v-for="item in kbOptions" :key="item.kbId" :label="item.kbName" :value="item.kbId" />
        </el-select>
      </el-form-item>
      <el-form-item label="收藏夹" prop="folderId">
        <el-select v-model="queryParams.folderId" placeholder="请选择收藏夹" clearable filterable style="width: 220px">
          <el-option v-for="item in folderOptions" :key="item.folderId" :label="item.folderName" :value="item.folderId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['wiki:favorite:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['wiki:favorite:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="favoriteList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="收藏编号" align="center" prop="favoriteId" width="120" />
      <el-table-column label="用户账号" align="center" prop="userName" width="120" />
      <el-table-column label="用户昵称" align="center" prop="nickName" width="120" />
      <el-table-column label="文档标题" align="center" prop="docTitle" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="知识库" align="center" prop="kbName" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="收藏夹" align="center" prop="folderName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="文档作者" align="center" prop="ownerName" width="120" />
      <el-table-column label="收藏时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['wiki:favorite:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户编号" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户编号" />
        </el-form-item>
        <el-form-item label="文档编号" prop="docId">
          <el-input v-model="form.docId" placeholder="请输入文档编号" />
        </el-form-item>
        <el-form-item label="收藏夹" prop="folderId">
          <el-select v-model="form.folderId" placeholder="请选择收藏夹" clearable filterable style="width: 100%">
            <el-option v-for="item in formFolderOptions" :key="item.folderId" :label="item.folderName" :value="item.folderId" />
          </el-select>
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
import { addAdminFavorite, deleteAdminFavorite, listAdminFavoriteFolderOptions, listAdminFavorites } from "@/api/wiki/workbenchAdmin"

export default {
  name: "WikiFavorite",
  components: { RightToolbar },
  data() {
    return {
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      total: 0,
      favoriteList: [],
      kbOptions: [],
      folderOptions: [],
      formFolderOptions: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        docTitle: undefined,
        kbId: undefined,
        folderId: undefined
      },
      form: {},
      rules: {
        userId: [{ required: true, message: "用户编号不能为空", trigger: "blur" }],
        docId: [{ required: true, message: "文档编号不能为空", trigger: "blur" }]
      }
    }
  },
  watch: {
    "form.userId"(value) {
      if (!this.open) {
        return
      }
      this.loadFormFolderOptions(value)
      if (!value) {
        this.form.folderId = undefined
      }
    }
  },
  created() {
    this.getList()
    this.getKbOptions()
    this.getFolderOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listAdminFavorites(this.queryParams).then(response => {
        this.favoriteList = response.rows || []
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
    getFolderOptions() {
      listAdminFavoriteFolderOptions({ pageNum: 1, pageSize: 1000 }).then(response => {
        this.folderOptions = response.data || response.rows || []
      })
    },
    loadFormFolderOptions(userId) {
      const query = { pageNum: 1, pageSize: 1000 }
      if (userId) {
        query.userId = userId
      }
      listAdminFavoriteFolderOptions(query).then(response => {
        this.formFolderOptions = response.data || response.rows || []
      })
    },
    reset() {
      this.form = {
        userId: undefined,
        docId: undefined,
        folderId: undefined,
        remark: undefined
      }
      this.formFolderOptions = []
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
      this.ids = selection.map(item => item.favoriteId)
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.loadFormFolderOptions()
      this.open = true
      this.title = "新增收藏"
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        addAdminFavorite(this.form).then(() => {
          this.$modal.msgSuccess("新增成功")
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
      const favoriteIds = row.favoriteId || this.ids
      this.$modal.confirm("是否确认删除选中的收藏记录？").then(() => {
        return deleteAdminFavorite(favoriteIds)
      }).then(() => {
        this.$modal.msgSuccess("删除成功")
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>

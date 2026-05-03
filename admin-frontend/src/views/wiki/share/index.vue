<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" label-width="68px" v-show="showSearch">
      <el-form-item label="文档标题" prop="docTitle">
        <el-input v-model="queryParams.docTitle" placeholder="请输入文档标题" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="知识库" prop="kbId">
        <el-select v-model="queryParams.kbId" placeholder="请选择知识库" clearable filterable style="width: 220px">
          <el-option v-for="item in kbOptions" :key="item.kbId" :label="item.kbName" :value="item.kbId" />
        </el-select>
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['wiki:share:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['wiki:share:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['wiki:share:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="shareList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="分享编号" align="center" prop="shareId" width="120" />
      <el-table-column label="文档标题" align="center" prop="docTitle" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="知识库" align="center" prop="kbName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="分享码" align="center" prop="shareCode" width="130" />
      <el-table-column label="分享范围" align="center" prop="accessScope" width="100">
        <template slot-scope="scope">
          <el-tag size="mini">{{ scope.row.accessScope === "public" ? "公开" : "内部" }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="访问次数" align="center" prop="accessCount" width="90" />
      <el-table-column label="次数上限" align="center" prop="accessLimit" width="90">
        <template slot-scope="scope">
          <span>{{ scope.row.accessLimit && scope.row.accessLimit > 0 ? scope.row.accessLimit : "不限" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="失效时间" align="center" prop="expireTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.expireTime) || "长期有效" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === 'enabled' ? 'success' : 'info'">
            {{ scope.row.status === "enabled" ? "启用" : "停用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-document-copy" @click="handleCopy(scope.row)">复制链接</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['wiki:share:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['wiki:share:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="文档编号" prop="docId">
          <el-input v-model="form.docId" placeholder="请输入文档编号" />
        </el-form-item>
        <el-form-item label="分享范围" prop="accessScope">
          <el-radio-group v-model="form.accessScope">
            <el-radio label="public">公开</el-radio>
            <el-radio label="internal">内部</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="失效时间" prop="expireTime">
          <el-date-picker v-model="form.expireTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择失效时间" clearable style="width: 100%" />
        </el-form-item>
        <el-form-item label="次数上限" prop="accessLimit">
          <el-input-number v-model="form.accessLimit" controls-position="right" :min="0" />
          <span style="margin-left: 10px; color: #909399">填 0 表示不限制</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="enabled">启用</el-radio>
            <el-radio label="disabled">停用</el-radio>
          </el-radio-group>
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
import { addShare, delShare, getShare, listShare, updateShare } from "@/api/wiki/share"

export default {
  name: "WikiShare",
  components: { RightToolbar },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      shareList: [],
      kbOptions: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        docTitle: undefined,
        kbId: undefined,
        status: undefined
      },
      form: {},
      rules: {
        docId: [{ required: true, message: "文档编号不能为空", trigger: "blur" }],
        accessScope: [{ required: true, message: "分享范围不能为空", trigger: "change" }],
        status: [{ required: true, message: "状态不能为空", trigger: "change" }]
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
      listShare(this.queryParams).then(response => {
        this.shareList = response.rows || []
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
        shareId: undefined,
        docId: undefined,
        accessScope: "public",
        expireTime: undefined,
        accessLimit: 0,
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
      this.ids = selection.map(item => item.shareId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增分享"
    },
    handleUpdate(row) {
      const shareId = row.shareId || this.ids[0]
      this.reset()
      getShare(shareId).then(response => {
        this.form = response.data || {}
        this.open = true
        this.title = "修改分享"
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.shareId ? updateShare(this.form) : addShare(this.form)
        request.then(() => {
          this.$modal.msgSuccess(this.form.shareId ? "修改成功" : "新增成功")
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
      const shareIds = row.shareId || this.ids
      this.$modal.confirm("是否确认删除选中的分享记录？").then(() => {
        return delShare(shareIds)
      }).then(() => {
        this.$modal.msgSuccess("删除成功")
        this.getList()
      }).catch(() => {})
    },
    handleCopy(row) {
      const link = window.location.origin + "/wiki/share/open/" + row.shareCode
      this.$copyText(link).then(() => {
        this.$modal.msgSuccess("分享链接已复制")
      }).catch(() => {
        this.$modal.msgError("复制失败，请手动复制")
      })
    }
  }
}
</script>

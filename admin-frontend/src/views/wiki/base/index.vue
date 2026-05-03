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
      <el-form-item label="知识库名" prop="kbName">
        <el-input
          v-model="queryParams.kbName"
          placeholder="请输入知识库名称"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="可见范围" prop="visibility">
        <el-select v-model="queryParams.visibility" placeholder="请选择可见范围" clearable style="width: 180px">
          <el-option label="公开" value="public" />
          <el-option label="部门可见" value="department" />
          <el-option label="仅自己" value="private" />
        </el-select>
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['wiki:kb:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['wiki:kb:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['wiki:kb:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="kbList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="知识库ID" align="center" prop="kbId" width="120" />
      <el-table-column label="知识库名称" align="center" prop="kbName" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="编码" align="center" prop="kbCode" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="可见范围" align="center" prop="visibility" width="110">
        <template slot-scope="scope">
          <el-tag size="mini" :type="visibilityType(scope.row.visibility)">
            {{ visibilityFormat(scope.row.visibility) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === 'enabled' ? 'success' : 'info'">
            {{ statusFormat(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="负责人" align="center" prop="ownerName" width="120" />
      <el-table-column label="文档数" align="center" prop="docCount" width="90" />
      <el-table-column label="排序" align="center" prop="sortNum" width="80" />
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-user" @click="handleMember(scope.row)" v-hasPermi="['wiki:kb:query']">协作者</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['wiki:kb:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['wiki:kb:remove']">删除</el-button>
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

    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="知识库名称" prop="kbName">
          <el-input v-model="form.kbName" placeholder="请输入知识库名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="知识库编码" prop="kbCode">
          <el-input v-model="form.kbCode" placeholder="请输入知识库编码" maxlength="64" />
        </el-form-item>
        <el-form-item label="可见范围" prop="visibility">
          <el-radio-group v-model="form.visibility">
            <el-radio label="public">公开</el-radio>
            <el-radio label="department">部门可见</el-radio>
            <el-radio label="private">仅自己</el-radio>
          </el-radio-group>
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
        <el-form-item label="知识库介绍" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入知识库介绍" />
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

    <el-dialog :title="memberTitle" :visible.sync="memberOpen" width="980px" append-to-body>
      <el-alert
        :title="permissionTip"
        :type="memberPermission.canManageMembers ? 'success' : 'info'"
        :closable="false"
        show-icon
        class="mb16"
      />

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAddMember"
            :disabled="!memberPermission.canManageMembers"
            v-hasPermi="['wiki:kb:add']"
          >新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="success"
            plain
            icon="el-icon-edit"
            size="mini"
            :disabled="memberSingle || !memberPermission.canManageMembers"
            @click="handleUpdateMember"
            v-hasPermi="['wiki:kb:edit']"
          >修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="mini"
            :disabled="memberMultiple || !memberPermission.canManageMembers"
            @click="handleDeleteMember"
            v-hasPermi="['wiki:kb:remove']"
          >删除</el-button>
        </el-col>
        <el-col :span="2">
          <el-button
            type="warning"
            plain
            icon="el-icon-switch-button"
            size="mini"
            :disabled="!memberPermission.canTransferOwner || transferCandidates.length === 0"
            @click="handleTransferOwner"
            v-hasPermi="['wiki:kb:edit']"
          >转交负责人</el-button>
        </el-col>
      </el-row>

      <el-table
        v-loading="memberLoading"
        :data="memberList"
        :row-class-name="memberRowClassName"
        @selection-change="handleMemberSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" :selectable="memberSelectable" />
        <el-table-column label="成员ID" align="center" prop="memberId" width="100" />
        <el-table-column label="用户ID" align="center" prop="userId" width="100" />
        <el-table-column label="登录账号" align="center" prop="userName" width="140" />
        <el-table-column label="用户昵称" align="center" prop="nickName" width="140" />
        <el-table-column label="部门" align="center" prop="deptName" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="角色" align="center" prop="roleCode" width="110">
          <template slot-scope="scope">
            <el-tag size="mini" :type="roleType(scope.row.roleCode)">
              {{ roleFormat(scope.row.roleCode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="90">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === 0 ? 'success' : 'info'">
              {{ scope.row.status === 0 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" align="center" prop="sortNum" width="80" />
        <el-table-column label="备注" align="center" prop="remark" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdateMember(scope.row)"
              :disabled="!memberPermission.canManageMembers || isOwnerMember(scope.row)"
              v-hasPermi="['wiki:kb:edit']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDeleteMember(scope.row)"
              :disabled="!memberPermission.canManageMembers || isOwnerMember(scope.row)"
              v-hasPermi="['wiki:kb:remove']"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog :title="memberFormTitle" :visible.sync="memberFormOpen" width="560px" append-to-body>
        <el-alert
          v-if="memberPermission.canManageMembers"
          title="负责人角色请通过“转交负责人”处理，成员表单只维护管理员、编辑者、查看者。"
          type="warning"
          :closable="false"
          show-icon
          class="mb16"
        />
        <el-form ref="memberForm" :model="memberForm" :rules="memberRules" label-width="90px">
          <el-form-item label="用户ID" prop="userId">
            <el-input v-model="memberForm.userId" placeholder="请输入用户ID" />
          </el-form-item>
          <el-form-item label="成员角色" prop="roleCode">
            <el-radio-group v-model="memberForm.roleCode">
              <el-radio v-for="item in memberRoleOptions" :key="item.value" :label="item.value">
                {{ item.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="memberForm.status">
              <el-radio :label="0">启用</el-radio>
              <el-radio :label="1">停用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="排序" prop="sortNum">
            <el-input-number v-model="memberForm.sortNum" controls-position="right" :min="0" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="memberForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button type="primary" @click="submitMemberForm">确 定</el-button>
          <el-button @click="cancelMemberForm">取 消</el-button>
        </div>
      </el-dialog>

      <el-dialog title="转交负责人" :visible.sync="transferOpen" width="520px" append-to-body>
        <el-alert
          title="转交后，原负责人会自动调整为管理员，新负责人必须是当前知识库已启用的协作者。"
          type="warning"
          :closable="false"
          show-icon
          class="mb16"
        />
        <el-form ref="transferForm" :model="transferForm" :rules="transferRules" label-width="90px">
          <el-form-item label="当前负责人">
            <el-input :value="memberPermission.ownerName || '-'" disabled />
          </el-form-item>
          <el-form-item label="新负责人" prop="targetUserId">
            <el-select v-model="transferForm.targetUserId" placeholder="请选择新负责人" filterable style="width: 100%">
              <el-option
                v-for="item in transferCandidates"
                :key="item.userId"
                :label="transferOptionLabel(item)"
                :value="item.userId"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button type="primary" @click="submitTransferOwner">确 定</el-button>
          <el-button @click="cancelTransferOwner">取 消</el-button>
        </div>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script>
import RightToolbar from "@/components/RightToolbar"
import {
  addKb,
  addKbMember,
  delKb,
  delKbMember,
  getKb,
  getKbMember,
  getKbPermission,
  listKb,
  listKbMember,
  transferKbOwner,
  updateKb,
  updateKbMember
} from "@/api/wiki/kb"

export default {
  name: "WikiBase",
  components: { RightToolbar },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      kbList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        kbName: undefined,
        visibility: undefined,
        status: undefined
      },
      form: {},
      rules: {
        kbName: [{ required: true, message: "知识库名称不能为空", trigger: "blur" }],
        kbCode: [{ required: true, message: "知识库编码不能为空", trigger: "blur" }],
        visibility: [{ required: true, message: "请选择可见范围", trigger: "change" }],
        status: [{ required: true, message: "请选择状态", trigger: "change" }]
      },
      currentKbId: undefined,
      memberOpen: false,
      memberTitle: "",
      memberLoading: false,
      memberIds: [],
      memberSingle: true,
      memberMultiple: true,
      memberList: [],
      memberPermission: {
        currentRole: "viewer",
        canEdit: false,
        canManageMembers: false,
        canTransferOwner: false,
        currentUserId: undefined,
        ownerId: undefined,
        ownerName: ""
      },
      memberFormOpen: false,
      memberFormTitle: "",
      memberForm: {},
      memberRules: {
        userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
        roleCode: [{ required: true, message: "请选择成员角色", trigger: "change" }]
      },
      memberRoleOptions: [
        { label: "管理员", value: "admin" },
        { label: "编辑者", value: "editor" },
        { label: "查看者", value: "viewer" }
      ],
      transferOpen: false,
      transferForm: {
        targetUserId: undefined
      },
      transferRules: {
        targetUserId: [{ required: true, message: "请选择新负责人", trigger: "change" }]
      }
    }
  },
  computed: {
    permissionTip() {
      const roleMap = {
        owner: "负责人",
        admin: "管理员",
        editor: "编辑者",
        viewer: "查看者"
      }
      const roleLabel = roleMap[this.memberPermission.currentRole] || "查看者"
      if (this.memberPermission.canManageMembers) {
        return `当前角色：${roleLabel}。你可以维护该知识库的协作者名单。`
      }
      if (this.memberPermission.canEdit) {
        return `当前角色：${roleLabel}。你可以编辑知识库内容，但不能维护协作者名单。`
      }
      return `当前角色：${roleLabel}。你只能查看协作者名单，不能执行新增、修改、删除。`
    },
    transferCandidates() {
      return this.memberList.filter(item => item.status === 0 && item.roleCode !== "owner")
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listKb(this.queryParams).then(response => {
        this.kbList = response.rows || []
        this.total = response.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        kbId: undefined,
        kbName: undefined,
        kbCode: undefined,
        description: undefined,
        visibility: "public",
        status: "enabled",
        sortNum: 0,
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
      this.ids = selection.map(item => item.kbId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增知识库"
    },
    handleUpdate(row) {
      this.reset()
      const kbId = row.kbId || this.ids[0]
      getKb(kbId).then(response => {
        this.form = response.data || {}
        this.open = true
        this.title = "修改知识库"
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.kbId ? updateKb(this.form) : addKb(this.form)
        request.then(() => {
          this.$modal.msgSuccess(this.form.kbId ? "修改成功" : "新增成功")
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const kbIds = row.kbId || this.ids
      this.$modal.confirm("确认删除选中的知识库吗？").then(() => {
        return delKb(kbIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleMember(row) {
      this.currentKbId = row.kbId
      this.memberTitle = "协作者管理 - " + row.kbName
      this.memberOpen = true
      this.loadMemberData()
    },
    loadMemberData() {
      this.memberLoading = true
      Promise.all([
        listKbMember({ kbId: this.currentKbId, pageNum: 1, pageSize: 1000 }),
        getKbPermission(this.currentKbId)
      ]).then(([memberResponse, permissionResponse]) => {
        this.memberList = memberResponse.rows || []
        this.memberPermission = permissionResponse.data || this.memberPermission
        this.resetMemberSelection()
      }).finally(() => {
        this.memberLoading = false
      })
    },
    resetMemberSelection() {
      this.memberIds = []
      this.memberSingle = true
      this.memberMultiple = true
    },
    handleMemberSelectionChange(selection) {
      this.memberIds = selection.map(item => item.memberId)
      this.memberSingle = selection.length !== 1
      this.memberMultiple = !selection.length
    },
    resetMemberForm() {
      this.memberForm = {
        memberId: undefined,
        kbId: this.currentKbId,
        userId: undefined,
        roleCode: "editor",
        sortNum: 0,
        status: 0,
        remark: undefined
      }
      this.resetForm("memberForm")
    },
    handleAddMember() {
      if (!this.memberPermission.canManageMembers) {
        this.$modal.msgWarning("当前角色不能维护协作者")
        return
      }
      this.resetMemberForm()
      this.memberFormOpen = true
      this.memberFormTitle = "新增协作者"
    },
    handleUpdateMember(row) {
      const currentRow = row || this.memberList.find(item => item.memberId === this.memberIds[0])
      if (!currentRow) {
        return
      }
      if (this.isOwnerMember(currentRow)) {
        this.$modal.msgWarning("负责人请通过“转交负责人”处理")
        return
      }
      this.resetMemberForm()
      getKbMember(currentRow.memberId).then(response => {
        this.memberForm = response.data || {}
        this.memberForm.kbId = this.currentKbId
        this.memberFormOpen = true
        this.memberFormTitle = "修改协作者"
      })
    },
    submitMemberForm() {
      this.$refs.memberForm.validate(valid => {
        if (!valid) {
          return
        }
        this.memberForm.kbId = this.currentKbId
        const request = this.memberForm.memberId ? updateKbMember(this.memberForm) : addKbMember(this.memberForm)
        request.then(() => {
          this.$modal.msgSuccess(this.memberForm.memberId ? "修改成功" : "新增成功")
          this.memberFormOpen = false
          this.loadMemberData()
        })
      })
    },
    handleDeleteMember(row) {
      const ids = row ? [row.memberId] : this.memberIds
      const ownerSelected = (row ? [row] : this.memberList.filter(item => ids.includes(item.memberId))).some(item => this.isOwnerMember(item))
      if (ownerSelected) {
        this.$modal.msgWarning("负责人不能在成员列表中删除")
        return
      }
      this.$modal.confirm("确认删除选中的协作者吗？").then(() => {
        return delKbMember(ids)
      }).then(() => {
        this.loadMemberData()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    cancelMemberForm() {
      this.memberFormOpen = false
      this.resetMemberForm()
    },
    handleTransferOwner() {
      if (!this.memberPermission.canTransferOwner) {
        this.$modal.msgWarning("当前角色不能转交负责人")
        return
      }
      this.transferForm = {
        targetUserId: undefined
      }
      this.transferOpen = true
      this.$nextTick(() => {
        this.resetForm("transferForm")
      })
    },
    submitTransferOwner() {
      this.$refs.transferForm.validate(valid => {
        if (!valid) {
          return
        }
        transferKbOwner(this.currentKbId, this.transferForm.targetUserId).then(() => {
          this.$modal.msgSuccess("负责人转交成功")
          this.transferOpen = false
          this.getList()
          this.loadMemberData()
        })
      })
    },
    cancelTransferOwner() {
      this.transferOpen = false
      this.transferForm = {
        targetUserId: undefined
      }
    },
    visibilityFormat(value) {
      return { public: "公开", department: "部门可见", private: "仅自己" }[value] || value
    },
    visibilityType(value) {
      return { public: "success", department: "warning", private: "info" }[value] || ""
    },
    statusFormat(value) {
      return { enabled: "启用", disabled: "停用" }[value] || value
    },
    roleFormat(value) {
      return { owner: "负责人", admin: "管理员", editor: "编辑者", viewer: "查看者" }[value] || value
    },
    roleType(value) {
      return { owner: "danger", admin: "warning", editor: "success", viewer: "info" }[value] || ""
    },
    isOwnerMember(row) {
      return row && row.roleCode === "owner"
    },
    memberSelectable(row) {
      return !this.isOwnerMember(row)
    },
    memberRowClassName({ row }) {
      return row.userId === this.memberPermission.currentUserId ? "member-self-row" : ""
    },
    transferOptionLabel(item) {
      const nickName = item.nickName ? `（${item.nickName}）` : ""
      return `${item.userName}${nickName} - ${this.roleFormat(item.roleCode)}`
    }
  }
}
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}

/deep/ .member-self-row {
  background: #f5f7fa;
}
</style>

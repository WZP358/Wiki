<template>
  <div class="app-container wiki-home">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="intro-card">
          <div class="intro-head">
            <div>
              <div class="eyebrow">企业内部知识库 Wiki 系统</div>
              <h1>面向团队协作、权限治理与文档沉淀的知识库平台</h1>
              <p>
                系统提供知识库空间、Markdown 文档管理、版本历史、全文检索、团队权限和 Redis 缓存能力。
                后台管理侧负责平台治理，用户端负责日常知识协作。
              </p>
            </div>
            <el-button type="primary" icon="el-icon-s-operation" @click="$router.push('/wiki/dashboard')">进入后台概览</el-button>
          </div>
        </el-card>

        <el-row :gutter="16" class="mt16">
          <el-col :xs="24" :sm="12" :lg="6" v-for="item in statsCards" :key="item.label">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">{{ item.label }}</div>
              <div class="stat-value">{{ item.value }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" class="mt16">
          <div slot="header" class="card-header">核心能力</div>
          <el-row :gutter="16">
            <el-col :xs="24" :md="12" v-for="item in features" :key="item.title">
              <div class="feature-item">
                <div class="feature-icon"><i :class="item.icon"></i></div>
                <div>
                  <h3>{{ item.title }}</h3>
                  <p>{{ item.desc }}</p>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="side-card">
          <div slot="header" class="card-header">角色边界</div>
          <el-timeline>
            <el-timeline-item color="#409EFF" timestamp="系统管理员">
              管理用户、团队、菜单、审计、公告和删除内容恢复，负责平台级治理。
            </el-timeline-item>
            <el-timeline-item color="#67C23A" timestamp="知识库管理员">
              维护知识库成员、邀请协作者、分配读写权限，负责知识库内部协作。
            </el-timeline-item>
            <el-timeline-item color="#909399" timestamp="普通用户">
              注册后先进入待分配状态；分配团队后可查看公开知识库、同团队知识库和受邀知识库。
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card shadow="never" class="side-card mt16">
          <div slot="header" class="card-header">权限规则</div>
          <ul class="rule-list">
            <li>公开知识库自动为所有已分配普通用户加入只读名单。</li>
            <li>团队知识库自动为所属团队及子团队用户加入只读名单。</li>
            <li>可见不等于可编辑，编辑必须来自知识库成员角色。</li>
            <li>文档权限不能突破知识库权限。</li>
            <li>普通用户只能软删除，管理员可审计与恢复。</li>
          </ul>
        </el-card>

        <el-card shadow="never" class="side-card mt16">
          <div slot="header" class="card-header">Redis 应用点</div>
          <el-tag size="medium" type="success">最近更新 Top5</el-tag>
          <el-tag size="medium" type="warning">最多阅读 Top5</el-tag>
          <el-tag size="medium" type="info">发布 HTML 缓存</el-tag>
          <el-tag size="medium">编辑锁</el-tag>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getOverview } from '@/api/wiki-admin'

export default {
  name: 'Index',
  data() {
    return {
      stats: {},
      features: [
        { title: '文档管理', icon: 'el-icon-document', desc: '支持树形目录、Markdown 编辑、图片插入、最终排版预览和发布状态管理。' },
        { title: '版本历史', icon: 'el-icon-time', desc: '保存文档修改历史，支持版本对比、回滚和编辑审计。' },
        { title: '全文检索', icon: 'el-icon-search', desc: '基于数据库 LIKE 完成标题与内容检索，前端可直接查询可见内容。' },
        { title: '权限控制', icon: 'el-icon-lock', desc: '区分公开、团队、私有文档，并叠加知识库成员角色实现读写边界。' }
      ]
    }
  },
  computed: {
    statsCards() {
      return [
        { label: '活跃用户', value: this.stats.activeUsers || 0 },
        { label: '正常文档', value: this.stats.totalDocs || 0 },
        { label: '已删文档', value: this.stats.deletedDocs || 0 },
        { label: '24h 操作', value: this.stats.operationLogs24h || 0 }
      ]
    }
  },
  created() {
    getOverview().then(res => {
      this.stats = res || {}
    })
  }
}
</script>

<style scoped>
.wiki-home {
  background: #f5f7fa;
  min-height: calc(100vh - 84px);
}
.intro-card {
  border: 1px solid #e4e7ed;
}
.intro-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
.eyebrow {
  color: #409eff;
  font-size: 13px;
  font-weight: 700;
}
.intro-head h1 {
  margin: 10px 0 12px;
  color: #1f2d3d;
  font-size: 26px;
  line-height: 1.35;
}
.intro-head p {
  max-width: 760px;
  margin: 0;
  color: #606266;
  line-height: 1.8;
}
.mt16 {
  margin-top: 16px;
}
.stat-card {
  margin-bottom: 16px;
}
.stat-label {
  color: #909399;
  font-size: 13px;
}
.stat-value {
  margin-top: 10px;
  color: #303133;
  font-size: 26px;
  font-weight: 700;
}
.card-header {
  color: #303133;
  font-weight: 700;
}
.feature-item {
  display: flex;
  min-height: 112px;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}
.feature-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  background: #ecf5ff;
  font-size: 20px;
  flex-shrink: 0;
}
.feature-item h3 {
  margin: 0 0 8px;
  color: #303133;
  font-size: 16px;
}
.feature-item p {
  margin: 0;
  color: #606266;
  line-height: 1.7;
}
.side-card {
  border: 1px solid #e4e7ed;
}
.rule-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.9;
}
.side-card .el-tag {
  margin: 0 8px 8px 0;
}
@media (max-width: 992px) {
  .intro-head {
    display: block;
  }
  .intro-head .el-button {
    margin-top: 16px;
  }
}
</style>

const systemChildren = [
  { name: 'SystemUser', path: 'user', component: 'system/user/index', meta: { title: '用户管理', icon: 'user' } },
  { name: 'SystemRole', path: 'role', component: 'system/role/index', meta: { title: '角色管理', icon: 'peoples' } },
  { name: 'SystemDept', path: 'dept', component: 'system/dept/index', meta: { title: '团队管理', icon: 'tree' } },
  { name: 'SystemMenu', path: 'menu', component: 'system/menu/index', meta: { title: '菜单管理', icon: 'tree-table' } },
  { name: 'SystemNotice', path: 'notice', component: 'system/notice/index', meta: { title: '通知公告', icon: 'message' } }
]

const wikiChildren = [
  { name: 'WikiBase', path: 'base', component: 'wiki/base/index', meta: { title: '知识库管理', icon: 'education' } },
  { name: 'WikiManage', path: 'manage', component: 'wiki/manage/index', meta: { title: '文档管理', icon: 'documentation' } },
  { name: 'WikiTemplate', path: 'template', component: 'wiki/template/index', meta: { title: '模板管理', icon: 'form' } },
  { name: 'WikiAdminDashboard', path: 'dashboard', component: 'wiki-admin/dashboard/index', meta: { title: '后台概览', icon: 'dashboard' } },
  { name: 'WikiAdminPendingUsers', path: 'pending-users', component: 'wiki-admin/pending-users/index', meta: { title: '待分配用户', icon: 'peoples' } },
  { name: 'WikiAdminUsers', path: 'users', component: 'wiki-admin/users/index', meta: { title: '用户审计', icon: 'user' } },
  { name: 'WikiAdminDepartments', path: 'departments', component: 'wiki-admin/departments/index', meta: { title: '团队审计', icon: 'tree' } },
  { name: 'WikiAdminKbs', path: 'kbs', component: 'wiki-admin/kbs/index', meta: { title: '知识库审计', icon: 'education' } },
  { name: 'WikiAdminDocs', path: 'docs', component: 'wiki-admin/docs/index', meta: { title: '内容审计', icon: 'form' } },
  { name: 'WikiAdminLogs', path: 'logs', component: 'wiki-admin/logs/index', meta: { title: '操作审计', icon: 'log' } }
]

export const getRouters = () => {
  return Promise.resolve({
    data: [
      {
        name: 'System',
        path: '/system',
        hidden: false,
        redirect: 'noRedirect',
        component: 'Layout',
        alwaysShow: true,
        meta: { title: '系统管理', icon: 'system' },
        children: systemChildren
      },
      {
        name: 'Wiki',
        path: '/wiki',
        hidden: false,
        redirect: 'noRedirect',
        component: 'Layout',
        alwaysShow: true,
        meta: { title: 'Wiki 管理', icon: 'documentation' },
        children: wikiChildren
      }
    ]
  })
}

const { test, expect } = require('@playwright/test')

async function postJson(request, path, body, token) {
  const response = await request.post(`http://localhost:8080/api${path}`, {
    data: body,
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  const json = await response.json()
  if (!response.ok() || json.success === false) {
    throw new Error(`${path} failed: ${response.status()} ${JSON.stringify(json)}`)
  }
  return json.data
}

async function createUserFixture(request) {
  const stamp = Date.now()
  const username = `e2e${stamp}`
  const email = `${username}@example.com`
  const code = await postJson(request, '/auth/send-code', { target: email })
  const login = await postJson(request, '/auth/register', {
    username,
    password: 'Passw0rd!',
    email,
    code: code.code
  })
  const kb = await postJson(request, '/kbs', {
    name: `E2E KB ${stamp}`,
    type: 'PRIVATE',
    description: 'e2e'
  }, login.token)
  const doc = await postJson(request, '/docs', {
    kbId: kb.id,
    title: 'E2E Doc',
    markdownContent: '# E2E',
    visibility: 'PUBLIC',
    published: true
  }, login.token)
  return { login, kb, doc }
}

async function loginUserInBrowser(page, fixture) {
  await page.goto('http://localhost:5173/auth')
  await page.evaluate(({ token, user }) => {
    localStorage.setItem('wiki-token', token)
    localStorage.setItem('wiki-user', JSON.stringify(user))
  }, fixture.login)
}

test('user portal opens the auth screen', async ({ page }) => {
  await page.goto('http://localhost:5173/auth')
  await expect(page.locator('body')).toContainText(/登录|注册|账号|密码/)
})

test('admin login page opens', async ({ page }) => {
  await page.goto('http://localhost:5181/login')
  await expect(page.locator('.login-form')).toBeVisible()
  await expect(page.locator('.login-form')).toContainText('Wiki')
})

test('admin login shows RuoYi modal on invalid password', async ({ page }) => {
  await page.goto('http://localhost:5181/login')
  await page.locator('input[type="password"]').fill('wrong-password')
  await page.locator('.login-form .el-button').click()

  const messageBox = page.locator('.el-message-box')
  await expect(messageBox).toBeVisible()
  await expect(messageBox).toContainText(/Invalid account or password|账号|密码|错误/)
})

test('authenticated user pages open with created knowledge base and document', async ({ page, request }) => {
  const fixture = await createUserFixture(request)
  await loginUserInBrowser(page, fixture)

  const paths = [
    '/',
    `/kb/${fixture.kb.id}`,
    `/editor/${fixture.kb.id}/${fixture.doc.id}`,
    `/settings/${fixture.kb.id}`,
    '/search',
    '/profile'
  ]

  for (const path of paths) {
    await page.goto(`http://localhost:5173${path}`)
    await expect(page.locator('body')).not.toContainText(/Cannot GET|请求失败（HTTP 500）|System is busy/)
  }
})

test('admin authenticated shell opens dashboard and wiki admin pages', async ({ page, request, context }) => {
  const login = await postJson(request, '/auth/login', {
    account: 'admin',
    password: 'Admin@123456'
  })
  await context.addCookies([{
    name: 'Admin-Token',
    value: login.token,
    domain: 'localhost',
    path: '/'
  }])

  const paths = ['/index', '/wiki/dashboard', '/wiki/users', '/wiki/kbs', '/wiki/logs']
  for (const path of paths) {
    await page.goto(`http://localhost:5181${path}`)
    await expect(page.locator('#app')).toBeVisible()
    await expect(page.locator('body')).not.toContainText(/Cannot GET|404错误|Request failed with status code 500/)
  }
})

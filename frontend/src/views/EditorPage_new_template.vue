<template>
  <div class="editor-page">
    <!-- 左侧文档树 -->
    <aside class="doc-sidebar">
      <div class="sidebar-header">
        <div class="search-box">
          <input v-model="keyword" class="input search-input" placeholder="🔍 搜索文档..." @keyup.enter="search" />
        </div>
        <button class="btn btn-primary new-doc-btn" @click="createDoc">
          <span>+ 新建</span>
        </button>
      </div>

      <div class="doc-tree">
        <div
          v-for="node in tree"
          :key="node.id"
          class="tree-item"
          :class="{ active: String(node.id) === String(docId) }"
          @click="openDoc(node.id)"
        >
          <span class="doc-icon">📄</span>
          <div class="tree-item-content">
            <span class="tree-item-title">{{ node.title }}</span>
            <span class="tree-item-version">v{{ node.versionNo }}</span>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主编辑区域 -->
    <section v-if="doc" class="editor-main">
      <!-- 顶部工具栏 -->
      <div class="editor-toolbar">
        <div class="toolbar-left">
          <input v-model="form.title" class="input title-input" placeholder="无标题文档" />
        </div>
        <div class="toolbar-right">
          <select v-model="form.visibility" class="input visibility-select">
            <option value="PUBLIC">🌐 公开</option>
            <option value="TEAM">👥 团队</option>
            <option value="PRIVATE">🔒 私有</option>
          </select>
          <button class="btn" @click="openVersions">
            <span>📜 历史</span>
          </button>
          <button class="btn" @click="createShare">
            <span>🔗 分享</span>
          </button>
          <button class="btn btn-primary" @click="saveDoc">
            <span>💾 保存</span>
          </button>
          <button class="btn btn-text" @click="removeDoc">
            <span>🗑️</span>
          </button>
        </div>
      </div>

      <!-- 状态栏 -->
      <div class="status-bar">
        <div class="status-item">
          <span class="status-label">协作状态:</span>
          <span class="status-value" :class="collabStatusClass">{{ collabStatus }}</span>
        </div>
        <div v-if="shareLink" class="status-item share-link">
          <span class="status-label">分享链接:</span>
          <a :href="`/share/${shareLink}`" target="_blank" class="status-value link">
            {{ location.origin }}/share/{{ shareLink }}
          </a>
        </div>
      </div>

      <!-- 协作者信息 -->
      <div v-if="collaborators.length > 0" class="collaborators-bar">
        <span class="collab-label">👥 协作者:</span>
        <div class="collab-avatars">
          <div v-for="p in collaborators" :key="p.sessionId" class="collab-avatar" :title="`${p.username} (${p.cursorStart}-${p.cursorEnd})`">
            <img v-if="p.avatarUrl" :src="p.avatarUrl" alt="avatar" />
            <span v-else class="avatar-text">{{ p.username?.slice(0, 1) || '?' }}</span>
          </div>
        </div>
      </div>

      <!-- 编辑器和预览 -->
      <div class="editor-container">
        <div class="editor-pane">
          <div class="pane-header">
            <span>✏️ 编辑</span>
          </div>
          <textarea
            ref="editorRef"
            v-model="form.markdownContent"
            class="markdown-editor"
            placeholder="开始编写你的文档..."
            @click="reportCursor"
            @keyup="reportCursor"
            @select="reportCursor"
          ></textarea>
        </div>

        <div class="preview-pane">
          <div class="pane-header">
            <span>👁️ 预览</span>
          </div>
          <article class="markdown-preview" v-html="previewHtml"></article>
        </div>
      </div>
    </section>

    <!-- 空状态 -->
    <section v-else class="empty-state">
      <div class="empty-content">
        <div class="empty-icon">📝</div>
        <h3>选择或创建一个文档</h3>
        <p>从左侧选择一个文档开始编辑，或创建一个新文档</p>
        <button class="btn btn-primary" @click="createDoc">+ 新建文档</button>
      </div>
    </section>

    <!-- 版本历史弹窗 -->
    <div v-if="showVersions" class="modal-overlay" @click.self="showVersions = false">
      <div class="modal-content version-modal">
        <div class="modal-header">
          <h3>📜 版本历史</h3>
          <button class="btn-text close-btn" @click="showVersions = false">✕</button>
        </div>

        <div class="version-compare">
          <select v-model="compare.left" class="input">
            <option value="">选择左侧版本</option>
            <option v-for="v in versions" :key="'l-' + v.id" :value="v.id">v{{ v.versionNo }}</option>
          </select>
          <button class="btn" @click="compareVersions">对比</button>
          <select v-model="compare.right" class="input">
            <option value="">选择右侧版本</option>
            <option v-for="v in versions" :key="'r-' + v.id" :value="v.id">v{{ v.versionNo }}</option>
          </select>
        </div>

        <div class="version-list">
          <div v-for="v in versions" :key="v.id" class="version-item" @click="rollback(v.id)">
            <div class="version-info">
              <span class="version-number">v{{ v.versionNo }}</span>
              <span class="version-author">{{ v.editorName || v.editorId }}</span>
            </div>
            <div class="version-message">{{ v.commitMessage || '更新文档' }}</div>
          </div>
        </div>

        <div v-if="versionDiff.length > 0" class="diff-container">
          <div v-for="(line, idx) in versionDiff" :key="idx" class="diff-line" :class="line.type.toLowerCase()">
            <code class="diff-left">{{ line.left }}</code>
            <code class="diff-right">{{ line.right }}</code>
          </div>
        </div>
      </div>
    </div>

    <!-- 冲突解决弹窗 -->
    <div v-if="conflict.visible" class="modal-overlay" @click.self="conflict.visible = false">
      <div class="modal-content conflict-modal">
        <div class="modal-header">
          <h3>⚠️ 检测到冲突</h3>
        </div>
        <p class="conflict-message">{{ conflict.message }}</p>
        <div class="conflict-actions">
          <button class="btn" @click="useServerVersion">使用远端版本</button>
          <button class="btn" @click="useSuggestedVersion">插入冲突标记</button>
          <button class="btn btn-primary" @click="retryLocalVersion">保留本地并重试</button>
        </div>
      </div>
    </div>
  </div>
</template>

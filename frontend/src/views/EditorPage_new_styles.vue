<style scoped>
.editor-page {
  display: flex;
  height: calc(100vh - 48px);
  gap: 0;
  margin: -24px;
}

/* 左侧文档树 */
.doc-sidebar {
  width: 280px;
  background: var(--panel);
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--line);
}

.search-box {
  margin-bottom: 12px;
}

.search-input {
  font-size: 13px;
}

.new-doc-btn {
  width: 100%;
}

.doc-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tree-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 4px;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s ease;
}

.tree-item:hover {
  background: var(--line-light);
}

.tree-item.active {
  background: var(--brand-light);
  color: var(--brand);
}

.doc-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.tree-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-item-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.tree-item-version {
  font-size: 11px;
  color: var(--muted);
  flex-shrink: 0;
  margin-left: 8px;
}

/* 主编辑区域 */
.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--panel);
  min-width: 0;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--line);
  gap: 16px;
}

.toolbar-left {
  flex: 1;
  min-width: 0;
}

.title-input {
  font-size: 18px;
  font-weight: 600;
  border: none;
  padding: 8px 0;
}

.title-input:focus {
  box-shadow: none;
}

.toolbar-right {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.visibility-select {
  width: 120px;
  font-size: 13px;
}

/* 状态栏 */
.status-bar {
  display: flex;
  gap: 24px;
  padding: 8px 20px;
  background: var(--line-light);
  border-bottom: 1px solid var(--line);
  font-size: 12px;
}

.status-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.status-label {
  color: var(--muted);
}

.status-value {
  color: var(--text);
  font-weight: 500;
}

.status-value.link {
  color: var(--brand);
  text-decoration: none;
}

.status-value.link:hover {
  text-decoration: underline;
}

.collabStatusClass {
  /* 可以根据状态添加不同颜色 */
}

/* 协作者栏 */
.collaborators-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 20px;
  background: var(--brand-light);
  border-bottom: 1px solid var(--line);
}

.collab-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.collab-avatars {
  display: flex;
  gap: 8px;
}

.collab-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--panel);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.collab-avatar:hover {
  transform: scale(1.1);
}

.collab-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand);
  color: white;
  font-size: 12px;
  font-weight: 600;
}

/* 编辑器容器 */
.editor-container {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  overflow: hidden;
}

.editor-pane,
.preview-pane {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-pane {
  border-right: 1px solid var(--line);
}

.pane-header {
  padding: 12px 20px;
  background: var(--line-light);
  border-bottom: 1px solid var(--line);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.markdown-editor {
  flex: 1;
  width: 100%;
  padding: 20px;
  border: none;
  resize: none;
  background: var(--panel);
  color: var(--text);
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.8;
  outline: none;
}

.markdown-preview {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  line-height: 1.8;
}

.markdown-preview h1 { font-size: 28px; margin: 24px 0 16px; }
.markdown-preview h2 { font-size: 24px; margin: 20px 0 12px; }
.markdown-preview h3 { font-size: 20px; margin: 16px 0 10px; }
.markdown-preview p { margin: 12px 0; }
.markdown-preview ul, .markdown-preview ol { margin: 12px 0; padding-left: 24px; }
.markdown-preview li { margin: 6px 0; }
.markdown-preview code { background: var(--line-light); padding: 2px 6px; border-radius: 4px; }
.markdown-preview pre { background: var(--line-light); padding: 16px; border-radius: var(--radius); overflow-x: auto; margin: 16px 0; }
.markdown-preview pre code { background: none; padding: 0; }
.markdown-preview blockquote { border-left: 4px solid var(--brand); padding-left: 16px; margin: 16px 0; color: var(--text-secondary); }
.markdown-preview a { color: var(--brand); }
.markdown-preview img { max-width: 100%; border-radius: var(--radius); }

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--panel);
}

.empty-content {
  text-align: center;
  max-width: 400px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-content h3 {
  font-size: 20px;
  margin-bottom: 8px;
  color: var(--text);
}

.empty-content p {
  color: var(--text-secondary);
  margin-bottom: 24px;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: var(--panel);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  max-width: 90vw;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.version-modal {
  width: 800px;
}

.conflict-modal {
  width: 500px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--line);
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  font-size: 20px;
  padding: 4px 8px;
}

/* 版本对比 */
.version-compare {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 12px;
  padding: 16px 24px;
  background: var(--line-light);
  border-bottom: 1px solid var(--line);
}

.version-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.version-item {
  padding: 12px 16px;
  border-radius: var(--radius);
  border: 1px solid var(--line);
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.version-item:hover {
  border-color: var(--brand);
  background: var(--brand-light);
}

.version-info {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 4px;
}

.version-number {
  font-weight: 600;
  color: var(--brand);
}

.version-author {
  color: var(--text-secondary);
  font-size: 13px;
}

.version-message {
  font-size: 13px;
  color: var(--text-secondary);
}

/* Diff 显示 */
.diff-container {
  border-top: 1px solid var(--line);
  max-height: 400px;
  overflow-y: auto;
}

.diff-line {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: var(--line);
  font-size: 13px;
}

.diff-left,
.diff-right {
  padding: 8px 12px;
  background: var(--panel);
  white-space: pre-wrap;
  word-break: break-all;
}

.diff-line.added {
  background: rgba(0, 185, 107, 0.1);
}

.diff-line.removed {
  background: rgba(245, 34, 45, 0.1);
}

.diff-line.changed {
  background: rgba(255, 153, 31, 0.1);
}

/* 冲突弹窗 */
.conflict-message {
  padding: 20px 24px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.conflict-actions {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid var(--line);
  justify-content: flex-end;
}

/* 响应式 */
@media (max-width: 1200px) {
  .editor-container {
    grid-template-columns: 1fr;
  }

  .editor-pane {
    border-right: none;
    border-bottom: 1px solid var(--line);
  }

  .preview-pane {
    display: none;
  }
}

@media (max-width: 768px) {
  .editor-page {
    flex-direction: column;
  }

  .doc-sidebar {
    width: 100%;
    max-height: 200px;
  }

  .toolbar-right {
    flex-wrap: wrap;
  }
}
</style>

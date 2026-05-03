import { Message, MessageBox, Notification, Loading } from 'element-ui'

let loadingInstance

export default {
  msg(content) {
    return Message.info(content)
  },
  msgError(content) {
    return Message.error(content)
  },
  msgSuccess(content) {
    return Message.success(content)
  },
  msgWarning(content) {
    return Message.warning(content)
  },
  alert(content, title = '系统提示', options = {}) {
    return MessageBox.alert(content, title, {
      confirmButtonText: '确定',
      closeOnClickModal: false,
      ...options
    })
  },
  alertError(content, options = {}) {
    return this.alert(content, '系统提示', { type: 'error', ...options })
  },
  alertSuccess(content, options = {}) {
    return this.alert(content, '系统提示', { type: 'success', ...options })
  },
  alertWarning(content, options = {}) {
    return this.alert(content, '系统提示', { type: 'warning', ...options })
  },
  notify(content) {
    return Notification.info(content)
  },
  notifyError(content) {
    return Notification.error(content)
  },
  notifySuccess(content) {
    return Notification.success(content)
  },
  notifyWarning(content) {
    return Notification.warning(content)
  },
  confirm(content, title = '系统提示', options = {}) {
    return MessageBox.confirm(content, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      type: 'warning',
      ...options
    })
  },
  prompt(content, title = '系统提示', options = {}) {
    return MessageBox.prompt(content, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      type: 'warning',
      ...options
    })
  },
  loading(content = '正在加载，请稍候') {
    loadingInstance = Loading.service({
      lock: true,
      text: content,
      spinner: 'el-icon-loading',
      background: 'rgba(0, 0, 0, 0.7)'
    })
  },
  closeLoading() {
    if (loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }
  }
}

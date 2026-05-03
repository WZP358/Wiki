import tab from './tab'
import auth from './auth'
import cache from './cache'
import modal from './modal'
import download from './download'

export default {
  install(Vue) {
    Vue.prototype.$tab = tab
    Vue.prototype.$auth = auth
    Vue.prototype.$cache = cache
    Vue.prototype.$modal = modal
    Vue.prototype.$alert = (...args) => modal.alert(...args)
    Vue.prototype.$confirm = (...args) => modal.confirm(...args)
    Vue.prototype.$prompt = (...args) => modal.prompt(...args)
    Vue.prototype.$download = download
  }
}

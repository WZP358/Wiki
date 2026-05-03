import store from '@/store'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isHttp, isEmpty } from '@/utils/validate'
import defAva from '@/assets/images/profile.jpg'

const user = {
  state: {
    token: getToken(),
    id: '',
    name: '',
    nickName: '',
    avatar: '',
    roles: [],
    permissions: []
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_ID: (state, id) => {
      state.id = id
    },
    SET_NAME: (state, name) => {
      state.name = name
    },
    SET_NICK_NAME: (state, nickName) => {
      state.nickName = nickName
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions
    }
  },

  actions: {
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      return new Promise((resolve, reject) => {
        login(username, password).then(res => {
          setToken(res.token)
          commit('SET_TOKEN', res.token)
          store.dispatch('lock/unlockScreen')
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    GetInfo({ commit }) {
      return new Promise((resolve, reject) => {
        getInfo().then(user => {
          let avatar = user.avatarUrl || ''
          if (!isHttp(avatar)) {
            avatar = isEmpty(avatar) ? defAva : process.env.VUE_APP_BASE_API + avatar
          }
          if (user.role === 'ADMIN') {
            commit('SET_ROLES', ['admin'])
            commit('SET_PERMISSIONS', ['*:*:*'])
          } else {
            commit('SET_ROLES', ['common'])
            commit('SET_PERMISSIONS', [])
          }
          commit('SET_ID', user.id)
          commit('SET_NAME', user.username)
          commit('SET_NICK_NAME', user.nickname || user.username)
          commit('SET_AVATAR', avatar)
          resolve(user)
        }).catch(error => {
          reject(error)
        })
      })
    },

    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        logout(state.token).then(() => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          removeToken()
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '')
        commit('SET_ROLES', [])
        commit('SET_PERMISSIONS', [])
        removeToken()
        resolve()
      })
    }
  }
}

export default user

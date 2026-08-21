import { defineStore } from 'pinia'

const TOKEN_KEY = 'paper-pages:token'
const USER_KEY = 'paper-pages:username'

/** 博主登录态（JWT 存于 localStorage）。 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    username: localStorage.getItem(USER_KEY) || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    setAuth(token, username) {
      this.token = token
      this.username = username
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, username)
    },
    logout() {
      this.token = ''
      this.username = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})

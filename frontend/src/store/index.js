import { createStore } from 'vuex'
import api from '../api'

export default createStore({
  state: {
    token: localStorage.getItem('token') || null,
    currentUser: JSON.parse(localStorage.getItem('user')) || null,
    loading: false,
    theme: localStorage.getItem('theme') || 'auto',
    isDarkMode: false,
    flightDataVersion: 0
  },
  
  getters: {
    isLoggedIn: state => !!state.token,
    isAdmin: state => state.currentUser && state.currentUser.role === 'ADMIN',
    isDarkMode: state => state.isDarkMode,
    currentTheme: state => state.theme,
    flightDataVersion: state => state.flightDataVersion
  },
  
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      if (token) {
        localStorage.setItem('token', token)
        api.defaults.headers.common['Authorization'] = `Bearer ${token}`
      } else {
        localStorage.removeItem('token')
        delete api.defaults.headers.common['Authorization']
      }
    },
    
    SET_USER(state, user) {
      state.currentUser = user
      if (user) {
        localStorage.setItem('user', JSON.stringify(user))
      } else {
        localStorage.removeItem('user')
      }
    },
    
    SET_LOADING(state, loading) {
      state.loading = loading
    },
    
    SET_THEME(state, theme) {
      state.theme = theme
      localStorage.setItem('theme', theme)
    },
    
    SET_DARK_MODE(state, isDark) {
      state.isDarkMode = isDark
    },
    
    FLIGHT_DATA_UPDATED(state) {
      console.log(`[VUEX] FLIGHT_DATA_UPDATED mutation: ${state.flightDataVersion} → ${state.flightDataVersion + 1}`)
      state.flightDataVersion += 1
    }
  },
  
  actions: {
    async login({ commit }, credentials) {
      try {
        commit('SET_LOADING', true)
        const response = await api.post('/auth/login', credentials)
        
        if (response.data.success) {
          const { token, user } = response.data.data
          commit('SET_TOKEN', token)
          commit('SET_USER', user)
          return { success: true }
        } else {
          return { success: false, message: response.data.message }
        }
      } catch (error) {
        return { success: false, message: error.response?.data?.message || 'Login failed' }
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async register({ commit }, userData) {
      try {
        commit('SET_LOADING', true)
        const response = await api.post('/auth/register', userData)
        
        if (response.data.success) {
          return { success: true }
        } else {
          return { success: false, message: response.data.message }
        }
      } catch (error) {
        return { success: false, message: error.response?.data?.message || 'Registration failed' }
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    logout({ commit }) {
      commit('SET_TOKEN', null)
      commit('SET_USER', null)
    },
    
    initAuth({ state }) {
      if (state.token) {
        api.defaults.headers.common['Authorization'] = `Bearer ${state.token}`
      }
    },
    
    setTheme({ commit, dispatch }, theme) {
      commit('SET_THEME', theme)
      dispatch('updateDarkMode')
    },
    
    updateDarkMode({ commit, state }) {
      let isDark = false
      
      if (state.theme === 'dark') {
        isDark = true
      } else if (state.theme === 'light') {
        isDark = false
      } else {
        
        isDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      }
      
      commit('SET_DARK_MODE', isDark)
      
      
      if (isDark) {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    },
    
    initTheme({ dispatch, state }) {
      
      dispatch('updateDarkMode')
      
      
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if (state.theme === 'auto') {
          dispatch('updateDarkMode')
        }
      })
    },
    
    notifyFlightDataUpdate({ commit }) {
      console.log('[VUEX] notifyFlightDataUpdate action called')
      commit('FLIGHT_DATA_UPDATED')
    }
  }
})
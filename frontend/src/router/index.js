import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/flights',
    name: 'FlightSearch',
    component: () => import('../views/FlightSearch.vue'),
    meta: { requiresAuth: true, requiresCustomer: true }
  },
  {
    path: '/my-tickets',
    name: 'MyTickets',
    component: () => import('../views/MyTickets.vue'),
    meta: { requiresAuth: true, requiresCustomer: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/flights',
    name: 'AdminFlights',
    component: () => import('../views/admin/FlightManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/tickets',
    name: 'AdminTickets',
    component: () => import('../views/admin/TicketManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('../views/admin/UserManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/statistics',
    name: 'AdminStatistics',
    component: () => import('../views/admin/Statistics.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/chat-bi',
    name: 'AdminChatBi',
    component: () => import('../views/admin/SmartDashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/approval-requests',
    name: 'AdminApprovalRequests',
    component: () => import('../views/admin/ApprovalRequests.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/airlines',
    name: 'AdminAirlines',
    component: () => import('../views/admin/AirlineManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/airports',
    name: 'AdminAirports',
    component: () => import('../views/admin/AirportManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/reset',
    name: 'AdminReset',
    component: () => import('../views/admin/Reset.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/booking',
    name: 'Booking',
    component: () => import('../views/Booking.vue'),
    meta: { requiresAuth: true, requiresCustomer: true }
  },
  {
    path: '/payment/:ticketId',
    name: 'Payment',
    component: () => import('../views/Payment.vue'),
    meta: { requiresAuth: true, requiresCustomer: true }
  },
  {
    path: '/payment/success',
    name: 'PaymentSuccess', 
    component: () => import('../views/PaymentSuccess.vue'),
    meta: { requiresAuth: true, requiresCustomer: true }
  },
  {
    path: '/payment/failed',
    name: 'PaymentFailed',
    component: () => import('../views/PaymentFailed.vue'),
    meta: { requiresAuth: true, requiresCustomer: true }
  },
  {
    path: '/payment/return',
    name: 'PaymentReturn',
    redirect: to => {
      
      const paymentNumber = to.query.out_trade_no
      const result = to.query.result || to.query.trade_status
      
      if (result === 'success' || result === 'TRADE_SUCCESS') {
        return { 
          path: '/payment/success', 
          query: { paymentNumber } 
        }
      } else {
        return { 
          path: '/payment/failed', 
          query: { paymentNumber } 
        }
      }
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!store.getters.isLoggedIn) {
      next('/login')
    } else if (to.matched.some(record => record.meta.requiresAdmin)) {
      if (store.getters.isAdmin) {
        next()
      } else {
        next('/dashboard')
      }
    } else if (to.matched.some(record => record.meta.requiresCustomer)) {
      if (!store.getters.isAdmin) {
        next()
      } else {
        next('/admin/statistics')
      }
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router

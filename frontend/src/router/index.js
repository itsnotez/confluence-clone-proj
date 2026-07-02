import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', component: () => import('@/views/auth/LoginView.vue'), meta: { public: true } },
  { path: '/', redirect: '/spaces' },
  { path: '/spaces', component: () => import('@/views/space/SpaceListView.vue') },
  { path: '/spaces/:spaceKey', component: () => import('@/views/space/SpaceHomeView.vue') },
  { path: '/spaces/:spaceKey/contents/:contentId', component: () => import('@/views/content/ContentView.vue') },
  { path: '/spaces/:spaceKey/contents/new', component: () => import('@/views/content/ContentEditorView.vue') },
  { path: '/spaces/:spaceKey/contents/:contentId/edit', component: () => import('@/views/content/ContentEditorView.vue') },
  { path: '/search', component: () => import('@/views/search/SearchResultView.vue') },
  { path: '/admin', component: () => import('@/views/admin/AdminDashboardView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) return '/login'
})

export default router

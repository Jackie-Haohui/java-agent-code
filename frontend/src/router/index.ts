import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/chat',
    },
    {
      path: '/chat',
      name: 'chat-new',
      component: () => import('@/views/ChatView.vue'),
    },
    {
      path: '/chat/:conversationId',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('@/views/HistoryView.vue'),
    },
    {
      path: '/ingest',
      name: 'ingest',
      component: () => import('@/views/IngestView.vue'),
    },
  ],
})

export default router

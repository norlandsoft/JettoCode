import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/applications',
    children: [
      {
        path: 'applications',
        name: 'Applications',
        component: () => import('@/views/Applications.vue'),
        meta: { title: '应用系统' }
      },
      {
        path: 'application/:id',
        name: 'ApplicationDetail',
        component: () => import('@/views/ApplicationDetail.vue'),
        meta: { title: '应用详情' },
        children: [
          {
            path: 'services',
            name: 'Services',
            component: () => import('@/views/Services.vue'),
            meta: { title: '服务列表' }
          },
          {
            path: 'service/:serviceId',
            name: 'ServiceDetail',
            component: () => import('@/views/ServiceDetail.vue'),
            meta: { title: '服务详情' },
            redirect: to => ({ path: `/application/${to.params.id}/service/${to.params.serviceId}/info` }),
            children: [
              {
                path: 'info',
                name: 'ServiceInfo',
                component: () => import('@/views/ServiceInfo.vue'),
                meta: { title: '服务信息' }
              },
              {
                path: 'code',
                name: 'CodeView',
                component: () => import('@/views/CodeView.vue'),
                meta: { title: '代码浏览' }
              },
              {
                path: 'architecture',
                name: 'Architecture',
                component: () => import('@/views/Architecture.vue'),
                meta: { title: '项目架构' }
              },
              {
                path: 'flow',
                name: 'BusinessFlow',
                component: () => import('@/views/BusinessFlow.vue'),
                meta: { title: '业务流程' }
              },
              {
                path: 'call-chain',
                name: 'CallChain',
                component: () => import('@/views/CallChain.vue'),
                meta: { title: '调用链路' }
              },
              {
                path: 'interface-trace',
                name: 'InterfaceTrace',
                component: () => import('@/views/InterfaceTrace.vue'),
                meta: { title: '接口追踪' }
              },
              {
                path: 'diff',
                name: 'DiffAnalysis',
                component: () => import('@/views/DiffAnalysis.vue'),
                meta: { title: 'Diff分析' }
              }
            ]
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || '代码分析平台'
  next()
})

export default router

<template>
  <div class="coming-soon-page">
    <div class="coming-soon-content">
      <div class="icon-wrapper">
        <ShareAltOutlined />
      </div>
      <h2>业务流程分析</h2>
      <p>此功能正在开发中，敬请期待</p>
      <div class="features-list">
        <div class="feature-item">
          <CheckCircleOutlined />
          <span>业务流程可视化</span>
        </div>
        <div class="feature-item">
          <CheckCircleOutlined />
          <span>流程节点追踪</span>
        </div>
        <div class="feature-item">
          <CheckCircleOutlined />
          <span>性能瓶颈识别</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ShareAltOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
</script>

<style scoped>
.coming-soon-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-3xl);
}

.coming-soon-content {
  text-align: center;
  max-width: 400px;
}

.icon-wrapper {
  width: 72px;
  height: 72px;
  margin: 0 auto var(--spacing-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3e8ff;
  border-radius: var(--radius-xl);
  font-size: 32px;
  color: #9333ea;
}

h2 {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-xs);
}

p {
  font-size: var(--font-size-base);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--spacing-xl);
}

.features-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-base);
}

.feature-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-base);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.feature-item .anticon {
  color: var(--color-success);
}
</style>

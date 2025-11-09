<template>
  <footer class="app-footer">
    <div class="footer-container">
      <div class="footer-content">
        <!-- Logo 和描述 -->
        <div class="footer-section footer-brand">
          <div class="footer-logo">
            <span class="logo-text">xCoder</span>
          </div>
          <p class="footer-description">
            专业的AI服务平台,为开发者提供编程AI解决方案。
          </p>
        </div>

        <!-- 联系我们 -->
        <div class="footer-section">
          <h3 class="footer-title">联系我们</h3>
          <ul class="footer-links">
            <li class="footer-link-item">
              <span class="footer-icon">📧</span>
              <span class="footer-label">邮箱:</span>
              <a href="mailto:support@xcoder.plus" class="footer-link">
                support@xcoder.plus
              </a>
            </li>
          </ul>
        </div>
      </div>

      <!-- 版权信息 -->
      <div class="footer-bottom">
        <p class="copyright">© 2025 xCoder</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  background: var(--color-bg-elevated);
  border-top: 1px solid var(--color-border-light);
  margin-top: var(--spacing-16);
  padding: var(--spacing-12) 0 var(--spacing-6);
}

.footer-container {
  max-width: var(--container-max-width);
  margin: 0 auto;
  padding: 0 var(--container-padding);
}

.footer-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--spacing-12);
  margin-bottom: var(--spacing-8);
}

/* Logo 和品牌 */
.footer-brand {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}

.logo-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 800;
}

.footer-description {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-relaxed);
  margin: 0;
  max-width: 400px;
}

/* Footer Section */
.footer-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.footer-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.footer-links {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.footer-link-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  font-size: var(--font-size-sm);
}

.footer-icon {
  font-size: var(--font-size-base);
}

.footer-label {
  color: var(--color-text-secondary);
  min-width: 80px;
}

.footer-link {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.footer-link:hover {
  color: var(--color-primary-hover);
  text-decoration: underline;
}

/* 版权信息 */
.footer-bottom {
  padding-top: var(--spacing-6);
  border-top: 1px solid var(--color-border-light);
  text-align: center;
}

.copyright {
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
  margin: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .footer-content {
    grid-template-columns: 1fr;
    gap: var(--spacing-8);
  }

  .footer-container {
    padding: 0 var(--spacing-4);
  }

  .app-footer {
    padding: var(--spacing-8) 0 var(--spacing-4);
  }
}
</style>

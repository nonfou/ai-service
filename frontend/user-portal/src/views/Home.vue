<script setup lang="ts">
import { useUserStore } from '../stores/user'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

// 根据登录状态决定"立即开始"按钮的行为
const handleGetStarted = () => {
  if (userStore.isLoggedIn) {
    router.push('/subscriptions')
  } else {
    router.push('/login?redirect=/subscriptions')
  }
}

// 计算按钮文本
const ctaButtonText = computed(() => {
  return userStore.isLoggedIn ? '前往控制台' : '立即开始'
})
</script>

<template>
  <div class="home-page">
    <!-- Hero -->
    <section class="hero">
      <div class="container hero-grid">
        <div class="hero-content">
          <span class="hero-badge">AI 开发者平台</span>
          <h1>xCoder</h1>
          <p class="hero-tagline">多模型协同的一站式智能研发平台</p>
          <p class="hero-description">
            专为开发者打造的 AI 能力平台。统一调度 GPT‑5、Claude、Gemini 等主流模型，无缝集成 IDE、终端与 CI/CD，让 AI 真正赋能开发全流程。
          </p>
          <div class="hero-actions">
            <button @click="handleGetStarted" class="btn btn-primary">
              {{ ctaButtonText }}
              <span class="btn-icon">→</span>
            </button>
          </div>
          <div class="hero-stats">
            <div class="stat-card">
              <div class="stat-value">12K+</div>
              <div class="stat-label">活跃开发者</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">99.9%</div>
              <div class="stat-label">服务可用性</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">24/7</div>
              <div class="stat-label">技术支持</div>
            </div>
          </div>
        </div>
        <div class="hero-visual">
          <div class="hero-visual-card">
            <div class="hero-visual-header">
              <span class="hero-visual-dot"></span>
              开发者控制台
            </div>
            <div class="hero-visual-body">
              <div class="hero-visual-column">
                <h4>API 管理</h4>
                <ul>
                  <li>多模型统一调用</li>
                  <li>实时用量监控</li>
                  <li>密钥安全管理</li>
                </ul>
              </div>
              <div class="hero-visual-column">
                <h4>开发工具</h4>
                <ul>
                  <li>在线调试测试</li>
                  <li>完整调用日志</li>
                  <li>代码示例生成</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Platform Support -->
    <section class="platform-support">
      <div class="container">
        <div class="section-heading">
          <h2>核心能力</h2>
          <p>为开发者打造的完整 AI 开发解决方案</p>
        </div>
        <div class="support-panels">
          <div class="support-card">
            <h3>🚀 快速接入</h3>
            <div class="feature-list">
              <div class="feature-item">
                <span class="feature-check">✓</span>
                <div>
                  <strong>统一 API 标准</strong>
                  <p>兼容 OpenAI SDK，零学习成本快速迁移</p>
                </div>
              </div>
              <div class="feature-item">
                <span class="feature-check">✓</span>
                <div>
                  <strong>简单易用</strong>
                  <p>三行代码即可开始调用，5分钟完成集成</p>
                </div>
              </div>
              <div class="feature-item">
                <span class="feature-check">✓</span>
                <div>
                  <strong>详细文档</strong>
                  <p>完善的 API 文档和代码示例</p>
                </div>
              </div>
            </div>
          </div>
          <div class="support-card">
            <h3>⚡ 开发者体验</h3>
            <div class="feature-list">
              <div class="feature-item">
                <span class="feature-check">✓</span>
                <div>
                  <strong>实时监控面板</strong>
                  <p>API 调用量、成本、性能一目了然</p>
                </div>
              </div>
              <div class="feature-item">
                <span class="feature-check">✓</span>
                <div>
                  <strong>灵活计费控制</strong>
                  <p>按需付费，实时查看用量和成本统计</p>
                </div>
              </div>
              <div class="feature-item">
                <span class="feature-check">✓</span>
                <div>
                  <strong>高并发支持</strong>
                  <p>企业级负载均衡，轻松应对流量高峰</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Why Choose -->
    <section class="why-us">
      <div class="container">
        <div class="section-heading">
          <h2>为何选择 xCoder</h2>
          <p>为开发者提供稳定、可靠的 AI 模型调用服务，让你专注于打造卓越产品。</p>
        </div>
        <div class="why-grid">
          <div class="why-card">
            <div class="why-icon why-icon-purple">🤝</div>
            <h3>开发者友好</h3>
            <p>兼容 OpenAI 标准接口，三行代码即可调用，丰富的文档和示例助你快速上手。</p>
          </div>
          <div class="why-card">
            <div class="why-icon why-icon-green">📈</div>
            <h3>稳定可靠</h3>
            <p>企业级基础设施，全球多区域部署，为开发者提供 99.9% 服务可用性保障。</p>
          </div>
          <div class="why-card">
            <div class="why-icon why-icon-orange">💰</div>
            <h3>按需付费</h3>
            <p>灵活的计费模式，实时监控 Token 消耗，开发者可自主控制成本。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta">
      <div class="container">
        <h2>开启你的 AI 开发之旅</h2>
        <p>立即开始构建你的 AI 应用，体验强大的模型能力。</p>
        <div class="cta-actions">
          <button @click="handleGetStarted" class="btn btn-contrast">{{ ctaButtonText }}</button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  background: linear-gradient(180deg, #f8f8ff 0%, #ffffff 100%);
  color: #0f172a;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.75rem;
}

/* Hero */
.hero {
  position: relative;
  padding: 6rem 0 4rem;
  background: linear-gradient(135deg, #f7f9ff 0%, #f1f0ff 100%);
  overflow: hidden;
}

.hero::before,
.hero::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.65;
  pointer-events: none;
}

.hero::before {
  background: radial-gradient(600px at -20% -20%, rgba(99, 102, 241, 0.4), transparent 60%),
    radial-gradient(600px at 120% 20%, rgba(59, 130, 246, 0.35), transparent 60%);
}

.hero::after {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.65) 0%, rgba(255, 255, 255, 0) 40%);
}

.hero-grid {
  position: relative;
  display: grid;
  gap: 3.5rem;
  align-items: center;
  z-index: 1;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.hero-content h1 {
  font-size: 3.5rem;
  font-weight: 800;
  margin-bottom: 1.25rem;
  letter-spacing: -0.04em;
  color: #1f2937;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.25rem;
  border-radius: 999px;
  font-size: 0.875rem;
  font-weight: 600;
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
  border: 1px solid rgba(99, 102, 241, 0.25);
  margin-bottom: 1.5rem;
}

.hero-tagline {
  font-size: 1.5rem;
  font-weight: 600;
  color: #312e81;
  margin-bottom: 1rem;
}

.hero-description {
  font-size: 1.05rem;
  line-height: 1.75;
  color: #475569;
  margin-bottom: 2rem;
  max-width: 540px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 2.5rem;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.9rem 2.25rem;
  border-radius: 0.95rem;
  font-weight: 600;
  letter-spacing: 0.01em;
  transition: transform 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  text-decoration: none;
}

.btn-primary {
  background: linear-gradient(135deg, #7c3aed 0%, #2563eb 100%);
  color: #ffffff;
  box-shadow: 0 20px 45px rgba(79, 70, 229, 0.25);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 24px 60px rgba(79, 70, 229, 0.35);
}

.btn-outline {
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(148, 163, 184, 0.6);
  color: #1f2937;
  backdrop-filter: blur(6px);
}

.btn-outline:hover {
  background: rgba(255, 255, 255, 0.85);
  transform: translateY(-2px);
}

.btn-icon {
  font-size: 1.15rem;
  transition: transform 0.25s ease;
}

.btn-primary:hover .btn-icon {
  transform: translateX(4px);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  max-width: 480px;
}

.stat-card {
  padding: 1.5rem 1rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(226, 232, 240, 0.8);
  text-align: center;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.stat-label {
  font-size: 0.95rem;
  color: #64748b;
}

.hero-visual {
  display: flex;
  justify-content: center;
}

.hero-visual-card {
  position: relative;
  width: 100%;
  max-width: 500px;
  border-radius: 1.75rem;
  padding: 2.5rem 2rem;
  background: radial-gradient(circle at top, rgba(79, 70, 229, 0.12), rgba(59, 130, 246, 0.12)),
    linear-gradient(180deg, rgba(248, 250, 252, 0.95), rgba(241, 245, 249, 0.95));
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: 0 34px 70px rgba(30, 64, 175, 0.25);
  overflow: hidden;
}

.hero-visual-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at -10% 120%, rgba(124, 58, 237, 0.22), transparent 50%),
    radial-gradient(circle at 110% -20%, rgba(59, 130, 246, 0.18), transparent 52%);
  pointer-events: none;
}

.hero-visual-header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #312e81;
  margin-bottom: 1.5rem;
}

.hero-visual-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #22c55e, #0ea5e9);
  box-shadow: 0 0 12px rgba(14, 197, 126, 0.45);
}

.hero-visual-body {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.75rem;
  z-index: 1;
}

.hero-visual-column h4 {
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 0.75rem;
}

.hero-visual-column ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.6rem;
  color: #475569;
  font-size: 0.95rem;
}

.hero-visual-column ul li::before {
  content: '•';
  color: #7c3aed;
  margin-right: 0.35rem;
}

/* Section Heading */
.section-heading {
  text-align: center;
  margin-bottom: 3rem;
}

.section-heading h2 {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 0.75rem;
  color: #1f2937;
}

.section-heading p {
  font-size: 1.05rem;
  color: #64748b;
  max-width: 640px;
  margin: 0 auto;
}

/* Platform Support */
.platform-support {
  padding: 5rem 0;
}

.support-panels {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.support-card {
  padding: 2.25rem 2rem;
  background: #ffffff;
  border-radius: 1.5rem;
  border: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
}

.support-card h3 {
  font-size: 1.35rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
}

.ide-grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.ide-item {
  text-align: center;
  padding: 1.25rem 1rem;
  border-radius: 1.25rem;
  border: 1px solid rgba(226, 232, 240, 0.6);
  background: #f8fafc;
}

.ide-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  margin: 0 auto 0.75rem;
}

.ide-icon-blue {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
}

.ide-icon-purple {
  background: linear-gradient(135deg, #a855f7, #7c3aed);
}

.ide-icon-orange {
  background: linear-gradient(135deg, #fb923c, #f97316);
}

.ide-icon-green {
  background: linear-gradient(135deg, #10b981, #059669);
}

.ide-item span {
  display: block;
  font-weight: 600;
  color: #1f2937;
}

.ide-item small {
  display: block;
  color: #64748b;
  margin-top: 0.25rem;
}

.language-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.language-tags span {
  padding: 0.6rem 1.2rem;
  border-radius: 999px;
  font-size: 0.9rem;
  background: #f1f5f9;
  color: #1e293b;
  border: 1px solid rgba(148, 163, 184, 0.35);
}

.language-tags .highlight {
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  color: #ffffff;
  border-color: transparent;
}

/* Feature List */
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  background: rgba(248, 250, 252, 0.5);
  border-radius: 0.75rem;
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(124, 58, 237, 0.05);
  transform: translateX(4px);
}

.feature-check {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: white;
  border-radius: 50%;
  font-weight: 700;
  font-size: 0.85rem;
}

.feature-item strong {
  display: block;
  color: #1f2937;
  font-size: 1rem;
  margin-bottom: 0.25rem;
}

.feature-item p {
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.5;
  margin: 0;
}

.support-card h3 {
  font-size: 1.35rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
}

/* Why Us */
.why-us {
  padding: 5rem 0;
  background: linear-gradient(180deg, rgba(124, 58, 237, 0.05), rgba(37, 99, 235, 0.05));
}

.why-grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.why-card {
  padding: 2.25rem 2rem;
  border-radius: 1.5rem;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.09);
  text-align: center;
}

.why-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.25rem;
  font-size: 1.75rem;
  color: #ffffff;
}

.why-icon-purple {
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
}

.why-icon-green {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}

.why-icon-orange {
  background: linear-gradient(135deg, #fb923c, #f97316);
}

.why-card h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
}

.why-card p {
  color: #64748b;
  line-height: 1.6;
}

/* Pricing */
.pricing {
  padding: 5rem 0;
  background: #f8fafc;
}

.pricing-grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.pricing-card {
  position: relative;
  padding: 2.25rem 2rem;
  border-radius: 1.5rem;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.pricing-card-featured {
  border: 1px solid rgba(124, 58, 237, 0.4);
  background: linear-gradient(180deg, rgba(124, 58, 237, 0.08), rgba(37, 99, 235, 0.08));
  box-shadow: 0 28px 70px rgba(76, 29, 149, 0.15);
}

.pricing-tag {
  position: absolute;
  top: -14px;
  left: 50%;
  transform: translateX(-50%);
  padding: 0.35rem 1rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  box-shadow: 0 12px 30px rgba(79, 70, 229, 0.35);
}

.pricing-card h3 {
  font-size: 1.4rem;
  font-weight: 700;
}

.pricing-price {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1f2937;
}

.pricing-price span {
  font-size: 1rem;
  color: #64748b;
  margin-left: 0.35rem;
}

.pricing-card ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.75rem;
  color: #475569;
}

.pricing-card ul li::before {
  content: '✔';
  color: #22c55e;
  margin-right: 0.5rem;
}

.pricing-btn {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  padding: 0.85rem 1.5rem;
  border-radius: 0.9rem;
  border: 1px solid rgba(148, 163, 184, 0.7);
  font-weight: 600;
  color: #1f2937;
  background: #ffffff;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.pricing-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 35px rgba(15, 23, 42, 0.12);
}

.pricing-btn-gradient {
  border: none;
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  color: #ffffff;
  box-shadow: 0 20px 45px rgba(79, 70, 229, 0.25);
}

.pricing-note {
  text-align: center;
  margin-top: 2rem;
  color: #475569;
  font-size: 0.95rem;
}

/* CTA */
.cta {
  padding: 5rem 0;
  text-align: center;
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  color: #ffffff;
}

.cta h2 {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

.cta p {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 2.5rem;
}

.cta-actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.btn-contrast {
  background: #ffffff;
  color: #1f2937;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.2);
}

.btn-contrast:hover {
  transform: translateY(-2px);
}

.btn-ghost {
  border: 1px solid rgba(255, 255, 255, 0.35);
  color: #ffffff;
  padding: 0.9rem 2.25rem;
}

.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

/* Responsive */
@media (max-width: 1024px) {
  .hero {
    padding-top: 4.5rem;
  }

  .hero-content h1 {
    font-size: 3rem;
  }

  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-visual-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .container {
    padding: 0 1.25rem;
  }

  .hero {
    padding: 4rem 0 3rem;
  }

  .hero-content h1 {
    font-size: 2.4rem;
  }

  .hero-tagline {
    font-size: 1.25rem;
  }

  .hero-description {
    font-size: 0.98rem;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .hero-visual-card {
    padding: 2.25rem 1.75rem;
  }

  .section-heading h2 {
    font-size: 2rem;
  }

  .support-card,
  .why-card,
  .pricing-card {
    padding: 2rem 1.5rem;
  }

  .cta h2 {
    font-size: 2rem;
  }
}

@media (max-width: 480px) {
  .hero-content h1 {
    font-size: 2.1rem;
  }

  .hero-description {
    font-size: 0.95rem;
  }

  .btn {
    width: 100%;
  }
}
</style>

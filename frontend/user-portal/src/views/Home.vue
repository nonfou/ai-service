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
            统一调度 GPT‑5、Claude、Gemini 等头部模型，接入 IDE、终端与 CI/CD，帮助团队快速构建、测试与发布应用。
          </p>
          <div class="hero-actions">
            <router-link to="/getting-started" class="btn btn-primary">
              立即开始
              <span class="btn-icon">→</span>
            </router-link>
            <router-link to="/subscriptions" class="btn btn-outline">订阅套餐</router-link>
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
              xCoder Workbench
            </div>
            <div class="hero-visual-body">
              <div class="hero-visual-column">
                <h4>多模型路由</h4>
                <ul>
                  <li>自动选择最优模型</li>
                  <li>Token 用量实时可视</li>
                  <li>权限隔离与审计</li>
                </ul>
              </div>
              <div class="hero-visual-column">
                <h4>智能流水线</h4>
                <ul>
                  <li>生成 → 评审 → 测试</li>
                  <li>一键同步到 Git</li>
                  <li>CI 失败自动补救</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Products -->
    <section class="products">
      <div class="container">
        <div class="section-heading">
          <h2>核心能力</h2>
          <p>基于原生多模型编排与自动化流水线，为团队打造高效率、可控的智能开发体验。</p>
        </div>
        <div class="product-grid">
          <div class="product-card product-card-purple">
            <span class="product-badge">智能代码副驾</span>
            <h3>xCoder Copilot</h3>
            <p class="product-desc">
              深度融合 Sonnet、Opus、GPT‑5，提供上下文感知的代码补全、调试与文档生成体验。
            </p>
            <ul>
              <li>IDE 原生提示与测试驱动拆解</li>
              <li>多语言知识库即时检索</li>
              <li>Pair Programming 协同模式</li>
            </ul>
            <router-link to="/getting-started" class="product-link">了解用法</router-link>
          </div>
          <div class="product-card product-card-blue">
            <span class="product-badge">企业级 API 网关</span>
            <h3>xCoder Gateway</h3>
            <p class="product-desc">
              一键聚合内部与公有模型，统一鉴权、计费与配额，满足多业务线并行需求。
            </p>
            <ul>
              <li>流量路由与熔断策略</li>
              <li>可视化 Token 报表</li>
              <li>安全审计与访问隔离</li>
            </ul>
            <router-link to="/models" class="product-link">查看模型</router-link>
          </div>
          <div class="product-card product-card-orange">
            <span class="product-badge">自动化研发流水线</span>
            <h3>xCoder Pipeline</h3>
            <p class="product-desc">
              将生成、评审、回归测试与部署串联成闭环，保持模型输出的可控性与可追溯。
            </p>
            <ul>
              <li>多阶段 Prompt Workflow</li>
              <li>CI 集成与缺陷回溯</li>
              <li>团队知识库持续学习</li>
            </ul>
            <router-link to="/dashboard" class="product-link">体验面板</router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="features">
      <div class="container">
        <div class="section-heading">
          <h2>覆盖全生命周期的智能协同</h2>
          <p>从需求拆解到上线运维，xCoder 提供标准化流程与模型协同工具，帮助团队稳定输出高质量代码。</p>
        </div>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon feature-icon-purple">⚡</div>
            <h3>极速启动</h3>
            <p>三分钟完成 CLI 与 IDE 配置，一键导入工程上下文，立即投入开发。</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon feature-icon-blue">🔍</div>
            <h3>代码理解</h3>
            <p>自动生成概要、依赖图与函数说明，帮助新人与外包快速融入项目。</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon feature-icon-orange">🛠</div>
            <h3>实时修复</h3>
            <p>捕获编译与测试失败，提供修复建议并支持一键生成补丁与 MR 草稿。</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon feature-icon-green">🧠</div>
            <h3>多模型编排</h3>
            <p>根据语言、规模与延迟自动路由模型，兼顾准确率、成本与体验。</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon feature-icon-pink">🧾</div>
            <h3>自动文档</h3>
            <p>规范化输出 API 文档、变更说明与上线 checklist，减少重复劳动。</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon feature-icon-teal">🔐</div>
            <h3>全链路安全</h3>
            <p>传输加密、操作审计与隐私洗稿保障，把控敏感代码与数据流向。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Platform Support -->
    <section class="platform-support">
      <div class="container">
        <div class="section-heading">
          <h2>生态兼容</h2>
          <p>原生适配主流 IDE、终端与自动化工具链，无需改造即可嵌入现有流程。</p>
        </div>
        <div class="support-panels">
          <div class="support-card">
            <h3>热门 IDE 原生适配</h3>
            <div class="ide-grid">
              <div class="ide-item">
                <div class="ide-icon ide-icon-blue"></div>
                <span>VS Code</span>
                <small>官方扩展</small>
              </div>
              <div class="ide-item">
                <div class="ide-icon ide-icon-purple"></div>
                <span>Cursor</span>
                <small>深度协作</small>
              </div>
              <div class="ide-item">
                <div class="ide-icon ide-icon-orange"></div>
                <span>JetBrains</span>
                <small>全系列支持</small>
              </div>
              <div class="ide-item">
                <div class="ide-icon ide-icon-green"></div>
                <span>Vim / Neovim</span>
                <small>终端驱动</small>
              </div>
            </div>
          </div>
          <div class="support-card">
            <h3>支持 100+ 主流语言</h3>
            <div class="language-tags">
              <span>JavaScript</span>
              <span>TypeScript</span>
              <span>Python</span>
              <span>Go</span>
              <span>Java</span>
              <span>Rust</span>
              <span>C++</span>
              <span>PHP</span>
              <span>Swift</span>
              <span class="highlight">+ 更多语言</span>
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
          <p>我们将智能模型能力与工程方法论结合，持续交付可验证的业务价值。</p>
        </div>
        <div class="why-grid">
          <div class="why-card">
            <div class="why-icon why-icon-purple">🤝</div>
            <h3>多模型协同</h3>
            <p>统一管理自研模型与公有模型，支持私有化部署与跨云容灾。</p>
          </div>
          <div class="why-card">
            <div class="why-icon why-icon-green">📈</div>
            <h3>高可用架构</h3>
            <p>全球多区域容灾，毫秒级路由切换，完善限流与监控报警体系。</p>
          </div>
          <div class="why-card">
            <div class="why-icon why-icon-orange">💰</div>
            <h3>精细化成本</h3>
            <p>按团队规模灵活扩展，实时掌握 token 与算力消耗，成本可预测。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Comparison -->
    <section class="comparison">
      <div class="container">
        <div class="section-heading">
          <h2>方案对比</h2>
          <p>xCoder 与常见智能开发方案的差异。</p>
        </div>
        <div class="comparison-table-wrapper">
          <table class="comparison-table">
            <thead>
              <tr>
                <th>能力项</th>
                <th>xCoder</th>
                <th>Cursor Pro</th>
                <th>自建方案</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>月费价格</td>
                <td><span class="table-positive">$10 起</span></td>
                <td>$20</td>
                <td>不封顶</td>
              </tr>
              <tr>
                <td>Claude Code 支持</td>
                <td class="table-positive">✔</td>
                <td>✖</td>
                <td>需额外商务</td>
              </tr>
              <tr>
                <td>Codex 支持</td>
                <td class="table-positive">✔</td>
                <td>✖</td>
                <td>需自建调度</td>
              </tr>
              <tr>
                <td>终端调用</td>
                <td class="table-positive">不限</td>
                <td>500 次</td>
                <td>需自研 CLI</td>
              </tr>
              <tr>
                <td>IDE 支持</td>
                <td class="table-positive">VS Code / JetBrains / Cursor</td>
                <td>Cursor</td>
                <td>需自行集成</td>
              </tr>
              <tr>
                <td>技术支持</td>
                <td class="table-positive">7×24 小时</td>
                <td>社区支持</td>
                <td>自研团队</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- Pricing -->
    <section class="pricing">
      <div class="container">
        <div class="section-heading">
          <h2>透明计费</h2>
          <p>根据团队规模灵活选择，按月付费随时升级。</p>
        </div>
        <div class="pricing-grid">
          <div class="pricing-card">
            <h3>成长版</h3>
            <div class="pricing-price">
              $1
              <span>/月</span>
            </div>
            <ul>
              <li>2000 调用额度</li>
              <li>个人与小团队</li>
              <li>基础模型访问</li>
            </ul>
            <router-link to="/subscriptions" class="pricing-btn">立即体验</router-link>
          </div>
          <div class="pricing-card pricing-card-featured">
            <span class="pricing-tag">最受欢迎</span>
            <h3>团队版</h3>
            <div class="pricing-price">
              $50
              <span>/月</span>
            </div>
            <ul>
              <li>50,000 调用额度</li>
              <li>团队协作与子账号</li>
              <li>企业级安全策略</li>
            </ul>
            <router-link to="/subscriptions" class="pricing-btn pricing-btn-gradient">预约开通</router-link>
          </div>
          <div class="pricing-card">
            <h3>企业版</h3>
            <div class="pricing-price">
              $100
              <span>/月</span>
            </div>
            <ul>
              <li>100,000 调用额度</li>
              <li>专属技术支持</li>
              <li>多模型混合编排</li>
            </ul>
            <router-link to="/tickets" class="pricing-btn">联系顾问</router-link>
          </div>
        </div>
        <p class="pricing-note">
          更高配额可定制私有化部署方案，支持本地化合规与 SLA 定制。
        </p>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta">
      <div class="container">
        <h2>准备好升级团队的研发效率了吗？</h2>
        <p>加入 12,000+ 开发者，共同构建高效可信的智能研发体系。</p>
        <div class="cta-actions">
          <router-link to="/getting-started" class="btn btn-contrast">立即试用</router-link>
          <router-link to="/documentation" class="btn btn-ghost">浏览文档</router-link>
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

/* Products */
.products {
  padding: 5rem 0;
}

.product-grid {
  display: grid;
  gap: 1.75rem;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.product-card {
  position: relative;
  padding: 2.5rem 2rem;
  border-radius: 1.75rem;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 24px 55px rgba(15, 23, 42, 0.08);
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.product-card::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.12);
}

.product-card:hover::after {
  opacity: 1;
}

.product-card-purple::after {
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.12), transparent 60%);
}

.product-card-blue::after {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12), transparent 60%);
}

.product-card-orange::after {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.12), transparent 60%);
}

.product-card h3 {
  font-size: 1.65rem;
  font-weight: 700;
  margin-bottom: 0.75rem;
  color: #111827;
  position: relative;
}

.product-badge {
  display: inline-flex;
  padding: 0.35rem 0.95rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
  background: rgba(15, 23, 42, 0.05);
  color: #475569;
  margin-bottom: 1rem;
}

.product-desc {
  color: #475569;
  line-height: 1.65;
  margin-bottom: 1.5rem;
}

.product-card ul {
  list-style: none;
  padding: 0;
  margin: 0 0 1.75rem 0;
  display: grid;
  gap: 0.75rem;
  color: #334155;
  font-size: 0.98rem;
}

.product-card ul li::before {
  content: '✔';
  color: #22c55e;
  margin-right: 0.5rem;
}

.product-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-weight: 600;
  color: #4338ca;
}

.product-link::after {
  content: '→';
  transition: transform 0.25s ease;
}

.product-link:hover::after {
  transform: translateX(4px);
}

/* Features */
.features {
  padding: 5rem 0;
  background: #f8fafc;
}

.features-grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.feature-card {
  padding: 2rem 1.75rem;
  border-radius: 1.5rem;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 16px 40px rgba(14, 23, 42, 0.08);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  text-align: left;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 55px rgba(14, 23, 42, 0.12);
}

.feature-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  margin-bottom: 1.25rem;
  color: #ffffff;
}

.feature-icon-purple {
  background: linear-gradient(135deg, #a855f7, #6366f1);
}

.feature-icon-blue {
  background: linear-gradient(135deg, #3b82f6, #0ea5e9);
}

.feature-icon-orange {
  background: linear-gradient(135deg, #f97316, #fb7185);
}

.feature-icon-green {
  background: linear-gradient(135deg, #10b981, #22c55e);
}

.feature-icon-pink {
  background: linear-gradient(135deg, #ec4899, #c026d3);
}

.feature-icon-teal {
  background: linear-gradient(135deg, #14b8a6, #06b6d4);
}

.feature-card h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
}

.feature-card p {
  color: #64748b;
  line-height: 1.6;
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

/* Comparison */
.comparison {
  padding: 5rem 0;
}

.comparison-table-wrapper {
  background: #ffffff;
  border-radius: 1.5rem;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.1);
  overflow-x: auto;
}

.comparison-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 640px;
}

.comparison-table thead {
  background: linear-gradient(135deg, #7c3aed, #2563eb);
  color: #ffffff;
}

.comparison-table th,
.comparison-table td {
  padding: 1.2rem 1.5rem;
  text-align: left;
  font-size: 0.98rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
}

.comparison-table tbody tr:nth-child(even) td {
  background: rgba(248, 250, 252, 0.8);
}

.table-positive {
  color: #16a34a;
  font-weight: 600;
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

  .product-card,
  .feature-card,
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

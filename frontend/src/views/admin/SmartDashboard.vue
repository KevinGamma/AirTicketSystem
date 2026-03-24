<template>
  <div class="smart-dashboard">
    <div class="smart-dashboard__hero">
      <span>ChatBI</span>
      <h1>管理员智能经营看板</h1>
      <p>面向运营分析的自然语言查询入口，自动完成 SQL 生成、执行和图表渲染。</p>
    </div>

    <AdminSmartChart />
  </div>
</template>

<script>
import AdminSmartChart from '../../components/AdminSmartChart.vue'

export default {
  name: 'SmartDashboard',
  components: {
    AdminSmartChart
  }
}
</script>

<style scoped>
.smart-dashboard {
  display: grid;
  gap: 24px;
}

.smart-dashboard__hero {
  padding: 28px 32px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.18), transparent 34%),
    linear-gradient(135deg, #082f49, #164e63 58%, #0f766e);
  color: #f8fafc;
}

.smart-dashboard__hero span {
  display: inline-flex;
  margin-bottom: 12px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  letter-spacing: 0.12em;
}

.smart-dashboard__hero h1 {
  margin: 0 0 10px;
  font-size: 34px;
}

.smart-dashboard__hero p {
  margin: 0;
  max-width: 720px;
  color: rgba(248, 250, 252, 0.82);
}

@media (max-width: 768px) {
  .smart-dashboard__hero {
    padding: 22px;
  }

  .smart-dashboard__hero h1 {
    font-size: 28px;
  }
}
</style>

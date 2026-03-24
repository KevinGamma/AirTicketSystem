<template>
  <section class="smart-chart">
    <el-card class="smart-chart__composer" shadow="never">
      <div class="smart-chart__heading">
        <div>
          <h2>智能经营看板</h2>
          <p>输入自然语言，系统会自动生成 SQL、执行查询并推荐图表类型。</p>
        </div>
        <div class="smart-chart__badges">
          <span>只读查询</span>
          <span>自动限流 100 行</span>
          <span>失败自动修正一次</span>
        </div>
      </div>

      <el-input
        v-model="question"
        type="textarea"
        :rows="4"
        resize="none"
        placeholder="例如：查询本月各航司的票务收入排行"
        @keydown.ctrl.enter.prevent="submitQuery"
      />

      <div class="smart-chart__suggestions">
        <el-button
          v-for="item in suggestions"
          :key="item"
          plain
          size="small"
          @click="question = item"
        >
          {{ item }}
        </el-button>
      </div>

      <div class="smart-chart__actions">
        <span>支持 `Ctrl + Enter` 快速执行</span>
        <el-button type="primary" :loading="loading" @click="submitQuery">
          开始分析
        </el-button>
      </div>
    </el-card>

    <el-card v-if="result" class="smart-chart__result" shadow="never">
      <div class="smart-chart__meta">
        <div class="smart-chart__meta-item">
          <label>推荐图表</label>
          <strong>{{ result.chartType }}</strong>
        </div>
        <div class="smart-chart__meta-item">
          <label>返回行数</label>
          <strong>{{ result.rowCount }}</strong>
        </div>
        <div class="smart-chart__meta-item">
          <label>执行状态</label>
          <strong>{{ result.repaired ? '已自动修正' : '首次成功' }}</strong>
        </div>
      </div>

      <div class="smart-chart__sql">
        <div class="smart-chart__section-title">执行 SQL</div>
        <pre>{{ result.sql }}</pre>
      </div>

      <div v-if="result.originalSql" class="smart-chart__sql smart-chart__sql--secondary">
        <div class="smart-chart__section-title">首次失败 SQL</div>
        <pre>{{ result.originalSql }}</pre>
      </div>

      <div class="smart-chart__section-title">图表预览</div>
      <div class="smart-chart__visual">
        <v-chart
          v-if="showChart"
          class="smart-chart__canvas"
          :option="chartOption"
          autoresize
        />
        <el-empty v-else description="当前结果更适合表格展示" />
      </div>

      <div class="smart-chart__section-title">明细结果</div>
      <el-table :data="result.rows" stripe border class="smart-chart__table">
        <el-table-column
          v-for="column in tableColumns"
          :key="column"
          :prop="column"
          :label="column"
          min-width="160"
          show-overflow-tooltip
        />
      </el-table>
    </el-card>
  </section>
</template>

<script>
import api from '../api'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
])

export default {
  name: 'AdminSmartChart',
  components: {
    VChart
  },
  data() {
    return {
      loading: false,
      question: '查询本月各航司的票务收入排行',
      result: null,
      suggestions: [
        '查询本月各航司的票务收入排行',
        '统计各支付方式的订单占比',
        '查看最近30天订单趋势',
        '分析各出发地的平均票价'
      ]
    }
  },
  computed: {
    tableColumns() {
      if (!this.result?.rows?.length) {
        return []
      }
      return Object.keys(this.result.rows[0])
    },
    chartMapping() {
      const rows = this.result?.rows || []
      if (!rows.length) {
        return null
      }

      const keys = Object.keys(rows[0])
      if (!keys.length) {
        return null
      }

      const metricKey = keys.find(key => rows.some(row => this.isNumericValue(row[key]))) || keys[1]
      const dimensionCandidates = keys.filter(key => key !== metricKey)
      const temporalKey = dimensionCandidates.find(key => /(date|time|month|day|week|year)/i.test(key))
      const dimensionKey = temporalKey || dimensionCandidates[0] || keys[0]

      if (!dimensionKey || !metricKey || dimensionKey === metricKey) {
        return null
      }

      return {
        dimensionKey,
        metricKey,
        labels: rows.map(row => this.formatDimension(row[dimensionKey])),
        values: rows.map(row => this.toNumber(row[metricKey]))
      }
    },
    showChart() {
      if (!this.result || this.result.chartType === 'TABLE') {
        return false
      }
      return Boolean(this.chartMapping)
    },
    chartOption() {
      if (!this.chartMapping || !this.result) {
        return {}
      }

      const { dimensionKey, metricKey, labels, values } = this.chartMapping
      const title = `${dimensionKey} / ${metricKey}`

      if (this.result.chartType === 'LINE') {
        return {
          tooltip: { trigger: 'axis' },
          grid: { left: 48, right: 24, top: 48, bottom: 32 },
          xAxis: {
            type: 'category',
            data: labels,
            axisLabel: { color: '#5b6472' }
          },
          yAxis: {
            type: 'value',
            axisLabel: { color: '#5b6472' }
          },
          series: [
            {
              type: 'line',
              smooth: true,
              data: values,
              lineStyle: { width: 3, color: '#1d4ed8' },
              itemStyle: { color: '#1d4ed8' },
              areaStyle: { color: 'rgba(29, 78, 216, 0.12)' }
            }
          ]
        }
      }

      if (this.result.chartType === 'PIE') {
        return {
          tooltip: { trigger: 'item' },
          legend: { bottom: 0 },
          series: [
            {
              type: 'pie',
              radius: ['35%', '68%'],
              center: ['50%', '44%'],
              data: labels.map((name, index) => ({
                name,
                value: values[index]
              }))
            }
          ]
        }
      }

      return {
        title: {
          text: title,
          left: 'center',
          textStyle: { fontSize: 14, fontWeight: 600 }
        },
        tooltip: { trigger: 'axis' },
        grid: { left: 48, right: 24, top: 56, bottom: 56 },
        xAxis: {
          type: 'category',
          data: labels,
          axisLabel: {
            color: '#5b6472',
            rotate: labels.length > 6 ? 25 : 0
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#5b6472' }
        },
        series: [
          {
            type: 'bar',
            barMaxWidth: 48,
            data: values,
            itemStyle: {
              borderRadius: [10, 10, 0, 0],
              color: '#0f766e'
            }
          }
        ]
      }
    }
  },
  methods: {
    async submitQuery() {
      if (!this.question.trim()) {
        ElMessage.warning('请输入分析问题')
        return
      }

      this.loading = true
      try {
        const response = await api.post('/admin/chat-bi/query', {
          question: this.question.trim()
        })

        if (!response.data.success) {
          throw new Error(response.data.message || '分析失败')
        }

        this.result = response.data.data
        ElMessage.success('智能分析完成')
      } catch (error) {
        this.result = null
        ElMessage.error(error.response?.data?.message || error.message || '分析失败')
      } finally {
        this.loading = false
      }
    },
    isNumericValue(value) {
      if (value === null || value === undefined || value === '') {
        return false
      }
      if (typeof value === 'number') {
        return true
      }
      return /^-?\d+(\.\d+)?$/.test(String(value).trim())
    },
    toNumber(value) {
      return Number(value) || 0
    },
    formatDimension(value) {
      if (value === null || value === undefined || value === '') {
        return '-'
      }
      return String(value)
    }
  }
}
</script>

<style scoped>
.smart-chart {
  display: grid;
  gap: 24px;
}

.smart-chart__composer,
.smart-chart__result {
  border-radius: 24px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.08), transparent 32%),
    linear-gradient(180deg, #ffffff, #f8fafc);
}

.smart-chart__heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.smart-chart__heading h2 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #0f172a;
}

.smart-chart__heading p {
  margin: 0;
  color: #475569;
}

.smart-chart__badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.smart-chart__badges span {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.08);
  color: #0f766e;
  font-size: 13px;
  white-space: nowrap;
}

.smart-chart__suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.smart-chart__actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 18px;
  color: #64748b;
  font-size: 13px;
}

.smart-chart__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.smart-chart__meta-item {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.04);
}

.smart-chart__meta-item label {
  display: block;
  margin-bottom: 6px;
  color: #64748b;
  font-size: 13px;
}

.smart-chart__meta-item strong {
  color: #0f172a;
  font-size: 18px;
}

.smart-chart__section-title {
  margin: 18px 0 10px;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.smart-chart__sql pre {
  margin: 0;
  padding: 16px;
  border-radius: 18px;
  background: #0f172a;
  color: #e2e8f0;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.smart-chart__sql--secondary pre {
  background: #334155;
}

.smart-chart__visual {
  min-height: 380px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
}

.smart-chart__canvas {
  width: 100%;
  height: 380px;
}

.smart-chart__table {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .smart-chart__heading,
  .smart-chart__actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .smart-chart__badges {
    justify-content: flex-start;
  }

  .smart-chart__meta {
    grid-template-columns: 1fr;
  }
}
</style>

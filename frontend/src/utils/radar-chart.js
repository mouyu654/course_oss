import * as echarts from 'echarts'

/**
 * 渲染专业达成度雷达图
 * @param {HTMLElement} dom - 容器 DOM 元素
 * @param {Array<{ name: string, value: number }>} data - 指标点数据
 */
export function renderRadarChart(dom, data) {
  const chart = echarts.init(dom)

  const indicators = data.map((d) => ({
    name: d.name,
    max: 1.0,
  }))

  const values = data.map((d) => Number(d.value.toFixed(4)))

  chart.setOption({
    radar: {
      center: ['50%', '50%'],
      radius: '65%',
      indicator: indicators,
      axisName: { color: '#606266', fontSize: 12 },
      splitArea: {
        areaStyle: { color: ['transparent'] },
      },
      splitLine: { lineStyle: { color: '#e0e0e0' } },
      axisLine: { lineStyle: { color: '#e0e0e0' } },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: values,
            name: '达成度',
            areaStyle: { color: 'rgba(64, 158, 255, 0.15)' },
            lineStyle: { color: '#409EFF', width: 2 },
            itemStyle: { color: '#409EFF' },
            label: {
              show: true,
              formatter: (p) => p.value.toFixed(4),
              color: '#303133',
              fontSize: 12,
            },
          },
        ],
      },
    ],
  })

  return chart
}

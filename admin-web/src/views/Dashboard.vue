<script setup>
import{ref,onMounted,onBeforeUnmount,nextTick,watch}from'vue';
import*as echarts from'echarts/core';
import{LineChart,PieChart}from'echarts/charts';
import{GridComponent,TooltipComponent,LegendComponent}from'echarts/components';
import{CanvasRenderer}from'echarts/renderers';
import{api}from'../api';
echarts.use([LineChart,PieChart,GridComponent,TooltipComponent,LegendComponent,CanvasRenderer]);
const PALETTE=['#246c59','#d0a95f','#4b8d7a','#b27327','#8c6a34','#5a9b87','#3f7d6c','#c98e3d'];
const data=ref({sales:0,todaySales:0,orders:0,todayOrders:0,verifications:0,todayVerifications:0,weekVerifications:0,monthVerifications:0,members:0,todayNewMembers:0});
const iso=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}T00:00:00`;
const end=new Date(),start=new Date(end.getTime()-29*86400000);
const range=ref([iso(start),iso(end)]);
const salesRef=ref(),vrfRef=ref(),methodRef=ref(),catRef=ref();
let salesChart,vrfChart,methodChart,catChart;
const onResize=()=>[salesChart,vrfChart,methodChart,catChart].forEach(c=>c&&c.resize());
const drawSales=s=>salesChart.setOption({grid:{left:50,right:20,top:30,bottom:35},tooltip:{trigger:'axis'},xAxis:{type:'category',data:s.map(x=>x.date),axisLine:{lineStyle:{color:'#d7e3de'}}},yAxis:{type:'value',splitLine:{lineStyle:{color:'#edf2f0'}}},series:[{name:'实收销售额',type:'line',smooth:true,data:s.map(x=>x.amount),symbolSize:7,lineStyle:{width:3,color:'#246c59'},itemStyle:{color:'#d0a95f'},areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#4b8d7a55'},{offset:1,color:'#4b8d7a05'}])}}]});
const drawVrf=s=>vrfChart.setOption({grid:{left:45,right:20,top:30,bottom:35},tooltip:{trigger:'axis'},xAxis:{type:'category',data:s.map(x=>x.date),axisLine:{lineStyle:{color:'#d7e3de'}}},yAxis:{type:'value',splitLine:{lineStyle:{color:'#edf2f0'}}},series:[{name:'核销人次',type:'line',smooth:true,data:s.map(x=>x.count),symbolSize:7,lineStyle:{width:3,color:'#b27327'},itemStyle:{color:'#d0a95f'},areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#b2732744'},{offset:1,color:'#b2732705'}])}}]});
const pie=(c,d,name)=>c.setOption({tooltip:{trigger:'item',formatter:'{b}: {c} ({d}%)'},legend:{bottom:0},color:PALETTE,series:[{name,type:'pie',radius:['40%','68%'],itemStyle:{borderColor:'#fff',borderWidth:2},label:{formatter:'{b}\n{d}%'},data:d}]});
const loadVrf=async()=>{const s=await api.verificationTrend(range.value?.[0],range.value?.[1]);drawVrf(s);};
onMounted(async()=>{
  const[o,s,m,c]=await Promise.all([api.overview(),api.sales(),api.salesByMethod(),api.category()]);
  data.value={...data.value,...o};
  await nextTick();
  salesChart=echarts.init(salesRef.value);vrfChart=echarts.init(vrfRef.value);methodChart=echarts.init(methodRef.value);catChart=echarts.init(catRef.value);
  drawSales(s);pie(methodChart,m.map(x=>({name:x.method,value:Number(x.amount)})),'支付方式金额');pie(catChart,c.map(x=>({name:x.category,value:x.count})),'品类订单');
  await loadVrf();
  addEventListener('resize',onResize);
});
watch(range,()=>{if(vrfChart)loadVrf();});
onBeforeUnmount(()=>{removeEventListener('resize',onResize);[salesChart,vrfChart,methodChart,catChart].forEach(c=>c&&c.dispose());});
</script>
<template>
  <div class="page-head"><div><h2>今日经营概览</h2><p>按已支付订单实收金额与核销人次统计，数据实时更新</p></div><el-tag type="success" effect="plain">系统运行中</el-tag></div>
  <div class="cards">
    <div class="metric"><small>累计销售额</small><strong>¥ {{data.sales}}</strong><em>微信与现金合计</em></div>
    <div class="metric"><small>今日销售额</small><strong>¥ {{data.todaySales}}</strong><em>今日 {{data.todayOrders}} 单</em></div>
    <div class="metric"><small>累计核销</small><strong>{{data.verifications}}</strong><em>现场使用记录</em></div>
    <div class="metric"><small>会员总数</small><strong>{{data.members}}</strong><em>今日新增 {{data.todayNewMembers}}</em></div>
    <div class="metric"><small>今日核销人次</small><strong>{{data.todayVerifications}}</strong><em>当日权益使用</em></div>
    <div class="metric"><small>本周核销人次</small><strong>{{data.weekVerifications}}</strong><em>本周一至今</em></div>
    <div class="metric"><small>本月核销人次</small><strong>{{data.monthVerifications}}</strong><em>本月一日至今</em></div>
    <div class="metric"><small>售卡订单</small><strong>{{data.orders}}</strong><em>累计已支付</em></div>
  </div>
  <div class="chart-grid">
    <div class="panel"><div class="panel-title">销售趋势</div><div ref="salesRef" class="chart-box"></div></div>
    <div class="panel">
      <div class="panel-head"><div class="panel-title">核销人次趋势</div>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DDTHH:mm:ss" start-placeholder="开始" end-placeholder="结束" size="small" :clearable="false" style="width:240px"/></div>
      <div ref="vrfRef" class="chart-box"></div>
    </div>
    <div class="panel"><div class="panel-title">销售支付方式占比</div><div ref="methodRef" class="chart-box"></div></div>
    <div class="panel"><div class="panel-title">品类销售占比</div><div ref="catRef" class="chart-box"></div></div>
  </div>
</template>

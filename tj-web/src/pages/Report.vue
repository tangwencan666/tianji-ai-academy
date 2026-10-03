<script setup>
import { ref, onMounted } from "vue";
import { api } from "../api";
import ReportCard from "../components/ReportCard.vue";
const data = ref(null),
  report = ref(null),
  busy = ref(false);
onMounted(async () => (data.value = await api.get("/learning/dashboard")));
async function generate() {
  busy.value = true;
  try {
    report.value = await api.get("/ai/report/weekly");
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">REFLECT ON YOUR JOURNEY</div>
      <h1>回望，是为了更好地向前。</h1>
      <p>用真实记录观察成长，让下周的学习更有方向。</p>
    </div>
    <el-button type="primary" :loading="busy" @click="generate"
      >✧ 生成 AI 周报</el-button
    >
  </div>
  <div class="stats" v-if="data">
    <div class="stat">
      <span>最近 7 天学习</span
      ><strong
        >{{ Math.floor(data.time.weekSeconds / 60)
        }}<small> 分钟</small></strong
      >
    </div>
    <div class="stat">
      <span>累计已学习课时</span
      ><strong>{{ data.records.length }}<small> 节</small></strong>
    </div>
    <div class="stat">
      <span>已覆盖知识点</span
      ><strong>{{ data.mastery.length }}<small> 个</small></strong>
    </div>
    <div class="stat">
      <span>最近考试记录</span
      ><strong>{{ data.exams.length }}<small> 次</small></strong>
    </div>
  </div>
  <div class="card">
    <h2>知识掌握情况</h2>
    <el-table :data="data?.mastery || []"
      ><el-table-column prop="name" label="知识点" /><el-table-column
        prop="practiceCount"
        label="练习次数" /><el-table-column label="累计正确率"
        ><template #default="{ row }"
          ><el-progress
            :percentage="row.correctRate" /></template></el-table-column
      ><el-table-column prop="recentAccuracy" label="最近10次正确率 (%)"
    /></el-table>
  </div>
  <div class="card spaced" v-if="report">
    <div class="eyebrow">AI WEEKLY REFLECTION</div>
    <p>
      近7天已判分 {{ report.facts.answerCount }} 次，正确率
      {{ report.facts.accuracy }}%
    </p>
    <ReportCard :report="report.report" />
  </div>
  <div v-else class="note-card">
    <h3>给这一周留一点思考时间。</h3>
    <p>点击生成周报，让 AI 结合学习时长、答题情况和知识掌握度给出建议。</p>
  </div>
</template>

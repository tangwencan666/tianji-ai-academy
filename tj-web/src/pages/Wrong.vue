<script setup>
import { ref, onMounted } from "vue";
import { api } from "../api";
import QuestionCard from "../components/QuestionCard.vue";
import ReportCard from "../components/ReportCard.vue";
const wrongs = ref([]),
  questions = ref([]),
  report = ref(null),
  busy = ref(false);
onMounted(async () => {
  wrongs.value = await api.get("/wrong-questions/details");
  questions.value = wrongs.value.map(w => w.question);
});
async function explain(row) {
  busy.value=true;
  try { report.value=await api.post(`/ai/answers/${row.answerRecordId}/explain`); }
  finally {busy.value=false;}
}
async function analyze() {
  busy.value = true;
  try {
    report.value = await api.post("/ai/wrong-analysis");
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">REVIEW & GROW</div>
      <h1>把错题变成收获。</h1>
      <p>错误不是终点，而是理解开始的地方。</p>
    </div>
    <el-button type="primary" @click="analyze" :loading="busy"
      >✧ AI 错因分析</el-button
    >
  </div>
  <ReportCard :report="report" />
  <section v-for="row in wrongs" :key="row.wrong.id" class="card spaced">
    <span class="tag">累计错误 {{row.wrong.wrongCount}} 次 · {{row.wrong.masteryStatus === 'MASTERED' ? '已掌握' : '待复习'}}</span>
    <h3>{{row.question.stem}}</h3>
    <p>我的答案：{{row.myAnswer || '未作答'}}</p>
    <p>正确答案：{{row.question.answer}}</p>
    <p>知识点：{{row.knowledgePoints.join('、')}}</p>
    <p>{{row.question.explanation}}</p>
    <el-button :loading="busy" @click="explain(row)">✧ AI 讲解本次错误</el-button>
    <details><summary>重新练习</summary><QuestionCard :question="row.question" /></details>
  </section>
  <el-empty
    v-if="!questions.length"
    description="目前没有可复习的错题，继续保持！"
  />
</template>

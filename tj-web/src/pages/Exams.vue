<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from "vue";
import { api } from "../api";
import CourseSelect from "../components/CourseSelect.vue";
import { ElMessageBox, ElMessage } from "element-plus";
const course = ref(null),
  exams = ref([]),
  history = ref([]),
  paper = ref(null),
  answers = ref({}),
  busy = ref(false),
  now = ref(Date.now());
const timer = setInterval(() => (now.value = Date.now()), 1000);
const remaining = computed(() =>
  paper.value
    ? Math.max(
        0,
        Math.floor(
          (new Date(paper.value.deadline + "+08:00").getTime() - now.value) /
            1000,
        ),
      )
    : 0,
);
watch(
  course,
  async (id) =>
    (exams.value = await api.get("/exams", { params: { courseId: id } })),
);
onMounted(load);
onUnmounted(() => clearInterval(timer));
async function load() {
  history.value = await api.get("/exam-records");
  const active = history.value.find((x) => x.status === "IN_PROGRESS");
  if (active) {
    paper.value = await api.get(`/exam-records/${active.id}`);
    answers.value = {};
  }
}
async function start(id) {
  await ElMessageBox.confirm(
    "考试期间学习辅助功能将暂停。准备好开始了吗？",
    "开始考试",
  );
  busy.value = true;
  try {
    paper.value = await api.post(`/exams/${id}/start`);
    answers.value = {};
  } finally {
    busy.value = false;
  }
}
async function submit() {
  busy.value = true;
  try {
    const normalized = Object.fromEntries(
      Object.entries(answers.value).map(([k, v]) => [
        k,
        Array.isArray(v) ? v.join(",") : v,
      ]),
    );
    const r = await api.post(`/exam-records/${paper.value.record.id}/submit`, {
      answers: normalized,
    });
    ElMessage.success(
      `已交卷：${r.score}/${r.maxScore}，${r.status === "NEEDS_REVIEW" ? "简答题等待复核" : r.status}`,
    );
    paper.value = null;
    await load();
  } finally {
    busy.value = false;
  }
}
function options(q) {
  try {
    return JSON.parse(q.options);
  } catch {
    return [];
  }
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">TEST YOUR UNDERSTANDING</div>
      <h1>认真作答，看见进步。</h1>
      <p>客观题即时判分，简答题由教师确认最终成绩。</p>
    </div>
    <CourseSelect v-if="!paper" v-model="course" />
  </div>
  <div v-if="paper">
    <div class="exam-banner">
      <strong>{{ paper.exam.title }}</strong
      ><span
        >剩余 {{ Math.floor(remaining / 60) }}:{{
          String(remaining % 60).padStart(2, "0")
        }}{{ !remaining ? " · 已超时，提交将按超时处理" : "" }}</span
      ><el-button type="primary" @click="submit" :loading="busy"
        >提交试卷</el-button
      >
    </div>
    <article
      v-for="(q, index) in paper.questions"
      :key="q.id"
      class="card question"
    >
      <div class="eyebrow">第 {{ index + 1 }} 题 · {{ q.maxScore }} 分</div>
      <h3>{{ q.stem }}</h3>
      <el-input
        v-if="q.type === 'SHORT'"
        v-model="answers[q.id]"
        type="textarea"
        :rows="4"
        maxlength="8000"
      /><el-checkbox-group
        v-else-if="q.type === 'MULTIPLE'"
        :model-value="answers[q.id] || []"
        @update:model-value="answers[q.id] = $event"
        ><el-checkbox
          v-for="(o, i) in options(q)"
          :key="i"
          :value="String.fromCharCode(65 + i)"
          >{{ o }}</el-checkbox
        ></el-checkbox-group
      ><el-radio-group v-else v-model="answers[q.id]"
        ><el-radio
          v-for="(o, i) in options(q)"
          :key="i"
          :value="String.fromCharCode(65 + i)"
          >{{ String.fromCharCode(65 + i) }}. {{ o }}</el-radio
        ></el-radio-group
      >
    </article>
  </div>
  <template v-else
    ><div class="card exam-row" v-for="e in exams" :key="e.id">
      <div>
        <h3>{{ e.title }}</h3>
        <p>{{ e.durationMinutes }} 分钟</p>
      </div>
      <el-button type="primary" @click="start(e.id)" :loading="busy"
        >开始考试 →</el-button
      >
    </div>
    <el-empty v-if="!exams.length" description="本课程暂无考试" />
    <h2>我的考试记录</h2>
    <el-table :data="history"
      ><el-table-column prop="id" label="记录" /><el-table-column
        prop="startTime"
        label="开始时间" /><el-table-column
        prop="score"
        label="得分" /><el-table-column
        prop="maxScore"
        label="总分" /><el-table-column prop="status" label="状态" /></el-table
  ></template>
</template>

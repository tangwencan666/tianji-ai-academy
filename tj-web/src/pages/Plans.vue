<script setup>
import { ref, onMounted } from "vue";
import { api, streamAi } from "../api";
import CourseSelect from "../components/CourseSelect.vue";
const course = ref(null),
  goal = ref("准备 Java 后端面试，巩固基础并完成练习"),
  days = ref(7),
  minutes = ref(120),
  busy = ref(false),
  plans = ref([]),
  selected = ref(null),
  steps = ref([]);
const status=ref(''),error=ref('');
async function load() {
  plans.value = await api.get("/study-plans");
  if (plans.value.length && !selected.value) await open(plans.value[0].id);
}
onMounted(load);
async function open(id) {
  selected.value = await api.get(`/study-plans/${id}`);
}
async function generate() {
  busy.value = true;
  steps.value = [];
  error.value='';
  try {
    let r;
    await streamAi('/ai/plans/stream', {
      courseId: course.value,
      goal: goal.value,
      days: days.value,
      dailyMinutes: minutes.value,
    },(name,value)=>{
      if(name==='status')status.value=value;
      if(name==='result')r=value;
      if(name==='error')throw new Error(value);
    });
    if(!r)throw new Error('未收到已验证的计划');
    steps.value = r.steps;
    await open(r.plan.id);
    await load();
  } catch(e) {
    error.value=e.message;
  } finally {
    busy.value = false;
    status.value='';
  }
}
async function done(d) {
  await api.post(`/study-plans/days/${d.id}/complete`);
  d.completed = true;
}
function decode(v) {
  try {
    return JSON.parse(v);
  } catch {
    return [];
  }
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">A LITTLE PROGRESS, EVERY DAY</div>
      <h1>让目标，有迹可循。</h1>
      <p>结合学习进度和薄弱点，生成适合你的行动计划。</p>
    </div>
  </div>
  <div class="card plan-form">
    <CourseSelect v-model="course" /><el-input
      v-model="goal"
      placeholder="描述学习目标"
      maxlength="1000"
    /><label>天数 <el-input-number v-model="days" :min="1" :max="30" /></label
    ><label
      >每天分钟
      <el-input-number v-model="minutes" :min="15" :max="240" /></label
    ><el-button
      type="primary"
      @click="generate"
      :disabled="!course || !goal"
      :loading="busy"
      >✧ 生成学习计划</el-button
    >
    <p v-if="busy" class="quiet">
      {{status || '正在连接学习助手…'}}
    </p>
  </div>
  <div v-if="steps.length" class="steps">
    <span v-for="(s, i) in steps" :key="s">✓ {{ i + 1 }}. {{ s }}</span>
  </div>
  <p v-if="error" class="error-text">{{error}}</p>
  <div class="toolbar spaced">
    <el-button
      v-for="p in plans"
      :key="p.id"
      @click="open(p.id)"
      :type="selected?.plan.id === p.id ? 'primary' : ''"
      >{{ p.startDate }} · {{ p.goal.slice(0, 16) }}</el-button
    >
  </div>
  <div v-if="selected">
    <h2>{{ selected.plan.goal }}</h2>
    <div class="plan-days">
      <article class="card" v-for="(d, i) in selected.days" :key="d.id">
        <div class="day-number">
          DAY {{ String(i + 1).padStart(2, "0") }}
          <span>{{ d.studyDate }}</span>
        </div>
        <h3 v-for="t in decode(d.topics)" :key="t">{{ t }}</h3>
        <router-link
          v-for="id in decode(d.lessonIds)"
          :key="id"
          :to="'/learn/' + id"
          class="tag"
          >学习课时 {{ id }} ↗</router-link
        >
        <p>
          练习 {{ d.practiceCount }} 道 · 预计 {{ d.estimatedMinutes }} 分钟
        </p>
        <p class="quiet">
          复习：{{
            decode(d.reviewKnowledgePoints).join("、") || "巩固当日内容"
          }}
        </p>
        <el-button @click="done(d)" :disabled="d.completed">{{
          d.completed ? "✓ 已完成" : "标记完成"
        }}</el-button>
      </article>
    </div>
  </div>
  <el-empty v-else description="生成你的第一份学习计划" />
</template>

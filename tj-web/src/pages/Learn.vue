<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { useRoute } from "vue-router";
import { api } from "../api";
import Tutor from "./Tutor.vue";
const id = Number(useRoute().params.id),
  lesson = ref(null),
  detail = ref(null),
  progress = ref(0),
  position = ref(0),
  busy = ref(false);
let timer,
  last = Date.now();
async function save(done = false) {
  if (!lesson.value || busy.value) return;
  busy.value = true;
  try {
    const duration = Math.min(300, Math.floor((Date.now() - last) / 1000));
    const r = await api.put(`/learning/lessons/${id}`, {
      duration,
      progress: done ? 100 : progress.value,
      lastPosition: Math.floor(position.value),
    });
    progress.value = r.progress;
    last = Date.now();
  } finally {
    busy.value = false;
  }
}
function video(e) {
  position.value = e.target.currentTime;
  if (e.target.duration)
    progress.value = Math.max(
      progress.value,
      Math.min(99, Math.floor((position.value / e.target.duration) * 100)),
    );
}
onMounted(async () => {
  lesson.value = await api.get(`/lessons/${id}`);
  detail.value = await api.get(`/courses/${lesson.value.courseId}`);
  const records = await api.get("/learning/records");
  const r = records.find((x) => x.lessonId === id);
  progress.value = r?.progress || 0;
  position.value = r?.lastPosition || 0;
  await save();
  timer = setInterval(() => {
    if (document.visibilityState === "visible") save().catch(() => {});
  }, 30000);
});
onUnmounted(() => clearInterval(timer));
</script>
<template>
  <div v-if="lesson">
    <div class="page-heading">
      <div>
        <div class="eyebrow">FOCUS MODE · 专注学习</div>
        <h1>{{ lesson.title }}</h1>
        <p>预计 {{ lesson.estimatedMinutes }} 分钟 · 已学习 {{ progress }}%</p>
      </div>
      <router-link :to="'/tutor?courseId=' + lesson.courseId" class="button"
        >✧ 请教 AI 导师</router-link
      >
    </div>
    <div class="learning-workspace">
      <aside class="card">
        <h3>课程目录</h3>
        <router-link
          v-for="l in detail?.lessons"
          :key="l.id"
          :to="'/learn/' + l.id"
          class="lesson-row"
          :class="{ selected: l.id === id }"
          >{{ l.id === id ? "●" : "○" }} {{ l.title }}</router-link
        >
      </aside>
      <article class="card lesson-content">
        <video
          v-if="lesson.videoUrl"
          controls
          :src="lesson.videoUrl"
          @timeupdate="video"
          @loadedmetadata="$event.target.currentTime = position"
        />
        <div class="article-text">
          {{ lesson.content || "本课时暂无文字讲义。" }}
        </div>
        <a
          v-if="lesson.attachment"
          :href="lesson.attachment"
          target="_blank"
          rel="noopener noreferrer"
          class="text-link"
          >打开课程附件 ↗</a
        >
        <div class="actions">
          <el-button type="primary" :loading="busy" @click="save(true)"
            >✓ 标记本节已完成</el-button
          ><el-button @click="save()" :loading="busy">保存进度</el-button>
        </div>
      </article>
      <Tutor :course-id="lesson.courseId" embedded :key="lesson.courseId" />

    </div>
  </div>
</template>

<style scoped>
.learning-workspace { display:grid; grid-template-columns:200px minmax(320px,1fr) minmax(300px,360px);gap:18px;align-items:start; }
.learning-workspace .lesson-row { font-size:13px; }
@media(max-width:1450px){.learning-workspace{grid-template-columns:170px minmax(0,1fr)}.embedded-tutor{grid-column:1 / -1}}
@media(max-width:800px){.learning-workspace{display:flex;flex-direction:column}.learning-workspace>*{width:100%}}
</style>

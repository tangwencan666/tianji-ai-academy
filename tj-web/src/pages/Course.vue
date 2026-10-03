<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { api } from "../api";
import { ElMessage } from "element-plus";
const id = Number(useRoute().params.id),
  detail = ref(null),
  progress = ref(null);
onMounted(async () => {
  [detail.value, progress.value] = await Promise.all([
    api.get(`/courses/${id}`),
    api.get(`/learning/courses/${id}`),
  ]);
});
async function favorite() {
  await api.put(`/favorites/${id}`);
  ElMessage.success("已加入收藏");
}
</script>
<template>
  <div v-if="detail">
    <router-link to="/courses" class="text-link">← 返回课程广场</router-link>
    <div class="course-intro">
      <img v-if="detail.course.cover" :src="detail.course.cover" alt="课程封面" class="detail-cover" />
      <div v-else class="detail-cover cover-placeholder">{{ detail.course.category }} · KNOWLEDGE INTO PRACTICE</div>
      <div class="eyebrow">
        {{ detail.course.category }} / {{ detail.course.difficulty }}
      </div>
      <h1>{{ detail.course.title }}</h1>
      <p>{{ detail.course.description }}</p>
      <p>授课教师：{{ detail.teacherName }} · {{ detail.course.totalLessons }} 节课</p>
      <div class="actions">
        <router-link
          v-if="detail.lessons.length"
          :to="'/learn/' + detail.lessons[0].id"
          class="button"
          >开始学习 →</router-link
        ><el-button @click="favorite">☆ 收藏课程</el-button>
      </div>
    </div>
    <div class="dashboard-grid">
      <div class="card">
        <h2>课程目录</h2>
        <section v-for="h in detail.chapters" :key="h.id" class="chapter">
          <h3>{{ h.title }}</h3>
          <router-link
            v-for="l in detail.lessons.filter((l) => l.chapterId === h.id)"
            :key="l.id"
            :to="'/learn/' + l.id"
            class="lesson-row"
            ><span>▷ {{ l.title }}</span
            ><small>{{ l.estimatedMinutes }} 分钟 →</small></router-link
          >
        </section>
        <el-empty
          v-if="!detail.chapters.length"
          description="教师正在准备课程内容"
        />
      </div>
      <div>
        <div class="card">
          <h3>我的学习进度</h3>
          <el-progress :percentage="progress?.percent || 0" />
          <p class="quiet">
            已完成 {{ progress?.completed }} / {{ progress?.totalLessons }} 节
          </p>
        </div>
        <div class="card spaced">
          <h3>你将掌握</h3>
          <span class="tag" v-for="k in detail.knowledgePoints" :key="k.id">{{
            k.name
          }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>.detail-cover{width:100%;max-height:220px;object-fit:cover;border-radius:18px;margin-bottom:24px}.cover-placeholder{padding:40px;background:linear-gradient(120deg,#d9e9d9,#f3e4cc);font-size:24px;color:#245840}</style>

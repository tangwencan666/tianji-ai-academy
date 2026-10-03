<script setup>
import { ref, watch } from "vue";
import { api } from "../api";
import CourseSelect from "../components/CourseSelect.vue";
import QuestionCard from "../components/QuestionCard.vue";
const course = ref(null),
  questions = ref([]);
watch(
  course,
  async (id) =>
    (questions.value = await api.get("/questions", {
      params: { courseId: id },
    })),
);
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">LEARN BY DOING</div>
      <h1>用练习，检验理解。</h1>
      <p>先独立思考，再和 AI 一起拆解难点。</p>
    </div>
    <CourseSelect v-model="course" />
  </div>
  <QuestionCard v-for="q in questions" :key="q.id" :question="q" /><el-empty
    v-if="!questions.length"
    description="本课程暂无已审核练习题"
  />
</template>

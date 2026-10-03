<script setup>
import { ref, computed } from "vue";
import { api } from "../api";
import ReportCard from "./ReportCard.vue";
const props = defineProps({ question: Object });
const answer = ref(""),
  multi = ref([]),
  result = ref(null),
  explanation = ref(null),
  busy = ref(false),
  grading = ref(null);
const options = computed(() => {
  try {
    return JSON.parse(props.question.options || "[]");
  } catch {
    return [];
  }
});
async function submit() {
  busy.value = true;
  try {
    result.value = await api.post(`/questions/${props.question.id}/practice`, {
      answer:
        props.question.type === "MULTIPLE"
          ? multi.value.join(",")
          : answer.value,
    });
    explanation.value = null;
    grading.value = null;
  } finally {
    busy.value = false;
  }
}
async function ai(kind) {
  busy.value = true;
  try {
    const r = await api.post(`/ai/answers/${result.value.result.id}/${kind}`);
    if (kind === "grade") grading.value = r;
    else explanation.value = r;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <article class="card question">
    <div class="eyebrow">{{ question.type }} · {{ question.difficulty }}</div>
    <h3>{{ question.stem }}</h3>
    <el-input
      v-if="question.type === 'SHORT'"
      v-model="answer"
      type="textarea"
      :rows="4"
      placeholder="写下你的理解…"
    /><el-checkbox-group
      v-else-if="question.type === 'MULTIPLE'"
      v-model="multi"
      ><el-checkbox
        v-for="(o, i) in options"
        :key="i"
        :value="String.fromCharCode(65 + i)"
        >{{ String.fromCharCode(65 + i) }}. {{ o }}</el-checkbox
      ></el-checkbox-group
    ><el-radio-group v-else v-model="answer"
      ><el-radio
        v-for="(o, i) in options"
        :key="i"
        :value="String.fromCharCode(65 + i)"
        >{{ String.fromCharCode(65 + i) }}. {{ o }}</el-radio
      ></el-radio-group
    >
    <div class="actions">
      <el-button
        type="primary"
        :loading="busy"
        @click="submit"
        :disabled="!answer && !multi.length"
        >提交练习</el-button
      >
    </div>
    <div v-if="result" class="feedback">
      <strong>{{
        result.result.reviewStatus === "NEEDS_REVIEW"
          ? "已提交，等待教师复核"
          : result.result.correct
            ? "回答正确"
            : "再想一想，你离理解又近了一步"
      }}</strong>
      <p>参考答案：{{ result.answer }}</p>
      <p>{{ result.explanation }}</p>
      <el-button :loading="busy" @click="ai('explain')">✧ AI 讲解</el-button
      ><el-button
        v-if="question.type === 'SHORT'"
        :loading="busy"
        @click="ai('grade')"
        >AI 辅助批改</el-button
      ><ReportCard :report="explanation" />
      <div v-if="grading">
        <p>{{ grading.note }}</p>
        <p>
          建议得分 {{ grading.suggestion.score }} /
          {{ grading.suggestion.maxScore }} · 置信度
          {{ grading.suggestion.confidence }}
        </p>
        <p>{{ grading.suggestion.feedback }}</p>
        <ul>
          <li v-for="s in grading.suggestion.suggestions" :key="s">{{ s }}</li>
        </ul>
      </div>
    </div>
  </article>
</template>

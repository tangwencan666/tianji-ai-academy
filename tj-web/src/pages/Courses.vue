<script setup>
import { ref, onMounted } from "vue";
import { api } from "../api";
const courses = ref([]),
  search = ref(""),
  category = ref(""),
  categories = ref([]),
  fav = ref(false),
  loading = ref(false);
async function load() {
  loading.value = true;
  try {
    courses.value = await api.get(fav.value ? "/favorites" : "/courses", {
      params: { search: search.value, category: category.value },
    });
  } finally {
    loading.value = false;
  }
}
onMounted(async () => {
  await load();
  try {
    categories.value = await api.get("/courses/categories");
  } catch {
    categories.value = [...new Set(courses.value.map((c) => c.category))];
  }
});
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">EXPLORE & DISCOVER</div>
      <h1>找到你的下一步。</h1>
      <p>从基础到进阶，让好奇心带你走得更远。</p>
    </div>
  </div>
  <div class="toolbar">
    <el-input
      v-model="search"
      placeholder="搜索课程…"
      clearable
      @keyup.enter="load"
      style="max-width: 320px"
    /><el-select
      v-model="category"
      clearable
      placeholder="全部分类"
      @change="load"
      style="width: 160px"
      ><el-option
        v-for="c in categories"
        :key="c"
        :value="c"
        :label="c" /></el-select
    ><el-button @click="load">搜索</el-button
    ><el-checkbox v-model="fav" @change="load">我的收藏</el-checkbox>
  </div>
  <div v-loading="loading" class="course-grid">
    <router-link
      v-for="(c, i) in courses"
      :key="c.id"
      :to="'/courses/' + c.id"
      class="card course-card"
      ><div class="course-cover" :class="'tone-' + (i % 3)">
        <span>{{ c.category }}</span
        ><strong>{{ ["{ }", "⌘", "✧"][i % 3] }}</strong
        ><small>TIANJI ACADEMY / {{ String(i + 1).padStart(2, "0") }}</small>
      </div>
      <div class="course-body">
        <div class="eyebrow">
          {{ c.difficulty }} · {{ c.totalLessons }} 节课
        </div>
        <h3>{{ c.title }}</h3>
        <p>{{ c.description }}</p>
        <div class="course-bottom">
          <span>系统学习 · 实践驱动</span><b>开始探索 ↗</b>
        </div>
      </div></router-link
    >
  </div>
  <el-empty
    v-if="!courses.length && !loading"
    description="暂时没有课程，教师发布后将在这里展示"
  />
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth } from "./stores/auth";
const route = useRoute(),
  router = useRouter(),
  auth = useAuth();
const nav = [
  ["/", "◈", "学习概览"],
  ["/courses", "▦", "课程广场"],
  ["/practice", "✎", "练习题库"],
  ["/exams", "▤", "课程考试"],
  ["/wrong", "↺", "错题复习"],
  ["/tutor", "✧", "AI 私人导师"],
  ["/plans", "☷", "学习计划"],
  ["/report", "◷", "学习报告"],
];
const title = computed(
  () =>
    nav.find((x) => x[0] === route.path)?.[2] ||
    {
      Course: "课程详情",
      Learn: "专注学习",
      Teacher: "教师工作台",
      Admin: "平台管理",
    }[route.name] ||
    "天机智学",
);
function logout() {
  auth.logout();
  router.push("/login");
}
</script>
<template>
  <router-view v-if="route.name === 'Login'" />
  <div v-else class="shell">
    <aside class="sidebar">
      <router-link to="/" class="brand"
        ><span class="brand-mark">天</span
        ><span>天机智学<small>TIANJI AI ACADEMY</small></span></router-link
      >
      <div class="nav-caption">我的学习空间</div>
      <nav>
        <router-link
          v-for="[path, icon, label] in nav"
          :key="path"
          :to="path"
          :aria-label="label"
          :title="label"
          :class="{ active: route.path === path }"
          ><span>{{ icon }}</span
          >{{ label }}<b v-if="path === '/tutor'">AI</b></router-link
        ><router-link
          v-if="['TEACHER', 'ADMIN'].includes(auth.user?.role)"
          to="/teacher"
          aria-label="教师工作台"
          ><span>⌘</span>教师工作台</router-link
        ><router-link v-if="auth.user?.role === 'ADMIN'" to="/admin"
          aria-label="平台管理"
          ><span>⚙</span>平台管理</router-link
        >
      </nav>
      <div class="sidebar-note">
        <span>✦</span><strong>让每一次学习<br />都有新的理解。</strong>
        <p>课程 × 实践 × 智能引导</p>
      </div>
      <div class="profile">
        <div class="avatar">{{ auth.user?.displayName?.[0] || "学" }}</div>
        <span
          >{{ auth.user?.displayName
          }}<small>{{ auth.user?.role }}</small></span
        ><button class="icon-btn" @click="logout" aria-label="退出登录">
          ↗
        </button>
      </div>
    </aside>
    <div class="main">
      <header>
        <span>{{ title }}</span>
        <div class="header-right">
          <span class="quiet">持续学习，日有所进</span
          ><span class="dot"></span>个人学习空间
        </div>
      </header>
      <main><router-view :key="route.path" /></main>
      <footer>TIANJI AI ACADEMY <span>知识有径，学习有伴。</span></footer>
    </div>
  </div>
</template>

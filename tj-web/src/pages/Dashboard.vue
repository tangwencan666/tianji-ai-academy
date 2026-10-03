<script setup>
import { ref, onMounted, computed } from "vue";
import { api } from "../api";
import { useAuth } from "../stores/auth";
const auth = useAuth(),
  data = ref(null),
  courses = ref([]);
onMounted(async () => {
  [data.value, courses.value] = await Promise.all([
    api.get("/learning/dashboard"),
    api.get("/courses"),
  ]);
});
const weak = computed(() =>
  [...(data.value?.mastery || [])]
    .sort((a, b) => a.correctRate - b.correctRate)
    .slice(0, 4),
);
const myCourses = computed(() => courses.value.filter(c => data.value?.records.some(r => r.courseId === c.id)));
const courseProgress = c => Math.round((data.value?.records.filter(r => r.courseId === c.id).reduce((sum,r) => sum+r.progress,0) || 0) / Math.max(1,c.totalLessons));
const completed = computed(
  () => data.value?.records.filter((x) => x.status === "COMPLETED").length || 0,
);
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">MY LEARNING OVERVIEW</div>
      <h1>{{ auth.user?.displayName }}，今天想探索什么？</h1>
      <p>每一个小小的进步，都在让知识成为你的能力。</p>
    </div>
    <span class="date-pill">{{
      new Intl.DateTimeFormat("zh-CN", {
        month: "long",
        day: "numeric",
        weekday: "long",
        timeZone: "Asia/Shanghai",
      }).format(new Date())
    }}</span>
  </div>
  <div class="hero">
    <div>
      <span class="hero-label">你的私人学习导师</span>
      <h2>不止学会答案，<br />更要理解「为什么」。</h2>
      <p>结合课程知识与学习记录，为你找到下一步。</p>
      <router-link to="/tutor" class="button cream"
        >和 AI 导师聊聊 <span>↗</span></router-link
      >
    </div>
    <div class="hero-art" aria-hidden="true">
      <div class="ring r1"></div>
      <div class="ring r2"></div>
      <div class="floating one">{ 理解 }</div>
      <div class="floating two">✧</div>
      <div class="floating three">学习 → 成长</div>
      <span class="spark">✳</span>
    </div>
  </div>
  <div v-if="data" class="stats">
    <div class="stat">
      <span>今日学习</span
      ><strong
        >{{ Math.floor(data.time.todaySeconds / 60)
        }}<small> 分钟</small></strong
      >
      <p>专注的时间，自有回响</p>
    </div>
    <div class="stat">
      <span>已完成课时</span><strong>{{ completed }}<small> 节</small></strong>
      <p>把每一次理解积累起来</p>
    </div>
    <div class="stat">
      <span>本周学习</span
      ><strong
        >{{ Math.floor(data.time.weekSeconds / 60)
        }}<small> 分钟</small></strong
      >
      <p>最近 7 天真实学习记录</p>
    </div>
    <div class="stat">
      <span>待复习错题</span
      ><strong
        >{{
          data.wrongQuestions.filter((w) => w.masteryStatus !== "MASTERED")
            .length
        }}<small> 道</small></strong
      >
      <p>错题是进步的起点</p>
    </div>
  </div>
  <div class="dashboard-grid">
    <section>
      <div class="section-title">
        <h2>继续学习</h2>
        <router-link to="/courses">全部课程 ↗</router-link>
      </div>
      <div v-if="!data?.records.length" class="card empty-inline">
        还没有学习记录。从一门感兴趣的课程开始吧。
      </div>
      <router-link
        v-for="r in data?.records.slice(0, 3)"
        :key="r.id"
        :to="'/learn/' + r.lessonId"
        class="card continue-card"
        ><div class="course-icon">{ }</div>
        <div>
          <h3>
            {{ courses.find((c) => c.id === r.courseId)?.title || "课程" }} ·
            课时 {{ r.lessonId }}
          </h3>
          <p>累计学习 {{ Math.floor(r.duration / 60) }} 分钟</p>
          <el-progress :percentage="r.progress" :stroke-width="5" />
        </div>
        <span>→</span></router-link
      >
      <div class="section-title"><h2>我的课程</h2></div>
      <div v-if="!myCourses.length" class="card">开始一节课后，这里显示你的课程进度。</div>
      <router-link v-for="c in myCourses" :key="c.id" :to="'/courses/'+c.id" class="card spaced" style="display:block">
        <h3>{{ c.title }}</h3><el-progress :percentage="courseProgress(c)" />
      </router-link>
      <div class="section-title">
        <h2>探索新知识</h2>
        <span class="quiet">BUILD YOUR NEXT SKILL</span>
      </div>
      <div class="mini-grid">
        <router-link
          v-for="(c, i) in courses.slice(0, 2)"
          :key="c.id"
          :to="'/courses/' + c.id"
          class="card mini-course"
          ><span class="tag">{{ c.category }}</span>
          <div class="mini-art">{{ i % 2 ? "⌘" : "{ }" }}</div>
          <h3>{{ c.title }}</h3>
          <p>{{ c.totalLessons }} 节课 · {{ c.difficulty }}</p></router-link
        >
      </div>
    </section>
    <section>
      <div class="section-title">
        <h2>知识掌握度</h2>
        <span>◎</span>
      </div>
      <div class="card">
        <p class="quiet">从练习中，发现值得多看一眼的知识点。</p>
        <el-empty
          v-if="!weak.length"
          description="完成练习后生成掌握度"
          :image-size="70"
        />
        <div v-for="k in weak" :key="k.knowledgePointId" class="mastery">
          <div>
            <span>{{ k.name }}</span
            ><b>{{ k.correctRate }}%</b>
          </div>
          <el-progress
            :percentage="k.correctRate"
            :show-text="false"
            :stroke-width="6"
          />
        </div>
        <router-link to="/wrong" class="text-link"
          >去复习薄弱知识点 →</router-link
        >
      </div>
      <div class="section-title"><h2>最近考试</h2></div>
      <div class="card">
        <p v-if="!data?.exams.length" class="quiet">尚无考试记录。</p>
        <router-link v-for="e in data?.exams.slice(0,3)" :key="e.id" to="/exams" class="lesson-row">
          <span>考试 #{{e.examId}} · {{e.status}}</span><b>{{e.score}} / {{e.maxScore}}</b>
        </router-link>
      </div>
      <div class="note-card">
        <span>✧ 学习建议</span>
        <h3>理解，比记住更重要。</h3>
        <p v-if="weak.length">先复习「{{weak[0].name}}」，当前正确率 {{weak[0].correctRate}}%。</p><p v-else>完成一组练习，建立自己的知识掌握记录。</p><p class="quiet">以上为学习记录规则建议。需要 AI 深入分析时，进入导师或周报。</p>
        <router-link to="/plans">制定我的学习计划 →</router-link>
      </div>
    </section>
  </div>
</template>

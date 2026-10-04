<script setup>
import { ref, onUnmounted, watch, nextTick } from "vue";
import { useRoute } from "vue-router";
import { streamChat } from "../api";
import CourseSelect from "../components/CourseSelect.vue";
const props = defineProps({ courseId: Number, embedded: Boolean });
const course = ref(props.courseId || Number(useRoute().query.courseId) || null),
  conversation = ref(crypto.randomUUID()),
  input = ref(""),
  messages = ref([]),
  busy = ref(false),
  status = ref(""),
  list = ref(null);
let abort;
watch(course, () => reset());
function reset() {
  abort?.abort();
  conversation.value = crypto.randomUUID();
  messages.value = [];
}
async function send(text = input.value) {
  if (!text.trim() || busy.value || !course.value) return;
  input.value = "";
  messages.value.push({ role: "user", text });
  const m = {
    role: "assistant",
    text: "",
    sources: [],
    suggestions: [],
    error: "",
  };
  messages.value.push(m);
  const index = messages.value.length - 1;
  busy.value = true;
  abort = new AbortController();
  const timeout = setTimeout(() => abort.abort(), 90000);
  try {
    await streamChat(
      {
        courseId: course.value,
        conversationId: conversation.value,
        message: text,
      },
      async (name, value) => {
        const item = messages.value[index];
        if (!item) return;
        if (name === "delta") item.text += value;
        if (name === "status") status.value = value;
        if (name === "sources") item.sources = value;
        if (name === "suggestions") item.suggestions = value;
        if (name === "error") { item.error = value; item.text = ""; }
        if (name === "answer") item.text = value.answer;
        await nextTick();
        list.value?.scrollTo({
          top: list.value.scrollHeight,
          behavior: "smooth",
        });
      },
      abort.signal,
    );
  } catch (e) {
    if (messages.value[index])
      messages.value[index].error =
        e.name === "AbortError"
          ? "已停止生成（或请求超时），可重新发送。"
          : e.message;
  } finally {
    clearTimeout(timeout);
    busy.value = false;
    status.value = "";
  }
}
onUnmounted(() => abort?.abort());
</script>
<template>
  <div class="page-heading" v-if="!embedded">
    <div>
      <div class="eyebrow">YOUR PERSONAL AI TUTOR</div>
      <h1>每一个「为什么」，都值得解答。</h1>
    </div>
    <div class="actions">
      <CourseSelect v-model="course" :disabled="busy" /><el-button
        @click="reset"
        :disabled="busy"
        >新对话</el-button
      >
    </div>
  </div>
  <div :class="embedded ? 'embedded-tutor' : 'chat-layout'">
    <section class="chat">
      <div class="chat-top">
        <div class="ai-avatar">✧</div>
        <div>
          <strong>天机 · 私人学习导师</strong
          ><small>结合课程资料，陪你循序渐进地理解</small>
        </div>
        <span class="tag">课程问答</span>
      </div>
      <div class="messages" ref="list">
        <div v-if="!messages.length" class="chat-welcome">
          <span>✧</span>
          <h2>你好，今天我们一起学什么？</h2>
          <p>
            选择课程，然后告诉我你的疑惑。<br />我会从课程资料出发，帮你找到思路。
          </p>
          <button
            v-for="q in [
              '这个课程应该怎么开始学？',
              '分析一下我的薄弱知识点',
              '如何理解 HashMap 的线程安全问题？',
            ]"
            :key="q"
            @click="send(q)"
          >
            {{ q }} ↗
          </button>
        </div>
        <div
          v-for="(m, i) in messages"
          :key="i"
          class="message"
          :class="m.role"
        >
          <div class="message-label">
            {{ m.role === "user" ? "你" : "✧ 天机导师" }}
          </div>
          <div class="bubble">
            {{ m.text || (!m.error ? "正在思考你的问题…" : "") }}
          </div>
          <p v-if="m.error" class="error-text">{{ m.error }}</p>
            <div class="sources" v-if="m.sources?.length">
            <small>检索到的课程资料 · 供核对</small>
            <details v-for="s in m.sources" :key="s.id">
              <summary>[{{ s.id }}] {{ s.title }} · 课程 {{s.courseId}} / 章节 {{s.chapterId || '课程总览'}} · {{ s.source }}</summary>
              <p>{{ s.content }}</p>
            </details>
          </div>
          <div class="suggestions">
            <button
              v-for="q in m.suggestions"
              :key="q"
              @click="send(q)"
              :disabled="busy"
            >
              {{ q }}
            </button>
          </div>
        </div>
      </div>
      <div class="composer">
        <div v-if="busy" class="status">
          <i></i>{{ status || "正在连接导师…"
          }}<button @click="abort?.abort()">停止</button>
        </div>
        <form @submit.prevent="send()">
          <textarea
            v-model="input"
            placeholder="描述你的问题，例如：HashMap 为什么线程不安全？"
            maxlength="4000"
            :disabled="busy"
            @keydown.ctrl.enter.prevent="send()"
          ></textarea
          ><button
            type="submit"
            :disabled="busy || !course || !input.trim()"
            aria-label="发送消息"
          >
            ↑
          </button>
        </form>
        <small>AI 可能有误，请结合课程资料核对 · Ctrl + Enter 发送</small>
      </div>
    </section>
    <aside v-if="!embedded">
      <div class="card">
        <span class="eyebrow">HOW I CAN HELP</span>
        <h3>不只给你一个答案。</h3>
        <div class="tutor-feature">
          <b>01</b>
          <div>
            <strong>解释课程难点</strong>
            <p>从概念到例子，逐步理解。</p>
          </div>
        </div>
        <div class="tutor-feature">
          <b>02</b>
          <div>
            <strong>发现知识盲区</strong>
            <p>参考你的练习和学习进度。</p>
          </div>
        </div>
        <div class="tutor-feature">
          <b>03</b>
          <div>
            <strong>规划下一步</strong>
            <p>把目标拆成每天可做的任务。</p>
          </div>
        </div>
      </div>
      <div class="note-card">
        <h3>带着问题学习</h3>
        <p>
          说出你已经理解的部分，也说出让你困惑的地方。清晰的问题，会带来更有帮助的回答。
        </p>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.embedded-tutor .messages { height: 380px; min-height: 220px; }
.embedded-tutor .chat-welcome { padding: 20px 10px; }
.embedded-tutor .chat-welcome h2 { font-size: 19px; }
.embedded-tutor .chat-top { padding: 14px; }
.embedded-tutor .chat-top .tag { display:none; }
</style>

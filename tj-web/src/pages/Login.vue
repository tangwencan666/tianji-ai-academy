<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { useAuth } from "../stores/auth";
const register = ref(false),
  busy = ref(false),
  form = reactive({ username: "", password: "" }),
  auth = useAuth(),
  router = useRouter();
async function submit() {
  busy.value = true;
  try {
    await auth.login(form, register.value);
    router.push("/");
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="login">
    <section class="login-story">
      <div class="brand">
        <span class="brand-mark">天</span
        ><span>天机智学<small>TIANJI AI ACADEMY</small></span>
      </div>
      <span class="eyebrow light">LEARN WITH INTELLIGENCE</span>
      <h1>把知识学懂。<br />把未来走宽。</h1>
      <p>
        从第一行 Java 代码，到属于你的技术体系。<br />让 AI
        导师陪你理解、实践，再进一步。
      </p>
      <div class="orbit">
        <span>Java</span><span>Spring</span><span>AI</span>
        <div>学 · 思 · 践</div>
      </div>
      <small>专注技术成长的智能学习平台</small>
    </section>
    <section class="login-form">
      <div>
        <span class="eyebrow">YOUR LEARNING JOURNEY</span>
        <h2>{{ register ? "开启你的学习旅程" : "欢迎回来" }}</h2>
        <p class="quiet">
          {{
            register
              ? "创建账号，从今天开始积累。"
              : "今天，也向更好的自己迈进一步。"
          }}
        </p>
        <el-form @submit.prevent="submit" label-position="top"
          ><el-form-item label="用户名"
            ><el-input
              v-model="form.username"
              autocomplete="username"
              placeholder="3–32位字母、数字或下划线"
              maxlength="32" /></el-form-item
          ><el-form-item label="密码"
            ><el-input
              v-model="form.password"
              type="password"
              show-password
              autocomplete="current-password"
              placeholder="8–72位密码" /></el-form-item
          ><el-button
            type="primary"
            native-type="submit"
            :loading="busy"
            class="full"
            >{{ register ? "创建账号" : "登录学习空间" }} →</el-button
          ></el-form
        ><button class="link" @click="register = !register">
          {{ register ? "已有账号？登录" : "还没有账号？免费注册" }}
        </button>
        <p class="fine-print">注册账号默认为学生。教师权限由管理员分配。</p>
      </div>
    </section>
  </div>
</template>

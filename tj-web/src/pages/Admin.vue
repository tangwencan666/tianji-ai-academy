<script setup>
import { ref, onMounted } from "vue";
import { api } from "../api";
import { ElMessageBox } from "element-plus";
import { useAuth } from "../stores/auth";
const users = ref([]),
  auth = useAuth();
async function load() {
  users.value = await api.get("/admin/users");
}
onMounted(load);
async function role(user, value) {
  await ElMessageBox.confirm(
    `将 ${user.username} 的角色设为 ${value}？`,
    "更改角色",
  );
  await api.patch(`/admin/users/${user.id}/role`, null, {
    params: { role: value },
  });
  await load();
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">PLATFORM ADMINISTRATION</div>
      <h1>平台管理</h1>
      <p>管理用户身份。角色修改后会在下一次请求生效。</p>
    </div>
  </div>
  <div class="card">
    <el-table :data="users"
      ><el-table-column prop="username" label="用户名" /><el-table-column
        prop="displayName"
        label="昵称" /><el-table-column
        prop="createTime"
        label="注册时间" /><el-table-column label="角色"
        ><template #default="{ row }"
          ><el-select
            :model-value="row.role"
            :disabled="row.id === auth.user.id"
            @update:model-value="role(row, $event)"
            ><el-option
              v-for="r in ['STUDENT', 'TEACHER', 'ADMIN']"
              :key="r"
              :value="r" /></el-select></template></el-table-column
    ></el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { api } from "../api";
const props = defineProps({ modelValue: [Number, String], mine: Boolean });
const emit = defineEmits(["update:modelValue"]);
const courses = ref([]);
onMounted(async () => {
  courses.value = await api.get("/courses", { params: { mine: props.mine } });
  if (!props.modelValue && courses.value.length)
    emit("update:modelValue", courses.value[0].id);
});
</script>
<template>
  <el-select
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    placeholder="选择一门课程"
    style="width: 260px"
    ><el-option v-for="c in courses" :key="c.id" :label="c.title" :value="c.id"
  /></el-select>
</template>

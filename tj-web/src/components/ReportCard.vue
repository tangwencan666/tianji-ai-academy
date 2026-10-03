<script setup>
defineProps({ report: Object });
</script>
<template>
  <div class="report" v-if="report">
    <h3>{{ report.summary }}</h3>
    <section
      v-for="[key, title] in [
        ['achievements', '学习收获'],
        ['weaknesses', '需要关注'],
        ['recommendations', '行动建议'],
        ['nextWeekGoals', '下一阶段目标'],
      ]"
      :key="key"
    >
      <h4>{{ title }}</h4>
      <ul>
        <li v-for="item in report[key]" :key="item">{{ item }}</li>
      </ul>
    </section>
  </div>
</template>

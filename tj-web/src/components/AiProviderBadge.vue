<script setup>
import {ref,onMounted,onUnmounted} from 'vue';
import {api} from '../api';
const provider=ref(null);
let timer;
async function refresh(){try{provider.value=await api.get('/ai/provider')}catch{provider.value=null}}
onMounted(()=>{refresh();timer=setInterval(refresh,15000)});
onUnmounted(()=>clearInterval(timer));
</script>
<template>
  <span class="tag" data-testid="ai-provider">{{provider?.label || 'AI状态待确认'}}<template v-if="provider?.enabled"> · {{provider.model}}</template></span>
</template>

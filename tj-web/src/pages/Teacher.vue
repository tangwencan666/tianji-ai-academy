<script setup>
import { ref, reactive, onMounted, watch } from "vue";
import { api } from "../api";
import { ElMessage } from "element-plus";
const courses = ref([]),
  id = ref(null),
  detail = ref(null),
  tab = ref("course"),
  busy = ref(false),
  questions = ref([]),
  reviews = ref([]),
  sources = ref([]),
  aiResult = ref(null);
const course = reactive({
    title: "",
    description: "",
    cover: "",
    category: "Java",
    difficulty: "入门",
  }),
  chapter = reactive({ title: "", sortOrder: 0 }),
  lesson = reactive({
    chapterId: null,
    title: "",
    videoUrl: "",
    content: "",
    attachment: "",
    estimatedMinutes: 20,
    sortOrder: 0,
  }),
  point = ref("");
const question = reactive({
    type: "SINGLE",
    stem: "",
    options: ["", "", "", ""],
    answer: "A",
    explanation: "",
    difficulty: "中等",
    knowledgePointIds: [],
  }),
  exam = reactive({ title: "", durationMinutes: 30, questionIds: [] }),
  material = reactive({
    title: "",
    source: "",
    content: "",
    chapterId: null,
    lessonId: null,
  }),
  topic = ref("Java 集合框架"),
  count = ref(5);
async function load() {
  courses.value = await api.get("/courses", { params: { mine: true } });
  if (!id.value && courses.value.length) id.value = courses.value[0].id;
}
async function refresh() {
  if (!id.value) {
    detail.value = null;
    return;
  }
  detail.value = await api.get(`/courses/${id.value}`);
  Object.assign(course, detail.value.course);
  [questions.value, reviews.value, sources.value] = await Promise.all([
    api.get("/questions", { params: { courseId: id.value, teacher: true } }),
    api.get("/teacher/reviews", { params: { courseId: id.value } }),
    api.get("/knowledge/sources", { params: { courseId: id.value } }),
  ]);
}
watch(id, () => {
  question.knowledgePointIds = [];
  exam.questionIds = [];
  lesson.chapterId = null;
  material.chapterId = null;
  material.lessonId = null;
  aiResult.value = null;
  refresh();
});
onMounted(load);
async function act(fn) {
  busy.value = true;
  try {
    await fn();
    ElMessage.success("操作成功");
    await load();
    await refresh();
  } finally {
    busy.value = false;
  }
}
function newCourse() {
  id.value = null;
  detail.value = null;
  Object.assign(course, {
    title: "",
    description: "",
    cover: "",
    category: "Java",
    difficulty: "入门",
  });
  tab.value = "course";
}
async function saveCourse() {
  await act(async () => {
    const r = await (id.value
      ? api.put(`/courses/${id.value}`, course)
      : api.post("/courses", course));
    id.value = r.id;
  });
}
async function status(value) {
  await act(() =>
    api.patch(`/courses/${id.value}/status`, null, {
      params: { status: value },
    }),
  );
}
async function importFile(e) {
  const file = e.target.files?.[0];
  if (file) {
    if (file.size > 180000) {
      ElMessage.error("文件过大，请控制在6万字符以内");
      return;
    }
    material.content = await file.text();
    material.source = file.name;
    material.title = file.name.replace(/\.[^.]+$/, "");
  }
}
async function ai(kind) {
  busy.value = true;
  try {
    aiResult.value = await api.post(`/ai/${kind}`, {
      courseId: id.value,
      topic: topic.value,
      count: count.value,
    });
    if (kind === "question/generate") await refresh();
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="page-heading">
    <div>
      <div class="eyebrow">TEACHER STUDIO</div>
      <h1>把你的知识，变成他人的成长。</h1>
      <p>管理课程内容、审核题目，与 AI 一起准备更好的课堂。</p>
    </div>
    <el-button @click="newCourse">＋ 创建课程</el-button>
  </div>
  <div class="toolbar">
    <el-select v-model="id" placeholder="选择管理的课程" style="width: 320px"
      ><el-option
        v-for="c in courses"
        :key="c.id"
        :value="c.id"
        :label="c.title" /></el-select
    ><span v-if="detail" class="tag">{{ detail.course.status }}</span>
  </div>
  <el-tabs v-model="tab" class="card"
    ><el-tab-pane label="课程信息" name="course"
      ><el-form label-position="top" class="editor-form"
        ><el-form-item label="课程名称"
          ><el-input v-model="course.title" maxlength="150" /></el-form-item
        ><el-form-item label="课程简介"
          ><el-input v-model="course.description" type="textarea" :rows="4"
        /></el-form-item>
        <div class="two-fields">
          <el-form-item label="分类"
            ><el-input v-model="course.category" /></el-form-item
          ><el-form-item label="难度"
            ><el-select v-model="course.difficulty"
              ><el-option
                v-for="x in ['入门', '中等', '进阶']"
                :key="x"
                :value="x" /></el-select
          ></el-form-item>
        </div>
        <div class="actions">
          <el-button type="primary" @click="saveCourse" :loading="busy"
            >保存课程</el-button
          ><el-button v-if="id" @click="status('PUBLISHED')" :loading="busy"
            >发布</el-button
          ><el-button v-if="id" @click="status('OFFLINE')" :loading="busy"
            >下架</el-button
          >
        </div></el-form
      ></el-tab-pane
    ><el-tab-pane v-if="id" label="章节与课时" name="lessons"
      ><div class="two-fields">
        <div>
          <h3>添加章节</h3>
          <el-form label-position="top"
            ><el-form-item label="章节标题"
              ><el-input v-model="chapter.title" /></el-form-item
            ><el-form-item label="排序"
              ><el-input-number
                v-model="chapter.sortOrder"
                :min="0" /></el-form-item
            ><el-button
              @click="act(() => api.post(`/courses/${id}/chapters`, chapter))"
              :loading="busy"
              >添加章节</el-button
            ></el-form
          >
          <div class="spaced">
            <h3>知识点</h3>
            <span
              v-for="k in detail?.knowledgePoints"
              :key="k.id"
              class="tag"
              >{{ k.name }}</span
            ><el-input
              v-model="point"
              placeholder="例如：集合框架"
              class="spaced"
            /><el-button
              class="spaced"
              @click="
                act(() =>
                  api.post(`/courses/${id}/knowledge-points`, null, {
                    params: { name: point },
                  }),
                )
              "
              >添加知识点</el-button
            >
          </div>
        </div>
        <el-form label-position="top"
          ><h3>添加课时</h3>
          <el-form-item label="所属章节"
            ><el-select v-model="lesson.chapterId"
              ><el-option
                v-for="h in detail?.chapters"
                :key="h.id"
                :label="h.title"
                :value="h.id" /></el-select></el-form-item
          ><el-form-item label="课时标题"
            ><el-input v-model="lesson.title" /></el-form-item
          ><el-form-item label="讲义正文"
            ><el-input
              v-model="lesson.content"
              type="textarea"
              :rows="8" /></el-form-item
          ><el-form-item label="视频地址（选填）"
            ><el-input
              v-model="lesson.videoUrl"
              placeholder="https://…" /></el-form-item
          ><el-form-item label="附件地址（选填）"
            ><el-input
              v-model="lesson.attachment"
              placeholder="https://…" /></el-form-item
          ><el-form-item label="预计学习分钟"
            ><el-input-number
              v-model="lesson.estimatedMinutes"
              :min="1"
              :max="300" /></el-form-item
          ><el-button
            type="primary"
            @click="act(() => api.post(`/courses/${id}/lessons`, lesson))"
            :loading="busy"
            >添加课时</el-button
          ></el-form
        >
      </div>
      <h3>已有课时</h3>
      <el-table :data="detail?.lessons"
        ><el-table-column prop="title" label="课时" /><el-table-column
          prop="estimatedMinutes"
          label="预计分钟" /></el-table></el-tab-pane
    ><el-tab-pane v-if="id" label="题库与审核" name="questions"
      ><el-form label-position="top"
        ><div class="two-fields">
          <el-form-item label="题型"
            ><el-select v-model="question.type"
              ><el-option
                v-for="[v, l] in [
                  ['SINGLE', '单选'],
                  ['MULTIPLE', '多选'],
                  ['BOOLEAN', '判断'],
                  ['SHORT', '简答'],
                ]"
                :key="v"
                :value="v"
                :label="l" /></el-select></el-form-item
          ><el-form-item label="关联知识点"
            ><el-select v-model="question.knowledgePointIds" multiple
              ><el-option
                v-for="k in detail?.knowledgePoints"
                :key="k.id"
                :label="k.name"
                :value="k.id" /></el-select
          ></el-form-item>
        </div>
        <el-form-item label="题干"
          ><el-input v-model="question.stem" type="textarea"
        /></el-form-item>
        <div v-if="question.type !== 'SHORT'" class="two-fields">
          <el-form-item
            v-for="(_, i) in question.options"
            :key="i"
            :label="'选项 ' + String.fromCharCode(65 + i)"
            ><el-input v-model="question.options[i]"
          /></el-form-item>
        </div>
        <el-form-item label="正确答案（客观题填字母，多选以英文逗号分隔）"
          ><el-input v-model="question.answer" /></el-form-item
        ><el-form-item label="解析"
          ><el-input
            v-model="question.explanation"
            type="textarea" /></el-form-item
        ><el-button
          type="primary"
          :loading="busy"
          @click="
            act(() =>
              api.post('/questions', {
                ...question,
                courseId: id,
                options:
                  question.type === 'SHORT'
                    ? []
                    : question.options.filter((o) => o.trim()),
              }),
            )
          "
          >保存正式题目</el-button
        ></el-form
      >
      <h3>课程题库</h3>
      <article v-for="q in questions" :key="q.id" class="review-item">
        <div class="eyebrow">{{ q.type }} · {{ q.status }}</div>
        <h4>{{ q.stem }}</h4>
        <p>{{ q.options }}</p>
        <p>答案：{{ q.answer }} · {{ q.explanation }}</p>
        <el-button
          v-if="q.status === 'AI_GENERATED'"
          type="primary"
          @click="act(() => api.post(`/questions/${q.id}/approve`))"
          >审核通过，加入正式题库</el-button
        >
      </article></el-tab-pane
    ><el-tab-pane v-if="id" label="考试与批改" name="exams"
      ><el-form label-position="top"
        ><el-form-item label="考试名称"
          ><el-input v-model="exam.title" /></el-form-item
        ><el-form-item label="考试时长（分钟）"
          ><el-input-number
            v-model="exam.durationMinutes"
            :min="1"
            :max="180" /></el-form-item
        ><el-form-item label="选择已审核题目（每题10分）"
          ><el-select v-model="exam.questionIds" multiple style="width: 100%"
            ><el-option
              v-for="q in questions.filter((q) => q.status === 'APPROVED')"
              :key="q.id"
              :value="q.id"
              :label="q.stem" /></el-select></el-form-item
        ><el-button
          type="primary"
          @click="act(() => api.post('/exams', { ...exam, courseId: id }))"
          :loading="busy"
          >创建并发布考试</el-button
        ></el-form
      >
      <h3>待复核简答题</h3>
      <article v-for="r in reviews" :key="r.id" class="review-item">
        <h4>{{ questions.find((q) => q.id === r.questionId)?.stem }}</h4>
        <p>学生作答：{{ r.answer }}</p>
        <p v-if="r.aiFeedback" class="article-text">
          AI建议（需核实）：{{ r.aiFeedback }}
        </p>
        <el-input-number
          v-model="r.score"
          :min="0"
          :max="r.maxScore"
        /><el-button
          @click="
            act(() => api.post(`/teacher/reviews/${r.id}`, { score: r.score }))
          "
          >确认成绩</el-button
        >
      </article>
      <el-empty
        v-if="!reviews.length"
        description="暂无待复核答题" /></el-tab-pane
    ><el-tab-pane v-if="id" label="课程知识库" name="knowledge"
      ><p class="quiet">
        导入 Markdown 或文本讲义，供 AI
        在当前课程内检索。相同来源名称再次导入将替换旧内容。
      </p>
      <input type="file" accept=".md,.txt" @change="importFile" /><el-form
        label-position="top"
        class="spaced"
        ><el-form-item label="资料标题"
          ><el-input v-model="material.title" /></el-form-item
        ><el-form-item label="来源名称"
          ><el-input v-model="material.source"
        /></el-form-item>
        <div class="two-fields">
          <el-form-item label="关联章节（选填）"
            ><el-select v-model="material.chapterId" clearable
              ><el-option
                v-for="h in detail?.chapters"
                :key="h.id"
                :label="h.title"
                :value="h.id" /></el-select></el-form-item
          ><el-form-item label="关联课时（选填）"
            ><el-select v-model="material.lessonId" clearable
              ><el-option
                v-for="l in detail?.lessons"
                :key="l.id"
                :label="l.title"
                :value="l.id" /></el-select
          ></el-form-item>
        </div>
        <el-form-item label="资料正文"
          ><el-input
            v-model="material.content"
            type="textarea"
            :rows="10"
            maxlength="60000" /></el-form-item
        ><el-button
          type="primary"
          :loading="busy"
          @click="
            act(() =>
              api.post('/knowledge/import', {
                ...material,
                courseId: id,
                chapterId: material.chapterId || null,
                lessonId: material.lessonId || null,
              }),
            )
          "
          >导入知识库</el-button
        ></el-form
      >
      <p>已入库 {{ sources.length }} 个文本分块</p></el-tab-pane
    ><el-tab-pane v-if="id" label="AI 教师助手" name="ai"
      ><el-input
        v-model="topic"
        type="textarea"
        :rows="3"
        placeholder="描述备课需求"
      />
      <div class="toolbar spaced">
        <el-input-number v-model="count" :min="1" :max="10" /><el-button
          :loading="busy"
          @click="ai('question/generate')"
          >生成待审核题目</el-button
        ><el-button :loading="busy" @click="ai('teacher/outline')"
          >生成课程大纲</el-button
        ><el-button :loading="busy" @click="ai('teacher/assistant')"
          >简介、摘要与班级分析</el-button
        >
      </div>
      <div v-if="aiResult" class="feedback">
        <p class="quiet">
          AI内容为草稿。请核对后用于正式课程；题目需在题库页审核。
        </p>
        <template v-if="!Array.isArray(aiResult)"
          ><section v-for="(value, key) in aiResult" :key="key">
            <h3>
              {{
                {
                  title: "标题",
                  objectives: "学习目标",
                  sections: "章节大纲",
                  knowledgePoints: "知识点",
                  exercises: "练习建议",
                  introduction: "课程简介",
                  chapterSummaries: "章节摘要",
                  faq: "常见问题",
                  classAnalysis: "班级分析",
                }[key] || key
              }}
            </h3>
            <ul v-if="Array.isArray(value)">
              <li v-for="v in value" :key="v">{{ v }}</li>
            </ul>
            <p v-else>{{ value }}</p>
          </section></template
        >
        <p v-else>
          已生成 {{ aiResult.length }} 道待审核题目，请前往「题库与审核」。
        </p>
      </div></el-tab-pane
    ></el-tabs
  >
</template>

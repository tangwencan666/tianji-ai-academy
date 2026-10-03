import { createApp } from "vue";
import { createPinia } from "pinia";
import { createRouter, createWebHashHistory } from "vue-router";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "./style.css";
import App from "./App.vue";
import { useAuth } from "./stores/auth";
const pages = import.meta.glob("./pages/*.vue");
const routes = [
  ["/login", "Login"],
  ["/", "Dashboard"],
  ["/courses", "Courses"],
  ["/courses/:id", "Course"],
  ["/learn/:id", "Learn"],
  ["/practice", "Practice"],
  ["/exams", "Exams"],
  ["/wrong", "Wrong"],
  ["/tutor", "Tutor"],
  ["/plans", "Plans"],
  ["/report", "Report"],
  ["/teacher", "Teacher"],
  ["/admin", "Admin"],
].map(([path, name]) => ({
  path,
  name,
  component: pages[`./pages/${name}.vue`],
}));
const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});
const pinia = createPinia();
const app = createApp(App);
app.use(pinia);
router.beforeEach(async (to) => {
  const auth = useAuth();
  if (to.name === "Login") return;
  if (!sessionStorage.getItem("tianji-token")) return "/login";
  if (!auth.user) {
    try {
      await auth.load();
    } catch {
      return "/login";
    }
  }
  if (to.name === "Teacher" && !["TEACHER", "ADMIN"].includes(auth.user.role))
    return "/";
  if (to.name === "Admin" && auth.user.role !== "ADMIN") return "/";
});
app.use(router).use(ElementPlus).mount("#app");

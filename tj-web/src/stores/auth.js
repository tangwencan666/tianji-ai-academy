import { defineStore } from "pinia";
import { ref } from "vue";
import { api } from "../api";
export const useAuth = defineStore("auth", () => {
  const user = ref(null);
  async function login(form, register = false) {
    const r = await api.post(`/auth/${register ? "register" : "login"}`, form);
    sessionStorage.setItem("tianji-token", r.token);
    user.value = r.user;
  }
  async function load() {
    user.value = await api.get("/auth/me");
  }
  function logout() {
    sessionStorage.removeItem("tianji-token");
    user.value = null;
  }
  return { user, login, load, logout };
});

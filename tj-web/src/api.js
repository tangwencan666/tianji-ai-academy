import axios from "axios";
import { ElMessage } from "element-plus";
import {consumeSse} from './sse';
export const api = axios.create({ baseURL: "/api", timeout: 90000 });
api.interceptors.request.use((c) => {
  const t = sessionStorage.getItem("tianji-token");
  if (t) c.headers.Authorization = `Bearer ${t}`;
  return c;
});
api.interceptors.response.use(
  (r) => r.data.data,
  (e) => {
    const message =
      e.response?.data?.message || "无法连接服务，请检查后端是否启动";
    ElMessage.error(message);
    if (e.response?.status === 401) {
      sessionStorage.removeItem("tianji-token");
      if (!location.hash.includes("login")) location.hash = "/login";
    }
    return Promise.reject(e);
  },
);
export async function streamChat(data, onEvent, signal) {
  return streamAi('/ai/tutor/chat',data,onEvent,signal);
}
export async function streamAi(path,data,onEvent,signal) {
  const r = await fetch('/api'+path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${sessionStorage.getItem("tianji-token")}`,
    },
    body: JSON.stringify(data),
    signal,
  });
  if (!r.ok) {
    let error;
    try {
      error = await r.json();
    } catch {}
    throw new Error(error?.message || "AI暂时不可用");
  }
  return consumeSse(r.body,onEvent);
}

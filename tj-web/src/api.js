import axios from "axios";
import { ElMessage } from "element-plus";
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
  const r = await fetch("/api/ai/tutor/chat", {
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
  const reader = r.body.getReader(),
    decoder = new TextDecoder();
  let buffer = "",
    doneEvent = false;
  try {
    while (true) {
      const { value, done } = await reader.read();
      buffer += decoder
        .decode(value || new Uint8Array(), { stream: !done })
        .replace(/\r\n/g, "\n");
      let index;
      while ((index = buffer.indexOf("\n\n")) >= 0) {
        const frame = buffer.slice(0, index);
        buffer = buffer.slice(index + 2);
        let name = "message";
        const lines = [];
        for (const line of frame.split("\n")) {
          if (line.startsWith("event:")) name = line.slice(6).trim();
          if (line.startsWith("data:")) lines.push(line.slice(5).trimStart());
        }
        if (lines.length) {
          const raw = lines.join("\n");
          let payload;
          try {
            payload = JSON.parse(raw);
          } catch {
            payload = raw;
          }
          onEvent(name, payload);
          if (name === "done" || name === "error") doneEvent = true;
        }
      }
      if (done) break;
    }
    if (!doneEvent) throw new Error("连接提前结束，请重试");
  } finally {
    await reader.cancel().catch(() => {});
    reader.releaseLock();
  }
}

package com.tang.tianji.ai.service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiProviderState {
  private final boolean enabled;
  private final String mode, model, embedding;
  private final AtomicBoolean verified=new AtomicBoolean();
  public AiProviderState(@Value("${app.ai-enabled:false}") boolean enabled,
      @Value("${app.ai-mode:disabled}") String mode,
      @Value("${spring.ai.openai.chat.options.model}") String model,
      @Value("${app.embedding-model}") String embedding) {
    this.enabled=enabled;this.mode=mode;this.model=model;this.embedding=embedding;
  }
  public void success() { if(enabled && "live".equals(mode)) verified.set(true); }
  public Map<String,Object> status() {
    return Map.of("enabled",enabled,"mode",enabled?mode:"disabled","provider","mock".equals(mode)?"Local Mock":"DeepSeek",
        "model","mock".equals(mode)?"offline-demo":model,"verified",verified.get(),"embedding",embedding,
        "label",!enabled?"AI未启用":"mock".equals(mode)?"Mock演示":verified.get()?"真实模型 · 已验证":"真实模型 · 待验证");
  }
}

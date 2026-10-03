package com.tang.tianji.ai.memory;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tang.tianji.common.*;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.*;
import java.time.*;
import java.util.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationMemory {
  private final AiConversationMapper conversations;
  private final AiMessageMapper messages;
  private final Clock clock;

  public ConversationMemory(AiConversationMapper c, AiMessageMapper m, Clock t) {
    conversations = c;
    messages = m;
    clock = t;
  }

  @Transactional
  public AiConversation open(String key, long course) {
    long uid = UserContext.getCurrentUserId();
    var c =
        conversations.selectOne(
            new QueryWrapper<AiConversation>().eq("user_id", uid).eq("conversation_key", key));
    if (c != null) {
      BusinessException.check(c.courseId == course, 400, "会话绑定了其他课程，请新建会话");
      return c;
    }
    c = new AiConversation();
    c.userId = uid;
    c.courseId = course;
    c.conversationKey = key;
    c.createTime = LocalDateTime.now(clock);
    conversations.insert(c);
    return c;
  }

  public List<Message> history(AiConversation c) {
    BusinessException.check(c.userId == UserContext.getCurrentUserId(), 403, "无权访问会话");
    var rows =
        messages.selectList(
            new QueryWrapper<AiMessage>()
                .eq("conversation_id", c.id)
                .orderByDesc("id")
                .last("LIMIT 20"));
    Collections.reverse(rows);
    return rows.stream()
        .map(
            m ->
                (Message)
                    ("user".equals(m.role)
                        ? new UserMessage(m.content)
                        : new AssistantMessage(m.content)))
        .toList();
  }

  @Transactional
  public void save(AiConversation c, String user, String assistant) {
    BusinessException.check(c.userId == UserContext.getCurrentUserId(), 403, "无权访问会话");
    for (String role : List.of("user", "assistant")) {
      var m = new AiMessage();
      m.conversationId = c.id;
      m.role = role;
      m.content = role.equals("user") ? user : assistant;
      m.createTime = LocalDateTime.now(clock);
      messages.insert(m);
    }
    var latest =
        messages.selectList(
            new QueryWrapper<AiMessage>()
                .eq("conversation_id", c.id)
                .orderByDesc("id")
                .last("LIMIT 20"));
    if (latest.size() == 20)
      messages.delete(
          new QueryWrapper<AiMessage>().eq("conversation_id", c.id).lt("id", latest.get(19).id));
  }

  public List<AiConversation> list() {
    return conversations.selectList(
        new QueryWrapper<AiConversation>()
            .eq("user_id", UserContext.getCurrentUserId())
            .orderByDesc("id")
            .last("LIMIT 50"));
  }
}

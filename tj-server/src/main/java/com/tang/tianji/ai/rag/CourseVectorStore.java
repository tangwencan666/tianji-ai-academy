package com.tang.tianji.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tang.tianji.common.*;
import com.tang.tianji.dto.Requests.Material;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.KnowledgeChunkMapper;
import com.tang.tianji.service.CourseService;
import java.util.*;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Small-course exact vector store. Persist vectors in MySQL; filter course BEFORE cosine search.
 */
@Service
public class CourseVectorStore {
  public record Source(
      long id,
      String title,
      String source,
      Long chapterId,
      Long lessonId,
      double score,
      String content) {}

  private final KnowledgeChunkMapper chunks;
  private final EmbeddingModel embedding;
  private final CourseService courses;
  private final ObjectMapper json;
  private final String model;

  public CourseVectorStore(
      KnowledgeChunkMapper m,
      EmbeddingModel e,
      CourseService c,
      ObjectMapper j,
      @Value("${app.embedding-model}") String n) {
    chunks = m;
    embedding = e;
    courses = c;
    json = j;
    model = n;
  }

  @Transactional
  public int ingest(Material d) {
    var course = courses.own(d.courseId());
    courses.validateChapter(d.courseId(), d.chapterId());
    if (d.lessonId() != null) {
      var l = courses.lesson(d.lessonId());
      BusinessException.check(
          l.courseId.equals(d.courseId())
              && (d.chapterId() == null || l.chapterId.equals(d.chapterId())),
          400,
          "课时范围不匹配");
    }
    // Replace this source atomically; failed embedding leaves the previous source intact.
    var old =
        new QueryWrapper<KnowledgeChunk>().eq("course_id", d.courseId()).eq("source", d.source());
    long existing =
        chunks.selectCount(new QueryWrapper<KnowledgeChunk>().eq("course_id", d.courseId()))
            - chunks.selectCount(old);
    int count = (d.content().length() + 699) / 700;
    BusinessException.check(existing + count <= 2000, 400, "单课程最多2000个分块，请拆分课程");
    var fresh = new ArrayList<KnowledgeChunk>();
    for (int start = 0; start < d.content().length(); start += 700) {
      String text = d.content().substring(start, Math.min(d.content().length(), start + 900));
      var k = new KnowledgeChunk();
      k.courseId = d.courseId();
      k.chapterId = d.chapterId();
      k.lessonId = d.lessonId();
      k.teacherId = course.teacherId;
      k.title = d.title();
      k.source = d.source();
      k.content = text;
      k.embeddingModel = model;
      try {
        k.embedding = json.writeValueAsString(embedding.embed(text));
      } catch (Exception e) {
        throw new BusinessException(503, "Embedding服务不可用，原资料保持不变");
      }
      if (Thread.currentThread().isInterrupted()) throw new BusinessException(504, "导入已取消");
      fresh.add(k);
    }
    chunks.delete(old);
    fresh.forEach(chunks::insert);
    return fresh.size();
  }

  public List<Source> search(long course, String query) {
    courses.access(course);
    var rows =
        chunks.selectList(
            new QueryWrapper<KnowledgeChunk>()
                .eq("course_id", course)
                .eq("embedding_model", model)
                .last("LIMIT 2000"));
    if (rows.isEmpty()) return List.of();
    float[] v = embedding.embed(query);
    var result = new ArrayList<Source>();
    for (var c : rows) {
      try {
        float[] x = json.readValue(c.embedding, float[].class);
        double score = cosine(v, x);
        if (score >= 0.25)
          result.add(
              new Source(c.id, c.title, c.source, c.chapterId, c.lessonId, score, c.content));
      } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        throw new BusinessException(503, "向量数据损坏，请重新导入");
      }
    }
    return result.stream()
        .sorted(Comparator.comparingDouble(Source::score).reversed())
        .limit(5)
        .toList();
  }

  public List<Map<String, Object>> sources(long course) {
    courses.own(course);
    return chunks
        .selectList(
            new QueryWrapper<KnowledgeChunk>()
                .select("id", "title", "source")
                .eq("course_id", course))
        .stream()
        .map(x -> Map.<String, Object>of("id", x.id, "title", x.title, "source", x.source))
        .toList();
  }

  public static double cosine(float[] a, float[] b) {
    if (a.length != b.length) return -1;
    double dot = 0, aa = 0, bb = 0;
    for (int i = 0; i < a.length; i++) {
      dot += a[i] * b[i];
      aa += a[i] * a[i];
      bb += b[i] * b[i];
    }
    return aa == 0 || bb == 0 ? 0 : dot / Math.sqrt(aa * bb);
  }
}

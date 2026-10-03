package com.tang.tianji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tang.tianji.entity.AnswerRecord;

public interface AnswerRecordMapper extends BaseMapper<AnswerRecord> {
  @org.apache.ibatis.annotations.Select(
      "SELECT q.id AS question_id, q.stem, COUNT(a.id) AS wrong_count FROM question q LEFT JOIN"
          + " answer_record a ON a.question_id=q.id AND a.review_status='GRADED' AND"
          + " a.correct=false WHERE q.course_id=#{course} GROUP BY q.id,q.stem")
  java.util.List<java.util.Map<String, Object>> courseWrongs(long course);

  @org.apache.ibatis.annotations.Select(
      "SELECT k.name, COUNT(a.id) AS practice_count, COALESCE(SUM(CASE WHEN a.correct=false THEN 1"
          + " ELSE 0 END),0) AS wrong_count, CASE WHEN COUNT(a.id)=0 THEN 0 ELSE 100*SUM(CASE WHEN"
          + " a.correct=true THEN 1 ELSE 0 END)/COUNT(a.id) END AS accuracy FROM knowledge_point k"
          + " LEFT JOIN question_knowledge qk ON qk.knowledge_point_id=k.id LEFT JOIN answer_record"
          + " a ON a.question_id=qk.question_id AND a.review_status='GRADED' WHERE"
          + " k.course_id=#{course} GROUP BY k.id,k.name")
  java.util.List<java.util.Map<String, Object>> courseWeakness(long course);
}

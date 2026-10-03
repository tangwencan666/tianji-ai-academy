package com.tang.tianji.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tang.tianji.common.*;
import com.tang.tianji.dto.Requests.*;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamService {
  private final AiActivity activity;
  private final QuestionMapper questions;
  private final ExamMapper exams;
  private final ExamQuestionMapper eqs;
  private final ExamRecordMapper records;
  private final AnswerRecordMapper answers;
  private final WrongQuestionMapper wrongs;
  private final QuestionKnowledgeMapper links;
  private final KnowledgePointMapper points;
  private final UserMapper users;
  private final CourseService courses;
  private final Clock clock;

  public ExamService(
      QuestionMapper q,
      ExamMapper e,
      ExamQuestionMapper eq,
      ExamRecordMapper r,
      AnswerRecordMapper a,
      WrongQuestionMapper w,
      QuestionKnowledgeMapper l,
      KnowledgePointMapper k,
      UserMapper u,
      CourseService c,
      Clock t,
      AiActivity activity) {
    this.activity = activity;
    questions = q;
    exams = e;
    eqs = eq;
    records = r;
    answers = a;
    wrongs = w;
    links = l;
    points = k;
    users = u;
    courses = c;
    clock = t;
  }

  public void noActive() {
    BusinessException.check(
        records.selectCount(
                new QueryWrapper<ExamRecord>()
                    .eq("user_id", UserContext.getCurrentUserId())
                    .eq("status", "IN_PROGRESS"))
            == 0,
        409,
        "正式考试进行中，请先提交考试；学习辅助暂时关闭");
  }

  public List<Question> bank(long course, boolean teacher) {
    courses.access(course);
    if (teacher) {
      courses.own(course);
      return questions.selectList(new QueryWrapper<Question>().eq("course_id", course));
    }
    return questions.selectList(
        new QueryWrapper<Question>()
            .select(
                "id", "course_id", "chapter_id", "type", "stem", "options", "difficulty", "status")
            .eq("course_id", course)
            .eq("status", "APPROVED")
            .last("LIMIT 200"));
  }

  public Question getQuestion(long id) {
    var q = questions.selectById(id);
    BusinessException.check(q != null, 404, "题目不存在");
    courses.access(q.courseId);
    return q;
  }

  @Transactional
  public Question create(QuestionInput d, String status) {
    courses.own(d.courseId());
    courses.validateChapter(d.courseId(), d.chapterId());
    BusinessException.check(
        d.type() != null && Set.of("SINGLE", "MULTIPLE", "BOOLEAN", "SHORT").contains(d.type()),
        400,
        "题型无效");
    if (!d.type().equals("SHORT")) {
      BusinessException.check(
          d.options() != null && d.options().size() >= 2 && d.options().size() <= 8,
          400,
          "选项数量应为2至8");
      String allowed = "ABCDEFGH".substring(0, d.options().size());
      var toks = d.answer().toUpperCase(Locale.ROOT).split(",");
      BusinessException.check(d.type().equals("MULTIPLE") || toks.length == 1, 400, "答案格式无效");
      for (String x : toks)
        BusinessException.check(x.length() == 1 && allowed.contains(x), 400, "答案必须是有效选项字母");
    }
    for (long k : d.knowledgePointIds()) {
      var p = points.selectById(k);
      BusinessException.check(p != null && p.courseId.equals(d.courseId()), 400, "知识点不属于此课程");
    }
    var q = new Question();
    q.courseId = d.courseId();
    q.chapterId = d.chapterId();
    q.type = d.type();
    q.stem = d.stem();
    try {
      q.options =
          new com.fasterxml.jackson.databind.ObjectMapper()
              .writeValueAsString(d.options() == null ? List.of() : d.options());
    } catch (Exception e) {
      throw new IllegalArgumentException();
    }
    q.answer = d.answer();
    q.explanation = d.explanation();
    q.difficulty = d.difficulty();
    q.status = status;
    questions.insert(q);
    for (long k : new HashSet<>(d.knowledgePointIds())) {
      var l = new QuestionKnowledge();
      l.questionId = q.id;
      l.knowledgePointId = k;
      links.insert(l);
    }
    return q;
  }

  public void approve(long id) {
    var q = getQuestion(id);
    courses.own(q.courseId);
    q.status = "APPROVED";
    questions.updateById(q);
  }

  @Transactional
  public Exam createExam(ExamInput d) {
    courses.own(d.courseId());
    BusinessException.check(
        new HashSet<>(d.questionIds()).size() == d.questionIds().size(), 400, "试题不能重复");
    var e = new Exam();
    e.courseId = d.courseId();
    e.title = d.title();
    e.durationMinutes = d.durationMinutes();
    e.status = "PUBLISHED";
    exams.insert(e);
    int i = 0;
    for (long id : d.questionIds()) {
      var q = getQuestion(id);
      BusinessException.check(
          q.courseId.equals(d.courseId()) && "APPROVED".equals(q.status), 400, "只能使用本课程审核通过的题目");
      var x = new ExamQuestion();
      x.examId = e.id;
      x.questionId = id;
      x.maxScore = 10;
      x.sortOrder = i++;
      eqs.insert(x);
    }
    return e;
  }

  public List<Exam> exams(long course) {
    courses.access(course);
    return exams.selectList(
        new QueryWrapper<Exam>().eq("course_id", course).eq("status", "PUBLISHED"));
  }

  @Transactional
  public Map<String, Object> start(long id) {
    long uid = UserContext.getCurrentUserId();
    users.lock(uid);
    activity.assertIdle(uid);
    var e = exams.selectById(id);
    BusinessException.check(e != null && "PUBLISHED".equals(e.status), 404, "考试不存在");
    courses.access(e.courseId);
    var active =
        records.selectList(
            new QueryWrapper<ExamRecord>().eq("user_id", uid).eq("status", "IN_PROGRESS"));
    if (!active.isEmpty()) {
      BusinessException.check(active.get(0).examId == id, 409, "请先完成正在进行的考试");
      return paper(active.get(0).id);
    }
    var r = new ExamRecord();
    r.userId = uid;
    r.examId = id;
    r.status = "IN_PROGRESS";
    r.startTime = LocalDateTime.now(clock);
    r.score = 0;
    r.maxScore = examQuestions(id).stream().mapToInt(x -> x.maxScore).sum();
    BusinessException.check(r.maxScore > 0, 400, "试卷为空");
    records.insert(r);
    return paper(r.id);
  }

  private List<ExamQuestion> examQuestions(long id) {
    return eqs.selectList(
        new QueryWrapper<ExamQuestion>().eq("exam_id", id).orderByAsc("sort_order"));
  }

  public ExamRecord ownRecord(long id) {
    var r = records.selectById(id);
    BusinessException.check(
        r != null && r.userId == UserContext.getCurrentUserId(), 404, "考试记录不存在");
    return r;
  }

  public Map<String, Object> paper(long id) {
    var r = ownRecord(id);
    var e = exams.selectById(r.examId);
    var qs = new ArrayList<Map<String, Object>>();
    for (var x : examQuestions(e.id)) {
      var q = questions.selectById(x.questionId);
      qs.add(
          Map.of(
              "id",
              q.id,
              "stem",
              q.stem,
              "type",
              q.type,
              "options",
              q.options,
              "maxScore",
              x.maxScore));
    }
    return Map.of(
        "record",
        r,
        "exam",
        e,
        "deadline",
        r.startTime.plusMinutes(e.durationMinutes),
        "questions",
        qs);
  }

  @Transactional
  public ExamRecord submit(long id, Answers d) {
    long uid = UserContext.getCurrentUserId();
    users.lock(uid);
    var r = ownRecord(id);
    if (!"IN_PROGRESS".equals(r.status)) return r;
    var now = LocalDateTime.now(clock);
    var e = exams.selectById(r.examId);
    boolean expired = now.isAfter(r.startTime.plusMinutes(e.durationMinutes).plusSeconds(5));
    var qs = examQuestions(r.examId);
    var valid = qs.stream().map(x -> x.questionId).toList();
    BusinessException.check(valid.containsAll(d.answers().keySet()), 400, "含非试卷题目");
    boolean review = false;
    for (var x : qs) {
      var q = questions.selectById(x.questionId);
      String a = expired ? "" : d.answers().getOrDefault(q.id, "");
      BusinessException.check(a != null && a.length() <= 8000, 400, "答案过长");
      var ar = answer(q, a, r.id, x.maxScore);
      r.score += ar.score;
      if ("NEEDS_REVIEW".equals(ar.reviewStatus)) review = true;
    }
    r.submitTime = now;
    r.status = expired ? "EXPIRED" : review ? "NEEDS_REVIEW" : "SUBMITTED";
    records.updateById(r);
    return r;
  }

  private AnswerRecord answer(Question q, String text, Long recordId, int max) {
    var a = new AnswerRecord();
    a.userId = UserContext.getCurrentUserId();
    a.examRecordId = recordId;
    a.questionId = q.id;
    a.answer = text;
    a.maxScore = max;
    a.correct = Grading.correct(q.type, q.answer, text);
    a.score = a.correct ? max : 0;
    a.reviewStatus = q.type.equals("SHORT") ? "NEEDS_REVIEW" : "GRADED";
    a.createTime = LocalDateTime.now(clock);
    answers.insert(a);
    if (!a.correct && !q.type.equals("SHORT")) wrong(q.id);
    return a;
  }

  private void wrong(long q) {
    long uid = UserContext.getCurrentUserId();
    var w =
        wrongs.selectOne(new QueryWrapper<WrongQuestion>().eq("user_id", uid).eq("question_id", q));
    boolean fresh = w == null;
    if (fresh) {
      w = new WrongQuestion();
      w.userId = uid;
      w.questionId = q;
      w.wrongCount = 0;
    }
    w.wrongCount++;
    w.masteryStatus = "REVIEW";
    w.lastWrongTime = LocalDateTime.now(clock);
    if (fresh) wrongs.insert(w);
    else wrongs.updateById(w);
  }

  @Transactional
  public Object practice(long id, Practice d) {
    users.lock(UserContext.getCurrentUserId());
    noActive();
    var q = getQuestion(id);
    BusinessException.check("APPROVED".equals(q.status), 403, "题目未审核");
    var a = answer(q, d.answer(), null, 10);
    if (a.correct) {
      var w =
          wrongs.selectOne(
              new QueryWrapper<WrongQuestion>().eq("user_id", a.userId).eq("question_id", id));
      if (w != null) {
        w.masteryStatus = "MASTERED";
        wrongs.updateById(w);
      }
    }
    return Map.of("result", a, "answer", q.answer, "explanation", q.explanation);
  }

  public List<ExamRecord> history() {
    return records.selectList(
        new QueryWrapper<ExamRecord>()
            .eq("user_id", UserContext.getCurrentUserId())
            .orderByDesc("id")
            .last("LIMIT 50"));
  }

  public List<WrongQuestion> wrongs() {
    return wrongs.selectList(
        new QueryWrapper<WrongQuestion>()
            .eq("user_id", UserContext.getCurrentUserId())
            .orderByDesc("last_wrong_time")
            .last("LIMIT 100"));
  }

  public List<AnswerRecord> ownAnswers(long record) {
    var r = ownRecord(record);
    BusinessException.check(!"IN_PROGRESS".equals(r.status), 409, "请先提交考试");
    return answers.selectList(
        new QueryWrapper<AnswerRecord>().eq("exam_record_id", record).eq("user_id", r.userId));
  }

  public AnswerRecord ownAnswer(long id) {
    noActive();
    var a = answers.selectById(id);
    BusinessException.check(
        a != null && a.userId == UserContext.getCurrentUserId(), 404, "答题记录不存在");
    return a;
  }

  public List<Map<String, Object>> mastery() {
    var all =
        answers.selectList(
            new QueryWrapper<AnswerRecord>()
                .eq("user_id", UserContext.getCurrentUserId())
                .eq("review_status", "GRADED")
                .orderByDesc("create_time"));
    var result = new ArrayList<Map<String, Object>>();
    if (all.isEmpty()) return result;
    var questionIds = all.stream().map(a -> a.questionId).distinct().toList();
    var byPoint =
        links
            .selectList(new QueryWrapper<QuestionKnowledge>().in("question_id", questionIds))
            .stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    x -> x.knowledgePointId,
                    java.util.stream.Collectors.mapping(
                        x -> x.questionId, java.util.stream.Collectors.toSet())));
    if (byPoint.isEmpty()) return result;
    for (var k : points.selectByIds(byPoint.keySet())) {
      var ids = byPoint.get(k.id);
      var as = all.stream().filter(a -> ids.contains(a.questionId)).toList();
      if (as.isEmpty()) continue;
      long correct = as.stream().filter(a -> Boolean.TRUE.equals(a.correct)).count();
      var recent = as.stream().limit(10).toList();
      result.add(
          Map.of(
              "knowledgePointId",
              k.id,
              "name",
              k.name,
              "courseId",
              k.courseId,
              "practiceCount",
              as.size(),
              "correctRate",
              correct * 100 / as.size(),
              "wrongCount",
              as.size() - correct,
              "recentAccuracy",
              recent.stream().filter(a -> Boolean.TRUE.equals(a.correct)).count()
                  * 100
                  / recent.size(),
              "lastPracticeTime",
              as.get(0).createTime));
    }
    return result;
  }

  /** Answers are revealed only to their owner and never during an active examination. */
  public List<Map<String, Object>> wrongDetails() {
    noActive();
    var ws = wrongs();
    if (ws.isEmpty()) return List.of();
    var ids = ws.stream().map(w -> w.questionId).toList();
    var qs =
        questions.selectByIds(ids).stream()
            .collect(java.util.stream.Collectors.toMap(q -> q.id, q -> q));
    var attempts =
        answers.selectList(
            new QueryWrapper<AnswerRecord>()
                .eq("user_id", UserContext.getCurrentUserId())
                .in("question_id", ids)
                .eq("correct", false)
                .orderByDesc("create_time", "id"));
    var latest = new HashMap<Long, AnswerRecord>();
    for (var a : attempts) latest.putIfAbsent(a.questionId, a);
    var associations =
        links.selectList(new QueryWrapper<QuestionKnowledge>().in("question_id", ids));
    var pointIds = associations.stream().map(x -> x.knowledgePointId).distinct().toList();
    var names =
        pointIds.isEmpty()
            ? Map.<Long, String>of()
            : points.selectByIds(pointIds).stream()
                .collect(java.util.stream.Collectors.toMap(k -> k.id, k -> k.name));
    var result = new ArrayList<Map<String, Object>>();
    for (var w : ws) {
      var q = qs.get(w.questionId);
      var a = latest.get(w.questionId);
      if (q == null || a == null) continue;
      courses.access(q.courseId);
      result.add(
          Map.of(
              "wrong",
              w,
              "question",
              q,
              "myAnswer",
              a.answer,
              "answerRecordId",
              a.id,
              "knowledgePoints",
              associations.stream()
                  .filter(x -> x.questionId.equals(q.id))
                  .map(x -> names.get(x.knowledgePointId))
                  .filter(Objects::nonNull)
                  .toList()));
    }
    return result;
  }

  public List<AnswerRecord> pending(long course) {
    courses.own(course);
    var ids =
        questions.selectList(new QueryWrapper<Question>().eq("course_id", course)).stream()
            .map(q -> q.id)
            .toList();
    if (ids.isEmpty()) return List.of();
    return answers.selectList(
        new QueryWrapper<AnswerRecord>()
            .in("question_id", ids)
            .eq("review_status", "NEEDS_REVIEW")
            .last("LIMIT 100"));
  }

  @Transactional
  public void review(long id, int score) {
    var a = answers.selectById(id);
    BusinessException.check(a != null, 404, "记录不存在");
    var q = getQuestion(a.questionId);
    courses.own(q.courseId);
    users.lock(a.userId);
    a = answers.selectById(id);
    BusinessException.check(score <= a.maxScore && score >= 0, 400, "分数超出范围");
    BusinessException.check("NEEDS_REVIEW".equals(a.reviewStatus), 409, "该答案已经批改，不能重复确认");
    int previous = a.score;
    a.score = score;
    a.correct = score == a.maxScore;
    a.reviewStatus = "GRADED";
    answers.updateById(a);
    if (!a.correct) {
      var old = UserContext.current();
      try {
        UserContext.set(new UserContext.Principal(a.userId, "STUDENT"));
        wrong(q.id);
      } finally {
        UserContext.set(old);
      }
    }
    if (a.examRecordId != null) {
      var r = records.selectById(a.examRecordId);
      r.score += score - previous;
      if (answers.selectCount(
                  new QueryWrapper<AnswerRecord>()
                      .eq("exam_record_id", r.id)
                      .eq("review_status", "NEEDS_REVIEW"))
              == 0
          && !"EXPIRED".equals(r.status)) r.status = "SUBMITTED";
      records.updateById(r);
    }
  }

  public void feedback(long id, String feedback) {
    var a = ownAnswer(id);
    a.aiFeedback = feedback;
    answers.updateById(a);
  }
}

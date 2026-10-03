CREATE TABLE app_user (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 username VARCHAR(255) NOT NULL,
 password_hash VARCHAR(255) NOT NULL,
 display_name VARCHAR(255),
 role VARCHAR(255) NOT NULL,
 create_time DATETIME(6),
 UNIQUE(username)
);


CREATE TABLE teacher (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 bio VARCHAR(255),
 UNIQUE(user_id),
 FOREIGN KEY(user_id) REFERENCES app_user(id)
);
CREATE INDEX idx_teacher_user_id ON teacher(user_id);

CREATE TABLE course (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(255),
 description TEXT,
 cover VARCHAR(255),
 teacher_id BIGINT,
 category VARCHAR(255),
 difficulty VARCHAR(255),
 status VARCHAR(255),
 total_lessons INT,
 create_time DATETIME(6),
 update_time DATETIME(6),
 FOREIGN KEY(teacher_id) REFERENCES app_user(id)
);
CREATE INDEX idx_course_teacher_id ON course(teacher_id);

CREATE TABLE chapter (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 course_id BIGINT NOT NULL,
 title VARCHAR(255),
 sort_order INT,
 FOREIGN KEY(course_id) REFERENCES course(id)
);
CREATE INDEX idx_chapter_course_id ON chapter(course_id);

CREATE TABLE lesson (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 course_id BIGINT NOT NULL,
 chapter_id BIGINT,
 title VARCHAR(255),
 video_url VARCHAR(255),
 content TEXT,
 attachment VARCHAR(255),
 estimated_minutes INT,
 sort_order INT,
 FOREIGN KEY(course_id) REFERENCES course(id),
 FOREIGN KEY(chapter_id) REFERENCES chapter(id)
);
CREATE INDEX idx_lesson_course_id ON lesson(course_id);
CREATE INDEX idx_lesson_chapter_id ON lesson(chapter_id);

CREATE TABLE learning_record (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 course_id BIGINT NOT NULL,
 chapter_id BIGINT,
 lesson_id BIGINT,
 duration INT,
 progress INT,
 last_position INT,
 status VARCHAR(255),
 last_learn_time DATETIME(6),
 completed_time DATETIME(6),
 UNIQUE(user_id,lesson_id),
 FOREIGN KEY(user_id) REFERENCES app_user(id),
 FOREIGN KEY(lesson_id) REFERENCES lesson(id)
);
CREATE INDEX idx_learning_record_user_id ON learning_record(user_id);
CREATE INDEX idx_learning_record_course_id ON learning_record(course_id);
CREATE INDEX idx_learning_record_chapter_id ON learning_record(chapter_id);
CREATE INDEX idx_learning_record_lesson_id ON learning_record(lesson_id);

CREATE TABLE learning_event (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 course_id BIGINT NOT NULL,
 lesson_id BIGINT,
 duration INT,
 create_time DATETIME(6)
);
CREATE INDEX idx_learning_event_user_id ON learning_event(user_id);
CREATE INDEX idx_learning_event_course_id ON learning_event(course_id);
CREATE INDEX idx_learning_event_lesson_id ON learning_event(lesson_id);

CREATE TABLE knowledge_point (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 course_id BIGINT NOT NULL,
 name VARCHAR(255)
);
CREATE INDEX idx_knowledge_point_course_id ON knowledge_point(course_id);

CREATE TABLE question (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 course_id BIGINT NOT NULL,
 chapter_id BIGINT,
 type VARCHAR(255),
 stem TEXT,
 options TEXT,
 answer TEXT,
 explanation TEXT,
 difficulty VARCHAR(255),
 status VARCHAR(255)
);
CREATE INDEX idx_question_course_id ON question(course_id);
CREATE INDEX idx_question_chapter_id ON question(chapter_id);

CREATE TABLE question_knowledge (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 question_id BIGINT,
 knowledge_point_id BIGINT,
 UNIQUE(question_id,knowledge_point_id),
 FOREIGN KEY(question_id) REFERENCES question(id),
 FOREIGN KEY(knowledge_point_id) REFERENCES knowledge_point(id)
);
CREATE INDEX idx_question_knowledge_question_id ON question_knowledge(question_id);
CREATE INDEX idx_question_knowledge_knowledge_point_id ON question_knowledge(knowledge_point_id);

CREATE TABLE exam (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 course_id BIGINT NOT NULL,
 title VARCHAR(255),
 duration_minutes INT,
 status VARCHAR(255)
);
CREATE INDEX idx_exam_course_id ON exam(course_id);

CREATE TABLE exam_question (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 exam_id BIGINT,
 question_id BIGINT,
 max_score INT,
 sort_order INT,
 UNIQUE(exam_id,question_id),
 FOREIGN KEY(exam_id) REFERENCES exam(id),
 FOREIGN KEY(question_id) REFERENCES question(id)
);
CREATE INDEX idx_exam_question_exam_id ON exam_question(exam_id);
CREATE INDEX idx_exam_question_question_id ON exam_question(question_id);

CREATE TABLE exam_record (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 exam_id BIGINT,
 status VARCHAR(255),
 score INT,
 max_score INT,
 start_time DATETIME(6),
 submit_time DATETIME(6),
 FOREIGN KEY(user_id) REFERENCES app_user(id),
 FOREIGN KEY(exam_id) REFERENCES exam(id)
);
CREATE INDEX idx_exam_record_user_id ON exam_record(user_id);
CREATE INDEX idx_exam_record_exam_id ON exam_record(exam_id);

CREATE TABLE answer_record (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 exam_record_id BIGINT,
 question_id BIGINT,
 answer TEXT,
 score INT,
 max_score INT,
 correct BOOLEAN,
 review_status VARCHAR(255),
 ai_feedback TEXT,
 create_time DATETIME(6),
 UNIQUE(exam_record_id,question_id),
 FOREIGN KEY(user_id) REFERENCES app_user(id),
 FOREIGN KEY(question_id) REFERENCES question(id)
);
CREATE INDEX idx_answer_record_user_id ON answer_record(user_id);
CREATE INDEX idx_answer_record_exam_record_id ON answer_record(exam_record_id);
CREATE INDEX idx_answer_record_question_id ON answer_record(question_id);

CREATE TABLE wrong_question (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 question_id BIGINT,
 wrong_count INT,
 mastery_status VARCHAR(255),
 last_wrong_time DATETIME(6),
 UNIQUE(user_id,question_id),
 FOREIGN KEY(user_id) REFERENCES app_user(id),
 FOREIGN KEY(question_id) REFERENCES question(id)
);
CREATE INDEX idx_wrong_question_user_id ON wrong_question(user_id);
CREATE INDEX idx_wrong_question_question_id ON wrong_question(question_id);

CREATE TABLE favorite (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 course_id BIGINT NOT NULL,
 UNIQUE(user_id,course_id),
 FOREIGN KEY(user_id) REFERENCES app_user(id),
 FOREIGN KEY(course_id) REFERENCES course(id)
);
CREATE INDEX idx_favorite_user_id ON favorite(user_id);
CREATE INDEX idx_favorite_course_id ON favorite(course_id);

CREATE TABLE study_plan (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 course_id BIGINT NOT NULL,
 goal TEXT,
 start_date DATE,
 end_date DATE,
 daily_minutes INT,
 status VARCHAR(255),
 create_time DATETIME(6)
);
CREATE INDEX idx_study_plan_user_id ON study_plan(user_id);
CREATE INDEX idx_study_plan_course_id ON study_plan(course_id);

CREATE TABLE study_plan_day (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 plan_id BIGINT,
 study_date DATE,
 topics TEXT,
 lesson_ids TEXT,
 practice_count INT,
 review_knowledge_points TEXT,
 estimated_minutes INT,
 completed BOOLEAN,
 FOREIGN KEY(plan_id) REFERENCES study_plan(id)
);
CREATE INDEX idx_study_plan_day_plan_id ON study_plan_day(plan_id);

CREATE TABLE ai_conversation (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 course_id BIGINT NOT NULL,
 conversation_key VARCHAR(64) NOT NULL,
 create_time DATETIME(6),
 UNIQUE(user_id,conversation_key),
 FOREIGN KEY(user_id) REFERENCES app_user(id)
);
CREATE INDEX idx_ai_conversation_user_id ON ai_conversation(user_id);
CREATE INDEX idx_ai_conversation_course_id ON ai_conversation(course_id);

CREATE TABLE ai_message (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 conversation_id BIGINT,
 role VARCHAR(255) NOT NULL,
 content TEXT,
 create_time DATETIME(6),
 FOREIGN KEY(conversation_id) REFERENCES ai_conversation(id)
);
CREATE INDEX idx_ai_message_conversation_id ON ai_message(conversation_id);

CREATE TABLE knowledge_chunk (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 course_id BIGINT NOT NULL,
 chapter_id BIGINT,
 lesson_id BIGINT,
 teacher_id BIGINT,
 title VARCHAR(255),
 source VARCHAR(255),
 content TEXT,
 embedding TEXT,
 embedding_model VARCHAR(255)
);
CREATE INDEX idx_knowledge_chunk_course_id ON knowledge_chunk(course_id);
CREATE INDEX idx_knowledge_chunk_chapter_id ON knowledge_chunk(chapter_id);
CREATE INDEX idx_knowledge_chunk_lesson_id ON knowledge_chunk(lesson_id);
CREATE INDEX idx_knowledge_chunk_teacher_id ON knowledge_chunk(teacher_id);

CREATE TABLE ai_call_log (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 request_id VARCHAR(255),
 user_id BIGINT NOT NULL,
 conversation_id VARCHAR(255),
 model VARCHAR(255),
 latency BIGINT,
 tool_calls INT,
 status VARCHAR(255),
 create_time DATETIME(6)
);
CREATE INDEX idx_ai_call_log_request_id ON ai_call_log(request_id);
CREATE INDEX idx_ai_call_log_user_id ON ai_call_log(user_id);
CREATE INDEX idx_ai_call_log_conversation_id ON ai_call_log(conversation_id);

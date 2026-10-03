CREATE INDEX idx_learning_event_user_time ON learning_event(user_id,create_time);
CREATE INDEX idx_exam_record_user_status ON exam_record(user_id,status);
CREATE INDEX idx_answer_user_time ON answer_record(user_id,create_time);
CREATE INDEX idx_answer_question_review ON answer_record(question_id,review_status);
CREATE INDEX idx_message_conversation_id ON ai_message(conversation_id,id);
CREATE INDEX idx_chunk_course_source ON knowledge_chunk(course_id,source);
CREATE INDEX idx_course_status_category ON course(status,category);

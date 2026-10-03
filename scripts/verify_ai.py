"""Opt-in REAL model acceptance. Requires running AI_ENABLED server and demo teacher/student.
Outputs only non-secret status. This consumes provider quota. No mock fallback.
"""
import os,json,urllib.request,uuid
from pathlib import Path
BASE=os.getenv('TIANJI_API','http://127.0.0.1:8080/api')
PASSWORD=os.environ['DEMO_PASSWORD']
def req(method,path,body=None,token=None):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 return urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
def call(method,path,body=None,token=None):
 with urllib.request.urlopen(req(method,path,body,token),timeout=110) as response:return json.load(response)['data']
t=call('POST','/auth/login',dict(username='demo_teacher',password=PASSWORD))['token']
s=call('POST','/auth/login',dict(username='demo_student',password=PASSWORD))['token']
c=next(c for c in call('GET','/courses',token=s) if c['category']=='Java')
d=call('GET',f"/courses/{c['id']}",token=s)
for lesson in d['lessons']:
 call('POST','/knowledge/import',dict(courseId=c['id'],chapterId=lesson['chapterId'],lessonId=lesson['id'],title=lesson['title'],source=f"knowledge/java/lesson-{lesson['id']}.md",content=lesson['content']),t)
sources=call('GET',f"/knowledge/sources?courseId={c['id']}",token=t)
assert sources,'No persisted RAG chunks'
conversation=str(uuid.uuid4());results=[]
for question in ['HashMap为什么线程不安全？','ArrayList和LinkedList有什么区别？','我最近Java哪个知识点最差？','根据我的错题给我推荐下一节课。','我最近在复习Java集合。','给我出5道题。']:
 with urllib.request.urlopen(req('POST','/ai/tutor/chat',dict(courseId=c['id'],conversationId=conversation,message=question),s),timeout=110) as response:
  stream=response.read().decode()
 assert 'event:done' in stream and 'event:error' not in stream, 'Tutor failed; inspect server metadata log'
 assert 'event:answer' in stream, 'Typed TutorAnswer missing'
 results.append(dict(question=question,transport='PASS',semanticReview='REQUIRED'))
plan=call('POST','/ai/plans',dict(courseId=c['id'],goal='我还有7天复习Java，每天2小时，帮我安排。',days=7,dailyMinutes=120),s)
assert len(plan['days'])==7 and len(plan['steps'])<=8
questions=call('POST','/ai/question/generate',dict(courseId=c['id'],topic='Java集合',count=5),t)
assert len(questions)==5 and all(q['status']=='AI_GENERATED' for q in questions)
call('GET','/ai/report/weekly',token=s);call('POST','/ai/wrong-analysis',token=s)
Path('docs/real-ai-verification.json').write_text(json.dumps(dict(provider='REAL',tutor=results,plan='PASS',questions='DRAFTS_ONLY',note='Transport/schema assertions do not verify factual accuracy, retrieval quality or memory semantics. Review six responses in browser.'),ensure_ascii=False,indent=2),encoding='utf-8')
print('Real provider transport, typed output and persistence checks passed. Human semantic review still required.')

import {test} from 'node:test';
import assert from 'node:assert/strict';
import {consumeSse} from '../src/sse.js';
function body(text){const bytes=new TextEncoder().encode(text);return new ReadableStream({start(c){for(const b of bytes)c.enqueue(Uint8Array.of(b));c.close()}})}
test('split UTF-8 and CRLF preserve real multiple chunks and completion',async()=>{
  const events=[];
  await consumeSse(body('event:delta\r\ndata:"集合"\r\n\r\nevent:delta\r\ndata:"线程安全"\r\n\r\nevent:complete\r\ndata:{}\r\n\r\n'),(n,v)=>events.push([n,v]));
  assert.deepEqual(events.slice(0,2),[['delta','集合'],['delta','线程安全']]);
  assert.equal(events[2][0],'complete');
});
test('premature connection end rejects without claiming success',async()=>{
  await assert.rejects(consumeSse(body('event:delta\ndata:"未完成"\n\n'),()=>{}),/提前结束/);
});
test('safe upstream error is terminal',async()=>{
  const events=[];await consumeSse(body('event:error\ndata:"模型请求超时"\n\n'),(n,v)=>events.push([n,v]));assert.equal(events[0][1],'模型请求超时');
});
test('cancelled reader rejects and releases the reader lock',async()=>{
  const stream=new ReadableStream({start(c){c.error(new DOMException('Stopped','AbortError'))}});
  await assert.rejects(consumeSse(stream,()=>{}),{name:'AbortError'});assert.equal(stream.locked,false);
});

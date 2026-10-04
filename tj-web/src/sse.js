export async function consumeSse(body,onEvent) {
  const reader=body.getReader(),decoder=new TextDecoder();
  let buffer='',terminal=false;
  try {
    while(true){
      const {value,done}=await reader.read();
      buffer+=decoder.decode(value||new Uint8Array(),{stream:!done});
      buffer=buffer.replace(/\r\n/g,'\n');
      let index;
      while((index=buffer.indexOf('\n\n'))>=0){
        const frame=buffer.slice(0,index);buffer=buffer.slice(index+2);
        let name='message';const lines=[];
        for(const line of frame.split('\n')){
          if(line.startsWith('event:'))name=line.slice(6).trim();
          if(line.startsWith('data:'))lines.push(line.slice(5).trimStart());
        }
        if(lines.length){
          const raw=lines.join('\n');let payload;try{payload=JSON.parse(raw)}catch{payload=raw}
          await onEvent(name,payload);
          if(['done','complete','error'].includes(name))terminal=true;
        }
      }
      if(done)break;
    }
    if(!terminal)throw new Error('连接提前结束，请重试');
  }finally{await reader.cancel().catch(()=>{});reader.releaseLock()}
}

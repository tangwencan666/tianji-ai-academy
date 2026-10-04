package com.tang.tianji.ai.service;

import java.util.regex.Pattern;

/** Decode only the public answer field as actual model JSON tokens arrive. Final schema is validated separately. */
public class StreamingAnswer {
  private final StringBuilder raw=new StringBuilder();
  private int cursor=-1;
  private boolean ended;
  public String append(String token) {
    raw.append(token);
    if(ended) return "";
    if(cursor<0) {
      var start=Pattern.compile("\"answer\"\\s*:\\s*\"").matcher(raw);
      if(!start.find()) return "";
      cursor=start.end();
    }
    var out=new StringBuilder();
    while(cursor<raw.length()) {
      char c=raw.charAt(cursor);
      if(c=='"') {ended=true;break;}
      if(c!='\\') {out.append(c);cursor++;continue;}
      if(cursor+1>=raw.length()) break;
      char esc=raw.charAt(cursor+1);
      if(esc=='u') {
        if(cursor+6>raw.length()) break;
        out.append((char)Integer.parseInt(raw.substring(cursor+2,cursor+6),16));cursor+=6;
      } else {
        out.append(switch(esc) {case 'n'->'\n';case 'r'->'\r';case 't'->'\t';case 'b'->'\b';case 'f'->'\f';case '"'->'"';case '\\'->'\\';case '/'->'/';default->throw new IllegalArgumentException("Invalid JSON escape");});
        cursor+=2;
      }
    }
    return out.toString();
  }
}

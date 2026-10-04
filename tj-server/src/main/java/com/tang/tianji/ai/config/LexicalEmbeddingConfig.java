package com.tang.tianji.ai.config;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

/** Actual local lexical vectors, NOT a neural embedding model or a mocked remote response. */
@Configuration
@ConditionalOnProperty(name="app.embedding-mode", havingValue="lexical", matchIfMissing=true)
public class LexicalEmbeddingConfig {
  public static final String MODEL = "local-lexical-hash-v1";
  public static float[] vector(String text) {
    float[] v = new float[2048];
    var matcher = Pattern.compile("[a-z_][a-z0-9_]+|[\\p{IsHan}]+", Pattern.CASE_INSENSITIVE).matcher(text.toLowerCase(Locale.ROOT));
    while (matcher.find()) {
      String term = matcher.group();
      if (term.charAt(0) < 128) add(v,term,3);
      else {
        // Single common characters and bare numbers are poor topic signals (e.g. "5道题").
        for(int i=0;i+1<term.length();i++) add(v,term.substring(i,i+2),1);
      }
    }
    double norm=0; for(float value:v) norm+=value*value;
    if(norm>0) for(int i=0;i<v.length;i++) v[i]/=(float)Math.sqrt(norm);
    return v;
  }
  private static void add(float[] v,String term,float weight) {v[Math.floorMod(term.hashCode(),v.length)]+=weight;}
  @Bean @Primary public EmbeddingModel lexicalEmbeddingModel() {
    return new EmbeddingModel() {
      public float[] embed(Document d) { return vector(d.getText()); }
      public EmbeddingResponse call(EmbeddingRequest request) {
        var results=new ArrayList<Embedding>(); int i=0;
        for(String text:request.getInstructions()) results.add(new Embedding(vector(text),i++));
        return new EmbeddingResponse(results);
      }
      public int dimensions() {return 2048;}
    };
  }
}

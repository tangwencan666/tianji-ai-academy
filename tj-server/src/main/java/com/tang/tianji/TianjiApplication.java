package com.tang.tianji;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.tang.tianji.mapper")
public class TianjiApplication {
  public static void main(String[] args) {
    SpringApplication.run(TianjiApplication.class, args);
  }
}

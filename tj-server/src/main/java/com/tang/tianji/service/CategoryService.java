package com.tang.tianji.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tang.tianji.entity.Course;
import com.tang.tianji.mapper.CourseMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {
  private final CourseMapper mapper;

  public CategoryService(CourseMapper m) {
    mapper = m;
  }

  @Cacheable("categories")
  public java.util.List<String> categories() {
    return new java.util.ArrayList<>(
        mapper.selectList(new QueryWrapper<Course>().eq("status", "PUBLISHED")).stream()
            .map(c -> c.category)
            .distinct()
            .sorted()
            .toList());
  }
}

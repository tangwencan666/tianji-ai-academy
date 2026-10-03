package com.tang.tianji.controller;

import com.tang.tianji.common.Result;
import com.tang.tianji.dto.Requests.*;
import com.tang.tianji.service.*;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CourseController {
  private final CourseService s;
  private final CategoryService categories;

  public CourseController(CourseService s, CategoryService c) {
    this.s = s;
    categories = c;
  }

  @GetMapping("/courses")
  public Object list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "false") boolean mine) {
    return Result.ok(s.list(search, category, mine));
  }

  @GetMapping("/courses/categories")
  public Object categories() {
    return Result.ok(categories.categories());
  }

  @GetMapping("/courses/{id}")
  public Object detail(@PathVariable long id) {
    return Result.ok(s.detail(id));
  }

  @GetMapping("/lessons/{id}")
  public Object lesson(@PathVariable long id) {
    return Result.ok(s.lesson(id));
  }

  @PostMapping("/courses")
  public Object create(@Valid @RequestBody CourseInput d) {
    return Result.ok(s.save(null, d));
  }

  @PutMapping("/courses/{id}")
  @CacheEvict(value = "categories", allEntries = true)
  public Object edit(@PathVariable long id, @Valid @RequestBody CourseInput d) {
    return Result.ok(s.save(id, d));
  }

  @PatchMapping("/courses/{id}/status")
  @CacheEvict(value = "categories", allEntries = true)
  public Object status(@PathVariable long id, @RequestParam String status) {
    s.status(id, status);
    return Result.ok(null);
  }

  @PostMapping("/courses/{id}/chapters")
  public Object chapter(@PathVariable long id, @Valid @RequestBody ChapterInput d) {
    return Result.ok(s.chapter(id, d));
  }

  @PostMapping("/courses/{id}/lessons")
  public Object lesson(@PathVariable long id, @Valid @RequestBody LessonInput d) {
    return Result.ok(s.addLesson(id, d));
  }

  @PostMapping("/courses/{id}/knowledge-points")
  public Object point(@PathVariable long id, @RequestParam String name) {
    return Result.ok(s.point(id, name));
  }

  @GetMapping("/favorites")
  public Object favorites() {
    return Result.ok(s.favorites());
  }

  @PutMapping("/favorites/{id}")
  public Object favorite(@PathVariable long id) {
    s.favorite(id, true);
    return Result.ok(null);
  }

  @DeleteMapping("/favorites/{id}")
  public Object unfavorite(@PathVariable long id) {
    s.favorite(id, false);
    return Result.ok(null);
  }
}

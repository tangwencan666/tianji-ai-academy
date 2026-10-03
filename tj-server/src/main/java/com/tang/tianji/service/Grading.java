package com.tang.tianji.service;

import java.util.*;
import java.util.stream.Collectors;

public final class Grading {
  public static boolean correct(String type, String expected, String actual) {
    if (actual == null || expected == null || "SHORT".equals(type)) return false;
    if ("MULTIPLE".equals(type)) return normalize(expected).equals(normalize(actual));
    return expected.strip().equalsIgnoreCase(actual.strip());
  }

  private static Set<String> normalize(String s) {
    return Arrays.stream(s.toUpperCase(Locale.ROOT).split("[,，\\s]+"))
        .filter(x -> !x.isBlank())
        .collect(Collectors.toSet());
  }
}

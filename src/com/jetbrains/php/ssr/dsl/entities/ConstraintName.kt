package com.jetbrains.php.ssr.dsl.entities

enum class ConstraintName(val docName: String, val string: Boolean) {
  NAME("name", true),
  REGEXP("regExp", true),
  MIN_COUNT("minCount", false),
  MAX_COUNT("maxCount", false),
  TYPE("type", true),
  REFERENCE_CONSTRAINT_NAME("referenceName", true),
  REFERENCE_CONSTRAINT("reference", true),
  TARGET("target", false),
}
package com.jetbrains.php.ssr.dsl.entities

enum class CustomDocTag(val displayName: String, val singleArgument: Boolean) {
  SSR_TEMPLATE("@structuralSearchTemplate", true),
  SCOPE("@scope", true),
  SEVERITY("@severity", true),
  VARIABLE("@variable", false),
  IGNORED_VARIABLES("@ignoredVariables", false),
}

enum class Severity {
  OFF, WEAK_WARNING, WARNING, ERROR, DEFAULT;
}

fun String.toSeverity(): Severity = Severity.values().find { it.name.toUpperCase() == this.toUpperCase() } ?: Severity.DEFAULT
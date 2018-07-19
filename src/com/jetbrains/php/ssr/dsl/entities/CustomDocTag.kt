package com.jetbrains.php.ssr.dsl.entities

enum class CustomDocTag(val displayName: String, val singleArgument: Boolean) {
  SSR_TEMPLATE("@structuralSearchTemplate", true),
  SCOPE("@scope", true),
  VARIABLE("@variable", false),
}
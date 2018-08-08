package com.jetbrains.php.ssr.dsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.ssr.dsl.entities.CustomDocTag
import com.jetbrains.php.ssr.dsl.entities.Severity

class SeverityCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, Patterns.docTagValue(CustomDocTag.SEVERITY), object : CompletionProvider<CompletionParameters>() {
      override fun addCompletions(p0: CompletionParameters, p1: ProcessingContext?, result: CompletionResultSet) {
        addStrings(Severity.values().map { it.name.toLowerCase() }, result)
      }
    })
  }
}
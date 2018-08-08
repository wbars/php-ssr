package com.jetbrains.php.ssr.dsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.jetbrains.php.ssr.dsl.completion.Patterns.docTagValue
import com.jetbrains.php.ssr.dsl.entities.CustomDocTag
import com.jetbrains.php.ssr.dsl.entities.SearchScope

class ScopesCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, docTagValue(CustomDocTag.SCOPE), ScopesCompletionProvider())
  }
}

class ScopesCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, ctx: ProcessingContext?, result: CompletionResultSet) {
    for (value in SearchScope.values()) {
      result.addElement(LookupElementBuilder.create(value.name.toLowerCase()))
    }
  }
}

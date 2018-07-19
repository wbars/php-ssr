package com.jetbrains.php.ssr.dsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes.DOC_STRING
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes
import com.jetbrains.php.ssr.dsl.entities.CustomDocTag
import com.jetbrains.php.ssr.dsl.entities.SearchScope

class ScopesCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, psiElement(DOC_STRING).withSuperParent(
      2, psiElement(PhpDocElementTypes.phpDocAttributeList)
      .withParent(Patterns.docTagWithName(CustomDocTag.SCOPE.displayName))
    ), ScopesCompletionProvider())
  }
}

class ScopesCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, ctx: ProcessingContext?, result: CompletionResultSet) {
    for (value in SearchScope.values()) {
      result.addElement(LookupElementBuilder.create(value.name.toLowerCase()))
    }
  }
}

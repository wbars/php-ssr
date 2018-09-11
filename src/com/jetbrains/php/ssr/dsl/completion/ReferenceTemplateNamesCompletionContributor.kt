package com.jetbrains.php.ssr.dsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.structuralsearch.plugin.ui.ConfigurationManager
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes.DOC_STRING
import com.jetbrains.php.ssr.dsl.completion.Patterns.insideValueOfAttribute
import com.jetbrains.php.ssr.dsl.entities.ConstraintName
import com.jetbrains.php.ssr.dsl.indexing.TemplateIndex

//import com.jetbrains.php.ssr.dsl.entities.ConstraintName
//import com.jetbrains.php.ssr.dsl.indexing.TemplateIndex

class ReferenceTemplateNamesCompletionContributor: CompletionContributor() {
  init {
    extend(CompletionType.BASIC, psiElement(DOC_STRING).withParent(insideValueOfAttribute(
      ConstraintName.REFERENCE_CONSTRAINT_NAME)), ReferenceTemplateNamesProvider())
  }
}

class ReferenceTemplateNamesProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val project = parameters.position.project
    val names: MutableSet<String> = mutableSetOf()
    for (name in ConfigurationManager.getInstance(project).allConfigurationNames + TemplateIndex.getAllTemplateNames(project)) {
      if (names.add(name)) {
        result.addElement(LookupElementBuilder.create(name))
      }
    }
  }

}

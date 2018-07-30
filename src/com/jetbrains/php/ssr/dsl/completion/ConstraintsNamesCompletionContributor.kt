package com.jetbrains.php.ssr.dsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes.*
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.ssr.dsl.indexing.getSearchConstraints

class ConstraintsNamesCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, psiElement().withParent(
      Patterns.variableAttributes).afterLeaf(psiElement(DOC_LPAREN)),
           ConstraintsNamesCompletionProvider())
    extend(CompletionType.BASIC, psiElement().withParent(
      Patterns.variableAttributes).afterLeaf(psiElement(DOC_COMMA)),
           ConstraintsNamesCompletionProvider())
    extend(CompletionType.BASIC, psiElement().withParent(
      Patterns.variableAttributes).afterLeaf(psiElement(DOC_LEADING_ASTERISK)),
           ConstraintsNamesCompletionProvider())
  }

  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    super.fillCompletionVariants(parameters, result)
  }
}

class ConstraintsNamesCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
    val project = parameters.position.project
    for (constraint in getSearchConstraints(project) ?: return) {
      val value = constraint.getValueContents() ?: return
      result.addElement(LookupElementBuilder.create(value).withPsiElement(constraint))
    }
  }
}

fun Field.getValueContents() = (defaultValue as? StringLiteralExpression)?.contents

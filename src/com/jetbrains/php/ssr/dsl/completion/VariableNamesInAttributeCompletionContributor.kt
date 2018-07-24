package com.jetbrains.php.ssr.dsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.ssr.dsl.entities.ConstraintName
import com.jetbrains.php.ssr.dsl.indexing.getPattern
import com.jetbrains.php.ssr.dsl.util.collectVariables

class VariableNamesInAttributeCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(PhpDocTokenTypes.DOC_STRING).withParent(Patterns.insideValueOfAttribute(
      ConstraintName.NAME)), VariableNamesInAttributeCompletionProvider())
  }
}

class VariableNamesInAttributeCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, processingContext: ProcessingContext?, result: CompletionResultSet) {
    val docComment: PhpDocComment = PhpPsiUtil.getParentByCondition(parameters.position, PhpDocComment.INSTANCEOF) ?: return
    for (variable in collectVariables(docComment.getPattern())) {
      result.addElement(LookupElementBuilder.create(variable.name).withIcon(AllIcons.Nodes.Variable))
    }
  }
}

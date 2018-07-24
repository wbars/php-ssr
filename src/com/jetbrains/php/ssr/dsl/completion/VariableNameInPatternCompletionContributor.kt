package com.jetbrains.php.ssr.dsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.lexer.PhpTokenTypes.IDENTIFIER
import com.jetbrains.php.lang.parser.PhpElementTypes.STATEMENT
import com.jetbrains.php.lang.parser.PhpElementTypes.USE_LIST
import com.jetbrains.php.lang.parser.PhpStubElementTypes.*
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.PhpPsiUtil.isOfType
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.ssr.dsl.entities.ConstraintName
import com.jetbrains.php.ssr.dsl.indexing.ConstraintRawData
import com.jetbrains.php.ssr.dsl.indexing.parseVariables
import com.jetbrains.php.ssr.dsl.indexing.ssrDoc
import com.jetbrains.php.ssr.dsl.marker.get

class VariableNameInPatternCompletionContributor: CompletionContributor() {
  init {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(IDENTIFIER).withText(StandardPatterns.string().startsWith("_")), VariableNamesInPatternCompletionProvider(false))
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(PhpTokenTypes.VARIABLE).withParent(Variable::class.java).withText(StandardPatterns.string().startsWith("\$_")), VariableNamesInPatternCompletionProvider(true))
  }
}

class VariableNamesInPatternCompletionProvider(val inVariable: Boolean) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
    val topStatement = PhpPsiUtil.getParentByCondition<PsiElement>(parameters.position) { it.isTopStatement() } ?: return
    val ssrDocComment = PhpPsiUtil.getPrevSiblingByCondition<PhpDocComment>(topStatement) { it is PhpDocComment && it.ssrDoc() } ?: return
    for (variable in ssrDocComment.parseVariables()) {
      result.addElement(LookupElementBuilder.create(createLookupString(variable)).withPresentableText(variable.name).withIcon(AllIcons.Nodes.Variable))
    }
  }

  private fun createLookupString(variable: ConstraintRawData) = if (inVariable) "\$_" + variable.name else "_" + variable.name
}

private val ConstraintRawData.name: String
  get() = constraints[ConstraintName.NAME]!!

fun PsiElement.isTopStatement(): Boolean = isOfType(this, FUNCTION, NAMESPACE, USE_LIST, CLASS, STATEMENT)

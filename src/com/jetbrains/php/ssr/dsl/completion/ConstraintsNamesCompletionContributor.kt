package com.jetbrains.php.ssr.dsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpIcons
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes.*
import com.jetbrains.php.ssr.dsl.entities.ConstraintName

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
    for (value in ConstraintName.values()) {
      result.addElement(
        LookupElementBuilder.create(value.docName)
          .withIcon(PhpIcons.PARAMETER)
          .withInsertHandler { ctx, _ ->
            PhpInsertHandlerUtil.insertStringAtCaret(ctx.editor, "=")
            if (value.string) {
              PhpInsertHandlerUtil.insertStringAtCaret(ctx.editor, "\"\"")
              ctx.editor.stepCaretBack()
            }
          })
    }
  }

}

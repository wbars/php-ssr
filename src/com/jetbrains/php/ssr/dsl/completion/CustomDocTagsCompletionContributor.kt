package com.jetbrains.php.ssr.dsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocPsiElement
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.ssr.dsl.entities.CustomDocTag

class CustomDocTagsCompletionContributor: CompletionContributor() {
  init {
    extend(CompletionType.BASIC, psiElement().withSuperParent(1, PhpDocPsiElement::class.java),
           CustomDocTagsCompletionProvider())
  }
}

class CustomDocTagsCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, ctx: ProcessingContext, result: CompletionResultSet) {
    val at = parameters.position.parent is PhpDocTag
    for (value in CustomDocTag.values()) {
      val lookupString = if (at) value.displayName.substring(1) else value.displayName
      result.addElement(LookupElementBuilder.create(lookupString).withBoldness(true).withInsertHandler { ctx, _ ->
        PhpInsertHandlerUtil.insertStringAtCaret(ctx.editor, "()")
        ctx.editor.stepCaretBack()
        if (value.singleArgument) {
          PhpInsertHandlerUtil.insertStringAtCaret(ctx.editor, "\"\"")
          ctx.editor.stepCaretBack()
        }
      })

    }
  }
}

fun Editor.stepCaretBack() {
  caretModel.moveToOffset(caretModel.offset - 1)
}

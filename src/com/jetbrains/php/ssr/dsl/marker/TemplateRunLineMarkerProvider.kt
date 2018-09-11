package com.jetbrains.php.ssr.dsl.marker

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes.DOC_TAG_NAME
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.PhpPsiUtil.isOfType
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class TemplateRunLineMarkerProvider : RunLineMarkerContributor() {
  override fun getInfo(leaf: PsiElement): Info? {
    val parent = PhpPsiUtil.getParentByCondition<PhpDocComment>(leaf, PhpDocComment.INSTANCEOF)
    if (parent != null && leaf.isSSRTemplateTag()) {
      val templateName = "Run " + (PsiTreeUtil.findChildOfType(parent, StringLiteralExpression::class.java)?.contents ?: "template")
      return Info(AllIcons.Actions.Rerun, arrayOf<AnAction>(object : AnAction(templateName) {
        override fun actionPerformed(event: AnActionEvent) {
          parent.runSearchTemplate(event)
        }
      })) { templateName }
    }
    return null
  }
}

fun PsiElement.isSSRTemplateTag() = isOfType(this, DOC_TAG_NAME) && text == "@structuralSearchTemplate"


package com.jetbrains.php.ssr.dsl.actions

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class NewSearchTemplateAction: CodeInsightAction(), CodeInsightActionHandler  {
  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    val template = TemplateManager.getInstance(project).createTemplate("", "")
    template.addTextSegment("/**\n" +
                            " * @structuralSearchTemplate(\"")
    template.addVariable("name", ConstantNode("Name"), true)
    template.addTextSegment("\") */")
    template.isToReformat = true
    TemplateManager.getInstance(project).startTemplate(editor, template)
  }

  override fun getHandler() = this
}
package com.jetbrains.php.ssr.dsl.marker

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.MatchVariableConstraint
import com.intellij.structuralsearch.plugin.ui.Configuration
import com.intellij.structuralsearch.plugin.ui.SearchCommand
import com.intellij.structuralsearch.plugin.ui.SearchConfiguration
import com.intellij.structuralsearch.plugin.ui.SearchContext
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.ssr.dsl.entities.ConstraintName
import com.jetbrains.php.ssr.dsl.entities.CustomDocTag
import com.jetbrains.php.ssr.dsl.indexing.TemplateIndex

fun PhpDocComment.runSearchTemplate(event: AnActionEvent) {
  val configuration = this.buildConfiguration()?:return
  SearchCommand(configuration, SearchContext.buildFromDataContext(event.dataContext)).startSearching()
}

private fun PhpDocComment.buildConfiguration(): Configuration? {
  val name = findTagByName(CustomDocTag.SSR_TEMPLATE.displayName)?.getStringValue() ?: return null
  val templateRawData = TemplateIndex.findTemplateRawData(project, name) ?: return null
  val configuration = SearchConfiguration()
  configuration.matchOptions.scope = templateRawData.scope.createScope(project)
  configuration.matchOptions.fileType = PhpFileType.INSTANCE
  configuration.matchOptions.searchPattern = templateRawData.pattern
  for (variable in templateRawData.variables) {
    val constraint: MatchVariableConstraint = createMatchVariableConstraint(variable.constraints,
                                                                                                             variable.inverses) ?: continue
    configuration.matchOptions.addVariableConstraint(constraint)
  }
  return configuration
}

fun PhpDocTag.getStringValue(): String? = getAttributesList()?.getChildByCondition(StringLiteralExpression.INSTANCEOF)?.contents

private fun PsiElement.getChildByCondition(instanceof: Condition<PsiElement>) =
  PhpPsiUtil.getChildByCondition<StringLiteralExpression>(this, instanceof)

fun PhpDocComment.findTagByName(name: String) =
  PhpPsiUtil.getChildByCondition<PhpDocTag>(this) { it is PhpDocTag && it.name == name }

private fun PhpDocTag.getAttributesList() =
  PhpPsiUtil.getChildOfType(this, PhpDocElementTypes.phpDocAttributeList)

private fun createMatchVariableConstraint(constraints: MutableMap<String, String>,
                                          inverses: MutableMap<String, Boolean>): MatchVariableConstraint? {
  if (!constraints.containsKey("name")) return null
  val res = MatchVariableConstraint()
  if (constraints[ConstraintName.NAME.docName] != null) res.name = constraints[ConstraintName.NAME.docName]
  if (constraints[ConstraintName.MIN_COUNT.docName] != null) res.minCount = Integer.parseInt(constraints[ConstraintName.MIN_COUNT.docName])
  if (constraints[ConstraintName.MAX_COUNT.docName] != null) {
    res.maxCount = if (constraints[ConstraintName.MAX_COUNT.docName] == "inf") Integer.MAX_VALUE
    else Integer.parseInt(constraints[ConstraintName.MAX_COUNT.docName])
  }
  if (constraints[ConstraintName.REGEXP.docName] != null) {
    res.regExp = constraints[ConstraintName.REGEXP.docName]
    res.isInvertRegExp = inverses[ConstraintName.REGEXP.docName]!!
  }
  if (constraints[ConstraintName.TYPE.docName] != null) {
    res.nameOfExprType = constraints[ConstraintName.TYPE.docName]
    res.isInvertExprType = inverses[ConstraintName.TYPE.docName]!!
  }
  if (constraints[ConstraintName.REFERENCE_CONSTRAINT_NAME.docName] != null) {
    res.referenceConstraint = constraints[ConstraintName.REFERENCE_CONSTRAINT_NAME.docName]
    res.isInvertReference = inverses[ConstraintName.REFERENCE_CONSTRAINT_NAME.docName]!!
  }
  else if (constraints[ConstraintName.REFERENCE_CONSTRAINT.docName] != null) {
    res.referenceConstraint = "\"" + constraints[ConstraintName.REFERENCE_CONSTRAINT.docName] + "\""
    res.isInvertReference = inverses[ConstraintName.REFERENCE_CONSTRAINT.docName]!!
  }
  return res
}
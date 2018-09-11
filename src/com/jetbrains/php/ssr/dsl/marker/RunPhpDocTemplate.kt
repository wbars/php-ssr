package com.jetbrains.php.ssr.dsl.marker

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.MatchVariableConstraint
import com.intellij.structuralsearch.plugin.ui.*
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.ssr.dsl.entities.ConstraintName
import com.jetbrains.php.ssr.dsl.entities.ConstraintName.*
import com.jetbrains.php.ssr.dsl.entities.CustomDocTag
import com.jetbrains.php.ssr.dsl.indexing.TemplateIndex
import com.jetbrains.php.ssr.dsl.indexing.TemplateRawData
import com.jetbrains.php.ssr.dsl.util.transformToStringCriteria
import com.jetbrains.php.ssr.dsl.util.wrapVariablesNames
import java.lang.Boolean.parseBoolean

fun PhpDocComment.runSearchTemplate(event: AnActionEvent) {
  val configuration = this.buildConfiguration()?:return
  SearchCommand(configuration, SearchContext(event.dataContext)).startSearching()
}

private fun PhpDocComment.buildConfiguration(): Configuration? {
  val name = findTagByName(CustomDocTag.SSR_TEMPLATE.displayName)?.getStringValue() ?: return null
  val templateRawData = TemplateIndex.findTemplateRawData(project, name) ?: return null
  return templateRawData.buildConfiguration(project)
}

fun TemplateRawData.buildConfiguration(project: Project): SearchConfiguration {
  val configuration = SearchConfiguration()
  configuration.name = name
  configuration.matchOptions.scope = scope.createScope(project)
  configuration.matchOptions.fileType = PhpFileType.INSTANCE
  configuration.matchOptions.searchPattern = wrapVariablesNames(pattern, { if (ignoredVariables.contains(it)) "_" else "$" },
                                                                { if (ignoredVariables.contains(it)) "" else "$" })
  for (variable in variables) {
    val constraint: MatchVariableConstraint = createMatchVariableConstraint(project, variable.constraints, variable.inverses) ?: continue
    configuration.matchOptions.addVariableConstraint(constraint)
  }
  return configuration
}

fun PhpDocTag.getStringValue(): String? = getAttributesList()?.getChildByCondition(StringLiteralExpression.INSTANCEOF)?.contents

fun PsiElement.getChildByCondition(instanceof: Condition<PsiElement>) =
  PhpPsiUtil.getChildByCondition<StringLiteralExpression>(this, instanceof)

fun PhpDocComment.findTagByName(name: String) =
  PhpPsiUtil.getChildByCondition<PhpDocTag>(this) { it is PhpDocTag && it.name == name }

fun PhpDocTag.getAttributesList() =
  PhpPsiUtil.getChildOfType(this, PhpDocElementTypes.phpDocAttributeList)

private fun createMatchVariableConstraint(project: Project, constraints: MutableMap<String, String>,
                                          inverses: MutableMap<String, Boolean>): MatchVariableConstraint? {
  if (!constraints.contains(NAME)) return null
  val res = MatchVariableConstraint()
  res.name = constraints[NAME]?:res.name
  res.minCount = constraints.getInt(MIN_COUNT)?:res.minCount
  res.maxCount = constraints[MAX_COUNT]?.roundToInf()?:res.maxCount
  if (constraints.contains(REGEXP)) {
    res.regExp = constraints[REGEXP]
    res.isInvertRegExp = inverses[REGEXP]!!
  }
  if (constraints.contains(TYPE)) {
    res.nameOfExprType = constraints[TYPE]
    res.isInvertExprType = inverses[TYPE]!!
  }
  if (constraints.contains(REFERENCE_CONSTRAINT_NAME)) {
    val constraintName = constraints[REFERENCE_CONSTRAINT_NAME]
    res.referenceConstraint = getStringCriteria(project, constraintName)
    res.isInvertReference = inverses[REFERENCE_CONSTRAINT_NAME]!!
  }
  else if (constraints.contains(REFERENCE_CONSTRAINT)) {
    res.referenceConstraint = "\"" + constraints[REFERENCE_CONSTRAINT] + "\""
    res.isInvertReference = inverses[REFERENCE_CONSTRAINT]!!
  }
  res.isPartOfSearchResults = constraints.getBoolean(TARGET)?:res.isPartOfSearchResults
  return res
}

fun getStringCriteria(project: Project, constraintName: String?, usedTemplatesNames : MutableSet<String> = mutableSetOf()): String {
  if (constraintName in ConfigurationManager.getInstance(project).allConfigurationNames) {
    return constraintName?:""
  }
  else {
    val templateRawData = TemplateIndex.findTemplateRawData(project, constraintName!!)
    if (templateRawData != null) {
      return '"' + transformToStringCriteria(project, templateRawData, usedTemplatesNames) + '"'
    }
  }
  return ""
}

private fun String.roundToInf(): Int = toIntOrNull() ?: Int.MAX_VALUE

private fun <V> MutableMap<String, V>.contains(name: ConstraintName) = this[name.docName] != null

operator fun <V> MutableMap<String, V>.get(name: ConstraintName) = this[name.docName]

private fun MutableMap<String, String>.getInt(name: ConstraintName): Int? {
  return this[name.docName]?.toIntOrNull()
}

fun MutableMap<String, String>.getBoolean(name: ConstraintName): Boolean? {
  val v = this[name.docName]
  return if (v != null) parseBoolean(v) else null
}


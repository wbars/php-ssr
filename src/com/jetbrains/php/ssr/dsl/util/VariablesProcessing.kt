package com.jetbrains.php.ssr.dsl.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Segment
import com.intellij.openapi.util.TextRange
import com.jetbrains.php.ssr.dsl.completion.name
import com.jetbrains.php.ssr.dsl.entities.ConstraintName.*
import com.jetbrains.php.ssr.dsl.indexing.ConstraintRawData
import com.jetbrains.php.ssr.dsl.indexing.TemplateRawData
import com.jetbrains.php.ssr.dsl.marker.getBoolean
import com.jetbrains.php.ssr.dsl.marker.getStringCriteria

fun collectVariables(searchPattern: String): List<VariableInfo> {
  val res: MutableList<VariableInfo> = mutableListOf()
  var insideVariable = false
  var startOffset = 0
  var sb = StringBuilder()
  for (i in searchPattern.indices) {
    val c = searchPattern[i]
    if (insideVariable) {
      //todo "_a_b_c" case
      if (!Character.isJavaIdentifierPart(c)) {
        res.add(VariableInfo(TextRange.create(startOffset, i), sb.toString()))
        sb = StringBuilder()
        insideVariable = false
      } else {
        sb.append(c)
      }
    }

    if (!insideVariable && variableStart(i, searchPattern)) {
      insideVariable = true
      startOffset = getStartOffset(i, searchPattern)
    }
  }
  if (insideVariable) {
    res.add(VariableInfo(TextRange.create(startOffset, searchPattern.length), sb.toString()))
  }
  return res
}

private fun getStartOffset(i: Int, searchPattern: String) = if (i > 0 && searchPattern[i - 1] == '$') i - 1 else i

private fun variableStart(index: Int, searchPattern: String) =
  (index == 0 || !searchPattern[index-1].isLetter()) && searchPattern[index] == '_' && index < searchPattern.length - 1 && Character.isJavaIdentifierPart(searchPattern[index + 1])

data class VariableInfo(val range: Segment, val name: String)

fun transformToStringCriteria(project: Project, template: TemplateRawData, usedTemplatesNames: MutableSet<String>): String {
  usedTemplatesNames += template.ignoredVariables
  return wrapVariablesNames(template.pattern, {name ->
    if (template.ignoredVariables.contains(name)) return@wrapVariablesNames "_"
    val variable = template.variables.find { it.name == name }
    if (variable != null && variable.target) {
      return@wrapVariablesNames "'"
    }
    "'_" }, { transformVariableToStringCriteria(usedTemplatesNames, it, template, project) })
}

private val ConstraintRawData.target: Boolean
  get() = constraints.getBoolean(TARGET) ?: false

fun transformVariableToStringCriteria(usedTemplatesNames: MutableSet<String>, name: String, template: TemplateRawData, project: Project): String {
  if (!usedTemplatesNames.add(name)) return ""
  val variable = template.variables.find { it.name == name } ?: return ""
  val constraints = variable.constraints
  if (constraints.isEmpty()) return ""
  val sb = StringBuilder()
  if (constraints.containsKey(MIN_COUNT.docName)) {
    sb.append("{")
    sb.append(constraints[MIN_COUNT.docName])
    sb.append(",")
    if (constraints.containsKey(MAX_COUNT.docName)) {
      if (constraints[MAX_COUNT.docName] != "inf") {
        sb.append(constraints[MAX_COUNT.docName])
      }
    }
    sb.append("}")
  }
  var colonAdded = false
  if (constraints.containsKey(REGEXP.docName)) {
    colonAdded = addStarter(colonAdded, sb)
    sb.append("regex( ")
    sb.append(constraints[REGEXP.docName])
    sb.append(" )")
  }

  if (constraints.containsKey(TYPE.docName)) {
    colonAdded = addStarter(colonAdded, sb)
    sb.append("exprtype( ")
    sb.append(constraints[TYPE.docName])
    sb.append(" )")
  }

  if (constraints.containsKey(REFERENCE_CONSTRAINT_NAME.docName)) {
    addStarter(colonAdded, sb)
    sb.append("ref( ")
    sb.append(getStringCriteria(project, constraints[REFERENCE_CONSTRAINT_NAME.docName], usedTemplatesNames))
    sb.append(" )")
  }
  else if (constraints.containsKey(REFERENCE_CONSTRAINT.docName)) {
    addStarter(colonAdded, sb)
    sb.append("ref( ")
    sb.append(constraints[TYPE.docName])
    sb.append(" )")
  }
  if (colonAdded) {
    sb.append("]")
  }
  return sb.toString()
}

fun addStarter(colonAdded: Boolean, sb: StringBuilder): Boolean {
  if (!colonAdded) {
    sb.append(":[")
    return true
  } else {
    sb.append(" && ")
    return false
  }
}

fun wrapVariablesNames(searchPattern: String, prefix: (name: String) -> String, suffix: (name: String) -> String): String {
  val variables: List<VariableInfo> = collectVariables(searchPattern)
  if (variables.isEmpty()) return searchPattern
  val sb = StringBuilder()
  var lastOffset = 0
  for (variable in variables) {
    sb.append(searchPattern.substring(lastOffset, variable.range.startOffset))
    val name = variable.name
    sb.append(prefix(name))
    sb.append(name)
    sb.append(suffix(name))
    lastOffset = variable.range.endOffset
  }
  val lastEndOffset = variables.last().range.endOffset
  if (lastEndOffset < searchPattern.length) {
    sb.append(searchPattern.substring(lastEndOffset))
  }
  return sb.toString()
}
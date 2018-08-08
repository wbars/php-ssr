package com.jetbrains.php.ssr.dsl.util

import com.intellij.openapi.util.Segment
import com.intellij.openapi.util.TextRange

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
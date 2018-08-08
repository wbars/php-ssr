package com.jetbrains.php.ssr.dsl.completion

import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.PsiNamePatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.ssr.dsl.entities.ConstraintName
import com.jetbrains.php.ssr.dsl.entities.CustomDocTag

object Patterns {
  private fun getDocTagNamedPattern(vararg names: String): PsiNamePatternCondition<PsiElement> {
    return object : PsiNamePatternCondition<PsiElement>("withName", StandardPatterns.string().oneOf(*names)) {
      override fun getPropertyValue(o: Any): String? {
        return (o as? PhpDocTag)?.name
      }
    }
  }

  val variableTag = docTagWithName("@variable")

  fun docTagWithName(name: String) = psiElement(PhpDocTag::class.java).with(
    getDocTagNamedPattern(name))
  val variableAttributes = psiElement(PhpDocElementTypes.phpDocAttributeList).withParent(
    variableTag)

  fun insideValueOfAttribute(constraintName: ConstraintName): PsiElementPattern.Capture<StringLiteralExpression> {
    return psiElement(StringLiteralExpression::class.java)
      .afterLeaf(
        psiElement().withText(StandardPatterns.string().oneOf("=", "=!"))
          .afterLeaf(psiElement(PhpDocTokenTypes.DOC_IDENTIFIER).withText(constraintName.docName))
      )
      .withParent(variableAttributes)
  }

  val firstChild = object : PatternCondition<JsonStringLiteral>("firstChild") {
    override fun accepts(literal: JsonStringLiteral, p1: ProcessingContext?) = literal.parent.firstChild == literal
  }

  fun<T : PsiElement> not(patternCondition: PatternCondition<T>) = object : PatternCondition<T>("not_"+patternCondition.debugMethodName) {
    override fun accepts(p0: T, p1: ProcessingContext?): Boolean = !patternCondition.accepts(p0, p1)
  }

  fun docTagValue(docTag: CustomDocTag): PsiElementPattern.Capture<PsiElement> = psiElement(PhpDocTokenTypes.DOC_STRING).withSuperParent(
    2, psiElement(PhpDocElementTypes.phpDocAttributeList)
    .withParent(Patterns.docTagWithName(docTag.displayName))
  )
}
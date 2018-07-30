package com.jetbrains.php.ssr.dsl.codeinsight

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.PhpCallbackReferenceBase
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes.DOC_IDENTIFIER
import com.jetbrains.php.ssr.dsl.completion.Patterns
import com.jetbrains.php.ssr.dsl.completion.getValueContents
import com.jetbrains.php.ssr.dsl.indexing.getSearchConstraints

class ConstrainNameReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registar: PsiReferenceRegistrar) {
    registar.registerReferenceProvider(psiElement(DOC_IDENTIFIER).withSuperParent(2, Patterns.variableTag),
                                       ConstraintNameReferenceProvider())
  }
}

class ConstraintNameReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    return arrayOf(object : PhpCallbackReferenceBase(element) {
      override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val find = getSearchConstraints(myElement.project)?.find { it.getValueContents() == element.text }
                   ?: return ResolveResult.EMPTY_ARRAY
        return PsiElementResolveResult.createResults(find)
      }

      override fun getRangeInElement(): TextRange {
        return TextRange.create(0, myElement.textLength)
      }
    })
  }

}

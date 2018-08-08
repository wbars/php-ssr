package com.jetbrains.php.ssr.dsl.codeinsight

import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.PhpCallbackReferenceBase
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.ssr.dsl.completion.Patterns.envSettingTemplateName
import com.jetbrains.php.ssr.dsl.entities.CustomDocTag
import com.jetbrains.php.ssr.dsl.indexing.TemplateIndex
import com.jetbrains.php.ssr.dsl.indexing.getTemplateDocs
import com.jetbrains.php.ssr.dsl.indexing.templateName
import com.jetbrains.php.ssr.dsl.marker.findTagByName
import com.jetbrains.php.ssr.dsl.marker.getAttributesList
import com.jetbrains.php.ssr.dsl.marker.getChildByCondition

class EnvironmentSettingsTemplateNameReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registar: PsiReferenceRegistrar) {
    registar.registerReferenceProvider(envSettingTemplateName, TemplateNameReferenceProvider())
  }
}

class TemplateNameReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    return arrayOf(object : PhpCallbackReferenceBase(element) {
      override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (element is JsonStringLiteral) {
          val name = element.value
          return PsiElementResolveResult.createResults(TemplateIndex.getTemplateFiles(element.project, name).mapNotNull {
            getTemplateDocs(
              PsiManager.getInstance(element.project).findFile(it)).find { phpDoc -> phpDoc.templateName() == name }?.findTagByName(
              CustomDocTag.SSR_TEMPLATE.displayName)?.getAttributesList()?.getChildByCondition(
              StringLiteralExpression.INSTANCEOF)
          })
        }
        return PsiElementResolveResult.EMPTY_ARRAY
      }

      override fun getRangeInElement(): TextRange {
        return TextRange.create(1, myElement.textLength-1)
      }
    })
  }

}

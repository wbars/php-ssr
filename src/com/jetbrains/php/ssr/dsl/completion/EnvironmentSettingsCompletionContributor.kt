package com.jetbrains.php.ssr.dsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.ssr.dsl.completion.Patterns.firstChild
import com.jetbrains.php.ssr.dsl.completion.Patterns.not
import com.jetbrains.php.ssr.dsl.entities.CustomDocTag
import com.jetbrains.php.ssr.dsl.entities.SearchScope
import com.jetbrains.php.ssr.dsl.entities.Severity
import com.jetbrains.php.ssr.dsl.indexing.TemplateIndex

class EnvironmentSettingsCompletionContributor: CompletionContributor() {
  init {
    extend(CompletionType.BASIC, psiElement().withParent(psiElement(JsonStringLiteral::class.java).with(firstChild).withSuperParent(3, PsiFile::class.java)), TemplateNamesProvider())
    extend(CompletionType.BASIC, psiElement().withParent(psiElement(JsonStringLiteral::class.java).withParent(JsonProperty::class.java).withSuperParent(3, JsonProperty::class.java).with(firstChild)), EnvironmentSettingsNamesProvider())
    extend(CompletionType.BASIC, psiElement().withParent(psiElement(JsonStringLiteral::class.java).withParent(JsonProperty::class.java).with(not(firstChild))), EnvironmentSettingsValuesProvider())
  }
}

class EnvironmentSettingsValuesProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, p1: ProcessingContext?, result: CompletionResultSet) {
    val settingName = PhpPsiUtil.getPrevSiblingByCondition<JsonStringLiteral>(parameters.position.parent) { it is JsonStringLiteral } ?: return
    when (settingName.value.toUpperCase()) {
      CustomDocTag.SCOPE.name -> addStrings(SearchScope.names().map { it.toLowerCase() }, result)
      CustomDocTag.SEVERITY.name -> addStrings(Severity.values().map { it.name.toLowerCase() }, result)
    }
  }
}

class EnvironmentSettingsNamesProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(p0: CompletionParameters, p1: ProcessingContext?, result: CompletionResultSet) {
    addStrings(listOf(CustomDocTag.SCOPE.name.toLowerCase(), CustomDocTag.SEVERITY.name.toLowerCase()), result)
  }
}

class TemplateNamesProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
    val templateNames = parameters.originalFile.getTemplateNames()
    addStrings(TemplateIndex.getAllTemplateNames(parameters.position.project).filterNot { it in templateNames }, result)
  }
}

fun addStrings(strings: Collection<String>, result: CompletionResultSet) {
  strings.distinct().forEach {result.addElement(LookupElementBuilder.create(it))}
}

private fun PsiFile.getTemplateNames(): Collection<String> {
  val root = PsiTreeUtil.findChildOfType(this, JsonObject::class.java) ?: return emptySet()
  return root.propertyList.map { it.name }.toSet()
}

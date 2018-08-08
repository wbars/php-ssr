package com.jetbrains.php.ssr.dsl.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.dupLocator.iterators.CountingNodeIterator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.structuralsearch.Matcher
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.iterators.SsrFilteringNodeIterator
import com.intellij.structuralsearch.plugin.ui.Configuration
import com.jetbrains.php.lang.inspections.PhpInspection
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import com.jetbrains.php.ssr.dsl.entities.SearchScope
import com.jetbrains.php.ssr.dsl.entities.Severity
import com.jetbrains.php.ssr.dsl.indexing.TemplateEnvironmentSetting
import com.jetbrains.php.ssr.dsl.indexing.TemplateIndex
import com.jetbrains.php.ssr.dsl.indexing.TemplateRawData
import com.jetbrains.php.ssr.dsl.indexing.TemplatesEnvironmentIndex
import com.jetbrains.php.ssr.dsl.marker.buildConfiguration

class StructuralSearchInspection : PhpInspection() {
  override fun buildVisitor(holder: ProblemsHolder, onTheFly: Boolean): PsiElementVisitor {
    return object : PhpElementVisitor() {
      val matcher = Matcher(holder.manager.project)

      override fun visitElement(element: PsiElement?) {
        if (element == null) return
        val settings = TemplatesEnvironmentIndex.getAllTemplatesSettings(holder.manager.project)
        val excludedTemplatesNames = settings.filter { it.severity == Severity.OFF }.map { it.templateName }.toSet()
        val configurationsRawData = getIncludedTemplateNames(element.project, excludedTemplatesNames).mapNotNull {
          TemplateIndex.findTemplateRawData(element.project, it)
        }
        val configurations = configurationsRawData.map { it.buildConfiguration(element.project) }
        val cache: Map<Configuration, MatchContext> = mutableMapOf()
        try {
          matcher.precompileOptions(configurations, cache)
        } catch (ignored: Exception) {
          return
        }
        val matchedNodes = SsrFilteringNodeIterator(element)
        for (configuration in configurations) {
          val configurationSetting = settings.find { it.templateName.toUpperCase() == configuration.name.toUpperCase() }
          val scope: SearchScope? = getSearchScope(configurationSetting, configurationsRawData, configuration.name)
          if (scope != null && !scope.createScope(element.project).contains(element.containingFile.virtualFile)) continue
          val severity: Severity? = getSeverity(configurationSetting, configurationsRawData, configuration.name)
          if (severity == Severity.OFF) continue
          val context = cache[configuration] ?: continue
          if (Matcher.checkIfShouldAttemptToMatch(context, matchedNodes)) {
            matcher.processMatchesInElement(context, configuration,
                                            CountingNodeIterator(context.pattern.nodeCount, matchedNodes)) { matchResult, c ->
              if (severity != null && severity != Severity.DEFAULT) {
                holder.registerProblem(matchResult.match, c.name, severity.highlightType)
              }
              else {
                holder.registerProblem(matchResult.match, c.name)
              }
              true
            }
            matchedNodes.reset()
          }
        }
      }
    }
  }
}

private fun getSeverity(configurationSetting: TemplateEnvironmentSetting?,
                        configurationsRawData: List<TemplateRawData>,
                        name: String): Severity? {
  val severity = configurationSetting?.severity
  if (severity == null || severity == Severity.DEFAULT) {
    return configurationsRawData.find { it.name == name }?.severity ?: severity
  }
  return severity
}

private fun getSearchScope(configurationSetting: TemplateEnvironmentSetting?,
                           configurationsRawData: Collection<TemplateRawData>,
                           name: String): SearchScope? {
  val scope = configurationSetting?.scope
  if (scope == null || scope == SearchScope.PROJECT) {
    return configurationsRawData.find { it.name == name }?.scope ?: scope
  }
  return scope;
}

private val Severity.highlightType: ProblemHighlightType
  get() = when (this) {
    Severity.ERROR -> ProblemHighlightType.GENERIC_ERROR
    Severity.WARNING -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    Severity.WEAK_WARNING -> ProblemHighlightType.WEAK_WARNING
    else -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
  }

fun getIncludedTemplateNames(project: Project, templateNames: Collection<String>) = TemplateIndex.getAllTemplateNames(
  project).filter { !templateNames.contains(it) }

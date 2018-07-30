package com.jetbrains.php.ssr.dsl.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.dupLocator.iterators.CountingNodeIterator
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.structuralsearch.MatchResult
import com.intellij.structuralsearch.Matcher
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.iterators.SsrFilteringNodeIterator
import com.intellij.structuralsearch.plugin.ui.Configuration
import com.intellij.util.PairProcessor
import com.jetbrains.php.lang.inspections.PhpInspection
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import com.jetbrains.php.ssr.dsl.indexing.TemplateIndex
import com.jetbrains.php.ssr.dsl.marker.buildConfiguration

class StructuralSearchInspection: PhpInspection() {
  override fun buildVisitor(holder: ProblemsHolder, onTheFly: Boolean): PsiElementVisitor {
    return object: PhpElementVisitor() {
      val matcher = Matcher(holder.manager.project)
      val processor : PairProcessor<MatchResult, Configuration> = PairProcessor { matchResult, configuration ->
        holder.registerProblem(matchResult.match, configuration.name)
        true
      }

      override fun visitElement(element: PsiElement?) {
        if (element == null) return
        val configurations = TemplateIndex.getAllTemplateNames(element.project).mapNotNull {
          TemplateIndex.findTemplateRawData(element.project, it)?.buildConfiguration(element.project)
        }
        val cache: Map<Configuration, MatchContext> = mutableMapOf()
        matcher.precompileOptions(configurations, cache)
        val matchedNodes = SsrFilteringNodeIterator(element)
        for (configuration in configurations) {
          val context = cache[configuration]?:continue
          if (Matcher.checkIfShouldAttemptToMatch(context, matchedNodes)) {
            matcher.processMatchesInElement(context, configuration, CountingNodeIterator(context.pattern.nodeCount, matchedNodes), processor)
            matchedNodes.reset()
          }
        }
      }
    }
  }
}
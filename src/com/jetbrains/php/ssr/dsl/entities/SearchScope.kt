package com.jetbrains.php.ssr.dsl.entities

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.search.SearchScope

enum class SearchScope {
  PROJECT {
    override fun createScope(project: Project): SearchScope = GlobalSearchScope.projectScope(project)
  },
  TEST {
    override fun createScope(project: Project): SearchScope = GlobalSearchScopesCore.projectTestScope(project)
  },
  OPEN {
    override fun createScope(project: Project): SearchScope = GlobalSearchScopes.openFilesScope(project)
  };

  abstract fun createScope(project: Project): SearchScope

  companion object {
    fun names(): Collection<String> = values().map { it.name }
  }
}
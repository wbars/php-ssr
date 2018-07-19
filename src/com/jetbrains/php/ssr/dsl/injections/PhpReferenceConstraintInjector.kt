package com.jetbrains.php.ssr.dsl.injections

import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.ssr.dsl.completion.Patterns.insideValueOfAttribute
import com.jetbrains.php.ssr.dsl.entities.ConstraintName.*
import org.intellij.plugins.intelliLang.inject.InjectorUtils

class PhpReferenceConstraintInjector : LanguageInjector {
  override fun getLanguagesToInject(host: PsiLanguageInjectionHost, places: InjectedLanguagePlaces) {
    if (host is StringLiteralExpression && insideValueOfAttribute(REFERENCE_CONSTRAINT).accepts(host)) {
      places.injectPhp(host, host.valueRange)
    }
  }
}

fun InjectedLanguagePlaces.injectPhp(host: PsiElement, range: TextRange) {
  addPlace(PhpLanguage.INSTANCE, range, "<?php ", "")
  InjectorUtils.putInjectedFileUserData(host, PhpLanguage.INSTANCE, InjectedLanguageUtil.FRANKENSTEIN_INJECTION, java.lang.Boolean.TRUE)
}
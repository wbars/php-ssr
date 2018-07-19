package com.jetbrains.php.ssr.dsl.injections

import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.ssr.dsl.entities.ConstraintName
import org.intellij.lang.regexp.RegExpLanguage
import com.jetbrains.php.ssr.dsl.completion.Patterns

class RegExpConstraintInjector : LanguageInjector {

  override fun getLanguagesToInject(host: PsiLanguageInjectionHost, places: InjectedLanguagePlaces) {
    if (host is StringLiteralExpression && Patterns.insideValueOfAttribute(
        ConstraintName.REGEXP).accepts(host)) {
      places.addPlace(RegExpLanguage.INSTANCE, host.valueRange, "", "")
    }
  }
}
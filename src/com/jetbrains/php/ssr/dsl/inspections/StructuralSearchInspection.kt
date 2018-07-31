package com.jetbrains.php.ssr.dsl.inspections

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.dupLocator.iterators.CountingNodeIterator
import com.intellij.ide.DataManager
import com.intellij.ide.util.ChooseElementsDialog
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.structuralsearch.MatchResult
import com.intellij.structuralsearch.Matcher
import com.intellij.structuralsearch.impl.matcher.MatchContext
import com.intellij.structuralsearch.impl.matcher.iterators.SsrFilteringNodeIterator
import com.intellij.structuralsearch.plugin.ui.Configuration
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.util.PairProcessor
import com.jetbrains.php.lang.inspections.PhpInspection
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import com.jetbrains.php.ssr.dsl.indexing.TemplateIndex
import com.jetbrains.php.ssr.dsl.marker.buildConfiguration
import org.jdom.Element
import java.awt.Component
import javax.swing.*

class StructuralSearchInspection : PhpInspection() {
  val myExcludedTemplatesNames: MutableList<String> = mutableListOf()

  override fun buildVisitor(holder: ProblemsHolder, onTheFly: Boolean): PsiElementVisitor {
    return object : PhpElementVisitor() {
      val matcher = Matcher(holder.manager.project)
      val processor: PairProcessor<MatchResult, Configuration> = PairProcessor { matchResult, configuration ->
        holder.registerProblem(matchResult.match, configuration.name)
        true
      }

      override fun visitElement(element: PsiElement?) {
        if (element == null) return
        val configurations = getIncludedTemplateNames(element.project, myExcludedTemplatesNames).mapNotNull {
          TemplateIndex.findTemplateRawData(element.project, it)?.buildConfiguration(element.project)
        }
        val cache: Map<Configuration, MatchContext> = mutableMapOf()
        matcher.precompileOptions(configurations, cache)
        val matchedNodes = SsrFilteringNodeIterator(element)
        for (configuration in configurations) {
          val context = cache[configuration] ?: continue
          if (Matcher.checkIfShouldAttemptToMatch(context, matchedNodes)) {
            matcher.processMatchesInElement(context, configuration, CountingNodeIterator(context.pattern.nodeCount, matchedNodes),
                                            processor)
            matchedNodes.reset()
          }
        }
      }
    }
  }

  override fun createOptionsPanel(): JComponent? {
    return ExcludedTemplateInspectionOptions(myExcludedTemplatesNames).component
  }

  override fun readSettings(node: Element) {
    myExcludedTemplatesNames.clear()
    node.children.forEach {
      if (it.name == "template") {
        myExcludedTemplatesNames.add(it.getAttributeValue("name"))
      }
    }
    super.readSettings(node)
  }

  override fun writeSettings(node: Element) {
    myExcludedTemplatesNames.forEach { node.addContent(Element("template").setAttribute("name", it)) }
    super.writeSettings(node)
  }
}

private val ExcludedTemplateInspectionOptions.component: JComponent
  get() {
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
    val table = ToolbarDecorator.createDecorator(myTemplatesNamesList)
      .setAddActionName("Exclude templates")
      .setAddAction {
        val project = panel.getProject() ?: return@setAddAction
        val chooser = ChooseTemplateDialog(panel, getIncludedTemplateNames(project, myTemplatesNames))
        chooser.show()
        myTemplatesNames.addAll(chooser.chosenElements)
        templatesChanged(project)
      }
      .setRemoveAction {
        myTemplatesNamesList.selectedValuesList.forEach { myTemplatesNames.remove(it) }
        templatesChanged(panel.getProject() ?: return@setRemoveAction)
      }
      .disableUpAction()
      .disableDownAction()
      .createPanel()
    panel.add(table)
    return panel
  }

private fun JPanel.getProject() = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(this))

private fun ExcludedTemplateInspectionOptions.templatesChanged(project: Project) {
  (myTemplatesNamesList.model as ExcludedTemplateInspectionOptions.MyModel).fireContentsChanged()
  DaemonCodeAnalyzer.getInstance(project).restart()
}

class ExcludedTemplateInspectionOptions(val myTemplatesNames: MutableList<String>) {
  val myTemplatesNamesList: JBList<String> = JBList(MyModel())

  init {
    myTemplatesNamesList.cellRenderer = object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(list: JList<*>?,
                                                value: Any?,
                                                index: Int,
                                                isSelected: Boolean,
                                                cellHasFocus: Boolean): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
        component.text = myTemplatesNames[index]
        return component
      }
    }
  }

  inner class MyModel : AbstractListModel<String>() {
    override fun getElementAt(index: Int): String = myTemplatesNames[index]

    override fun getSize() = myTemplatesNames.size

    fun fireContentsChanged() {
      fireContentsChanged(myTemplatesNamesList, -1, -1)
    }
  }

  inner class ChooseTemplateDialog(parent: JComponent, templates: List<String>) : ChooseElementsDialog<String>(parent, templates,
                                                                                                               "Choose template to exclude") {
    override fun getItemIcon(item: String?): Nothing? = null

    override fun getItemText(item: String?) = item ?: ""
  }
}

fun getIncludedTemplateNames(project: Project,
                             templateNames: Collection<String>) = TemplateIndex.getAllTemplateNames(
  project).filter { !templateNames.contains(it) }

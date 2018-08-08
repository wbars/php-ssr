package com.jetbrains.php.ssr.dsl.indexing

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.ssr.dsl.entities.*
import com.jetbrains.php.ssr.dsl.indexing.TemplateIndex.Companion.not
import com.jetbrains.php.ssr.dsl.marker.findTagByName
import com.jetbrains.php.ssr.dsl.marker.getStringValue
import com.jetbrains.php.ssr.dsl.marker.isSSRTemplateTag
import com.jetbrains.php.ssr.dsl.util.VariableInfo
import com.jetbrains.php.ssr.dsl.util.collectVariables
import java.io.DataInput
import java.io.DataOutput

class TemplateIndex : FileBasedIndexExtension<String, TemplateRawData>() {
  companion object {
    const val not = "=!"
    private val key = ID.create<String, TemplateRawData>("php.ssr.dsl.template")
    fun findTemplateRawData(project: Project, name: String): TemplateRawData? =
      FileBasedIndex.getInstance().getValues(key, name, GlobalSearchScope.allScope(project)).firstOrNull()

    fun getAllTemplateNames(project: Project): Collection<String> = FileBasedIndex.getInstance().getAllKeys(key, project)

    fun getTemplateFiles(project: Project, name: String): MutableCollection<VirtualFile> {
      return FileBasedIndex.getInstance().getContainingFiles(key, name, GlobalSearchScope.allScope(project))
    }
  }

  private val externalizer = TemplateRawDataKeyExternalizer()

  override fun getName(): ID<String, TemplateRawData> = key

  override fun getVersion(): Int = 1

  override fun dependsOnFileContent(): Boolean = true

  override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(PhpFileType.INSTANCE)

  override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

  override fun getValueExternalizer(): DataExternalizer<TemplateRawData> = externalizer

  override fun getIndexer(): DataIndexer<String, TemplateRawData, FileContent> {
    return DataIndexer { inputData ->
      val map = ContainerUtil.newHashMap<String, TemplateRawData>()
      for (docComment in getTemplateDocs(inputData.psiFile)) {
        val name = docComment.templateName() ?: continue
        val scopeName = docComment.findTagByName("@scope")?.getStringValue() ?: SearchScope.PROJECT.name
        val scope = if (SearchScope.names().contains(scopeName)) SearchScope.valueOf(scopeName) else SearchScope.PROJECT
        val severityName = docComment.findTagByName(CustomDocTag.SEVERITY.displayName)?.getStringValue() ?: Severity.DEFAULT.name
        val templateRawData = TemplateRawData(name, replaceUnderscoresWithDollars(docComment.getPattern()), scope, severityName.toSeverity())
        templateRawData.variables.addAll(docComment.parseVariables())
        map[name] = templateRawData
      }
      map
    }
  }
}

fun getTemplateDocs(psiFile: PsiFile?): List<PhpDocComment> {
  if (psiFile is PhpFile) {
    return PsiTreeUtil.findChildrenOfType(psiFile, PhpDocComment::class.java).filter { it.ssrDoc() }
  }
  return emptyList()
}

fun PhpDocComment.parseVariables(): List<ConstraintRawData> {
  val variables = mutableListOf<ConstraintRawData>()
  for (variable in findTagsByName("@variable")) {
    val attributes = variable.getAttributesList() ?: continue
    val constraints: MutableMap<String, String> = mutableMapOf()
    val inverses: MutableMap<String, Boolean> = mutableMapOf()
    collectConstraints(attributes, constraints, inverses)
    if (constraints[ConstraintName.NAME.docName] != null) {
      val constraintRawData = ConstraintRawData(constraints, inverses)
      variables.add(constraintRawData)
    }
  }
  return variables
}
fun PhpDocComment.templateName() =
  findTagByName(CustomDocTag.SSR_TEMPLATE.displayName)?.getStringValue()

private fun collectConstraints(attributes: PsiElement,
                               constraints: MutableMap<String, String>,
                               inverses: MutableMap<String, Boolean>) {
  var current = attributes.firstChild
  while (current != null) {
    val constraintName = PhpPsiUtil.getNextSiblingIgnoreWhitespace(current, true)
    if (!PhpPsiUtil.isOfType(constraintName, PhpDocTokenTypes.DOC_IDENTIFIER)) break
    val equals = PhpPsiUtil.getNextSiblingIgnoreWhitespace(constraintName!!, true) ?: break
    if (equals.text == "=" || equals.text == not) {
      val valueToken = PhpPsiUtil.getNextSiblingIgnoreWhitespace(equals, true) ?: break
      val value = (valueToken as? StringLiteralExpression)?.contents ?: valueToken.text
      constraints[constraintName.text] = value
      inverses[constraintName.text] = equals.text == not
      current = PhpPsiUtil.getNextSiblingIgnoreWhitespace(valueToken, true)
    }
    else {
      val value = equals.text.substring(if (equals.text.startsWith(not)) 2 else 1)
      constraints[constraintName.text] = value
      inverses[constraintName.text] = equals.text.startsWith(not)
      current = PhpPsiUtil.getNextSiblingIgnoreWhitespace(equals, true)
    }
  }
}

private fun PhpDocTag.getAttributesList() =
  PhpPsiUtil.getChildOfType(this, PhpDocElementTypes.phpDocAttributeList)

private fun PhpDocComment.findTagsByName(name: String) =
  PhpPsiUtil.getChildren<PhpDocTag>(this) { it is PhpDocTag && it.name == name }

fun PsiElement.ssrDoc(): Boolean {
  for (phpDocTag in PsiTreeUtil.findChildrenOfType(this, PhpDocTag::class.java)) {
    if (phpDocTag.firstChild.isSSRTemplateTag()) {
      return true
    }
  }
  return false
}

fun PhpDocComment.getPattern(): String {
  val nextDocConfiguration: PsiElement? = PhpPsiUtil.getNextSiblingByCondition(this) { it is PhpDocComment && it.ssrDoc() }
  val endOffset = nextDocConfiguration?.textOffset ?: containingFile.textLength
  return containingFile.text.substring(textRange.endOffset until endOffset).trim()
}

private fun replaceUnderscoresWithDollars(searchPattern: String): String {
  val variables: List<VariableInfo> = collectVariables(searchPattern)
  if (variables.isEmpty()) return searchPattern
  val sb = StringBuilder()
  var lastOffset = 0
  for (variable in variables) {
    sb.append(searchPattern.substring(lastOffset, variable.range.startOffset))
    sb.append("$" + variable.name + "$")
    lastOffset = variable.range.endOffset
  }
  val lastEndOffset = variables.last().range.endOffset
  if (lastEndOffset < searchPattern.length) {
    sb.append(searchPattern.substring(lastEndOffset))
  }
  return sb.toString()
}

data class TemplateRawData(val name: String, val pattern: String, val scope: SearchScope, val severity: Severity, val variables: MutableList<ConstraintRawData> = mutableListOf())
data class ConstraintRawData(val constraints: MutableMap<String, String> = mutableMapOf(),
                             val inverses: MutableMap<String, Boolean> = mutableMapOf())

class TemplateRawDataKeyExternalizer : DataExternalizer<TemplateRawData> {
  override fun save(out: DataOutput, data: TemplateRawData?) {
    if (data != null) {
      out.writeUTF(data.name)
      out.writeUTF(data.pattern)
      out.writeUTF(data.scope.name)
      out.writeUTF(data.severity.name)
      out.writeInt(data.variables.size)
      for (variable in data.variables) {
        out.writeInt(variable.constraints.size)
        for (constraint in variable.constraints) {
          out.writeUTF(constraint.key)
          out.writeUTF(constraint.value)
        }
        out.writeInt(variable.inverses.size)
        for (inverse in variable.inverses) {
          out.writeUTF(inverse.key)
          out.writeBoolean(inverse.value)
        }
      }

    }
  }

  override fun read(input: DataInput): TemplateRawData {
    val name = input.readUTF()
    val pattern = input.readUTF()
    val scope = SearchScope.valueOf(input.readUTF())
    val severity = input.readUTF().toSeverity()
    val result = TemplateRawData(name, pattern, scope, severity)
    val variablesSize = input.readInt()
    for (i in 0 until variablesSize) {
      val constraintRawData = ConstraintRawData()
      val constraintsSize = input.readInt()
      for (j in 0 until constraintsSize) {
        constraintRawData.constraints[input.readUTF()] = input.readUTF()
      }

      val inversesSize = input.readInt()
      for (j in 0 until inversesSize) {
        constraintRawData.inverses[input.readUTF()] = input.readBoolean()
      }
      result.variables.add(constraintRawData)
    }
    return result
  }
}

fun getSearchConstraints(project: Project): List<Field>? {
  val index = PhpIndex.getInstance(project)
  return index.getClassesByFQN("\\StructuralSearchConstraints").firstOrNull()?.ownFields?.filter { it.isConstant }
}
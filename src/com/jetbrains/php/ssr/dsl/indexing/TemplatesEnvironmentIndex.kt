package com.jetbrains.php.ssr.dsl.indexing

import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.VoidDataExternalizer
import com.jetbrains.php.ssr.dsl.entities.SearchScope
import com.jetbrains.php.ssr.dsl.entities.Severity
import com.jetbrains.php.ssr.dsl.entities.toSeverity
import gnu.trove.THashMap
import java.io.DataInput
import java.io.DataOutput

class TemplatesEnvironmentIndex : FileBasedIndexExtension<TemplateEnvironmentSetting, Void>() {
  companion object {
    private val key = ID.create<TemplateEnvironmentSetting, Void>("php.ssr.dsl.template.env.setting")
    fun getAllTemplatesSettings(project: Project): Collection<TemplateEnvironmentSetting> {
      val index = FileBasedIndex.getInstance()
      return index.getAllKeys(key, project).filterNot {
        index.getContainingFiles(key, it, GlobalSearchScope.allScope(project)).isEmpty()
      }
    }
  }

  override fun getVersion(): Int = 1

  override fun getName(): ID<TemplateEnvironmentSetting, Void> = key

  override fun getValueExternalizer(): DataExternalizer<Void> = VoidDataExternalizer.INSTANCE

  override fun dependsOnFileContent(): Boolean = true

  override fun getInputFilter(): FileBasedIndex.InputFilter = TemplatesEnvironmentInputFilter.INSTANCE

  override fun getKeyDescriptor(): KeyDescriptor<TemplateEnvironmentSetting> = TemplateEnvironmentSettingDescriptor.INSTANCE

  override fun getIndexer(): DataIndexer<TemplateEnvironmentSetting, Void, FileContent> {
    return DataIndexer {
      val file = it.psiFile as? JsonFile ?: return@DataIndexer emptyMap()
      val root = file.topLevelValue as? JsonObject ?: return@DataIndexer emptyMap()
      getSettings(root)
    }
  }

  private fun getSettings(root: JsonObject): THashMap<TemplateEnvironmentSetting, Void> {
    val result = THashMap<TemplateEnvironmentSetting, Void>()
    for (property in root.propertyList) {
      val value = property.value as? JsonObject ?: continue
      val readEnvVariables = readEnvVariables(property.name, value) ?: continue
      result[readEnvVariables] = null
    }
    return result
  }

  private fun readEnvVariables(name: String, value: JsonObject?): TemplateEnvironmentSetting? {
    if (value == null) return null
    val severity = value.findPropertyValue("severity") ?: Severity.DEFAULT.name
    val scope = value.findPropertyValue("scope") ?: ""
    return TemplateEnvironmentSetting(name, severity, scope)
  }

  private fun JsonObject.findPropertyValue(s: String) = (findProperty(s)?.value as? JsonStringLiteral)?.value?.toUpperCase()
}

data class TemplateEnvironmentSetting(val templateName: String, val severity: Severity, val scope: SearchScope?) {
  constructor(templateName: String, severity: String, scope: String) : this(templateName, severity.toSeverity(), scope.toScope())
}

private fun String.toScope() = SearchScope.values().find { it.name.toUpperCase() == this.toUpperCase() }

class TemplateEnvironmentSettingDescriptor : KeyDescriptor<TemplateEnvironmentSetting> {
  companion object {
    val INSTANCE = TemplateEnvironmentSettingDescriptor()
  }

  override fun getHashCode(p0: TemplateEnvironmentSetting?) = p0!!.hashCode()

  override fun isEqual(p0: TemplateEnvironmentSetting?, p1: TemplateEnvironmentSetting?) = p0 == p1

  override fun save(output: DataOutput, setting: TemplateEnvironmentSetting?) {
    if (setting != null) {
      output.writeUTF(setting.templateName)
      output.writeUTF(setting.severity.name)
      output.writeUTF(setting.scope?.name?:"")
    }
  }

  override fun read(input: DataInput): TemplateEnvironmentSetting {
    val templateName = input.readUTF()
    val severity = input.readUTF()
    val scope = input.readUTF()
    return TemplateEnvironmentSetting(templateName, severity, scope)
  }
}

class TemplatesEnvironmentInputFilter : DefaultFileTypeSpecificInputFilter(JsonFileType.INSTANCE) {
  companion object {
    val INSTANCE = TemplatesEnvironmentInputFilter()
  }

  private val name = "php-ssr-templates-settings.env.json"

  override fun acceptInput(file: VirtualFile): Boolean {
    return super.acceptInput(file) && file.name == name
  }
}
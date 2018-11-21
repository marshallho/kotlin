/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.dsl

import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainerWithPresets
import java.io.File

fun main() {
    generateKotlinTargetContainerWithPresetFunctionsInterface()
}

private val parentInterface = KotlinTargetsContainerWithPresets::class

private val presetsProperty = KotlinTargetsContainerWithPresets::presets.name

private fun generateKotlinTargetContainerWithPresetFunctionsInterface() {
    // Generate KotlinMutliplatformExtension subclass with member functions for the presets:
    val functions = allPresetEntries.map {
        generatePresetFunctions(it, presetsProperty, "configureOrCreate")
    }

    val parentInterfaceName =
        typeName(parentInterface.java.canonicalName)

    val className =
        typeName("org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions")

    val imports = allPresetEntries
        .flatMap { it.typeNames() }
        .plus(parentInterfaceName)
        .plus(typeName("org.gradle.util.ConfigureUtil"))
        .plus(typeName("groovy.lang.Closure"))
        .filter { it.packageName() != className.packageName() }
        .flatMap { it.collectFqNames() }
        .toSortedSet()
        .joinToString("\n") { "import $it" }

    val generatedCodeWarning = "// DO NOT EDIT MANUALLY! Generated by ${object {}.javaClass.enclosingClass.name}"

    val code = listOf(
        "package ${className.packageName()}",
        imports,
        generatedCodeWarning,
        "interface ${className.renderShort()} : ${parentInterfaceName.renderShort()} {",
        functions.joinToString("\n\n") { it.indented(4) },
        "}"
    ).joinToString("\n\n")

    val outputSourceRoot = System.getProperties()["org.jetbrains.kotlin.generators.gradle.dsl.outputSourceRoot"]
    val targetFile = File("$outputSourceRoot/${className.fqName.replace(".", "/")}.kt")
    targetFile.writeText(code)
}

private fun generatePresetFunctions(
    presetEntry: KotlinPresetEntry,
    getPresetsExpression: String,
    configureOrCreateFunctionName: String
): String {
    val presetName = presetEntry.presetName
    return """
    fun $presetName(
        name: String = "$presetName",
        configure: ${presetEntry.targetType.renderShort()}.() -> Unit = { }
    ): ${presetEntry.targetType.renderShort()} =
        $configureOrCreateFunctionName(
            name,
            $getPresetsExpression.getByName("$presetName") as ${presetEntry.presetType.renderErased()},
            configure
        )

    fun $presetName() = $presetName("$presetName") { }
    fun $presetName(name: String) = $presetName(name) { }
    fun $presetName(name: String, configure: Closure<*>) = $presetName(name) { ConfigureUtil.configure(configure, this) }
    fun $presetName(configure: Closure<*>) = $presetName { ConfigureUtil.configure(configure, this) }
""".trimIndent()
}

private fun String.indented(nSpaces: Int = 4): String {
    val spaces = String(CharArray(nSpaces) { ' ' })
    return lines().joinToString("\n") { "$spaces$it"  }
}
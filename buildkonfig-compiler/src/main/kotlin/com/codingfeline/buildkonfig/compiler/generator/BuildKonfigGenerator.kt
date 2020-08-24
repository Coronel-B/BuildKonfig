package com.codingfeline.buildkonfig.compiler.generator

import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.Logger
import com.codingfeline.buildkonfig.compiler.TargetConfigFile
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

abstract class BuildKonfigGenerator(
    val file: TargetConfigFile,
    val objectModifiers: Array<KModifier>,
    val propertyModifiers: Array<KModifier>,
    val logger: Logger
) {

    fun generateType(objectName: String): TypeSpec {
        val obj = TypeSpec.objectBuilder(objectName)
            .addModifiers(*objectModifiers)

        val props = file.config.fieldSpecs.values
            .map { generateProp(it) }

        obj.addProperties(props)

        return obj.build()
    }

    abstract fun generateProp(fieldSpec: FieldSpec): PropertySpec

    companion object {
        /**
         * Generate common object
         */
        fun ofCommonObject(file: TargetConfigFile, exposeObject: Boolean, logger: Logger): BuildKonfigGenerator {
            val objectModifiers = arrayOf(getVisibilityModifier(exposeObject))
            return object : BuildKonfigGenerator(
                file = file,
                objectModifiers = objectModifiers,
                propertyModifiers = emptyArray(),
                logger = logger
            ) {
                override fun generateProp(fieldSpec: FieldSpec): PropertySpec {
                    return PropertySpec.builder(fieldSpec.name, fieldSpec.typeName)
                        .initializer(fieldSpec.template, fieldSpec.value)
                        .addModifiers(*propertyModifiers)
                        .build()
                }
            }
        }

        /**
         * Generate common `expect` object
         */
        fun ofCommon(file: TargetConfigFile, exposeObject: Boolean, logger: Logger): BuildKonfigGenerator {
            val objectModifiers = arrayOf(KModifier.EXPECT, getVisibilityModifier(exposeObject))
            return object : BuildKonfigGenerator(
                file = file,
                objectModifiers = objectModifiers,
                propertyModifiers = emptyArray(),
                logger = logger
            ) {
                override fun generateProp(fieldSpec: FieldSpec): PropertySpec {
                    return PropertySpec.builder(fieldSpec.name, fieldSpec.typeName)
                        .addModifiers(*propertyModifiers)
                        .build()
                }
            }
        }

        /**
         * Generate target `actual` object
         */
        fun ofTarget(file: TargetConfigFile, exposeObject: Boolean, logger: Logger): BuildKonfigGenerator {
            val objectModifiers = arrayOf(KModifier.ACTUAL, getVisibilityModifier(exposeObject))
            return object : BuildKonfigGenerator(
                file = file,
                objectModifiers = objectModifiers,
                propertyModifiers = arrayOf(KModifier.ACTUAL),
                logger = logger
            ) {
                override fun generateProp(fieldSpec: FieldSpec): PropertySpec {
                    val spec = PropertySpec.builder(fieldSpec.name, fieldSpec.typeName)
                        .initializer(fieldSpec.template, fieldSpec.value)

                    if (!fieldSpec.isTargetSpecific) {
                        spec.addModifiers(*propertyModifiers)
                    }

                    return spec.build()
                }
            }
        }
    }
}

private fun getVisibilityModifier(exposeObject: Boolean): KModifier =
    if (exposeObject) KModifier.PUBLIC else KModifier.INTERNAL

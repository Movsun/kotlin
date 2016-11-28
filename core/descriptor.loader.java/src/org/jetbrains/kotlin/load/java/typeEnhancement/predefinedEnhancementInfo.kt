/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java.typeEnhancement

import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.signatures
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType.BOOLEAN

class TypeEnhancementInfo(val map: Map<Int, JavaTypeQualifiers>) {
    constructor(vararg pairs: Pair<Int, JavaTypeQualifiers>) : this(mapOf(*pairs))
}

class PredefinedFunctionEnhancementInfo(
        val returnTypeInfo: TypeEnhancementInfo? = null,
        val parametersInfo: List<TypeEnhancementInfo?> = emptyList()
)

/** Type is always nullable: `T?` */
private val NULLABLE = JavaTypeQualifiers(NullabilityQualifier.NULLABLE, null, isNotNullTypeParameter = false)
/** Nullability depends on substitution, but the type is not platform: `T` */
private val NOT_PLATFORM = JavaTypeQualifiers(NullabilityQualifier.NOT_NULL, null, isNotNullTypeParameter = false)
/** Type is always non-nullable: `T & Any` */
private val NOT_NULLABLE = JavaTypeQualifiers(NullabilityQualifier.NOT_NULL, null, isNotNullTypeParameter = true)

val PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE = signatures {
    val JLObject = javaLang("Object")
    val JFPredicate = javaFunction("Predicate")
    val JFConsumer = javaFunction("Consumer")
    val JFBiFunction = javaFunction("BiFunction")
    val JFFunction = javaFunction("Function")
    val JUOptional = javaUtil("Optional")

    enhancement {
        forClass(javaUtil("Iterator")) {
            function("forEachRemaining") {
                parameter(JFConsumer, NOT_PLATFORM, NOT_PLATFORM)
            }
        }
        forClass(javaUtil("Collection")) {
            function("removeIf") {
                parameter(JFPredicate, NOT_PLATFORM, NOT_PLATFORM)
                returns(BOOLEAN)
            }
        }
        forClass(javaUtil("Map")) {
            function("merge") {
                parameter(JLObject, NOT_PLATFORM)
                parameter(JLObject, NOT_NULLABLE)
                parameter(JFBiFunction, NOT_PLATFORM, NOT_NULLABLE, NOT_NULLABLE, NULLABLE)
                returns(JLObject, NULLABLE)
            }
        }
        forClass(JFConsumer) {
            function("accept") {
                parameter(JLObject, NOT_PLATFORM)
            }
        }
        forClass(JFPredicate) {
            function("test") {
                parameter(JLObject, NOT_PLATFORM)
                returns(BOOLEAN)
            }
        }
        forClass(JFFunction) {
            function("apply") {
                parameter(JLObject, NOT_PLATFORM)
                returns(JLObject, NOT_PLATFORM)
            }
        }
        forClass(JFBiFunction) {
            function("apply") {
                parameter(JLObject, NOT_PLATFORM)
                parameter(JLObject, NOT_PLATFORM)
                returns(JLObject, NOT_PLATFORM)
            }
        }
        forClass(JUOptional) {
            function("empty") {
                returns(JUOptional, NOT_PLATFORM, NOT_NULLABLE)
            }
            function("of") {
                parameter(JLObject, NOT_NULLABLE)
                returns(JUOptional, NOT_PLATFORM, NOT_NULLABLE)
            }
            function("ofNullable") {
                parameter(JLObject, NULLABLE)
                returns(JUOptional, NOT_PLATFORM, NOT_NULLABLE)
            }
            function("get") {
                returns(JLObject, NOT_NULLABLE)
            }
            function("ifPresent") {
                parameter(JFConsumer, NOT_PLATFORM, NOT_NULLABLE)
            }
        }
    }
}


private inline fun enhancement(block: SignatureEnhancementBuilder.() -> Unit): Map<String, PredefinedFunctionEnhancementInfo>
        = SignatureEnhancementBuilder().apply(block).build()

private class SignatureEnhancementBuilder {
    private val signatures = mutableMapOf<String, PredefinedFunctionEnhancementInfo>()

    inline fun forClass(internalName: String, block: ClassEnhancementBuilder.() -> Unit) =
            ClassEnhancementBuilder(internalName).block()

    inner class ClassEnhancementBuilder(val className: String) {
        fun function(name: String, block: FunctionEnhancementBuilder.() -> Unit) {
            signatures += FunctionEnhancementBuilder(name).apply(block).build()
        }

        inner class FunctionEnhancementBuilder(val functionName: String) {
            private val parameters = mutableListOf<Pair<String, TypeEnhancementInfo?>>()
            private var returnType: Pair<String, TypeEnhancementInfo?> = "V" to null

            fun parameter(type: String, vararg pairs: Pair<Int, JavaTypeQualifiers>) {
                parameters += type to
                        if (pairs.isEmpty()) null else TypeEnhancementInfo(*pairs)
            }
            fun parameter(type: String, vararg qualifiers: JavaTypeQualifiers) {
                parameters += type to
                        if (qualifiers.isEmpty()) null else TypeEnhancementInfo(qualifiers.withIndex().associateBy({it.index}, {it.value}))
            }
            fun returns(type: String, vararg pairs: Pair<Int, JavaTypeQualifiers>) {
                returnType = type to TypeEnhancementInfo(*pairs)
            }
            fun returns(type: String, vararg qualifiers: JavaTypeQualifiers) {
                returnType = type to TypeEnhancementInfo(qualifiers.withIndex().associateBy({it.index}, {it.value}))
            }
            fun returns(type: JvmPrimitiveType) {
                returnType = type.desc to null
            }
            fun build() = with (SignatureBuildingComponents) {
                signature(className, jvmDescriptor(functionName, parameters.map { it.first }, returnType.first)) to
                        PredefinedFunctionEnhancementInfo(returnType.second, parameters.map { it.second })
            }
        }

    }

    fun build(): Map<String, PredefinedFunctionEnhancementInfo> = signatures
}



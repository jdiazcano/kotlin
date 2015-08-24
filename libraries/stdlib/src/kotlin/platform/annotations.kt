/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package kotlin.platform

import kotlin.annotation.AnnotationTarget.*

/**
 * Specifies the name for the target platform element (Java method, JavaScript function)
 * which is generated from this element.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/java-interop.html#handling-signature-clashes-with-platformname)
 * for more information.
 * @property name the name of the element.
 */
target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
public annotation(retention = AnnotationRetention.RUNTIME, mustBeDocumented = true) class platformName(public val name: String)

target(FUNCTION, PROPERTY, FIELD, PROPERTY_GETTER, PROPERTY_SETTER)
deprecated("Use kotlin.jvm.jvmStatic instead", ReplaceWith("kotlin.jvm.jvmStatic"))
public annotation(retention = AnnotationRetention.RUNTIME, mustBeDocumented = true) class platformStatic

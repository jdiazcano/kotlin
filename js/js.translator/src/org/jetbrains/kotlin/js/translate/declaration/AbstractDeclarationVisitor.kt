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

package org.jetbrains.kotlin.js.translate.declaration

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.expression.translateAndAliasParameters
import org.jetbrains.kotlin.js.translate.expression.translateFunction
import org.jetbrains.kotlin.js.translate.expression.wrapWithInlineMetadata
import org.jetbrains.kotlin.js.translate.general.TranslatorVisitor
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtensionProperty

abstract class AbstractDeclarationVisitor : TranslatorVisitor<Unit>()  {
    override fun emptyResult(context: TranslationContext) { }

    override fun visitClassOrObject(classOrObject: KtClassOrObject, context: TranslationContext) {
        ClassTranslator.translate(classOrObject, context)
        val descriptor = BindingUtils.getClassDescriptor(context.bindingContext(), classOrObject)
        context.export(descriptor)
    }

    override fun visitProperty(expression: KtProperty, context: TranslationContext) {
        val descriptor = BindingUtils.getPropertyDescriptor(context.bindingContext(), expression)
        if (descriptor.modality === Modality.ABSTRACT) return

        val propertyContext = context.newDeclaration(descriptor)

        val defaultTranslator = DefaultPropertyTranslator(descriptor, context, getBackingFieldReference(descriptor))
        val getter = descriptor.getter!!
        val getterExpr = if (expression.hasCustomGetter()) {
            translateFunction(getter, expression.getter!!, propertyContext)
        }
        else {
            val function = context.getFunctionObject(getter)
            defaultTranslator.generateDefaultGetterFunction(getter, function)
            function
        }

        val setterExpr = if (descriptor.isVar) {
            val setter = descriptor.setter!!
            if (expression.hasCustomSetter()) {
                translateFunction(setter, expression.setter!!, propertyContext)
            }
            else {
                val function = context.getFunctionObject(setter)
                defaultTranslator.generateDefaultSetterFunction(setter, function)
                function
            }
        }
        else {
            null
        }

        if (TranslationUtils.shouldAccessViaFunctions(descriptor) || descriptor.isExtensionProperty) {
            addFunction(descriptor.getter!!, getterExpr)
            descriptor.setter?.let { addFunction(it, setterExpr!!) }
        }
        else {
            addProperty(descriptor, getterExpr, setterExpr)
        }
    }

    override fun visitNamedFunction(expression: KtNamedFunction, context: TranslationContext) {
        val descriptor = BindingUtils.getFunctionDescriptor(context.bindingContext(), expression)
        val jsFunction = if (descriptor.modality != Modality.ABSTRACT) translateFunction(descriptor, expression, context) else null
        addFunction(descriptor, jsFunction)
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias, data: TranslationContext?) {}

    private fun translateFunction(
            descriptor: FunctionDescriptor,
            expression: KtDeclarationWithBody,
            context: TranslationContext
    ): JsExpression {
        val function = context.getFunctionObject(descriptor)
        var innerContext = context.newDeclaration(descriptor).translateAndAliasParameters(descriptor, function.parameters)

        if (descriptor.isSuspend) {
            if (descriptor.requiresStateMachineTransformation(context)) {
                function.fillCoroutineMetadata(context, descriptor, hasController = false, isLambda = false)
                innerContext = innerContext.innerContextWithAliased(descriptor, JsAstUtils.stateMachineReceiver())
            }
            else {
                val continuationRef = ReferenceTranslator.translateAsValueReference(
                        innerContext.continuationParameterDescriptor!!, innerContext)
                innerContext = innerContext.innerContextWithAliased(descriptor, continuationRef)
            }
        }

        if (!descriptor.isOverridable) {
            function.body.statements += FunctionBodyTranslator.setDefaultValueForArguments(descriptor, innerContext)
        }
        innerContext.translateFunction(expression, function)
        return innerContext.wrapWithInlineMetadata(function, descriptor)
    }

    protected abstract fun addFunction(
            descriptor: FunctionDescriptor,
            expression: JsExpression?
    )

    protected abstract fun addProperty(
            descriptor: PropertyDescriptor,
            getter: JsExpression,
            setter: JsExpression?
    )

    protected abstract fun getBackingFieldReference(descriptor: PropertyDescriptor): JsExpression
}
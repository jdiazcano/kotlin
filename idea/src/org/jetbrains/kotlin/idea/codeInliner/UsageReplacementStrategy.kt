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

package org.jetbrains.kotlin.idea.codeInliner

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.ui.GuiUtils
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

interface UsageReplacementStrategy {
    fun createReplacer(usage: KtSimpleNameExpression): (() -> KtElement?)?
}

private val LOG = Logger.getInstance(UsageReplacementStrategy::class.java)

fun UsageReplacementStrategy.replaceUsagesInWholeProject(
        targetPsiElement: PsiElement,
        progressTitle: String,
        commandName: String,
        postAction: () -> Unit = {}
) {
    val project = targetPsiElement.project
    ProgressManager.getInstance().run(
            object : Task.Modal(project, progressTitle, true) {
                override fun run(indicator: ProgressIndicator) {
                    val usages = runReadAction {
                        val searchScope = KotlinSourceFilterScope.projectSources(GlobalSearchScope.projectScope(project), project)
                        ReferencesSearch.search(targetPsiElement, searchScope)
                                .filterIsInstance<KtSimpleNameReference>()
                                .map { ref -> ref.expression }
                    }
                    this@replaceUsagesInWholeProject.replaceUsages(usages, targetPsiElement, project, commandName, postAction)
                }
            })
}

private fun UsageReplacementStrategy.replaceUsages(
        usages: Collection<KtSimpleNameExpression>,
        targetPsiElement: PsiElement,
        project: Project,
        commandName: String,
        postAction: () -> Unit
) {
    GuiUtils.invokeLaterIfNeeded({
        project.executeWriteCommand(commandName) {
            // we should delete imports later to not affect other usages
            val importsToDelete = arrayListOf<KtImportDirective>()
            val replacements = mutableListOf<KtElement>()

            var invalidUsagesFound = false
            for (usage in usages) {
                try {
                    if (!usage.isValid) {
                        invalidUsagesFound = true
                        continue
                    }

                    //TODO: keep the import if we don't know how to replace some of the usages
                    val importDirective = usage.getStrictParentOfType<KtImportDirective>()
                    if (importDirective != null) {
                        if (!importDirective.isAllUnder && importDirective.targetDescriptors().size == 1) {
                            importsToDelete.add(importDirective)
                        }
                        continue
                    }

                    val replacement = createReplacer(usage)?.invoke()
                    if (replacement != null) {
                        replacements += replacement
                    }
                }
                catch (e: Throwable) {
                    LOG.error(e)
                }
            }

            if (invalidUsagesFound && targetPsiElement is KtNamedDeclaration) {
                val name = targetPsiElement.name
                val targetDescriptor = targetPsiElement.descriptor
                if (name != null && targetDescriptor != null) {
                    for (replacement in replacements) {
                        replacement.forEachDescendantOfType<KtSimpleNameExpression> { usage ->
                            if (usage.isValid && usage.getReferencedName() == name) {
                                val context = usage.analyze(BodyResolveMode.PARTIAL)
                                if (targetDescriptor == context[BindingContext.REFERENCE_TARGET, usage]) {
                                    createReplacer(usage)?.invoke()
                                }
                            }
                        }
                    }
                }
            }

            importsToDelete.forEach { it.delete() }

            postAction()
        }
    }, ModalityState.NON_MODAL)
}

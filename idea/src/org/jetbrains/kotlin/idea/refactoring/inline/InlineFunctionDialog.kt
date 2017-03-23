/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.help.HelpManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.psi.util.PsiFormatUtilBase
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.inline.InlineMethodProcessor
import com.intellij.refactoring.inline.InlineOptionsDialog
import com.intellij.refactoring.inline.InlineOptionsWithSearchSettingsDialog

// NB: copied & J2K-ed from IDEA InlineMethodDialog
class InlineMethodDialog(
        project: Project,
        private val myMethod: PsiMethod,
        private val myReferenceElement: PsiJavaCodeReferenceElement?,
        private val myEditor: Editor,
        private val myAllowInlineThisOnly: Boolean
) : InlineOptionsWithSearchSettingsDialog(project, true, myMethod) {

    private var myOccurrencesNumber = -1

    init {
        myInvokedOnReference = myReferenceElement != null

        title = REFACTORING_NAME
        myOccurrencesNumber = InlineOptionsDialog.initOccurrencesNumber(myMethod)
        init()
    }

    override fun allowInlineAll(): Boolean {
        return true
    }

    override fun getNameLabelText(): String {
        val occurrencesString = if (myOccurrencesNumber > -1) " - " + myOccurrencesNumber + " occurrence" + (if (myOccurrencesNumber == 1) "" else "s") else ""
        val methodText = PsiFormatUtil.formatMethod(myMethod,
                                                    PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME or PsiFormatUtilBase.SHOW_PARAMETERS,
                                                    PsiFormatUtilBase.SHOW_TYPE)
        return RefactoringBundle.message("inline.method.method.label", methodText, occurrencesString)
    }

    override fun getBorderTitle(): String {
        return RefactoringBundle.message("inline.method.border.title")
    }

    override fun getInlineThisText(): String {
        return RefactoringBundle.message("this.invocation.only.and.keep.the.method")
    }

    override fun getInlineAllText(): String {
        return RefactoringBundle.message(if (myMethod.isWritable) "all.invocations.and.remove.the.method" else "all.invocations.in.project")
    }

    override fun getKeepTheDeclarationText(): String {
        if (myMethod.isWritable) return RefactoringBundle.message("all.invocations.keep.the.method")
        return super.getKeepTheDeclarationText()
    }

    override fun doAction() {
        super.doAction()
        invokeRefactoring(
                InlineMethodProcessor(project, myMethod, myReferenceElement, myEditor, isInlineThisOnly, isSearchInCommentsAndStrings,
                                      isSearchForTextOccurrences, !isKeepTheDeclaration))
        val settings = JavaRefactoringSettings.getInstance()
        if (myRbInlineThisOnly.isEnabled && myRbInlineAll.isEnabled) {
            settings.INLINE_METHOD_THIS = isInlineThisOnly
        }
    }

    override fun doHelpAction() {
        if (myMethod.isConstructor)
            HelpManager.getInstance().invokeHelp(HelpID.INLINE_CONSTRUCTOR)
        else
            HelpManager.getInstance().invokeHelp(HelpID.INLINE_METHOD)
    }

    override fun canInlineThisOnly(): Boolean {
        return false //InlineMethodHandler.checkRecursive(myMethod) || myAllowInlineThisOnly
    }

    override fun isInlineThis(): Boolean {
        return JavaRefactoringSettings.getInstance().INLINE_METHOD_THIS
    }

    override fun isSearchInCommentsAndStrings(): Boolean {
        return JavaRefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_METHOD
    }

    override fun saveSearchInCommentsAndStrings(searchInComments: Boolean) {
        JavaRefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_METHOD = searchInComments
    }

    override fun isSearchForTextOccurrences(): Boolean {
        return JavaRefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_METHOD
    }

    override fun saveSearchInTextOccurrences(searchInTextOccurrences: Boolean) {
        JavaRefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_METHOD = searchInTextOccurrences
    }

    companion object {
        val REFACTORING_NAME = RefactoringBundle.message("inline.method.title")
    }
}

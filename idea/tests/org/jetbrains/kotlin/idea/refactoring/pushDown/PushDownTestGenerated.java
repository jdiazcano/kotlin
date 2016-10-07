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

package org.jetbrains.kotlin.idea.refactoring.pushDown;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/testData/refactoring/pushDown")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class PushDownTestGenerated extends AbstractPushDownTest {
    @TestMetadata("accidentalOverrides.kt")
    public void testAccidentalOverrides() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/accidentalOverrides.kt");
        doTest(fileName);
    }

    public void testAllFilesPresentInPushDown() throws Exception {
        KotlinTestUtils.assertAllTestsPresentInSingleGeneratedClass(this.getClass(), new File("idea/testData/refactoring/pushDown"), Pattern.compile("^(.+)\\.kt$"));
    }

    @TestMetadata("clashingMembers.kt")
    public void testClashingMembers() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/clashingMembers.kt");
        doTest(fileName);
    }

    @TestMetadata("classToInterface.kt")
    public void testClassToInterface() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/classToInterface.kt");
        doTest(fileName);
    }

    @TestMetadata("conflictingSuperCall.kt")
    public void testConflictingSuperCall() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/conflictingSuperCall.kt");
        doTest(fileName);
    }

    @TestMetadata("dropVisibilityOnGeneratedOverride.kt")
    public void testDropVisibilityOnGeneratedOverride() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/dropVisibilityOnGeneratedOverride.kt");
        doTest(fileName);
    }

    @TestMetadata("finalClass.kt")
    public void testFinalClass() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/finalClass.kt");
        doTest(fileName);
    }

    @TestMetadata("implicitCompanionUsages.kt")
    public void testImplicitCompanionUsages() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/implicitCompanionUsages.kt");
        doTest(fileName);
    }

    @TestMetadata("kotlinToJava.kt")
    public void testKotlinToJava() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/kotlinToJava.kt");
        doTest(fileName);
    }

    @TestMetadata("liftPrivate.kt")
    public void testLiftPrivate() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/liftPrivate.kt");
        doTest(fileName);
    }

    @TestMetadata("noCaret.kt")
    public void testNoCaret() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/noCaret.kt");
        doTest(fileName);
    }

    @TestMetadata("objectDeclaration.kt")
    public void testObjectDeclaration() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/objectDeclaration.kt");
        doTest(fileName);
    }

    @TestMetadata("outsideOfClass.kt")
    public void testOutsideOfClass() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/outsideOfClass.kt");
        doTest(fileName);
    }

    @TestMetadata("pushClassMembers.kt")
    public void testPushClassMembers() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/pushClassMembers.kt");
        doTest(fileName);
    }

    @TestMetadata("pushClassMembersAndMakeAbstract.kt")
    public void testPushClassMembersAndMakeAbstract() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/pushClassMembersAndMakeAbstract.kt");
        doTest(fileName);
    }

    @TestMetadata("pushClassMembersWithGenerics.kt")
    public void testPushClassMembersWithGenerics() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/pushClassMembersWithGenerics.kt");
        doTest(fileName);
    }

    @TestMetadata("pushInterfaceMembers.kt")
    public void testPushInterfaceMembers() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/pushInterfaceMembers.kt");
        doTest(fileName);
    }

    @TestMetadata("pushInterfaceMembersAndMakeAbstract.kt")
    public void testPushInterfaceMembersAndMakeAbstract() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/pushInterfaceMembersAndMakeAbstract.kt");
        doTest(fileName);
    }

    @TestMetadata("pushMembersUsingPrivates.kt")
    public void testPushMembersUsingPrivates() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/pushMembersUsingPrivates.kt");
        doTest(fileName);
    }

    @TestMetadata("pushMembersWithExternalUsages.kt")
    public void testPushMembersWithExternalUsages() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/pushMembersWithExternalUsages.kt");
        doTest(fileName);
    }

    @TestMetadata("pushSuperInterfaces.kt")
    public void testPushSuperInterfaces() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/pushSuperInterfaces.kt");
        doTest(fileName);
    }

    @TestMetadata("pushSuperInterfacesWithGenerics.kt")
    public void testPushSuperInterfacesWithGenerics() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/refactoring/pushDown/pushSuperInterfacesWithGenerics.kt");
        doTest(fileName);
    }
}

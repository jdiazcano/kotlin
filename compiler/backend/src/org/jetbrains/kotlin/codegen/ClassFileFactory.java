/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.load.kotlin.PackageParts;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.serialization.jvm.JvmPackageTable;
import org.jetbrains.org.objectweb.asm.Type;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.getMappingFileName;

public class ClassFileFactory implements OutputFileCollection {
    private final GenerationState state;
    private final ClassBuilderFactory builderFactory;
    private final Map<String, OutAndSourceFileList> generators = new LinkedHashMap<String, OutAndSourceFileList>();

    private boolean isDone = false;

    private final Set<File> packagePartSourceFiles = new HashSet<File>();
    private final Map<String, PackageParts> partsGroupedByPackage = new LinkedHashMap<String, PackageParts>();

    public ClassFileFactory(@NotNull GenerationState state, @NotNull ClassBuilderFactory builderFactory) {
        this.state = state;
        this.builderFactory = builderFactory;
    }

    public GenerationState getGenerationState() {
        return state;
    }

    @NotNull
    public ClassBuilder newVisitor(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull Type asmType,
            @NotNull PsiFile sourceFile) {
        return newVisitor(origin, asmType, Collections.singletonList(sourceFile));
    }

    @NotNull
    public ClassBuilder newVisitor(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull Type asmType,
            @NotNull Collection<? extends PsiFile> sourceFiles
    ) {
        ClassBuilder answer = builderFactory.newClassBuilder(origin);
        generators.put(
                asmType.getInternalName() + ".class",
                new ClassBuilderAndSourceFileList(answer, toIoFilesIgnoringNonPhysical(sourceFiles))
        );
        return answer;
    }

    public void done() {
        if (!isDone) {
            isDone = true;
            writeModuleMappings();
        }
    }

    public void releaseGeneratedOutput() {
        generators.clear();
    }

    private void writeModuleMappings() {
        final JvmPackageTable.PackageTable.Builder builder = JvmPackageTable.PackageTable.newBuilder();
        String outputFilePath = getMappingFileName(state.getModuleName());

        for (PackageParts part : ClassFileUtilsKt.addCompiledPartsAndSort(partsGroupedByPackage.values(), state)) {
            part.addTo(builder);
        }

        if (builder.getPackagePartsCount() == 0) return;

        generators.put(outputFilePath, new OutAndSourceFileList(CollectionsKt.toList(packagePartSourceFiles)) {
            @Override
            public byte[] asBytes(ClassBuilderFactory factory) {
                return ClassFileUtilsKt.serializeToByteArray(builder);
            }

            @Override
            public String asText(ClassBuilderFactory factory) {
                try {
                    return new String(asBytes(factory), "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @NotNull
    @Override
    public List<OutputFile> asList() {
        done();
        return getCurrentOutput();
    }

    @NotNull
    public List<OutputFile> getCurrentOutput() {
        return ContainerUtil.map(generators.keySet(), new Function<String, OutputFile>() {
            @Override
            public OutputFile fun(String relativeClassFilePath) {
                return new OutputClassFile(relativeClassFilePath);
            }
        });
    }

    @Override
    @Nullable
    public OutputFile get(@NotNull String relativePath) {
        return generators.containsKey(relativePath) ? new OutputClassFile(relativePath) : null;
    }

    @NotNull
    @TestOnly
    public String createText() {
        StringBuilder answer = new StringBuilder();

        for (OutputFile file : asList()) {
            answer.append("@").append(file.getRelativePath()).append('\n');
            answer.append(file.asText());
        }

        return answer.toString();
    }

    @NotNull
    @TestOnly
    public Map<String, String> createTextForEachFile() {
        Map<String, String> answer = new LinkedHashMap<String, String>();
        for (OutputFile file : asList()) {
            answer.put(file.getRelativePath(), file.asText());
        }
        return answer;
    }

    @NotNull
    public PackageCodegen forPackage(@NotNull FqName fqName, @NotNull Collection<KtFile> files) {
        assert !isDone : "Already done!";
        registerPackagePartSourceFiles(files);
        return state.getCodegenFactory().createPackageCodegen(state, files, fqName, buildNewPackagePartRegistry(fqName));
    }

    @NotNull
    public MultifileClassCodegen forMultifileClass(@NotNull FqName facadeFqName, @NotNull Collection<KtFile> files) {
        assert !isDone : "Already done!";
        registerPackagePartSourceFiles(files);
        return state.getCodegenFactory().createMultifileClassCodegen(state, files, facadeFqName, buildNewPackagePartRegistry(facadeFqName.parent()));
    }

    private PackagePartRegistry buildNewPackagePartRegistry(@NotNull FqName packageFqName) {
        final String packageFqNameAsString = packageFqName.asString();
        return new PackagePartRegistry() {
            @Override
            public void addPart(@NotNull String partShortName, @Nullable String facadeShortName) {
                PackageParts packageParts = partsGroupedByPackage.get(packageFqNameAsString);
                if (packageParts == null) {
                    packageParts = new PackageParts(packageFqNameAsString);
                    partsGroupedByPackage.put(packageFqNameAsString, packageParts);
                }
                packageParts.addPart(partShortName, facadeShortName);
            }
        };
    }

    private void registerPackagePartSourceFiles(Collection<KtFile> files) {
        packagePartSourceFiles.addAll(toIoFilesIgnoringNonPhysical(PackagePartClassUtils.getFilesWithCallables(files)));
    }

    @NotNull
    private static List<File> toIoFilesIgnoringNonPhysical(@NotNull Collection<? extends PsiFile> psiFiles) {
        List<File> result = new ArrayList<File>(psiFiles.size());
        for (PsiFile psiFile : psiFiles) {
            VirtualFile virtualFile = psiFile.getVirtualFile();
            // We ignore non-physical files here, because this code is needed to tell the make what inputs affect which outputs
            // a non-physical file cannot be processed by make
            if (virtualFile != null) {
                result.add(new File(virtualFile.getPath()));
            }
        }
        return result;
    }

    private class OutputClassFile implements OutputFile {
        private final String relativeClassFilePath;

        public OutputClassFile(String relativeClassFilePath) {
            this.relativeClassFilePath = relativeClassFilePath;
        }

        @NotNull
        @Override
        public String getRelativePath() {
            return relativeClassFilePath;
        }

        @NotNull
        @Override
        public List<File> getSourceFiles() {
            OutAndSourceFileList pair = generators.get(relativeClassFilePath);
            if (pair == null) {
                throw new IllegalStateException("No record for binary file " + relativeClassFilePath);
            }

            return pair.sourceFiles;
        }

        @NotNull
        @Override
        public byte[] asByteArray() {
            try {
                return generators.get(relativeClassFilePath).asBytes(builderFactory);
            }
            catch (RuntimeException e) {
                throw new RuntimeException("Error generating class file " + this.toString() + ": " + e.getMessage(), e);
            }
        }

        @NotNull
        @Override
        public String asText() {
            try {
                return generators.get(relativeClassFilePath).asText(builderFactory);
            }
            catch (RuntimeException e) {
                throw new RuntimeException("Error generating class file " + this.toString() + ": " + e.getMessage(), e);
            }
        }

        @NotNull
        @Override
        public String toString() {
            return getRelativePath() + " (compiled from " + getSourceFiles() + ")";
        }
    }

    private static final class ClassBuilderAndSourceFileList extends OutAndSourceFileList {
        private final ClassBuilder classBuilder;

        private ClassBuilderAndSourceFileList(ClassBuilder classBuilder, List<File> sourceFiles) {
            super(sourceFiles);
            this.classBuilder = classBuilder;
        }

        @Override
        public byte[] asBytes(ClassBuilderFactory factory) {
            return factory.asBytes(classBuilder);
        }

        @Override
        public String asText(ClassBuilderFactory factory) {
            return factory.asText(classBuilder);
        }
    }

    private static abstract class OutAndSourceFileList {

        protected final List<File> sourceFiles;

        private OutAndSourceFileList(List<File> sourceFiles) {
            this.sourceFiles = sourceFiles;
        }

        public abstract byte[] asBytes(ClassBuilderFactory factory);

        public abstract String asText(ClassBuilderFactory factory);
    }

    public void removeClasses(Set<String> classNamesToRemove) {
        for (String classInternalName : classNamesToRemove) {
            generators.remove(classInternalName + ".class");
        }
    }

    @TestOnly
    public List<KtFile> getInputFiles() {
        return state.getFiles();
    }
}

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

package org.jetbrains.kotlin.js.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.PathUtil;
import com.intellij.util.SmartList;
import com.intellij.util.io.URLUtil;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration;
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor;
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil;
import org.jetbrains.kotlin.serialization.js.ModuleKind;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.utils.JsMetadataVersion;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils;

import java.io.File;
import java.util.*;

import static org.jetbrains.kotlin.config.CommonConfigurationKeysKt.getLanguageVersionSettings;
import static org.jetbrains.kotlin.utils.PathUtil.getKotlinPathsForDistDirectory;

public class JsConfig {
    public static final List<String> JS_STDLIB =
            Collections.singletonList(getKotlinPathsForDistDirectory().getJsStdLibJarPath().getAbsolutePath());

    public static final List<String> JS_KOTLIN_TEST =
            Collections.singletonList(getKotlinPathsForDistDirectory().getJsKotlinTestJarPath().getAbsolutePath());

    public static final String UNKNOWN_EXTERNAL_MODULE_NAME = "<unknown>";

    private final Project project;
    private final CompilerConfiguration configuration;
    private final LockBasedStorageManager storageManager = new LockBasedStorageManager();

    private final List<KotlinJavascriptMetadata> metadata = new SmartList<KotlinJavascriptMetadata>();

    @Nullable
    private List<JsModuleDescriptor<ModuleDescriptorImpl>> moduleDescriptors = null;

    private boolean initialized = false;

    public JsConfig(@NotNull Project project, @NotNull CompilerConfiguration configuration) {
        this.project = project;
        this.configuration = configuration;
    }

    @NotNull
    public CompilerConfiguration getConfiguration() {
        return configuration;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public String getModuleId() {
        return configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME);
    }

    @NotNull
    public ModuleKind getModuleKind() {
        return configuration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN);
    }

    @NotNull
    public List<String> getLibraries() {
        return getConfiguration().getList(JSConfigurationKeys.LIBRARIES);
    }

    public static abstract class Reporter {
        public void error(@NotNull String message) { /*Do nothing*/ }

        public void warning(@NotNull String message) { /*Do nothing*/ }
    }

    public boolean checkLibFilesAndReportErrors(@NotNull JsConfig.Reporter report) {
        return checkLibFilesAndReportErrors(report, null);
    }

    private boolean checkLibFilesAndReportErrors(@NotNull JsConfig.Reporter report, @Nullable Function1<VirtualFile, Unit> action) {
        List<String> libraries = getLibraries();
        if (libraries.isEmpty()) {
            return false;
        }

        VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);
        VirtualFileSystem jarFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL);

        Set<String> modules = new HashSet<String>();

        boolean skipMetadataVersionCheck =
                getLanguageVersionSettings(configuration).isFlagEnabled(AnalysisFlags.getSkipMetadataVersionCheck());

        for (String path : libraries) {
            VirtualFile file;

            File filePath = new File(path);
            if (!filePath.exists()) {
                report.error("Path '" + path + "' does not exist");
                return true;
            }

            if (path.endsWith(".jar") || path.endsWith(".zip")) {
                file = jarFileSystem.findFileByPath(path + URLUtil.JAR_SEPARATOR);
            }
            else {
                file = fileSystem.findFileByPath(path);
            }

            if (file == null) {
                report.error("File '" + path + "' does not exist or could not be read");
                return true;
            }

            List<KotlinJavascriptMetadata> metadataList = KotlinJavascriptMetadataUtils.loadMetadata(filePath);
            if (metadataList.isEmpty()) {
                report.warning("'" + path + "' is not a valid Kotlin Javascript library");
                continue;
            }

            for (KotlinJavascriptMetadata metadata : metadataList) {
                if (!metadata.getVersion().isCompatible() && !skipMetadataVersionCheck) {
                    report.error("File '" + path + "' was compiled with an incompatible version of Kotlin. " +
                                 "The binary version of its metadata is " + metadata.getVersion() +
                                 ", expected version is " + JsMetadataVersion.INSTANCE);
                    return true;
                }
                if (!modules.add(metadata.getModuleName())) {
                    report.warning("Module \"" + metadata.getModuleName() + "\" is defined in more than one file");
                }
            }

            if (action != null) {
                action.invoke(file);
            }
        }

        return false;
    }

    @NotNull
    public List<JsModuleDescriptor<ModuleDescriptorImpl>> getModuleDescriptors() {
        init();
        if (moduleDescriptors != null) return moduleDescriptors;

        moduleDescriptors = new SmartList<JsModuleDescriptor<ModuleDescriptorImpl>>();
        List<ModuleDescriptorImpl> kotlinModuleDescriptors = new ArrayList<ModuleDescriptorImpl>();
        for (KotlinJavascriptMetadata metadataEntry : metadata) {
            JsModuleDescriptor<ModuleDescriptorImpl> descriptor = createModuleDescriptor(metadataEntry);
            moduleDescriptors.add(descriptor);
            kotlinModuleDescriptors.add(descriptor.getData());
        }

        for (JsModuleDescriptor<ModuleDescriptorImpl> module : moduleDescriptors) {
            // TODO: remove downcast
            setDependencies(module.getData(), kotlinModuleDescriptors);
        }

        moduleDescriptors = Collections.unmodifiableList(moduleDescriptors);

        return moduleDescriptors;
    }

    private void init() {
        if (initialized) return;

        if (!getLibraries().isEmpty()) {
            Function1<VirtualFile, Unit> action = new Function1<VirtualFile, Unit>() {
                @Override
                public Unit invoke(VirtualFile file) {
                    String libraryPath = PathUtil.getLocalPath(file);
                    assert libraryPath != null : "libraryPath for " + file + " should not be null";
                    metadata.addAll(KotlinJavascriptMetadataUtils.loadMetadata(libraryPath));

                    return Unit.INSTANCE;
                }
            };

            boolean hasErrors = checkLibFilesAndReportErrors(new Reporter() {
                @Override
                public void error(@NotNull String message) {
                    throw new IllegalStateException(message);
                }
            }, action);
            assert !hasErrors : "hasErrors should be false";
        }

        initialized = true;
    }

    private JsModuleDescriptor<ModuleDescriptorImpl> createModuleDescriptor(KotlinJavascriptMetadata metadata) {
        LanguageVersionSettings languageVersionSettings = CommonConfigurationKeysKt.getLanguageVersionSettings(configuration);
        assert metadata.getVersion().isCompatible() ||
               languageVersionSettings.isFlagEnabled(AnalysisFlags.getSkipMetadataVersionCheck()) :
                "Expected JS metadata version " + JsMetadataVersion.INSTANCE + ", but actual metadata version is " + metadata.getVersion();

        ModuleDescriptorImpl moduleDescriptor = new ModuleDescriptorImpl(
                Name.special("<" + metadata.getModuleName() + ">"), storageManager, JsPlatform.INSTANCE.getBuiltIns()
        );

        JsModuleDescriptor<PackageFragmentProvider> rawDescriptor = KotlinJavascriptSerializationUtil.readModule(
                metadata.getBody(), storageManager, moduleDescriptor,
                new CompilerDeserializationConfiguration(languageVersionSettings)
        );

        PackageFragmentProvider provider = rawDescriptor.getData();
        moduleDescriptor.initialize(provider != null ? provider : PackageFragmentProvider.Empty.INSTANCE);

        return rawDescriptor.copy(moduleDescriptor);
    }

    private static void setDependencies(ModuleDescriptorImpl module, List<ModuleDescriptorImpl> modules) {
        module.setDependencies(CollectionsKt.plus(modules, JsPlatform.INSTANCE.getBuiltIns().getBuiltInsModule()));
    }
}

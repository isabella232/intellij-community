// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.lang.ant.config.impl;

import com.intellij.lang.ant.AntBundle;
import com.intellij.lang.ant.config.AntConfigurationBase;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static com.intellij.ide.macro.CompilerContextMakeMacro.COMPILER_CONTEXT_MAKE_KEY;

class AntBeforeCompileTask implements CompileTask {
  @Override
  public boolean execute(CompileContext context) {
    return initializeAndRun(context, antConfiguration -> antConfiguration.executeTargetBeforeCompile(
      createDataContext(context)));
  }

  static boolean initializeAndRun(CompileContext context, Processor<? super AntConfigurationBase> action) {
    context.getProgressIndicator().pushState();
    try {
      context.getProgressIndicator().setText(AntBundle.message("progress.text.loading.ant.config"));
      AntConfigurationBase config = AntConfigurationBase.getInstance(context.getProject());
      config.ensureInitialized();
      context.getProgressIndicator().setText(AntBundle.message("progress.text.running.ant.tasks"));
      return action.process(config);
    }
    finally {
      context.getProgressIndicator().popState();
    }
  }

  @NotNull
  static DataContext createDataContext(CompileContext context) {
    final HashMap<String, Object> dataMap = new HashMap<>();
    final Project project = context.getProject();
    dataMap.put(CommonDataKeys.PROJECT.getName(), project);
    final CompileScope scope = context.getCompileScope();
    final Module[] modules = scope.getAffectedModules();
    if (modules.length == 1) {
      dataMap.put(LangDataKeys.MODULE.getName(), modules[0]);
    }
    dataMap.put(LangDataKeys.MODULE_CONTEXT_ARRAY.getName(), modules);
    dataMap.put(COMPILER_CONTEXT_MAKE_KEY.getName(), context.isMake());
    return SimpleDataContext.getSimpleContext(dataMap, null);
  }

}

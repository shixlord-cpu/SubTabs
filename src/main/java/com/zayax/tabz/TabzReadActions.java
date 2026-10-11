package com.zayax.tabz;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteIntentReadAction;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;

final class TabzReadActions {
    private TabzReadActions() {
    }

    static <T> T compute(@NotNull Computable<T> computable) {
        return ApplicationManager.getApplication().runReadAction(computable);
    }

    static <T> T computeOnUiThreadWithWriteIntent(@NotNull Computable<T> computable) {
        Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            Ref<T> result = Ref.create();
            Runnable action = () -> result.set(computable.compute());
            WriteIntentReadAction.run(action);
            return result.get();
        }
        return application.runReadAction(computable);
    }
}

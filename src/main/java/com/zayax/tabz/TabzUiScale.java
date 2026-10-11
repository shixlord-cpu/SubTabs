package com.zayax.tabz;

import com.intellij.util.ui.JBUI;
/** Replaces deprecated {@link JBUI#scale(float)} with integer user-unit scaling. */
final class TabzUiScale {
    private TabzUiScale() {
    }

    static float units(float value) {
        return scaledInt(Math.round(value));
    }

    static float scaledInt(int userUnits) {
        return JBUI.scale(userUnits);
    }
}

package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DemoReplayJson {
    private final StringBuilder out = new StringBuilder();
    private boolean needsComma;

    private DemoReplayJson() {
    }

    static @NotNull DemoReplayJson object() {
        return new DemoReplayJson().startObject();
    }

    @NotNull DemoReplayJson startObject() {
        out.append('{');
        needsComma = false;
        return this;
    }

    @NotNull DemoReplayJson endObject() {
        out.append('}');
        return this;
    }

    @NotNull DemoReplayJson key(@NotNull String name) {
        comma();
        out.append('"').append(escape(name)).append("\":");
        return this;
    }

    @NotNull DemoReplayJson value(@Nullable String value) {
        if (value == null) {
            out.append("null");
        } else {
            out.append('"').append(escape(value)).append('"');
        }
        return this;
    }

    @NotNull DemoReplayJson rawValue(@NotNull String jsonFragment) {
        out.append(jsonFragment);
        return this;
    }

    @NotNull DemoReplayJson value(boolean value) {
        out.append(value);
        return this;
    }

    @NotNull DemoReplayJson value(int value) {
        out.append(value);
        return this;
    }

    @NotNull DemoReplayJson value(long value) {
        out.append(value);
        return this;
    }

    @NotNull DemoReplayJson nested(@NotNull String name, @NotNull DemoReplayJson nestedObject) {
        key(name);
        out.append(nestedObject.build());
        return this;
    }

    @NotNull DemoReplayJson arrayStart(@NotNull String name) {
        key(name);
        out.append('[');
        needsComma = false;
        return this;
    }

    @NotNull DemoReplayJson arrayEnd() {
        out.append(']');
        needsComma = true;
        return this;
    }

    @NotNull DemoReplayJson element(@NotNull DemoReplayJson elementObject) {
        comma();
        out.append(elementObject.build());
        return this;
    }

    @NotNull String build() {
        return out.toString();
    }

    private void comma() {
        if (needsComma) {
            out.append(',');
        }
        needsComma = true;
    }

    static @NotNull String escape(@NotNull String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }
}

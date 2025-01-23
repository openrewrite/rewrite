package org.openrewrite.json.style;

import lombok.Value;
import lombok.With;
import org.openrewrite.style.LineWrapSetting;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@Value
@With
public class WrappingAndBracesStyle implements JsonStyle {
    public static final WrappingAndBracesStyle DEFAULT = new WrappingAndBracesStyle(LineWrapSetting.WrapAlways, LineWrapSetting.WrapAlways);

    LineWrapSetting wrapObjects;
    LineWrapSetting wrapArrays;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(DEFAULT, this);
    }
}

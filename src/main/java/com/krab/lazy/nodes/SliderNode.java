package com.krab.lazy.nodes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;


import com.krab.lazy.input.LazyKeyEvent;
import com.krab.lazy.input.LazyMouseEvent;
import com.krab.lazy.stores.DelayStore;
import com.krab.lazy.utils.KeyCodes;
import com.krab.lazy.stores.ShaderStore;
import com.krab.lazy.themes.ThemeColorType;
import com.krab.lazy.themes.ThemeStore;
import com.krab.lazy.utils.ListBuilder;
import com.krab.lazy.utils.ClipboardUtils;
import com.krab.lazy.stores.JsonSaveStore;
import processing.core.PGraphics;
import processing.opengl.PShader;

import java.util.List;

import static com.krab.lazy.stores.NormColorStore.*;
import static com.krab.lazy.stores.GlobalReferences.app;
import static processing.core.PApplet.*;

public class SliderNode extends AbstractNode {

    @Expose
    public float valueFloat;
    @Expose
    protected int currentPrecisionIndex;
    @Expose
    protected float valueFloatPrecision;

    float valueFloatDefault;
    final float valueFloatMin;
    final float valueFloatMax;
    final boolean valueFloatConstrained;
    float backgroundScrollX = 0;
    float mouseDeltaX, mouseDeltaY;
    boolean verticalMouseMode = false;
    protected String numpadBufferValue = "";
    protected boolean showPercentIndicatorWhenConstrained = true;
    protected final List<Float> precisionRange = new ListBuilder<Float>()
            .add(0.0001f)
            .add(0.001f)
            .add(0.01f)
            .add(0.1f)
            .add(1f)
            .add(10.0f)
            .add(100.0f).build();

    private static final String SQUIGGLY_EQUALS = "≈";
    final List<Character> numpadChars = new ListBuilder<Character>()
            .add('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
            .build();
    private int numpadInputAppendLastMillis = -1;
    private boolean wasNumpadInputActiveLastFrame = false;


    private static final String REGEX_FRACTION_SEPARATOR = "[.,]";
    private static final String REGEX_ANY_NUMBER_SERIES = "[0-9]*";
    private static final String FRACTIONAL_FLOAT_REGEX = REGEX_ANY_NUMBER_SERIES + REGEX_FRACTION_SEPARATOR + REGEX_ANY_NUMBER_SERIES;
    private final String shaderPath = "sliderBackground.glsl";
    protected int maximumFloatPrecisionIndex = -1;
    protected int minimumFloatPrecisionIndex = -1;

    public SliderNode(String path, FolderNode parentFolder, float defaultValue, float min, float max, boolean constrained) {
        super(NodeType.VALUE, path, parentFolder);
        valueFloatDefault = defaultValue;
        if (!Float.isNaN(defaultValue)) {
            valueFloat = defaultValue;
        }
        valueFloatMin = min;
        valueFloatMax = max;
        valueFloatConstrained = constrained &&
                max != Float.MAX_VALUE &&
                min != -Float.MAX_VALUE ;
        setSensiblePrecision(nf(valueFloat, 0, 0));
        JsonSaveStore.overwriteWithLoadedStateIfAny(this);
    }

    public void initSliderBackgroundShader() {
        ShaderStore.getShader(shaderPath);
    }

    private void setSensiblePrecision(String value) {
        if (value.equals("0") || value.equals("0.0")) {
            setPrecisionIndexAndValue(precisionRange.indexOf(0.1f));
            return;
        }
        if (value.matches(FRACTIONAL_FLOAT_REGEX)) {
            int fractionalDigitLength = getFractionalDigitLength(value);
            setPrecisionIndexAndValue(4 - fractionalDigitLength);
            return;
        }
        setPrecisionIndexAndValue(precisionRange.indexOf(1f));
    }

    private int getFractionalDigitLength(String value) {
        if (value.contains(".") || value.contains(",")) {
            return value.split(REGEX_FRACTION_SEPARATOR)[1].length();
        }
        return 0;
    }

    @Override
    protected void drawNodeBackground(PGraphics pg) {
        boolean constrainedThisFrame = tryConstrainValue();
        if(isInlineNodeDragged || isMouseOverNode){
            drawBackgroundScroller(pg, constrainedThisFrame);
        }
        mouseDeltaX = 0;
        mouseDeltaY = 0;
    }

    @Override
    public void updateValuesRegardlessOfParentWindowOpenness() {
        if (isInlineNodeDragged || isMouseOverNode) {
            updateValueMouseInteraction();
        }
        updateNumpad();
    }

    @Override
    protected void drawNodeForeground(PGraphics pg, String name) {
        fillForegroundBasedOnMouseOver(pg);
        drawLeftText(pg, name);
        drawRightText(pg, getValueToDisplay() + (isNumpadInputActive() ? "_" : ""), true);
    }

    private void drawBackgroundScroller(PGraphics pg, boolean constrainedThisFrame) {
        if (!constrainedThisFrame) {
            backgroundScrollX -= verticalMouseMode ? mouseDeltaY : mouseDeltaX;
        }
        float percentIndicatorNorm = 1f;
        boolean shouldShowPercentIndicator = valueFloatConstrained && showPercentIndicatorWhenConstrained;
        if (shouldShowPercentIndicator) {
            percentIndicatorNorm = constrain(norm(valueFloat, valueFloatMin, valueFloatMax), 0, 1);
            backgroundScrollX = 0;
        }

        updateBackgroundShader(pg);
        pg.fill(ThemeStore.getColor(ThemeColorType.FOCUS_BACKGROUND));
        pg.noStroke();
        pg.rect(1, 0, (size.x - 1) * percentIndicatorNorm, size.y);
        pg.resetShader();

        if (shouldShowPercentIndicator) {
            pg.stroke(ThemeStore.getColor(ThemeColorType.WINDOW_BORDER));
            pg.strokeWeight(2);
            float lineX = (size.x - 1) * percentIndicatorNorm;
            pg.line(lineX, 0, lineX, size.y);
        }
    }

    protected void updateBackgroundShader(PGraphics pg) {
        PShader shader = ShaderStore.getShader(shaderPath);
        shader.set("scrollX", backgroundScrollX);
        int bgColor = ThemeStore.getColor(ThemeColorType.NORMAL_BACKGROUND);
        int fgColor = ThemeStore.getColor(ThemeColorType.FOCUS_BACKGROUND);
        shader.set("colorA", red(bgColor), green(bgColor), blue(bgColor));
        shader.set("colorB", red(fgColor), green(fgColor), blue(fgColor));
        shader.set("precisionNormalized", norm(currentPrecisionIndex, 0, precisionRange.size()));
        pg.shader(shader);
    }

    protected String getValueToDisplay() {
        // the display value flickers back to the "valueFloat" for one frame if we just rely on "isNumpadActive()"
        // so we keep displaying the buffer for 1 more frame with "wasNumpadInputActiveLastFrame"
        if (isNumpadInputActive() || wasNumpadInputActiveLastFrame) {
            return numpadBufferValue;
        }
        if (Float.isNaN(valueFloat)) {
            return "NaN";
        }
        String valueToDisplay;
        boolean isFractionalPrecision = valueFloatPrecision % 1f > 0;
        if (isFractionalPrecision) {
            valueToDisplay = nf(valueFloat, 0, getFractionalDigitLength(String.valueOf(valueFloatPrecision)));
        } else {
            valueToDisplay = nf(round(valueFloat), 0, 0);
            if(!valueToDisplay.equals(nf(valueFloat, 0, 0)) && abs(valueFloat) < 100){
                // the display value was rounded into an integer and that made it misleading, so we indicate that
                return SQUIGGLY_EQUALS + " " + valueToDisplay;
            }
        }
        // java float literals use . so we also use .
        return valueToDisplay.replaceAll(",", ".");
    }

    @Override
    public void mouseWheelMovedOverNode(float x, float y, int dir) {
        super.mouseWheelMovedOverNode(x, y, dir);
        if (dir > 0) {
            increasePrecision();
        } else if (dir < 0) {
            decreasePrecision();
        }
    }

    private void setWholeNumberPrecision() {
        for (int i = 0; i < precisionRange.size(); i++) {
            if (precisionRange.get(i) >= 1f) {
                setPrecisionIndexAndValue(i);
                break;
            }
        }
    }

    void decreasePrecision() {
        setPrecisionIndexAndValue(min(currentPrecisionIndex + 1, precisionRange.size() - 1));
    }

    void increasePrecision() {
        setPrecisionIndexAndValue(max(currentPrecisionIndex - 1, 0));
    }

    protected void setPrecisionIndexAndValue(int newPrecisionIndex) {
        if(!validatePrecision(newPrecisionIndex)){
            return;
        }
        currentPrecisionIndex = constrain(newPrecisionIndex, 0, precisionRange.size() - 1);
        valueFloatPrecision = precisionRange.get(currentPrecisionIndex);
    }

    protected boolean validatePrecision(int newPrecisionIndex) {
        return  (maximumFloatPrecisionIndex == -1 || newPrecisionIndex <= maximumFloatPrecisionIndex) &&
                (minimumFloatPrecisionIndex == -1 || newPrecisionIndex >= minimumFloatPrecisionIndex);
    }

    private void updateValueMouseInteraction() {
        float mouseDelta = verticalMouseMode ? mouseDeltaY : mouseDeltaX;
        if (mouseDelta != 0) {
            float delta = mouseDelta * precisionRange.get(currentPrecisionIndex);
            setValueFloat(valueFloat - delta);
            mouseDeltaX = 0;
            mouseDeltaY = 0;
        }
    }

    protected boolean tryConstrainValue() {
        boolean constrained = false;
        if (valueFloatConstrained) {
            if (valueFloat > valueFloatMax || valueFloat < valueFloatMin) {
                constrained = true;
            }
            valueFloat = constrain(valueFloat, valueFloatMin, valueFloatMax);
        }
        return constrained;
    }

    private void updateNumpad() {
        if (!isNumpadInputActive() && wasNumpadInputActiveLastFrame) {
            if (numpadBufferValue.endsWith(".")) {
                numpadBufferValue += "0";
            }
            if (tryParseAndSetValueFloat(numpadBufferValue)) {
                setSensiblePrecision(numpadBufferValue);
            }
        }
        wasNumpadInputActiveLastFrame = isNumpadInputActive();
    }

    @Override
    public void keyPressedOverNode(LazyKeyEvent e, float x, float y) {
        super.keyPressedOverNode(e, x, y);
        if (e.getKey() == 'r') {
            if (!Float.isNaN(valueFloatDefault)) {
                setValueFloat(valueFloatDefault);
            }
            e.consume();
        }
        tryReadNumpadInput(e);
        if (e.isControlDown() && e.getKeyCode() == KeyCodes.C) {
            String value = getValueToDisplay().replaceAll(SQUIGGLY_EQUALS, "");
            if (value.endsWith(".")) {
                value += "0";
            }
            ClipboardUtils.setClipboardString(value);
            e.consume();
        }
        if (e.isControlDown() && e.getKeyCode() == KeyCodes.V) {
            String clipboardString = ClipboardUtils.getClipboardString();
            
            try {
                float clipboardValue = Float.parseFloat(clipboardString);
                if (!Float.isNaN(clipboardValue)) {
                    setValueFloat(clipboardValue);
                } else {
                    println("Could not parse float from this clipboard string: " + clipboardString);
                }
            } catch (NumberFormatException nfe) {
                println("Could not parse float from this clipboard string: " + clipboardString);
            }
            e.consume();
        }
    }

    private void tryReadNumpadInput(LazyKeyEvent e) {
        boolean inReplaceMode = isNumpadInReplaceMode();
        if (numpadChars.contains(e.getKey())) {
            tryAppendNumberInputToBufferValue(Integer.valueOf(String.valueOf(e.getKey())), inReplaceMode);
            e.consume();
        }
        switch (e.getKey()) {
            case '.':
            case ',':
                setNumpadInputActiveStarted();
                if (numpadBufferValue.isEmpty()) {
                    numpadBufferValue += "0";
                }
                if (!numpadBufferValue.endsWith(".")) {
                    numpadBufferValue += ".";
                }
                e.consume();
                break;
            case '+':
            case '-':
                if (inReplaceMode) {
                    numpadBufferValue = "" + e.getKey();
                }
                setNumpadInputActiveStarted();
                e.consume();
                break;
            case '*':
                decreasePrecision();
                e.consume();
                break;
            case '/':
                increasePrecision();
                e.consume();
                break;
        }
    }

    private void tryAppendNumberInputToBufferValue(Integer input, boolean inReplaceMode) {
        String inputString = String.valueOf(input);
        setNumpadInputActiveStarted();
        if (inReplaceMode) {
            numpadBufferValue = inputString;
            if (input != 0) {
                // when I only reset a value to 0 I usually want to keep its old precision
                // when I start typing something other than 0 I usually do want whole number precision
                setWholeNumberPrecision();
            }
            return;
        }
        numpadBufferValue += inputString;
    }

    protected void setNumpadInputActiveStarted() {
        numpadInputAppendLastMillis = app.millis();
    }

    protected boolean isNumpadInputActive() {
        return numpadInputAppendLastMillis != -1 &&
                app.millis() <= numpadInputAppendLastMillis + DelayStore.getKeyboardBufferDelayMillis();
    }

    private boolean isNumpadInReplaceMode() {
        return numpadInputAppendLastMillis == -1 ||
                app.millis() - numpadInputAppendLastMillis > DelayStore.getKeyboardBufferDelayMillis();
    }

    private boolean tryParseAndSetValueFloat(String toParseAsFloat) {
        float parsed;
        try {
            parsed = Float.parseFloat(toParseAsFloat);
        } catch (NumberFormatException formatException) {
            println(formatException.getMessage(), formatException);
            return false;
        }
        setValueFloat(parsed);
        onActionEnded();
        return true;
    }

    protected void setValueFloat(float floatToSet) {
        valueFloat = floatToSet;
        onValueFloatChanged();
    }

    protected void onValueFloatChanged() {
        tryConstrainValue();
    }

    @Override
    public void mouseDragNodeContinue(LazyMouseEvent e) {
        super.mouseDragNodeContinue(e);
        mouseDeltaX = e.getPrevX() - e.getX();
        mouseDeltaY = e.getPrevY() - e.getY();
        e.setConsumed(true);
    }

    @Override
    public void overwriteState(JsonElement loadedNode) {
        JsonObject json = loadedNode.getAsJsonObject();
        if (json.has("currentPrecisionIndex")) {
            currentPrecisionIndex = json.get("currentPrecisionIndex").getAsInt();
        }
        if (json.has("valueFloatPrecision")) {
            valueFloatPrecision = json.get("valueFloatPrecision").getAsFloat();
        }
        if (json.has("valueFloat")) {
            setValueFloat(json.get("valueFloat").getAsFloat());
        }
    }

    @Override
    public String getValueAsString() {
        return getValueToDisplay();
    }

}

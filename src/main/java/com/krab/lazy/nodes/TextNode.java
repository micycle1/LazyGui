package com.krab.lazy.nodes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.krab.lazy.input.LazyKeyEvent;
import com.krab.lazy.stores.*;
import com.krab.lazy.themes.ThemeColorType;
import com.krab.lazy.themes.ThemeStore;
import com.krab.lazy.utils.ClipboardUtils;
import com.krab.lazy.utils.KeyCodes;
import processing.core.PConstants;
import processing.core.PGraphics;

import static processing.core.PConstants.*;

public class TextNode extends AbstractNode {

    @Expose
    String stringValue;
    String buffer;

    private final int millisInputDelay;
    private int millisInputStarted;

    private final String regexLookBehindForNewLine = "(?<=\\n)";
    private final boolean shouldDisplayHeaderRow;

    public TextNode(String path, FolderNode folder, String content) {
        super(NodeType.VALUE, path, folder);
        this.stringValue = content;
        this.buffer = content;
        shouldDisplayHeaderRow = !name.trim().isEmpty();
        millisInputDelay = DelayStore.getKeyboardBufferDelayMillis();
        millisInputStarted = -millisInputDelay * 2;
        JsonSaveStore.overwriteWithLoadedStateIfAny(this);
    }

    @Override
    protected void drawNodeBackground(PGraphics pg) {
//        drawLeftIndentLine(pg);
    }

    @Override
    protected void drawNodeForeground(PGraphics pg, String name) {
        fillForegroundBasedOnMouseOver(pg);
        String toDisplay = buffer;
        int lineCount = toDisplay.split(regexLookBehindForNewLine).length + (toDisplay.endsWith("\n") ? 1 : 0);
        if(shouldDisplayHeaderRow){
            drawLeftText(pg, name);
            lineCount += 1;
        }
        masterInlineNodeHeightInCells = lineCount;
        String contentToDraw = toDisplay.length() == 0 ? "..." : toDisplay;
        fillForegroundBasedOnMouseOver(pg);
        drawContent(pg, contentToDraw);
    }

    protected void drawContent(PGraphics pg, String contentToDraw) {
        fillForegroundBasedOnMouseOver(pg);
        pg.textAlign(LEFT, CENTER);
        String[] lines = contentToDraw.split(regexLookBehindForNewLine);
        pg.pushMatrix();
        float contentMarginLeft = 0.3f * LayoutStore.cell;
        if(shouldDisplayHeaderRow){
            pg.translate(0, LayoutStore.cell);
        }
        pg.translate(0, 0);
        pg.textFont(FontStore.getSideFont());
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].replace("\n", "");
            boolean isLastLine = i == lines.length - 1;
            float textFieldWidth = size.x - contentMarginLeft - FontStore.textMarginX + (isLastLine ? -LayoutStore.cell : 0);
            float fadeoutWidth = LayoutStore.cell * 1.5f;
            String lineThatFitsWindow = FontStore.getSubstringFromStartToFit(pg, line, textFieldWidth);
            if (isLastLine) {
                // last line is displayed "fromEnd" because you want to see what you're typing,
                // and you never want to draw the right indicator there
                lineThatFitsWindow = FontStore.getSubstringFromEndToFit(pg, line, textFieldWidth);
            }
            pg.translate(0, LayoutStore.cell);
            pg.text(lineThatFitsWindow, contentMarginLeft + FontStore.textMarginX, -FontStore.textMarginY);

            if(!isMouseOverNode){
                int bgColor = ThemeStore.getColor(ThemeColorType.NORMAL_BACKGROUND);
                if(isLastLine){
                    boolean isTrimmedToFit = lineThatFitsWindow.length() < line.length();
                    if(isTrimmedToFit){
                        drawGradientRectangle(pg, 0, -LayoutStore.cell, fadeoutWidth, LayoutStore.cell,
                                bgColor, NormColorStore.toTransparent(bgColor));
                    }
                }else{
                    drawGradientRectangle(pg, size.x-fadeoutWidth, -LayoutStore.cell, fadeoutWidth, LayoutStore.cell,
                            NormColorStore.toTransparent(bgColor), bgColor);

                }
            }
        }
        pg.popMatrix();
    }

    void drawGradientRectangle(PGraphics pg, float x, float y, float w, float h, int colorLeft, int colorRight){
        pg.pushMatrix();
        pg.pushStyle();
        pg.translate(x, y);
        pg.noStroke();
        pg.beginShape();
        pg.fill(colorLeft);
        pg.vertex(0,0);
        pg.fill(colorRight);
        pg.vertex(w, 0);
        pg.vertex(w, h);
        pg.fill(colorLeft);
        pg.vertex(0, h);
        pg.endShape(CLOSE);
        pg.popStyle();
        pg.popMatrix();
    }

    @Override
    public void updateValuesRegardlessOfParentWindowOpenness() {
        trySetContentToBufferAfterDelay();
    }

    private void trySetContentToBufferAfterDelay() {
        if(!stringValue.equals(buffer) && GlobalReferences.app.millis() > millisInputStarted + millisInputDelay){
            setStringValueUndoably(buffer);
        }
    }

    @Override
    public void keyPressedOverNode(LazyKeyEvent e, float x, float y) {
        // based on tip #13 in here:
        // https://amnonp5.wordpress.com/2012/01/28/25-life-saving-tips-for-processing/
        if (isMouseOverNode) {
//            PApplet.println("key code" + e.getKeyCode());
            if(KeyCodes.shouldIgnoreForTextInput(e.getKeyCode())){
                return;
            }
            millisInputStarted = GlobalReferences.app.millis();
            if (e.getKeyCode() == PConstants.BACKSPACE) {
                if (buffer.length() > 0) {
                    buffer = buffer.substring(0, buffer.length() - 1);
                }
                e.consume();
            } else if (e.getKeyCode() == KeyCodes.DELETE || e.getKey() == PConstants.DELETE) {
                buffer = "";
                e.consume();
            } else if (e.isControlDown() && e.getKeyCode() == KeyCodes.C) {
                ClipboardUtils.setClipboardString(this.buffer);
                e.consume();
            } else if (e.isControlDown() && e.getKeyCode() == KeyCodes.V) {
                setStringValueUndoably(ClipboardUtils.getClipboardString());
                e.consume();
            } else if(!e.isControlDown() && !e.isAltDown()){
                buffer = buffer + e.getKey();
                e.consume();
            }
        }
    }

    @Override
    public void overwriteState(JsonElement loadedNode) {
        JsonObject json = loadedNode.getAsJsonObject();
        if (json.has("stringValue")) {
            stringValue = json.get("stringValue").getAsString();
            buffer = stringValue;
        }
    }

    public String getStringValue() {
        return stringValue;
    }

    private void setStringValueUndoably(String newValue) {
        setStringValue(newValue);
        onActionEnded();
    }

    public void setStringValue(String newValue) {
        stringValue = newValue;
        buffer = newValue;
    }

    @Override
    public String getValueAsString() {
        return stringValue;
    }
}

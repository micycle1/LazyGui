package com.krab.lazy.nodes;

import com.google.gson.JsonElement;
import com.krab.lazy.stores.GlobalReferences;
import processing.core.PGraphics;

class GradientColorStopNode extends ColorPickerFolderNode {
    final GradientColorStopPositionSlider posSlider;

    GradientColorStopNode(String path, FolderNode parentFolder, int hex, float gradientPos) {
        super(path, parentFolder, hex);
        posSlider = new GradientColorStopPositionSlider(path + "/pos", this, gradientPos, 0,1,true);
        this.children.add(posSlider);
    }

    @Override
    protected void drawNodeForeground(PGraphics pg, String name) {
        drawLeftText(pg, name);
        drawPreviewRect(pg);
    }

    float getGradientPos() {
        return posSlider.valueFloat;
    }

    @Override
    public void overwriteState(JsonElement loadedNode) {
        super.overwriteState(loadedNode);
    }

    public boolean isPosSliderBeingUsed() {
        return isMouseOverNode || posSlider.isInlineNodeDragged ||
            (window != null && !window.closed && window.isPointInsideContent(GlobalReferences.app.mouseX, GlobalReferences.app.mouseY));
    }


}
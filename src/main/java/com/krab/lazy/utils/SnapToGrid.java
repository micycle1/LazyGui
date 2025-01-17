package com.krab.lazy.utils;

import com.krab.lazy.stores.GlobalReferences;
import com.krab.lazy.stores.LayoutStore;
import com.krab.lazy.stores.NormColorStore;
import com.krab.lazy.PickerColor;
import com.krab.lazy.stores.ShaderStore;
import com.krab.lazy.windows.Window;
import com.krab.lazy.windows.WindowManager;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.opengl.PShader;

import java.util.List;

import static processing.core.PApplet.*;

public class SnapToGrid {
    public static boolean snapToGridEnabled = true;
    static final List<String> availableVisibilityModes = new ListBuilder<String>().add("always", "on drag", "never").build();
    private static PShader pointShader;
    private static final String pointShaderPath = "guideGridPoints.glsl";
    private static final int VISIBILITY_ALWAYS = 0;
    private static final int VISIBILITY_ON_DRAG = 1;
    private static final int VISIBILITY_NEVER = 2;
    private static final int defaultVisibilityModeIndex = VISIBILITY_ON_DRAG;
    private static int selectedVisibilityModeIndex = defaultVisibilityModeIndex;
    private static float dragAlpha = 0;
    private static final float dragAlphaDelta = 0.05f;
    private static PickerColor pointGridColor = new PickerColor(0xFF7F7F7F, 1,1,0.5f,1);
    private static float pointWeight = 3f;
    private static float sdfCropDistance = 100;
    private static boolean shouldCenterPoints = true;
    static float cellSizeLastFrame = -1;
    private static int pointColorPrev = -1;
    private static float pointColorRed, pointColorGreen, pointColorBlue;

    public static void displayGuideAndApplyFilter(PGraphics pg, Window draggedWindow){
        if(pointShader == null){
            pointShader = ShaderStore.getShader(pointShaderPath);
        }
        if(selectedVisibilityModeIndex == VISIBILITY_ON_DRAG){
            updateAlpha(draggedWindow);
        }
        if(selectedVisibilityModeIndex == VISIBILITY_NEVER){
            return;
        }
        pointShader.set("alpha", selectedVisibilityModeIndex == VISIBILITY_ALWAYS ? pointGridColor.alpha : dragAlpha);
        pointShader.set("sdfCropEnabled", selectedVisibilityModeIndex == VISIBILITY_ON_DRAG);
        pointShader.set("shouldCenterPoints", shouldCenterPoints);
        pointShader.set("sdfCropDistance", sdfCropDistance);
        pointShader.set("gridCellSize", (float) floor(LayoutStore.cell));
        int pointColor = pointGridColor.hex;
        if(pointColorPrev == -1 || pointColor != pointColorPrev){
            pointColorPrev = pointColor;
            pointColorRed = NormColorStore.red(pointColor);
            pointColorGreen = NormColorStore.green(pointColor);
            pointColorBlue = NormColorStore.blue(pointColor);
        }
        pointShader.set("pointColor", pointColorRed, pointColorGreen, pointColorBlue);
        pointShader.set("pointWeight", pointWeight);
        if(draggedWindow != null){
            pointShader.set("window", draggedWindow.posX, draggedWindow.posY, draggedWindow.windowSizeX, draggedWindow.windowSizeY);
        }
        pg.filter(pointShader);
        pg.resetShader();
    }

    private static void updateAlpha(Window draggedWindow) {
        float dragAlphaMax = pointGridColor.alpha;
        dragAlphaMax = constrain(dragAlphaMax, 0, 1);
        if(draggedWindow != null){
            dragAlpha = lerp(dragAlpha, dragAlphaMax, dragAlphaDelta);
        }else{
            dragAlpha = lerp(dragAlpha, 0, dragAlphaDelta);
        }
        dragAlpha = constrain(dragAlpha, 0, dragAlphaMax);
    }

    public static PVector trySnapToGrid(float inputX, float inputY){
        if(!snapToGridEnabled) {
            return new PVector(inputX, inputY);
        }
        float negativeModuloBuffer = LayoutStore.cell * 60;
        inputX += negativeModuloBuffer;
        inputY += negativeModuloBuffer;
        int x = floor(inputX);
        int y = floor(inputY);
        if(x % LayoutStore.cell > LayoutStore.cell / 2 ){
            x += LayoutStore.cell;
        }
        if(y % LayoutStore.cell > LayoutStore.cell / 2 ){
            y += LayoutStore.cell;
        }
        while(x % LayoutStore.cell != 0){
            x -= 1;
        }
        while(y % LayoutStore.cell != 0){
            y -= 1;
        }
        return new PVector(x-negativeModuloBuffer, y-negativeModuloBuffer);
    }

    public static List<String> getOptions() {
        return availableVisibilityModes;
    }

    public static void setSelectedVisibilityMode(String mode) {
        if(!availableVisibilityModes.contains(mode)){
            return;
        }
        selectedVisibilityModeIndex = availableVisibilityModes.indexOf(mode);
    }

    public static String getDefaultVisibilityMode() {
        return getOptions().get(defaultVisibilityModeIndex);
    }

    public static void updateSettings() {
        GlobalReferences.gui.pushFolder("grid");
        boolean previousSnapToGridEnabled = snapToGridEnabled;
        snapToGridEnabled = GlobalReferences.gui.toggle("snap to grid", true);
        if(hasCellSizeJustChanged() || hasJustBeenEnabled(previousSnapToGridEnabled, snapToGridEnabled)){
            WindowManager.snapAllStaticWindowsToGrid();
        }
        setSelectedVisibilityMode(GlobalReferences.gui.radio("show grid", getOptions(), getDefaultVisibilityMode()));
        sdfCropDistance = GlobalReferences.gui.slider("drag range", sdfCropDistance);
        pointGridColor = GlobalReferences.gui.colorPicker("point color", pointGridColor.hex);
        pointWeight = GlobalReferences.gui.slider("point size", pointWeight);
        shouldCenterPoints = GlobalReferences.gui.toggle("points centered", shouldCenterPoints);
        GlobalReferences.gui.popFolder();
    }

    private static boolean hasCellSizeJustChanged() {
        boolean result = cellSizeLastFrame != LayoutStore.cell;
        cellSizeLastFrame = LayoutStore.cell;
        return result;
    }

    private static boolean hasJustBeenEnabled(boolean previousState, boolean currentState) {
        return !previousState && currentState;
    }
}

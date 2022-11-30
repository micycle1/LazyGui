package lazy;

import processing.core.PGraphics;
import processing.core.PVector;
import processing.opengl.PShader;

import java.util.List;

import static lazy.State.cell;
import static processing.core.PApplet.*;

public class UtilGridSnap {
    public static boolean snapToGridEnabled = false;

    private static PShader pointShader;
    private static final String pointShaderPath = "gridPointFrag.glsl";
    static List<String> availableVisibilityModes = new Utils.ArrayListBuilder<String>().add("always", "on drag", "never").build();
    private static final int VISIBILITY_ALWAYS = 0;
    private static final int VISIBILITY_ON_DRAG = 1;
    private static final int VISIBILITY_NEVER = 2;
    private static final int defaultVisibilityModeIndex = VISIBILITY_ON_DRAG;
    private static int selectedVisibilityModeIndex = defaultVisibilityModeIndex;

    private static float dragAlpha = 0;
    private static final float dragAlphaDelta = 0.05f;
    private static PickerColor pointGridColor = null;
    private static float pointWeight = 4;

    static void displayGuideAndApplyFilter(PGraphics pg, Window draggedWindow){
        if(pointShader == null){
            pointShader = InternalShaderStore.getShader(pointShaderPath);
        }
        if(selectedVisibilityModeIndex == VISIBILITY_ON_DRAG){
            updateAlpha(draggedWindow);
        }
        if(selectedVisibilityModeIndex == VISIBILITY_NEVER){
            return;
        }
        pointShader.set("alpha", selectedVisibilityModeIndex == VISIBILITY_ALWAYS ? pointGridColor.alpha : dragAlpha);
        pointShader.set("sdfCropEnabled", selectedVisibilityModeIndex == VISIBILITY_ON_DRAG);
        if(draggedWindow != null){
            pointShader.set("window", draggedWindow.posX, draggedWindow.posY, draggedWindow.windowSizeX, draggedWindow.windowSizeY);
        }
        pg.shader(pointShader);

        pg.pushStyle();
        pg.strokeWeight(pointWeight);
        float w = pg.width;
        float h = pg.height;
        int step = floor(cell);
        pg.beginShape(POINTS);
        int pointColor = pointGridColor != null ? pointGridColor.hex : State.normColor(1);
        pg.stroke(pointColor);
        for (int x = 0; x <= w; x+= step) {
            for (int y = 0; y <= h; y+= step) {
                pg.vertex(x+0.5f,y+0.5f);
            }
        }
        pg.endShape();
        pg.popStyle();


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

    static PVector trySnapToGrid(float inputX, float inputY){
        if(!snapToGridEnabled) {
            return new PVector(inputX, inputY);
        }
        float negativeModuloBuffer = cell * 60;
        inputX += negativeModuloBuffer;
        inputY += negativeModuloBuffer;
        int x = floor(inputX);
        int y = floor(inputY);
        if(x % cell > cell / 2 ){
            x += cell;
        }
        if(y % cell > cell / 2 ){
            y += cell;
        }
        while(x % cell != 0){
            x -= 1;
        }
        while(y % cell != 0){
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

    public static void setPointColor(PickerColor clr){
        pointGridColor = clr;
    }

    public static void setPointWeight(float weight) {
        pointWeight = weight;
    }

    public static void update() {
        State.gui.pushFolder("grid");
        boolean previousSnapToGridState = UtilGridSnap.snapToGridEnabled;
        UtilGridSnap.snapToGridEnabled = State.gui.toggle("snap to grid", true);
        if(!previousSnapToGridState && UtilGridSnap.snapToGridEnabled){
            // cell size must be updated before this for this auto snap to work on startup
            WindowManager.snapAllStaticWindowsToGrid();
        }
        UtilGridSnap.setSelectedVisibilityMode(State.gui.radio("show grid",
                UtilGridSnap.getOptions(), UtilGridSnap.getDefaultVisibilityMode()));
        PickerColor clr = State.gui.colorPicker("point color", State.normColor(0.5f, 1));

        UtilGridSnap.setPointColor(clr);
        UtilGridSnap.setPointWeight(State.gui.slider("point weight", UtilGridSnap.pointWeight));
        State.gui.popFolder();
    }
}

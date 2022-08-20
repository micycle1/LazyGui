package lazy.windows.nodes.select;

import processing.core.PGraphics;
import lazy.global.State;
import lazy.global.themes.ThemeColorType;
import lazy.global.themes.ThemeStore;
import lazy.windows.nodes.NodeFolder;
import lazy.windows.nodes.ToggleNode;

import static processing.core.PConstants.*;

public class StringPickerItem extends ToggleNode {

    String valueString;

    public StringPickerItem(String path, NodeFolder folder, boolean valueBoolean, String valueString) {
        super(path, folder, valueBoolean);
        this.valueString = valueString;
    }

    @Override
    public void updateDrawInlineNode(PGraphics pg){
        // do not draw the right toggle handle
    }

    @Override
    public void mouseReleasedOverNode(float x, float y){
        if(armed && !valueBoolean){ // can only toggle manually to true, toggle to false happens automatically
            valueBoolean = true;
        }
        armed = false;
    }

    @Override
    public void drawLeftText(PGraphics pg, String text) {
        if(valueBoolean){
            pg.noStroke();
            pg.fill(ThemeStore.getColor(ThemeColorType.FOCUS_BACKGROUND));
            pg.rectMode(CORNER);
            pg.rect(0,0,size.x,size.y);
            pg.fill(ThemeStore.getColor(ThemeColorType.FOCUS_FOREGROUND));
            float rectSize = cell * 0.25f;
            pg.rectMode(CENTER);
            pg.rect(size.x - cell*0.5f, size.y - cell*0.5f, rectSize, rectSize);
        }else{
            fillForegroundBasedOnMouseOver(pg);
        }
        pg.textAlign(LEFT, CENTER);
        pg.text(text, State.textMarginX, size.y - State.font.getSize() * 0.6f);
    }
}
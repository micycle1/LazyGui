package test;

import processing.core.PApplet;
import processing.core.PGraphics;
import gui.Gui;

@SuppressWarnings("DuplicatedCode")
public class Template extends PApplet {
    Gui gui;
    PGraphics pg;

    public static void main(String[] args) {
        PApplet.main(java.lang.invoke.MethodHandles.lookup().lookupClass());
    }

    public void settings() {
        size(800, 800, P2D);
//        fullScreen(P2D);
    }

    public void setup() {
        gui = new Gui(this);
        pg = createGraphics(width, height, P2D);
    }

    public void draw() {
        pg.beginDraw();
        pg.noStroke();
        pg.image(gui.gradient("background"), 0, 0);
        drawScene();
        gui.applyPremadeFilters("filters", pg);
        pg.image(gui.gradient("overlay", 0), 0, 0);
        pg.endDraw();
        clear();
        image(pg, 0, 0);
        gui.draw();
        gui.record(pg);
    }

    private void drawScene() {

    }
}

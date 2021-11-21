package toolbox;

import com.jogamp.newt.opengl.GLWindow;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PSurface;

import java.awt.*;

import static processing.core.PConstants.P2D;


public class GlobalState {
    public static float cell = 24;

    public static PFont font = null;
    public static PApplet app = null;
    public static Gui gui;
    public static Robot robot;
    public static GLWindow window;
    public static String libraryPath;
    public static PGraphics colorProvider;

    public static void init(Gui gui, PApplet app){
        GlobalState.gui = gui;
        GlobalState.app = app;
        GlobalState.font = app.createFont("Calibri", 20);

        colorProvider = app.createGraphics(256,256, P2D);

        PSurface surface = GlobalState.app.getSurface();
        if (surface instanceof processing.opengl.PSurfaceJOGL) {
            window = (com.jogamp.newt.opengl.GLWindow) (surface.getNative());
        }

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        libraryPath = Utils.getLibraryPath();
    }

}

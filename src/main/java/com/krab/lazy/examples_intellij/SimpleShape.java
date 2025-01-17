package com.krab.lazy.examples_intellij;

import com.krab.lazy.LazyGui;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

public class SimpleShape extends PApplet {
    LazyGui gui;

    public static void main(String[] args) {
        PApplet.main(java.lang.invoke.MethodHandles.lookup().lookupClass());
    }

    public void settings() {
        size(22*41, 22*20, P2D);
        smooth(8);
    }

    public void setup() {
        gui = new LazyGui(this);
    }

    @SuppressWarnings("DuplicatedCode")
    public void draw() {
        gui.pushFolder("scene");
        drawBackground();
        drawForegroundShape();
        gui.popFolder();
    }

    private void drawForegroundShape() {
        gui.pushFolder("shape");
        String[] shapeTypeOptions = new String[]{
                "rectangle",
                "ellipse"
        };
        String selectedShape = gui.radio("shape type", shapeTypeOptions);
        PVector pos = gui.plotXY("position");
        PVector size = gui.plotXY("size", 250);
        translate(width/2f, height/2f);
        float rotationAngle = gui.slider("rotation");
        float rotationAngleDelta = gui.slider("rotation ++", 0.1f);
        gui.sliderSet("rotation", rotationAngle + rotationAngleDelta);
        fill(gui.colorPicker("fill", color(0xFF689FC8)).hex);
        gui.colorPickerHueAdd("fill", radians(gui.slider("fill hue ++", 0.1f)));
        stroke(gui.colorPicker("stroke").hex);
        strokeWeight(gui.slider("stroke weight", 10));
        if(gui.toggle("no stroke")){
            noStroke();
        }
        rectMode(CENTER);
        translate(pos.x, pos.y);
        rotate(radians(rotationAngle));
        if(selectedShape.equals("ellipse")){
            ellipse(0, 0, size.x, size.y);
        }else{
            rect(0, 0, size.x, size.y);
        }
        gui.popFolder();
    }

    private void drawBackground() {
        gui.pushFolder("background");
        boolean useGradient = gui.toggle("solid\\/gradient", false);
        int solidBackgroundColor = gui.colorPicker("solid", color(0xFF252525)).hex;
        PGraphics gradient = gui.gradient("gradient");
        if(useGradient){
            image(gradient, 0, 0);
        }else{
            background(solidBackgroundColor);
        }
        gui.popFolder();
    }
}

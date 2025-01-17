LazyGui gui;
PGraphics canvas;
PickerColor circleColor;
PickerColor lineColor;
float lineWeight = 40;
float circleSize = 75;

void setup() {
  size(800, 800, P2D);
  gui = new LazyGui(this);
  canvas = createGraphics(width, height);
  canvas.beginDraw();
  canvas.background(50);
  canvas.endDraw();
  noStroke();
}

void draw() {
  gui.pushFolder("drawing");
  circleColor = gui.colorPicker("circle color", color(0));
  circleSize = gui.slider("circle size", circleSize);
  lineColor = gui.colorPicker("line color", color(150, 255, 100));
  gui.colorPickerHueAdd("line color", radians(gui.slider("line hue +", 0.5f)));
  lineWeight = gui.slider("line weight", lineWeight);
  gui.popFolder();
  clear();
  image(canvas, 0, 0);
}

void mousePressed() {
  if (gui.isMouseOutsideGui()) {
    drawCircleAtMouse();
  }
}

void mouseReleased() {
  if (gui.isMouseOutsideGui()) {
    drawCircleAtMouse();
  }
}

void mouseDragged() {
  if (gui.isMouseOutsideGui()) {
    drawLineAtMouse();
  }
}

void drawCircleAtMouse() {
  canvas.beginDraw();
  canvas.noStroke();
  canvas.fill(circleColor.hex);
  canvas.ellipse(mouseX, mouseY, circleSize, circleSize);
  canvas.endDraw();
}

void drawLineAtMouse() {
  canvas.beginDraw();
  canvas.stroke(lineColor.hex);
  canvas.strokeWeight(lineWeight);
  canvas.line(pmouseX, pmouseY, mouseX, mouseY);
  canvas.endDraw();
}
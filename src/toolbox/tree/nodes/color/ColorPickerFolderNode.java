package toolbox.tree.nodes.color;

import processing.core.PGraphics;
import toolbox.GlobalState;
import toolbox.tree.nodes.FolderNode;

import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.RGB;

public class ColorPickerFolderNode extends FolderNode {

    public Color color = new Color();
    ColorPreviewNode previewNode;
    HexNode hexNode;
    HueNode hueNode;
    SaturationNode   satNode;
    BrightnessNode   brNode;
    AlphaNode        alphaNode;
    PrimaryColorNode redNode;
    PrimaryColorNode greenNode;
    PrimaryColorNode blueNode;

    public ColorPickerFolderNode(String path, FolderNode parentFolder) {
        super(path, parentFolder);

        previewNode = new ColorPreviewNode(path + "/preview", this);
        hexNode = new HexNode(path + "/hex", this);
        hueNode = new HueNode(path + "/hue", this);
        satNode = new SaturationNode(path + "/sat", this);
        brNode = new BrightnessNode(path + "/br", this);
        alphaNode = new AlphaNode(path + "/alpha", this);
        redNode = new PrimaryColorNode(path + "/red", this, PrimaryColor.RED);
        greenNode = new PrimaryColorNode(path + "/green", this, PrimaryColor.GREEN);
        blueNode = new PrimaryColorNode(path + "/blue", this, PrimaryColor.BLUE);
        children.add(previewNode);
        children.add(hexNode);
        children.add(hueNode);
        children.add(satNode);
        children.add(brNode);
        children.add(alphaNode);
        children.add(redNode);
        children.add(greenNode);
        children.add(blueNode);

    }

    @Override
    protected void updateDrawInlineNode(PGraphics pg) {
        strokeContentBasedOnFocus(pg);
        float previewRectSize = cell * 0.5f;
        pg.translate(size.x - cell * 0.5f, size.y * 0.5f);
        pg.rectMode(CENTER);
        pg.noFill();
        pg.translate(1, 1);
        pg.rect(0, 0, previewRectSize, previewRectSize);
        pg.fill(color.hex);
        pg.translate(-2, -2);
        pg.rect(0, 0, previewRectSize, previewRectSize);
    }

    public Color getColor() {
        return color;
    }

    public void initWithRGB(float r, float g, float b) {
        color.r = r;
        color.g = g;
        color.b = b;
        buildColorFromRGB();
    }

    void buildColorFromRGB(){
        PGraphics colorProvider = GlobalState.colorProvider;
        colorProvider.colorMode(RGB,1,1,1,1);
        int hex = colorProvider.color(color.r, color.g, color.b);
        color.hex = hex;
        color.hue = colorProvider.hue(hex);
        color.sat = colorProvider.saturation(hex);
        color.br = colorProvider.brightness(hex);
        color.alpha = 1;
    }
}

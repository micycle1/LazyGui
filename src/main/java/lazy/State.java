package lazy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static lazy.Utils.prettyPrintTree;
import static processing.core.PApplet.*;

class State {

    static float cell = 24;
    static float previewRectSize = cell * 0.6f;

    static PFont font = null;
    static PApplet app = null;
    static LazyGui gui = null;
    static PGraphics colorStore = null;

    private static final String fontPath = "JetBrainsMono-Regular.ttf";
    static private final int defaultFontSize = 16;
    static private int lastFontSize = -1;
    static float textMarginX = 5;
    static float textMarginY = 14;
    static boolean keepWindowsInBounds = true;

    static String sketchName = null;
    static boolean autosaveEnabled = false;
    private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
    static final float defaultWindowWidth = 10;
    private static ArrayList<File> saveFilesSorted;
    static Map<String, JsonElement> lastLoadedStateMap = new HashMap<>();
    static File saveDir;

    static ArrayList<String> undoStack = new ArrayList<>();
    static ArrayList<String> redoStack = new ArrayList<>();

    private static long lastFrameMillis;
    private static final long lastFrameMillisStuckLimit = 1000;
    private static final int undoStackSizeLimit = 1000;

    static void init(LazyGui gui, PApplet app) {
        State.gui = gui;
        State.app = app;

        tryUpdateFont(defaultFontSize, textMarginX, textMarginY);

        registerExitHandler();

        sketchName = app.getClass().getSimpleName();
        lazyInitSaveDir();

        colorStore = app.createGraphics(256, 256, P2D);
        colorStore.colorMode(HSB, 1, 1, 1, 1);
    }

    static void tryUpdateFont(int inputFontSize, float textMarginX, float textMarginY) {
        State.textMarginX = textMarginX;
        State.textMarginY = textMarginY;
        if(inputFontSize == lastFontSize){
            return;
        }
        try {
            State.font = app.createFont(fontPath, inputFontSize);
        } catch (RuntimeException ex) {
            if (ex.getMessage().contains("createFont() can only be used inside setup() or after setup() has been called")) {
                throw new RuntimeException("the new Gui(this) constructor can only be used inside setup() or after setup() has been called");
            }
        }
        lastFontSize = inputFontSize;
    }

    static int getLastFontSize(){
        return lastFontSize;
    }

    private static void lazyInitSaveDir() {
        saveDir = new File(State.app.sketchPath() + "/saves/" + sketchName);
        if (!saveDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            saveDir.mkdirs();
        }
    }

    private static void createTreeSaveFiles(String filenameWithoutSuffix) {
        // save main json
        String jsonPath = getFullPathWithSuffix(filenameWithoutSuffix, ".json");
        overwriteFile(jsonPath, getTreeAsJsonString());
//        println("Saved current state to: " + jsonPath);

        // save pretty printed preview
        String treeViewNotice = "NOTICE: This file contains a preview of the tree found in the json next to it." +
                "\n\t\tDo not edit this file, any changes you make will probably be overwritten." +
                "\n\t\tEdit or delete the corresponding json file instead to change or erase the saved values." +
                "\n\t\tYou can find it here: " + jsonPath + "\n\n";
        String prettyPrintPath = getFullPathWithSuffix(filenameWithoutSuffix, ".txt");
        String prettyTree = prettyPrintTree();
        overwriteFile(prettyPrintPath, treeViewNotice + prettyTree);
//        println("Saved current state preview to: " + prettyPrintPath);

        gui.requestScreenshot(getFullPathWithSuffix(filenameWithoutSuffix, ".jpg"));
    }

    static void loadMostRecentSave() {
        reloadSaveFolderContents();
        if (saveFilesSorted.size() > 0) {
            loadStateFromFile(saveFilesSorted.get(0));
        }
    }


    private static void reloadSaveFolderContents() {
        lazyInitSaveDir();
        File[] saveFiles = saveDir.listFiles();
        assert saveFiles != null;
        saveFilesSorted = new ArrayList<>(Arrays.asList(saveFiles));
        saveFilesSorted.removeIf(file -> !file.isFile() || !file.getAbsolutePath().contains(".json"));
        if (saveFilesSorted.size() == 0) {
            return;
        }
        saveFilesSorted.sort((o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
    }

    static void loadStateFromFile(String filename) {
        for (File saveFile : saveFilesSorted) {
            if (saveFile.getName().equals(filename)) {
                loadStateFromFile(saveFile);
                return;
            }
        }
    }

    private static String readFile(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
        }
        return sb.toString();
    }

    private static String getFullPathWithSuffix(String filenameWithoutSuffix, String suffix) {
        return getFullPathWithoutTypeSuffix(filenameWithoutSuffix + suffix);
    }

    private static String getFullPathWithoutTypeSuffix(String filenameWithSuffix) {
        return saveDir.getAbsolutePath() + "\\" + filenameWithSuffix;
    }

    static void overwriteFile(String fullPath, String content) {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(fullPath, false));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getTreeAsJsonString() {
        return gson.toJson(NodeTree.getRoot());
    }

    static ArrayList<File> getSaveFileList() {
        reloadSaveFolderContents();
        return saveFilesSorted;
    }

    static void loadStateFromFile(File file) {
        if (!file.exists()) {
            println("Error: save file doesn't exist");
            return;
        }
        String json;
        try {
            json = readFile(file);
        } catch (IOException e) {
            println("Error loading state from file", e.getMessage());
            return;
        }
        JsonElement root = gson.fromJson(json, JsonElement.class);
        loadStateFromJsonElement(root);
    }

    static void loadStateFromJsonElement(JsonElement root) {
        lastLoadedStateMap.clear();
        Queue<JsonElement> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            JsonElement loadedNode = queue.poll();
            String loadedPath = loadedNode.getAsJsonObject().get("path").getAsString();
            AbstractNode nodeToEdit = NodeTree.findNode(loadedPath);
            if (nodeToEdit != null) {
                overwriteWithLoadedStateIfAny(nodeToEdit, loadedNode);
            }
            lastLoadedStateMap.put(loadedPath, loadedNode);
            String loadedType = loadedNode.getAsJsonObject().get("type").getAsString();
            if (Objects.equals(loadedType, NodeType.FOLDER.toString())) {
                JsonArray loadedChildren = loadedNode.getAsJsonObject().get("children").getAsJsonArray();
                for (JsonElement child : loadedChildren) {
                    queue.offer(child);
                }
            }
        }
    }

    static void overwriteWithLoadedStateIfAny(AbstractNode abstractNode) {
        overwriteWithLoadedStateIfAny(abstractNode, lastLoadedStateMap.get(abstractNode.path));
    }

    static void overwriteWithLoadedStateIfAny(AbstractNode abstractNode, JsonElement loadedNodeState) {
        if (loadedNodeState == null) {
            return;
        }
        abstractNode.overwriteState(loadedNodeState);
    }

    private static void registerExitHandler() {
        Runtime.getRuntime().addShutdownHook(new Thread(State::createAutosave));
    }

    static void createAutosave() {
        if(!autosaveEnabled){
            return;
        }
        if (isSketchStuckInEndlessLoop()) {
            println("NOT autosaving," +
                    " because the last frame took more than " + lastFrameMillisStuckLimit + " ms," +
                    " which looks like the program stopped due to an exception or reached an endless loop");
            return;
        }
        createTreeSaveFiles("auto");
    }

    static void updateEndlessLoopDetection() {
        lastFrameMillis = app.millis();
    }

    static boolean isSketchStuckInEndlessLoop() {
        long timeSinceLastFrame = app.millis() - lastFrameMillis;
        return timeSinceLastFrame > lastFrameMillisStuckLimit;
    }

    static void onUndoableActionEnded() {
        pushToUndoStack();
    }

    static void undo() {
        popFromUndoStack();
    }

    static void redo() {
        popFromRedoStack();
    }

    private static void pushToUndoStack() {
        undoStack.add(getTreeAsJsonString());
        while (undoStack.size() > undoStackSizeLimit) {
            undoStack.remove(0);
        }
        redoStack.clear();
    }

    private static void popFromUndoStack() {
        if (undoStack.isEmpty()) {
            return;
        }
        String poppedJson = undoStack.remove(undoStack.size() - 1);
        redoStack.add(poppedJson);
        loadStateFromJsonElement(gson.fromJson(poppedJson, JsonElement.class));
    }

    private static void popFromRedoStack() {
        if (redoStack.isEmpty()) {
            return;
        }
        String poppedJson = redoStack.remove(redoStack.size() - 1);
        undoStack.add(poppedJson);
        loadStateFromJsonElement(gson.fromJson(poppedJson, JsonElement.class));
    }

    static void createNewSaveWithRandomName() {
        String newName = Utils.generateRandomShortId();
        State.createTreeSaveFiles(newName);
    }

    public static void setCellSize(float inputCellSize) {
        cell = inputCellSize;
        previewRectSize = cell * 0.6f;
    }



    static void setKeepWindowsInBounds(boolean value) {
        keepWindowsInBounds = value;
    }
}

package toolbox.global;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

import java.net.URL;
import java.nio.file.Paths;

import static processing.core.PApplet.*;

public class Utils {
    public static boolean isPointInRect(float x, float y, PVector pos, PVector size) {
        return isPointInRect(x, y, pos.x, pos.y, size.x, size.y);
    }

    public static boolean isPointInRect(float px, float py, float rx, float ry, float rw, float rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    /**
     * Takes any float and returns the positive fractional part of it, so the result is always between 0 and 1.
     * For example -0.1 becomes 0.1 and 1.5 becomes 0.5. Used with hue due to its cyclical
     * nature.
     *
     * @param hue float to apply modulo to
     * @return float in the range [0,1)
     */
    protected static float hueModulo(float hue) {
        while (hue < 0) {
            hue += 1;
        }
        hue %= 1;
        return hue;
    }

    /**
     * Returns the number of digits in a floored number. Useful for approximating the most useful default precision
     * of a slider.
     *
     * @param inputNumber number to floor and check the size of
     * @return number of digits in floored number
     */
    public static int numberOfDigitsInFlooredNumber(float inputNumber) {
        return String.valueOf(Math.floor(inputNumber)).length();
    }

    /**
     * A random function that always returns the same number for the same seed.
     *
     * @param seed seed to use
     * @return hash value between 0 and 1
     */
    protected static  float hash(float seed) {
        return (float) (Math.abs(Math.sin(seed * 323.121f) * 454.123f) % 1);
    }


    /**
     * Constructs a random square image url with the specified size.
     *
     * @param size image width to request
     * @return random square image
     */
    public static String randomImageUrl(float size) {
        return randomImageUrl(size, size);
    }

    /**
     * Constructs a random image url with the specified size.
     *
     * @param width  image width to request
     * @param height image height to request
     * @return random image url
     */
    public static String randomImageUrl(float width, float height) {
        return "https://picsum.photos/" + Math.floor(width) + "/" + Math.floor(height) + ".jpg";
    }

    protected static float clampNorm(float x, float min, float max) {
        return constrain(PApplet.norm(x, min, max), 0, 1);
    }

    @SuppressWarnings("unused")
    protected static float clampMap(float x, float xMin, float xMax, float min, float max) {
        return constrain(map(x, xMin, xMax, min, max), min, max);
    }

    /**
     * Returns the angular diameter of a circle with radius 'r' on the edge of a circle with radius 'size'.
     *
     * @param r    the radius of the circle to check the angular diameter of
     * @param size the radius that the circle rests on the edge of
     * @return angular diameter of r at radius size
     */
    public static float angularDiameter(float r, float size) {
        return (float) Math.atan(2 * (size / (2 * r)));
    }


    public static String getLibraryPath() {
        URL url = State.app.getClass().getResource(State.class.getSimpleName() + ".class");
        if (url != null) {
            // Convert URL to string, taking care of spaces represented by the "%20"
            // string.
            String path = url.toString().replace("%20", " ");

            if (!path.contains(".jar"))
                return State.app.sketchPath();

            int n0 = path.indexOf('/');

            int n1;

            // read jar file name
            String fullJarPath = Utils.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();

            if (PApplet.platform == PConstants.WINDOWS) {
                // remove leading slash in windows path
                fullJarPath = fullJarPath.substring(1);
            }

            String jar = Paths.get(fullJarPath).getFileName().toString();

            n1 = path.indexOf(jar);
            if (PApplet.platform == PConstants.WINDOWS) {
                // remove leading slash in windows path
                n0++;
            }


            if ((-1 < n0) && (-1 < n1)) {
                return path.substring(n0, n1);
            } else {
                return State.app.sketchPath();
            }
        }
        return State.app.sketchPath();
    }

}
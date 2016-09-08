
package guns.ai;

public interface Config {
    public static final int WIDTH = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
    public static final int HEIGHT = (int) Math.round(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height * .80);
    public static final int PIXELS = WIDTH * HEIGHT;
    
    public static final int BITMAP_CELL_DIMENSION = 30; // dimension in pixels ... creates (WIDTH / BCP) * (HEIGHT / BCP) cells. don't change
    public static final int BITMAP_INTERPOLATION_STEP = 2; // interpolation step in pixels, less = more accurate but more expensive. don't change
    
    public static final int UPDATE_DELAY = 17; // ~58 - 59 updates per second
    
    public static enum GameState {play, win, end};
    public static enum EntityType {LocalPlayer, TerrainObject, Actor, Bullet};
    public static enum Direction {Up, Down, Left, Right};
    
    // DEBUG VARS
    public static final boolean BITMAP_DEBUG_MODE = false;
    
}

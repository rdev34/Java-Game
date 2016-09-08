
package guns.ai;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public abstract class Actor implements Config
{
    public int health;
    public int id;
    public BufferedImage pic;
    public int img_width, img_height;
    public double angle;
    public Vector2D position;
    public boolean dead = false;
    
    public Actor(String path)
    {
        try
        {
            pic = ImageIO.read(getClass().getClassLoader().getResource(("images/" + path)));
            img_width = pic.getWidth();
            img_height = pic.getHeight();
        }
        catch (IOException e)
        {
            throw new Error("IO Error - Could Not Load Actor Image File (" + path + ")");
        }
        
        angle = 0;
        id = IdPool.generateId(this);
        position = Game.terrain.getRandomPosition(img_width + BITMAP_CELL_DIMENSION, img_height + BITMAP_CELL_DIMENSION);
        Game.terrain.populate(id, EntityType.Actor, position, img_width, img_height);
    }
    
    
    abstract public void update();
    abstract public void render(Graphics g);
}

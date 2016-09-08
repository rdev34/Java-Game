
package guns.ai;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class TerrainObject implements Config // standard static terrain object
{
    public BufferedImage pic;
    public int width, height;
    public Vector2D position;
    public int id;
    
    public TerrainObject(String path, Vector2D arg_position)
    {
        try
        {
            pic = ImageIO.read(getClass().getClassLoader().getResource(("images/" + path)));
            width = pic.getWidth();
            height = pic.getHeight();
        }
        catch (IOException e)
        {
            throw new Error("IO Error - Could Not Load TerrainObject (" + path + ")");
        }
        
        position = arg_position;
        
        id = IdPool.generateId(this);
        
        Game.terrain.populate(id, EntityType.TerrainObject, position, width, height);
    }
    
    public void render(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform transform = AffineTransform.getTranslateInstance(position.x, position.y);
        transform.scale(1.05, 1.05); // make the image a bit bigger to make up for the empty space due to rectangular space
        g2.drawImage(pic, transform, null);
        //g.drawImage(pic, position.x, position.y, null);
    }
    
    public String toString()
    {
        return "TerrainObject";
    }
}

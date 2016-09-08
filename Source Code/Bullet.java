
package guns.ai;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;


public class Bullet implements Config{
    
    public BufferedImage pic;
    private int width, height;
    public Vector2D position;
    public Vector2D old_position;
    public Vector2D start;
    public Vector2D end;
    public HashMap path;
    public double angle;
    public double progress = 0;
    public int id;
    public boolean remove = false;
    
    public Bullet()
    {
        position = new Vector2D();
        angle = 0;
        id = IdPool.generateId(this);
    }
    
    public Bullet(Vector2D arg_position, double arg_angle)
    {
        position = arg_position;
        angle = arg_angle;
        start = new Vector2D(arg_position);
        end = calculateEnd();
        id = IdPool.generateId(this);
        
        try
        {
            pic = ImageIO.read(getClass().getClassLoader().getResource(("images/Bullet3.PNG")));
            width = pic.getWidth();
            height = pic.getHeight();
        }
        catch (IOException e)
        {
            throw new Error("IO Error - Could Not Load Image File Bullet1.PNG");
        }
        
    }
    
    public Vector2D calculateEnd()
    {
        //Vector2D test = new Vector2D(start);
        double x = (double)start.x;
        double y = (double)start.y;
        //System.out.println("Angle: " + angle);
        //System.out.println("start: " + start);
        while ((int)x > 0 && (int)y > 0 && (int)x <= WIDTH && (int)y <= HEIGHT)
        {
            x = x + (5 * Math.cos(angle - (Math.PI / 2)));
            y = y + (5 * Math.sin(angle - (Math.PI / 2)));
        }
        
        return new Vector2D((int)Math.round(x), (int)Math.round(y));
    }
    
    public void update()
    {
        if (!remove)
        {
            old_position = new Vector2D(position);
            progress = progress + (PIXELS / 150000);
            position = start.plus(new Vector2D((int)Math.round(progress * Math.cos(angle - (Math.PI / 2))), (int)Math.round(progress * Math.sin(angle - (Math.PI / 2)))));

            if (position.onScreen())
            {
                boolean change = Game.terrain.populate(id, EntityType.Bullet, position, width, height);
                if (!change) 
                {
                    position = old_position;
                    remove = true;
                }
                // calculate new positions from start position, not current position, otherwise double truncation causes massive loss of angle accuracy
                Game.gui_change = true;
            }
            else
            {
                remove = true;
                //System.out.println("Bullet Remove Queued");
            }
        }
        
    }
    
    public void render(Graphics g)
    {
        if (!remove && position.onScreen())
        {
            Graphics2D g2 = (Graphics2D) g;
            AffineTransform transform = AffineTransform.getTranslateInstance(position.x, position.y);
            transform.rotate(angle, width / 2, height / 2); // rotate around center of image
        
            g2.drawImage(pic, transform, null);
        }
    }
    
    public String toString()
    {
        return "Bullet";
    }
}

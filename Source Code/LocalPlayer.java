
package guns.ai;
import static guns.ai.Config.UPDATE_DELAY;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;

public class LocalPlayer implements Config 
{
    
    public static BufferedImage player;
    private static final double pi = Math.PI; // improved access speed to class local
    public static Vector2D position;
    public static int id;
    public static int img_width, img_height;
    public static boolean dir[] = {false, false, false, false};
    public static boolean firing = false;
    private static double angle = 0;
    private static Timer fire_delay;
    private static boolean can_shoot = false;
    public static int health = 100;
    
    public LocalPlayer()
    {
        
        try
        {
            //player = ImageIO.read(getClass().getClassLoader().getResource(("images/LC6.PNG")));
            player = ImageIO.read(getClass().getResourceAsStream("/images/LC6.png"));
            img_width = player.getWidth();
            img_height = player.getHeight();
        }
        catch (IOException e)
        {
            throw new Error("IO Error - Could Not Load Image File LC6.PNG");
        }
        
        id = IdPool.generateId(this);
        position = new Vector2D(900, 400);
        
        Game.terrain.populate(id, EntityType.LocalPlayer, position, img_width, img_height);
        
        ActionListener task = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                can_shoot = true;
            }
        };
        
        fire_delay = new Timer(200, task);
        fire_delay.start();
    }
    

    public static void render(Graphics g) // not called every frame
    {
        //g.fillOval(position.x, position.y, 5, 5);
       // g.fillOval(position.x + player.getWidth(), position.y, 5, 5);
       // g.fillOval(position.x, position.y + player.getHeight(), 5, 5);
       // g.fillOval(position.x + player.getWidth(), position.y + player.getHeight(), 5, 5);
        
        g.setFont(new Font("Courier", Font.BOLD, 20));
        g.drawString("" + health, position.x, position.y);
        
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform transform = AffineTransform.getTranslateInstance(position.x, position.y);
        
        
        transform.rotate(angle, img_width / 2, img_height / 2); // rotate around center of image
        
        //int dimension = 25;
        //g2.fillOval(position.x, position.y, dimension, dimension);
        //g2.fillOval(position.x + player.getWidth(), position.y, dimension, dimension);
        //g2.fillOval(position.x, position.y + player.getHeight(), dimension, dimension);
        //g2.fillOval(position.x + player.getWidth(), position.y + player.getHeight(), dimension, dimension);
        
        //g.fillRect(100, 100, dimension, dimension);
        
        g2.drawImage(player, transform, null);
        
      
        //g.fillOval(getBarrelPosition().x, getBarrelPosition().y, 5, 5);
       
        //System.out.println("Angle: " + angle);
        
    }
    
    public static void update()
    {
        /// Health
        if (health <= 0) 
        {
            Game.status = GameState.end;
        }
        // Position and Angle Handling
        Vector2D old_position = new Vector2D(position);
        boolean map_update = false;
        
        if (dir[0])
        {
            position.y = position.y - 2;
            map_update = true;
        }
        if (dir[1])
        {
            position.y = position.y + 2;
            map_update = true;
        }
        if (dir[2])
        {
            position.x = position.x - 2;
            map_update = true;
        }
        if (dir[3])
        {
            position.x = position.x + 2;
            map_update = true;
        }
        
        double old_angle = angle;
        angle = Math.atan2(Game.mouse_position.y - (position.y + img_height / 2), Game.mouse_position.x - (position.x + img_width / 2)) + (Math.PI / 2);
        
        if (map_update)
        {
            boolean change = Game.terrain.populate(id, EntityType.LocalPlayer, position, img_width, img_height);
            if (!change) position = old_position;
            Game.gui_change = true;
        }
        
        if (old_angle != angle)
        {
            Game.gui_change = true;
        }
        
        if (firing && can_shoot == true)
        {
            can_shoot = false;
            Game.spawnBullet(getBarrelPosition(), angle + (new Random().nextInt() == 0 ? 1 : -1 * (new Random().nextDouble()) / 20));
            fire_delay.restart();
        }
        
    }
    
    public static Vector2D getBarrelPosition()
    {
        int quadrant = getQuadrant(angle);
        
        if (quadrant == 4)
        {
            return new Vector2D((int)((position.x - 3 + img_width / 2) + (20 * Math.cos(angle - (Math.PI / 2)))), (int)((position.y + img_height / 2) + (20 * Math.sin(angle - (Math.PI / 2)))));
        }
        else if (quadrant == 3)
        {
            return new Vector2D((int)((position.x - 4 + img_width / 2) + (20 * Math.cos(angle - (Math.PI / 2)))), (int)((position.y - 2 + img_height / 2) + (20 * Math.sin(angle - (Math.PI / 2)))));
        }
        else if (quadrant == 2)
        {
            return new Vector2D((int)((position.x + 1 + img_width / 2) + (20 * Math.cos(angle - (Math.PI / 2)))), (int)((position.y - 5 + img_height / 2) + (20 * Math.sin(angle - (Math.PI / 2)))));
        }
            
        return new Vector2D((int)((position.x + img_width / 2) + (20 * Math.cos(angle - (Math.PI / 2)))), (int)((position.y + img_height / 2) + (20 * Math.sin(angle - (Math.PI / 2)))));
    }
    
    public static int getQuadrant(double ang)
    {
        int q = 0;
        
        if (ang > 0 && ang <= 1.5707)
        {
            q = 1;
        }
        else if (ang < 0 && ang > -1.5707)
        {
            q = 2;
        }
        else if (ang > 1.5707 && ang <= 3.1415)
        {
            q = 4;
        }
        else if (ang <= 4.72 && ang > 3.1415)
        {
            q = 3;
        }
        
        return q;
    }

}
           

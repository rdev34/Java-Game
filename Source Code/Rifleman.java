
package guns.ai;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Random;
import javax.swing.Timer;

public class Rifleman extends Actor implements Config
{
    public HashMap<Integer, Vector2D> path;
    public int current_node = 0;
    private Direction dir;
            
    public boolean pathing = false;
    public boolean shooting = false;
    
    private int raycast_ticks = 0;
    private int path_request_ticks = 0;
    private int fail_move_ticks = 0;
    
    private int ticks = 0;
    private Timer fire_delay;
    private boolean can_shoot = false;
    
    public Rifleman()
    {
        super("rifleman3.PNG");
        System.out.println("id: " + id);
        health = 100;
        
        ActionListener task = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                can_shoot = true;
            }
        };
        
        fire_delay = new Timer(200, task);
        fire_delay.start();
    }
    
    @Override
    public void update()
    {
        if (health <= 0)
        {
            dead = true;
        }
        
        Vector2D old_position = new Vector2D(position);
        double old_angle = angle;
        raycast_ticks++; path_request_ticks++;
        
        if (raycast_ticks > 10)
        {
            raycast_ticks = 0;
            shooting = Game.terrain.lineOfSight(position.x + (img_width / 2), position.y + (img_height / 2), LocalPlayer.position.x + (LocalPlayer.img_width / 2), LocalPlayer.position.y + (LocalPlayer.img_height / 2));
            System.out.println("LoS: " + shooting);
        }
        
        if (shooting == true)
        {
            angle = Math.atan2(LocalPlayer.position.y - (position.y + img_height / 2), LocalPlayer.position.x - (position.x + img_width / 2)) + (Math.PI / 2);
            
            if (can_shoot)
            {
                Game.spawnBullet(getBarrelPosition(), angle + (new Random().nextInt() == 0 ? 1 : -1 * (new Random().nextDouble()) / 20));
                can_shoot = false;
            }
            
            path = null;
            pathing = false;
            current_node = 0;
        }
        else if (pathing == false)
        {
            if (path_request_ticks > 20 && !shooting)
            {
                path = null;
                Vector2D[] possible_goals = Game.terrain.getBitmapNeighbors(position);
                Vector2D[] possible_starts = Game.terrain.getBitmapNeighbors(LocalPlayer.position);
                
                iteration:
                for (int i = 0; i < possible_starts.length; i++)
                {
                    for (int ii = 0; ii < possible_goals.length; ii++)
                    {
                        path = Game.terrain.findPath(possible_starts[i], possible_goals[ii]);
                        if (path != null)
                        {
                            break iteration;
                        }
                    }
                }
                
                
                if (path != null)
                {
                    pathing = true;
                    current_node = 0;
                }
                
                path_request_ticks = 0;
            }
            
        }
        else if (pathing == true)
        {
            System.out.println("Pathing (executing path)");
            ticks++;
            if (ticks > 0)
            {   
                ticks = 0;
                double distance_to_next = Vector2D.distance(position, path.get(current_node));
                //System.out.println("distance_to_next: " + distance_to_next);
                if (distance_to_next < 2)
                {
                    current_node++;
                    if (path.get(current_node) == null)
                    {
                        pathing = false;
                    }
                    else
                    {
                        Vector2D next_pos = new Vector2D(path.get(current_node));
                        angle = Math.atan2(next_pos.y + (BITMAP_CELL_DIMENSION / 2) - (position.y + img_height / 2), next_pos.x + (BITMAP_CELL_DIMENSION / 2) - (position.x + img_width / 2)) + (Math.PI / 2);
                        //System.out.println("New Node: " + current_node);
                    }
                }
                
                if (pathing == true)
                {
                    Vector2D target_pos = new Vector2D(path.get(current_node));
                    //angle = Math.atan2(target_pos.y - (position.y + img_height / 2), target_pos.x - (position.x + img_width / 2)) + (Math.PI / 2);
                    //angle = Math.atan2(next_pos.y - (position.y + img_height / 2), next_pos.x - (position.x + img_width / 2)) + (Math.PI / 2);
                    //System.out.println("angle: " + angle);
                    dir = getDirection(position, target_pos);
                    if (fail_move_ticks > 1)
                    {
                        dir = getRandomDirection();
                        fail_move_ticks = 0;
                    }

                    //System.out.println("Dir: " + dir);
                    if (dir != null)
                    {
                        if (dir == Direction.Up)
                        {
                            position.y = position.y - 1;
                        }
                        else if (dir == Direction.Down)
                        {
                            position.y = position.y + 1;
                        }
                        else if (dir == Direction.Left)
                        {
                            position.x = position.x - 1;
                        }
                        else if (dir == Direction.Right)
                        {
                            position.x = position.x + 1;
                        }
                    }
                    else
                    {
                        System.out.println("Direction is null");
                    }

                    //System.out.println("Angle: " + angle);

                    boolean change = Game.terrain.populate(id, EntityType.Actor, position, img_width, img_height);
                    if (!change)
                    {
                        fail_move_ticks++;
                        System.out.println("Failed to populate new position");
                        position = old_position;
                    }
                    else
                    {
                        fail_move_ticks = 0;
                    }

                }

            }
        }
        
        if (!old_position.equals(position) || old_angle != angle)
        {
            Game.gui_change = true;
        }
    }
    
    private Direction getRandomDirection()
    {
        Integer rand = new Random().nextInt(3);
        if (rand == 0) return Direction.Up;
        if (rand == 1) return Direction.Down;
        if (rand == 2) return Direction.Left;
        if (rand == 3) return Direction.Right;
        return Direction.Up;
    }
    
    @Override
    public void render(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform transform = AffineTransform.getTranslateInstance(position.x, position.y);

        transform.rotate(angle, img_width / 2, img_height / 2); // rotate around center of image
        //transform.scale(1.1, 1.4);
        
        g2.drawImage(pic, transform, null);
        
        g.setFont(new Font("Courier", Font.BOLD, 20));
        g.drawString("" + health, position.x, position.y);
        
        if (path != null && 1 == 2)
        {
            Vector2D old_position = position;
            Vector2D new_pos = new Vector2D();
            for (int i = 0; i < path.size(); i++)
            {
                new_pos = path.get(i);
                g.drawLine(old_position.x, old_position.y, new_pos.x, new_pos.y);
                old_position = new_pos;
            }
        }
    }
    
    public Direction getDirection(Vector2D start, Vector2D end)
    {
        if (end.x > start.x)
        {
            return Direction.Right;
        }
        else if (end.x < start.x)
        {
            return Direction.Left;
        }
        else if (end.y > start.y)
        {
            return Direction.Down;
        }
        else if (end.y < start.y)
        {
            return Direction.Up;
        }
        
        return null;
        
    }
    
    public Vector2D getBarrelPosition()
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
    
    public int getQuadrant(double ang)
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

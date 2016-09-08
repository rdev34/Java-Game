
package guns.ai;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;


public class World implements Config
{
    public BufferedImage background;
    public int rock_count = 0, tree_count = 0;
    public int rock_width, rock_height, tree_width, tree_height;
    public HashMap<Integer, TerrainObject> world_objects = new HashMap<Integer, TerrainObject>(500, (float).85);
    public HashMap<Integer, Actor> actors = new HashMap<Integer, Actor>(35);
    private int hash_counter = 0;
    
    
    public World(int difficulty)
    {
        loadImages();
        
        // Spawn Rocks
        int failed_search = 0;
        for (int i = 0; i < 40; i++)
        {
            Vector2D position = Game.terrain.getRandomPosition(rock_width, rock_height); // heuristic call
            
            if (position != null)
            {
                world_objects.put(hash_counter, new TerrainObject("rock.png", position));
                hash_counter++;
                rock_count++;
            }
            else
            {
                failed_search++;
                if (failed_search == 3) break;
            }
        }
        
        // Spawn Trees
        failed_search = 0;
        for (int i = 0; i < 40; i++)
        {
            Vector2D position = Game.terrain.getRandomPosition(tree_width, tree_height); // heuristic call
            
            if (position != null)
            {
                world_objects.put(hash_counter, new TerrainObject("tree.png", position));
                hash_counter++;
                tree_count++;
            }
            else
            {
                failed_search++;
                if (failed_search == 3) break;
            }
        }
        
        System.out.println("Trees in World: " + tree_count);
        
        // Spawn AI
        for (int i = 0; i < 5; i++)
        {
            Actor new_actor = new Rifleman();
            actors.put(new_actor.id, new_actor);
        }
        
        Game.gui_change = true;
    }
    
    public void loadImages()
    {
        try
        {
            BufferedImage pic = ImageIO.read(getClass().getClassLoader().getResource(("images/rock.png")));
            rock_width = pic.getWidth();
            rock_height = pic.getHeight();
            
            pic = ImageIO.read(getClass().getClassLoader().getResource(("images/tree.png")));
            tree_width = pic.getWidth();
            tree_height = pic.getHeight();
            
        }
        catch (IOException e)
        {
            throw new Error("IO Error - Could Not Load TerrainObject (" + "rock.png" + ")");
        }
    }
    
    public void update()
    {
        
        Iterator iterator = actors.entrySet().iterator();
        int[] removes = new int[10];
        int remove_count = 0;

        while (iterator.hasNext()) 
        {
            Map.Entry pair = (Map.Entry) iterator.next();
            Actor actor = (Actor) pair.getValue();
            
            if (actor.dead)
            {
                removes[remove_count] = actor.id;
                remove_count++;
            }
            else
            {
                actor.update();
            }
        }
        
        
        for (int i = 0; i < remove_count; i++)
        {
            actors.remove(removes[i]);
        } 
    }
    
    public void render(Graphics g)
    {
        for (int i = 0; i < hash_counter; i++) // render terrain
        {
            world_objects.get(i).render(g);
        }
        
        Iterator iterator = actors.entrySet().iterator();

        while (iterator.hasNext()) 
        {
            Map.Entry pair = (Map.Entry) iterator.next();
            Actor actor = (Actor) pair.getValue();
            actor.render(g);

        }
   
    }
    
    
}

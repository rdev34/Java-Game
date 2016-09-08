
package guns.ai;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.List;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.swing.Timer;


public class Terrain implements Config {
    public HashMap<Integer, TerrainEntry> map;
    public HashMap<Vector2D, Cell> bitmap;
    public Vector2D[][] bitmap_key_reference;
    // internal size re-hashing occurs if initial capacity * load factor == size
    // set a high initial capacity to avoid re-hashing
    // this HashMap is frequently added to
    // ^ re-hashing, especially with thousands of objects, is extremely expensive and can noticably freeze the application
    // ^ re-hashing occurs when HashMap size == initial size * loading factor
    // ^ results in doubled initial size, HashMap continually doubles when size == initial size * loading factor
    // HashMap allows null entries (de-referenced Bullets), which should greatly diminish re-hashing cost
    
    public HashMap<Integer, Vector2D> path;
    public HashMap<Integer, Vector2D> rays;
    
    private Timer timer;
    private boolean pathed = false;
    
    public Terrain()
    {
        map = new HashMap<Integer, TerrainEntry>(200, (float).95);
        bitmap = new HashMap<Vector2D, Cell>((int)((WIDTH / BITMAP_CELL_DIMENSION) * (HEIGHT / BITMAP_CELL_DIMENSION) * 1.02), (float).99); // re-hash should not ever occur
        bitmap_key_reference = new Vector2D[(int)(WIDTH * 1.06)][(int)(HEIGHT * 1.06)];
        // bitmap_key_reference is necessary because we can't .get() values from bitmap with positions because the key is an object, therefore, we store the bitmap keys by their positions in another table
        
        // create all the cells
        for (int w = 0; w <= WIDTH + BITMAP_CELL_DIMENSION; w = w + BITMAP_CELL_DIMENSION)
        {
            for (int h = 0; h <= HEIGHT + BITMAP_CELL_DIMENSION; h = h + BITMAP_CELL_DIMENSION)
            {
                Vector2D vec = new Vector2D(w, h);
                bitmap.put(vec, new Cell(vec));
                bitmap_key_reference[vec.x][vec.y] = vec;
            }
        }
        
        // deduce the neighbors of all the cells
        Iterator iterator = bitmap.entrySet().iterator();

        while (iterator.hasNext())
        {
            Map.Entry pair = (Map.Entry)iterator.next();
            Cell cell = (Cell)pair.getValue();
            cell.neighbors = getCellNeighbors(cell);
        }
        
        System.out.println("Bitmap size: " + bitmap.size());
        
        //path = findPath(getCellKey(new Vector2D(BITMAP_CELL_DIMENSION, BITMAP_CELL_DIMENSION)), getCellKey(new Vector2D(BITMAP_CELL_DIMENSION * 110, BITMAP_CELL_DIMENSION * 40)));
        //System.out.println("path size: " + path.size());
        
    }
    
    public boolean populate(int id, EntityType type, Vector2D position, int width, int height)
    {
        TerrainEntry entity = map.get(id);
        HashMap<Cell, Boolean> cells_to_occupy = new HashMap<Cell, Boolean>();
        int cell_counter = 0;
        boolean valid_transfer = true;
        
        if (entity != null)
        {
            Vector2D old_position = new Vector2D(entity.position);
            entity.rectangle.setLocation(position.x, position.y);
            
            iteration: // loop identifier to break nested loop with unconditional jump
            for (int x = position.x; x < position.x + width; x = x + BITMAP_INTERPOLATION_STEP) // determine if move is valid and store cells to occupy
            {
                for (int y = position.y; y < position.y + height; y = y + BITMAP_INTERPOLATION_STEP)
                {
                    Cell cell = bitmap.get(getCellKey(new Vector2D(x, y)));
                    
                    if (cell == null)
                    {
                        valid_transfer = false;
                        break iteration;
                    }
                    
                    if (cell.isForSale(id)) 
                    {
                        cells_to_occupy.put(cell, true);
                        cell_counter++;
                    }
                    else
                    {
                        //System.out.println("is not for sale");
                        valid_transfer = false;
                        break iteration;
                    }
                }
            }
            
            if (valid_transfer)
            {
                //System.out.println("Old P: " + entity.position + " ... new P: " + position + " .. total equality: " + (position == entity.position));
                for (int x = entity.position.x; x < entity.position.x + entity.width; x = x + BITMAP_INTERPOLATION_STEP) // clean out old occupation (recommend storing these cells on TerrainEntry object instead of re-calculating them)
                {
                    for (int y = entity.position.y; y < entity.position.y + entity.height; y = y + BITMAP_INTERPOLATION_STEP)
                    {
                        Cell cell = bitmap.get(getCellKey(new Vector2D(x, y)));
                        cell.removeOccupant(id);
                    }
                }
                
                
                entity.position = new Vector2D(position); // update position with new position object so we can track changes for next call
                
                Iterator iterator = cells_to_occupy.entrySet().iterator(); // populate the new cells
                while (iterator.hasNext()) 
                {
                    Map.Entry pair = (Map.Entry)iterator.next();
                    Cell cell = (Cell)pair.getKey();
                    cell.addOccupant(id);
                }
            }
            else
            {
                entity.rectangle.setLocation(old_position.x, old_position.y);
            }
            
        }
        else
        {
            map.put(id, new TerrainEntry(type, new Vector2D(position), width, height, id)); // create new position object so we can track changes in position for the bitmap
            
            iteration:
            for (int x = position.x; x < position.x + width; x = x + BITMAP_INTERPOLATION_STEP)
            {
                for (int y = position.y; y < position.y + height; y = y + BITMAP_INTERPOLATION_STEP)
                {
                    Cell cell = bitmap.get(getCellKey(new Vector2D(x, y)));
                    
                    if (cell == null)
                    {
                        valid_transfer = false;
                        break iteration;
                    }
                    
                    if (cell.isForSale(id))
                    {
                        cell.addOccupant(id);
                    }
                }
            }
            
        }
        
        //System.out.println("Transfer: " + valid_transfer);
        return valid_transfer;
    }
    
    public void depopulate(int id, Vector2D position)
    {
        map.remove(id);
        Cell cell = bitmap.get(getCellKey(position));
        
        if (cell != null)
        {
            cell.removeOccupant(id);
        }
    }
    
    public Vector2D getCellKey(Vector2D pos)
    {
        if (!pos.onScreen()) return null;
        
        int x = pos.x;
        int y = pos.y;
        
        while (x % BITMAP_CELL_DIMENSION != 0)
        {
            x--;
        }
        
        while (y % BITMAP_CELL_DIMENSION != 0)
        {
            y--;
        }
        
        return bitmap_key_reference[x][y];
    }
    
//    private Cell[] getCellNeighbors(Cell cell) // gets ALL neighbors (includes diagonals)
//    {
//        Cell[] neighbors = new Cell[8]; // maximum of 8 neighbors
//        HashMap<Vector2D, Boolean> traversed = new HashMap<Vector2D, Boolean>(14);
//        Vector2D pos = new Vector2D();
//        Vector2D neighbor_pos;
//        int ncount = 0;
//        
//        for (int x = cell.key.x - BITMAP_CELL_DIMENSION; x < cell.key.x + (BITMAP_CELL_DIMENSION * 2); x = x + BITMAP_CELL_DIMENSION)
//        {
//            for (int y = cell.key.y - BITMAP_CELL_DIMENSION; y < cell.key.y + (BITMAP_CELL_DIMENSION * 2); y = y + BITMAP_CELL_DIMENSION)
//            {
//                pos.x = x; pos.y = y;
//                neighbor_pos = getCellKey(pos);
//                
//                if (traversed.get(neighbor_pos) != null) continue;
//                
//                traversed.put(neighbor_pos, true);
//                
//                if (neighbor_pos != null)
//                {
//                    if (!neighbor_pos.equals(cell.key))
//                    {
//                        neighbors[ncount] = bitmap.get(neighbor_pos);
//                        ncount++;
//                    }
//                }
//                else
//                {
//                    neighbors[ncount] = null;
//                    ncount++;
//                }
//            }
//        }
//        
//        return neighbors;
//    }
    
    private Cell[] getCellNeighbors(Cell cell)
    {
        Cell[] neighbors = new Cell[4]; // maximum of 8 neighbors
        Vector2D neighbor_pos;

        neighbor_pos = getCellKey(new Vector2D(cell.key.x - BITMAP_CELL_DIMENSION, cell.key.y)); // left
        if (neighbor_pos != null)
        {
            neighbors[0] = bitmap.get(neighbor_pos);
        }
        else neighbors[0] = null;
        
        neighbor_pos = getCellKey(new Vector2D(cell.key.x + BITMAP_CELL_DIMENSION, cell.key.y)); // right
        if (neighbor_pos != null)
        {
            neighbors[1] = bitmap.get(neighbor_pos);
        }
        else neighbors[1] = null;
        
        neighbor_pos = getCellKey(new Vector2D(cell.key.x, cell.key.y - BITMAP_CELL_DIMENSION)); // up
        if (neighbor_pos != null)
        {
            neighbors[2] = bitmap.get(neighbor_pos);
        }
        else neighbors[2] = null;
        
        neighbor_pos = getCellKey(new Vector2D(cell.key.x, cell.key.y + BITMAP_CELL_DIMENSION)); // down
        if (neighbor_pos != null)
        {
            neighbors[3] = bitmap.get(neighbor_pos);
        }
        else neighbors[3] = null;
                
          
        
        return neighbors;
    }
    
    public boolean lineOfSight(int x0, int y0, int x1, int y1) // http://www.codeproject.com/Articles/15604/Ray-casting-in-a-D-tile-based-environment
    {                                                          // adapted from JavaScript implementation
        double angle = Math.atan2(y1 - (y0), x1 - (x0));
        
        HashMap<Integer, Vector2D> ray = new HashMap<Integer, Vector2D>(700);
        int ray_index = 0;
        
        double x = x0;
        double y = y0;
        
        Cell cell = new Cell();
        
        double progress = 0;
        double max_progress = Vector2D.distance(new Vector2D(x0, y0), new Vector2D(x1, y1));
        
        while (progress < max_progress)
        {
            x = x + (progress * Math.cos(angle));
            y = y + (progress * Math.sin(angle));
            
            cell = bitmap.get(getCellKey(new Vector2D((int)x, (int)y)));
            
            if (cell != null)
            {
                Iterator iterator = cell.occupants.entrySet().iterator();
                while (iterator.hasNext()) 
                {
                    Map.Entry pair = (Map.Entry) iterator.next();
                    TerrainEntry entity = (TerrainEntry) pair.getValue();
                    
                    ray.put(ray_index, new Vector2D((int) x,(int) y));
                    ray_index++;

                    if (entity.type == EntityType.LocalPlayer) 
                    {
                        rays = ray;
                        return true;
                    }

                    if (entity.type != EntityType.Actor)
                    {
                        if (entity.rectangle.contains(new Point((int)x, (int)y)))
                        {
                            return false;
                        }
                    }
                }
            }
                    
            progress = progress + 1;
        }
        
        rays = ray;
        
        return true;
    }
    
    public void update()
    {

        
        //System.out.println("Occupied: " + occupied);
    }
    
    public void render(Graphics g)
    {
        //Iterator iterator = map.entrySet().iterator();
        
        //while (iterator.hasNext()) {
            //Map.Entry pair = (Map.Entry)iterator.next();
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            //iterator.remove(); // avoids a ConcurrentModificationException
       //}
        if (BITMAP_DEBUG_MODE)
        {
            Iterator iterator = bitmap.entrySet().iterator();
            Vector2D pos;
            Map.Entry pair;
            Cell cell;
            String t = "1";
            String f = "0";
            String output;
            Color c1 = new Color(124, 252, 0);
            Color c2 = Color.red;
            while (iterator.hasNext()) 
            { 
                //if (rectangle.intersects(((TerrainEntry)((Map.Entry)iterator.next()).getValue()).rectangle)) return false; // unreadable, but less allocation
                pair = (Map.Entry)iterator.next();
                //System.out.println(pair.getValue());
                pos = (Vector2D)pair.getKey();

                //int dimension = 5;
                //g.fillOval(pos.x, pos.y, dimension, dimension);
                //g.fillOval(pos.x + 25, pos.y, dimension, dimension);
                //g.fillOval(pos.x, pos.y + 25, dimension, dimension);
                //g.fillOval(pos.x + 25, pos.y + 25, dimension, dimension);

                cell = (Cell)((Map.Entry)pair).getValue();
                g.setColor((cell.occupied) ? c2 : c1);
                if (pos == null) System.out.println("POS IS NULL!!!!!"); // could be caused by init bitmap failure
                g.drawString((cell.occupied) ? "" + cell.occupants.size() : f, pos.x + BITMAP_CELL_DIMENSION / 2, pos.y + BITMAP_CELL_DIMENSION / 2);
                g.setColor(c1);
                g.drawRect(pos.x, pos.y, BITMAP_CELL_DIMENSION, BITMAP_CELL_DIMENSION);
            }

            Iterator iterator2 = map.entrySet().iterator();

            g.setColor(Color.BLACK);
            while (iterator2.hasNext()) 
            { 
                //if (rectangle.intersects(((TerrainEntry)((Map.Entry)iterator.next()).getValue()).rectangle)) return false; // unreadable, but less allocation
                //Vector2D p = (Vector2D)((TerrainEntry)((Map.Entry)iterator.next()).getValue()).position);
                //TerrainEntry te = ((TerrainEntry)((Map.Entry)iterator2.next().getValue()));

                Map.Entry pair2 = (Map.Entry)iterator2.next();
                TerrainEntry te = (TerrainEntry)pair2.getValue();

                if (te.type != EntityType.Bullet)
                {
                    Vector2D p = te.position;
                    int width = te.width;
                    int height = te.height;

                    int dimension = 5;
                    g.fillOval(p.x, p.y, dimension, dimension);
                    g.fillOval(p.x + width, p.y, dimension, dimension);
                    g.fillOval(p.x, p.y + height, dimension, dimension);
                    g.fillOval(p.x + width, p.y + height, dimension, dimension);
                }
            }
            
            if (path != null)
            {
                g.setColor(Color.CYAN);
                Iterator iterator3 = path.entrySet().iterator();
                while (iterator3.hasNext()) 
                { 
                    Map.Entry pair3 = (Map.Entry)iterator3.next();
                    Vector2D c = (Vector2D)pair3.getValue();

                    g.fillOval(c.x, c.y, 9, 9);  
                }
            }
            
            if (rays != null)
            {
                for (int i = 0; i < rays.size(); i++)
                {
                    Vector2D pos2 = rays.get(i);
                    g.setColor(Color.CYAN);
                    g.fillOval(pos2.x, pos2.y, 15, 15); 
                }
            }
            
        }
    }
    
    public Vector2D getRandomPosition(int width, int height) // heuristic
    {
        Random rand = new Random();
        int attempts = 0;
        int x, y;
        
        while (attempts < 150)
        {
            x = rand.nextInt(WIDTH - width - 10);
            y = rand.nextInt(HEIGHT - height);
            
            if (isForSale(x, y, width, height)) // recommend change to local implementation of isForSale to use less stack space / frames in potentially exhaustive iteration
            {
                return new Vector2D(x, y);
            }
            
            attempts++;
        }
        
        return null;
    }
    
    public boolean isForSale(int x, int y, int width, int height)
    {
        Rectangle rectangle = new Rectangle(x, y, width, height);
        Iterator iterator = map.entrySet().iterator();
        
        while (iterator.hasNext()) 
        { 
            if (rectangle.intersects(((TerrainEntry)((Map.Entry)iterator.next()).getValue()).rectangle)) return false; // unreadable, but less allocation
        }
        
        return true;
    }
    
    
    private class TerrainEntry 
    {
        public EntityType type;
        public Vector2D position;
        public int width;
        public int height;
        public Object object;
        public Rectangle rectangle;
        public int id;
        
        public TerrainEntry(EntityType arg_type, Vector2D arg_position, int arg_width, int arg_height, int arg_id)
        {
            type = arg_type;
            position = arg_position;
            width = arg_width;
            height = arg_height;
            rectangle = new Rectangle(position.x, position.y, width, height);
            id = arg_id;
            
            if (type == EntityType.TerrainObject)
            {
                //System.out.println("width: " + width +" .. height: " + height);
            }
        }
        
        public String toString()
        {
            return "TerrainEntity";
        }
    }
    
    private class Cell
    {
        public boolean occupied = false;
        public int occupant_count = 0;
        public int last_removed;
        public Vector2D key;
        public HashMap<Integer, TerrainEntry> occupants = new HashMap<Integer, TerrainEntry>();
        public Cell[] neighbors;
        public double gScore = Double.MAX_VALUE;
        public double fScore = Double.MAX_VALUE;
        
        public Cell(){}
        
        public Cell(Vector2D arg_key)
        {
            key = arg_key;
        }
        
        public boolean isForSale(int id)
        {
            TerrainEntry buyer = map.get(id);
            
            Iterator iterator = occupants.entrySet().iterator();
                
            while (iterator.hasNext()) 
            {
                Map.Entry pair = (Map.Entry)iterator.next();
                TerrainEntry existing_occupant = (TerrainEntry)pair.getValue();
                
                //System.out.println("EO type: " + existing_occupant.type);
                //if (existing_occupant.type == EntityType.TerrainObject) return false;
                
                if (buyer != existing_occupant)
                {
                    if (buyer.rectangle.intersects(existing_occupant.rectangle))
                    {
                        //System.out.println("Detected a Collision");
                        if (buyer.type == EntityType.Bullet)
                        {
                            if (existing_occupant.type == EntityType.LocalPlayer)
                            {
                                LocalPlayer.health = LocalPlayer.health - 4;
                            }
                            else if (existing_occupant.type == EntityType.Actor)
                            {
                                Rifleman actor = (Rifleman) IdPool.entities.get(existing_occupant.id);
                                actor.health = actor.health - 25;
                            }
                            
                            if (existing_occupant.type == EntityType.Bullet)
                            {
                                Bullet bullet = Game.bullets.get(id);
                                
                                if (bullet != null && !bullet.remove)
                                {
                                    removeOccupant(id);
                                }
                            }
                            else removeOccupant(id);
                        }
                        if (existing_occupant.type == EntityType.Bullet)
                        {
                            removeOccupant((Integer)pair.getKey());
                        }
                        return false;
                    }
                    
                    if (existing_occupant.type == EntityType.TerrainObject)
                    {
                        //System.out.println(existing_occupant.rectangle.getSize());
                        //System.out.println(buyer.rectangle.getSize());
                        //System.out.println(existing_occupant.rectangle.getLocation());
                        //System.out.println(buyer.rectangle.getLocation());
                    }
                    
                }
                
            }
            
            return true;
        }
        
        public void addOccupant(int id)
        {
            if (occupants.get(id) == null)
            {
                occupants.put(id, map.get(id));
                occupant_count++;
                occupied = true;
            }
        }
        
        public void removeOccupant(int id)
        {
            TerrainEntry removed = occupants.remove(id);
            if (removed != null)
            {
                System.out.println("Occupant Removed, new count: " + occupant_count);
                occupant_count--;
                if (occupant_count == 0)
                {
                    occupied = false;
                }
                last_removed = id;
            }

        }
    }
    
    public Vector2D[] getBitmapNeighbors(Vector2D goal)
    {
        return new Vector2D[]{
            getCellKey(new Vector2D(goal.x, goal.y)),
            getCellKey(new Vector2D(goal.x - BITMAP_CELL_DIMENSION, goal.y)),
            getCellKey(new Vector2D(goal.x + BITMAP_CELL_DIMENSION, goal.y)),
            getCellKey(new Vector2D(goal.x, goal.y - BITMAP_CELL_DIMENSION)),
            getCellKey(new Vector2D(goal.x, goal.y + BITMAP_CELL_DIMENSION))
        };
    }
    
    public HashMap<Integer, Vector2D> findPath(Vector2D start_pos, Vector2D goal_pos) // A* Algorithm with Euclidean heuristic cost calculation and 4-direction movement
    {                                                                             // start_pos and goal_pos must be valid bitmap keys
        // implementation based on language-less pseudocode from https://en.wikipedia.org/wiki/A*_search_algorithm     
        if (start_pos == null || goal_pos == null)
        {
            System.out.println("Invalid start_pos or goal_pos");
            return null;
        }
        
        if (bitmap.get(start_pos) == null || bitmap.get(goal_pos) == null)
        {
            System.out.println("no valid key for start or goal");
            return null;
        }
        
        HashMap<Vector2D, Boolean> closed = new HashMap<Vector2D, Boolean>(125);
        HashMap<Vector2D, Cell> open = new HashMap<Vector2D, Cell>(125);
        HashMap<Vector2D, Cell> came_from = new HashMap<Vector2D, Cell>(125);
        
        
        Cell start_cell = bitmap.get(start_pos);
        
        Vector2D current_pos;
        Cell current_cell = null;
       
        Iterator iterator = bitmap.entrySet().iterator(); // reset cost values on all cells
        while (iterator.hasNext())
        {
            Map.Entry pair = (Map.Entry)iterator.next();
            Cell cell = (Cell)pair.getValue();
            cell.fScore = Double.MAX_VALUE;
            cell.gScore = Double.MAX_VALUE;
        }
        
        start_cell.gScore = 0;
        start_cell.fScore = Vector2D.distance(start_pos, goal_pos);
        
        if (bitmap.get(goal_pos).occupied) // if destination is occupied
        {
            System.out.println("Goal Cell is occupied");
            return null;
        }
        
        if (bitmap.get(start_pos).occupied)
        {
            System.out.println("idk if this matters but start pos is occupied");
        }
        
        open.put(start_pos, bitmap.get(start_pos));
        
        while (open.size() > 0)
        {
            current_pos = lowestFScoreInOpen(open);
            current_cell = bitmap.get(current_pos);
            
            if (current_pos == goal_pos)
            {
                return reconstructedMap(came_from, current_cell);
            }
            
            open.remove(current_pos);
            closed.put(current_pos, true);
            
            for (Cell neighbor : current_cell.neighbors) // iterate through all the neighbors
            {
                if (neighbor == null || closed.get(neighbor.key) != null || neighbor.occupied) continue;
                
                double tentative_gScore = current_cell.gScore + Vector2D.distance(current_pos, neighbor.key);
                if (open.get(neighbor.key) == null)
                {
                    open.put(neighbor.key, neighbor);
                }
                else if (tentative_gScore >= neighbor.gScore)
                {
                    continue;
                }
                
                came_from.put(neighbor.key, current_cell);
                neighbor.gScore = tentative_gScore;
                neighbor.fScore = neighbor.gScore + Vector2D.distance(neighbor.key, goal_pos);
            }
        }
        
        System.out.println("A* Failed");
        return null;
    }
    
    public Vector2D lowestFScoreInOpen(HashMap open)
    {
        int iterations = 0;
        double lowest = Double.MAX_VALUE;
        Vector2D lowest_key = new Vector2D();
        Iterator iterator = open.entrySet().iterator(); // reset cost values on all cells
        
        while (iterator.hasNext())
        {
            Map.Entry pair = (Map.Entry)iterator.next();
            Cell cell = (Cell)pair.getValue();
            
            if (cell.fScore < lowest)
            {
                lowest = cell.fScore;
                lowest_key = cell.key;
            }
            
            if (iterations == 0)
            {
                lowest_key = cell.key; 
            }
            
            iterations++;
        }
        
        return lowest_key;
    }
    
    private HashMap<Integer, Vector2D> reconstructedMap(HashMap came_from, Cell current) // create map and adjust for entity size
    {
        HashMap<Integer, Vector2D> npath = new HashMap<Integer, Vector2D>((int)(came_from.size() * 1.4));
        int node_count = 0;
        Cell neighbor_cell;
        
        // create path and adjust for entity size
        while (came_from.get(current.key) != null)
        {
            current = (Cell)came_from.get(current.key);
           
            for (Cell neighbor : current.neighbors) // iterate through all the neighbors
            {
                if (neighbor == null || neighbor == current) continue;
                
//                if (neighbor.occupied)
//                {
//                    neighbor_cell = getNeighborCellOpposite(current, neighbor);
//                    if (neighbor_cell == null) continue;
//                    
//                    //System.out.println("current: " + current.key + " | " + "opposite neighbor: " + neighbor_cell.key);
//                    
//                    if (!neighbor_cell.occupied)
//                    {
//                        path.put(node_count, neighbor_cell.key);
//                        System.out.println("Adjustment made");
//                    } else path.put(node_count, current.key);
//                } else path.put(node_count, current.key);
                
                 npath.put(node_count, current.key);
            }
            
            if (current.occupied) return null;
            node_count++;
        }
        
        path = npath;
        
        return path;
    }
    
//    private Cell getNeighborCellOpposite(Cell cell1, Cell cell2)
//    {
//        Vector2D neighbor_pos = new Vector2D();
//        Vector2D key;
//        int difx = cell2.key.x - cell1.key.x;
//        int dify = cell2.key.y - cell1.key.y;
//        final int BCD = BITMAP_CELL_DIMENSION;
//        
////        if (difx < 0 && dify > 0) // top right diag
////        {
////            neighbor = new Vector2D(cell1.key.x - BCD, cell1.key.y + BCD);
////        }
////        else if (difx == 0 && dify > 0) // above
////        {
////            neighbor = new Vector2D(cell1.key.x, cell1.key.y + BCD);
////        }
////        else if (difx > 0 && dify > 0) // top left diag
////        {
////            neighbor = new Vector2D(cell1.key.x + BCD, cell1.key.y + BCD);
////        }
////        else if (difx > 0 && dify == 0) // left
////        {
////           neighbor = new Vector2D(cell1.key.x + BCD, cell1.key.y); 
////        }
////        else if (difx > 0 && dify < 0) // bottom left diag
////        {
////            neighbor = new Vector2D(cell1.key.x + BCD, cell1.key.y - BCD);
////        }
////        else if (difx == 0 && dify < 0) // under
////        {
////            neighbor = new Vector2D(cell1.key.x, cell1.key.y - BCD);
////        }
////        else if (difx < 0 && dify < 0) // bottom right diag
////        {
////            neighbor = new Vector2D(cell1.key.x - BCD, cell1.key.y - BCD);
////        }
////        else if (difx < 0 && dify == 0) // right
////        {
////            neighbor = new Vector2D(cell1.key.x - BCD, cell1.key.y);
////        }
//        double farthest = 0;
//        Vector2D farthest_pos = new Vector2D();
//        
//        for (Cell neighbor : cell1.neighbors) // iterate through all the neighbors
//        {
//            if (neighbor == null || neighbor == cell1) continue;
//            double distance = Vector2D.distance(cell1.key, neighbor.key);
//            
//            if (distance > farthest)
//            {
//                farthest = distance;
//                farthest_pos = neighbor.key;
//            }
//        }
//
//        
//        //System.out.println("occupied: " + cell2.key + " | opposite: " + neighbor);
//        if (farthest_pos.x == 0 && farthest_pos.y == 0) return null;
//        
//        key = getCellKey(farthest_pos);
//        
//        
//        if (key != null)
//        {
//            Cell neighbor_cell = bitmap.get(key);
//            
//            if (neighbor_cell != null)
//            {
//                return neighbor_cell;
//            } else System.out.println("invalid bitmap key");
//        }
//        else System.out.println("neighbor is null in getneighboropposite");
//        
//        return null;
//    }
//    
}

// for object raycasting (like bullets), generate full bitmap of terrain in a grid format by aggregating squares of pixels
// store this new bitmap in a HashMap<Vector2D, Boolean(occupied)> 
// HashMap<Vector2D, TerrainEntry> is not possible because parts of two objects may occupy the same cell
// Bullets first reference 'bitmap', and do not access the 'map' unless a cell is occupied
// if a cell a Bullet is travelling through is occupied, it retrieves the entities in the cell and checks for collisions (intersections)

// Basic idea: general purpose 'map' stores TerrainEntry as value with unique id as key
//             TerrainEntry is a nice wrapper class for any object that needs to be mapped (guaranteed pos, width, height, rectangle)
//             customizable bitmap maps the screen
//             uses Cells which store occupants among other things (TerrainEntry's)

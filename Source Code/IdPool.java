
package guns.ai;

import java.util.HashMap;

public class IdPool {
    public static HashMap<Integer, Boolean> id_pool = new HashMap<Integer, Boolean>(500, (float).85);
    public static HashMap<Integer, Object> entities = new HashMap<Integer, Object>(500);
    public static int size = 0;
    
    public IdPool()
    {
    
    }
    
    public static int generateId(Object requester)
    {
        id_pool.put(size, true);
        entities.put(size, requester);
        size++;
        return size - 1;
    }
    
    public static void flush()
    {
        
    }
}

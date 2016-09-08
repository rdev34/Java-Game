
package guns.ai;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


public class Game implements Config, ActionListener, KeyListener, MouseListener {
    
    private JFrame frame;
    private DrawingPanel panel;
    public static GameState status;
    private Timer updater;
    private BufferedImage background;
    
    public static boolean gui_change = false;
    public static Point mouse_position = new Point(0, 0);
    
    public static Terrain terrain;
    public static HashMap<Integer, Bullet> bullets = new HashMap<Integer, Bullet>(200, (float).85);
    
    public static World current_world;
    
    public Game()
    {
        status = GameState.play;
        // INIT
        System.out.println("Width: " + WIDTH + " | Height: " + HEIGHT);
        
        terrain = new Terrain();
        
        
        
        new LocalPlayer(); // LocalPlayer populates terrain
        
        try
        {
            background = ImageIO.read(getClass().getClassLoader().getResource(("images/map.png")));
        }
        catch (IOException e)
        {
            throw new Error("IO Error - Could Not Load Image File map.png");
        }
        
        panel = new DrawingPanel();
        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        
        System.out.println("Width: " + WIDTH);
        
        
        frame = new JFrame("My Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // end application if user closes game window
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);

        status = GameState.play;
        
        ActionListener task = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (status == GameState.play)
                {
                    Point temp_mouse_pos = panel.getMousePosition();
                    if (temp_mouse_pos != null)
                    {
                        mouse_position = temp_mouse_pos;
                    }

                    LocalPlayer.update();

                    Iterator iterator = bullets.entrySet().iterator();
                    Bullet[] removes = new Bullet[bullets.size() + 1];
                    int remove_count = 0;

                    while (iterator.hasNext()) 
                    {
                        Map.Entry pair = (Map.Entry)iterator.next();
                        Bullet bullet = (Bullet)pair.getValue();
                        if (!bullet.remove)
                        {
                            bullet.update();
                        }
                        else
                        {
                            removes[remove_count] = bullet;
                        }
                        //System.out.println(pair.getKey() + " = " + pair.getValue());
                        //iterator.remove(); // avoids a ConcurrentModificationException
                    }

                    for (int i = 0; i < remove_count; i++)
                    {
                        Vector2D p = new Vector2D(removes[i].position);
                        Game.terrain.depopulate(removes[i].id, p);
                        Game.terrain.depopulate(removes[i].id, new Vector2D(p.x + BITMAP_CELL_DIMENSION, p.y));
                        Game.terrain.depopulate(removes[i].id, new Vector2D(p.x - BITMAP_CELL_DIMENSION, p.y));
                        Game.terrain.depopulate(removes[i].id, new Vector2D(p.x, p.y + BITMAP_CELL_DIMENSION));
                        Game.terrain.depopulate(removes[i].id, new Vector2D(p.x, p.y - BITMAP_CELL_DIMENSION));
                        bullets.remove(removes[i].id);
                    }

                    if (terrain != null)
                    {
                        //terrain.update();
                    }

                    if (current_world != null)
                    {
                        current_world.update();
                        
                        if (current_world.actors.size() == 1)
                        {
                            status = GameState.win;
                        }
                    }


                    if (gui_change == true) // this should be last
                    {
                        gui_change = false;
                        panel.repaint();
                    }
                }
            }
        };
        
        panel.setFocusable(true); // make panel respond to keyboard
        panel.requestFocus();
        panel.addKeyListener(this);
        panel.addMouseListener(this);
        
        current_world = new World(1);
        
        updater = new Timer(UPDATE_DELAY, task);
        updater.start();
    }
    
    public static void spawnBullet(Vector2D position, double angle)
    {
        Bullet bullet = new Bullet(new Vector2D(position), angle);
        bullets.put(bullet.id, bullet);
    }
    
    public static void bulletRemove(int id) // remove all references to the bullet
    {
        terrain.map.remove(id);
        
    }
    

    @Override
    public void actionPerformed(ActionEvent e) {
        
    }

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        //System.out.println("ENTER KEYPRESSED");
        if (e.getKeyCode() == KeyEvent.VK_UP)
        {
            LocalPlayer.dir[0] = true;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_DOWN)
        {
            LocalPlayer.dir[1] = true;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_LEFT)
        {
            LocalPlayer.dir[2] = true;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_RIGHT)
        {
            LocalPlayer.dir[3] = true;
        }
        
        
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP)
        {
            LocalPlayer.dir[0] = false;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_DOWN)
        {
            LocalPlayer.dir[1] = false;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_LEFT)
        {
            LocalPlayer.dir[2] = false;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_RIGHT)
        {
            LocalPlayer.dir[3] = false;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) { // click and release
        
    }

    @Override
    public void mousePressed(MouseEvent e) { // just click
        LocalPlayer.firing = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        LocalPlayer.firing = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
       
    }


    
    
    public class DrawingPanel extends JPanel {
        
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g); // call the method for JPanel
            // extension below
            //if (status == GameState.play)
            
                drawPlay(g);
            
//            else if (status == GameState.end)
//            {
//                drawEnd(g);
//            }
//            else if (status == GameState.win)
//            {
//                drawWin(g);
//            }
        }
        
        public void drawPlay(Graphics g)
        {
            g.drawImage(background, 0, 0, Config.WIDTH, Config.HEIGHT, null);
            if (current_world != null)
            {
                current_world.render(g);
            }
            
            LocalPlayer.render(g);
            terrain.render(g);
            
            Iterator iterator = bullets.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry pair = (Map.Entry)iterator.next();

                Bullet b = (Bullet) pair.getValue();
                b.render(g);
                //iterator.remove(); // avoids a ConcurrentModificationException
                
            }
            
            if (status == GameState.end)
            {
                g.setColor(Color.RED);
                g.setFont(new Font("Courier", Font.BOLD, 40));
                g.drawString("Game Over", Config.WIDTH / 2 - 40, Config.HEIGHT / 2 - 20);   
            }
            else if (status == GameState.win)
            {
                g.setColor(Color.RED);
                g.setFont(new Font("Courier", Font.BOLD, 40));
                g.drawString("You Won!", Config.WIDTH / 2 - 40, Config.HEIGHT / 2 - 20);
            }
        }
        
        public void drawEnd(Graphics g)
        {
            g.setColor(Color.RED);
            g.setFont(new Font("Courier", Font.BOLD, 40));
            g.drawString("Game Over", Config.WIDTH / 2 - 40, Config.HEIGHT / 2 - 20);
        }
        
        public void drawWin(Graphics g)
        {
            g.setColor(Color.RED);
            g.setFont(new Font("Courier", Font.BOLD, 40));
            g.drawString("You Won!", Config.WIDTH / 2 - 40, Config.HEIGHT / 2 - 20);
        }
        
    }
    
}


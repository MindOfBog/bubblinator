package bog.bubblinator;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;

import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * @author Bog
 */
public class Main {

    public static Bubblinator bubblinator = new Bubblinator();
    public static JFrame mainForm;

    public static final String VERSION = "3.1";

    public static void main(String args[])
    {
        mainForm = new JFrame("Bubblinator " + VERSION);
        mainForm.setContentPane(bubblinator.mainForm);
        LafManager.install(new DarculaTheme());
        mainForm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainForm.setResizable(false);
        mainForm.pack();
        mainForm.setVisible(true);
        try {
            mainForm.setIconImage(ImageIO.read(Main.class.getResourceAsStream("/bubblinatoricon.png")));
        }catch (Exception e){e.printStackTrace();}
    }
}

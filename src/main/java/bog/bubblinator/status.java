package bog.bubblinator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Bog
 */
public class status {
    public JPanel status;
    public JLabel progress;
    public JLabel amount;
    public JFrame frame;
    public int progressLive = 0;
    ScheduledExecutorService exec;

    public status()
    {
        frame = new JFrame("Progress");
        frame.setContentPane(this.status);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
        try {
            frame.setIconImage(ImageIO.read(Main.class.getResourceAsStream("/bubblinatoricon.png")));
        }catch (Exception e){e.printStackTrace();}

        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                progress.setText(Integer.toString(progressLive));
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void close()
    {
        if(exec != null)
            exec.shutdown();
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
}

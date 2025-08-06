package bog.bubblinator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

/**
 * @author Bog
 */
public class addDescriptor {
    private JPanel panel1;
    private JTextField guidTF;
    private JTextField hashTF;
    private JButton addButton;
    private JButton cancelButton;

    public JFrame frame;

    public addDescriptor()
    {
        frame = new JFrame("Add descriptor");
        frame.setContentPane(this.panel1);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
        try {
            frame.setIconImage(ImageIO.read(Main.class.getResourceAsStream("/bubblinatoricon.png")));
        }catch (Exception e){e.printStackTrace();}

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultListModel<String> model = (DefaultListModel<String>) Main.bubblinator.planList.getModel();
                String guid = guidTF.getText().trim();

                if(!guid.isEmpty())
                    try{
                        Long.parseLong(guid);
                    }catch (Exception ex)
                    {
                        try{
                            Long.parseLong(guid.substring(1));
                        }catch (Exception ex1)
                        {
                            guid = "";
                        }
                    }

                if(!guid.isEmpty())
                    guid = guid.startsWith("g") ? guid : "g" + guid;
                String hash = hashTF.getText().trim();
                if(!hash.isEmpty() && !(hash.matches("^[a-fA-F0-9]{40}$") || hash.substring(1).matches("^[a-fA-F0-9]{40}$")))
                    hash = "";
                if(!hash.isEmpty())
                    hash = hash.startsWith("h") ? hash : "h" + hash;

                String descriptors = "";

                if(!guid.isEmpty())
                    descriptors = guid;

                if(!guid.isEmpty() && !hash.isEmpty())
                    descriptors += " : ";

                if(!hash.isEmpty())
                    descriptors += hash;

                if(!descriptors.isEmpty())
                {
                    if(!model.contains(descriptors))
                        model.addElement(descriptors);
                    Main.bubblinator.entryCount.setText("Entries: " + model.size());
                }
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });
    }
}

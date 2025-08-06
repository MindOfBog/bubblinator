package toolkit.gl;

import cwlib.resources.custom.RSceneGraph;
import cwlib.singleton.ResourceSystem;
import cwlib.types.Resource;
import editor.gl.Camera;
import editor.gl.RenderSystem;


import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.joml.Vector3f;

public class CraftworldRenderer {
    public static class ViewportListener implements KeyListener {
        @Override public void keyTyped(KeyEvent e) { return; }
        @Override public void keyReleased(KeyEvent e) { return; }
        @Override public void keyPressed(KeyEvent e) {
            Vector3f translation = RenderSystem.getMainCamera().getTranslation();
            int code = e.getKeyCode();

            float displacement = 3.0f * 50.0f;
            if (code == KeyEvent.VK_D) translation.x += displacement;
            else if (code == KeyEvent.VK_A) translation.x -= displacement;
            else if (code == KeyEvent.VK_W) translation.y += displacement;
            else if (code == KeyEvent.VK_S) translation.y -= displacement;

            RenderSystem.getMainCamera().setTranslation(translation);
        }
    }

    private static final long serialVersionUID = 1L;

    public CraftworldRenderer() {

        ResourceSystem.DISABLE_LOGS = true;
        
        RSceneGraph graph = new RSceneGraph();

        RenderSystem.setSceneGraph(graph);

        ResourceSystem.DISABLE_LOGS = false;

        this.setupAWT();
    }

    private void setupAWT() {
    }

    public void initGL() {
        RenderSystem.initialize();
    }
    public void paintGL() {
        Camera camera = RenderSystem.getMainCamera();

        camera.recomputeProjectionMatrix();
        camera.recomputeViewMatrix();
    }
}

import javax.swing.RepaintManager;

public class NullRepaintManager extends RepaintManager {
	public void install() {
		RepaintManager repaintManager = new NullRepaintManager();
		repaintManager.setDoubleBufferingEnabled(false);
		RepaintManager.setCurrentManager(repaintManager);
	}
}
package ui;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.LayoutManager;

/**
 * This is an implementation of JPanel that allows for background images
 * @author David Wang
 */
public class JImagePanel extends javax.swing.JPanel {
	private static final long serialVersionUID = 1L;
	private Image bgImage;
	
	public JImagePanel(Image bgImage) {
		super();
		this.bgImage = bgImage;
	}
	
	public JImagePanel(Image bgImage, LayoutManager l) {
		super(l);
		this.bgImage = bgImage;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (bgImage != null ) g.drawImage(bgImage.getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_DEFAULT), 0, 0, null); //have to add this to add image support
	}

	/**
	 * Sets the bgImage and repaints the components so it shows up
	 * @param bgImage
	 */
	public void setImage(Image bgImage) {
		this.bgImage = bgImage;
		this.update(this.getGraphics());
	}
}

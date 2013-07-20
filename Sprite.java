import java.awt.Image;

public class Sprite {

	private Animation anim;
	private double x;
	private double y;
	private double dx;
	private double dy;

	public Sprite(Animation anim) {
		this.anim = anim;
	}

	public void update(long elapsedTime) {
		x += dx * elapsedTime;
		y += dy * elapsedTime;
		anim.update(elapsedTime);
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public int getWidth() {
		return anim.getImage().getWidth(null);
	}

	public int getHeight() {
		return anim.getImage().getHeight(null);
	}

	public double getVelocityX() {
		return dx;
	}

	public double getVelocityY() {
		return dy;
	}

	public double getVelocity() {
		return Math.sqrt(dx * dx + dy * dy);
	}

	public void setVelocityX(double dx) {
		this.dx = dx;
	}

	public void setVelocityY(double dy) {
		this.dy = dy;
	}

	public Image getImage() {
		return this.anim.getImage();
	}
}

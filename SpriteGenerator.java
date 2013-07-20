import java.awt.Image;

public class SpriteGenerator {
	private int type;
	private Image[] imageArray;
	private Image image;
	private Animation anim;
	private Sprite sprite;

	SpriteGenerator(int type, Image[] image) {
		this.type = type;
		this.imageArray = new Image[image.length];
		this.imageArray = image;
		anim = new Animation();
	}

	SpriteGenerator(int type, Image image) {
		this.type = type;
		this.image = image;
		anim = new Animation();
	}

	Sprite getSprite() {
		switch (type) {
		case -1:// Bullet
			anim.addFrame(image, 5000);
			sprite = new Sprite(anim);
			break;
		case 0:// Ghost 0
			for (int i = 0; i < imageArray.length; i++)
				anim.addFrame(imageArray[i], 150);
			sprite = new Sprite(anim);
			break;
		case 1:// Ghost 1
			for (int i = 0; i < imageArray.length; i++)
				anim.addFrame(imageArray[i], 150);
			sprite = new Sprite(anim);
			break;
		case 2:// Ghost 2
			for (int i = 0; i < imageArray.length; i++)
				anim.addFrame(imageArray[i], 150);
			sprite = new Sprite(anim);
			break;
		case 3:// Ghost 3
			for (int i = 0; i < imageArray.length; i++)
				anim.addFrame(imageArray[i], 250);
			sprite = new Sprite(anim);
			break;
		case 4:// Ghost 4
			for (int i = 0; i < imageArray.length; i++)
				anim.addFrame(imageArray[i], 120);
			sprite = new Sprite(anim);
			break;
		case 5:// Ghost 5
			for (int i = 0; i < imageArray.length; i++)
				anim.addFrame(imageArray[i], 120);
			sprite = new Sprite(anim);
			break;
		case 99:// Boss
			for (int i = 0; i < imageArray.length - 6; i++)
				anim.addFrame(imageArray[i], 100);
			anim.addFrame(imageArray[22], 500);
			anim.addFrame(imageArray[23], 500);
			anim.addFrame(imageArray[24], 1500);
			anim.addFrame(imageArray[25], 2500);
			sprite = new Sprite(anim);
			break;
		case 100:// Boss - furiousA
			anim.addFrame(imageArray[26], 4500);
			anim.addFrame(imageArray[27], 500);
			sprite = new Sprite(anim);
			break;
		case 101:// Boss - furiousAB
			anim.addFrame(imageArray[27], 500);
			anim.addFrame(imageArray[28], 500);
			anim.addFrame(imageArray[29], 2500);
			sprite = new Sprite(anim);
			break;
		default:
			return null;
		}
		return sprite;
	}
}

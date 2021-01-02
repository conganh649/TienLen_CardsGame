
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Card {
	// Khai báo các giá trị của lá bài
	Image img = null;
	String[] Set = { "Spades", "Clubs", "Diamonds", "Hearts" };
	String[] Value = { "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace", "2" };
	// Lấy src ảnh để gắn cho các lá bài
	static String imgsrc = "D:\\cards\\img\\";

	boolean isChoosing = false;
	public int ID;
	public String Name;

	// Khởi tạo lá bài
	Card(int ID) {
		this.ID = ID;

		// Size lá bài
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int cardw = screenSize.width / 10;
		int cardh = screenSize.height / 4;

		// Đọc file
		try {
			File f = new File(imgsrc + ID + ".png");
			img = new BufferedImage(cardw, cardh, BufferedImage.TYPE_INT_ARGB);
			img = ImageIO.read(f);
			img = img.getScaledInstance(cardw, cardh, Image.SCALE_SMOOTH);
		} catch (IOException e) {
			System.out.println("Load image fail!");
		}
		// Lấy name -> đặt ID bài từ 3 bích -> 2 cơ là 1 -> 52
		// Phần nguyên sẽ lấy bài từ 3 -> 2, phần dư lấy thuộc tính
		this.Name = Value[ID / 4] + " of " + Set[ID % 4] + "\tID:" + ID;
	}
}

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;

// Phòng chơi
@SuppressWarnings("serial")
public class PlayRoom extends JFrame implements Runnable {
	// Lấy background nền
	String filePath = "D:\\cards\\img\\";

	// Khai báo
	public Player pl;
	Socket socket;
	String name;
	int check = 0;
	int ID = 0;
	int kc = 30;
	int currentID = 0;
	Card[] currentMove = null;
	Card[] cardChoosing = null;
	int soNguoi;
	Image hidden, ban;

	boolean isCreator, isStart = false;
	boolean status, isFinish = false;
	Dimension screenSize;
	int cardw, cardh;
	Button btnDanhBai, btnBoLuot, btnReady, btn_send;
	Panel pn1, pn2, pn;
	TextField tf, timeTF;
	TextArea result;
	Timer timer;
	int interval = 60;
	int left_cards = 13, right_cards = 13, oppo_cards = 13;
	int difw, difh;

	public PlayRoom(Socket soc, String name, boolean stat) {

		// Tạo 1 playroom với tên phòng, player và socket để tương tác với SV
		this.name = name;
		this.pl = new Player(name);
		this.socket = soc;
		isCreator = stat;
		setTitle("Welcome " + this.name + " !!!");
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
		setLayout(null);

		addComponent();
		GUI();
		loadImage();
		addListener();
		repaint();
	}

	private void addComponent() {

		// Thêm các component vào PlayRoom của mỗi Client
		screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		difw = this.getWidth() * 1 / 5 + kc * 12 * 5 / 4 + screenSize.width / 10;
//		difw = kc * 12 * 5 / 4 + screenSize.width / 10;
		difh = this.getHeight() * 1 / 9 + kc * 12 * 7 / 10 + screenSize.height / 4;
		btnDanhBai = new Button("Danh bai");
		btnDanhBai.setBounds(difw, screenSize.height * 14 / 20, 100, 40);
		btnBoLuot = new Button("Bo luot");
		btnBoLuot.setBounds(difw, screenSize.height * 15 / 20, 100, 40);
		btnReady = new Button("Start");
		btnReady.setBounds(this.getWidth() * 7 / 20 + kc * 3, this.getHeight() * 8 / 20, 200, 150);
		// Nếu là người tạo phòng thì có quyền bắt đầu trò chơi
		if (isCreator)
			add(btnReady);

		// Thêm 1 đồng hồ đếm ngược vào và không cho Edit
		timeTF = new TextField();
		timeTF.setBounds(difw, screenSize.height * 16 / 20, 100, 100);
		timeTF.setFont(new Font("Tahoma", Font.PLAIN, 80));
		timeTF.setBackground(Color.red);
		timeTF.setEditable(false);
		timer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (status) {
					// Đếm ngược từ 60 về, về 0 thì tắt time đi
					timeTF.setText(interval-- + "");
					if (interval < 0) {
						interval = 60;
						try {
							DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
							dos.writeUTF("drop");
						} catch (Exception e1) {
						}
						status = false;
						remove(timeTF);
						// Không cho đánh bài hay bỏ lượt nữa
						btnDanhBai.setEnabled(status);
						btnBoLuot.setEnabled(status);
						repaint();
					}
				}
			}
		});
	}

	private void addListener() {
		btnBoLuot.addActionListener(new ActionListener() {

			// Nếu bỏ lược thì truyền lệnh Drop lên SV xử lý
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
					dos.writeUTF("drop");
				} catch (Exception e1) {
					// Do nothing
				}
				// Bỏ luôn đồng hồ đếm ngược
				status = false;
				remove(timeTF);
				btnDanhBai.setEnabled(status);
				btnBoLuot.setEnabled(status);
			}
		});
		btnDanhBai.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Xử lý sự kiện đánh bài
				int count = 0;
				// Tạo 1 mảng pick bài
				Card[] tmp = new Card[13];

				// Chọn bài trên tay thêm vào mảng pick bài
				for (int i = 0; i < pl.myDeck.length; i++) {
					if (pl.myDeck[i].isChoosing) {
						tmp[count++] = pl.myDeck[i];
					}
				}

				// Nếu có chọn bài thì tạo ra 1 mảng bài đã chọn để gởi đi
				if (count > 0) {
					cardChoosing = Arrays.copyOfRange(tmp, 0, count);
				}
				// Kiểm tra có được đánh mảng bài chọn xuống hay không
				if (currentMove == null || pl.PlayCard(currentMove, cardChoosing)) {
					// Set bài trên bàn là mảng bài vừa đánh xuống
					// Lấy mảng bài vừa đánh ra khỏi bộ bài trên tay
					currentMove = cardChoosing;
					pl.Pop(cardChoosing);

					// Hết bài trên tay thì Finish là true
					if (pl.myDeck.length == 0)
						isFinish = true;
					try {
						// Gởi mảng bài lên Server, nếu finish thì gởi kèm finish lên
						DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
						String s = "card ";
						for (int k = 0; k < cardChoosing.length; k++)
							s += cardChoosing[k].ID + " ";
						if (isFinish)
							s += "Finish";
						dos.writeUTF(s);
					} catch (Exception e1) {
						// Do nothing
					}
					// Đánh xong thì bỏ thời gian đi
					status = false;
					remove(timeTF);
					btnDanhBai.setEnabled(status);
					btnBoLuot.setEnabled(status);
				} else {
					// Nếu không đánh được thì hủy chọn các lá bài
					for (Card c : pl.myDeck) {
						c.isChoosing = false;
					}
					cardChoosing = null;
				}
				repaint();
			}
		});
		PlayRoom r = this;
		// Xử lý sự kiện khi ấn vào lá bài
		r.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				// Bấm vào lá bài thì lá bài nhảy lên trên để biết là đang được chọn
				int x = e.getX();
				int y = e.getY();
				/*
				 * Tọa độ x và y tại nơi nhấn chuột Nếu x và y nằm trong vùng có bài --> sẽ chọn
				 * được bài Lá bài sẽ nằm trong khoảng như sau Trục dọc chia thành 20 phần từ
				 * trên xuống, phần dưới lá bài sẽ nằm tại phần thứ 19 lên trên chiều cao lá bài
				 * = 1/4 chiều cao màn hình
				 * 
				 * Chiều ngang của lá bài sẽ bắt đầu từ 1/5 màn hình bên trái, mỗi lá bài cách
				 * nhau kc, có 12 lá bài như vậy, lá cuối cùng thì lấy nguyên lá
				 * 
				 */
				if (y > r.getHeight() * 19 / 20 - screenSize.height / 4
						&& (y < r.getHeight() * 19 / 20 - screenSize.height / 4 + cardh) && x > r.getWidth() * 1 / 5
						&& x < r.getWidth() * 1 / 5 + (kc * 5 / 4) * 12 + cardw) {
					// Xác định lá bài là lá bài nào (index từ 0 -> 12)
					int ind = (x - r.getWidth() * 1 / 5) / (kc * 5 / 4);
					System.out.println(ind);
					if (ind > 12)
						ind = 12;
					if (pl.myDeck[ind] != null) {
						pl.myDeck[ind].isChoosing = !pl.myDeck[ind].isChoosing;
						r.repaint();
					}
				}
			}
		});
		this.addWindowListener(new WindowAdapter() {
			@Override
			// Tắt cửa sổ game thì rời phòng
			public void windowClosing(WindowEvent e) {
				try {
					DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
					dos.writeUTF("Leave");
					dos.flush();
					dos.close();
					socket.close();
				} catch (Exception err) {
					System.out.println("Err");
				}
			}
		});
		btnReady.addActionListener(new ActionListener() {
			@Override

			// Bấm vào button Start thì bắt đầu chơi
			public void actionPerformed(ActionEvent arg0) {
				DataOutputStream dos;
				try {
					dos = new DataOutputStream(socket.getOutputStream());
					dos.writeUTF("Play");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		// Xử lý chat room
		btn_send.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Lấy text trong text field đẩy lên SV
				String s = tf.getText();
				try {
					// Gởi message với chuỗi vừa nhận lên SV để SV phân phối cho các client khác
					DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
					dos.writeUTF("message " + s);
					// Result hiện lên you chat và xuống dòng
					result.append("You : " + s + "\n");
					tf.setText("");
					// Set textField về rỗng
				} catch (Exception ex) {
					System.err.println(ex);
				}
			}
		});
	}

	private void loadImage() {
		try {
			// Load ảnh ra màn hình
			cardw = screenSize.width / 10;
			cardh = screenSize.height / 4;
			File f = new File(filePath + "back.png");

			// Lấy ảnh mặt sau các lá bài đại diện cho bài của đối thủ
			hidden = new BufferedImage(cardw, cardh, BufferedImage.TYPE_INT_ARGB);
			hidden = ImageIO.read(f);
			hidden = hidden.getScaledInstance(cardw, cardh, Image.SCALE_SMOOTH);
		} catch (IOException e) {
			System.out.println("Load image error!");
		}
		// Load bàn chơi
		try {
			File f = new File(filePath + "bg.png");
			ban = ImageIO.read(f);
		} catch (IOException e) {
			System.out.println("Load image error! ");
		}
	}

	// Tạo các component trong chat box
	private void GUI() {
		pn2 = new Panel(new FlowLayout());
		pn1 = new Panel(new GridLayout(1, 1));
		pn = new Panel(new FlowLayout());
		tf = new TextField();
		tf.setPreferredSize(new Dimension(screenSize.width - difw - 180, 25));
		btn_send = new Button("Send");
		result = new TextArea();
		result.setPreferredSize(new Dimension(screenSize.width - difw - 120, screenSize.height - difh - 70));
		result.setEditable(false);
		pn.setBounds(difw + 120, difh, screenSize.width - difw - 120, screenSize.height - difh);
		pn1.add(result);
		pn2.add(tf);
		pn2.add(btn_send);
		pn.add(pn1);
		pn.add(pn2);
		add(pn);
	}

	// Fill các lá bài của các người chơi khác
	@Override
	public void paint(Graphics g) {
		g = this.getGraphics();

		// Bàn chơi
		g.drawImage(ban, 0, 0, this);

		if (isStart) {
			// Người bên trái
			if (soNguoi >= 2)
				for (int k = 0; k < left_cards; k++)
					g.drawImage(hidden, this.getWidth() * 1 / 20, this.getHeight() * 1 / 9 + kc * k * 7 / 10, this);
			// Người đối diện
			if (soNguoi >= 3)
				for (int k = 0; k < oppo_cards; k++)
					g.drawImage(hidden, this.getWidth() * 1 / 5 + kc * k * 5 / 4, this.getHeight() * 1 / 20, this);
			// Người bên phải
			if (soNguoi == 4)
				for (int k = 0; k < right_cards; k++)
					g.drawImage(hidden, this.getWidth() * 17 / 20 - screenSize.width / 10,
							this.getHeight() * 1 / 9 + kc * k * 7 / 10, this);
			// Những người chơi khác chỉ cho thấy mặt sau của lá bài

			/*
			 * Bài của cá nhân phải vẽ các quân bài được chia, dựa theo ID Bài nào được chọn
			 * thì sẽ nhảy lên 40 đơn vị
			 */
			for (int k = 0; k < pl.myDeck.length; k++) {
				if (pl.myDeck[k] != null) {
					if (pl.myDeck[k].isChoosing)
						g.drawImage(pl.myDeck[k].img, this.getWidth() * 1 / 5 + kc * k * 5 / 4,
								this.getHeight() * 19 / 20 - screenSize.height / 4 - 40, this);
					else
						g.drawImage(pl.myDeck[k].img, this.getWidth() * 1 / 5 + kc * k * 5 / 4,
								this.getHeight() * 19 / 20 - screenSize.height / 4, this);
				}
			}

			// Những lá bài đánh xuống
			if (currentMove != null) {
				for (int k = 0; k < currentMove.length; k++)
					g.drawImage(currentMove[k].img, this.getWidth() * 6 / 20 + kc * k * 5 / 4,
							this.getHeight() * 7 / 20, this);
			}
		}
	}

	// Chạy tiến trình
	@Override
	public void run() {
		while (true) {
			try {
				DataInputStream dis = new DataInputStream(socket.getInputStream());
				String msg = dis.readUTF();
				String[] card = msg.split(" ");
				// game start

				// Kiểm tra các lệnh trong mảng lấy từ Server
				// Nếu Server ra lệnh bắt đầu --> lấy các thông tin về số người, ID
				// lấy bài được chia và gán cho từng người chơi, gọi hàm duel bắt đầu
				if (card[0].equals("duel")) {
					remove(btnReady);
					soNguoi = Integer.parseInt(card[1]);
					ID = Integer.parseInt(card[2]);
					System.out.println(ID);
					int[] result = new int[card.length - 3];
					for (int i = 0; i < card.length - 3; i++) {
						result[i] = Integer.parseInt(card[i + 3]);
					}
					Card[] tmp1 = new Card[result.length];
					for (int k = 0; k < result.length; k++) {
						tmp1[k] = new Card(result[k]);
					}
					pl.myDeck = tmp1;
					duel();
				}
				// Nếu nhận được Message thì thêm message vào khung kết quả (chat room)
				else if (card[0].equals("message")) {
					result.append(msg.substring(8) + "\n");
				}
				// Nếu nhận được lệnh cards
				// Hiển thị ra bài đối phương đánh
				// other player plays cards
				else if (card[0].equals("card")) {
					int[] result = new int[card.length - 2];
					for (int i = 0; i < card.length - 2; i++) {
						result[i] = Integer.parseInt(card[i + 1]);
					}
					Card[] tmp2 = new Card[result.length];
					for (int k = 0; k < result.length; k++) {
						tmp2[k] = new Card(result[k]);
					}
					currentMove = tmp2;
					int next = Integer.parseInt(card[card.length - 1]);
					System.out.println("Next: " + next);
					NextPlayer(next, currentMove.length);
					repaint();
				}
				// nếu nhận được lệnh drop thì chuyển lượt cho người tiếp theo
				// other player drop turn
				else if (card[0].equals("drop")) {
					int next = Integer.parseInt(card[card.length - 1]);
					NextPlayer(next, 0);
				}
				// hết 1 vòng đánh
				// end a round
				else if (card[0].equals("refresh")) {
					currentMove = null;
					int next = Integer.parseInt(card[card.length - 1]);
					NextPlayer(next, 0);
				}
				// Kết thúc game
				// end the game
				else if (card[0].equals("end")) {
					int[] result = new int[card.length - 1];
					for (int i = 0; i < card.length - 1; i++) {
						result[i] = Integer.parseInt(card[i + 1]);
					}
					for (int k = 0; k < result.length; k++) {
						if (result[k] == ID)
							JOptionPane.showMessageDialog(new JFrame(), "Top " + (k + 1));
					}
					endGame();
				}
				repaint();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// end the game
	private void endGame() {
		ID = 4;
		soNguoi = 0;
		pl.myDeck = new Card[13];
		currentMove = null;
		status = false;
		isStart = false;
		remove(timeTF);
		remove(btnDanhBai);
		remove(btnBoLuot);
		if (isCreator)
			add(btnReady);
		repaint();
	}

	// start the game
	private void duel() {
		isStart = true;
		isFinish = false;
		add(btnDanhBai);
		add(btnBoLuot);
		if (ID != 0)
			status = false;
		else
			status = true;
		if (isCreator)
			currentID = -1;
		else
			currentID = 0;
		btnDanhBai.setEnabled(status);
		btnBoLuot.setEnabled(status);
		left_cards = 13;
		right_cards = 13;
		oppo_cards = 13;
		interval = 60;
		if (status)
			add(timeTF);
		timer.start();
	}

	// Chuyển lượt chơi sang cho người tiếp theo
	// Kiểm tra số lượng các lá bài của đối thủ
	// Đánh ra bao nhiêu thì trừ đi số lượng đúng
	private void NextPlayer(int next, int cards) {
		if (next == ID) {
			status = true;
			interval = 60;
			add(timeTF);
		} else {
			status = false;
			remove(timeTF);
		}
		btnDanhBai.setEnabled(status);
		btnBoLuot.setEnabled(status);
		if (currentID != -1 && cards != 0) {
			if (isLeft(currentID))
				left_cards -= currentMove.length;
			if (isRight(currentID))
				right_cards -= currentMove.length;
			if (isOppo(currentID))
				oppo_cards -= currentMove.length;
		}
		currentID = next;
	}

	// Kiểm tra có phải lượt hiện tại có phải của người bên trái mình hay không
	boolean isLeft(int i) {
		int k = ID + 1;
		if (k >= soNguoi)
			k = 0;
		if (i == k)
			return true;
		return false;
	}

	// Kiểm tra có phải lượt hiện tại đang là của người bên phải hay không
	boolean isRight(int i) {
		if (soNguoi != 4)
			return false;
		int k = ID - 1;
		if (k < 0)
			k = soNguoi - 1;
		if (i == k)
			return true;
		return false;
	}

	// Kiểm tra lượt hiện tại có phải là của người đối diện hay không
	boolean isOppo(int i) {
		if (soNguoi < 3)
			return false;
		int k = ID + 1;
		if (k >= soNguoi)
			k = 0;
		k++;
		if (k >= soNguoi)
			k = 0;
		if (i == k)
			return true;
		return false;
	}
}

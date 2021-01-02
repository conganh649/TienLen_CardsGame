
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class LobbyFrame extends JFrame implements Runnable {

	ServerCard server;
	Socket soc;
	private String name;
	ArrayList<Room> listrooms = new ArrayList<Room>();
	DataInputStream dis = null;
	DataOutputStream dos = null;
	String[] part = null;
	@SuppressWarnings("rawtypes")
	private JList list;
	private JPanel contentPane;
	private JTextField find_txt;
	private JButton btnNewRoom;
	private JButton btnJoinRoom;
	private JButton btnExit;
	private JButton btnFindRoom;

	@SuppressWarnings({ "unchecked", "rawtypes" })

	// Giao diện sảnh chờ, gồm socket đang sử dụng để tương tác với Server
	// và tên player
	public LobbyFrame(Socket soc, String name) {
		this.name = name;
		this.soc = soc;
		try {
			// Khai báo các DIS và DOS để gởi và nhận dữ liệu
			dis = new DataInputStream(this.soc.getInputStream());
			String receive = dis.readUTF();
			part = receive.split("-"); // Lấy list phòng có trước để đổ ra list
			dos = new DataOutputStream(this.soc.getOutputStream());
		} catch (Exception e) {
		}
		this.setFocusable(true);
		setTitle("Welcome " + this.name);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(550, 300);
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		list = new JList(part);
		list.setPreferredSize(new Dimension(380, 230));
		btnNewRoom = new JButton("New Room");
		btnJoinRoom = new JButton("Join Room");
		btnExit = new JButton("Exit");
		find_txt = new JTextField();
		find_txt.setColumns(10);
		btnFindRoom = new JButton("Find room");

		Panel p2 = new Panel(new GridLayout(5, 1, 0, 20));
		p2.add(find_txt);
		p2.add(btnFindRoom);
		p2.add(btnNewRoom);
		p2.add(btnJoinRoom);
		p2.add(btnExit);

		contentPane.add(list);
		contentPane.add(p2);
		this.setVisible(true);
		addListener();
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// Tắt windows = thoát
				try {
					dos.writeUTF("Leave");
					dos.flush();
					dos.close();
					soc.close();
				} catch (Exception err) {
					System.out.println("Err");
				}
			}
		});
	}

	private void addListener() {
		btnNewRoom.addActionListener(new ActionListener() {
			@Override
			// Tạo room mới, gởi lệnh new và tên phòng sang server xử lý
			public void actionPerformed(ActionEvent arg0) {
				try {
					dos.writeUTF("new-" + name);
					Thread t;
					// Tạo 1 thread Playroom mới để vào phòng chơi, truyền luôn socket đang tương
					// tác với Server vào
					t = new Thread(new PlayRoom(soc, name, true));
					t.start();
					// Sau khi xong công việc của Thread thì hủy các tài nguyên sử dụng cho nó
					dispose();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		btnJoinRoom.addActionListener(new ActionListener() {
			@Override
			// Gởi lệnh join
			public void actionPerformed(ActionEvent arg0) {
				String jr = list.getSelectedValue().toString();
				// Lấy tên phòng và gởi lệnh lên Server, gởi tên phòng qua Server xử lý
				try {
					dos.writeUTF("join-" + jr);
					Thread t;
					t = new Thread(new PlayRoom(soc, name, false));
					// Join phòng nào thì tạo Thread vào phòng đó, truyền socket theo luôn
					t.start();
					dispose();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		btnExit.addActionListener(new ActionListener() {
			@Override
			// Exit gởi lệnh Leave lên server rồi thoát
			public void actionPerformed(ActionEvent arg0) {
				try {
					dos.writeUTF("Leave");
				} catch (IOException e) {
					e.printStackTrace();
				}
				dispose();
			}
		});
		btnFindRoom.addActionListener(new ActionListener() {
			@Override
			@SuppressWarnings("unchecked")
			// Tìm phòng, lấy tên phòng và gởi lệnh lên server
			// Server trả về list các phòng có tên, hiện phòng ra
			public void actionPerformed(ActionEvent arg0) {
				try {
					String fr = find_txt.getText();
					if (fr.isEmpty())
						dos.writeUTF("refresh");
					else
						dos.writeUTF("find-" + fr);
					// Lấy list phòng từ Server và tìm theo nội dung trong txt
					String receive = dis.readUTF();
					part = receive.split("-");
					List<String> result = new ArrayList<String>();
					String r = find_txt.getText();
					for (String room : part)
						if (room.contains(r))
							result.add(room);
					list.setListData(result.toArray());
				} catch (Exception e) {
				}

			}
		});
	}

	@Override
	public void run() {
	}
}

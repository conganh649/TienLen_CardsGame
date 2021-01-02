
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerCard {

	// Khai báo Server socket và các ListRoom sẽ có trong Server
	ServerSocket server;
	ArrayList<Room> Rooms = new ArrayList<Room>();

	public static void main(String[] args) {
		new ServerCard();
	}

	// Mở Server với port 3000, IPAddress là Localhost
	public ServerCard() {
		try {
			server = new ServerSocket(3000);
			while (true) {
				Socket soc = server.accept();

				// Tạo 1 thread Lobby
				Thread lobby = new Lobby(this, soc);
				lobby.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// return results for the room name search -> Trả về List Room name hiện trên
	// màn hình
	public String getRoomsName(String find) {
		if (Rooms.size() == 0)
			return "-";
		else {
			String rooms = "-";
			for (Room r : Rooms)
				if (r.name.contains(find))
					rooms += "-" + r.name;
			rooms = rooms.substring(2);
			return rooms;
		}
	}
}

// Luồng Lobby
class Lobby extends Thread {
	Socket soc;
	ServerCard sv;

	// khởi tạo luồng lobby với tham số là Server đang mở và Socket tương tác tới
	public Lobby(ServerCard sv, Socket soc) throws IOException {
		this.sv = sv;
		this.soc = soc;
	}

	@Override
	public void run() {
		try {

			String username = null;
			Player pl = null;

			// Đẩy toàn bộ Room names ra socket để hiển thị
			DataOutputStream dos = new DataOutputStream(soc.getOutputStream());
			dos.writeUTF(sv.getRoomsName(""));

			// Lấy thông báo (lệnh gởi từ Client lên)
			DataInputStream dis = new DataInputStream(soc.getInputStream());
			String z = dis.readUTF();
			String[] z_split = z.split("-");

			// user login
			if (z_split[0].equals("login")) {
				username = z_split[1];
				pl = new Player(username);
				System.out.println(pl.Name + " logged in");
				// In thông báo ra màn hình Terminal của Server
			}

			// Bật cờ lên True
			boolean flag = true;
			while (flag) {

				// Tiếp tục nhận lệnh từ Client gởi lên, luôn luôn lắng nghe
				z = dis.readUTF();
				z_split = z.split("-");
				// Lệnh new -> Tạo 1 phòng mới
				if (z_split[0].equals("new")) {
					Room t = new Room(sv, soc, pl, z_split[1]);
					sv.Rooms.add(t);
					System.out.println("Create " + t.name);
					// Tắt cờ đi không nhận lệnh nữa vì đã vào Room rồi
					flag = false;
					t.start();
					// Luồng Room start
				}
				// nhận lệnh join -> Vô room có sẵn với name đúng
				else if (z_split[0].equals("join")) {
					String rname = z_split[1];
					for (Room r : sv.Rooms) {
						if (r.name.equals(rname)) {
							r.joinRoom(soc, pl);
							System.out.println(pl.Name + " join " + r.name);
						}
					}
					flag = false;
				}
				// Tìm room theo tên
				else if (z_split[0].equals("find")) {
					String fname = z_split[1];
					dos.writeUTF(sv.getRoomsName(fname));
				}
				// Reload lại list room để chọn
				else if (z_split[0].equals("refresh")) {
					dos.writeUTF(sv.getRoomsName(""));
				}
				// Thóat game
				else if (z_split[0].equals("Leave")) {
					System.out.println(username + " leaved");
					flag = false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
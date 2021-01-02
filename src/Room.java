
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class Room extends Thread {

	// Khai báo phòng chơi
	static int countid = 1;
	ServerCard server;
	ArrayList<Socket> PlayerSocket = new ArrayList<Socket>();
	String name;
	int ID;
	String currentPlay;
	ArrayList<Player> Players = new ArrayList<Player>();
	Deck bb = new Deck();
	ArrayList<String> playerCurrentRounds = new ArrayList<String>();
	int nextplayer = 0;
	ArrayList<String> FinishedPlayer = new ArrayList<String>();
	boolean flag = true;

	// Constructor
	public Room(ServerCard server, Socket soc, Player pl, String name) {
		this.server = server;
		PlayerSocket.add(soc);
		Players.add(pl);
		this.name = name + "'s Room";
		this.ID = countid++;
		Client cli = new Client(server, this, soc, pl);
		cli.start();
	}

	// Mỗi phòng chơi sẽ có 1 socket tương tác với Server - mỗi người có 1 socket
	public void joinRoom(Socket soc, Player pl) {
		if (PlayerSocket.size() < 4) {
			PlayerSocket.add(soc);
			Players.add(pl);
			Client cli = new Client(server, this, soc, pl);
			cli.start();
		}
	}

	// Chia bài cho người chơi
	public void DistributeCard() {
		this.bb.Shuffle();
		for (int i = 0; i < this.Players.size(); i++) {
			this.Players.get(i).myDeck = Arrays.copyOfRange(this.bb.Deck, 13 * i, 13 * i + 13);
		}
	}

	@Override
	public void run() {
		while (flag) {
		}
		server.Rooms.remove(this);
	}
}

class Client extends Thread {
	// Client
	ServerCard server;
	Socket socket;
	Room room;
	Player player;
	boolean flag;

	public Client(ServerCard server, Room r, Socket soc, Player pl) {
		this.server = server;
		this.socket = soc;
		this.room = r;
		this.player = pl;
		flag = true;
	}

	@Override
	public void run() {
		while (flag) {
			// Trong khi phòng chơi vẫn còn
			try {

				// Client liên tục nhận các thông điệp từ SV

				DataInputStream dis = new DataInputStream(socket.getInputStream());
				String msg = dis.readUTF();

				// Tách thông điệp ra
				String[] part = msg.split(" ");

				// Kiểm tra, nếu thông điệp trong Socket là message
				if (part[0].equals("message"))

					// Gởi message đọc được ra socket để gởi cho các Client khác
					sendClient(msg.substring(8), socket);

				// Nếu thông điệp là đánh bài
				else if (part[0].equals("card")) {
					if (msg.contains("Finish")) {
						// Nếu hết bài, thêm người chơi đã đánh xong vào FinishedPlayer
						room.FinishedPlayer.add(room.playerCurrentRounds.get(room.nextplayer));

						// Bỏ người chơi đánh xong ra khỏi vòng đánh bài
						room.playerCurrentRounds.remove(room.nextplayer);
						msg = msg.substring(0, msg.length() - 6);
						System.out.println("Finish" + room.FinishedPlayer);
						System.out.println("Current" + room.playerCurrentRounds);

						// Chuyển lượt chơi cho người tiếp theo
						if (room.nextplayer >= room.playerCurrentRounds.size())
							room.nextplayer = 0;
						room.currentPlay = room.playerCurrentRounds.get(room.nextplayer);
					} else {
						room.currentPlay = room.playerCurrentRounds.get(room.nextplayer);
						room.nextplayer++;
						if (room.nextplayer >= room.playerCurrentRounds.size())
							room.nextplayer = 0;
					}
					/*
					 * Nếu không phải lệnh finish --> vẫn còn đánh bài, gọi hàm play Card để đánh
					 * bài Truyền theo socket đi
					 */

					playCard(msg + room.playerCurrentRounds.get(room.nextplayer), socket);
				}
				// Nếu thông điệp là rời khỏi phòng
				else if (part[0].equals("Leave")) {
					// Đóng socket -> in ra người chơi rời phòng
					socket.close();
					System.out.println(player.Name + " leaved");
					// Bỏ socket của player rời phòng ra khỏi room -> remove luôn player
					room.PlayerSocket.remove(socket);
					room.Players.remove(player);
					// Nếu trong phòng không còn ai nữa thì xóa luôn phòng -> tắt cờ
					if (room.Players.size() == 0) {
						server.Rooms.remove(room);
						System.out.println(room.name + " removed");
						room.flag = false;
					}
					flag = false;
				}
				// Nếu lệnh nhận được là bắt đầu
				else if (part[0].equals("Play")) {
					// Kiểm tra phải có 2 người chơi trở lên mới đc chia bài
					if (room.Players.size() > 1) {
						room.DistributeCard();
						for (int i = 0; i < room.Players.size(); i++) {
							// Mỗi người chơi phải đc sắp xếp bài tự động từ nhỏ đến lớn
							room.Players.get(i).SortCard();
							room.playerCurrentRounds.add(i + "");
							room.nextplayer = 0;

							// Gởi lệnh duel vào trong PlayRoom để các người chơi bắt đầu chơi
							DataOutputStream dos = new DataOutputStream(room.PlayerSocket.get(i).getOutputStream());
							String s = "duel " + room.Players.size() + " " + i + " " + room.Players.get(i).Display();
							dos.writeUTF(s);
						}
					}
				}
				// Nhận lệnh bỏ lượt
				else if (part[0].equals("drop")) {
					// Chuyển lượt sang cho người tiếp theo
					room.nextplayer++;
					// Nếu lượt chơi > số người chơi -> về lại người đầu tiên
					if (room.nextplayer >= room.playerCurrentRounds.size())
						room.nextplayer = 0;

					// Nếu cùng 1 người đánh, tức là không ai bắt được bài --> đánh lại lượt mới
					if (room.currentPlay.equals(room.playerCurrentRounds.get(room.nextplayer)))
						playCard("refresh " + room.playerCurrentRounds.get(room.nextplayer), null);
					else
						playCard("drop " + room.playerCurrentRounds.get(room.nextplayer), socket);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Khi đã xác định kết quả -> kết thúc game
	private void endGame() {
		String end = "end ";
		for (int i = 0; i < room.FinishedPlayer.size(); i++) {
			// Lấy thứ tự xếp hạng ra
			end += room.FinishedPlayer.get(i) + " ";
		}
		end += room.playerCurrentRounds.get(0);
		for (Socket i : room.PlayerSocket) {
			try {
				// Lần lượt gởi kết quả cho các client trong Play Room
				DataOutputStream dos = new DataOutputStream(i.getOutputStream());
				dos.writeUTF(end);
				System.out.println(end);
			} catch (Exception e) {
				System.err.println("Error3!");
			}
		}
		room.FinishedPlayer.clear();
		room.playerCurrentRounds.clear();
		room.currentPlay = null;
		room.nextplayer = 0;
	}

	// Đánh bài
	private void playCard(String c, Socket soc) {
		for (Socket i : room.PlayerSocket) {
			try {
				// Gởi chuỗi bài đánh vào socket để xử lý
				if (!(i.equals(soc))) {
					DataOutputStream dos = new DataOutputStream(i.getOutputStream());
					dos.writeUTF(c);
					System.out.println(c);
				}
			} catch (Exception e) {
				System.err.println("Error3!");
			}
		}
		// Nếu chỉ còn 1 người có bài thì kết thúc game
		if (room.playerCurrentRounds.size() == 1) {
			endGame();
		}
	}

	// gởi tin nhắn cho các client khác (chat room)
	private void sendClient(String str, Socket soc) {
		for (Socket i : room.PlayerSocket) {
			try {
				if (!(i.equals(soc))) {
					DataOutputStream dos = new DataOutputStream(i.getOutputStream());
					dos.writeUTF("message " + player.Name + " : " + str);
				}
			} catch (Exception e) {
				System.err.println("Error3!");
			}
		}
	}
}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Player {
	// Player gồm: Tên, ID của lá bài, bộ bài đang giữ trong tay (gồm 13 lá max)
	// và có phải turn hay không
	String Name;
	public int ID;
	public Card[] myDeck = new Card[13];
	public boolean isMyTurn = false;

	public Player(String Name) {
		this.Name = Name;
	}

	// Trả về 1 String các ID của lá bài -> bài cầm trên tay
	public String Display() {
		String s = "";
		for (int i = 0; i < 13; i++) {
			s += myDeck[i].ID + " ";
		}
		return s;
	}

	// Xếp bài từ id bé đến id lớn, tức là xếp từ 3 -> heo, giống nhau thì
	// Bích chuồng rô cơ lần lượt
	public void SortCard() {
		int n = this.myDeck.length;
		for (int i = 0; i < n - 1; i++) {
			int min_idx = i;
			for (int j = i + 1; j < n; j++)
				if (myDeck[j].ID < myDeck[min_idx].ID)
					min_idx = j;
			Card temp = myDeck[min_idx];
			myDeck[min_idx] = myDeck[i];
			myDeck[i] = temp;
		}
	}

	// Trả về max của bộ Card đang xét (đôi, sam, tứ quý, sảnh)
	int max(Card[] current) {
		int max = current[0].ID;
		for (int i = 0; i < current.length; i++) {
			if (current[i].ID > max)
				max = current[i].ID;
		}
		return max;
	}

	// Trả về xem có phải 1 đôi, sam, tứ quý hay không
	boolean isKind(Card[] current) {
		for (int i = 0; i < current.length - 1; i++)
			if (current[i].ID / 4 != current[i + 1].ID / 4)
				return false;
		return true;
	}

	// Kiểm tra có phải sảnh hay không
	boolean isStraight(Card[] current) {
		int len = current.length;
		// length <=2 thì không phải sảnh
		if (len <= 2)
			return false;

		// Tạo 1 List các Id để xét sảnh
		List<Integer> straight = new ArrayList<Integer>();
		for (Card i : current) {
			straight.add(i.ID);
		}

		// Sort lại sảnh từ nhỏ đến lớn
		Collections.sort(straight);
		// Sảnh mà có con heo thì sai luôn
		if (current[len - 1].ID / 4 == 12)
			return false;

		// Cái sau lớn hơn cái đầu 1 nút thì nó là sảnh
		for (int i = 1; i < len; i++) {
			if (straight.get(i) / 4 - straight.get(i - 1) / 4 != 1)
				return false;
		}
		return true;
	}

	// Kiểm tra đôi thông
	boolean isStraightPair(Card[] current) {
		int len = current.length;
		// Length mà lẻ thì sai
		// Length phải từ 6 trở lên -> 3 đôi thông trở lên
		if (len % 2 != 0 || len < 6)
			return false;
		List<Integer> straight = new ArrayList<Integer>();

		// Add card vô để check
		for (Card i : current) {
			straight.add(i.ID);
		}
		// Sort lại
		Collections.sort(straight);

		// Có con heo thì sai
		if (current[len - 1].ID / 4 == 12)
			return false;

		// Check các con bài
		for (int i = 1; i < len; i += 2) {
			// 2 con liên tiếp mà khác nhau thì sai
			if (straight.get(i) - straight.get(i - 1) != 0)
				return false;
			// Ví dụ 3 đôi thông thì 1 3 5 và 2 4 6 phải cách nhau 1 đơn vị
			if (i > 2)
				if (straight.get(i) - straight.get(i - 2) != 1)
					return false;
		}
		return true;
	}

	// Kiểm tra lượt đánh bài
	@SuppressWarnings("unused")
	int check(Card[] current, Card[] move) {
		// Bài đánh ra null thì trả về 0 -> kh đi được
		if (move == null)
			return 0;

		// Nếu bài đang ở trên bàn là null và bài đánh xuống khác null
		if (current == null && move != null) {
			// Đánh xuống 1 lá thì là cạch lẻ cho đánh
			if (move.length == 1) {
				return 1;
			}
			// Nếu đánh xuống 2 lá thì phải đánh đôi, tức là id / 4 phải = nhau
			else if (move.length == 2) {
				if (move[0].ID / 4 == move[1].ID / 4) {
					return 1;
				} else
					return 0;
			} else if (move.length >= 3) {
				// Nếu đánh xuống >= 3 lá thì phải đánh sam, tứ quý hoặc đánh sảnh
				if (isKind(move) || isStraight(move)) {
					return 1;
				}
			} else
				return 0;

			// Bài ở trên bàn lượt trước có 1 lá, đánh xuống 1 lá
			// Lá đánh xuống phải lớn hơn lá trên bàn
		} else if (current.length == 1 && move.length == 1) {
			if (current[0].ID < move[0].ID) {
				return 1;
			} else
				return 0;
			// Nếu đánh xuống 2 lá --> đánh đôi
			// Current phải là đôi, move xuống cũng phải đôi
			// Đôi đánh xuống phải lớn hơn đôi trên bàn đang có <max sau hơn max trên bàn>
		} else if (current.length == 2 && move.length == 2) {
			if ((current[0].ID / 4 == current[1].ID / 4) && (move[0].ID / 4 == move[1].ID / 4))
				if (max(move) > max(current))
					return 1;
				else
					return 0;
			// Nếu đánh xuống 3 lá hoặc 4 lá
			// Nếu là Sam thì cả 2 phải cùng là Sam, tứ quý thì cả 2 phải cùng là tứ quý
			// Đánh xuống phải lớn hơn bài trên bàn
		} else if ((current.length == 3 && move.length == 3) || (current.length == 4 && move.length == 4)) {
			if (isKind(current) && isKind(move)) {
				if (max(move) > max(current))
					return 1;
				else
					return 0;
			}

			// Nếu là sảnh thì sảnh đánh xuống phải lớn hơn sảnh trên bàn
			if (isStraight(current) && isStraight(move)) {
				if (max(move) > max(current))
					return 1;
				else
					return 0;
			} else
				return 0;

			// >=5 thì chắc chắn phải là sảnh hoặc đôi thông
			// Kiểm tra có phải sảnh / đôi thông hay không
			// Cái sau đánh xuống phải lớn hơn cái trên bàn
		} else if (current.length == move.length) {
			if (isStraight(current) && isStraight(move)) {
				if (max(move) > max(current))
					return 1;
				else
					return 0;
			}
			if (isStraightPair(current) && isStraightPair(move)) {
				if (max(move) > max(current))
					return 1;
				else
					return 0;
			} else
				return 0;
		}
		// Nếu các cái if sai hết -> không cho đánh
		return 0;
	}

	// Lấy bài ra, lấy cái gì thì truyền vào
	Card[] Pop(Card[] current) {
		for (int i = 0; i < current.length; i++) {
			for (int k = 0; k < myDeck.length; k++) {

				// Kiểm tra bài lấy ra có trong bộ bài đang cầm trên tay
				if (current[i].ID == myDeck[k].ID) {
					// Lấy con nào ra thì các con còn lại đôn lên
					for (int m = k; m < myDeck.length - 1; m++) {
						myDeck[m] = myDeck[m + 1];
					}
					myDeck = Arrays.copyOfRange(myDeck, 0, myDeck.length - 1);
					// Tạo ra lại 1 mảng bài mới (1 set trên tay mới sau khi lấy bài ra)
				}
			}
		}
		// Trả ra
		return myDeck;
	}

	// Lấy bài ra (lấy ID)
	Card getCard(int ID) {
		for (int i = 0; i < myDeck.length; i++) {
			// Có ID trong bộ bài đang cầm thì trả ra lá đó
			if (myDeck[i].ID == ID)
				return myDeck[i];
		}
		return null;
	}

	// Đánh bài xuống thì truyền vào
	// Bài trên bàn trước đó và bài đánh xuống
	// Check bước đi hợp lệ thì cho đánh và ngược lại
	boolean PlayCard(Card[] current, Card[] move) {
		return (check(current, move) == 1) ? true : false;
	}
}

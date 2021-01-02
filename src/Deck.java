import java.util.Random;

public class Deck {
	Card[] Deck = new Card[52];
	String[] Set = { "Spades", "Clubs", "Diamonds", "Hearts" };
	String[] Value = { "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace", "2" };

	// Khởi tạo 1 mảng bài với id từ 1 -> 52 tương ứng với bài từ 3 bích -> 2 cơ
	Deck() {
		for (int i = 0; i < 52; i++) {
			Deck[i] = new Card(i);
		}
	}

	// xáo bài lên
	public void Shuffle() {
		int index;
		Card temp;
		Random random = new Random();
		for (int i = Deck.length - 1; i > 0; i--) {
			index = random.nextInt(i + 1);
			temp = Deck[index];
			Deck[index] = Deck[i];
			Deck[i] = temp;
		}
	}
}
package btClient;

import java.util.Comparator;

public class PieceRarityComparator implements Comparator<Piece> {
	@Override
	public int compare(Piece x, Piece y) {
		return y.getPeerCount() - x.getPeerCount();
	}
}

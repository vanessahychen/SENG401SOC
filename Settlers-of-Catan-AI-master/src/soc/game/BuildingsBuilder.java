package soc.game;

public class BuildingsBuilder {
	public Pieces prepareStartingBuildings (SOCPlayer pl, SOCBoard board, int r1, int r2, int s1, int s2) {
		Pieces pieces = new Pieces();
		pieces.addItem(new Roads(pl, r1, board));
		pieces.addItem(new Roads(pl, r2, board));
		pieces.addItem(new Settlements(pl, s1, board));
		pieces.addItem(new Settlements(pl, s2, board));
		return pieces;
	}
	
	public Pieces prepareStartingBuildings2 (SOCPlayer pl, SOCBoard board, int r1, int r2, int s1) {
		Pieces pieces = new Pieces();
		pieces.addItem(new Roads(pl, r1, board));
		pieces.addItem(new Roads(pl, r2, board));
		pieces.addItem(new Settlements(pl, s1, board));
		return pieces;
	}
	
}

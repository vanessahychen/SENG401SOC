package soc.game;

public class Settlements extends Buildings {

	public Settlements(SOCPlayer pl, int co, SOCBoard board) {
		super(1, pl, co, board);
	}

	@Override
	public SOCResourceSet resourceCost() {
		return new SOCResourceSet(1, 0, 1, 1, 1, 0);
	}

	@Override
	public int coords() {
		return coord;
	}

	@Override
	public Hex hex() {
		return new Hex(coord);
	}

	@Override
	public SOCPlayingPiece getPlayingPiece() {
		return this;
	}

}

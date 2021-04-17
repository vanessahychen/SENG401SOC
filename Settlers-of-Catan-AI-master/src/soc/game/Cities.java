package soc.game;

public class Cities extends Buildings {

	public Cities(SOCPlayer pl, int co, SOCBoard board) {
		super(2, pl, co, board);
	}

	@Override
	public SOCResourceSet resourceCost() {
		return new SOCResourceSet(0, 3, 0, 2, 0, 0);
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

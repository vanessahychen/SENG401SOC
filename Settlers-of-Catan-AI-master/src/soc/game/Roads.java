package soc.game;

public class Roads extends Buildings {

	public Roads(SOCPlayer pl, int co, SOCBoard board) {
		super(0, pl, co, board);
	}

	@Override
	public SOCResourceSet resourceCost() {
		return new SOCResourceSet(1, 0, 0, 0, 1, 0);
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

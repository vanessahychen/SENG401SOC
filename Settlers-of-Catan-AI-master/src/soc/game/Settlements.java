package soc.game;

public class Settlements extends Buildings {

	public Settlements(int currentCoord) {
		super(currentCoord);
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

}

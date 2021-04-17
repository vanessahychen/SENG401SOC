package soc.game;

public class Cities extends Buildings {

	public Cities(int currentCoord) {
		super(currentCoord);
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

}

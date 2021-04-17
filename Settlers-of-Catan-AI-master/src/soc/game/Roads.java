package soc.game;

public class Roads extends Buildings {

	public Roads(int currentCoord) {
		super(currentCoord);
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

}

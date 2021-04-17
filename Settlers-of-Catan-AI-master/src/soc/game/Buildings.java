package soc.game;

public abstract class Buildings implements Item {
	
	protected int coord;
	
	public Buildings (int currentCoord) {
		this.coord = currentCoord;
	}
	
	public int assemble() {
		Hex r = new Hex(coord);
		return r.assemble();
	}
}

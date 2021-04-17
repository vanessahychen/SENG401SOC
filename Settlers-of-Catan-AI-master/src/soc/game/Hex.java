package soc.game;

public class Hex implements BoardLocation{
	private int coord;
	
	public Hex (int currentCoord) {
		this.coord = currentCoord;
	}
	
	@Override
	public int assemble() {
		return coord;
	}

}

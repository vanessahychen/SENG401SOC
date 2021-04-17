package soc.game;

public abstract class Buildings extends SOCPlayingPiece implements Item {
	
	protected int coord;
	
	public Buildings (final int ptype, SOCPlayer pl, final int currentCoord, SOCBoard pboard) {
		super(ptype, pl, currentCoord, pboard);
		this.coord = currentCoord;
	}
	
	public int assemble() {
		Hex r = new Hex(coord);
		return r.assemble();
	}
}

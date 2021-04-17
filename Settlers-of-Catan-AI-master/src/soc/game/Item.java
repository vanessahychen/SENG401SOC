package soc.game;

public interface Item {
	public int coords();
	public Hex hex();
	public SOCResourceSet resourceCost();
	public SOCPlayingPiece getPlayingPiece();
}

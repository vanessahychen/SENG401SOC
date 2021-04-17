package soc.game;

import java.util.Vector;

public class Pieces {
	private Vector<Item> items = new Vector<Item>();
	
	public void addItem(Item anItem) {
		items.add(anItem);
	}
	
	public SOCResourceSet getResourceCost() {
		int cl = 0, or = 0, sh = 0, wh = 0, wo = 0, uk  = 0;
		for (Item it : items) {
			cl += it.resourceCost().getAmount(0);
			or += it.resourceCost().getAmount(1);
			sh += it.resourceCost().getAmount(2);
			wh += it.resourceCost().getAmount(3);
			wo += it.resourceCost().getAmount(4);
			uk += it.resourceCost().getAmount(5);
		}
		SOCResourceSet resSet = new SOCResourceSet(cl, or, sh, wh, wo, uk);
		return resSet;
	}

	/**
	 * @return the items
	 */
	public Vector<Item> getItems() {
		return items;
	}

	/**
	 * @param items the items to set
	 */
	public void setItems(Vector<Item> items) {
		this.items = items;
	}
}

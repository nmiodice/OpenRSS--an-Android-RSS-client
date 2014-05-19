package com.iodice.utilities;

public interface SelectorRefreshCallback extends Callback {
	public abstract void refreshCurrentSelector();
	public abstract void refreshCurrentSelectorMaintainSelection();
}

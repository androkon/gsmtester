package ru.konsoft.gsmtester;

public class Info {

	public long time;

	public int slot;
	
	public int level;
	public int progress;
	public int nettype;
	public String operator = new String();
	public int opercode;
	public int lac;
	public int cid;
	
	public int datastate;
	public long rx;
	public long tx;
	public int srx;
	public int stx;

	public Info(int i) {
		this.slot = i + 1;
	}
}

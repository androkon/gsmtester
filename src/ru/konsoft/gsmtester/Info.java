package ru.konsoft.gsmtester;

public class Info {

	public long time = 0;;

	public double lat = 0;
	public double lon = 0;

	public int level = 0;
	public int nettype = 0;
	public String operator = "";
	public int lac = 0;
	public int cid = 0;

	public boolean equals(Info i)
	{
		if(
			this.lat == i.lat &&
			this.lon == i.lon &&
			this.level == i.level &&
			this.nettype == i.nettype &&
			this.operator.equalsIgnoreCase(i.operator) &&
			this.lac == i.lac &&
			this.cid == i.cid
		)
			return true;
		else
			return false;
	}
	
}

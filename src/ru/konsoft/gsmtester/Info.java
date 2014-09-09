package ru.konsoft.gsmtester;

public class Info {

	public long time;

	public double lat;
	public double lon;
	
	public float accuracy;

	public int level;
	public int nettype;
	public String operator = "";
	public int opercode;
	public int lac;
	public int cid;
	
	public Info(){}
	
	public Info(Info i){
		this.lat = i.lat;
		this.lon = i.lon;
		
		this.level = i.level;
		this.nettype = i.nettype;
		this.operator = new String(i.operator);
		this.opercode = i.opercode;
		this.lac = i.lac;
		this.cid = i.cid;
		
	}

	public boolean equals(Info i)
	{
		if(
			this.lat == i.lat &&
			this.lon == i.lon &&
			
			this.level == i.level &&
			this.nettype == i.nettype &&
			this.operator.equalsIgnoreCase(i.operator) &&
			this.opercode == i.opercode &&
			this.lac == i.lac &&
			this.cid == i.cid
		)
			return true;
		else
			return false;
	}
	
	public String getDiff(Info i){
		String s = "";
		
		if(this.lat != i.lat)
			s += "lat;";
		if(this.lon != i.lon)
			s += "lon;";
		if(this.level != i.level)
			s += "level;";
		if(this.nettype != i.nettype)
			s += "nt;";
		if(! this.operator.equalsIgnoreCase(i.operator))
			s += "op;";
		if(this.opercode != i.opercode)
			s += "opc;";
		if(this.lac != i.lac)
			s += "lac;";
		if(this.cid != i.cid)
			s += "cid;";
		
		return s;
	}
	
}

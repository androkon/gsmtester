package ru.konsoft.gsmtester;

public class Info {

	public long time = 0;

	public double lat = 0;
	public double lon = 0;
	
	public float accuracy = 0;

	public int level = 0;
	public int nettype = 0;
	public String operator = "";
	public int opercode = 0;
	public int lac = 0;
	public int cid = 0;
	
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

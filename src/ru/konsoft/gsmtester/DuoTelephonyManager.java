package ru.konsoft.gsmtester;

import java.lang.reflect.Method;

import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.SignalStrength;
import android.util.Log;

public class DuoTelephonyManager extends Object
{
	private TelephonyManager tm;
	
	//private Class<? extends TelephonyManager> tl;
	private Method[] m = new Method[5];
	
	private static final int M_LISTEN = 0;
	private static final int M_NAME = 1;
	private static final int M_TYPE = 2;
	private static final int M_OPERATOR = 3;
	private static final int M_CELL = 4;
	
	public boolean duoReady = false;
	
	public DuoTelephonyManager(TelephonyManager telephonyManager){
		this.tm = telephonyManager;

		Class<? extends TelephonyManager> tl = tm.getClass();
		Method mm[] = tl.getMethods();
		for(int i = 0; i < mm.length; i++){
			//Log.e("qwertl", mm[i].toString());
		}
		Class<?> args[];
		try{
			args = new Class[3];
			args[0] = PhoneStateListener.class;
			args[1] = Integer.TYPE;
			args[2] = Integer.TYPE;
			m[M_LISTEN] = tl.getDeclaredMethod("listenGemini", args);
			args = new Class[1];
			args[0] = Integer.TYPE;
			m[M_NAME] = tl.getDeclaredMethod("getNetworkOperatorNameGemini", args);
			m[M_TYPE] = tl.getDeclaredMethod("getNetworkTypeGemini", args);
			m[M_OPERATOR] = tl.getDeclaredMethod("getNetworkOperatorGemini", args);
			m[M_CELL] = tl.getDeclaredMethod("getCellLocationGemini", args);
			duoReady = true;
			//o = (String) m[0].invoke(tl, 1);
			//Log.e("qwer", "listen");
		}catch(Exception e){
			Log.e("qwererr", e.toString());
		}

	}
	
	public String getNetworkOperatorName(int sim)
	{
		String name;
		try{
			name = (String) m[M_NAME].invoke(tm, sim);
		}catch(Exception e){
			name = null;
		}
		return name;
		//return tm.getNetworkOperatorName();
	}

	public int getNetworkType(int sim)
	{
		int type;
		try{
			type = (int) m[M_TYPE].invoke(tm, sim);
		}catch(Exception e){
			type = 0;
		}
		return type;
		//return tm.getNetworkType();
	}

	public String getNetworkOperator(int sim)
	{
		String operator;
		try{
			operator = (String) m[M_OPERATOR].invoke(tm, sim);
		}catch(Exception e){
			operator = null;
		}
		return operator;
		//return tm.getNetworkOperator();
	}

	public CellLocation getCellLocation(int sim)
	{
		CellLocation cell;
		try{
			cell = (CellLocation) m[M_CELL].invoke(tm, sim);
		}catch(Exception e){
			cell = null;
		}
		return cell;
		//return tm.getCellLocation();
	}

	public void listen(PhoneStateListener phoneStateListener, int events, int sim) {
		//Class<? extends PhoneStateListener> ll = phoneStateListener.getClass();
		//Method mm[] = ll.getMethods();
		//for(int i = 0; i < mm.length; i++){
			//Log.e("qwerll", mm[i].toString());
		//}
		//SignalStrength ss = new SignalStrength();
		//mm = ss.getClass().getMethods();
		//for(int i = 0; i < mm.length; i++){
			//Log.e("qwerss", mm[i].toString());
		//}
		try{
			m[M_LISTEN].invoke(tm, phoneStateListener, events, sim);
		}catch(Exception e){
			Log.e("qwererr", e.toString());
		}
		//tm.listen(phoneStateListener, events);
	}
	
}

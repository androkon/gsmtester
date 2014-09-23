package ru.konsoft.gsmtester;

import java.lang.reflect.Method;

import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.*;

public class DuoTelephonyManager {
	private static TelephonyManager tm;
	
	private static Method[] m = new Method[6];
	
	private static final int M_LISTEN = 0;
	private static final int M_NAME = 1;
	private static final int M_TYPE = 2;
	private static final int M_OPERATOR = 3;
	private static final int M_CELL = 4;
	private static final int M_DATASTATE = 5;
	private static final int M_TEST = 6;
	
	public boolean duoReady = false;
	
	public DuoTelephonyManager(TelephonyManager telephonyManager, GSMinfo activiti) {
		tm = telephonyManager;
		Class<? extends TelephonyManager> tl = tm.getClass();
		Class<?> args[], params[], ret;
		
		try{
			Method[] mm = tl.getDeclaredMethods();
			StringBuilder sb = new StringBuilder();
			for(Method mi: mm){
				params = mi.getParameterTypes();
				ret = mi.getReturnType();
				
				sb.append(mi.getName()).append("\n");
				for(Class<?> param: params) {
					sb.append("\t").append(param.getCanonicalName()).append("\n");
				}
				sb.append("\t\t").append(ret.getCanonicalName()).append("\n");
			}
			activiti.debug(sb.toString());
			
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
			m[M_DATASTATE] = tl.getDeclaredMethod("getDataStateGemini", args);
			//m[M_TEST] = tl.getDeclaredMethod("getNetworkClass", null);
			
			duoReady = true;
		}catch(Exception e){
			Log.e("qwererr", Debug.stack(e));
		}
	}
	
	public String getNetworkOperatorName(int sim) {
		String name = "";
		
		try{
			name = (String) m[M_NAME].invoke(tm, sim);
		}catch(Exception e){
			Log.e("qwererr", Debug.stack(e));
		}
		
		return name;
	}

	public int getNetworkType(int sim) {
		int type = 0;
		
		try{
			type = (Integer) m[M_TYPE].invoke(tm, sim);
		}catch(Exception e){
			Log.e("qwererr", Debug.stack(e));
		}
		
		return type;
	}

	public String getNetworkOperator(int sim) {
		String operator = "";
		
		try{
			operator = (String) m[M_OPERATOR].invoke(tm, sim);
		}catch(Exception e){
			Log.e("qwererr: " + sim, Debug.stack(e));
		}
		
		if("".equals(operator))
			operator = "0";
			
		return operator;
	}

	public CellLocation getCellLocation(int sim) {
		CellLocation cell = null;
		
		try{
			cell = (CellLocation) m[M_CELL].invoke(tm, sim);
		}catch(Exception e){
			Log.e("qwererr", Debug.stack(e));
		}
		
		return cell;
	}

	public int getDataState(int sim) {
		int state = 0;

		try{
			state = (Integer) m[M_DATASTATE].invoke(tm, sim);
		}catch(Exception e){
			Log.e("qwererr", Debug.stack(e));
		}

		return state;
	}
	
	public String getTest(int sim) {
		String name = "";

		try{
			name = (String) m[M_TEST].invoke(tm);
		}catch(Exception e){
			Log.e("qwererr: " + sim, Debug.stack(e));
		}

		if("".equals(name))
			name = "unknown";

		return name;
	}
	
	public void listen(PhoneStateListener phoneStateListener, int events, int sim) {
		try{
			m[M_LISTEN].invoke(tm, phoneStateListener, events, sim);
		}catch(Exception e){
			Log.e("qwererr", Debug.stack(e));
		}
	}
	
}

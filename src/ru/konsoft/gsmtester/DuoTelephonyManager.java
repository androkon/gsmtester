package ru.konsoft.gsmtester;

import java.lang.reflect.Method;

import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class DuoTelephonyManager extends Object
{
	private TelephonyManager tm;
	private Method[] m = new Method[5];
	
	public DuoTelephonyManager(TelephonyManager __tm){
		this.tm = __tm;

		Class<? extends TelephonyManager> c = tm.getClass();
		Method m[] = c.getMethods();
//		for(int i = 0; i < m.length; i++){
//			Log.e("c", m[i].toString());
//		}
		Class<?> a[] = new Class[1];
		a[0] = Integer.TYPE;
		String o = new String("");
		try{
			m[0] = c.getDeclaredMethod("getNetworkOperatorNameGemini", a);
			o = (String) m[0].invoke(tm, 1);
			Log.e("ccc net", o);
		}catch(Exception e){
			Log.e("ccc", e.toString());
		}

	}
	
	public String getNetworkOperatorName(int sim)
	{
		return tm.getNetworkOperatorName();
	}

	public int getNetworkType(int sim)
	{
		return tm.getNetworkType();
	}

	public String getNetworkOperator(int sim)
	{
		return tm.getNetworkOperator();
	}

	public CellLocation getCellLocation(int sim)
	{
		return tm.getCellLocation();
	}

	public void listen(PhoneStateListener phoneStateListener, int events, int sim) {
		tm.listen(phoneStateListener, events);
	}
	
}

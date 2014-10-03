package ru.konsoft.gsmtester;

import android.telephony.*;
import java.lang.reflect.*;

public class DuoTelephonyManager {
	private TelephonyManager mStdTelephonyManager;
	
	private Method[] mMethods = new Method[6];
	
	private static final int M_LISTEN = 0;
	private static final int M_NAME = 1;
	private static final int M_TYPE = 2;
	private static final int M_OPERATOR = 3;
	private static final int M_CELL = 4;
	private static final int M_DATASTATE = 5;
	private static final int M_TEST = 6;
	
	private boolean mDuoReady = false;
	
	public DuoTelephonyManager(TelephonyManager telephonyManager) {
		initDuoTelephonyManager(telephonyManager);
	}

	public DuoTelephonyManager(TelephonyManager telephonyManager, GSMinfo activiti) {
		initDuoTelephonyManager(telephonyManager);
		
		try{
			Class<? extends TelephonyManager> tl = mStdTelephonyManager.getClass();
			Class<?> params[], ret;
			Method[] methods = tl.getDeclaredMethods();
			StringBuilder sb = new StringBuilder();
			for(Method method: methods){
				params = method.getParameterTypes();
				ret = method.getReturnType();
				
				sb.append("Name: ").append(method.getName()).append("\n");
				for(Class<?> param: params) {
					sb.append("\tParam: ").append(param.getCanonicalName()).append("\n");
				}
				sb.append("\tReturn: ").append(ret.getCanonicalName()).append("\n");
			}
			activiti.debugScreen(sb.toString());
		}catch(Exception e){
			Debug.log(Debug.stack(e));
		}
	}
	
	private void initDuoTelephonyManager(TelephonyManager telephonyManager) {
		mStdTelephonyManager = telephonyManager;
		
		try{
			Class<? extends TelephonyManager> tl = mStdTelephonyManager.getClass();
			Class<?> args[];

			args = new Class[3];
			args[0] = PhoneStateListener.class;
			args[1] = Integer.TYPE;
			args[2] = Integer.TYPE;
			mMethods[M_LISTEN] = tl.getDeclaredMethod("listenGemini", args);

			args = new Class[1];
			args[0] = Integer.TYPE;
			mMethods[M_NAME] = tl.getDeclaredMethod("getNetworkOperatorNameGemini", args);
			mMethods[M_TYPE] = tl.getDeclaredMethod("getNetworkTypeGemini", args);
			mMethods[M_OPERATOR] = tl.getDeclaredMethod("getNetworkOperatorGemini", args);
			mMethods[M_CELL] = tl.getDeclaredMethod("getCellLocationGemini", args);
			mMethods[M_DATASTATE] = tl.getDeclaredMethod("getDataStateGemini", args);
			
			//m[M_TEST] = tl.getDeclaredMethod("getNetworkClass", null);
			
			mDuoReady = true;
		}catch(Exception e){
			Debug.log(Debug.stack(e));
		}
	}

	public String getNetworkOperatorName(int sim) {
		String name = "";
		
		try{
			name = (String) mMethods[M_NAME].invoke(mStdTelephonyManager, sim);
		}catch(Exception e){
			Debug.log(Debug.stack(e));
		}
		
		return name;
	}

	public int getNetworkType(int sim) {
		int type = 0;
		
		try{
			type = (Integer) mMethods[M_TYPE].invoke(mStdTelephonyManager, sim);
		}catch(Exception e){
			Debug.log(Debug.stack(e));
		}
		
		return type;
	}

	public String getNetworkOperator(int sim) {
		String opercode = "";
		
		try{
			opercode = (String) mMethods[M_OPERATOR].invoke(mStdTelephonyManager, sim);
		}catch(Exception e){
			Debug.log(Debug.stack(e));
		}
		
		if("".equals(opercode))
			opercode = "0";
			
		return opercode;
	}

	public CellLocation getCellLocation(int sim) {
		CellLocation cell = null;
		
		try{
			cell = (CellLocation) mMethods[M_CELL].invoke(mStdTelephonyManager, sim);
		}catch(Exception e){
			Debug.log(Debug.stack(e));
		}
		
		return cell;
	}

	public int getDataState(int sim) {
		int state = 0;

		try{
			state = (Integer) mMethods[M_DATASTATE].invoke(mStdTelephonyManager, sim);
		}catch(Exception e){
			Debug.log(Debug.stack(e));
		}

		return state;
	}
	
	public String getTest(int sim) {
		String name = "";

		try{
			name = (String) mMethods[M_TEST].invoke(mStdTelephonyManager);
		}catch(Exception e){
			Debug.log(Debug.stack(e));
		}

		if("".equals(name))
			name = "unknown";

		return name;
	}
	
	public void listen(PhoneStateListener phoneStateListener, int events, int sim) {
		try{
			mMethods[M_LISTEN].invoke(mStdTelephonyManager, phoneStateListener, events, sim);
		}catch(Exception e){
			Debug.log(Debug.stack(e));
		}
	}

	public boolean isDuoReady() {
		return mDuoReady;
	}

	public TelephonyManager getStdTelephonyManager() {
		return mStdTelephonyManager;
	}

}

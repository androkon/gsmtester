package ru.konsoft.gsmtester;

import android.util.Log;

public class Debug {
	
	private static final String LOG_TAG = "qwer";
	
	public static String stack(Exception e) {
		StackTraceElement[] stack = e.getStackTrace();
		StringBuilder sb = new StringBuilder();
		
		if(e.getMessage() != null)
			sb.append("Message: ").append(e.getMessage()).append("\n");
		
		sb.append("Stack: ");
		for(StackTraceElement se : stack){
			sb.append(se.toString()).append("\n");
		}
		
		return sb.toString();
	}
	
	public static void log(String msg) {
		Log.e(LOG_TAG, msg);
	}

}

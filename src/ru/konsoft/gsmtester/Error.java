package ru.konsoft.gsmtester;

import android.util.Log;
import java.io.*;

public class Error {
	public static String stack(Exception e) {
		StackTraceElement[] stack = e.getStackTrace();
		StringBuilder s = new StringBuilder();
		
		if(stack != null){
			for(int i = 0; i < stack.length; i++){
				s.append(stack[i].toString()).append("\n");
			}
		}
		
		return s.toString();
	}
	
	public static void log(String msg) {
		Log.e("qwer", msg);
	}
}

package ru.konsoft.gsmtester;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GSMinfo extends Activity {

	private static final int EXCELLENT_LEVEL = 75;
	private static final int GOOD_LEVEL = 50;
	private static final int MODERATE_LEVEL = 25;
	private static final int WEAK_LEVEL = 0;
	
	private int lastSignalLevel = 0;
	private Map<Long, Integer> levels;
	private Timer timer;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gsminfo);
        
        try{
	        startSignalLevelListener();
        }catch(Exception e){
        	setTextViewText(R.id.device_info, e.toString());
        }
	
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		stopListening();
		super.onDestroy();
	}
	
	private void setTextViewText(int id, String text) {
		((TextView) findViewById(id)).setText(text);
	}
	
	private void setSignalLevel(int id, int infoid, int level) {
		if(level == 99)
			level = 0;
		int progress = (int) ((((float) level) / 31.0) * 100);
		String signalLevelString = getSignalLevelString(progress);
		((ProgressBar) findViewById(id)).setProgress(progress);
		setTextViewText(infoid, signalLevelString + " (" + level + ")");
		lastSignalLevel = level;
	}
	
	private String getSignalLevelString(int level) {
		String signalLevelString = "Absent";
		if (level > EXCELLENT_LEVEL)
			signalLevelString = "Excellent";
		else if (level > GOOD_LEVEL)
			signalLevelString = "Good";
		else if (level > MODERATE_LEVEL)
			signalLevelString = "Moderate";
		else if (level > WEAK_LEVEL)
			signalLevelString = "Weak";
		return signalLevelString;
	}
	
	private void stopListening() {
		
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		
		timer.cancel();
	}
	
	private Handler timeHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			levels.put(System.currentTimeMillis(), lastSignalLevel);
			super.handleMessage(msg);
		}
	};	
	
	private void startSignalLevelListener() {
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		int events = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		tm.listen(phoneStateListener, events);
		
		timer = new Timer();
		long delay = 1000, interval = 1000;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				timeHandler.obtainMessage().sendToTarget();
			}
		}, delay, interval);
	}
	
	private void displayTelephonyInfo() {
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		String simoperator = tm.getSimOperatorName();
		String networktype = getNetworkTypeString(tm.getNetworkType());
		String phonetype = getPhoneTypeString(tm.getPhoneType());
		String deviceinfo = "";
		deviceinfo += ("SIM Operator: " + simoperator + "\n");
		deviceinfo += ("Network Type: " + networktype + "\n");
		deviceinfo += ("Phone Type: " + phonetype + "\n");
		setTextViewText(R.id.device_info, deviceinfo);
	}

	private final PhoneStateListener phoneStateListener = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			setSignalLevel(R.id.signalLevel, R.id.signalLevelInfo, signalStrength.getGsmSignalStrength());
			displayTelephonyInfo();
			super.onSignalStrengthsChanged(signalStrength);
		}
	
	};
	
	private String getNetworkTypeString(int val){
		switch(val){
			case TelephonyManager.NETWORK_TYPE_1xRTT: return "1xRTT";
			case TelephonyManager.NETWORK_TYPE_CDMA: return "CDMA";
			case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE";
			case TelephonyManager.NETWORK_TYPE_EHRPD: return "eHRPD";
			case TelephonyManager.NETWORK_TYPE_EVDO_0: return "EVDO revision 0";
			case TelephonyManager.NETWORK_TYPE_EVDO_A: return "EVDO revision A";
			case TelephonyManager.NETWORK_TYPE_EVDO_B: return "EVDO revision B";
			case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS";
			case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA";
			case TelephonyManager.NETWORK_TYPE_HSPA: return "HSPA";
			case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPA+";
			case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA";
			case TelephonyManager.NETWORK_TYPE_IDEN: return "iDen";
			case TelephonyManager.NETWORK_TYPE_LTE: return "LTE";
			case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS";
			case TelephonyManager.NETWORK_TYPE_UNKNOWN: return "unknown";
			default: return "Unknown NetworkType: " + val;
		}
	}

	private String getPhoneTypeString(int val){
		switch(val){
			case TelephonyManager.PHONE_TYPE_CDMA: return "CDMA";
			case TelephonyManager.PHONE_TYPE_GSM: return "GSM";
			case TelephonyManager.PHONE_TYPE_NONE: return "No phone radio.";
			case TelephonyManager.PHONE_TYPE_SIP: return "Phone is via SIP.";
			default: return "Unknown PhoneType: " + val;
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gsminfo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_exit) {
        	super.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

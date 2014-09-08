package ru.konsoft.gsmtester;

import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.CellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class GSMinfo extends Activity {

	private DuoTelephonyManager tm;
	private static final int EXCELLENT_LEVEL = 75;
	private static final int GOOD_LEVEL = 50;
	private static final int MODERATE_LEVEL = 25;
	private static final int WEAK_LEVEL = 0;
	
	private Info[] lastInfo = Info[2];
	private static Info currInfo = new Info();
	private static String diff = "";
	private static String lastdeviceinfo = "";
	
	private Timer timer = new Timer();
	private final Handler timeHandler = new Handler();
	
	private LocationManager lm;
	
	private boolean draw = true;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gsminfo);
        
        try{
	        startAllListeners();
        }catch(Exception e){
        	setTextViewText(R.id.device_info, e.toString());
        }
	
	}
	
	@Override
	protected void onPause() {
		Log.e(getPackageName(), "pause");
		draw = false;
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.e(getPackageName(), "resume");
		draw = true;
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
	
	private static int setSignalLevel(int level) {
		if(level > 31)
			level = 0;
		int progress = (int) ((((float) level) / 31.0) * 100);
		return progress;
	}
	
	private void setInfo(){
		try{
			currInfo.operator = tm.getNetworkOperatorName(0);
			currInfo.nettype = tm.getNetworkType();
			currInfo.opercode = Integer.parseInt(tm.getNetworkOperator());
			GsmCellLocation cl = (GsmCellLocation) tm.getCellLocation();
			currInfo.cid = cl.getCid();
			currInfo.lac = cl.getLac();
		}catch(Exception e){
			setTextViewText(R.id.device_info, "some errors");
		}
	}
	
	private void saveLog(){
		if(! isInfoReady())
			return;
			
		if(currInfo.equals(lastInfo))
			return;
			
		diff = currInfo.getDiff(lastInfo);
		lastdeviceinfo = getGsmTextInfo(lastInfo);
		
		lastInfo = new Info(currInfo);
		lastInfo.time = System.currentTimeMillis();
		
		addToStorage();
	}
	
	private boolean isInfoReady(){
		return true;
		
		/*if(
			currInfo.lat != 0 &&
			currInfo.lon != 0 &&
			currInfo.accuracy < 50
		)
			return true;
		else
			return false;*/
	}
	
	private void addToStorage(){
		//Log.e(getPackageName(), "add to storage: " + getLastGsmTextInfo());
	}
	
	private static String getGsmTextInfo(Info currInfo){
		String deviceinfo = "";
		deviceinfo += ("SIM Operator: " + currInfo.operator + "\n");
		deviceinfo += ("Oper code: " + currInfo.opercode + "\n");
		deviceinfo += ("Network Type: " + getNetworkTypeString(currInfo.nettype) + "\n");
		deviceinfo += ("Cell ID: " + currInfo.cid + "\n");
		deviceinfo += ("Cell LAC: " + currInfo.lac + "\n");
		deviceinfo += ("Level: " + currInfo.level + "\n");
		return deviceinfo;
	}
	
	private void displayInfo(){
		if(! draw)
			return;
			
		String signalLevelString = getSignalLevelString(currInfo.level);
		((ProgressBar) findViewById(R.id.signalLevel)).setProgress(currInfo.level);
		setTextViewText(R.id.signalLevelInfo, signalLevelString + " (" + currInfo.level + ")");

		setTextViewText(R.id.device_info, getGsmTextInfo(currInfo) + "\n" + 
			"\nDiff: " + diff + "\n\nLast: \n" + lastdeviceinfo);
		
		if(currInfo.lat != 0 && currInfo.lon != 0){
			String gpsinfo = "";
			gpsinfo += "lat: " + currInfo.lat + "\n";
			gpsinfo += "lon: " + currInfo.lon + "\n";
			gpsinfo += "acc: " + currInfo.accuracy + " m";
			setTextViewText(R.id.gps_info, gpsinfo);
		}else{
			setTextViewText(R.id.gps_info, "No GPS");
		}
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
		
		tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		
		timer.cancel();
	}
		
	private void startAllListeners() {
		// get GSM mobile network settings
		tm = (DuoTelephonyManager) getSystemService(TELEPHONY_SERVICE);
		int events = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		tm.listen(phoneStateListener, events);
		setSignalLevel(99);
		
		Class c = tm.getClass();
		Method m[] = c.getDeclaredMethods();
		for(int i = 0; i < m.length; i++){
			Log.e("c", m[i].toString());
		}
		Class a[] = new Class[1];
		a[0] = Integer.TYPE;
		String o = new String("");
		try{
			m[0] = c.getDeclaredMethod("getNetworkOperatorNameGemini", a);
			o = (String) m[0].invoke(tm, 1);
			Log.e("ccc net", o);
		}catch(NoSuchMethodException e){
			Log.e("cccccc", e.toString());
		}catch(Exception e){
			Log.e("ccc", e.toString());
		}

		//finish();
		
		// get GPS coordinates
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setCostAllowed(false);
		String provider = lm.getBestProvider(criteria, false);
		// location updates: at least 1 sec and 2 meter change
		lm.requestLocationUpdates(provider, 1000, 0, gpsLocationListener);

		// create collection data timer
		long delay = 1000, interval = 1000;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				timeHandler.post(new Runnable() {
					@Override
					public void run() {
						setInfo();
						saveLog();
						displayInfo();
					}
				});
			}
		}, delay, interval);
	}
	
	private final PhoneStateListener phoneStateListener = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			
			currInfo.level = setSignalLevel(signalStrength.getGsmSignalStrength());
			
			super.onSignalStrengthsChanged(signalStrength);
		}
	
	};
	
	private static String getNetworkTypeString(int val){
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

	private final LocationListener gpsLocationListener = new LocationListener() {
		
		@Override
		public void onLocationChanged(Location location) {
			currInfo.lat = location.getLatitude();
			currInfo.lon = location.getLongitude();
			currInfo.accuracy = location.getAccuracy();
			//Log.e(getPackageName(), "change lat: " + Location.convert(lastLocation.getLatitude(), Location.FORMAT_SECONDS));
		}

		@Override
		public void onProviderEnabled(String provider) {
			//Log.e(getPackageName(), "gps: onProviderEnabled");
		}

		@Override
		public void onProviderDisabled(String provider) {
			currInfo.lat = currInfo.lon = currInfo.accuracy = 0;
			//Log.e(getPackageName(), "gps: onProviderDisabled");
		}
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			//Log.e(getPackageName(), "gps: onStatusChanged");
		}

	};

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

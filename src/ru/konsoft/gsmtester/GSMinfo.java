package ru.konsoft.gsmtester;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
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
	
	private static int SIM_CNT = 2;
	private Info[] lastInfo = new Info[SIM_CNT];
	private static Info[] currInfo = new Info[SIM_CNT];
	private static String[] diff = new String[SIM_CNT];
	private static String[] lastdeviceinfo = new String[SIM_CNT];
	
	private Timer timer = new Timer();
	private final Handler timeHandler = new Handler();
	
	private LocationManager lm;
	private Location loc = new Location("gps");
	
	private boolean draw = true;
	
	private void initDuo(){
		for(int i = 0; i < SIM_CNT; i++){
			lastInfo[i] = new Info();
	        currInfo[i] = new Info();
	        diff[i] = new String();
	        lastdeviceinfo[i] = new String();
		}
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gsminfo);
        
        initDuo();
        
        try{
	        startAllListeners();
        }catch(Exception e){
        	setTextViewText(R.id.device_info0, e.toString());
        }
	
	}
	
	@Override
	protected void onPause() {
		//Log.e(getPackageName(), "pause");
		draw = false;
		super.onPause();
	}

	@Override
	protected void onResume() {
		//Log.e(getPackageName(), "resume");
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
		for(int i = 0; i < SIM_CNT; i++){
			Info info = currInfo[i];
			
			try{
				info.operator = tm.getNetworkOperatorName(i);
				info.nettype = tm.getNetworkType(i);
				info.opercode = Integer.valueOf(tm.getNetworkOperator(i));
				GsmCellLocation cl = (GsmCellLocation) tm.getCellLocation(i);
				info.cid = cl.getCid();
				info.lac = cl.getLac();
			}catch(Exception e){
				int rid;
				if(i == 0)
					rid = R.id.device_info0;
				else
					rid = R.id.device_info1; 
				setTextViewText(rid, "some errors");
			}
		}
	}
	
	private void saveLog(){
		if(! isInfoReady(loc))
			return;
			
		for(int i = 0; i < SIM_CNT; i++){
			if(currInfo[i].equals(lastInfo[i]))
				return;
				
			diff[i] = currInfo[i].getDiff(lastInfo[i]);
			lastdeviceinfo[i] = getGsmTextInfo(lastInfo[i]);
			
			lastInfo[i] = new Info(currInfo[i]);
			lastInfo[i].time = System.currentTimeMillis();
			
			addToStorage(lastInfo[i]);
		}
	}
	
	private static boolean isInfoReady(Location location){
		return true;
		
/*		if(
			location.getLatitude() != 0 &&
			location.getLongitude() != 0 &&
			location.getAccuracy() < 50
		)
			return true;
		else
			return false;
*/	}
	
	private void addToStorage(Info info){
		//Log.e(getPackageName(), "add to storage: " + getGsmTextInfo(info));
	}
	
	private static String getGsmTextInfo(Info info){
		String deviceinfo = "";
		deviceinfo += ("SIM Operator: " + info.operator + "\n");
		deviceinfo += ("Oper code: " + info.opercode + "\n");
		deviceinfo += ("Network Type: " + getNetworkTypeString(info.nettype) + "\n");
		deviceinfo += ("Cell ID: " + info.cid + "\n");
		deviceinfo += ("Cell LAC: " + info.lac + "\n");
		deviceinfo += ("Level: " + info.level + "\n");
		return deviceinfo;
	}
	
	private void displayInfo(){
		if(! draw)
			return;
		
		for(int i = 0; i < SIM_CNT; i++){
			Info info = currInfo[i];
			int rid;
			
			if(i == 0)
				rid = R.id.signalLevel0;
			else
				rid = R.id.signalLevel1; 
			String signalLevelString = getSignalLevelString(info.level);
			((ProgressBar) findViewById(rid)).setProgress(info.level);

			if(i == 0)
				rid = R.id.signalLevelInfo0;
			else
				rid = R.id.signalLevelInfo1; 
			setTextViewText(rid, signalLevelString + " (" + info.level + ")");
	
			if(i == 0)
				rid = R.id.device_info0;
			else
				rid = R.id.device_info1; 
			setTextViewText(rid, getGsmTextInfo(info) + "\n" + 
				"\nDiff: " + diff[i] + "\n\nLast: \n" + lastdeviceinfo[i] + "\n"
			);
		}
	}
	
	private void displayGpsInfo(){
		if(! draw)
			return;
		
		if(loc.getLatitude() != 0 && loc.getLongitude() != 0){
			String gpsinfo = "";
			gpsinfo += "lat: " + loc.getLatitude() + "\n";
			gpsinfo += "lon: " + loc.getLongitude() + "\n";
			gpsinfo += "acc: " + loc.getAccuracy() + " m";
			setTextViewText(R.id.gps_info, gpsinfo);
		}else{
			setTextViewText(R.id.gps_info, "No GPS");
		}
	}
	
	private static String getSignalLevelString(int level) {
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
		
		tm.listen(phoneStateListener0, PhoneStateListener.LISTEN_NONE, 0);
		tm.listen(phoneStateListener1, PhoneStateListener.LISTEN_NONE, 1);
		
		timer.cancel();
	}
		
	private void startAllListeners() {
		// get GSM mobile network settings
		tm = new DuoTelephonyManager((TelephonyManager) getSystemService(TELEPHONY_SERVICE));
		if(! tm.duoReady){
			Log.e(getPackageName(), "Supports only duo SIM");
			finish();
		}
		int events = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		tm.listen(phoneStateListener0, events, 0);
		tm.listen(phoneStateListener1, events, 1);
		
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
						displayGpsInfo();
					}
				});
			}
		}, delay, interval);
	}
	
	private final PhoneStateListener phoneStateListener0 = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			currInfo[0].level = setSignalLevel(signalStrength.getGsmSignalStrength());
			super.onSignalStrengthsChanged(signalStrength);
		}
	
	};
	
	private final PhoneStateListener phoneStateListener1 = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			currInfo[1].level = setSignalLevel(signalStrength.getGsmSignalStrength());
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
			setLocation(location);
			//Log.e(getPackageName(), "change lat: " + Location.convert(lastLocation.getLatitude(), Location.FORMAT_SECONDS));
		}
		
		private void setLocation(Location location){
			loc = location;
		}

		@Override
		public void onProviderEnabled(String provider) {
			//Log.e(getPackageName(), "gps: onProviderEnabled");
		}

		@Override
		public void onProviderDisabled(String provider) {
			Location noLoc = new Location("gps");
			noLoc.setLatitude(0);
			noLoc.setLongitude(0);
			noLoc.setAccuracy(0);
			setLocation(noLoc);
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

package ru.konsoft.gsmtester;

import java.util.Timer;
import java.util.TimerTask;
import java.util.List;

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

	private TelephonyManager tm;
	private static final int EXCELLENT_LEVEL = 75;
	private static final int GOOD_LEVEL = 50;
	private static final int MODERATE_LEVEL = 25;
	private static final int WEAK_LEVEL = 0;
	
	private static Info lastInfo = new Info();
	private static Info currInfo = new Info();
	
	private Timer timer = new Timer();
	private final Handler timeHandler = new Handler();
	
	private LocationManager lm;


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
	
	private void setSignalLevel(int level) {
		if(level > 31)
			level = 0;
		int progress = (int) ((((float) level) / 31.0) * 100);
		currInfo.level = progress;
	}
	
	private void displayInfo(){
		String signalLevelString = getSignalLevelString(currInfo.level);
		((ProgressBar) findViewById(R.id.signalLevel)).setProgress(currInfo.level);
		setTextViewText(R.id.signalLevelInfo, signalLevelString + " (" + currInfo.level + ")");
		
		try{
			String simoperator = tm.getSimOperatorName();
			String networktype = getNetworkTypeString(tm.getNetworkType());
			String opercode = tm.getNetworkOperator();
			GsmCellLocation cl = (GsmCellLocation) tm.getCellLocation();
			int cid = cl.getCid();
			int lac = cl.getLac();
			String deviceinfo = "";
			deviceinfo += ("SIM Operator: " + simoperator + "\n");
			deviceinfo += ("Oper code: " + opercode + "\n");
			deviceinfo += ("Network Type: " + networktype + "\n");
			deviceinfo += ("Cell ID: " + cid + "\n");
			deviceinfo += ("Cell LAC: " + lac + "\n");
			setTextViewText(R.id.device_info, deviceinfo);
        }catch(Exception e){
        	setTextViewText(R.id.device_info, "Some errors");
        	e.printStackTrace();
        }
		
		if(currInfo.lat != 0 && currInfo.lon != 0){
			String gpsinfo = "";
			gpsinfo += "lat: " + currInfo.lat + "\n";
			gpsinfo += "lon: " + currInfo.lon;
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
	
//	private static class CollectTimeHandler extends Handler {
//		@Override
//		public void handleMessage(Message msg) {
//			//displayInfo((GSMinfo)msg.obj);
//			//Data d = new  Data(lastSignalLevel);
////			if(lastLocation == null)
////				Log.e(getPackageName(), "last lat: null");
////			else
////				Log.e(getPackageName(), "last lat: " + Location.convert(lastLocation.getLatitude(), Location.FORMAT_SECONDS) + " accuracy: " + lm.getProvider(provider).getAccuracy());
//			super.handleMessage(msg);
//		}
//	}
		
	private void startAllListeners() {
		// get GSM mobile network settings
		tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		int events = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		tm.listen(phoneStateListener, events);
		setSignalLevel(99);
		
		// get GPS coordinates
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setCostAllowed(false);
		String provider = lm.getBestProvider(criteria, false);
		//Log.e(getPackageName(), "prov: " + provider);
		//Log.e(getPackageName(), "lat: " + Location.convert(lastLocation.getLatitude(), Location.FORMAT_SECONDS));
		// location updates: at least 1 sec and 1 meter change
		lm.requestLocationUpdates(provider, 1000, 1, gpsLocationListener);

		// create collection data timer
		long delay = 1000, interval = 1000;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				//Log.e(getPackageName(), "timertask");
				//timeHandler.obtainMessage().sendToTarget();
				timeHandler.post(new Runnable() {
					@Override
					public void run() {
						
						displayInfo();
					}
				}
				);
			}
		}, delay, interval);
	}
	
	private final PhoneStateListener phoneStateListener = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			
			setSignalLevel(signalStrength.getGsmSignalStrength());
			//Log.e(getPackageName(), "gsm: " + signalStrength.getGsmSignalStrength());
			
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

	private final LocationListener gpsLocationListener = new LocationListener() {
		
		@Override
		public void onLocationChanged(Location location) {
			currInfo.lat = location.getLatitude();
			currInfo.lon = location.getLongitude();
			//Log.e(getPackageName(), "change lat: " + Location.convert(lastLocation.getLatitude(), Location.FORMAT_SECONDS));
		}

		@Override
		public void onProviderEnabled(String provider) {
			//Log.e(getPackageName(), "gps: onProviderEnabled");
		}

		@Override
		public void onProviderDisabled(String provider) {
			//Log.e(getPackageName(), "gps: onProviderDisabled");
			//lastLocation = null;
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

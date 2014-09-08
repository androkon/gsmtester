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
	
	private Info[] lastInfo = new Info[2];
	private static Info[] currInfo = new Info[2];
	private static String[] diff = new String[2];
	private static String[] lastdeviceinfo = new String[2];
	
	private Timer timer = new Timer();
	private final Handler timeHandler = new Handler();
	
	private LocationManager lm;
	
	private boolean draw = true;
	
	private void initDuo(int sim){
		lastInfo[sim] = new Info();
        currInfo[sim] = new Info();
        diff[sim] = new String();
        lastdeviceinfo[sim] = new String();
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gsminfo);
        
        initDuo(0);
        initDuo(1);
        
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
	
	private void setInfo(int sim){
		try{
			currInfo[sim].operator = tm.getNetworkOperatorName(0);
			currInfo[sim].nettype = tm.getNetworkType(0);
			currInfo[sim].opercode = Integer.parseInt(tm.getNetworkOperator(0));
			GsmCellLocation cl = (GsmCellLocation) tm.getCellLocation(0);
			currInfo[sim].cid = cl.getCid();
			currInfo[sim].lac = cl.getLac();
		}catch(Exception e){
			setTextViewText(R.id.device_info, "some errors");
		}
	}
	
	private void saveLog(int sim){
		if(! isInfoReady(currInfo[sim]))
			return;
			
		if(currInfo[sim].equals(lastInfo[sim]))
			return;
			
		diff[sim] = currInfo[sim].getDiff(lastInfo[sim]);
		lastdeviceinfo[sim] = getGsmTextInfo(lastInfo[sim]);
		
		lastInfo[sim] = new Info(currInfo[sim]);
		lastInfo[sim].time = System.currentTimeMillis();
		
		addToStorage(lastInfo[sim]);
	}
	
	private static boolean isInfoReady(Info currInfo){
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
	
	private void addToStorage(Info simInfo){
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
	
	private void displayInfo(int sim){
		if(! draw)
			return;
		
		Info info = currInfo[sim];
		
		String signalLevelString = getSignalLevelString(info.level);
		((ProgressBar) findViewById(R.id.signalLevel)).setProgress(info.level);
		setTextViewText(R.id.signalLevelInfo, signalLevelString + " (" + info.level + ")");

		setTextViewText(R.id.device_info, getGsmTextInfo(info) + "\n" + 
			"\nDiff: " + diff[sim] + "\n\nLast: \n" + lastdeviceinfo[sim]);
		
		if(info.lat != 0 && info.lon != 0){
			String gpsinfo = "";
			gpsinfo += "lat: " + info.lat + "\n";
			gpsinfo += "lon: " + info.lon + "\n";
			gpsinfo += "acc: " + info.accuracy + " m";
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
		
		tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE, 0);
		tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE, 1);
		
		timer.cancel();
	}
		
	private void startAllListeners() {
		// get GSM mobile network settings
		tm = new DuoTelephonyManager((TelephonyManager) getSystemService(TELEPHONY_SERVICE));
		//finish();
		int events = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		tm.listen(phoneStateListener, events, 0);
		tm.listen(phoneStateListener, events, 1);
		
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
						setInfo(0);
						setInfo(1);
						
						saveLog(0);
						saveLog(1);
						
						displayInfo(0);
						displayInfo(1);
					}
				});
			}
		}, delay, interval);
	}
	
	private final PhoneStateListener phoneStateListener = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			
			currInfo[0].level = setSignalLevel(signalStrength.getGsmSignalStrength());
			
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
			setLocation(0, location);
			setLocation(1, location);
			//Log.e(getPackageName(), "change lat: " + Location.convert(lastLocation.getLatitude(), Location.FORMAT_SECONDS));
		}
		
		private void setLocation(int sim, Location location){
			currInfo[sim].lat = location.getLatitude();
			currInfo[sim].lon = location.getLongitude();
			currInfo[sim].accuracy = location.getAccuracy();
		}

		@Override
		public void onProviderEnabled(String provider) {
			//Log.e(getPackageName(), "gps: onProviderEnabled");
		}

		@Override
		public void onProviderDisabled(String provider) {
			Location l = new Location("gps");
			l.setLatitude(0);
			l.setLongitude(0);
			l.setAccuracy(0);
			setLocation(0, l);
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

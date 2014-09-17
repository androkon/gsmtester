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
import android.content.ContextWrapper;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import android.os.Environment;

public class GSMinfo extends Activity {

	private static DuoTelephonyManager tm;
	private static final int EXCELLENT_LEVEL = 75;
	private static final int GOOD_LEVEL = 50;
	private static final int MODERATE_LEVEL = 25;
	private static final int WEAK_LEVEL = 0;
	
	private static final int SIM_CNT = 2;
	private static Info[] lastInfo = new Info[SIM_CNT];
	private static Info[] currInfo = new Info[SIM_CNT];
	//private static String[] diff = new String[SIM_CNT];
	//private static String[] lastdeviceinfo = new String[SIM_CNT];
	
	private static BufferedWriter buf;
	private static int bufLen = 111;
	
	private Timer timer = new Timer();
	private static Handler timeHandler = new Handler();
	private static long timerDelay = 1000; // millisec
	private static long timerInterval = 1000; // millisec
	
	private static LocationManager lm;
	private static Location loc = new Location("gps");
	private static float dist = 0; // meter
	private static int gpsUpdateInterval = 1000; // millisec
	private static int gpsUpdateDist = 2; // meter
	
	private static boolean draw = true;
	
	private void initDuo() {
		for(int i = 0; i < SIM_CNT; i++){
			lastInfo[i] = new Info();
	        currInfo[i] = new Info();
	        //diff[i] = new String();
	        //lastdeviceinfo[i] = new String();
		}
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gsminfo);
        
		//Error.log("create");
		
        initDuo();
        
        try{
	        startAllListeners();
        }catch(Exception e){
        	setTextViewText(R.id.error_info, "create " + Error.stack(e));
        }
	
	}
	
	@Override
	protected void onPause() {
		//Error.log("pause");
		draw = false;
		super.onPause();
	}

	@Override
	protected void onResume() {
		//Error.log("resume");
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
	
	private String getTextView(int id) {
		return getString(id);
	}
	
	private static int setSignalLevel(int level) {
		if(level > 31)
			level = 0;
		int progress = (int) ((((float) level) / 31.0) * 100);
		
		return progress;
	}
	
	private void setInfo() {
		for(int i = 0; i < SIM_CNT; i++){
			Info info = currInfo[i];
			
			try{
				info.opercode = Integer.valueOf(tm.getNetworkOperator(i));
				if(info.opercode == 0){
					info.operator = "no name";
					info.nettype = 0;
					info.cid = 0;
					info.lac = 0;
					info.level = 0;
				}else{
					info.operator = tm.getNetworkOperatorName(i);
					info.nettype = tm.getNetworkType(i);		
					GsmCellLocation cl = (GsmCellLocation) tm.getCellLocation(i);
					info.cid = cl.getCid();
					info.lac = cl.getLac();
				}
			}catch(Exception e){
				setTextViewText(R.id.error_info, "error sim " + i + " " + Error.stack(e));
			}
		}
	}
	
	private void saveLog() {
		if(! isGpsReady())
			return;
		
		for(int i = 0; i < SIM_CNT; i++){
			Info last, curr;
			
			last = lastInfo[i];
			curr = currInfo[i];
			//lastInfo[i] = new Info(currInfo[i]);
			last.operator = curr.operator;
			last.opercode = curr.opercode;
			last.nettype = curr.nettype;
			last.level = curr.level;
			last.cid = curr.cid;
			last.lac = curr.lac;
			
			if(last.opercode != 0){
				last.time = System.currentTimeMillis();
				addToStorage(last);
			}
		}
	}
	
	private boolean isGpsReady() {
		if(
			loc.getLatitude() != 0 &&
			loc.getLongitude() != 0 &&
			loc.getAccuracy() < 50
			//|| true
		)
			return true;
		else
			return false;
	}
	
	private void addToStorage(Info info) {
		try{
			//Error.log("write");
			buf.write(getLog(loc, info));
			buf.flush();
		}catch(Exception e){
			setTextViewText(R.id.error_info, "write " + Error.stack(e));
		}
	}
	
	private static String getGsmTextInfo(Info info) {
		StringBuilder s = new StringBuilder();
		String sep = "\n";
		
		// s.append("SIM Operator: ").append(info.operator).append(sep);
		// s.append("Oper code: ").append(String.valueOf(info.opercode)).append(sep);
		s.append("Network Type: ").append(getNetworkTypeString(info.nettype)).append(sep);
		// s.append("Cell ID: ").append(String.valueOf(info.cid)).append(sep);
		// s.append("Cell LAC: ").append(String.valueOf(info.lac)).append(sep);
		s.append("Level: ").append(String.valueOf(info.level));
		
		return s.toString();
	}
	
	private static String getLog(Location loc, Info info) {
		StringBuilder s = new StringBuilder();
		String sep = ",";
		String eol = "\n";

		s.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(info.time))).append(sep);
		s.append(String.valueOf(info.time)).append(sep);
		s.append(String.valueOf(loc.getLatitude())).append(sep);
		s.append(String.valueOf(loc.getLongitude())).append(sep);
		s.append(String.valueOf(loc.getAccuracy())).append(sep);
		s.append(info.operator).append(sep);
		s.append(String.valueOf(info.opercode)).append(sep);
		s.append(String.valueOf(info.nettype)).append(sep);
		s.append(String.valueOf(info.level)).append(sep);
		s.append(String.valueOf(info.cid)).append(sep);
		s.append(String.valueOf(info.lac)).append(eol);
			
		return s.toString();
	}
	
	private void displayInfo() {
		if(! draw)
			return;
		
		for(int i = 0; i < SIM_CNT; i++){
			Info info = currInfo[i];
			int signalLevel, signalLevelInfo, deviceInfo, simNameId;
			String simName;
			
			if(i == 0){
				signalLevel = R.id.signalLevel0;
				// signalLevelInfo = R.id.signalLevelInfo0;
				deviceInfo = R.id.device_info0;
				simNameId = R.id.sim_name0;
				simName = getTextView(R.string.text_sim_name0);
			}else{
				signalLevel = R.id.signalLevel1;
				// signalLevelInfo = R.id.signalLevelInfo1;
				deviceInfo = R.id.device_info1;
				simNameId = R.id.sim_name1;
				simName = getTextView(R.string.text_sim_name1);
			}
			
			StringBuilder s = new StringBuilder();
			s.append(simName).append(": ").append(info.operator);
			setTextViewText(simNameId, s.toString());
			// String signalLevelString = getSignalLevelString(info.level);
			((ProgressBar) findViewById(signalLevel)).setProgress(info.level);
			// setTextViewText(signalLevelInfo, signalLevelString);
			setTextViewText(deviceInfo, getGsmTextInfo(info));
		}
	}
	
	private void displayGpsInfo() {
		if(! draw)
			return;
		
		if(loc.getLatitude() != 0 && loc.getLongitude() != 0){
			StringBuilder gpsinfo = new StringBuilder();
			
			gpsinfo.append("lat: ").append(String.valueOf(loc.getLatitude())).append("\n");
			gpsinfo.append("lon: ").append(String.valueOf(loc.getLongitude())).append("\n");
			gpsinfo.append("acc: ").append(String.valueOf(loc.getAccuracy())).append(" m\n");
			gpsinfo.append("vel: ").append(String.valueOf(loc.getSpeed())).append(" m/s\n");
			gpsinfo.append("dist: ").append(dist).append(" m");
			setTextViewText(R.id.gps_info, gpsinfo.toString());
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
		
		try{
			buf.flush();
			buf.close();
		}catch(Exception e){
			Error.log(Error.stack(e));
		}
	}
	
	private void debug(String text) {
		setTextViewText(R.id.error_info, text);
	}
		
	private void startAllListeners() {
		//Error.log("start");
		// create new dayli log file with append
		File dir = Environment.getDataDirectory();
		//File dir = Context.getExternalFilesDir(null);
		debug(dir.getAbsolutePath());
		String fname = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis())) + ".log";
		try{
			buf = new BufferedWriter(new FileWriter(new File(dir, fname), true), bufLen);
		}catch(Exception e){
			//setTextViewText(R.id.error_info, "file " + Error.stack(e));
		}
		
		// get GSM mobile network settings
		tm = new DuoTelephonyManager((TelephonyManager) getSystemService(TELEPHONY_SERVICE));
		if(! tm.duoReady){
			setTextViewText(R.id.error_info, "Supports only duo SIM");
			return;
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
		lm.requestLocationUpdates(provider, gpsUpdateInterval, gpsUpdateDist, gpsLocationListener);

		// create collection data timer
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				timeHandler.post(new Runnable() {
					@Override
					synchronized
					public void run() {
						//Error.log("timer");
						setInfo();
						saveLog();
						displayInfo();
						displayGpsInfo();
					}
				});
			}
		}, timerDelay, timerInterval);
		
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
		}
		
		private void setLocation(Location location){
			float res[] = new float[1];
			
			Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), 
				location.getLatitude(), location.getLongitude(), res);
			dist = res[0];
			loc = location;
		}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {
			Location noLoc = new Location("gps");
			
			noLoc.setLatitude(0);
			noLoc.setLongitude(0);
			noLoc.setAccuracy(0);
			setLocation(noLoc);
		}
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Error.log("gps status");
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
        	finish();
            return true;
        }
		
        return super.onOptionsItemSelected(item);
    }

}

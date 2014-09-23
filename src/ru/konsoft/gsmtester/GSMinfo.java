package ru.konsoft.gsmtester;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileWriter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.os.*;

public class GSMinfo extends Activity {

	private static DuoTelephonyManager tm;
	
	private static final int SIM_CNT = 2;
	private static Info[] lastInfo = new Info[SIM_CNT];
	private static Info[] currInfo = new Info[SIM_CNT];
	
	private Timer timer = new Timer();
	private static Handler timeHandler = new Handler();
	private static long timerDelay = 1000; // millisec
	private static long timerInterval = 1000; // millisec
	
	private static LocationManager lm;
	private static Location loc = new Location("gps");
	private static float dist = 0; // meter
	private static int gpsUpdateInterval = 1000; // millisec
	private static int gpsUpdateDist = 2; // meter
	private boolean emulateGps;
	
	private static boolean draw = true;
	
	private void initDuo() {
		for(int i = 0; i < SIM_CNT; i++){
			lastInfo[i] = new Info(i);
	        currInfo[i] = new Info(i);
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
        	debug("create " + Debug.stack(e));
        }
	
	}
	
	@Override
	protected void onPause() {
		draw = false;
		super.onPause();
	}

	@Override
	protected void onResume() {
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
	
	private String getStringResource(int id) {
		return getString(id);
	}
	
	private static int getSignalLevelProgress(int level) {
		if(level > 31)
			level = 0;
		int progress = (int) ((((float) level) / 31.0) * 100);
		
		return progress;
	}
	
	private void setInfo() {
		long time = System.currentTimeMillis();
		
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
					info.progress = 0;
					info.datastate = 0;
				}else{
					info.progress = getSignalLevelProgress(info.level);
					info.operator = tm.getNetworkOperatorName(i);
					info.nettype = tm.getNetworkType(i);		
					GsmCellLocation cl = (GsmCellLocation) tm.getCellLocation(i);
					info.cid = cl.getCid();
					info.lac = cl.getLac();
					info.datastate = tm.getDataState(i);
				}
			}catch(Exception e){
				debug("error sim " + i + " " + Debug.stack(e));
			}
			
			info.rx = TrafficStats.getMobileRxBytes();
			info.tx = TrafficStats.getMobileTxBytes();
			
			info.time = time;
		}
	}
	
	private void swapInfo() {
		for(int i = 0; i < SIM_CNT; i++){
			Info last, curr;
			
			last = lastInfo[i];
			curr = currInfo[i];

			last.operator = curr.operator;
			last.opercode = curr.opercode;
			last.nettype = curr.nettype;
			last.level = curr.level;
			last.progress = curr.progress;
			last.cid = curr.cid;
			last.lac = curr.lac;
			last.datastate = curr.datastate;
			
			last.srx = (int)((curr.rx - last.rx) * 1000.0 / (curr.time - last.time));
			last.stx = (int)((curr.tx - last.tx) * 1000.0 / (curr.time - last.time));
			
			last.rx = curr.rx;
			last.tx = curr.tx;
			
			last.time = curr.time;
		}
	}
	
	private void saveLog() {
		if(! isGpsReady())
			return;
			
		addToStorage(loc, lastInfo);
	}
	
	private boolean isGpsReady() {
		if(
			loc.getLatitude() != 0 &&
			loc.getLongitude() != 0 &&
			loc.getAccuracy() < 50
			|| emulateGps
		)
			return true;
		else
			return false;
	}
	
	private void addToStorage(Location location, Info[] data) {
		// create new daily log file with append
		File dir = getExternalFilesDir(null);
		FileWriter file;
		String fname = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(System.currentTimeMillis())) + ".log";

		try{
			file = new FileWriter(new File(dir, fname), true);

			for(int i = 0; i < SIM_CNT; i++){
				Info info = data[i];
				if(info.opercode != 0){
					String s = getLog(location, info);
					file.write(s);
				}
			}
			file.flush();
			file.close();
		}catch(Exception e){
			debug("write " + Debug.stack(e));
		}
	}
	
	private static String getGsmTextInfo(Info info) {
		StringBuilder sb = new StringBuilder();
		String sep = "\n";
		
		sb.append("Network Type: ").append(getNetworkTypeString(info.nettype)).append(sep);
		sb.append("Level: ").append(String.valueOf(info.level)).append(sep);
		sb.append("Progress: ").append(String.valueOf(info.progress)).append(sep);
		sb.append("Slot: ").append(String.valueOf(info.slot)).append(sep);
		sb.append("LAC: ").append(String.valueOf(info.lac)).append(sep);
		sb.append("CID: ").append(String.valueOf(info.cid)).append(sep);
		
		sb.append(sep);
		sb.append("DataState: ").append(String.valueOf(info.datastate)).append(sep);
		//sb.append("Test: ").append(tm.getTest(info.slot - 1)).append(sep);
		sb.append("Rx: ").append(String.valueOf(TrafficStats.getMobileRxBytes())).append(" byte").append(sep);
		sb.append("Tx: ").append(String.valueOf(TrafficStats.getMobileTxBytes())).append(" byte").append(sep);
		sb.append("Srx: ").append(String.valueOf(info.srx)).append(" byte/sec").append(sep);
		sb.append("Stx: ").append(String.valueOf(info.stx)).append(" byte/sec").append(sep);
		
		return sb.toString();
	}
	
	private static String getLog(Location loc, Info info) {
		StringBuilder sb = new StringBuilder();
		String sep = ",";
		String eol = "\n";

		sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date(info.time))).append(sep);
		sb.append(String.valueOf(info.time)).append(sep);
		sb.append(String.valueOf(loc.getLatitude())).append(sep);
		sb.append(String.valueOf(loc.getLongitude())).append(sep);
		sb.append(String.valueOf(loc.getAccuracy())).append(sep);
		sb.append(String.valueOf(info.slot)).append(sep);
		sb.append(info.operator).append(sep);
		sb.append(String.valueOf(info.opercode)).append(sep);
		sb.append(String.valueOf(info.nettype)).append(sep);
		sb.append(String.valueOf(info.level)).append(sep);
		sb.append(String.valueOf(info.progress)).append(sep);
		sb.append(String.valueOf(info.cid)).append(sep);
		sb.append(String.valueOf(info.lac)).append(sep);
		sb.append(String.valueOf(info.datastate)).append(sep);
		sb.append(String.valueOf(info.srx)).append(sep);
		sb.append(String.valueOf(info.stx)).append(eol);
			
		return sb.toString();
	}
	
	private void displayGsmInfo() {
		if(! draw)
			return;
		
		for(int i = 0; i < SIM_CNT; i++){
			Info info = lastInfo[i];
			int signalLevel, deviceInfo, simNameId;
			String simName;
			
			if(i == 0){
				signalLevel = R.id.signalLevel0;
				deviceInfo = R.id.device_info0;
				simNameId = R.id.sim_name0;
				simName = getStringResource(R.string.text_sim_name0);
			}else{
				signalLevel = R.id.signalLevel1;
				deviceInfo = R.id.device_info1;
				simNameId = R.id.sim_name1;
				simName = getStringResource(R.string.text_sim_name1);
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append(simName).append(": ").append(info.operator);

			setTextViewText(simNameId, sb.toString());
			((ProgressBar) findViewById(signalLevel)).setProgress(info.progress);
			setTextViewText(deviceInfo, getGsmTextInfo(info));
		}
	}
	
	private void displayGpsInfo() {
		if(! draw)
			return;
		
		if(loc.getLatitude() != 0 && loc.getLongitude() != 0){
			StringBuilder sb = new StringBuilder();
			
			sb.append("lat: ").append(String.valueOf(loc.getLatitude())).append("\n");
			sb.append("lon: ").append(String.valueOf(loc.getLongitude())).append("\n");
			sb.append("acc: ").append(String.valueOf(loc.getAccuracy())).append(" m\n");
			sb.append("vel: ").append(String.valueOf(loc.getSpeed())).append(" m/s\n");
			sb.append("dist: ").append(dist).append(" m");
			setTextViewText(R.id.gps_info, sb.toString());
		}else{
			setTextViewText(R.id.gps_info, "No GPS");
		}
	}
	
	private void stopListening() {
		tm.listen(phoneStateListener0, PhoneStateListener.LISTEN_NONE, 0);
		tm.listen(phoneStateListener1, PhoneStateListener.LISTEN_NONE, 1);
		
		timer.cancel();
	}
	
	public void debug(String text) {
		setTextViewText(R.id.error_info, text);
	}
		
	private void startAllListeners() {
		// get GSM mobile network settings
		tm = new DuoTelephonyManager((TelephonyManager) getSystemService(TELEPHONY_SERVICE), this);
		if(! tm.duoReady){
			//debug("Supports only duo SIM");
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
					//synchronized
					public void run() {
						setInfo();
						swapInfo();
						saveLog();
						displayGsmInfo();
						displayGpsInfo();
					}
				});
			}
		}, timerDelay, timerInterval);
		
	}
	
	private final PhoneStateListener phoneStateListener0 = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			currInfo[0].level = signalStrength.getGsmSignalStrength();
			super.onSignalStrengthsChanged(signalStrength);
		}
	
	};
	
	private final PhoneStateListener phoneStateListener1 = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			currInfo[1].level = signalStrength.getGsmSignalStrength();
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
		public void onStatusChanged(String provider, int status, Bundle extras) {}

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
    
	public void onToggleEmulate(View view) {
		// Is the toggle on?
		boolean on = ((ToggleButton) view).isChecked();
		
		if (on) {
			emulateGps = true;
		} else {
			emulateGps = false;
		}
	}
    
}

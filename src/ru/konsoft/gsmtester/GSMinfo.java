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

public class GSMinfo extends Activity {

	private DuoTelephonyManager mDuoTM;
	
	private static final int SIM_CNT = 2;
	private Info[] mLastInfo = new Info[SIM_CNT];
	private Info[] mCurrInfo = new Info[SIM_CNT];
	private Object mInfoLock = new Object();
	
	private Timer mTimer = new Timer();
	private Handler mTimerHandler = new Handler();
	private static final long TIMER_DELAY = 1000; // millisec
	private static final long TIMER_INTERVAL = 1000; // millisec
	
	private LocationManager mLM;
	private float mDist; // meter
	private static final int GPS_UPDATE_INTERVAL = 1000; // millisec
	private static final int GPS_UPDATE_DISTANCE = 2; // meter
	private boolean mEmulateGps;

	private boolean mDraw = true;
	
	private void initDuo() {
		for(int i = 0; i < SIM_CNT; i++){
			mLastInfo[i] = new Info(i);
	        mCurrInfo[i] = new Info(i);
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
        	debugScreen("create " + Debug.stack(e));
        }
	}
	
	@Override
	protected void onPause() {
		mDraw = false;
		super.onPause();
	}

	@Override
	protected void onResume() {
		mDraw = true;
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
	
	private void setGpsInfo(Location location){
		for(int i = 0; i < SIM_CNT; i++){
			mCurrInfo[i].setGps(location);
		}

		float res[] = new float[1];
		Location.distanceBetween(mLastInfo[0].getLat(), mLastInfo[0].getLon(), 
			mCurrInfo[0].getLat(), mCurrInfo[0].getLon(), res);
		mDist = res[0];
	}
	
	private void setGsmInfo() {
		long time = System.currentTimeMillis();
		
		for(int i = 0; i < SIM_CNT; i++){
			Info info = mCurrInfo[i];
			
			try{
				info.setOpercode(Integer.valueOf(mDuoTM.getNetworkOperator(i)));
				if(info.getOpercode() == 0){
					info.resetGsm();
				}else{
					info.setProgress(getSignalLevelProgress(info.getLevel()));
					info.setOperator(mDuoTM.getNetworkOperatorName(i));
					info.setNettype(mDuoTM.getNetworkType(i));		
					GsmCellLocation cl = (GsmCellLocation) mDuoTM.getCellLocation(i);
					info.setCID(cl.getCid());
					info.setLAC(cl.getLac());
					info.setDatastate(mDuoTM.getDataState(i));
				}
			}catch(Exception e){
				debugScreen("error sim " + i + " " + Debug.stack(e));
			}
			
			info.setRX(TrafficStats.getMobileRxBytes());
			info.setTX(TrafficStats.getMobileTxBytes());
			
			info.setTime(time);
		}
	}
	
	private void swapInfo() {
		for(int i = 0; i < SIM_CNT; i++){
			Info last, curr;
			
			last = mLastInfo[i];
			curr = mCurrInfo[i];

			last.setOperator(curr.getOperator());
			last.setOpercode(curr.getOpercode());
			last.setNettype(curr.getNettype());
			last.setLevel(curr.getLevel());
			last.setProgress(curr.getProgress());
			last.setCID(curr.getCID());
			last.setLAC(curr.getLAC());
			last.setDatastate(curr.getDatastate());
			
			last.setSpeedRX((int)((curr.getRX() - last.getRX()) * 1000.0 / (curr.getTime() - last.getTime())));
			last.setSpeedTX((int)((curr.getTX() - last.getTX()) * 1000.0 / (curr.getTime() - last.getTime())));
			
			last.setRX(curr.getRX());
			last.setTX(curr.getTX());
			
			last.setLat(curr.getLat());
			last.setLon(curr.getLon());
			last.setAcc(curr.getAcc());
			last.setSpeed(curr.getSpeed());

			last.setTime(curr.getTime());
		}
	}
	
	private void saveLog() {
		if(! isGpsReady())
			return;
			
		addToStorage();
	}
	
	private boolean isGpsReady() {
		if(
			mLastInfo[0].getLat() != 0.0 &&
			mLastInfo[0].getLon() != 0.0 &&
			mLastInfo[0].getAcc() < 50.0
			|| mEmulateGps
		)
			return true;
		else
			return false;
	}
	
	private void addToStorage() {
		// create new daily log file with append
		try{
			File dir = getExternalFilesDir(null);
			FileWriter file;
			String fname = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(System.currentTimeMillis())) + ".log";

			file = new FileWriter(new File(dir, fname), true);

			for(int i = 0; i < SIM_CNT; i++){
				Info info = mLastInfo[i];
				if(info.getOpercode() != 0){
					file.write(getLog(info));
				}
			}
			file.flush();
			file.close();
		}catch(Exception e){
			debugScreen("write " + Debug.stack(e));
		}
	}
	
	private static String getGsmTextInfo(Info info) {
		StringBuilder sb = new StringBuilder();
		String sep = "\n";
		
		sb.append("Network Type: ").append(getNetworkTypeString(info.getNettype())).append(sep);
		sb.append("Level: ").append(String.valueOf(info.getLevel())).append(sep);
		sb.append("Progress: ").append(String.valueOf(info.getProgress())).append(sep);
		sb.append("Slot: ").append(String.valueOf(info.getSlot())).append(sep);
		sb.append("LAC: ").append(String.valueOf(info.getLAC())).append(sep);
		sb.append("CID: ").append(String.valueOf(info.getCID())).append(sep);
		sb.append("DataState: ").append(String.valueOf(info.getDatastate())).append(sep);
		sb.append("Speed RX: ").append(String.valueOf(info.getSpeedRX())).append(" bytes/sec").append(sep);
		sb.append("Speed TX: ").append(String.valueOf(info.getSpeedTX())).append(" bytes/sec").append(sep);
		
		sb.append(sep);
		//sb.append("Test: ").append(tm.getTest(info.slot - 1)).append(sep);
		sb.append("RX: ").append(String.valueOf(TrafficStats.getMobileRxBytes())).append(" bytes").append(sep);
		sb.append("TX: ").append(String.valueOf(TrafficStats.getMobileTxBytes())).append(" bytes").append(sep);
		
		return sb.toString();
	}
	
	private static String getLog(Info info) {
		StringBuilder sb = new StringBuilder();
		String sep = ",";
		String eol = "\n";

		sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date(info.getTime()))).append(sep);
		sb.append(String.valueOf(info.getTime())).append(sep);
		sb.append(String.valueOf(info.getLat())).append(sep);
		sb.append(String.valueOf(info.getLon())).append(sep);
		sb.append(String.valueOf(info.getAcc())).append(sep);
		sb.append(String.valueOf(info.getSlot())).append(sep);
		sb.append(info.getOperator()).append(sep);
		sb.append(String.valueOf(info.getOpercode())).append(sep);
		sb.append(String.valueOf(info.getNettype())).append(sep);
		sb.append(String.valueOf(info.getLevel())).append(sep);
		sb.append(String.valueOf(info.getProgress())).append(sep);
		sb.append(String.valueOf(info.getCID())).append(sep);
		sb.append(String.valueOf(info.getLAC())).append(sep);
		sb.append(String.valueOf(info.getDatastate())).append(sep);
		sb.append(String.valueOf(info.getSpeedRX())).append(sep);
		sb.append(String.valueOf(info.getSpeedTX())).append(eol);
			
		return sb.toString();
	}
	
	private void displayGsmInfo() {
		if(! mDraw)
			return;
		
		for(int i = 0; i < SIM_CNT; i++){
			Info info = mLastInfo[i];
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
			sb.append(simName).append(": ").append(info.getOperator());

			setTextViewText(simNameId, sb.toString());
			((ProgressBar) findViewById(signalLevel)).setProgress(info.getProgress());
			setTextViewText(deviceInfo, getGsmTextInfo(info));
		}
	}
	
	private void displayGpsInfo() {
		if(! mDraw)
			return;
		
		Info info = mLastInfo[0];
		
		if(info.getLat() != 0.0 && info.getLon() != 0.0){
			StringBuilder sb = new StringBuilder();
			
			sb.append("lat: ").append(String.valueOf(info.getLat())).append("\n");
			sb.append("lon: ").append(String.valueOf(info.getLon())).append("\n");
			sb.append("acc: ").append(String.valueOf(info.getAcc())).append(" m\n");
			sb.append("vel: ").append(String.valueOf(info.getSpeed())).append(" m/s\n");
			sb.append("dist: ").append(mDist).append(" m");
			setTextViewText(R.id.gps_info, sb.toString());
		}else{
			setTextViewText(R.id.gps_info, "No GPS");
		}
	}
	
	private void stopListening() {
		mDuoTM.listen(mPhoneStateListener0, PhoneStateListener.LISTEN_NONE, 0);
		mDuoTM.listen(mPhoneStateListener1, PhoneStateListener.LISTEN_NONE, 1);
		
		mLM.removeUpdates(mGpsLocationListener);
		
		mTimer.cancel();
	}
	
	public void debugScreen(String text) {
		setTextViewText(R.id.error_info, text);
	}
		
	private void startAllListeners() {
		// get GSM mobile network settings
		mDuoTM = new DuoTelephonyManager((TelephonyManager) getSystemService(TELEPHONY_SERVICE));
		if(! mDuoTM.isDuoReady()){
			debugScreen("Supports only duo SIM");
			return;
		}
		int events = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		mDuoTM.listen(mPhoneStateListener0, events, 0);
		mDuoTM.listen(mPhoneStateListener1, events, 1);
		
		// get GPS coordinates
		mLM = (LocationManager) getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setCostAllowed(false);
		String provider = mLM.getBestProvider(criteria, false);
		// location updates: at least 1 sec and 2 meter change
		mLM.requestLocationUpdates(provider, GPS_UPDATE_INTERVAL, GPS_UPDATE_DISTANCE, mGpsLocationListener);

		// create collection data timer
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				mTimerHandler.post(new Runnable() {
					@Override
					public void run() {
						synchronized(mInfoLock) {
							setGsmInfo();
						}
						swapInfo();
						saveLog();
						displayGsmInfo();
						displayGpsInfo();
					}
				});
			}
		}, TIMER_DELAY, TIMER_INTERVAL);
		
	}
	
	private final PhoneStateListener mPhoneStateListener0 = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			synchronized(mInfoLock){
				mCurrInfo[0].setLevel(signalStrength.getGsmSignalStrength());
			}
			super.onSignalStrengthsChanged(signalStrength);
		}
	
	};
	
	private final PhoneStateListener mPhoneStateListener1 = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			synchronized(mInfoLock){
				mCurrInfo[1].setLevel(signalStrength.getGsmSignalStrength());
			}
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

	private final LocationListener mGpsLocationListener = new LocationListener() {
		
		@Override
		public void onLocationChanged(Location location) {
			synchronized(mInfoLock){
				setGpsInfo(location);
			}
		}
		
		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {
			synchronized(mInfoLock){
				for(int i = 0; i < SIM_CNT; i++){
					mCurrInfo[i].resetGps();
				}
			}
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
			mEmulateGps = true;
		} else {
			mEmulateGps = false;
		}
	}
    
}

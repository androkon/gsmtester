package ru.konsoft.gsmtester;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

public class GSMservice extends Service {
	
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
	private static final int GPS_UPDATE_INTERVAL = 1000; // millisec
	private static final int GPS_UPDATE_DISTANCE = 2; // meter
	private boolean mEmulateGps = false;
	private Location mFakeLocation = new Location("gps");
	
	private final static int LOG_SIZE = 4;
	private Info[] mLogBuff = new Info[LOG_SIZE];
	private int mLogFill = 0;
	private static final String mLoggerUrl = "http://www.kadastr-ekz.ru/plugins/gsmlogger/logging.php";

	private BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mEmulateGps = intent.getBooleanExtra(getString(R.string.gsmservice_emulate_gps), false);
			if(mEmulateGps){
				mFakeLocation.setLatitude(55.70);
				mFakeLocation.setLongitude(37.62);
				mFakeLocation.setAccuracy(10);
				setGpsInfo(mFakeLocation);
			}
			//Debug.log("emulate gps: " + mEmulateGps);
		}

	};

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
				setGpsInfo(null);
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

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

	private final PhoneStateListener mPhoneStateListener0 = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			synchronized(mInfoLock){
				mCurrInfo[0].setLevel(signalStrength.getGsmSignalStrength());
			}
			super.onSignalStrengthsChanged(signalStrength);
		}

	};

	public class loggerWebService extends AsyncTask<String, String, Boolean> {
		
		@Override
		protected Boolean doInBackground(String... args) {
			HttpURLConnection conn = null;
			try{
				conn = (HttpURLConnection) (new URL(args[0])).openConnection();
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-encoding", "gzip");
				conn.setRequestProperty("Content-type", "application/octet-stream");
				
				GZIPOutputStream gz = new GZIPOutputStream(conn.getOutputStream());
				gz.write(args[1].getBytes());
				gz.flush();
				gz.close();
				
				int status = conn.getResponseCode();
				
				if(status == HttpURLConnection.HTTP_OK){
					Debug.log("send OK");
					Debug.log("response: " + getHttpResponse(conn.getInputStream()));
				}else{
					Debug.log("send ERR. status: " + status + " " + conn.getResponseMessage());
					Debug.log("err: " + getHttpResponse(conn.getErrorStream()));
				}
			}catch(Exception e){
				Debug.log("Exception: " + Debug.stack(e));
			}finally{
			    if(conn != null)
			        conn.disconnect();
			}
			
			return null;
		}

		private String getHttpResponse(InputStream inputStream) throws IOException  {
			InputStreamReader streamReader = null;
			BufferedReader bufferedReader = null;
			char[] buf = new char[1024];
			StringBuilder sb = new StringBuilder();
			
			try{
				streamReader = new InputStreamReader(inputStream, "UTF-8");
				bufferedReader = new BufferedReader(streamReader, 1024);
				int len = 0;
				while((len = bufferedReader.read(buf)) != -1){
				    sb.append(buf, 0, len);
				}
			}catch(Exception e){
				Debug.log(Debug.stack(e));
			}finally{
				if(bufferedReader != null)
					bufferedReader.close();
				if(streamReader != null)
					streamReader.close();
			}
			
			return sb.toString();
		}

	}

	private void initDuo() {
		for(int i = 0; i < SIM_CNT; i++){
	        mCurrInfo[i] = new Info(i);
	        mLastInfo[i] = new Info(i);
		}
	}

	private static int getSignalLevelProgress(int level) {
		if(level > 31)
			level = 0;
		int progress = (int) ((((float) level) / 31.0) * 100);

		return progress;
	}

	private void setGpsInfo(Location location) {
		for(int i = 0; i < SIM_CNT; i++){
			if(location == null)
				mCurrInfo[i].resetGps();
			else
				mCurrInfo[i].setGps(location);
		}
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
				Debug.log("error sim " + i + " " + Debug.stack(e));
			}

			info.setRX(TrafficStats.getMobileRxBytes());
			info.setTX(TrafficStats.getMobileTxBytes());

			info.setTime(time);
		}
		if(mEmulateGps){
			mFakeLocation.setLatitude(mFakeLocation.getLatitude() + 0.01);
			mFakeLocation.setLongitude(mFakeLocation.getLongitude() + 0.01);
			setGpsInfo(mFakeLocation);
		}
	}

	private void swapInfo() {
		for(int i = 0; i < SIM_CNT; i++){
			Info last, curr, prev;

			curr = mCurrInfo[i];
			prev = mLastInfo[i];
			
			last = new Info(curr.getSlot() - 1);

			last.setOperator(curr.getOperator());
			last.setOpercode(curr.getOpercode());
			last.setNettype(curr.getNettype());
			last.setLevel(curr.getLevel());
			last.setProgress(curr.getProgress());
			last.setCID(curr.getCID());
			last.setLAC(curr.getLAC());
			last.setDatastate(curr.getDatastate());

			last.setSpeedRX((int)((curr.getRX() - prev.getRX()) * 1000.0 / (curr.getTime() - prev.getTime())));
			last.setSpeedTX((int)((curr.getTX() - prev.getTX()) * 1000.0 / (curr.getTime() - prev.getTime())));

			last.setRX(curr.getRX());
			last.setTX(curr.getTX());

			last.setLat(curr.getLat());
			last.setLon(curr.getLon());
			last.setAcc(curr.getAcc());
			last.setSpeed(curr.getSpeed());

			last.setTime(curr.getTime());
			
			mLastInfo[i] = last;
		}
	}

	private void saveLog() {
		if(! isGpsReady())
			return;

		for(int i = 0; i < SIM_CNT; i++){
			if(mLogFill >= LOG_SIZE)
				addToStorage();
			mLogBuff[mLogFill] = mLastInfo[i];
			mLogFill++;
		}
	}

	private boolean isGpsReady() {
		if(
			mLastInfo[0].getLat() != 0.0 &&
			mLastInfo[0].getLon() != 0.0 &&
			mLastInfo[0].getAcc() < 50.0
		)
			return true;
		else
			return false;
	}

	private void addToStorage() {
		if(! mEmulateGps){
			try{
				saveLocalFile();
			}catch(Exception e){
				Debug.log(Debug.stack(e));
			}
			sendToRemoteServer();
		}

		mLogFill = 0;
	}
	
	private void saveLocalFile() throws IOException {
		FileWriter file = null;
		// create new daily log file with append
		try{
			File dir = getExternalFilesDir(null);
			String fname = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(System.currentTimeMillis())) + ".log";

			file = new FileWriter(new File(dir, fname), true);

			for(int i = 0; i < LOG_SIZE; i++){
				Info info = mLogBuff[i];
				if(info.getOpercode() != 0){
					file.write(getLog(info));
				}
			}
			file.flush();
		}catch(Exception e){
			Debug.log("write " + Debug.stack(e));
		}finally{
			if(file != null)
				file.close();
		}
	}
	
	private void sendToRemoteServer() {
		if(isInternet(getApplicationContext())){
			JSONArray jsonArr = new JSONArray();
			try{
				for(int i = 0; i < LOG_SIZE; i++){
					Info info = mLogBuff[i];
					if(info.getOpercode() != 0){
						jsonArr.put(info.toJson());
					}
				}
				new loggerWebService().execute(new String[] {mLoggerUrl, jsonArr.toString()});
			}catch(Exception e){
				Debug.log(Debug.stack(e));
			}
		}
	}
	
	private static boolean isInternet(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
		if(cm != null){
			NetworkInfo[] ni = cm.getAllNetworkInfo();
			if(ni != null){
				for(int i = 0; i < ni.length; i++){
					if(ni[i].getState() == NetworkInfo.State.CONNECTED)
						return true;
				}
			}
		}
		return false;
	}
	
	private static String getLog(Info info) {
		StringBuilder sb = new StringBuilder();
		String sep = ",";
		String eol = "\n";

		sb
			.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date(info.getTime()))).append(sep)
			.append(String.valueOf(info.getTime())).append(sep)
			.append(String.valueOf(info.getLat())).append(sep)
			.append(String.valueOf(info.getLon())).append(sep)
			.append(String.valueOf(info.getAcc())).append(sep)
			.append(String.valueOf(info.getSlot())).append(sep)
			.append(info.getOperator()).append(sep)
			.append(String.valueOf(info.getOpercode())).append(sep)
			.append(String.valueOf(info.getNettype())).append(sep)
			.append(String.valueOf(info.getLevel())).append(sep)
			.append(String.valueOf(info.getProgress())).append(sep)
			.append(String.valueOf(info.getCID())).append(sep)
			.append(String.valueOf(info.getLAC()));
		if(info.getDatastate() == TelephonyManager.DATA_CONNECTED){
			sb
				.append(sep)
				.append(String.valueOf(info.getDatastate())).append(sep)
				.append(String.valueOf(info.getSpeedRX())).append(sep)
				.append(String.valueOf(info.getSpeedTX()));
		}
		sb.append(eol);

		return sb.toString();
	}
	
	private void sendInfo() {
		Intent intent = new Intent(getString(R.string.gsmserviceserver));
		intent.putExtra(getString(R.string.gsmservice_info), mLastInfo);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void stopListening() {
		mDuoTM.listen(mPhoneStateListener0, PhoneStateListener.LISTEN_NONE, 0);
		mDuoTM.listen(mPhoneStateListener1, PhoneStateListener.LISTEN_NONE, 1);

		mLM.removeUpdates(mGpsLocationListener);

		mTimer.cancel();
	}

	private void startAllListeners() {
		initDuo();
		
		// get GSM mobile network settings
		mDuoTM = new DuoTelephonyManager((TelephonyManager) getSystemService(TELEPHONY_SERVICE));
		if(! mDuoTM.isDuoReady()){
			Debug.log("Supports only duo SIM");
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
									swapInfo();
								}
								saveLog();
								sendInfo();
							}
						});
				}
			}, TIMER_DELAY, TIMER_INTERVAL
		);
	}

	public static int getSIM_CNT() {
		return SIM_CNT;
	}

	@Override
    public void onCreate() {
		super.onCreate();
		
		LocalBroadcastManager.getInstance(this).
			registerReceiver(mBR, new IntentFilter(getString(R.string.gsmserviceclient)));
		
        try{
	        startAllListeners();
        }catch(Exception e){
        	Debug.log("create " + Debug.stack(e));
        }
	}

	@Override
	public void onDestroy() {
		stopListening();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}

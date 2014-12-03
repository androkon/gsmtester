package ru.konsoft.gsmtester;

import android.app.*;
import android.content.*;
import android.hardware.*;
import android.location.*;
import android.net.*;
import android.os.*;
import android.support.v4.content.*;
import android.telephony.*;
import android.telephony.gsm.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;

public class GSMservice extends Service {

	private DuoTelephonyManager mDuoTM;

	private final static int SIM_CNT = 2;
	private Info[] mLastInfo = new Info[SIM_CNT];
	private Info[] mCurrInfo = new Info[SIM_CNT];
	private Object mInfoLock = new Object();

	private Timer mTimer = new Timer();
	private Handler mTimerHandler = new Handler();
	private final static long TIMER_DELAY = 1000; // millisec
	private final static long TIMER_INTERVAL = 1000; // millisec

	private LocationManager mLM;
	private final static int GPS_UPDATE_INTERVAL = 1000; // millisec
	private final static int GPS_UPDATE_DISTANCE = 2; // meter
	private boolean mEmulateGps = false;
	private Location mFakeLocation = new Location("gps");
	
	private final static int LOG_SIZE = 1200; // 1 minute X 2 sim
	private Info[][] mLogBuff = new Info[LOG_SIZE][SIM_CNT];
	private int mLogFill = 0;
	private final static String mLoggerUrl = "http://www.kadastr-ekz.ru/plugins/gsmlogger/logging.php";
	private final static Boolean mUseRemoteLogger = false;

	private BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mEmulateGps = intent.getBooleanExtra(getString(R.string.gsmservice_emulate_gps), false);
			if(! mEmulateGps){
				setGPSInfo(null);
			}
			//Debug.log("emulate gps: " + mEmulateGps);
		}

	};

	private final LocationListener mGpsLocationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			synchronized(mInfoLock){
				setGPSInfo(location);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {
			synchronized(mInfoLock){
				setGPSInfo(null);
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

	private void setGPSInfo(Location location) {
		for(int i = 0; i < SIM_CNT; i++){
			if(location == null)
				mCurrInfo[i].resetGps();
			else
				mCurrInfo[i].setGps(location);
		}
	}

	private void setGSMInfo() {
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
			//double r = g / 180f * Math.PI;
			//mFakeLocation.setLatitude(mFakeLocation.getLatitude() + 0.001);// * Math.sin(r));
			mFakeLocation.setLongitude(mFakeLocation.getLongitude() + 0.0001);// * Math.cos(r));
			setGPSInfo(mFakeLocation);
			//g++;
			//if(g == 360)
			//	g = 0;
		}
		
		swapInfo();
	}
	
	private int g = 0;

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
			
		if(mLogFill >= LOG_SIZE){
			addToStorage();
			mLogFill = 0;
		}
		
		for(int i = 0; i < SIM_CNT; i++)
			mLogBuff[mLogFill][i] = mLastInfo[i];
		
		mLogFill++;
		
		showInfo();
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
		//if(! isGpsReady())
		//	return;

		try{
			saveLocalFile();
		}catch(Exception e){
			Debug.log(Debug.stack(e));
		}
		sendToRemoteServer();
	}
	
	private void saveLocalFile() throws IOException {
		FileWriter file = null;
		// create new daily log file with append
		try{
			File dir = getExternalFilesDir(null);
			String fname = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(System.currentTimeMillis())) + ".log";

			file = new FileWriter(new File(dir, fname), true);

			for(int i = 0; i < LOG_SIZE; i++){
				for(int j = 0; j < SIM_CNT; j++){
					Info info = mLogBuff[i][j];
					if(info.getOpercode() != 0)
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
		if(! mUseRemoteLogger)
			return;
		
		if(isInternet(getApplicationContext())){
			JSONArray jsonArr = new JSONArray();
			try{
				for(int i = 0; i < LOG_SIZE; i++){
					for(int j = 0; j < SIM_CNT; j++){
						Info info = mLogBuff[i][j];
						if(info.getOpercode() != 0){
							jsonArr.put(info.toJson());
						}
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

		  sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date(info.getTime()))).append(sep)
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
			  sb.append(sep)
				.append(String.valueOf(info.getDatastate())).append(sep)
				.append(String.valueOf(info.getSpeedRX())).append(sep)
				.append(String.valueOf(info.getSpeedTX()));
		}
		
		sb.append(eol);

		return sb.toString();
	}
	
	public class TrackInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public int mCurrPosition;
		public Info[][] mTrackInfo;
	}
	
	private TrackInfo mSendTrack = new TrackInfo();
	
	private void showInfo() {
		//Debug.log("collect");
		Intent intent = new Intent(getString(R.string.gsmserviceserver));
		mSendTrack.mCurrPosition = mLogFill;
		mSendTrack.mTrackInfo = mLogBuff;
		intent.putExtra(getString(R.string.gsmservice_info), mSendTrack);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		//Debug.log("send");
	}

	private void stopListening() {
		//mSM.unregisterListener(mSL);
		
		mDuoTM.listen(mPhoneStateListener0, PhoneStateListener.LISTEN_NONE, 0);
		mDuoTM.listen(mPhoneStateListener1, PhoneStateListener.LISTEN_NONE, 1);

		mLM.removeUpdates(mGpsLocationListener);

		mTimer.cancel();
	}
	
	//private Sensor mAccel;
	public class Accel {
		public long time = 0;
		public float x = 0, y = 0, z = 0;
		public float[] avgG = new float[3];
		public float[] currG = new float[3];
		public float[] V = new float[3], L = new float[3];
		public void Accel() {
			avgG[0] = avgG[1] = avgG[2] = 0;
			V[0] = V[1] = V[2] = 0;
		}
	}
	/*private Accel mPrevAcc = new Accel();
	private static float gravity = 9.81f;
	private static float alpha = 0.8f;
	private boolean isFirst = true;*/
	/*private SensorEventListener mSL = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			Debug.log(sensor.getName() + " : accuracy : " + accuracy);
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			SensorEvent e = event;
			//Debug.log("x : " + e.values[0] + " y : " + e.values[1] + " z : " + e.values[2]);
			Accel acc = new Accel();
			acc.x = e.values[0];
			acc.y = e.values[1];
			acc.z = e.values[2];
			//acc.time = System.currentTimeMillis();
			acc.time = e.timestamp;
			
			mPrevAcc.avgG[0] = alpha * mPrevAcc.avgG[0] + (1 - alpha) * acc.x;
			mPrevAcc.currG[0] = acc.x - mPrevAcc.avgG[0];
			
			double dT;
			double dA, dV, dL;
			double dVx = 0, Ax = 0, dLx = 0;
			dA = Math.sqrt(Math.pow(acc.x - mPrevAcc.x, 2) + Math.pow(acc.y - mPrevAcc.y, 2) + Math.pow(acc.z - mPrevAcc.z, 2));
			dT = (acc.time - mPrevAcc.time) / 1000000000f;
			dV = dA * dT * 100f;
			dL = dV * dT;
			
			if(! isFirst){
				Ax = mPrevAcc.currG[0];
				//if(Math.abs(Ax) > 0.1f){
					dVx = Ax * dT;
					dLx = dVx * dT;
					mPrevAcc.V[0] += dVx;
					mPrevAcc.L[0] += dLx;
				//}
			}
			
			double xR, yR, zR;
			//xR = Math.acos(acc.x / gravity) / Math.PI * 180;
			xR = Math.acos(mPrevAcc.avgG[0] / gravity) / Math.PI * 180;
			
			//if(dL > 1f){
				//Debug.log(" dv : " + dV + " dl : " + dL);
				//Debug.log("x : " + acc.x + " y : " + acc.y + " z : " + acc.z);
				//Debug.log("x : " + acc.x + " xR : " + xR);
				//Debug.log("avgX : " + mPrevAcc.avgG[0] + " currX : " + mPrevAcc.currG[0]);
				Debug.log("Vx : " + mPrevAcc.V[0] + " Ax : " + Ax);
			//}
			
			mPrevAcc.x = acc.x;
			mPrevAcc.y = acc.y;
			mPrevAcc.z = acc.z;
			mPrevAcc.time = acc.time;
			isFirst = false;
		}
		
	};*/
	
	//private SensorManager mSM;

	private void startAllListeners() {
		mFakeLocation.setLatitude(55.70);
		mFakeLocation.setLongitude(37.62);
		mFakeLocation.setAccuracy(10);
		
		initDuo();
		
		//mSM = (SensorManager) getSystemService(SENSOR_SERVICE);
		//List<Sensor> sl = mSM.getSensorList(Sensor.TYPE_ALL);
		//for(Iterator<Sensor> si = sl.iterator(); si.hasNext();){
		//	Sensor s = si.next();
		//	//Debug.log(s.getName());
		//}
		///mAccel = mSM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		//mSM.registerListener(mSL, mAccel, SensorManager.SENSOR_DELAY_FASTEST);
		
		// get GSM mobile network settings
		mDuoTM = new DuoTelephonyManager((TelephonyManager) getSystemService(TELEPHONY_SERVICE));
		if(! mDuoTM.isDuoReady()){
			Debug.log("Supports only duo SIM");
			return;
		}
		int events = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
		mDuoTM.listen(mPhoneStateListener0, events, 0);
		mDuoTM.listen(mPhoneStateListener1, events, 1);

		// GPS on
		//Intent i = new Intent("android.location.GPS_ENABLE_CHANGE");
		//i.putExtra("enabled", true);
		//sendBroadcast(i);
		//Settings.Secure.putString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, LocationManager.GPS_PROVIDER);
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
									setGSMInfo();
								}
								saveLog();
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
        Debug.log("service create");
	}

	@Override
	public void onDestroy() {
		stopListening();
		Debug.log("service destroy");
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}

package ru.konsoft.gsmtester;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.graphics.drawable.shapes.*;
import android.hardware.*;
import android.os.*;
import android.support.v4.content.*;
import android.telephony.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import ru.konsoft.gsmtester.GSMservice.*;
import ru.yandex.yandexmapkit.*;
import ru.yandex.yandexmapkit.overlay.*;
import ru.yandex.yandexmapkit.utils.*;

public class GSMinfo extends Activity {

	private TrackInfo mTrackInfo;
	
    private ViewFlipper flipper = null;
	private boolean mIsVisible = false;
	
	private MapView mMapView;
	private MapController mMapController;
	private OverlayManager mOverlayManager;
    private Overlay[] mOverlays = new Overlay[VIEW_CNT];
	private final static int VIEW_LEVEL = 0;
	//private final static int VIEW_TYPE = 1;
	//private final static int VIEW_SPEEDRX = 2;
	//private final static int VIEW_SPEEDTX = 3;
	private final static int VIEW_CNT = 1;
	private final static int COLOR_START = Color.parseColor("#FF0303");
	private final static int COLOR_END   = Color.parseColor("#0CFF03");

	private BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			//Debug.log("receive");
			mTrackInfo = (TrackInfo)intent.getSerializableExtra(getString(R.string.gsmservice_info));
			displayGsmInfo(mTrackInfo);
			displayGpsInfo(mTrackInfo);
			//Debug.log("recv: " + mTrackInfo.mInfo[0][0].toString());
		}
		
	};
	
	private void regReceiver() {
		LocalBroadcastManager.getInstance(this).
		registerReceiver(mBR, new IntentFilter(getString(R.string.gsmserviceserver)));
	}

	private void unregReceiver() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBR);
	}
	
	private void setTextViewText(int id, String text) {
		((TextView) findViewById(id)).setText(text);
	}
	
	private static String getGsmTextInfo(Info info) {
		StringBuilder sb = new StringBuilder();
		String sep = "\n";
		
		if(info.getOpercode() != 0){
			sb
				.append("Network Type: ").append(getNetworkTypeString(info.getNettype())).append(sep)
				.append("Level: ").append(String.valueOf(info.getLevel())).append(sep)
				.append("Progress: ").append(String.valueOf(info.getProgress())).append(sep)
				.append("Slot: ").append(String.valueOf(info.getSlot())).append(sep)
				.append("LAC: ").append(String.valueOf(info.getLAC())).append(sep)
				.append("CID: ").append(String.valueOf(info.getCID()));
			//if(info.getDataState() == TelephonyManager.DATA_CONNECTED){
				sb
					.append(sep)
					.append("DataState: ").append(info.getDataStateTxt()).append(sep)
					.append("Speed RX: ").append(String.valueOf(info.getSpeedRX())).append(" bytes/sec").append(sep)
					.append("Speed TX: ").append(String.valueOf(info.getSpeedTX())).append(" bytes/sec").append(sep)
					.append("RX: ").append(String.valueOf(info.getRX())).append(" bytes").append(sep)
					.append("TX: ").append(String.valueOf(info.getTX())).append(" bytes");
			//}
		}
		
		return sb.toString();
	}
	
	private void displayGsmInfo(TrackInfo trackInfo) {
		if(! mIsVisible)
			return;
			
		Info[] simInfo = trackInfo.mInfo[trackInfo.mCurrPosition];
		for(int i = 0; i < GSMservice.getSIM_CNT(); i++){
			Info info = simInfo[i];
			int signalLevel, deviceInfo, simNameId;
			String simName;
			
			if(i == 0){
				signalLevel = R.id.signalLevel0;
				deviceInfo = R.id.device_info0;
				simNameId = R.id.sim_name0;
				simName = getString(R.string.text_sim_name0);
			}else{
				signalLevel = R.id.signalLevel1;
				deviceInfo = R.id.device_info1;
				simNameId = R.id.sim_name1;
				simName = getString(R.string.text_sim_name1);
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append(simName).append(": ").append(info.getOperator());

			setTextViewText(simNameId, sb.toString());
			((ProgressBar) findViewById(signalLevel)).setProgress(info.getProgress());
			setTextViewText(deviceInfo, getGsmTextInfo(info));
		}
	}
	
	private void displayGpsInfo(TrackInfo trackInfo) {
		Info[] simInfo = trackInfo.mInfo[trackInfo.mCurrPosition];
		Info info = simInfo[0];
		
		if(mIsVisible){
			if(info.getLat() != 0.0 && info.getLon() != 0.0){
				StringBuilder sb = new StringBuilder();
			
				sb
					.append("lat: ").append(String.valueOf(info.getLat())).append("\n")
					.append("lon: ").append(String.valueOf(info.getLon())).append("\n")
					.append("acc: ").append(String.valueOf(info.getAcc())).append(" m\n")
					.append("vel: ").append(String.valueOf(info.getSpeed())).append(" m/s\n")
					.append("vel: ").append(String.valueOf(info.getSpeed() * 3.6)).append(" km/h");
				setTextViewText(R.id.gps_info, sb.toString());
			
			}else{
				setTextViewText(R.id.gps_info, "No GPS");
			}
		}
		
		//mOverlays[0].clearOverlayItems();
		
		Info prev = trackInfo.mInfo[(trackInfo.mCurrPosition + GSMservice.LOG_SIZE - 1) % GSMservice.LOG_SIZE][0];
		showPoint(simInfo, prev);
		
		if(mOverlays[VIEW_LEVEL].getOverlayItems().size() == GSMservice.LOG_SIZE)
			mOverlays[VIEW_LEVEL].removeOverlayItem((OverlayItem) mOverlays[VIEW_LEVEL].getOverlayItems().get(0));
		
		if(mIsVisible)
			mMapController.notifyRepaint();
	}

    private void showPoint(Info[] simInfo, Info prev){
		ShapeDrawable[] point = new ShapeDrawable[GSMservice.getSIM_CNT()];
		int x = 0, sz = 10;
		Info info = null;
		for(int i = 0; i < GSMservice.getSIM_CNT(); i++){
			info = simInfo[i];
			if(i == 0)
				point[i] = new ShapeDrawable(new OvalShape());
			else
				point[i] = new ShapeDrawable(new RectShape());
			//point.getPaint().setColor(interpolateColor(COLOR_START, COLOR_END, info[i].getProgress() / 100f));
			point[i].getPaint().setColor(interpolateColor(COLOR_START, COLOR_END, info.getLevel() / 31f));
			point[i].setBounds(x, 0, sz + x, sz);
			x += sz;
		}
		float r = 0f;
		if(prev != null){
			double dY = info.getLat() - prev.getLat();
			double dX = info.getLon() - prev.getLon();
			if(dX == 0 && dY == 0)
				r = 0f;
			else
				r = 90f - (float)Math.abs(Math.ceil(Math.atan(dY / dX) / Math.PI * 180f));
		}
		
		GeoPoint geoPoint = new GeoPoint(info.getLat(), info.getLon());
		OverlayItem y = new OverlayItem(geoPoint, drawableToBitmapDrawable(getResources(), point, r));
		mOverlays[VIEW_LEVEL].addOverlayItem(y);
    }
	
	private static int interpolateColor(int a, int b, float p) {
		float hsva[] = new float[3];
		float hsvb[] = new float[3];
		Color.colorToHSV(a, hsva);
		Color.colorToHSV(b, hsvb);
		for(int i = 0; i < 3; i++)
			hsvb[i] = interpolate(hsva[i], hsvb[i], p);
		return Color.HSVToColor(hsvb);
	}
	
	private static float interpolate(float a, float b, float p) {
		return (a + (b - a) * p);
	}
    
    private static BitmapDrawable drawableToBitmapDrawable (Resources res, Drawable[] drawable, float angle) {
        //if(drawable instanceof BitmapDrawable)
        //    return (BitmapDrawable)drawable;
			
		Rect rect = new Rect(0, 0, drawable[0].getBounds().width() + drawable[1].getBounds().width(), drawable[0].getBounds().height());
        Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
        drawable[0].draw(canvas);
		drawable[1].draw(canvas);
		
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		//RectF rf = new RectF(rect);
		//matrix.mapRect(rf);
		//rf.roundOut(rect);
		//Debug.log("r: " + angle + "; w: " + rect.width());
		Bitmap rbitmap = Bitmap.createBitmap(bitmap, 0, 0, rect.width(), rect.height(), matrix, true);
		
        return new BitmapDrawable(res, rbitmap);
    }
    
    public void debugScreen(String text) {
		setTextViewText(R.id.error_info, text);
	}
		
	private void startGSMservice() {
		startService(new Intent(this, GSMservice.class));
	}
	
	private void stopGSMservice() {
		stopService(new Intent(this, GSMservice.class));
	}
	
	private static String getNetworkTypeString(int val) {
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

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
        setContentView(R.layout.activity_gsminfo);
        
        flipper = (ViewFlipper) findViewById(R.id.flipper);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int layouts[] = new int[]{R.layout.gsm, R.layout.ymap};
        for(int layout : layouts){
            flipper.addView(inflater.inflate(layout, null));
        }
		
        mMapView = (MapView) findViewById(R.id.map);
        mMapController = mMapView.getMapController();
        mOverlayManager = mMapController.getOverlayManager();
		for(int i = 0; i < VIEW_CNT; i++){
    	    mOverlays[i] = new Overlay(mMapController);
        	mOverlayManager.addOverlay(mOverlays[i]);
		}
        
        mMapController.setZoomCurrent(12);
        mOverlayManager.getMyLocation().setEnabled(true);
        mMapView.showJamsButton(true);
        
		try{
	        startGSMservice();
			regReceiver();
  	  	}catch(Exception e){
    	    debugScreen("create " + Debug.stack(e));
  	 	}
		
		//createAccel();
		
		Debug.log("create activity");
	}
	
	@Override
	protected void onStart() {
		Debug.log("start");
		super.onStart();
	}

	@Override
	protected void onStop() {
		Debug.log("stop");
		super.onStop();
	}
	
	@Override
	protected void onPause() {
		//unregReceiver();
		mIsVisible = false;
		//pauseAccel();
		Debug.log("pause");
		super.onPause();
	}

	@Override
	protected void onResume() {
		//regReceiver();
		mIsVisible = true;
		//resumeAccel();
		Debug.log("resume");
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		if(isFinishing()){
			stopGSMservice();
			unregReceiver();
		}
		Debug.log("destroy; isfinish: " + isFinishing());
		super.onDestroy();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		//Debug.log("create menu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gsminfo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		//Debug.log("select menu");
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
		if(showFlipp(id))
			return true;
		return super.onOptionsItemSelected(item);
	}
	
	private int mFlipp = 0;
	
	private boolean showFlipp(int id) {
		mFlipp = id;
        if (id == R.id.action_gsm) {
        	flipper.setDisplayedChild(flipper.indexOfChild(findViewById(R.id.view_gsm)));
            return true;
        }
        if (id == R.id.action_ymap) {
        	flipper.setDisplayedChild(flipper.indexOfChild(findViewById(R.id.view_ymap)));
            return true;
        }
		return false;
    }
    
	public void onToggleEmulate(View view) {
		boolean OnOff = ((ToggleButton) view).isChecked();
		
		sendEmulate(OnOff);
	}
	
	private void sendEmulate(boolean isEmulate) {
		Intent intent = new Intent(getString(R.string.gsmserviceclient));
		intent.putExtra(getString(R.string.gsmservice_emulate_gps), isEmulate);
		
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		
		//if(isEmulate)
		//	startAccelTest();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt("flipp", mFlipp);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		boolean OnOff = ((ToggleButton) findViewById(R.id.toggleButton1)).isChecked();
		
		if(OnOff)
			sendEmulate(OnOff);
			
		showFlipp(savedInstanceState.getInt("flipp"));
	}
	
	public abstract class Accelerometer implements SensorEventListener {
		protected float lastX;
		protected float lastY;
		protected float lastZ;
		public abstract Point getPoint();
		public void onAccuracyChanged(Sensor arg0, int arg1) {}
	}
	
	public class Point {
		private float x = 0;
		private float y = 0;
		private float z = 0;
		private int cnt = 1;
		public float getX() {
			return this.x / (float)this.cnt;
		}
		public float getY() {
			return this.y / (float)this.cnt;
		}
		public float getZ() {
			return this.z / (float)this.cnt;
		}
		public Point(float x, float y, float z, int cnt) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.cnt = cnt;
		}
	}
	
	public class XYZAccelerometer extends Accelerometer {
		private static final int BUFFER_SIZE = 500;
		// calibration
		private float dX = 0;
		private float dY = 0;
		private float dZ = -9.81f;
		// buffer variables
		private float X;
		private float Y;
		private float Z;
		private int cnt = 0;
		// returns last SensorEvent parameters
		public Point getLastPoint() {
			return new Point(this.lastX, this.lastY, this.lastZ, 1);
		}
		// returns parameters, using buffer: average acceleration
		// since last call of getPoint().
		public Point getPoint() {
			if(this.cnt == 0){
				return getLastPoint();
			}
			Point p = new Point(this.X, this.Y, this.Z, this.cnt);
			reset();
			return p;
		}
		// resets buffer
		public void reset() {
			this.cnt = 0;
			this.X = this.Y = this.Z = 0;
		}
		@Override
		public void onSensorChanged(SensorEvent se) {
			float x = se.values[SensorManager.DATA_X] + dX;
			float y = se.values[SensorManager.DATA_Y] + dY;
			float z = se.values[SensorManager.DATA_Z] + dZ;
			this.lastX = x;
			this.lastY = y;
			this.lastZ = z;
			this.X += x;
			this.Y += y;
			this.Z += z;
			if(this.cnt < BUFFER_SIZE - 1){
				this.cnt++;
			}else{
				reset();
			}
		}
		/*public void setdX(float dX) {
			this.dX = dX;
		}
		public void setdY(float dY) {
			this.dY = dY;
		}
		public void setdZ(float dZ) {
			this.dZ = dZ;
		}*/
	}
	
	public class MeasurePoint {
		private float x;
		private float y;
		private float z;
		private float speedBefore; // meter / sec
		private float speedAfter; // meter / sec
		private float distance; // meter
		private float acceleration; // meter / sec ^ 2
		private long interval; // millisec
		public MeasurePoint(float x, float y, float z, float speedBefore, long interval) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.speedBefore = speedBefore;
			this.interval = interval;
			this.speedAfter = 0;
			calc();
		}
		private void calc() {
			//Acceleration as projection of current vector on average
			this.acceleration = (float)Math.sqrt(this.x * this.x + 
				this.y * this.y + 
				this.z * this.z
			);
			float t = ((float)this.interval / 1000f); // sec
			this.speedAfter = this.speedBefore + this.acceleration * t;
			this.distance = this.speedBefore * t + this.acceleration * t * t / 2;
		}
		public float getSpeedAfter() {
			return speedAfter;
		}
	}
	
	public class MeasureData {
		// points from accelerometr
		private LinkedList accData;
		private LinkedList data;
		// timer interval of generating points
		private long interval; // millisec
		public MeasureData(long interval) {
			this.interval = interval;
			this.accData = new LinkedList();
			this.data = new LinkedList();
		}
		public void addPoint(Point p) {
			this.accData.add(p);
		}
		public void process() {
			for(int i = 0; i < this.accData.size(); ++i){
				Point p = (Point)this.accData.get(i);
				float speed = 0;
				if(i > 0){
					speed = ((MeasurePoint)this.data.get(i - 1)).getSpeedAfter();
				}
				this.data.add(new MeasurePoint(p.getX(), p.getY(), p.getZ(), speed, this.interval));
			}
			this.accData.clear();
		}
		public float getLastSpeed() {
			return ((MeasurePoint)this.data.getLast()).getSpeedAfter();
		}
		public float getLastSpeedKm() {
			float ms = getLastSpeed();
			return ms * 3.6f;
		}
	}
	
	static final int TIMER_DONE = 2;
	static final int START = 3;
	//private StartCatcher mStartListener;
	private XYZAccelerometer xyzAcc;
	private SensorManager mSensorManager;
	private static final long UPDATE_INTERVAL = 500; // millisec
	private static final long MEASURE_TIMES = 2; // times of update interval
	private Timer timer;
	private TextView tv;
	//private Button testBtn;
	int counter;
	private MeasureData mdXYZ;
	/** handler for async events*/
	Handler hRefresh = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case TIMER_DONE:
					onMeasureDone();
					String es1 = Float.toString(Math.round(mdXYZ.getLastSpeedKm() * 100) / 100f);
					tv.append(" END SPEED " + es1 +" \n");
					enableButtons();
					break;
				case START:
					tv.append(" START");
					timer = new Timer();
					timer.scheduleAtFixedRate(
						new TimerTask() {
							public void run() {
								dumpSensor();
							}
						},
						0,
						UPDATE_INTERVAL);
					break;
			}
		}
	};
	/** Called when the activity is first created. */
	//@Override
	//public void onCreate(Bundle savedInstanceState) {
	private void createAccel() {
		//super.onCreate(savedInstanceState);
		//setContentView(R.layout.main);
		tv = (TextView) findViewById(R.id.error_info);
		//testBtn = (Button) findViewById(R.id.btn);
	}
	//@Override
	//protected void onResume() {
	private void resumeAccel() {
		//super.onResume();
		tv.append("\n ..");
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		setAccelerometer();
		//setStartCatcher();
		mSensorManager.registerListener(xyzAcc, 
			mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
			SensorManager.SENSOR_DELAY_GAME
		);
	}
	//@Override
	//protected void onPause() {
	private void pauseAccel() {
		mSensorManager.unregisterListener(xyzAcc);
		//super.onPause();
	}
	//public void onButtonTest(View v) {
	private void startAccelTest() {
		disableButtons();
		mdXYZ = new MeasureData(UPDATE_INTERVAL);
		counter = 0;
		tv.setText("");
		hRefresh.sendEmptyMessage(START);
	}
	void dumpSensor() {
		// each UPDATE_INTERVAL millisec
		++counter;
		mdXYZ.addPoint(xyzAcc.getPoint());
		if(counter > MEASURE_TIMES){
			//timer.cancel();
			counter = 0;
			hRefresh.sendEmptyMessage(TIMER_DONE);
		}
	}
	private void enableButtons() {
		//testBtn.setEnabled(true);
	}
	private void setAccelerometer() {
		xyzAcc = new XYZAccelerometer();
		mSensorManager.registerListener(xyzAcc,
			mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
			SensorManager.SENSOR_DELAY_UI
		);
	}
	private void disableButtons() {
		//testBtn.setEnabled(false);
	}
	private void onMeasureDone() {
		mdXYZ.process();
	}
    
}

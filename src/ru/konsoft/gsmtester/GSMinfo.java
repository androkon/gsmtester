package ru.konsoft.gsmtester;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.graphics.drawable.*;
import android.graphics.drawable.shapes.*;
import android.os.*;
import android.support.v4.content.*;
import android.telephony.*;
import android.view.*;
import android.widget.*;
import ru.yandex.yandexmapkit.*;
import ru.yandex.yandexmapkit.overlay.*;
import ru.yandex.yandexmapkit.utils.*;

public class GSMinfo extends Activity {

	private Info[] mLastInfo;
	
    private ViewFlipper flipper = null;
	
	private MapView mMapView;
	private MapController mMapController;
	private OverlayManager mOverlayManager;
    private Overlay mOverlay;
    private MyOverlayIRender mRender;

	private BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mLastInfo = (Info[])intent.getSerializableExtra(getString(R.string.gsmservice_info));
			displayGsmInfo(mLastInfo);
			displayGpsInfo(mLastInfo[0]);
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
			if(info.getDatastate() == TelephonyManager.DATA_CONNECTED){
				sb
					.append(sep)
					.append("DataState: ").append(String.valueOf(info.getDatastate())).append(sep)
					.append("Speed RX: ").append(String.valueOf(info.getSpeedRX())).append(" bytes/sec").append(sep)
					.append("Speed TX: ").append(String.valueOf(info.getSpeedTX())).append(" bytes/sec").append(sep)
					.append("RX: ").append(String.valueOf(info.getRX())).append(" bytes").append(sep)
					.append("TX: ").append(String.valueOf(info.getTX())).append(" bytes");
			}
		}
		
		return sb.toString();
	}
	
	private void displayGsmInfo(Info[] gsmInfo) {
		for(int i = 0; i < GSMservice.getSIM_CNT(); i++){
			Info info = gsmInfo[i];
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
	
	private void displayGpsInfo(Info info) {
		
		if(info.getLat() != 0.0 && info.getLon() != 0.0){
			StringBuilder sb = new StringBuilder();
			
			sb
				.append("lat: ").append(String.valueOf(info.getLat())).append("\n")
				.append("lon: ").append(String.valueOf(info.getLon())).append("\n")
				.append("acc: ").append(String.valueOf(info.getAcc())).append(" m\n")
				.append("vel: ").append(String.valueOf(info.getSpeed())).append(" m/s");
			setTextViewText(R.id.gps_info, sb.toString());

	        GeoPoint geoPoint = new GeoPoint(info.getLat(), info.getLon());
			//mMapController.setPositionAnimationTo(geoPoint);
	        showObject(geoPoint);
		}else{
			setTextViewText(R.id.gps_info, "No GPS");
		}
	}

    private void showObject(GeoPoint geoPoint){
        Resources res = getResources();
        OverlayItem y;

        ShapeDrawable o = new ShapeDrawable(new OvalShape());
		o.getPaint().setColor(10);
		o.setBounds(0, 0, 100, 100);
		
		ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
		drawable.getPaint().setColor(Color.GRAY);
		drawable.getPaint().setStyle(Style.STROKE);
		drawable.getPaint().setStrokeWidth(10);
		drawable.getPaint().setAntiAlias(true);
		drawable.setBounds(0, 0, 100, 100);

//		Bitmap b = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
//		Canvas c = new Canvas(b);
//		
//		o.draw(c);
		
//		BitmapDrawable d = (BitmapDrawable)res.getDrawable(R.drawable.ymk_user_location_gps);
//		y = new OverlayItem(geoPoint, d);
//		mOverlay.addOverlayItem(y);

		y = new OverlayItem(geoPoint, drawableToBitmapDrawable(drawable));
		mOverlay.addOverlayItem(y);
    }
    
    private BitmapDrawable drawableToBitmapDrawable (Drawable drawable) {
        if(drawable instanceof BitmapDrawable){
            return ((BitmapDrawable)drawable);
        }

        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return new BitmapDrawable(getResources(), bitmap);
    }
    
    public class MyOverlayIRender extends OverlayIRender {
    	Overlay mOverlay;

		@Override
		public void draw(Canvas canvas, OverlayItem item) {
			Drawable d = item.getDrawable();
			d.draw(canvas);
		}
		
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
        mOverlay = new Overlay(mMapController);
        mOverlayManager.addOverlay(mOverlay);
        
        mMapController.setZoomCurrent(12);
        mOverlayManager.getMyLocation().setEnabled(false);
        mMapView.showJamsButton(false);
        //mRender = new MyOverlayIRender(this);
        //mOverlay.setIRender(mRender);
        
        try{
	        startGSMservice();
        }catch(Exception e){
        	debugScreen("create " + Debug.stack(e));
        }
		//Debug.log("create");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		//Debug.log("start");
	}

	@Override
	protected void onStop() {
		super.onStop();
		//Debug.log("stop");
	}
	
	@Override
	protected void onPause() {
		unregReceiver();
		super.onPause();
		//Debug.log("pause");
	}

	@Override
	protected void onResume() {
		regReceiver();
		super.onResume();
		//Debug.log("resume");
	}
	
	@Override
	protected void onDestroy() {
		stopGSMservice();
		super.onDestroy();
		//Debug.log("destroy");
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
        if (id == R.id.action_gsm) {
        	flipper.setDisplayedChild(flipper.indexOfChild(findViewById(R.id.view_gsm)));
            return true;
        }
        if (id == R.id.action_ymap) {
        	flipper.setDisplayedChild(flipper.indexOfChild(findViewById(R.id.view_ymap)));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
	public void onToggleEmulate(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		
		Intent intent = new Intent(getString(R.string.gsmserviceclient));
		intent.putExtra(getString(R.string.gsmservice_emulate_gps), on);
		
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
    
}

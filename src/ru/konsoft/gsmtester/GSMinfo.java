package ru.konsoft.gsmtester;

import ru.konsoft.gsmtester.GSMservice.TrackInfo;
import ru.yandex.yandexmapkit.MapController;
import ru.yandex.yandexmapkit.MapView;
import ru.yandex.yandexmapkit.OverlayManager;
import ru.yandex.yandexmapkit.overlay.Overlay;
import ru.yandex.yandexmapkit.overlay.OverlayItem;
import ru.yandex.yandexmapkit.utils.GeoPoint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

public class GSMinfo extends Activity {

	private TrackInfo mTrackInfo;
	
    private ViewFlipper flipper = null;
	
	private MapView mMapView;
	private MapController mMapController;
	private OverlayManager mOverlayManager;
    private Overlay mOverlay;
	private final static int COLOR_START = Color.parseColor("#FF0303");
	private final static int COLOR_END   = Color.parseColor("#0CFF03");

	private BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Debug.log("receive");
			mTrackInfo = (TrackInfo)intent.getSerializableExtra(getString(R.string.gsmservice_info));
			mTrackInfo.mCurrPosition -= GSMservice.getSIM_CNT();
			displayGsmInfo(mTrackInfo);
			displayGpsInfo(mTrackInfo);
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
	
	private void displayGsmInfo(TrackInfo trackInfo) {
		for(int i = 0; i < GSMservice.getSIM_CNT(); i++){
			Info info = trackInfo.mTrackInfo[trackInfo.mCurrPosition + i];
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
		Info info = trackInfo.mTrackInfo[trackInfo.mCurrPosition];
		
		if(info.getLat() != 0.0 && info.getLon() != 0.0){
			StringBuilder sb = new StringBuilder();
			
			sb
				.append("lat: ").append(String.valueOf(info.getLat())).append("\n")
				.append("lon: ").append(String.valueOf(info.getLon())).append("\n")
				.append("acc: ").append(String.valueOf(info.getAcc())).append(" m\n")
				.append("vel: ").append(String.valueOf(info.getSpeed())).append(" m/s");
			setTextViewText(R.id.gps_info, sb.toString());
			
		}else{
			setTextViewText(R.id.gps_info, "No GPS");
		}

        mOverlay.clearOverlayItems();
		for(int i = 0; i < trackInfo.mCurrPosition + GSMservice.getSIM_CNT(); i++)
			if(trackInfo.mTrackInfo[i].getSlot() == 1)
				showPoint(trackInfo.mTrackInfo[i]);
		
		mMapController.notifyRepaint();
	}

    private void showPoint(Info info){
		ShapeDrawable point = new ShapeDrawable(new OvalShape());

		point.getPaint().setColor(interpolateColor(COLOR_START, COLOR_END, info.getProgress() / 100f));

		point.setBounds(0, 0, 20, 20);

		GeoPoint geoPoint = new GeoPoint(info.getLat(), info.getLon());
		OverlayItem y = new OverlayItem(geoPoint, drawableToBitmapDrawable(getResources(), point));
		mOverlay.addOverlayItem(y);
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
    
    private static BitmapDrawable drawableToBitmapDrawable (Resources res, Drawable drawable) {
        if(drawable instanceof BitmapDrawable)
            return (BitmapDrawable)drawable;
			
		Rect rect = drawable.getBounds();
        Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        drawable.draw(new Canvas(bitmap));

        return new BitmapDrawable(res, bitmap);
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
        
        try{
	        startGSMservice();
        }catch(Exception e){
        	debugScreen("create " + Debug.stack(e));
        }
		Debug.log("create");
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
		unregReceiver();
		Debug.log("pause");
		super.onPause();
	}

	@Override
	protected void onResume() {
		regReceiver();
		Debug.log("resume");
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		stopGSMservice();
		Debug.log("destroy");
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

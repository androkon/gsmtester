package ru.konsoft.gsmtester;

import ru.yandex.yandexmapkit.MapController;
import ru.yandex.yandexmapkit.MapView;
import ru.yandex.yandexmapkit.utils.GeoPoint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class GSMinfo extends Activity {

	private Info[] mLastInfo;
	
	private MapView mMapView;
	private MapController mMapController;

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
		sb.append("RX: ").append(String.valueOf(info.getRX())).append(" bytes").append(sep);
		sb.append("TX: ").append(String.valueOf(info.getTX())).append(" bytes").append(sep);
		
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
			
			sb.append("lat: ").append(String.valueOf(info.getLat())).append("\n");
			sb.append("lon: ").append(String.valueOf(info.getLon())).append("\n");
			sb.append("acc: ").append(String.valueOf(info.getAcc())).append(" m\n");
			sb.append("vel: ").append(String.valueOf(info.getSpeed())).append(" m/s");
			setTextViewText(R.id.gps_info, sb.toString());
		}else{
			setTextViewText(R.id.gps_info, "No GPS");
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gsminfo);
		
          mMapView = (MapView) findViewById(R.id.map);
        mMapController = mMapView.getMapController();
        mMapController.setPositionAnimationTo(new GeoPoint(60.113337, 55.151317));
        mMapController.setZoomCurrent(15);
        
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
        return super.onOptionsItemSelected(item);
    }
    
	public void onToggleEmulate(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		
		Intent intent = new Intent(getString(R.string.gsmserviceclient));
		intent.putExtra(getString(R.string.gsmservice_emulate_gps), on);
		
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
    
}

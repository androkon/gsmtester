package ru.konsoft.gsmtester;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.telephony.TelephonyManager;

public class Info implements Serializable {

	private static final long serialVersionUID = 1L;

	private long mTime;
	
	private double mLat;
	private double mLon;
	private float mAcc;
	private float mSpeed;

	private int mSlot;

	private int mLevel;
	private int mProgress;
	private int mNettype;
	private String mOperator;
	private int mOpercode;
	private int mLAC;
	private int mCID;

	private int mDataState;
	private long mRX; // no log
	private long mTX; // no log
	private int mSpeedRX;
	private int mSpeedTX;

	public Info(int i) {
		this.mSlot = i + 1;
	}

	public long getTime() {
		return mTime;
	}

	public int getSlot() {
		return mSlot;
	}

	public int getLevel() {
		return mLevel;
	}

	public int getProgress() {
		return mProgress;
	}

	public int getNettype() {
		return mNettype;
	}

	public String getOperator() {
		return mOperator;
	}

	public int getOpercode() {
		return mOpercode;
	}

	public int getLAC() {
		return mLAC;
	}

	public int getCID() {
		return mCID;
	}

	public int getDataState() {
		return mDataState;
	}
	
	public String getDataStateTxt() {
		int state = getDataState();
		String ret;
		switch(state) {
			case 0: ret = "not connected"; break;
			case 1: ret = "connecting..."; break;
			case 2: ret = "connected"; break;
			default: ret = "unknown"; break;
		}
		return ret;
	}

	public long getRX() {
		return mRX;
	}

	public long getTX() {
		return mTX;
	}

	public int getSpeedRX() {
		return mSpeedRX;
	}

	public int getSpeedTX() {
		return mSpeedTX;
	}

	public double getLat() {
		return mLat;
	}

	public double getLon() {
		return mLon;
	}

	public float getAcc() {
		return mAcc;
	}

	public float getSpeed() {
		return mSpeed;
	}

	public void setTime(long time) {
		this.mTime = time;
	}

	public void setSlot(int slot) {
		this.mSlot = slot;
	}

	public void setLevel(int level) {
		this.mLevel = level;
	}

	public void setProgress(int progress) {
		this.mProgress = progress;
	}

	public void setNettype(int nettype) {
		this.mNettype = nettype;
	}

	public void setOperator(String operator) {
		this.mOperator = operator;
	}

	public void setOpercode(int opercode) {
		this.mOpercode = opercode;
	}

	public void setLAC(int lac) {
		this.mLAC = lac;
	}

	public void setCID(int cid) {
		this.mCID = cid;
	}

	public void setDataState(int datastate) {
		this.mDataState = datastate;
	}

	public void setRX(long rx) {
		this.mRX = rx;
	}

	public void setTX(long tx) {
		this.mTX = tx;
	}

	public void setSpeedRX(int srx) {
		this.mSpeedRX = srx;
	}

	public void setSpeedTX(int stx) {
		this.mSpeedTX = stx;
	}
	
	public void setLat(double lat) {
		this.mLat = lat;
	}

	public void setLon(double lon) {
		this.mLon = lon;
	}

	public void setAcc(float acc) {
		this.mAcc = acc;
	}

	public void setSpeed(float speed) {
		this.mSpeed = speed;
	}

	public void resetGsm() {
		mOpercode = 0;
		mOperator = "no name";
		mNettype = 0;
		mCID = 0;
		mLAC = 0;
		mLevel = 0;
		mProgress = 0;
		mDataState = 0;
	}

	public void resetGps() {
		mLat = 0.0;
		mLon = 0.0;
		mAcc = (float) 0.0;
		mSpeed = (float) 0.0;
	}
	
	public void setGps(Location loc) {
		mLat = loc.getLatitude();
		mLon = loc.getLongitude();
		mAcc = loc.getAccuracy();
		mSpeed = loc.getSpeed();
	}

	/**
	* Сохранить объект в jsоn
	* @throws JSONException 
	*/
	public JSONObject toJson() throws JSONException {
		JSONObject jsonObj = new JSONObject();
		
		jsonObj
			.put("time", mTime)
			.put("slot", mSlot)
			.put("level", mLevel)
			.put("progress", mProgress)
			.put("nettype", mNettype)
			.put("operator", mOperator)
			.put("LAC", mLAC)
			.put("CID", mCID)
			.put("latitude", mLat)
			.put("longitude", mLon)
			.put("gpsAccuracy", mAcc)
			.put("gpsSpeed", mSpeed);
		if(getDataState() == TelephonyManager.DATA_CONNECTED){
			jsonObj
				.put("datastate", mDataState)
				.put("speedRX", mSpeedRX)
				.put("speedTX", mSpeedTX);
		}
		
		return jsonObj;
	}

}

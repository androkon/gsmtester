package ru.konsoft.gsmtester;

import android.location.Location;

public class Info {

	private long mTime;
	
	private double mLat;
	private double mLon;
	private double mAcc;
	private float mSpeed;

	private int mSlot;

	private int mLevel;
	private int mProgress;
	private int mNettype;
	private String mOperator;
	private int mOpercode;
	private int mLAC;
	private int mCID;

	private int mDatastate;
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

	public void setTime(long time) {
		this.mTime = time;
	}

	public int getSlot() {
		return mSlot;
	}

	public void setSlot(int slot) {
		this.mSlot = slot;
	}

	public int getLevel() {
		return mLevel;
	}

	public void setLevel(int level) {
		this.mLevel = level;
	}

	public int getProgress() {
		return mProgress;
	}

	public void setProgress(int progress) {
		this.mProgress = progress;
	}

	public int getNettype() {
		return mNettype;
	}

	public void setNettype(int nettype) {
		this.mNettype = nettype;
	}

	public String getOperator() {
		return mOperator;
	}

	public void setOperator(String operator) {
		this.mOperator = operator;
	}

	public int getOpercode() {
		return mOpercode;
	}

	public void setOpercode(int opercode) {
		this.mOpercode = opercode;
	}

	public int getLAC() {
		return mLAC;
	}

	public void setLAC(int lac) {
		this.mLAC = lac;
	}

	public int getCID() {
		return mCID;
	}

	public void setCID(int cid) {
		this.mCID = cid;
	}

	public int getDatastate() {
		return mDatastate;
	}

	public void setDatastate(int datastate) {
		this.mDatastate = datastate;
	}

	public long getRX() {
		return mRX;
	}

	public void setRX(long rx) {
		this.mRX = rx;
	}

	public long getTX() {
		return mTX;
	}

	public void setTX(long tx) {
		this.mTX = tx;
	}

	public int getSpeedRX() {
		return mSpeedRX;
	}

	public void setSpeedRX(int srx) {
		this.mSpeedRX = srx;
	}

	public int getSpeedTX() {
		return mSpeedTX;
	}

	public void setSpeedTX(int stx) {
		this.mSpeedTX = stx;
	}
	
	public void resetGsm() {
		mOpercode = 0;
		mOperator = "no name";
		mNettype = 0;
		mCID = 0;
		mLAC = 0;
		mLevel = 0;
		mProgress = 0;
		mDatastate = 0;
	}

	public void resetGps() {
		mLat = 0.0;
		mLon = 0.0;
		mAcc = 0.0;
		mSpeed = 0;
	}
	
	public void setGps(Location loc) {
		mLat = loc.getLatitude();
		mLon = loc.getLongitude();
		mAcc = loc.getAccuracy();
		mSpeed = loc.getSpeed();
	}

	public double getLat() {
		return mLat;
	}

	public void setLat(double lat) {
		this.mLat = lat;
	}

	public double getLon() {
		return mLon;
	}

	public void setLon(double lon) {
		this.mLon = lon;
	}

	public double getAcc() {
		return mAcc;
	}

	public void setAcc(double acc) {
		this.mAcc = acc;
	}

	public float getSpeed() {
		return mSpeed;
	}

	public void setSpeed(float speed) {
		this.mSpeed = speed;
	}
}

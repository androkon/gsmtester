package ru.konsoft.gsmtester;

import android.telephony.TelephonyManager;
import android.telephony.*;

public class DuoTelephonyManager extends TelephonyManager
{
	
	public String getNetworkOperatorName(int sim)
	{
		// TODO: Implement this method
		return super.getNetworkOperatorName();
	}

	public int getNetworkType(int sim)
	{
		// TODO: Implement this method
		return super.getNetworkType();
	}

	public String getNetworkOperator(int sim)
	{
		// TODO: Implement this method
		return super.getNetworkOperator();
	}

	public CellLocation getCellLocation(int sim)
	{
		// TODO: Implement this method
		return super.getCellLocation();
	}
	
}

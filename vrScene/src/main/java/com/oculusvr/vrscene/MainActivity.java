/************************************************************************************

Filename    :   MainActivity.java
Content     :   
Created     :   
Authors     :   

Copyright   :   Copyright 2014 Oculus VR, LLC. All Rights reserved.

*************************************************************************************/
package com.oculusvr.vrscene;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.oculusvr.vrlib.VrActivity;
import com.oculusvr.vrlib.VrLib;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends VrActivity {
	public static final String TAG = "VrScene";

	private static final UUID FVR_UUID = UUID.fromString("AEDBD263-E6EC-467D-8461-746329DE6754");
	private BTManager mBTManager;
	private BluetoothDevice dTango;
	
	/** Load jni .so on initialization */
	static {
		Log.d( TAG, "LoadLibrary" );
		System.loadLibrary( "vrscene" );
	}

	public static native long nativeSetAppInterface( VrActivity act, String fromPackageNameString, String commandString, String uriString );
	public static native void nativeSetPosition( float x, float y, float z );

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d( TAG, "onCreate" );
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String commandString = VrLib.getCommandStringFromIntent( intent );
		String fromPackageNameString = VrLib.getPackageStringFromIntent( intent );
		String uriString = VrLib.getUriStringFromIntent( intent );

		appPtr = nativeSetAppInterface( this, fromPackageNameString, commandString, uriString );


		mBTManager = new BTManager(this, FVR_UUID, true, false);
		mBTManager.Enable();

		Set<BluetoothDevice> pairedDevices = mBTManager.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice d : pairedDevices) {
				Log.d(TAG, String.format("Bluetooth paired device[%s] address[%s] class[%s]", d.getName(), d.getAddress(), d.getBluetoothClass().toString()));
				if (d.getName().endsWith("Tango")) dTango = d;
			}
		}
		if (dTango != null) mBTManager.ConnectTo(dTango);

		//nativeSetPosition(20.0f,0.0f,0.0f);
	}

	@Override
	protected void onDestroy() {
		Log.d( TAG, "onDestroy" );
		super.onDestroy();
		mBTManager.Close();
	}
}

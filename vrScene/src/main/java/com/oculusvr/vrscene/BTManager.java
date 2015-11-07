package com.oculusvr.vrscene;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

public class BTManager {
	private static final String TAG = "BluetoothDataManager";

	private final BluetoothAdapter mBluetoothAdapter;

	private final Activity context;
	private UUID uuid;
	private boolean client = false;
	private boolean server = false;

	public BTManager(Activity context, UUID uuid, boolean client, boolean server) {
		this.context = context; this.uuid = uuid; this.client = client; this.server = server;

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) return;

		//System Broadcast Intents
		IntentFilter flt = new IntentFilter();
		flt.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		flt.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		flt.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		flt.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		flt.addAction(BluetoothDevice.ACTION_FOUND);
		//flt.addAction(BluetoothDevice.ACTION_UUID);
		context.registerReceiver(mReceiver, flt);
	}

	public static byte[] toByteArray(double value) {
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(value);
		return bytes;
	}
	public static double toDouble(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getDouble();
	}

	public int Enable() {
		if (mBluetoothAdapter == null) {
			Log.d(TAG, "Bluetooth NOT supported");
			return 0;
		}
		int rtn = 1;
		String status = "Bluetooth supported";
		if (!mBluetoothAdapter.isEnabled()) {
			BTRequestEnable();
		} else {
			if (server) BTServerThreadStart();
			status += " (already enabled)";
			rtn = 2;
		}
		Log.d(TAG, status);
		return rtn;
	}
	public void Scan() {
		if (mBluetoothAdapter.isEnabled()) {
			//TODO close any currently open bluetooth connections
			mBluetoothAdapter.startDiscovery();
		}
	}
	public int ConnectTo(String address) {
		if (mBluetoothAdapter == null) {
			Log.d(TAG, "Bluetooth NOT supported");
			return 0;
		}

		BluetoothDevice device = null;
		try {
			device = mBluetoothAdapter.getRemoteDevice(address);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "EXCEPTION " + e.getMessage());
			return 0;
		}

		BTClientThreadStart(device);
		return 1;
	}
	public void ConnectTo(BluetoothDevice device) {
		Log.d(TAG, "Connecting to " + device.getName());
		BTClientThreadStart(device);
	}
	public Set<BluetoothDevice> getBondedDevices() {
		if (mBluetoothAdapter == null) return null;
		return mBluetoothAdapter.getBondedDevices();
	}

	public void Close() {
		if (context != null) context.unregisterReceiver(mReceiver);
		if (BTSocket != null) BTSocket.cancel();
		if (BTServer != null) BTServer.cancel();
		if (BTClient != null) BTClient.cancel();
	}

	//Activity Intents
	static final int REQUEST_BT_ENABLE = 60198;
	private void BTRequestEnable() {
		Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		context.startActivityForResult(intent, REQUEST_BT_ENABLE);
	}
	public int onActivityResult(int requestCode, int resultCode, Intent data) {
		if (mBluetoothAdapter == null) return 0;
		int rtn = 0;
		//check which request we are responding to
		if (requestCode == REQUEST_BT_ENABLE) {
			if (resultCode == Activity.RESULT_CANCELED) {
				Log.d(TAG, "Bluetooth NOT enabled"); rtn = -1;
			} else {
				Log.d(TAG, "Bluetooth enabled");
				if (server) BTServerThreadStart();
				rtn = 1;
			}
		}
		return rtn;
	}


	//System Broadcast Intents
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				int state_prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR);

				//Log.d(TAG, String.format("Bluetooth state changed to [%d] prev[%d]", state, state_prev));
				if (state == BluetoothAdapter.STATE_ON) Log.d(TAG, "Bluetooth state = STATE_ON");
				if (state == BluetoothAdapter.STATE_OFF) Log.d(TAG, "Bluetooth state = STATE_OFF");
				if (state == BluetoothAdapter.STATE_TURNING_ON) Log.d(TAG, "Bluetooth state = STATE_TURNING_ON");
				if (state == BluetoothAdapter.STATE_TURNING_OFF) Log.d(TAG, "Bluetooth state = STATE_TURNING_OFF");
				if (state_prev == BluetoothAdapter.STATE_ON) Log.d(TAG, "Bluetooth state_prev = STATE_ON");
				if (state_prev == BluetoothAdapter.STATE_OFF) Log.d(TAG, "Bluetooth state_prev = STATE_OFF");
				if (state_prev == BluetoothAdapter.STATE_TURNING_ON) Log.d(TAG, "Bluetooth state_prev = STATE_TURNING_ON");
				if (state_prev == BluetoothAdapter.STATE_TURNING_OFF) Log.d(TAG, "Bluetooth state_prev = STATE_TURNING_OFF");

				if (state == BluetoothAdapter.STATE_ON && state_prev == BluetoothAdapter.STATE_TURNING_ON) {
					//BTListPairedHeadphones();
				}
			} else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
				int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
				int mode_prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1);

				//Log.d(TAG, String.format("Bluetooth scan mode changed to [%d] prev[%d]", mode, mode_prev));
				if (mode == BluetoothAdapter.SCAN_MODE_NONE) Log.d(TAG, "Bluetooth mode = SCAN_MODE_NONE");
				if (mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE) Log.d(TAG, "Bluetooth mode = SCAN_MODE_CONNECTABLE");
				if (mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) Log.d(TAG, "Bluetooth mode = SCAN_MODE_CONNECTABLE_DISCOVERABLE");
				if (mode_prev == BluetoothAdapter.SCAN_MODE_NONE) Log.d(TAG, "Bluetooth mode_prev = SCAN_MODE_NONE");
				if (mode_prev == BluetoothAdapter.SCAN_MODE_CONNECTABLE) Log.d(TAG, "Bluetooth mode_prev = SCAN_MODE_CONNECTABLE");
				if (mode_prev == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) Log.d(TAG, "Bluetooth mode_prev = SCAN_MODE_CONNECTABLE_DISCOVERABLE");

			}


			if (BluetoothDevice.ACTION_FOUND.equals(action)) { // When discovery finds a device
				BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				BluetoothClass c = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);

				Log.d(TAG, String.format("Bluetooth discovered device[%s] add[%s] class[%S]", d.getName(), d.getAddress(), c));
				//TODO if headset and not already paired, make a connection to pair the device
//				if (d.getName().toLowerCase().startsWith("streamz")) {
//					//TODX show detail after successful connection
//					//BTListPairedHeadphones();
//				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				Log.d(TAG, "Bluetooth ACTION_DISCOVERY_STARTED");
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				Log.d(TAG, "Bluetooth ACTION_DISCOVERY_FINISHED");

//			} else if (BluetoothDevice.ACTION_UUID.equals(action)) {
//				BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//				//ParcelUuid u = intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID);
//				//Log.d(TAG, String.format("     UUID[%S]", u.getUuid().toString()));
//
////				for (ParcelUuid id : d.getUuids()) {
////					Log.d(TAG, String.format("     UUID[%S]", id.getUuid().toString()));
////				}
//				Log.d(TAG, String.format("Bluetooth UUID device[%s] add[%s]", d.getName(), d.getAddress()));
			}
		}
	};


	//***server connect thread
	private void BTServerThreadStart() {
		if (BTServer != null) BTServer.cancel();
		BTServer = new BTServerThread();
		BTServer.start();
	}
	private static BTServerThread BTServer;
	private class BTServerThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;

		public BTServerThread() {
			// Use a temporary object that is later assigned to mmServerSocket, because mmServerSocket is final
			BluetoothServerSocket tmp = null;
			try {
				tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("BTServer", uuid); // uuid also used by the client code
			} catch (IOException e) {
				Log.e(TAG, "EXCEPTION " + e.getMessage());
			}
			mmServerSocket = tmp;
			Log.d(TAG, "Server Listening = " + mmServerSocket);
		}

		@Override
		public void run() {
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				try {
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "EXCEPTION " + e.getMessage());
					break;
				}
				// If a connection was accepted
				if (socket != null) {
					Log.d(TAG, String.format("Server got [%s]", socket.getRemoteDevice().getName()));

					BTSocketThreadStart(socket);
					break;
				}
			}
			cancel();
		}

		//cancel the listening socket and cause the thread to finish
		public void cancel() {
			try {
				if (mmServerSocket == null) return;
				mmServerSocket.close();
				Log.d(TAG, "Server Closed = " + mmServerSocket);
			} catch (IOException e) {
				Log.e(TAG, "EXCEPTION " + e.getMessage());
			}
		}
	}

	//***client connect thread
	private void BTClientThreadStart(BluetoothDevice device) {
		if (BTClient != null) BTClient.cancel();
		BTClient = new BTClientThread(device);
		BTClient.start();
	}
	private static BTClientThread BTClient;
	private class BTClientThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public BTClientThread(BluetoothDevice device) {
			mmDevice = device;

			// Use a temporary object that is later assigned to mmSocket, because mmSocket is final
			BluetoothSocket tmp = null;
			try {
				tmp = device.createInsecureRfcommSocketToServiceRecord(uuid); // uuid also used by the server code
			} catch (IOException e) {
				Log.e(TAG, "EXCEPTION " + e.getMessage());
			}
			mmSocket = tmp;

			Log.d(TAG, "Client created = " + mmSocket);
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();

			try {
				// connect to the device through the socket. This will block until it succeeds or throws an exception
				mmSocket.connect();

				Log.d(TAG, "Client connected = " + mmSocket);
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
				} catch (IOException e) {
					Log.e(TAG, "EXCEPTION " + e.getMessage());
				}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			BTSocketThreadStart(mmSocket);
			//cancel();
		}

		//will cancel an in-progress connection, and close the socket
		public void cancel() {
			try {
				if (mmSocket == null) return;
				mmSocket.close();
				Log.d(TAG, "Client Closed = " + mmSocket);
			} catch (IOException e) {
				Log.e(TAG, "EXCEPTION " + e.getMessage());
			}
		}
	}


	public static boolean socketReady = false;
	public static OutputStream mmOutStream;
	public static InputStream mmInStream;
	//***socket thread
	private void BTSocketThreadStart(BluetoothSocket socket) {
		if (BTSocket != null) BTSocket.cancel();
		BTSocket = new BTSocketThread(socket);
		BTSocket.start();
	}
	private static BTSocketThread BTSocket;
	private class BTSocketThread extends Thread {
		private final BluetoothSocket mmSocket;
		//private final InputStream mmInStream;
		//private final OutputStream mmOutStream;

		public BTSocketThread(BluetoothSocket socket) {
			Log.d(TAG, "BTSocketThread created");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "EXCEPTION " + e.getMessage());
			}
			mmInStream = tmpIn;
			mmOutStream = tmpOut;

			if (client) {
				String test = "s"; //start
				byte[] buffer = new byte[0];
				try {
					buffer = test.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, "EXCEPTION " + e.getMessage());
				}
				try {
					mmOutStream.write(buffer);
				} catch (IOException e) {
					Log.e(TAG, "EXCEPTION " + e.getMessage());
				}
			}
		}

		public void run() {
			Log.d(TAG, "BTSocketThread started");
			byte[] buffer = new byte[512];
			int bytes;
//			long totalBytes = 0;
//			long start = System.currentTimeMillis();

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					//Log.d(TAG, "Bytes read: " + Integer.toString(bytes));
					if (bytes < 12) continue;

					float x = ByteBuffer.wrap(buffer,bytes-12,4).getFloat();
					float y = ByteBuffer.wrap(buffer,bytes-8,4).getFloat();
					float z = ByteBuffer.wrap(buffer,bytes-4,4).getFloat();
					//Log.d(TAG, String.format("Position: x[%f] y[%f] z[%f]", x, y, z));
					MainActivity.nativeSetPosition(x, z, -y);

//					totalBytes += bytes;
//					if (totalBytes > 10000000) {
//					long totaltime = System.currentTimeMillis() - start;
//					if (totaltime > 40000) {
//						double bytespersec = (totalBytes/(double)(totaltime/1000.0d));
//						Log.d(TAG, "Total bytes read: " + Long.toString(totalBytes));
//						Log.d(TAG, "Speed (bytes/sec): " + Double.toString(bytespersec));
//						Log.d(TAG, "Latency (ms/pose): " + Double.toString(24.0d/(bytespersec/1000.0d)));
//						break;
//					}
					//socketReady = true;
					//TODO send somewhere


				} catch (IOException e) {
					Log.d(TAG, "Disconnected " + e.getMessage());
					//TODO Start the service over to restart listening mode
					break;
				}
			}
			cancel();
		}

		//write to the connected OutStream
		public void write(byte[] buffer) {
			try {
				if (mmOutStream != null) mmOutStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "EXCEPTION " + e.getMessage());
			}
		}

		public void cancel() {
			try {
				if (mmSocket == null) return;
				mmSocket.close();
				Log.d(TAG, "Socket Closed = " + mmSocket);
			} catch (IOException e) {
				Log.e(TAG, "EXCEPTION " + e.getMessage());
			}
		}
	}
}

//00000000-0000-1000-8000-00805F9B34FB Bluetooth base
//00001101-0000-1000-8000-00805F9B34FB serial port

//Streamz v1 UUIDs
//0000110A-0000-1000-8000-00805F9B34FB //A2DP (AudioSource)
//00001105-0000-1000-8000-00805F9B34FB //OPP (OBEXObjectPush)
//00001115-0000-1000-8000-00805F9B34FB //Personal Area Networking (PANU)
//00001116-0000-1000-8000-00805F9B34FB //Personal Area Networking (NAP)
//0000112F-0000-1000-8000-00805F9B34FB //Phonebook Access Profile (PSE)
//00001112-0000-1000-8000-00805F9B34FB //Headset Profile (Audio Gateway)
//0000111F-0000-1000-8000-00805F9B34FB //Hands-free Profile (HandsfreeAudioGateway)
//453994D5-D58B-96F9-6616-B37F586BA2EC //Persistent Publish/Subscribe service (PPS)
//936DA01F-9ABD-4D9D-80C7-02AF85C822A8 //Applink??

//Streamz v2 UUIDs

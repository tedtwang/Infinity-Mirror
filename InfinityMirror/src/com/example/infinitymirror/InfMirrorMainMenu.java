package com.example.infinitymirror;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class InfMirrorMainMenu extends Activity {
	private int mode = 0;
	private static final int REQUEST_ENABLE_BT = 10;
	private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); 
	// standard spp uuid
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;
	//private Set<BluetoothDevice> pairedDevices;
	private ConnectThread mConnectThread;
	private static String btDeviceAddress = "20:13:12:02:16:99";
							//MAC address of bluetooth receiver
	private boolean gotOutStream = false, 
					connectedBT = false;
	private Button onSwitch, offSwitch, autoButton, tempButton,connectButton;
	private SeekBar rseekBar, gseekBar, bseekBar;
	private TextView rval, gval, bval;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			connected();
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().hide();
		setContentView(R.layout.activity_inf_mirror_main_menu);
		addListenerOnAutoButton();
		addListenerOnTempButton();
		addListenerOnConnectButton();
		addListenerOnRedSeekBar();
		addListenerOnGreenSeekBar();
		addListenerOnBlueSeekBar();
		addListenerOnOnButton();
		addListenerOnOffButton();
		makeControlsInvisible();
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth, send a message and finish
			finish();
		}

		if (!mBluetoothAdapter.isEnabled()) {
			@SuppressWarnings("static-access")
			Intent enableBtIntent = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.inf_mirror_main_menu, menu);
		return true;
	}
	
	@Override
	public void onResume() {
		if (!mBluetoothAdapter.isEnabled()) {
			@SuppressWarnings("static-access")
			Intent enableBtIntent = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		if (!connectedBT) {
			makeControlsInvisible();
		}
		super.onResume();
	}
	
	@Override
	public void onPause() {
		gotOutStream = false;
		connectedBT = false;
		try {
			mConnectThread.cancel();
		} catch (NullPointerException e) {
			
		}
		mConnectThread = null;
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		connectedBT = false;
		try {
			btSocket.close();
		} catch (IOException e) {
			Log.d("Ted", "Failed to close socket");
		} catch (NullPointerException npe) {
			Log.d("Ted", "Failed to close socket during onDestroy");
		}
	}
	
	public void addListenerOnOnButton() {
		onSwitch = (Button) findViewById(R.id.onButton);
		onSwitch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// send message to bluetooth slave
				sendData("*A1~");
				onSwitch.setVisibility(View.INVISIBLE);
				offSwitch.setVisibility(View.VISIBLE);
			}
		});
	}
	
	public void addListenerOnOffButton() {
		offSwitch = (Button) findViewById(R.id.offButton);
		offSwitch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// send message to bluetooth slave
				sendData("*A0~");
				offSwitch.setVisibility(View.INVISIBLE);
				onSwitch.setVisibility(View.VISIBLE);
			}
		});
	}
	
	public void addListenerOnAutoButton() {
		autoButton = (Button) findViewById(R.id.autobutton);
		autoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// send message to bluetooth slave
				sendData("*D~");
				if (mode == 333) {
					mode = 0;
				}//paused
				else mode = 333;				
			}
		});
	}
	
	public void addListenerOnTempButton() {
		tempButton = (Button) findViewById(R.id.tempButton);
		tempButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// send message to bluetooth slave
				sendData("*C~");
				if (mode == 222) {
					mode = 0;
				}//paused
				else mode = 222;
			}
		});
	}
	
	public void addListenerOnConnectButton() {
		connectButton = (Button) findViewById(R.id.connectButton);
		connectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// send message to bluetooth slave
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(btDeviceAddress);
				while (!mBluetoothAdapter.isEnabled()) {}
				mConnectThread = new ConnectThread(device);
				mConnectThread.start();				
			}
		});
	}
	
	public void addListenerOnRedSeekBar() {
		rseekBar = (SeekBar) findViewById(R.id.RedSlider);
		rval = (TextView) findViewById(R.id.redValue);
		rseekBar.setMax(255);
		//Maximum analog value
		rseekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar sb, int value, boolean b) {
				sendData("*Br"+String.valueOf(value)+"~");
				//status code = '1', analog value, end byte = '~'
				rval.setText(String.valueOf(value));
				mode = 111;
			}

			@Override
			public void onStartTrackingTouch(SeekBar sb) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar sb) {
			}
		});
	}
	
	public void addListenerOnGreenSeekBar() {
		gseekBar = (SeekBar) findViewById(R.id.GreenSlider);
		gval = (TextView) findViewById(R.id.greenValue);
		gseekBar.setMax(255);
		//Maximum analog value
		gseekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar sb, int value, boolean b) {
				sendData("*Bg"+String.valueOf(value)+"~");
				//status code = '1', color indicator,analog value, end byte = '~'
				gval.setText(String.valueOf(value));
				mode = 111;
			}

			@Override
			public void onStartTrackingTouch(SeekBar sb) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar sb) {
				
			}
		});
	}
	
	public void addListenerOnBlueSeekBar() {
		bseekBar = (SeekBar) findViewById(R.id.BlueSlider);
		bval = (TextView) findViewById(R.id.blueValue);
		bseekBar.setMax(255);
		//Maximum analog value
		bseekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar sb, int value, boolean b) {
				sendData("*Bb"+String.valueOf(value)+"~");
				//status code = '1', analog value, end byte = '~'
				bval.setText(String.valueOf(value));
				mode = 111;
			}

			@Override
			public void onStartTrackingTouch(SeekBar sb) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar sb) {
				
			}
		});
	}
	
	public void makeControlsVisible() {
		connectButton.setVisibility(View.INVISIBLE);
		onSwitch.setVisibility(View.VISIBLE);
		autoButton.setVisibility(View.VISIBLE);
		tempButton.setVisibility(View.VISIBLE);
		rseekBar.setVisibility(View.VISIBLE);
		gseekBar.setVisibility(View.VISIBLE);
		bseekBar.setVisibility(View.VISIBLE);
		rval.setVisibility(View.VISIBLE);
		gval.setVisibility(View.VISIBLE);
		bval.setVisibility(View.VISIBLE);
	}
	
	public void makeControlsInvisible() {
		connectButton.setVisibility(View.VISIBLE);
		onSwitch.setVisibility(View.INVISIBLE);
		offSwitch.setVisibility(View.INVISIBLE);
		autoButton.setVisibility(View.INVISIBLE);
		tempButton.setVisibility(View.INVISIBLE);
		rseekBar.setVisibility(View.INVISIBLE);
		gseekBar.setVisibility(View.INVISIBLE);
		bseekBar.setVisibility(View.INVISIBLE);
		rval.setVisibility(View.INVISIBLE);
		gval.setVisibility(View.INVISIBLE);
		bval.setVisibility(View.INVISIBLE);
	}
	
	public void connected() {
		connectedBT = true;
		Toast.makeText(getBaseContext(), "Successfully connected to Device",
					Toast.LENGTH_LONG).show();
		makeControlsVisible();
	}

	private void sendData(String message) {
		if (!connectedBT) {
			return;
		}
		if (!gotOutStream) {
			getOutStream(btSocket);
		}
		Log.d("Ted", "Attempting to send message: " + message);
		byte[] msgBuffer = message.getBytes();

		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {
			Toast.makeText(getBaseContext(), "Error: Failed to write message",
					Toast.LENGTH_LONG).show();
		} catch (NullPointerException ne) {
			Toast.makeText(getBaseContext(), "Error: No output stream",
					Toast.LENGTH_LONG).show();
		}
		Log.d("Ted", "Write succcesful!");
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;

		public ConnectThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(uuid);
			} catch (IOException e) {
			}
			mmSocket = tmp;
			btSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();
			Log.d("Ted", "Attempting to connect...");
			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				Log.d("Ted", "Failed to connect");
				try {
					mmSocket.close();
				} catch (IOException closeException) {
				}
			}

			mHandler.sendMessage(new Message());
			Log.d("Ted", "Connection successful!");
			return;
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {			
			 try { 
				 mmSocket.close(); 
			 } catch (IOException e) { 
				 Log.d("Ted", "Failed to close socket"); 
			 }
		}
	}
	
	public void getOutStream(BluetoothSocket socket) {
        OutputStream tmpOut = null;
		Log.d("Ted", "In getOutStream..");
        // Get the BluetoothSocket input and output streams
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        	Toast.makeText(getBaseContext(), "Error: Could not get output stream",
					Toast.LENGTH_LONG).show();
        }

		Log.d("Ted", "Got output stream!");
        outStream = tmpOut; 
        gotOutStream = true;
	}

}

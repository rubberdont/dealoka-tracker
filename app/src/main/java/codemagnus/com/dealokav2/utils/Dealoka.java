package codemagnus.com.dealokav2.utils;

import android.app.Application;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;

import codemagnus.com.dealokav2.BaseTabActivity;
import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

public class Dealoka extends Application {

	public static final String TAG = "HaroldApp";
	
	private SocketIO socket = null;
	private static Dealoka instance = null;

    private BaseTabActivity activity;

    public BaseTabActivity getActivity() {
        return activity;
    }

    public void setActivity(BaseTabActivity activity) {
        this.activity = activity;
    }

    public Dealoka() {
		try {
			socket = new SocketIO("http://54.169.132.22:3001");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		if (socket != null) {
			socket.connect(new IOCallback() {
				@Override
				public void onMessage(JSONObject json, IOAcknowledge ack) {
					try {
						Log.d("socket-io", "Server said:" + json.toString(2));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onMessage(String data, IOAcknowledge ack) {
					Log.d("socket-io", "Server said: " + data);
				}

				@Override
				public void onError(SocketIOException socketIOException) {
					Log.d("socket-io", "an Error occured");
					socketIOException.printStackTrace();
				}

				@Override
				public void onDisconnect() {
					Log.d("socket-io", "Connection terminated.");
				}

				@Override
				public void onConnect() {
					Log.d("socket-io", "Connection established");
				}

				@Override
				public void on(String event, IOAcknowledge ack, Object... args) {
					Log.d("socket-io", "Server triggered event '" + event + "'");
				}

			});
		} else {
			Log.e(TAG, "socket is null");
		}
	}
	
	public static Dealoka getInstance() {
		if (instance == null) {
			instance = new Dealoka();
		}
		return instance;
	}
	
	public SocketIO getSocketIO() {
		return socket;
	}
	
}

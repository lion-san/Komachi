package com.fujitsu.jp.komoachi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * 「赤外線リモコンキット」のドライバ。
 * - 注：このクラスの AsyncTask は Activity のライフサイクルと同期しません。
 * Activity を持たないように。
 *
 */
public class IrrcUsbDriver implements UsbReceiver.Driver, RemoconConst {
	private static final String TAG = "IrrcUsbDriver";
	private static final long TIMEOUT = 1000;

	// 「赤外線リモコンキット」のデバイス関連定数
	private static final int VENDER_ID = 0x22ea;
	private static final int PRODUCT_ID = 0x001e;
	private static final int INTERFACE_INDEX = 3; // for F.W. Ver-2.1.0
	private static final int PACKET_SIZE = 64;
	private static final byte RECEIVE_IR_DATA_CMD = 0x52;
	private static final byte RECEIVE_IR_MODE_CMD = 0x53;
	private static final int SEND_IR_CMD = 0x61;

	private UsbManager usbManager;
	private UsbDevice usbDevice;
	private PendingIntent permissionIntent;
	private boolean isReady = false;

	private UsbDeviceConnection usbConnection;
	private UsbEndpoint endpointIn;
	private UsbEndpoint endpointOut;

    private static MainActivity activity;

	/**
	 * IRリモコンの応答を受け取るリスナ。
	 */
	public interface IrrcResponseListener {
		public void onIrrcResponse(byte[] data);
	}

	public static IrrcUsbDriver init(Activity activity, String permissionName) {
        activity = activity;
		IrrcUsbDriver driver = new IrrcUsbDriver(activity, permissionName);
		// USB_DEVICE_ATTACHEDから起動された場合は intent がデバイスを持っている。
		UsbDevice device = activity.getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (device == null) {
			// LAUNCHER からの起動の場合は接続済デバイス一覧から検索する。
			device = findDevice(driver.usbManager, VENDER_ID, PRODUCT_ID);
		}
		/*
		 * USB_DEVICE_ATTACHED で起動するように AndroidManifest.xml を記述すると
		 * USB_DEVICE_ATTACHED で必ず onCreate() が呼ばれるので Activity から設定した Receiver は呼ばれない。
		 * 従って、ここで onAttach() を呼ぶ。
		 */
		driver.onAttach(device);
		return driver;
	}

	public IrrcUsbDriver(Activity activity, String permissionName) {
		this.usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
		this.permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(permissionName), 0);
	}

	/**
	 * @return true=デバイスの確認。
	 */
	public boolean hasDevice() {
		return usbDevice != null;
	}

	/**
	 * @return true=デバイスの準備OK。
	 */
	public boolean isReady() {
		return isReady;
	}
	
	/**
	 * デバイスの検索。
	 * @return true=デバイスの準備OK。
	 */
	public boolean findDevice() {
		UsbDevice device = findDevice(usbManager, VENDER_ID, PRODUCT_ID);
		onAttach(device);
		return device != null;
	}

	/**
	 * デバイスの接続通知。
	 * 
	 * @return null=正常、null以外=エラーメッセージ
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public String onAttach(UsbDevice device) {
		Log.d(TAG, "onAttach:" + device);
		usbDevice = device;
		if (usbDevice == null) {
			Log.e(TAG, "Not found USB Device.");
			return "Not found USB Device.";
		}
		if (usbManager.hasPermission(usbDevice)) {
			return onStart(usbDevice);
		} else {
			// デバイスの利用許可をユーザに求める。
			// 結果は UsbReceiver.onReceive()にコールバック。
			usbManager.requestPermission(usbDevice, permissionIntent);
		}
		return null;
	}

	/**
	 * デバイスの利用開始。
	 * 
	 * @return null=正常、null以外=エラーメッセージ
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public String onStart(UsbDevice device) {
		Log.d(TAG, "onStart:" + device);
		if (!device.equals(usbDevice)) {
			return "No device attach.";
		}
		if (!usbManager.hasPermission(usbDevice)) {
			return "No device permission.";
		}

		usbConnection = usbManager.openDevice(usbDevice);
		// TODO:インターフェースの検出は端折ってます。
		UsbInterface usbIf = usbDevice.getInterface(INTERFACE_INDEX);

		// EndPointの検索。分かってる場合は直接取り出しても良い。
		for (int i = 0; i < usbIf.getEndpointCount(); i++) {
			UsbEndpoint ep = usbIf.getEndpoint(i);
			Log.d(TAG, "tye=" + ep.getType());
			if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
				if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
					endpointIn = ep;
				} else if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
					endpointOut = ep;
				}
			}
		}
		if (endpointIn == null || endpointOut == null) {
			Log.e(TAG, "Device has not IN/OUT Endpoint.");
			return "Device has not IN/OUT Endpoint.";
		}
		// デバイスの確保
		usbConnection.claimInterface(usbIf, true);
		isReady = true;
		return null;
	}

	/**
	 * デバイスの切断通知
	 * 
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public String onDetach(UsbDevice device) {
		Log.d(TAG, "onDetach:" + device);

		if (!device.equals(usbDevice)) {
			Log.d(TAG, "onDetach: Other device.");
			return "Other device";
		}

		if (usbConnection != null) {
			UsbInterface usbIf = usbDevice.getInterface(INTERFACE_INDEX);
			usbConnection.releaseInterface(usbIf);
			usbConnection.close();
		}
		usbConnection = null;
		usbDevice = null;
		isReady = false;
		return null;
	}

	/**
	 * リモコンの赤外線受信開始。
	 * 
	 * @param listener 応答リスナ。
	 */
	public RequestAsyncTask startReceiveIr(IrrcResponseListener listener) {
		byte[] buff = initBuffer(new byte[PACKET_SIZE], (byte) 0xff);
		buff[0] = RECEIVE_IR_MODE_CMD;
		buff[1] = 1;
		return new RequestAsyncTask().request(listener, buff, true, false, TIMEOUT);
	}

	/**
	 * リモコンの赤外線受信終了。
	 * 
	 * @param listener 応答リスナ。
	 */
	public RequestAsyncTask endReceiveIr(IrrcResponseListener listener) {
		byte[] buff = initBuffer(new byte[PACKET_SIZE], (byte) 0xff);
		buff[0] = RECEIVE_IR_MODE_CMD;
		buff[1] = 0;
		return new RequestAsyncTask().request(listener, buff, true, false, TIMEOUT);
	}

	/**
	 * リモコンの赤外線受信データ取得。
	 * - データが取れるまで戻らない。
	 * 
	 * @param listener 応答リスナ。
	 */
	public RequestAsyncTask getReceiveIrData(IrrcResponseListener listener, long timeout) {
		byte[] buff = initBuffer(new byte[PACKET_SIZE], (byte) 0xff);
		buff[0] = RECEIVE_IR_DATA_CMD;
		return new RequestAsyncTask().request(listener, buff, true, true, timeout);
	}

	/**
	 * 赤外線データ送信。
	 * 
	 * @param buff データ
	 */
	public RequestAsyncTask sendData(byte[] buff) {
		buff[0] = SEND_IR_CMD;
		return new RequestAsyncTask().request(null, buff, false, false, TIMEOUT);
	}

	/**
	 * デバイスとの通信処理。
	 * - 「赤外線リモコンキット」の通信は非同期なので UsbRequest を使用する必要がある。
	 */
	public class RequestAsyncTask extends AsyncTask<byte[], Void, byte[]> {
		private IrrcResponseListener listener;
		private boolean withResponse = false;
		private boolean withRetry = false;
		private UsbRequest currentUsbRequest = null;
		private long timeout = 0;
		private String errorMessage = null;

		public RequestAsyncTask() {
		}

		public RequestAsyncTask request(IrrcResponseListener listener, byte[] buff,
				boolean withResponse, boolean withRetry, long timeout) {
			this.listener = listener;
			this.withResponse = withResponse;
			this.withRetry = withRetry;
			this.timeout = timeout;
			execute(buff);
			return this;
		}

		@JavascriptInterface
		public String getErrorMessage() {
			return errorMessage;
		}

		@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        @JavascriptInterface
		public void cancel() {
			Log.d(TAG, "cancel");
			super.cancel(false);
			if (currentUsbRequest != null) {
				currentUsbRequest.cancel();
			}
			errorMessage = "cancel";
		}

		@Override
		protected byte[] doInBackground(byte[]... args) {
			Log.d(TAG, "RequestAsyncTask start");
			try {
				byte[] reqData = args[0];
				byte[] resData = null;
				boolean isRetry = false;
				long startTime = System.currentTimeMillis();
				do {
					long runningTime = System.currentTimeMillis() - startTime;
                    if (runningTime > timeout) {
                        cancel();
                        errorMessage = "timeout";
                    }
					if (isCancelled()) break;
					doRequest(reqData);
					if (withResponse) {
						resData = doResponse();
						if (resData[0] != reqData[0]) {
							errorMessage = "Bad resposne code " + resData[0];
							Log.e(TAG, errorMessage);
							return null;
						}
						if (withRetry && resData[1] == 0x00) {
							sleep(500);
							isRetry = true;
						} else {
							isRetry = false;
						}
					}
				} while (isRetry);
				return resData;
			} catch (Throwable t) {
				errorMessage = t.getMessage();
				Log.e(TAG, t.getMessage(), t);
				return null;
			}
		}

		@Override
		protected void onPostExecute(byte[] result) {
			if (listener != null) {
				listener.onIrrcResponse(result);
			}
		}

		@Override
		protected void onCancelled(byte[] result) {
			if (listener != null) {
				listener.onIrrcResponse(null);
			}
		}

		/**
		 * デバイスへパケット送信。
		 * 
		 * @param buff パケットデータ
		 * @throws java.io.IOException
		 */
		@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        private void doRequest(byte[] buff) throws IOException {
			Log.d(TAG, "request:" + dump(buff));

			ByteBuffer buffer = ByteBuffer.allocate(buff.length);
			UsbRequest request = new UsbRequest();
			this.currentUsbRequest = request;
			buffer.put(buff);

			request.initialize(usbConnection, endpointOut);
			request.queue(buffer, buff.length);

			UsbRequest finishReq;
			while ((finishReq = usbConnection.requestWait()) != request) {
				if (finishReq == null) throw new IOException("Request failed.");
				sleep(100);
			}
			request.close();
			this.currentUsbRequest = null;
		}

		/**
		 * デバイスからパケット受信。
		 * 
		 * @return パケットデータ
		 * @throws java.io.IOException
		 */
		@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        private byte[] doResponse() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(endpointIn.getMaxPacketSize());
			buffer.clear();
			UsbRequest request = new UsbRequest();
			this.currentUsbRequest = request;
			request.initialize(usbConnection, endpointIn);
			request.queue(buffer, endpointIn.getMaxPacketSize());

			UsbRequest finishReq;
			while ((finishReq = usbConnection.requestWait()) != request) {
				if (finishReq == null) throw new IOException("Request failed.");
				sleep(100);
			}

			// Note: OSバージョンにより flip() の必要性が異なる気がする...
			if (buffer.remaining() == 0) buffer.flip();

			byte[] buff = new byte[buffer.remaining()];
			buffer.get(buff);
			Log.d(TAG, "response:" + dump(buff));
			request.close();
			this.currentUsbRequest = null;
            activity.info("");
			return buff;
		}

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private static UsbDevice findDevice(UsbManager usbManager, int venderId, int productId) {
		HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while (deviceIterator.hasNext()) {
			UsbDevice d = deviceIterator.next();
			Log.d(TAG, "device=" + d);
			if (d.getVendorId() == venderId && d.getProductId() == productId) {
				return d;
			}
		}
		return null;
	}

	private static byte[] initBuffer(byte[] buff, byte data) {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = data;
		}
		return buff;
	}

	private static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
			// ignore.
		}
	}

	public static String dump(byte[] buff) {
		return dump(buff, 0, buff.length);
	}

	public static String dump(byte[] buff, int off, int len) {
		String str = "";
		for (int i = 0; i < len; i++) {
			str += " " + Integer.toHexString((int) buff[off + i] & 0x0ff);
		}
		return str;
	}

}
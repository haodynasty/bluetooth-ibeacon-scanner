package com.blakequ.blelibrary.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;

import com.blakequ.blelibrary.BuildConfig;
import com.blakequ.blelibrary.device.BluetoothCrashResolver;
import com.blakequ.blelibrary.device.BluetoothLeDevice;
import com.blakequ.blelibrary.device.beacon.BeaconType;
import com.blakequ.blelibrary.device.beacon.BeaconUtils;
import com.blakequ.blelibrary.device.beacon.ibeacon.IBeaconDevice;
import com.blakequ.blelibrary.logging.LogManager;
import com.blakequ.blelibrary.powersave.BackgroundPowerSaver;
import com.blakequ.blelibrary.scanner.CycledLeScanCallback;
import com.blakequ.blelibrary.scanner.CycledLeScanner;
import com.blakequ.blelibrary.scanner.DetectionTracker;
import com.blakequ.blelibrary.scanner.StartRMData;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import static android.app.PendingIntent.FLAG_ONE_SHOT;
import static android.app.PendingIntent.getBroadcast;

/**
 * Copyright (C) BlakeQu All Rights Reserved <blakequ@gmail.com>
 * <p/>
 * Licensed under the blakequ.com License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * author  : quhao <blakequ@gmail.com> <br>
 * date     : 2016/6/7 20:46 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description: scan service
 */
@TargetApi(5)
public class ScanService extends Service{
    private final static String TAG = "ScanService";
    /**
     * using for process to process communication(不同进程间通信)
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private BluetoothCrashResolver bluetoothCrashResolver;
    final Messenger mMessenger = new Messenger(new IncomingHandler(this));
    private ExecutorService mExecutor;
    private CycledLeScanner mCycledScanner;
    private boolean mBackgroundFlag = false;

    /**
     * Command to the service to display a message
     */
    public static final int MSG_START_RANGING = 2;
    public static final int MSG_STOP_RANGING = 3;
    public static final int MSG_START_MONITORING = 4;
    public static final int MSG_STOP_MONITORING = 5;
    public static final int MSG_SET_SCAN_PERIODS = 6;

    static class IncomingHandler extends Handler {
        private final WeakReference<ScanService> mService;

        IncomingHandler(ScanService service) {
            mService = new WeakReference<ScanService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            ScanService service = mService.get();
            StartRMData startRMData = (StartRMData) msg.obj;

            if (service != null) {
                switch (msg.what) {
                    case MSG_START_RANGING:
                        LogManager.i(TAG, "start ranging received");
                        service.startRangingBeaconsInRegion(startRMData.getRegionData(), new org.altbeacon.beacon.service.Callback(startRMData.getCallbackPackageName()));
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_STOP_RANGING:
                        LogManager.i(TAG, "stop ranging received");
                        service.stopRangingBeaconsInRegion(startRMData.getRegionData());
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_START_MONITORING:
                        LogManager.i(TAG, "start monitoring received");
                        service.startMonitoringBeaconsInRegion(startRMData.getRegionData(), new org.altbeacon.beacon.service.Callback(startRMData.getCallbackPackageName()));
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_STOP_MONITORING:
                        LogManager.i(TAG, "stop monitoring received");
                        service.stopMonitoringBeaconsInRegion(startRMData.getRegionData());
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_SET_SCAN_PERIODS:
                        LogManager.i(TAG, "set scan intervals received");
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        }
    }

    /**
     * using for same process communication(用于相同进程通信)
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class ScanBinder extends Binder {
        public ScanService getService() {
            LogManager.i(TAG, "getService of BeaconBinder called");
            // Return this instance of LocalService so clients can call public methods
            return ScanService.this;
        }
    }

    @Override
    public void onCreate() {
        LogManager.i(TAG, "beaconService version %s is starting up", BuildConfig.VERSION_NAME);
        bluetoothCrashResolver = new BluetoothCrashResolver(this);
        bluetoothCrashResolver.start();
        // Create a private executor so we don't compete with threads used by AsyncTask
        // This uses fewer threads than the default executor so it won't hog CPU
        mExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

        mCycledScanner = CycledLeScanner.createScanner(this, BackgroundPowerSaver.DEFAULT_FOREGROUND_SCAN_PERIOD,
                BackgroundPowerSaver.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD, mBackgroundFlag, mCycledLeScanCallback, bluetoothCrashResolver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogManager.i(TAG, "binding");
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogManager.i(TAG, "unbinding");
        return false;
    }

    @Override
    public void onDestroy() {
        LogManager.e(TAG, "onDestroy()");
        if (Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.");
            return;
        }
        bluetoothCrashResolver.stop();
        LogManager.i(TAG, "onDestroy called.  stopping scanning");
        mCycledScanner.stop();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        LogManager.d(TAG, "task removed");
        if (Build.VERSION.RELEASE.contains("4.4.1") ||
                Build.VERSION.RELEASE.contains("4.4.2") ||
                Build.VERSION.RELEASE.contains("4.4.3")) {
            AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, getRestartIntent());
            LogManager.d(TAG, "Setting a wakeup alarm to go off due to Android 4.4.2 service restarting bug.");
        }
    }

    private PendingIntent getRestartIntent() {
        Intent restartIntent = new Intent();
        restartIntent.setClassName(getApplicationContext(), StartupBroadcastReceiver.class.getName());
        return getBroadcast(getApplicationContext(), 1, restartIntent, FLAG_ONE_SHOT);
    }


    /**
     * set scan params
     * @param scanPeriod
     * @param betweenScanPeriod
     * @param backgroundFlag
     */
    public void setScanPeriods(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        mCycledScanner.setScanPeriods(scanPeriod, betweenScanPeriod, backgroundFlag);
    }

    /**
     * methods for clients
     */
    public void startRangingBeaconsInRegion(Region region, Callback callback) {

        mCycledScanner.start();
    }

    public void stopRangingBeaconsInRegion(Region region) {

        mCycledScanner.stop();
    }

    public void startMonitoringBeaconsInRegion(Region region, Callback callback) {
        LogManager.d(TAG, "startMonitoring called");
        mCycledScanner.start();
    }

    public void stopMonitoringBeaconsInRegion(Region region) {
        LogManager.d(TAG, "stopMonitoring called");
        mCycledScanner.stop();
    }

    private class ScanData {
        public ScanData(BluetoothDevice device, int rssi, byte[] scanRecord) {
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
        }

        int rssi;
        BluetoothDevice device;
        byte[] scanRecord;
    }


    /**
     * callback class
     */
    protected final CycledLeScanCallback mCycledLeScanCallback = new CycledLeScanCallback(){

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            try {
                new ScanProcessor().executeOnExecutor(mExecutor,
                        new ScanData(device, rssi, scanRecord));
            } catch (RejectedExecutionException e) {
                LogManager.w(TAG, "Ignoring scan result because we cannot keep up.");
            }
        }

        @Override
        public void onScanEnd() {
//            monitoringStatus.updateNewlyOutside();
        }
    };

    private class ScanProcessor extends AsyncTask<ScanData, Void, Void> {
        final DetectionTracker mDetectionTracker = DetectionTracker.getInstance();

        @Override
        protected Void doInBackground(ScanData... params) {
            ScanData scanData = params[0];
            BluetoothLeDevice deviceLe = new BluetoothLeDevice(scanData.device, scanData.rssi, scanData.scanRecord, System.currentTimeMillis());
            final boolean isIBeacon = BeaconUtils.getBeaconType(deviceLe) == BeaconType.IBEACON;
            if (isIBeacon){
                IBeaconDevice iBeaconDevice = new IBeaconDevice(deviceLe);
            }

            //下面过滤设备
            return null;
        }
    }
}

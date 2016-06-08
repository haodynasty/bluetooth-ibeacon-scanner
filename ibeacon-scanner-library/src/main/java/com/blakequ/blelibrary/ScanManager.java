package com.blakequ.blelibrary;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.blakequ.blelibrary.bluetoothcompat.ScanFilterCompat;
import com.blakequ.blelibrary.logging.LogManager;
import com.blakequ.blelibrary.powersave.BackgroundPowerSaver;
import com.blakequ.blelibrary.scanner.StartRMData;
import com.blakequ.blelibrary.service.MonitorNotifier;
import com.blakequ.blelibrary.service.RangeNotifier;
import com.blakequ.blelibrary.service.Region;
import com.blakequ.blelibrary.service.ScanService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
 * date     : 2016/6/7 20:42 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 * A class used to set up interaction with beacons from an <code>Activity</code> or <code>Service</code>.
 *
 * <pre><code>
 * 1. private ScanManger scanManager = ScanManger.getInstance(this);
 * 2. start scan
 *    scanManager.setRangeNotifier(new RangeNotifier() {
 *              {@literal @}Override
 *              public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
 *                  if (beacons.size() > 0) {
 *                      Log.i(TAG, "The first beacon I see is about "+beacons.iterator().next().getDistance()+" meters away.");
 *                  }
 *              }
 *          });
 *
 *          try {
 *              scanManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
 *          } catch (RemoteException e) {
 *              e.printStackTrace();
 *          }
 *
 * 3. when you stop scan or quit, you can invoke scanManager.release();
 * <code/><pre/>
 */
public class ScanManager {

    private final static String TAG = "ScanManager";
    private static ScanManager INSTANCE = null;
    private Context mContext;
    private Messenger serviceMessenger = null;
    private final List<ScanFilterCompat> scanFilterCompats = new CopyOnWriteArrayList<>();
    private final ArrayList<Region> monitoredRegions = new ArrayList<Region>();
    private final ArrayList<Region> rangedRegions = new ArrayList<Region>();
    protected RangeNotifier rangeNotifier = null;
    protected MonitorNotifier monitorNotifier = null;
    private BackgroundPowerSaver mPowerSaver;
    private ConsumerInfo consumerInfo;

    /**
     * The default duration in milliseconds of region exit time
     */
    public static final long DEFAULT_EXIT_PERIOD = 10000L;
    private static long sExitRegionPeriod = DEFAULT_EXIT_PERIOD;
    private boolean mBackgroundMode = false;
    private boolean serviceConnected = false;
    private static boolean sAndroidLScanningDisabled = false;
    //pause stop scan device
    private static boolean isPauseStopScan = false;

    private ScanManager(Context context){
        this.mContext = context;
        verifyServiceDeclaration();
        mPowerSaver = new BackgroundPowerSaver(context);
    }

    public static ScanManager getInstance(Context context){
        if (INSTANCE == null){
            LogManager.d(TAG, "ScanManager instance creation");
            INSTANCE = new ScanManager(context);
        }
        return INSTANCE;
    }

    /**
     * get a list of ScanFilterCompat
     * @return
     */
    public List<ScanFilterCompat> getScanFilter() {
        return scanFilterCompats;
    }

    /**
     * Specifies a class that should be called each time the <code>BeaconService</code> gets ranging
     * data, which is nominally once per second when beacons are detected.
     * <p/>
     * IMPORTANT:  Only one RangeNotifier may be active for a given application.  If two different
     * activities or services set different RangeNotifier instances, the last one set will receive
     * all the notifications.
     *
     * @param notifier
     * @see RangeNotifier
     * @see #startRangingBeaconsInRegion(Region)
     */
    public void setRangeNotifier(RangeNotifier notifier) {
        rangeNotifier = notifier;
    }

    /**
     * Specifies a class that should be called each time the <code>BeaconService</code> sees
     * or stops seeing a Region of beacons.
     * <p/>
     * IMPORTANT:  Only one MonitorNotifier may be active for a given application.  If two different
     * activities or services set different MonitorNotifier instances, the last one set will receive
     * all the notifications.
     *
     * @param notifier
     * @see MonitorNotifier
     * @see #startMonitoringBeaconsInRegion(Region)
     * @see Region
     */
    public void setMonitorNotifier(MonitorNotifier notifier) {
        monitorNotifier = notifier;
    }

    public MonitorNotifier getMonitorNotifier() {
        return monitorNotifier;
    }

    public RangeNotifier getRangeNotifier() {
        return rangeNotifier;
    }

    /**
     * bind service
     */
    private void bind() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }

        if (consumerInfo == null) {
            consumerInfo = new ConsumerInfo();
            LogManager.d(TAG, "This consumer is not bound.  binding: %s", consumerInfo);
            Intent intent = new Intent(mContext.getApplicationContext(), ScanService.class);
            mContext.bindService(intent, consumerInfo.beaconServiceConnection, Context.BIND_AUTO_CREATE);
        }else {
            LogManager.d(TAG, "This consumer is already bound");
        }
    }

    /**
     * release resource and unbind service, stop scan
     */
    public void realse() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (consumerInfo != null) {
            try {
                for (Region region : rangedRegions) {
                    stopRangingBeaconsInRegion(region);
                }
                for (Region region : monitoredRegions){
                    stopMonitoringBeaconsInRegion(region);
                }
            } catch (RemoteException e) {
                LogManager.e(e, TAG, "Can't stop bootstrap regions");
            }
            LogManager.d(TAG, "Unbinding");
            mContext.unbindService(consumerInfo.beaconServiceConnection);
            // If this is the last consumer to disconnect, the service will exit
            // release the serviceMessenger.
            serviceMessenger = null;
            // Reset the mBackgroundMode to false, which is the default value
            // This way when we restart ranging or monitoring it will always be in
            // foreground mode
            mBackgroundMode = false;
        }
    }

    /**
     * Updates an already running scan with scanPeriod/betweenScanPeriod according to Background/Foreground state.
     * Change will take effect on the start of the next scan cycle.
     *
     * @throws RemoteException - If the ScanManager is not bound to the service.
     */
    @TargetApi(18)
    public void updateScanPeriods() throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            bind();
            return;
//            throw new RemoteException("The ScanManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, ScanService.MSG_SET_SCAN_PERIODS, 0, 0);
        LogManager.d(TAG, "updating background flag to %s", mBackgroundMode);
        LogManager.d(TAG, "updating scan period to %s, %s", mPowerSaver.getScanPeriod(), mPowerSaver.getBetweenScanPeriod());
        StartRMData obj = new StartRMData(mPowerSaver.getScanPeriod(), mPowerSaver.getBetweenScanPeriod(), this.mBackgroundMode);
        msg.obj = obj;
        serviceMessenger.send(msg);
    }


    /**
     * Tells the <code>ScanService</code> to start looking for beacons that match the passed
     * <code>Region</code> object, and providing updates on the estimated mDistance every seconds while
     * beacons in the Region are visible.  Note that the Region's unique identifier must be retained to
     * later call the stopRangingBeaconsInRegion method.
     *
     * @param region
     * @see ScanManager#stopRangingBeaconsInRegion(Region)
     * @see RangeNotifier
     * @see Region
     */
    @TargetApi(18)
    public void startRangingBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (region == null){
            LogManager.w(TAG, "region is null, Method invocation will be ignored");
            return;
        }
        synchronized (rangedRegions) {
            rangedRegions.add(region);
        }

        if (serviceMessenger == null) {
            bind();
            return;
//            throw new RemoteException("The ScanManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, ScanService.MSG_START_RANGING, 0, 0);
        StartRMData obj = new StartRMData(region, callbackPackageName(), mPowerSaver.getScanPeriod(), mPowerSaver.getBetweenScanPeriod(), this.mBackgroundMode);
        msg.obj = obj;
        serviceMessenger.send(msg);
    }

    /**
     * Tells the <code>ScanService</code> to stop looking for beacons that match the passed
     * <code>Region</code> object and providing mDistance information for them.
     *
     * @param region
     * @see #startRangingBeaconsInRegion(Region)
     * @see MonitorNotifier
     * @see Region
     */
    @TargetApi(18)
    public void stopRangingBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (region == null){
            LogManager.w(TAG, "region is null, Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The ScanManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, ScanService.MSG_STOP_RANGING, 0, 0);
        StartRMData obj = new StartRMData(region, callbackPackageName(), mPowerSaver.getScanPeriod(), mPowerSaver.getBetweenScanPeriod(), this.mBackgroundMode);
        msg.obj = obj;
        serviceMessenger.send(msg);
        synchronized (rangedRegions) {
            Region regionToRemove = null;
            for (Region rangedRegion : rangedRegions) {
                if (region.getUniqueId().equals(rangedRegion.getUniqueId())) {
                    regionToRemove = rangedRegion;
                }
            }
            rangedRegions.remove(regionToRemove);
        }
    }

    /**
     * Tells the <code>ScanService</code> to start looking for beacons that match the passed
     * <code>Region</code> object.  Note that the Region's unique identifier must be retained to
     * later call the stopMonitoringBeaconsInRegion method.
     *
     * Specifies a class that should be called each time the <code>ScanService</code> sees
     * or stops seeing a Region of beacons.
     *
     * @param region
     * @see ScanManager#stopMonitoringBeaconsInRegion(Region region)
     * @see MonitorNotifier
     * @see Region
     */
    @TargetApi(18)
    public void startMonitoringBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (region == null){
            LogManager.w(TAG, "region is null, Method invocation will be ignored");
            return;
        }
        synchronized (monitoredRegions) {
            monitoredRegions.add(region);
        }
        if (serviceMessenger == null) {
            bind();
            return;
//            throw new RemoteException("The ScanManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, ScanService.MSG_START_MONITORING, 0, 0);
        StartRMData obj = new StartRMData(region, callbackPackageName(), mPowerSaver.getScanPeriod(), mPowerSaver.getBetweenScanPeriod(), this.mBackgroundMode);
        msg.obj = obj;
        serviceMessenger.send(msg);
    }

    /**
     * Tells the <code>ScanService</code> to stop looking for beacons that match the passed
     * <code>Region</code> object.  Note that the Region's unique identifier is used to match it to
     * an existing monitored Region.
     *
     * @param region
     * @see ScanManager#startMonitoringBeaconsInRegion(Region)
     * @see MonitorNotifier
     * @see Region
     */
    @TargetApi(18)
    public void stopMonitoringBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (region == null){
            LogManager.w(TAG, "region is null, Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The ScanManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, ScanService.MSG_STOP_MONITORING, 0, 0);
        StartRMData obj = new StartRMData(region, callbackPackageName(), mPowerSaver.getScanPeriod(), mPowerSaver.getBetweenScanPeriod(), this.mBackgroundMode);
        msg.obj = obj;
        serviceMessenger.send(msg);
        synchronized (monitoredRegions) {
            Region regionToRemove = null;
            for (Region monitoredRegion : monitoredRegions) {
                if (region.getUniqueId().equals(monitoredRegion.getUniqueId())) {
                    regionToRemove = monitoredRegion;
                }
            }
            monitoredRegions.remove(regionToRemove);
        }
    }

    /**
     * @return the list of regions currently being monitored
     */
    public Collection<Region> getMonitoredRegions() {
        synchronized(this.monitoredRegions) {
            return new ArrayList<Region>(this.monitoredRegions);
        }
    }

    /**
     * @return the list of regions currently being ranged
     */
    public Collection<Region> getRangedRegions() {
        synchronized(this.rangedRegions) {
            return new ArrayList<Region>(this.rangedRegions);
        }
    }

    /**
     * This method notifies the beacon service that the application is either moving to background
     * mode or foreground mode.  When in background mode, BluetoothLE scans to look for beacons are
     * executed less frequently in order to save battery life. The specific scan rates for
     * background and foreground operation are set by the defaults below, but may be customized.
     * When ranging in the background, the time between updates will be much less frequent than in
     * the foreground.  Updates will come every time interval equal to the sum total of the
     * BackgroundScanPeriod and the BackgroundBetweenScanPeriod.
     *
     * @param backgroundMode true indicates the app is in the background
     * @see BackgroundPowerSaver#DEFAULT_FOREGROUND_SCAN_PERIOD
     * @see BackgroundPowerSaver#DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
     * @see BackgroundPowerSaver#DEFAULT_BACKGROUND_SCAN_PERIOD;
     * @see BackgroundPowerSaver#DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;
     * @see BackgroundPowerSaver#setForegroundScanPeriod(long p)
     * @see BackgroundPowerSaver#setForegroundBetweenScanPeriod(long p)
     * @see BackgroundPowerSaver#setBackgroundScanPeriod(long p)
     * @see BackgroundPowerSaver#setBackgroundBetweenScanPeriod(long p)
     */
    public void setBackgroundMode(boolean backgroundMode) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
        }
        if (backgroundMode != mBackgroundMode) {
            mBackgroundMode = backgroundMode;
            try {
                this.updateScanPeriods();
            } catch (RemoteException e) {
                LogManager.e(TAG, "Cannot contact service to set scan periods");
            }
        }
    }

    /**
     * is scan background mode
     * @return
     */
    public boolean isBackgroundMode() {
        return mBackgroundMode;
    }

    private String callbackPackageName() {
        String packageName = mContext.getPackageName();
        LogManager.d(TAG, "callback packageName: %s", packageName);
        return packageName;
    }

    /**
     * Set region exit period in milliseconds
     *
     * @param regionExitPeriod
     */
    public static void setRegionExitPeriod(long regionExitPeriod){
        sExitRegionPeriod = regionExitPeriod;
    }

    /**
     * Get region exit milliseconds
     *
     * @return exit region period in milliseconds
     */
    public static long getRegionExitPeriod(){
        return sExitRegionPeriod;
    }

    private void verifyServiceDeclaration() {
        final PackageManager packageManager = mContext.getPackageManager();
        final Intent intent = new Intent(mContext, ScanService.class);
        List resolveInfo =
                packageManager.queryIntentServices(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo.size() == 0) {
            throw new ServiceNotDeclaredException();
        }
    }

    private class ConsumerInfo {
        public boolean isConnected = false;
        public BeaconServiceConnection beaconServiceConnection;

        public ConsumerInfo() {
            this.isConnected = false;
            this.beaconServiceConnection= new BeaconServiceConnection();
        }
    }

    private class BeaconServiceConnection implements ServiceConnection {
        private BeaconServiceConnection() {
        }

        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogManager.d(TAG, "we have a connection to the service now");
            serviceConnected = true;
            serviceMessenger = new Messenger(service);
            try {
                for (Region region : rangedRegions){
                    startRangingBeaconsInRegion(region);
                }
                for (Region region : monitoredRegions){
                    startMonitoringBeaconsInRegion(region);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        // Called when the connection with the service disconnects
        public void onServiceDisconnected(ComponentName className) {
            LogManager.e(TAG, "onServiceDisconnected");
            serviceMessenger = null;
            serviceConnected = false;
        }
    }


    /**
     * Check if Bluetooth LE is supported by this Android device, and if so, make sure it is enabled.
     *
     * @return false if it is supported and not enabled
     * @throws BleNotAvailableException if Bluetooth LE is not supported.  (Note: The Android emulator will do this)
     */
    @TargetApi(18)
    public boolean checkAvailability() throws BleNotAvailableException {
        if (isSDKAvailable()) {
            if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                throw new BleNotAvailableException("Bluetooth LE not supported by this device");
            } else {
                if (((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().isEnabled()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSDKAvailable(){
        if (android.os.Build.VERSION.SDK_INT < 18) {
            throw new BleNotAvailableException("Bluetooth LE not supported by this device");
        }
        return true;
    }

    /**
     * Determines if Android L Scanning is disabled by user selection, so will use old scan method
     *
     * @return
     */
    public static boolean isAndroidLScanningDisabled() {
        return sAndroidLScanningDisabled;
    }

    /**
     * Allows disabling use of Android L BLE Scanning APIs on devices with API 21+
     * If set to false (default), devices with API 21+ will use the Android L APIs to
     * scan for beacons
     *
     * @param disabled
     */
    public static void setAndroidLScanningDisabled(boolean disabled) {
        sAndroidLScanningDisabled = disabled;
    }

    /**
     * is open pause scan switch
     * @return
     */
    public static boolean isPauseStopScan() {
        return isPauseStopScan;
    }

    /**
     * pause stop scan,
     * for example: if you connect device and need not scan other device, so you can use this switch
     * @return
     */
    public static void setIsPauseStopScan(boolean isPauseStopScan) {
        ScanManager.isPauseStopScan = isPauseStopScan;
    }
}

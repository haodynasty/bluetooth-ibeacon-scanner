package com.blakequ.blelibrary.scanner;

import android.bluetooth.BluetoothDevice;

/**
 * Created by dyoung on 10/6/14.
 */
public interface CycledLeScanCallback {
    /**
     * 扫描ble设备
     * @param device
     * @param rssi
     * @param scanRecord
     */
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);

    /**
     * 扫描结束时回调
     */
    public void onScanEnd();
}

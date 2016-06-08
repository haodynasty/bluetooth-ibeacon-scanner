/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blakequ.blelibrary.service;

import android.os.Parcel;
import android.os.Parcelable;

import com.blakequ.blelibrary.device.beacon.ibeacon.IBeaconDevice;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * This class represents a criteria of fields used to match beacons.
 *
 * The uniqueId field is used to distinguish this Region in the system.  When you set up
 * monitoring or ranging based on a Region and later want to stop monitoring or ranging,
 * you must do so by passing a Region object that has the same uniqueId field value.  If it
 * doesn't match, you can't cancel the operation.  There is no other purpose to this field.
 *
 * which indicates that they are a wildcard and will match any value.
 *
 * @author dyoung
 *
 */
public class Region implements Parcelable, Serializable {
    private static final String TAG = "Region";
    private static final Pattern MAC_PATTERN = Pattern.compile("^[0-9A-Fa-f]{2}\\:[0-9A-Fa-f]{2}\\:[0-9A-Fa-f]{2}\\:[0-9A-Fa-f]{2}\\:[0-9A-Fa-f]{2}\\:[0-9A-Fa-f]{2}$");

    /**
     * Required to make class Parcelable
     */
    public static final Creator<Region> CREATOR
            = new Creator<Region>() {
        public Region createFromParcel(Parcel in) {
            return new Region(in);
        }

        public Region[] newArray(int size) {
            return new Region[size];
        }
    };
    protected final String mBluetoothAddress;
    protected final String mUniqueId;


    /**
     * Constructs a new Region object to be used for Ranging or Monitoring
     * @param uniqueId - A unique identifier used to later cancel Ranging and Monitoring, or change the region being Ranged/Monitored
     */
    public Region(String uniqueId) {
       this(uniqueId, null);
    }

    /**
     * Constructs a new Region object to be used for Ranging or Monitoring
     * @param uniqueId - A unique identifier used to later cancel Ranging and Monitoring, or change the region being Ranged/Monitored
     * @param bluetoothAddress - mac address
     */
    public Region(String uniqueId, String bluetoothAddress) {
        validateMac(bluetoothAddress);
        this.mUniqueId = uniqueId;
        this.mBluetoothAddress = bluetoothAddress;
        if (uniqueId == null) {
            throw new NullPointerException("uniqueId may not be null");
        }
    }


    /**
     * Returns the identifier used to start or stop ranging/monitoring this region when calling
     * the <code>BeaconManager</code> methods.
     * @return
     */
    public String getUniqueId() {
        return mUniqueId;
    }

    /**
     * Returns the mac address used to filter for beacons
     */
    public String getBluetoothAddress() { return mBluetoothAddress; }

    /**
     * Checks to see if an Beacon object is included in the matching criteria of this Region
     * @param beacon the beacon to check to see if it is in the Region
     * @return true if is covered
     */
    public boolean matchesBeacon(IBeaconDevice beacon) {
        if (mBluetoothAddress != null && !mBluetoothAddress.equalsIgnoreCase(beacon.getAddress())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.mUniqueId.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Region) {
            return ((Region)other).mUniqueId.equals(this.mUniqueId);
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        sb.append("mUniqueId:");
        sb.append(mUniqueId);
        sb.append("mBluetoothAddress:");
        sb.append(mBluetoothAddress);
        return sb.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mUniqueId);
        out.writeString(mBluetoothAddress);
    }


    protected Region(Parcel in) {
        mUniqueId = in.readString();
        mBluetoothAddress = in.readString();
    }

    private void validateMac(String mac) throws IllegalArgumentException {
        if (mac != null) {
            if(!MAC_PATTERN.matcher(mac).matches()) {
                throw new IllegalArgumentException("Invalid mac address: '"+mac+"' Must be 6 hex bytes separated by colons.");
            }
        }
    }

    /**
     * Returns a clone of this instance.
     * @deprecated instances of this class are immutable and therefore don't have to be cloned when
     * used in concurrent code.
     * @return a new instance of this class with the same uniqueId and identifiers
     */
    @Override
    @Deprecated
    public Region clone() {
        return new Region(mUniqueId, mBluetoothAddress);
    }
}

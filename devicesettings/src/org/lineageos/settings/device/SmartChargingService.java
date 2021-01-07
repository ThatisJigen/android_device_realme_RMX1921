/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.lineageos.settings.device;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import androidx.preference.PreferenceManager;

public class SmartChargingService extends Service {

    private static boolean Debug = false;

    private boolean mconnectionInfoReceiver;

    private static boolean resetBatteryStats = false;

    public static String mmi_charging_enable = "/sys/class/power_supply/battery/mmi_charging_enable";

    public static String battery_capacity = "/sys/class/power_supply/battery/capacity";

    public static String battery_temperature = "/sys/class/power_supply/battery/temp";

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        IntentFilter connectionInfo = new IntentFilter();
                     connectionInfo.addAction(Intent.ACTION_POWER_CONNECTED);
                     connectionInfo.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(mconnectionInfo, connectionInfo);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mconnectionInfo);
        if (mconnectionInfoReceiver) getApplicationContext().unregisterReceiver(mBatteryInfo);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public BroadcastReceiver mconnectionInfo = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int battCap = Integer.parseInt(Utils.readLine(battery_capacity));
            if (intent.getAction() == Intent.ACTION_POWER_CONNECTED) {
                if (!mconnectionInfoReceiver) {
                    IntentFilter batteryInfo = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    context.getApplicationContext().registerReceiver(mBatteryInfo, batteryInfo);
                    mconnectionInfoReceiver = true;
                }
            Log.d("DeviceSettings", "Charger/USB Connected");
            } else if (intent.getAction() == Intent.ACTION_POWER_DISCONNECTED) {
                if (mconnectionInfoReceiver) {
                    context.getApplicationContext().unregisterReceiver(mBatteryInfo);
                    mconnectionInfoReceiver = false;
                }
            Log.d("DeviceSettings", "Charger/USB Disconnected");
            }
        }
    };

    public BroadcastReceiver mBatteryInfo = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float battTemp = ((float) Integer.parseInt(Utils.readLine(battery_temperature))) / 10;
            int battCap = Integer.parseInt(Utils.readLine(battery_capacity));
            int chargingLimit = Integer.parseInt(Utils.readLine(mmi_charging_enable));
            if (Debug) Log.d("DeviceSettings", "Battery Temperature: " + battTemp + ", Battery Capacity: " +battCap +"%" );

            // Charging limit based on user selected battery percentage 
            if (((SeekBarPreference.getProgress() == battCap) || (SeekBarPreference.getProgress() < battCap)) && chargingLimit != 0) {
                Utils.writeValue(mmi_charging_enable, "0");
                Log.d("DeviceSettings", "Battery Temperature: " + battTemp + ", Battery Capacity: " +battCap+"%, " +"User selected charging limit: "+SeekBarPreference.getProgress()+"% . Stopped charging");
            }
            else if (SeekBarPreference.getProgress() > battCap && chargingLimit != 1) {
                Utils.writeValue(mmi_charging_enable, "1");
                Log.d("DeviceSettings", "Charging...");
            }
        }
    };
}

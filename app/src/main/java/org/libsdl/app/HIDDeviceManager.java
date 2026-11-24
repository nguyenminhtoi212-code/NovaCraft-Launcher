/*
 * This file is part of SDL3 android-project java code.
 * Licensed under the zlib license: https://www.libsdl.org/license.php
 */

package org.libsdl.app;

import androidx.annotation.Keep;
import androidx.core.content.ContextCompat;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Keep
@android.annotation.SuppressLint("MissingPermission")
public class HIDDeviceManager {
    private static final String TAG = "hidapi";
    private static final String ACTION_USB_PERMISSION = "org.libsdl.app.USB_PERMISSION";

    private static HIDDeviceManager sManager;
    private static int sManagerRefCount = 0;

    static public HIDDeviceManager acquire(Context context) {
        if (sManagerRefCount == 0) {
            sManager = new HIDDeviceManager(context);
        }
        ++sManagerRefCount;
        return sManager;
    }

    static public void release(HIDDeviceManager manager) {
        if (manager == sManager) {
            --sManagerRefCount;
            if (sManagerRefCount == 0) {
                sManager.close();
                sManager = null;
            }
        }
    }

    private Context mContext;
    private HashMap<Integer, HIDDevice> mDevicesById = new HashMap<Integer, HIDDevice>();
    private HashMap<BluetoothDevice, HIDDeviceBLESteamController> mBluetoothDevices = new HashMap<BluetoothDevice, HIDDeviceBLESteamController>();
    private int mNextDeviceId = 0;
    private SharedPreferences mSharedPreferences = null;
    private boolean mIsChromebook = false;
    private UsbManager mUsbManager;
    private Handler mHandler;
    private BluetoothManager mBluetoothManager;
    private List<BluetoothDevice> mLastBluetoothDevices;

    private final BroadcastReceiver mUsbBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent != null ? intent.getAction() : null;
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (usbDevice != null) {
                    handleUsbDeviceAttached(usbDevice);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (usbDevice != null) {
                    handleUsbDeviceDetached(usbDevice);
                }
            } else if (HIDDeviceManager.ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (usbDevice != null) {
                    handleUsbDevicePermission(usbDevice, intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));
                }
            }
        }
    };

    private final BroadcastReceiver mBluetoothBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent != null ? intent.getAction() : null;
            // Bluetooth device was connected. If it was a Steam Controller, handle it
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Bluetooth device connected: " + device);

                if (isSteamController(device)) {
                    connectBluetoothDevice(device);
                }
            }

            // Bluetooth device was disconnected, remove from controller manager (if any)
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Bluetooth device disconnected: " + device);

                if (device != null) {
                    disconnectBluetoothDevice(device);
                }
            }
        }
    };

    private HIDDeviceManager(final Context context) {
        mContext = context;

        HIDDeviceRegisterCallback();

        mSharedPreferences = mContext.getSharedPreferences("hidapi", Context.MODE_PRIVATE);
        mIsChromebook = SDLActivity.isChromebook();

//        if (shouldClear) {
//            SharedPreferences.Editor spedit = mSharedPreferences.edit();
//            spedit.clear();
//            spedit.apply();
//        }
//        else
        {
            mNextDeviceId = mSharedPreferences.getInt("next_device_id", 0);
        }
    }

    Context getContext() {
        return mContext;
    }

    int getDeviceIDForIdentifier(String identifier) {
        SharedPreferences.Editor spedit = mSharedPreferences.edit();

        int result = mSharedPreferences.getInt(identifier, 0);
        if (result == 0) {
            result = mNextDeviceId++;
            spedit.putInt("next_device_id", mNextDeviceId);
        }

        spedit.putInt(identifier, result);
        spedit.apply();
        return result;
    }

    private void initializeUSB() {
        mUsbManager = (UsbManager)mContext.getSystemService(Context.USB_SERVICE);
        if (mUsbManager == null) {
            return;
        }

        /*
        // Logging
        for (UsbDevice device : mUsbManager.getDeviceList().values()) {
            Log.i(TAG,"Path: " + device.getDeviceName());
            Log.i(TAG,"Manufacturer: " + device.getManufacturerName());
            Log.i(TAG,"Product: " + device.getProductName());
            Log.i(TAG,"ID: " + device.getDeviceId());
            Log.i(TAG,"Class: " + device.getDeviceClass());
            Log.i(TAG,"Protocol: " + device.getDeviceProtocol());
            Log.i(TAG,"Vendor ID " + device.getVendorId());
            Log.i(TAG,"Product ID: " + device.getProductId());
            Log.i(TAG,"Interface count: " + device.getInterfaceCount());
            Log.i(TAG,"---------------------------------------");

            // Get interface details
            for (int index = 0; index < device.getInterfaceCount(); index++) {
                UsbInterface mUsbInterface = device.getInterface(index);
                Log.i(TAG,"  *****     *****");
                Log.i(TAG,"  Interface index: " + index);
                Log.i(TAG,"  Interface ID: " + mUsbInterface.getId());
                Log.i(TAG,"  Interface class: " + mUsbInterface.getInterfaceClass());
                Log.i(TAG,"  Interface subclass: " + mUsbInterface.getInterfaceSubclass());
                Log.i(TAG,"  Interface protocol: " + mUsbInterface.getInterfaceProtocol());
                Log.i(TAG,"  Endpoint count: " + mUsbInterface.getEndpointCount());

                // Get endpoint details
                for (int epi = 0; epi < mUsbInterface.getEndpointCount(); epi++)
                {
                    UsbEndpoint mEndpoint = mUsbInterface.getEndpoint(epi);
                    Log.i(TAG,"    ++++   ++++   ++++");
                    Log.i(TAG,"    Endpoint index: " + epi);
                    Log.i(TAG,"    Attributes: " + mEndpoint.getAttributes());
                    Log.i(TAG,"    Direction: " + mEndpoint.getDirection());
                    Log.i(TAG,"    Number: " + mEndpoint.getEndpointNumber());
                    Log.i(TAG,"    Interval: " + mEndpoint.getInterval());
                    Log.i(TAG,"    Packet size: " + mEndpoint.getMaxPacketSize());
                    Log.i(TAG,"    Type: " + mEndpoint.getType());
                }
            }
        }
        Log.i(TAG," No more devices connected.");
        */

        // Register for USB broadcasts and permission completions
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(HIDDeviceManager.ACTION_USB_PERMISSION);
        ContextCompat.registerReceiver(mContext, mUsbBroadcast, filter, ContextCompat.RECEIVER_EXPORTED);

        for (UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
            handleUsbDeviceAttached(usbDevice);
        }
    }

    UsbManager getUSBManager() {
        return mUsbManager;
    }

    private void shutdownUSB() {
        try {
            mContext.unregisterReceiver(mUsbBroadcast);
        } catch (Exception e) {
            // We may not have registered, that's okay
        }
    }

    private boolean isHIDDeviceInterface(UsbDevice usbDevice, UsbInterface usbInterface) {
        if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_HID) {
            return true;
        }
        if (isXbox360Controller(usbDevice, usbInterface) || isXboxOneController(usbDevice, usbInterface)) {
            return true;
        }
        return false;
    }

    private boolean isXbox360Controller(UsbDevice usbDevice, UsbInterface usbInterface) {
        final int XB360_IFACE_SUBCLASS = 93;
        final int XB360_IFACE_PROTOCOL = 1; // Wired
        final int XB360W_IFACE_PROTOCOL = 129; // Wireless
        final int[] SUPPORTED_VENDORS = {
            0x0079, // GPD Win 2
            0x044f, // Thrustmaster
            0x045e, // Microsoft
            0x046d, // Logitech
            0x056e, // Elecom
            0x06a3, // Saitek
            0x0738, // Mad Catz
            0x07ff, // Mad Catz
            0x0e6f, // PDP
            0x0f0d, // Hori
            0x1038, // SteelSeries
            0x11c9, // Nacon
            0x12ab, // Unknown
            0x1430, // RedOctane
            0x146b, // BigBen
            0x1532, // Razer Sabertooth
            0x15e4, // Numark
            0x162e, // Joytech
            0x1689, // Razer Onza
            0x1949, // Lab126, Inc.
            0x1bad, // Harmonix
            0x20d6, // PowerA
            0x24c6, // PowerA
            0x2c22, // Qanba
            0x2dc8, // 8BitDo
            0x3537, // GameSir
            0x37d7, // Flydigi
            0x9886, // ASTRO Gaming
        };

        if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC &&
            usbInterface.getInterfaceSubclass() == XB360_IFACE_SUBCLASS &&
            (usbInterface.getInterfaceProtocol() == XB360_IFACE_PROTOCOL ||
             usbInterface.getInterfaceProtocol() == XB360W_IFACE_PROTOCOL)) {
            int vendor_id = usbDevice.getVendorId();
            for (int supportedVid : SUPPORTED_VENDORS) {
                if (vendor_id == supportedVid) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isXboxOneController(UsbDevice usbDevice, UsbInterface usbInterface) {
        final int XB1_IFACE_SUBCLASS = 71;
        final int XB1_IFACE_PROTOCOL = 208;
        final int[] SUPPORTED_VENDORS = {
            0x03f0, // HP
            0x044f, // Thrustmaster
            0x045e, // Microsoft
            0x0738, // Mad Catz
            0x0b05, // ASUS
            0x0e6f, // PDP
            0x0f0d, // Hori
            0x10f5, // Turtle Beach
            0x1532, // Razer Wildcat
            0x20d6, // PowerA
            0x24c6, // PowerA
            0x294b, // Snakebyte
            0x2dc8, // 8BitDo
            0x2e24, // Hyperkin
            0x2e95, // SCUF
            0x3285, // Nacon
            0x3537, // GameSir
            0x366c, // ByoWave
        };

        if (usbInterface.getId() == 0 &&
            usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC &&
            usbInterface.getInterfaceSubclass() == XB1_IFACE_SUBCLASS &&
            usbInterface.getInterfaceProtocol() == XB1_IFACE_PROTOCOL) {
            int vendor_id = usbDevice.getVendorId();
            for (int supportedVid : SUPPORTED_VENDORS) {
                if (vendor_id == supportedVid) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleUsbDeviceAttached(UsbDevice usbDevice) {
        connectHIDDeviceUSB(usbDevice);
    }

    private void handleUsbDeviceDetached(UsbDevice usbDevice) {
        List<Integer> devices = new ArrayList<Integer>();
        for (HIDDevice device : mDevicesById.values()) {
            if (usbDevice.equals(device.getDevice())) {
                devices.add(device.getId());
            }
        }
        for (int id : devices) {
            HIDDevice device = mDevicesById.get(id);
            mDevicesById.remove(id);
            if (device != null) {
                device.shutdown();
            }
            HIDDeviceDisconnected(id);
        }
    }

    private void handleUsbDevicePermission(UsbDevice usbDevice, boolean permission_granted) {
        for (HIDDevice device : mDevicesById.values()) {
            if (usbDevice.equals(device.getDevice())) {
                boolean opened = false;
                if (permission_granted) {
                    opened = device.open();
                }
                HIDDeviceOpenResult(device.getId(), opened);
            }
        }
    }

    private void connectHIDDeviceUSB(UsbDevice usbDevice) {
        synchronized (this) {
            int interface_mask = 0;
            for (int interface_index = 0; interface_index < usbDevice.getInterfaceCount(); interface_index++) {
                UsbInterface usbInterface = usbDevice.getInterface(interface_index);
                if (isHIDDeviceInterface(usbDevice, usbInterface)) {
                    // Check to see if we've already added this interface
                    // This happens with the Xbox Series X controller which has a duplicate interface 0, which is inactive
                    int interface_id = usbInterface.getId();
                    if ((interface_mask & (1 << interface_id)) != 0) {
                        continue;
                    }
                    interface_mask |= (1 << interface_id);

                    HIDDeviceUSB device = new HIDDeviceUSB(this, usbDevice, interface_index);
                    int id = device.getId();
                    mDevicesById.put(id, device);
                    HIDDeviceConnected(id, device.getIdentifier(), device.getVendorId(), device.getProductId(), device.getSerialNumber(), device.getVersion(), device.getManufacturerName(), device.getProductName(), usbInterface.getId(), usbInterface.getInterfaceClass(), usbInterface.getInterfaceSubclass(), usbInterface.getInterfaceProtocol(), false, 0);
                }
            }
        }
    }

    private void initializeBluetooth() {
        Log.d(TAG, "Initializing Bluetooth");

        if (Build.VERSION.SDK_INT >= 31 /* Android 12  */ &&
            mContext.getPackageManager().checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT, mContext.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Couldn't initialize Bluetooth, missing android.permission.BLUETOOTH_CONNECT");
            return;
        }

        if (Build.VERSION.SDK_INT <= 30 /* Android 11.0 (R) */ &&
            mContext.getPackageManager().checkPermission(android.Manifest.permission.BLUETOOTH, mContext.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Couldn't initialize Bluetooth, missing android.permission.BLUETOOTH");
            return;
        }

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d(TAG, "Couldn't initialize Bluetooth, this version of Android does not support Bluetooth LE");
            return;
        }

        // Find bonded bluetooth controllers and create SteamControllers for them
        mBluetoothManager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            // This device doesn't support Bluetooth.
            return;
        }

        BluetoothAdapter btAdapter = mBluetoothManager.getAdapter();
        if (btAdapter == null) {
            // This device has Bluetooth support in the codebase, but has no available adapters.
            return;
        }

        // Get our bonded devices.
        if (hasBluetoothConnectPermission()) {
            try {
                for (BluetoothDevice device : btAdapter.getBondedDevices()) {

                    Log.d(TAG, "Bluetooth device available: " + device);
                    if (isSteamController(device)) {
                        connectBluetoothDevice(device);
                    }

                }
            } catch (SecurityException e) {
                Log.w(TAG, "Failed to query bonded Bluetooth devices", e);
            }
        }

        // NOTE: These don't work on Chromebooks, to my undying dismay.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        ContextCompat.registerReceiver(mContext, mBluetoothBroadcast, filter, ContextCompat.RECEIVER_EXPORTED);

        if (mIsChromebook) {
            mHandler = new Handler(Looper.getMainLooper());
            mLastBluetoothDevices = new ArrayList<BluetoothDevice>();

            // final HIDDeviceManager finalThis = this;
            // mHandler.postDelayed(new Runnable() {
            //     @Override
            //     public void run() {
            //         finalThis.chromebookConnectionHandler();
            //     }
            // }, 5000);
        }
    }

    private boolean hasBluetoothConnectPermission() {
        if (mContext == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 31 /* Android 12 */) {
            return ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
    }

    private void shutdownBluetooth() {
        try {
            mContext.unregisterReceiver(mBluetoothBroadcast);
        } catch (Exception e) {
            // We may not have registered, that's okay
        }
    }

    // Chromebooks do not pass along ACTION_ACL_CONNECTED / ACTION_ACL_DISCONNECTED properly.
    // This function provides a sort of dummy version of that, watching for changes in the
    // connected devices and attempting to add controllers as things change.
    void chromebookConnectionHandler() {
        if (!mIsChromebook || mBluetoothManager == null || !hasBluetoothConnectPermission()) {
            return;
        }

        ArrayList<BluetoothDevice> disconnected = new ArrayList<BluetoothDevice>();
        ArrayList<BluetoothDevice> connected = new ArrayList<BluetoothDevice>();

        List<BluetoothDevice> currentConnected;
        try {
            currentConnected = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        } catch (SecurityException e) {
            Log.w(TAG, "Failed to query connected GATT devices", e);
            return;
        }

        for (BluetoothDevice bluetoothDevice : currentConnected) {
            if (!mLastBluetoothDevices.contains(bluetoothDevice)) {
                connected.add(bluetoothDevice);
            }
        }
        for (BluetoothDevice bluetoothDevice : mLastBluetoothDevices) {
            if (!currentConnected.contains(bluetoothDevice)) {
                disconnected.add(bluetoothDevice);
            }
        }

        mLastBluetoothDevices = currentConnected;

        for (BluetoothDevice bluetoothDevice : disconnected) {
            disconnectBluetoothDevice(bluetoothDevice);
        }
        for (BluetoothDevice bluetoothDevice : connected) {
            connectBluetoothDevice(bluetoothDevice);
        }

        final HIDDeviceManager finalThis = this;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finalThis.chromebookConnectionHandler();
            }
        }, 10000);
    }

    boolean connectBluetoothDevice(BluetoothDevice bluetoothDevice) {
        Log.v(TAG, "connectBluetoothDevice device=" + bluetoothDevice);
        synchronized (this) {
            if (mBluetoothDevices.containsKey(bluetoothDevice)) {
                Log.v(TAG, "Steam controller with address " + bluetoothDevice + " already exists, attempting reconnect");

                HIDDeviceBLESteamController device = mBluetoothDevices.get(bluetoothDevice);
                if (device != null) {
                    device.reconnect();
                }

                return false;
            }
            HIDDeviceBLESteamController device = new HIDDeviceBLESteamController(this, bluetoothDevice);
            int id = device.getId();
            mBluetoothDevices.put(bluetoothDevice, device);
            mDevicesById.put(id, device);

            // The Steam Controller will mark itself connected once initialization is complete
        }
        return true;
    }

    void disconnectBluetoothDevice(BluetoothDevice bluetoothDevice) {
        synchronized (this) {
            HIDDeviceBLESteamController device = mBluetoothDevices.get(bluetoothDevice);
            if (device == null)
                return;

            int id = device.getId();
            mBluetoothDevices.remove(bluetoothDevice);
            mDevicesById.remove(id);
            device.shutdown();
            HIDDeviceDisconnected(id);
        }
    }

    boolean isSteamController(BluetoothDevice bluetoothDevice) {
        // Sanity check.  If you pass in a null device, by definition it is never a Steam Controller.
        if (bluetoothDevice == null || !hasBluetoothConnectPermission()) {
            return false;
        }

        try {
            // If the device has no local name, we really don't want to try an equality check against it.
            String name = bluetoothDevice.getName();
            if (name == null) {
                return false;
            }

            // Steam Controllers will always support Bluetooth Low Energy
            if ((bluetoothDevice.getType() & BluetoothDevice.DEVICE_TYPE_LE) == 0) {
                return false;
            }

            // Match on the name either the original Steam Controller or the new second-generation one advertise with.
            return name.equals("SteamController") || name.startsWith("Steam Ctrl");
        } catch (SecurityException e) {
            return false;
        }
    }

    private void close() {
        shutdownUSB();
        shutdownBluetooth();
        synchronized (this) {
            for (HIDDevice device : mDevicesById.values()) {
                device.shutdown();
            }
            mDevicesById.clear();
            mBluetoothDevices.clear();
            HIDDeviceReleaseCallback();
        }
    }

    public void setFrozen(boolean frozen) {
        synchronized (this) {
            for (HIDDevice device : mDevicesById.values()) {
                device.setFrozen(frozen);
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////

    private HIDDevice getDevice(int id) {
        synchronized (this) {
            HIDDevice result = mDevicesById.get(id);
            if (result == null) {
                Log.v(TAG, "No device for id: " + id);
                Log.v(TAG, "Available devices: " + mDevicesById.keySet());
            }
            return result;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////// JNI interface functions
    //////////////////////////////////////////////////////////////////////////////////////////////////////

    boolean initialize(boolean usb, boolean bluetooth) {
        Log.v(TAG, "initialize(" + usb + ", " + bluetooth + ")");

        if (usb) {
            initializeUSB();
        }
        if (bluetooth) {
            initializeBluetooth();
        }
        return true;
    }

    boolean openDevice(int deviceID) {
        Log.v(TAG, "openDevice deviceID=" + deviceID);
        HIDDevice device = getDevice(deviceID);
        if (device == null) {
            HIDDeviceDisconnected(deviceID);
            return false;
        }

        // Look to see if this is a USB device and we have permission to access it
        UsbDevice usbDevice = device.getDevice();
        if (usbDevice != null && mUsbManager != null && !mUsbManager.hasPermission(usbDevice)) {
            HIDDeviceOpenPending(deviceID);
            try {
                final int FLAG_MUTABLE = 0x02000000; // PendingIntent.FLAG_MUTABLE, but don't require SDK 31
                int flags;
                if (Build.VERSION.SDK_INT >= 31 /* Android 12.0 (S) */) {
                    flags = FLAG_MUTABLE;
                } else {
                    flags = 0;
                }

                Intent intent = new Intent(HIDDeviceManager.ACTION_USB_PERMISSION);
                intent.setPackage(mContext.getPackageName());
                mUsbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(mContext, 0, intent, flags));
            } catch (Exception e) {
                Log.v(TAG, "Couldn't request permission for USB device " + usbDevice);
                HIDDeviceOpenResult(deviceID, false);
            }
            return false;
        }

        try {
            return device.open();
        } catch (Exception e) {
            Log.e(TAG, "Got exception: " + Log.getStackTraceString(e));
        }
        return false;
    }

    int writeReport(int deviceID, byte[] report, boolean feature) {
        try {
            //Log.v(TAG, "writeReport deviceID=" + deviceID + " length=" + report.length);
            HIDDevice device;
            device = getDevice(deviceID);
            if (device == null) {
                HIDDeviceDisconnected(deviceID);
                return -1;
            }

            return device.writeReport(report, feature);
        } catch (Exception e) {
            Log.e(TAG, "Got exception: " + Log.getStackTraceString(e));
        }
        return -1;
    }

    boolean readReport(int deviceID, byte[] report, boolean feature) {
        try {
            //Log.v(TAG, "readReport deviceID=" + deviceID);
            HIDDevice device;
            device = getDevice(deviceID);
            if (device == null) {
                HIDDeviceDisconnected(deviceID);
                return false;
            }

            return device.readReport(report, feature);
        } catch (Exception e) {
            Log.e(TAG, "Got exception: " + Log.getStackTraceString(e));
        }
        return false;
    }

    void closeDevice(int deviceID) {
        try {
            Log.v(TAG, "closeDevice deviceID=" + deviceID);
            HIDDevice device;
            device = getDevice(deviceID);
            if (device == null) {
                HIDDeviceDisconnected(deviceID);
                return;
            }

            device.close();
        } catch (Exception e) {
            Log.e(TAG, "Got exception: " + Log.getStackTraceString(e));
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////// Native methods
    //////////////////////////////////////////////////////////////////////////////////////////////////////

    private native void HIDDeviceRegisterCallback();
    private native void HIDDeviceReleaseCallback();

    native void HIDDeviceConnected(int deviceID, String identifier, int vendorId, int productId, String serial_number, int release_number, String manufacturer_string, String product_string, int interface_number, int interface_class, int interface_subclass, int interface_protocol, boolean bBluetooth, int reportID);
    native void HIDDeviceOpenPending(int deviceID);
    native void HIDDeviceOpenResult(int deviceID, boolean opened);
    native void HIDDeviceDisconnected(int deviceID);

    native void HIDDeviceInputReport(int deviceID, byte[] report);
    native void HIDDeviceReportResponse(int deviceID, byte[] report);
}

interface HIDDevice {
    int getId();
    int getVendorId();
    int getProductId();
    String getSerialNumber();
    int getVersion();
    String getManufacturerName();
    String getProductName();
    UsbDevice getDevice();
    boolean open();
    int writeReport(byte[] report, boolean feature);
    boolean readReport(byte[] report, boolean feature);
    void setFrozen(boolean frozen);
    void close();
    void shutdown();
}

@Keep
class HIDDeviceUSB implements HIDDevice {

    private static final String TAG = "hidapi";

    protected HIDDeviceManager mManager;
    protected UsbDevice mDevice;
    protected int mInterfaceIndex;
    protected int mInterface;
    protected int mDeviceId;
    protected UsbDeviceConnection mConnection;
    protected UsbEndpoint mInputEndpoint;
    protected UsbEndpoint mOutputEndpoint;
    protected InputThread mInputThread;
    protected boolean mRunning;
    protected boolean mFrozen;
    protected boolean mClaimed;

    public HIDDeviceUSB(HIDDeviceManager manager, UsbDevice usbDevice, int interface_index) {
        mManager = manager;
        mDevice = usbDevice;
        mInterfaceIndex = interface_index;
        mInterface = mDevice.getInterface(mInterfaceIndex).getId();
        mDeviceId = manager.getDeviceIDForIdentifier(getIdentifier());
        mRunning = false;
        mClaimed = false;
    }

    String getIdentifier() {
        return String.format(Locale.ENGLISH, "%s/%x/%x/%d", mDevice.getDeviceName(), mDevice.getVendorId(), mDevice.getProductId(), mInterfaceIndex);
    }

    @Override
    public int getId() {
        return mDeviceId;
    }

    @Override
    public int getVendorId() {
        return mDevice.getVendorId();
    }

    @Override
    public int getProductId() {
        return mDevice.getProductId();
    }

    @Override
    public String getSerialNumber() {
        String result = null;
        try {
            result = mDevice.getSerialNumber();
        } catch (Exception exception) {
            // Permission may restrict serial lookup.
        }
        if (result == null) {
            result = "";
        }
        return result;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public String getManufacturerName() {
        String result = mDevice.getManufacturerName();
        if (result == null) {
            result = String.format(Locale.ENGLISH, "%x", getVendorId());
        }
        return result;
    }

    @Override
    public String getProductName() {
        String result = mDevice.getProductName();
        if (result == null) {
            result = String.format(Locale.ENGLISH, "%x", getProductId());
        }
        return result;
    }

    @Override
    public UsbDevice getDevice() {
        return mDevice;
    }

    String getDeviceName() {
        return getManufacturerName() + " " + getProductName() + "(0x" + String.format(Locale.ENGLISH, "%x", getVendorId()) + "/0x" + String.format(Locale.ENGLISH, "%x", getProductId()) + ")";
    }

    @Override
    public boolean open() {
        UsbManager usbManager = mManager != null ? mManager.getUSBManager() : null;
        if (usbManager == null) {
            return false;
        }
        mConnection = usbManager.openDevice(mDevice);
        if (mConnection == null) {
            Log.w(TAG, "Unable to open USB device " + getDeviceName());
            return false;
        }

        UsbInterface iface = mDevice.getInterface(mInterfaceIndex);
        if (!mConnection.claimInterface(iface, true)) {
            Log.w(TAG, "Failed to claim interfaces on USB device " + getDeviceName());
            close();
            return false;
        }
        mClaimed = true;

        for (int j = 0; j < iface.getEndpointCount(); j++) {
            UsbEndpoint endpt = iface.getEndpoint(j);
            switch (endpt.getDirection()) {
                case UsbConstants.USB_DIR_IN:
                    if (mInputEndpoint == null) {
                        mInputEndpoint = endpt;
                    }
                    break;
                case UsbConstants.USB_DIR_OUT:
                    if (mOutputEndpoint == null) {
                        mOutputEndpoint = endpt;
                    }
                    break;
            }
        }

        if (mInputEndpoint == null) {
            Log.w(TAG, "Missing required endpoint on USB device " + getDeviceName());
            mConnection.releaseInterface(iface);
            close();
            return false;
        }

        mRunning = true;
        mInputThread = new InputThread();
        mInputThread.start();

        return true;
    }

    @Override
    public int writeReport(byte[] report, boolean feature) {
        if (report == null || report.length == 0) {
            return -1;
        }
        if (mConnection == null) {
            Log.w(TAG, "writeReport() called with no device connection");
            return -1;
        }
        if (!mClaimed) {
            Log.w(TAG, "writeReport() called but some other process currently owns the USB device");
            return -1;
        }

        if (feature) {
            int offset = 0;
            int length = report.length;
            boolean skipped_report_id = false;
            byte report_number = report[0];

            if (report_number == 0x0) {
                ++offset;
                --length;
                skipped_report_id = true;
            }

            int res = mConnection.controlTransfer(
                UsbConstants.USB_TYPE_CLASS | 0x01 | UsbConstants.USB_DIR_OUT,
                0x09,
                (3 << 8) | (report_number & 0xFF),
                mInterface,
                report, offset, length,
                1000);

            if (res < 0) {
                Log.w(TAG, "writeFeatureReport() returned " + res + " on device " + getDeviceName());
                return -1;
            }

            if (skipped_report_id) {
                ++length;
            }
            return length;
        } else {
            if (mOutputEndpoint == null) {
                Log.e(TAG, "Tried to write an output report to an interface with no output endpoint!");
                return -1;
            }
            int res = mConnection.bulkTransfer(mOutputEndpoint, report, report.length, 1000);
            if (res != report.length) {
                Log.w(TAG, "writeOutputReport() returned " + res + " on device " + getDeviceName());
            }
            return res;
        }
    }

    @Override
    public boolean readReport(byte[] report, boolean feature) {
        if (report == null || report.length == 0) {
            return false;
        }
        if (mConnection == null) {
            Log.w(TAG, "readReport() called with no device connection");
            return false;
        }
        if (!mClaimed) {
            return !feature;
        }

        int offset = 0;
        int length = report.length;
        boolean skipped_report_id = false;
        byte report_number = report[0];

        if (report_number == 0x0) {
            ++offset;
            --length;
            skipped_report_id = true;
        }

        int res = mConnection.controlTransfer(
            UsbConstants.USB_TYPE_CLASS | 0x01 | UsbConstants.USB_DIR_IN,
            0x01,
            ((feature ? 3 : 1) << 8) | (report_number & 0xFF),
            mInterface,
            report, offset, length,
            1000);

        if (res < 0) {
            Log.w(TAG, "getFeatureReport() returned " + res + " on device " + getDeviceName());
            return false;
        }

        if (skipped_report_id) {
            ++res;
            ++length;
        }

        byte[] data = (res == length) ? report : Arrays.copyOfRange(report, 0, res);
        HIDDeviceManager manager = mManager;
        if (manager != null) {
            manager.HIDDeviceReportResponse(mDeviceId, data);
        }

        return true;
    }

    @Override
    public void close() {
        mRunning = false;
        if (mInputThread != null) {
            while (mInputThread.isAlive()) {
                mInputThread.interrupt();
                try {
                    mInputThread.join();
                } catch (InterruptedException e) {
                    // Keep trying until we're done
                }
            }
            mInputThread = null;
        }
        if (mConnection != null) {
            if (mClaimed) {
                UsbInterface iface = mDevice.getInterface(mInterfaceIndex);
                mConnection.releaseInterface(iface);
            }
            mConnection.close();
            mConnection = null;
            mClaimed = false;
        }
    }

    @Override
    public void shutdown() {
        close();
        mManager = null;
    }

    @Override
    public void setFrozen(boolean frozen) {
        mFrozen = frozen;

        if (mConnection != null && mClaimed == mFrozen) {
            UsbInterface iface = mDevice.getInterface(mInterfaceIndex);
            if (frozen) {
                mClaimed = !mConnection.releaseInterface(iface);
                if (mClaimed) {
                    Log.e(TAG, "Tried to release claim on USB device, but failed!");
                }
            } else {
                mClaimed = mConnection.claimInterface(iface, true);
                if (!mClaimed) {
                    Log.e(TAG, "Tried to regain claim on USB device, but failed!");
                }
            }
        }
    }

    protected class InputThread extends Thread {
        @Override
        public void run() {
            int packetSize = mInputEndpoint != null ? mInputEndpoint.getMaxPacketSize() : 0;
            if (packetSize <= 0) {
                return;
            }
            byte[] packet = new byte[packetSize];
            while (mRunning) {
                UsbDeviceConnection conn = mConnection;
                if (conn == null || mInputEndpoint == null) {
                    break;
                }
                int r;
                try {
                    r = conn.bulkTransfer(mInputEndpoint, packet, packetSize, 1000);
                } catch (Exception e) {
                    Log.v(TAG, "Exception in UsbDeviceConnection bulktransfer: " + e);
                    break;
                }
                if (r > 0) {
                    byte[] data = (r == packetSize) ? packet : Arrays.copyOfRange(packet, 0, r);
                    HIDDeviceManager manager = mManager;
                    if (!mFrozen && manager != null) {
                        manager.HIDDeviceInputReport(mDeviceId, data);
                    }
                }
            }
        }
    }
}

@Keep
@android.annotation.SuppressLint("MissingPermission")
class HIDDeviceBLESteamController extends BluetoothGattCallback implements HIDDevice {

    private static final String TAG = "hidapi";
    private HIDDeviceManager mManager;
    private BluetoothDevice mDevice;
    private int mDeviceId;
    private BluetoothGatt mGatt;
    private boolean mIsRegistered = false;
    private boolean mIsConnected = false;
    private boolean mIsChromebook = false;
    private boolean mIsReconnecting = false;
    private boolean mHasEnabledNotifications = false;
    private boolean mHasSeenInputUpdate = false;
    private boolean mFrozen = false;
    private LinkedList<GattOperation> mOperations;
    GattOperation mCurrentOperation = null;
    private Handler mHandler;
    private int mProductId = -1;
    private int mReportId = 0;
    private UUID mInputCharacteristic;

    private static final int D0G_BLE2_PID = 0x1106;
    private static final int TRITON_BLE_PID = 0x1303;

    private static final int TRANSPORT_LE = 2;
    private static final int CHROMEBOOK_CONNECTION_CHECK_INTERVAL = 10000;

    static final UUID steamControllerService = UUID.fromString("100F6C32-1735-4313-B402-38567131E5F3");
    static final UUID inputCharacteristicD0G = UUID.fromString("100F6C33-1735-4313-B402-38567131E5F3");
    static final UUID inputCharacteristicTriton_0x45 = UUID.fromString("100F6C7A-1735-4313-B402-38567131E5F3");
    static final UUID inputCharacteristicTriton_0x47 = UUID.fromString("100F6C7C-1735-4313-B402-38567131E5F3");
    static final UUID reportCharacteristic = UUID.fromString("100F6C34-1735-4313-B402-38567131E5F3");
    static private final byte[] enterValveMode = new byte[] { (byte)0xC0, (byte)0x87, 0x03, 0x08, 0x07, 0x00 };

    private HashMap<Integer, BluetoothGattCharacteristic> mOutputReportChars = new HashMap<Integer, BluetoothGattCharacteristic>();

    static class GattOperation {
        private enum Operation {
            CHR_READ,
            CHR_WRITE,
            ENABLE_NOTIFICATION
        }

        Operation mOp;
        UUID mUuid;
        byte[] mValue;
        BluetoothGatt mGatt;
        boolean mResult = true;
        int mDelayMs = 0;

        private GattOperation(BluetoothGatt gatt, GattOperation.Operation operation, UUID uuid) {
            mGatt = gatt;
            mOp = operation;
            mUuid = uuid;
        }

        private GattOperation(BluetoothGatt gatt, GattOperation.Operation operation, UUID uuid, int delayMs) {
            mGatt = gatt;
            mOp = operation;
            mUuid = uuid;
            mDelayMs = delayMs;
        }

        private GattOperation(BluetoothGatt gatt, GattOperation.Operation operation, UUID uuid, byte[] value) {
            mGatt = gatt;
            mOp = operation;
            mUuid = uuid;
            mValue = value;
        }

        public void run() {
            BluetoothGattCharacteristic chr;
            if (mGatt == null) {
                mResult = false;
                return;
            }

            try {
                switch (mOp) {
                    case CHR_READ:
                        chr = getCharacteristic(mUuid);
                        if (chr == null || !mGatt.readCharacteristic(chr)) {
                            Log.e(TAG, "Unable to read characteristic " + mUuid.toString());
                            mResult = false;
                            break;
                        }
                        mResult = true;
                        break;
                    case CHR_WRITE:
                        chr = getCharacteristic(mUuid);
                        if (chr == null || mValue == null) {
                            mResult = false;
                            break;
                        }
                        chr.setValue(mValue);
                        if (!mGatt.writeCharacteristic(chr)) {
                            Log.e(TAG, "Unable to write characteristic " + mUuid.toString());
                            mResult = false;
                            break;
                        }
                        mResult = true;
                        break;
                    case ENABLE_NOTIFICATION:
                        chr = getCharacteristic(mUuid);
                        if (chr != null) {
                            BluetoothGattDescriptor cccd = chr.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                            if (cccd != null) {
                                int properties = chr.getProperties();
                                byte[] value;
                                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                                    value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                                } else if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
                                    value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
                                } else {
                                    Log.e(TAG, "Unable to start notifications on input characteristic");
                                    mResult = false;
                                    return;
                                }

                                mGatt.setCharacteristicNotification(chr, true);
                                cccd.setValue(value);
                                if (!mGatt.writeDescriptor(cccd)) {
                                    Log.e(TAG, "Unable to write descriptor " + mUuid.toString());
                                    mResult = false;
                                    return;
                                }
                                mResult = true;
                            }
                        }
                }
            } catch (SecurityException e) {
                Log.w(TAG, "Missing Bluetooth permission during GATT op " + mOp, e);
                mResult = false;
            }
        }

        public boolean finish() {
            return mResult;
        }

        public int getDelayMs() { return mDelayMs; }

        private BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
            BluetoothGattService valveService = mGatt != null ? mGatt.getService(steamControllerService) : null;
            if (valveService == null) {
                return null;
            }
            return valveService.getCharacteristic(uuid);
        }

        static public GattOperation readCharacteristic(BluetoothGatt gatt, UUID uuid) {
            return new GattOperation(gatt, Operation.CHR_READ, uuid);
        }

        static public GattOperation writeCharacteristic(BluetoothGatt gatt, UUID uuid, byte[] value) {
            return new GattOperation(gatt, Operation.CHR_WRITE, uuid, value);
        }

        static public GattOperation enableNotification(BluetoothGatt gatt, UUID uuid, int delayMs) {
            return new GattOperation(gatt, Operation.ENABLE_NOTIFICATION, uuid, delayMs);
        }
    }

    HIDDeviceBLESteamController(HIDDeviceManager manager, BluetoothDevice device) {
        mManager = manager;
        mDevice = device;
        mDeviceId = mManager.getDeviceIDForIdentifier(getIdentifier());
        mIsRegistered = false;
        mIsChromebook = SDLActivity.isChromebook();
        mOperations = new LinkedList<GattOperation>();
        mHandler = new Handler(Looper.getMainLooper());

        mGatt = connectGatt();
        mHasEnabledNotifications = false;
        mHasSeenInputUpdate = false;
    }

    String getIdentifier() {
        return String.format("SteamController.%s", mDevice.getAddress());
    }

    BluetoothGatt getGatt() {
        return mGatt;
    }

    private boolean hasBluetoothConnectPermission() {
        Context context = mManager != null ? mManager.getContext() : null;
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 31) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
    }

    private BluetoothGatt connectGatt(boolean managed) {
        if (!hasBluetoothConnectPermission() || mDevice == null || mManager == null) {
            return null;
        }
        Context context = mManager.getContext();
        try {
            return mDevice.connectGatt(context, managed, this, TRANSPORT_LE);
        } catch (SecurityException se) {
            return null;
        } catch (Exception e) {
            try {
                return mDevice.connectGatt(context, managed, this);
            } catch (SecurityException se) {
                return null;
            }
        }
    }

    private BluetoothGatt connectGatt() {
        return connectGatt(false);
    }

    protected int getConnectionState() {
        Context context = mManager != null ? mManager.getContext() : null;
        if (context == null || !hasBluetoothConnectPermission() || mDevice == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }

        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (btManager == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }

        try {
            return btManager.getConnectionState(mDevice, BluetoothProfile.GATT);
        } catch (SecurityException e) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
    }

    void reconnect() {
        if (!hasBluetoothConnectPermission()) {
            return;
        }
        if (getConnectionState() != BluetoothProfile.STATE_CONNECTED) {
            if (mGatt != null) {
                try {
                    mGatt.disconnect();
                } catch (SecurityException ignored) {
                }
            }
            mGatt = connectGatt();
        }
    }

    protected void checkConnectionForChromebookIssue() {
        if (!mIsChromebook || !hasBluetoothConnectPermission()) {
            return;
        }

        int connectionState = getConnectionState();

        switch (connectionState) {
            case BluetoothProfile.STATE_CONNECTED:
                if (!mIsConnected) {
                    Log.v(TAG, "Chromebook: forcing reconnect.");
                    mIsReconnecting = true;
                    if (mGatt != null) {
                        try { mGatt.disconnect(); } catch (SecurityException ignored) {}
                    }
                    mGatt = connectGatt(false);
                    break;
                } else if (!isRegistered()) {
                    if (mGatt != null && mGatt.getServices().size() > 0) {
                        probeService(this);
                    } else {
                        mIsReconnecting = true;
                        if (mGatt != null) {
                            try { mGatt.disconnect(); } catch (SecurityException ignored) {}
                        }
                        mGatt = connectGatt(false);
                        break;
                    }
                } else {
                    return;
                }
                break;

            case BluetoothProfile.STATE_DISCONNECTED:
                mIsReconnecting = true;
                if (mGatt != null) {
                    try { mGatt.disconnect(); } catch (SecurityException ignored) {}
                }
                mGatt = connectGatt(false);
                break;

            case BluetoothProfile.STATE_CONNECTING:
                break;
        }

        final HIDDeviceBLESteamController finalThis = this;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finalThis.checkConnectionForChromebookIssue();
            }
        }, CHROMEBOOK_CONNECTION_CHECK_INTERVAL);
    }

    private boolean isRegistered() {
        return mIsRegistered;
    }

    private void setRegistered() {
        mIsRegistered = true;
    }

    private boolean probeService(HIDDeviceBLESteamController controller) {
        if (isRegistered()) {
            return true;
        }
        if (!mIsConnected || mGatt == null) {
            return false;
        }

        for (BluetoothGattService service : mGatt.getServices()) {
            if (service.getUuid().equals(steamControllerService)) {
                for (BluetoothGattCharacteristic chr : service.getCharacteristics()) {
                    if (chr.getUuid().equals(inputCharacteristicTriton_0x45)) {
                        mProductId = TRITON_BLE_PID;
                        mReportId = 0x45;
                        mInputCharacteristic = chr.getUuid();
                    } else if (chr.getUuid().equals(inputCharacteristicTriton_0x47)) {
                        mProductId = TRITON_BLE_PID;
                        mReportId = 0x47;
                        mInputCharacteristic = chr.getUuid();
                    } else if (chr.getUuid().equals(inputCharacteristicD0G)) {
                        mProductId = D0G_BLE2_PID;
                        mReportId = 0x03;
                        mInputCharacteristic = chr.getUuid();
                    } else {
                        Pattern reportPattern = Pattern.compile("100F6C([0-9A-Z]{2})", Pattern.CASE_INSENSITIVE);
                        Matcher matcher = reportPattern.matcher(chr.getUuid().toString());
                        if (matcher.find()) {
                            try {
                                String hex = matcher.group(1);
                                if (hex != null) {
                                    int reportId = Integer.parseInt(hex, 16) - 0x35;
                                    if (reportId >= 0x80) {
                                        mOutputReportChars.put(reportId, chr);
                                    }
                                }
                            } catch (NumberFormatException nfe) {
                                Log.w(TAG, "Could not parse report characteristic " + chr.getUuid() + ": " + nfe);
                            }
                        }
                    }
                }

                for (BluetoothGattCharacteristic chr : service.getCharacteristics()) {
                    if (chr.getUuid().equals(mInputCharacteristic)) {
                        BluetoothGattDescriptor cccd = chr.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        if (cccd != null) {
                            enableNotification(chr.getUuid());
                        }
                    }
                }
                return true;
            }
        }

        if ((mGatt != null && mGatt.getServices().isEmpty()) && mIsChromebook && !mIsReconnecting && hasBluetoothConnectPermission()) {
            mIsConnected = false;
            mIsReconnecting = true;
            try { mGatt.disconnect(); } catch (SecurityException ignored) {}
            mGatt = connectGatt(false);
        }

        return false;
    }

    private void finishCurrentGattOperation() {
        GattOperation op = null;
        synchronized (mOperations) {
            if (mCurrentOperation != null) {
                op = mCurrentOperation;
                mCurrentOperation = null;
            }
        }
        if (op != null) {
            boolean result = op.finish();
            if (!result) {
                synchronized (mOperations) {
                    mOperations.addFirst(op);
                }
            }
        }
        executeNextGattOperation();
    }

    private void executeNextGattOperation() {
        GattOperation scheduled;
        synchronized (mOperations) {
            if (mCurrentOperation != null || mOperations.isEmpty()) {
                return;
            }
            mCurrentOperation = mOperations.removeFirst();
            scheduled = mCurrentOperation;
        }

        Runnable gattOperationRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (mOperations) {
                    if (mCurrentOperation == null) {
                        Log.e(TAG, "Current operation null in executor?");
                        return;
                    }
                    mCurrentOperation.run();
                }
            }
        };

        if (scheduled.getDelayMs() == 0) {
            mHandler.post(gattOperationRunnable);
        } else {
            mHandler.postDelayed(gattOperationRunnable, scheduled.getDelayMs());
        }
    }

    private void queueGattOperation(GattOperation op) {
        synchronized (mOperations) {
            mOperations.add(op);
        }
        executeNextGattOperation();
    }

    private void enableNotification(UUID chrUuid) {
        GattOperation op = HIDDeviceBLESteamController.GattOperation.enableNotification(mGatt, chrUuid, 500);
        queueGattOperation(op);

        final HIDDeviceBLESteamController finalThis = this;
        final UUID finalUuid = chrUuid;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!finalThis.mHasEnabledNotifications && finalThis.mManager != null) {
                    if (finalThis.mHasSeenInputUpdate) {
                        finalThis.mHasEnabledNotifications = true;
                        finalThis.enableValveMode();
                        return;
                    }
                    GattOperation retry = HIDDeviceBLESteamController.GattOperation.enableNotification(finalThis.mGatt, finalUuid, 500);
                    finalThis.queueGattOperation(retry);
                }
            }
        }, 1000);
    }

    void writeCharacteristic(UUID uuid, byte[] value) {
        GattOperation op = HIDDeviceBLESteamController.GattOperation.writeCharacteristic(mGatt, uuid, value);
        queueGattOperation(op);
    }

    void readCharacteristic(UUID uuid) {
        GattOperation op = HIDDeviceBLESteamController.GattOperation.readCharacteristic(mGatt, uuid);
        queueGattOperation(op);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
        mIsReconnecting = false;
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            mIsConnected = true;
            if (!isRegistered()) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mGatt != null && hasBluetoothConnectPermission()) {
                            try { mGatt.discoverServices(); } catch (SecurityException ignored) {}
                        }
                    }
                });
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            mIsConnected = false;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (gatt.getServices().isEmpty()) {
                mIsReconnecting = true;
                mIsConnected = false;
                if (hasBluetoothConnectPermission()) {
                    try { gatt.disconnect(); } catch (SecurityException ignored) {}
                }
                mGatt = connectGatt(false);
            } else {
                if (getProductId() == TRITON_BLE_PID && mGatt != null && hasBluetoothConnectPermission()) {
                    try { mGatt.requestMtu(517); } catch (SecurityException ignored) {}
                }
                probeService(this);
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (characteristic.getUuid().equals(reportCharacteristic) && !mFrozen && mManager != null) {
            mManager.HIDDeviceReportResponse(getId(), characteristic.getValue());
        }
        finishCurrentGattOperation();
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (characteristic.getUuid().equals(reportCharacteristic) && !isRegistered() && mManager != null) {
            Log.v(TAG, "Registering Steam Controller with ID: " + getId());
            mManager.HIDDeviceConnected(getId(), getIdentifier(), getVendorId(), getProductId(), getSerialNumber(), getVersion(), getManufacturerName(), getProductName(), 0, 0, 0, 0, true, mReportId);
            setRegistered();
        }
        finishCurrentGattOperation();
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(mInputCharacteristic) && !mFrozen && mManager != null) {
            mHasSeenInputUpdate = true;
            mManager.HIDDeviceInputReport(getId(), characteristic.getValue());
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    }

    private void enableValveMode() {
        BluetoothGatt gatt = mGatt;
        if (gatt == null || mManager == null) {
            return;
        }
        BluetoothGattService valveService = gatt.getService(steamControllerService);
        if (valveService == null) {
            return;
        }

        BluetoothGattCharacteristic reportChr = valveService.getCharacteristic(reportCharacteristic);
        if (reportChr != null) {
            if (getProductId() == TRITON_BLE_PID) {
                Log.v(TAG, "Registering Triton Steam Controller with ID: " + getId());
                mManager.HIDDeviceConnected(getId(), getIdentifier(), getVendorId(), getProductId(), getSerialNumber(), getVersion(), getManufacturerName(), getProductName(), 0, 0, 0, 0, true, mReportId);
                setRegistered();
            } else if (hasBluetoothConnectPermission()) {
                Log.v(TAG, "Writing report characteristic to enter valve mode");
                reportChr.setValue(enterValveMode);
                try { gatt.writeCharacteristic(reportChr); } catch (SecurityException ignored) {}
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        BluetoothGattCharacteristic chr = descriptor.getCharacteristic();
        if (chr != null && chr.getUuid().equals(mInputCharacteristic)) {
            mHasEnabledNotifications = true;
            enableValveMode();
        }
        finishCurrentGattOperation();
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
    }

    @Override
    public int getId() {
        return mDeviceId;
    }

    @Override
    public int getVendorId() {
        return 0x28DE;
    }

    @Override
    public int getProductId() {
        if (mProductId > 0) {
            return mProductId;
        }

        String name = null;
        if (mDevice != null && hasBluetoothConnectPermission()) {
            try { name = mDevice.getName(); } catch (SecurityException ignored) {}
        }
        if (name != null && name.startsWith("Steam Ctrl")) {
            mProductId = TRITON_BLE_PID;
        } else {
            mProductId = D0G_BLE2_PID;
        }

        return mProductId;
    }

    @Override
    public String getSerialNumber() {
        return "12345";
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public String getManufacturerName() {
        return "Valve Corporation";
    }

    @Override
    public String getProductName() {
        return "Steam Controller";
    }

    @Override
    public UsbDevice getDevice() {
        return null;
    }

    @Override
    public boolean open() {
        return true;
    }

    @Override
    public int writeReport(byte[] report, boolean feature) {
        if (report == null) {
            return -1;
        }
        if (!isRegistered()) {
            Log.e(TAG, "Attempted writeReport before Steam Controller is registered!");
            if (mIsConnected) {
                probeService(this);
            }
            return -1;
        }

        if (feature) {
            if (report.length <= 1) {
                return -1;
            }
            byte[] actual_report = Arrays.copyOfRange(report, 1, report.length - 1);
            writeCharacteristic(reportCharacteristic, actual_report);
            return report.length;
        } else {
            if (getProductId() == D0G_BLE2_PID) {
                writeCharacteristic(reportCharacteristic, report);
                return report.length;
            }

            if (report.length > 1) {
                int reportId = report[0] & 0xFF;
                BluetoothGattCharacteristic targetedReportCharacteristic = mOutputReportChars.get(reportId);
                if (targetedReportCharacteristic != null) {
                    byte[] actual_report = Arrays.copyOfRange(report, 1, report.length - 1);
                    writeCharacteristic(targetedReportCharacteristic.getUuid(), actual_report);
                    return report.length;
                } else {
                    Log.w(TAG, "Got report write request for unknown report type 0x" + Integer.toString(reportId, 16));
                }
            }
        }

        return -1;
    }

    @Override
    public boolean readReport(byte[] report, boolean feature) {
        if (!isRegistered()) {
            Log.e(TAG, "Attempted readReport before Steam Controller is registered!");
            if (mIsConnected) {
                probeService(this);
            }
            return false;
        }

        if (feature) {
            readCharacteristic(reportCharacteristic);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void setFrozen(boolean frozen) {
        mFrozen = frozen;
    }

    @Override
    public void shutdown() {
        close();

        BluetoothGatt g = mGatt;
        if (g != null) {
            if (hasBluetoothConnectPermission()) {
                try { g.disconnect(); } catch (SecurityException ignored) {}
                try { g.close(); } catch (SecurityException ignored) {}
            }
            mGatt = null;
        }
        mManager = null;
        mIsRegistered = false;
        mIsConnected = false;
        synchronized (mOperations) {
            mOperations.clear();
        }
    }
}

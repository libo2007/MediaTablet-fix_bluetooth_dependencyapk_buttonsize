package com.jiaying.mediatablet.service;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.jiaying.mediatablet.utils.BluetoothTools;
import com.jiaying.mediatablet.utils.ClsUtis;

/**
 *
 * @项目名称：BluetoothConnect
 * @类名称：BluetoothConnectService
 * @类描述：连接指定蓝牙服务
 * @创建人：李波
 * @创建时间：2016-6-7 上午9:20:51
 * @版本号：v1.0
 *
 */
public class BluetoothConnectService extends Service {
    private static final String TAG = "BluetoothConnectService";
    // 需要连接的蓝牙名称
    private static final String BLUETOOTH_NAME = "GT-I9300";
    // 搜索到的远程设备集合
    private List<BluetoothDevice> discoveredDevices = new ArrayList<BluetoothDevice>();

    // 蓝牙适配器
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter
            .getDefaultAdapter();
    private boolean TempB = false;// 判断是否是主动取消的搜索

    // 查询次数
    private int seacrh_count = 0;
    // 查询最大次数
    private static final int SEARCH_COUNT_MAX = 10;
    // 连接次数
    private int connect_count = 0;
    // 连接次数最大值
    private static final int CONNECT_COUNT_MAX = 5;

    // 当前要连接的设备
    private BluetoothDevice device = null;
    // 控制信息广播的接收器
    private BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothTools.ACTION_SELECTED_DEVICE.equals(action)) {
                // 搜到设备
                device = (BluetoothDevice) intent.getExtras().get(
                        BluetoothTools.DEVICE);
                // 开启设备连接
                if (device != null) {
                    boolean pairResult = ClsUtis.pair(device.getAddress(), "");
                    Log.e(TAG, "蓝牙配对结果:" + pairResult + ",connect_count:"
                            + connect_count);
                    if (pairResult) {
                        stopSelf();
                    } else {
                        if (connect_count <= CONNECT_COUNT_MAX) {
                            pairResult = ClsUtis.pair(device.getAddress(), "");
                            connect_count++;
                        } else {
                            stopSelf();
                        }
                    }
                }
                // new BluetoothClientConnThread(handler, device).start();
            } else if (BluetoothTools.ACTION_STOP_SERVICE.equals(action)) {
                // 停止后台服务
                stopSelf();
            } else if (BluetoothTools.ACTION_DATA_TO_SERVICE.equals(action)) {
                // 获取数据
            }
        }
    };

    // 蓝牙搜索广播的接收器
    private BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 获取广播的Action
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                // 开始搜索
                Log.e(TAG, "开始搜索");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 发现远程蓝牙设备
                // 获取设备
                BluetoothDevice bluetoothDevice = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String address = bluetoothDevice.getAddress();
                String name = bluetoothDevice.getName();
                Log.e(TAG, "this bluetooth address:" + address);
                Log.e(TAG, "this bluetooth name:" + name);
                // 这里搜索到与地址匹配的手机后，发送广播，由注册了该广播的Receiver进行连接操作
                if (!TextUtils.isEmpty(name) && name.equals(BLUETOOTH_NAME)) {
                    TempB = true;
                    bluetoothAdapter.cancelDiscovery();// 取消搜索
                    // 将广播发送出去
                    Intent selectDeviceIntent = new Intent(
                            BluetoothTools.ACTION_SELECTED_DEVICE);
                    selectDeviceIntent.putExtra(BluetoothTools.DEVICE,
                            bluetoothDevice);
                    sendBroadcast(selectDeviceIntent);
                } else {
                    if (!TempB) {
                        // 若未找到设备，则发动未发现设备广播
                        Intent foundIntent = new Intent(
                                BluetoothTools.ACTION_NOT_FOUND_SERVER);
                        sendBroadcast(foundIntent);
                    }
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                    .equals(action)) {
                // 搜索结束，如果不是主动取消的搜索，就发送广播
                if (!TempB) {
                    // 若未找到设备，则发动未发现设备广播
                    Intent foundIntent = new Intent(
                            BluetoothTools.ACTION_NOT_FOUND_SERVER);
                    sendBroadcast(foundIntent);
                }
            } else if (BluetoothTools.ACTION_NOT_FOUND_SERVER.equals(action)) {

                if (seacrh_count <= SEARCH_COUNT_MAX) {
                    startDiscoveryDevice();
                    Log.e(TAG, " 搜索完成，未发现设备，继续调用搜索,次数：" + seacrh_count);
                } else {
                    Log.e(TAG, " 搜索完成，未发现设备，停止搜索,次数：" + seacrh_count);
                    bluetoothAdapter.disable();
                    stopSelf();
                }

            }
        }
    };

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        // discoveryReceiver的IntentFilter
        Log.e(TAG, "servie onCreate");
        IntentFilter discoveryFilter = new IntentFilter();
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
        discoveryFilter.addAction(BluetoothTools.ACTION_NOT_FOUND_SERVER);

        // controlReceiver的IntentFilter
        IntentFilter controlFilter = new IntentFilter();
        controlFilter.addAction(BluetoothTools.ACTION_SELECTED_DEVICE);
        controlFilter.addAction(BluetoothTools.ACTION_STOP_SERVICE);
        controlFilter.addAction(BluetoothTools.ACTION_DATA_TO_SERVICE);

        // 注册BroadcastReceiver
        registerReceiver(discoveryReceiver, discoveryFilter);
        registerReceiver(controlReceiver, controlFilter);
        startDiscoveryDevice();

    }

    private void startDiscoveryDevice() {
        discoveredDevices.clear(); // 清空存放设备的集合

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        // 如果正在搜索，就先取消搜索
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        // 开始搜索蓝牙设备,搜索到的蓝牙设备通过广播返回
        boolean startDiscovery = bluetoothAdapter.startDiscovery();
        if (startDiscovery) {
            seacrh_count++;
        }
        Log.e(TAG, "开始搜索设备：" + startDiscovery);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        // 解除绑定
        if(discoveryReceiver!=null){
            unregisterReceiver(discoveryReceiver);
        }
        if(controlReceiver!=null){
            unregisterReceiver(controlReceiver);
        }
        // bluetoothAdapter.disable();// 关闭蓝牙
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);

    }
}

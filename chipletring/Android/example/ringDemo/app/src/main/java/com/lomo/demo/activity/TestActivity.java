package com.lomo.demo.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.lm.sdk.BLEService;
import com.lm.sdk.LmAPILite;

import com.lm.sdk.LogicalApi;
import com.lm.sdk.OtaApi;
import com.lm.sdk.inter.ICreateToken;
import com.lm.sdk.inter.IGoMoreListener;
import com.lm.sdk.inter.IGoMoreUserListener;
import com.lm.sdk.inter.ISNListener;
import com.lm.sdk.inter.LmOtaProgressListener;
import com.lm.sdk.library.utils.DateUtils;
import com.lm.sdk.library.utils.Logger;
import com.lm.sdk.lmApiInter.IBatteryListenerLite;
import com.lm.sdk.lmApiInter.IBindConnectRefreshListenerLite;
import com.lm.sdk.lmApiInter.IBloodOxygenListenerLite;
import com.lm.sdk.lmApiInter.IHeartListenerLite;
import com.lm.sdk.lmApiInter.IHistoryListenerLite;
import com.lm.sdk.lmApiInter.IResponseListenerLite;
import com.lm.sdk.lmApiInter.IStepListenerLite;
import com.lm.sdk.lmApiInter.ISyncTimeListenerLite;
import com.lm.sdk.lmApiInter.ISystemControlListenerLite;
import com.lm.sdk.lmApiInter.ITempListenerLite;
import com.lm.sdk.lmApiInter.IVersionListenerLite;
import com.lm.sdk.mode.BleDeviceInfo;
import com.lm.sdk.mode.GoMoreSleep;
import com.lm.sdk.mode.HistoryDataBean;
import com.lm.sdk.mode.SystemControlLiteBean;
import com.lm.sdk.utils.BLEUtils;
import com.lm.sdk.utils.ImageSaverUtil;
import com.lm.sdk.utils.UtilSharedPreference;
import com.lomo.demo.GsonUtils;
import com.lomo.demo.R;

import com.lomo.demo.adapter.DeviceBean;
import com.lomo.demo.application.App;
import com.lomo.demo.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class TestActivity extends BaseActivity implements IResponseListenerLite, View.OnClickListener {
    public String TAG = getClass().getSimpleName();
    TextView tv_result;
    EditText et_collection_period, et_bluetooth_name, et_gomore_age, et_gomore_sex, et_gomore_height, et_gomore_weight;
    private int lastSetPeriod = 0;//记录最近一次设置的采集周期，用于回调提示
    private DeviceBean deviceBean;
    private BluetoothDevice bluetoothDevice;

    private ISystemControlListenerLite iSystemControlListenerLite=new ISystemControlListenerLite() {
        @Override
        public void reset() {
            postView("Factory reset"+"\n");
        }

        @Override
        public void getSerialNum(String serial) {
            postView("Serial number: "+serial+"\n");
        }

        @Override
        public void setCollection(boolean success) {
            postView("Set interval: "+lastSetPeriod+"sec: "+(success?"Success":"Failed")+"\n");
        }

        @Override
        public void getCollection(int data) {
            postView("Interval: "+data+"sec"+"\n");
        }

        @Override
        public void setSerialNum(boolean success) {
            postView("Set serial number: "+success+"\n");
        }


        @Override
        public void setBlueToolName(boolean success) {
            postView("Set Bluetooth name: "+success+"\n");
        }

        @Override
        public void readBlueToolName(int len, String name) {
            postView("Bluetooth name: "+name+", length: "+len+"\n");
        }
    };


    Handler handler = new Handler(new Handler.Callback() {


        @Override
        public boolean handleMessage(@NonNull Message msg) {

            if (msg.what == 101) {

                String mac = UtilSharedPreference.getStringValue(TestActivity.this, "address");
                if (!TextUtils.isEmpty(mac) && !BLEUtils.isGetToken() && App.needAutoConnect) {
                    Log.e("TAG", "Handler delayed reconnect resetConnect 1111 ");
                    BLEUtils.setConnecting(false);
                    // BLEUtils.connectLockByBLE(TestActivity.this, deviceBean.getDevice());
                    connect(mac);
                }

            }
            return false;
        }});

    static String mac;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        LmAPILite.addWLSCmdListener(this, this);

        tv_result=findViewById(R.id.tv_result); enableShareLog(tv_result);
        et_collection_period = findViewById(R.id.et_collection_period);
        et_bluetooth_name = findViewById(R.id.et_bluetooth_name);
        et_gomore_age = findViewById(R.id.et_gomore_age);
        et_gomore_sex = findViewById(R.id.et_gomore_sex);
        et_gomore_height = findViewById(R.id.et_gomore_height);
        et_gomore_weight = findViewById(R.id.et_gomore_weight);

        findViewById(R.id.bt_app_bind).setOnClickListener(this);
        findViewById(R.id.bt_app_connect).setOnClickListener(this);
        findViewById(R.id.bt_app_refresh).setOnClickListener(this);
        findViewById(R.id.bt_sync_time).setOnClickListener(this);
        findViewById(R.id.bt_read_time).setOnClickListener(this);
        findViewById(R.id.bt_battery).setOnClickListener(this);
        findViewById(R.id.bt_battery_state).setOnClickListener(this);
        findViewById(R.id.bt_soft_version).setOnClickListener(this);
        findViewById(R.id.bt_hardware_version).setOnClickListener(this);
        findViewById(R.id.bt_clear_step).setOnClickListener(this);
        findViewById(R.id.bt_step).setOnClickListener(this);
        findViewById(R.id.bt_heart).setOnClickListener(this);
        findViewById(R.id.bt_blood_oxygen).setOnClickListener(this);
        findViewById(R.id.bt_read_log).setOnClickListener(this);
        findViewById(R.id.bt_test_temp).setOnClickListener(this);
        findViewById(R.id.bt_gomore_test).setOnClickListener(this);
        findViewById(R.id.bt_gomore_user).setOnClickListener(this);
        findViewById(R.id.bt_get_gomore_user).setOnClickListener(this);
        findViewById(R.id.bt_setSn).setOnClickListener(this);
        findViewById(R.id.bt_getSn).setOnClickListener(this);
        findViewById(R.id.bt_setBluttoothName).setOnClickListener(this);
        findViewById(R.id.bt_getBluttoothName).setOnClickListener(this);
        findViewById(R.id.tv_connect).setOnClickListener(this);
        findViewById(R.id.bt_setCollection).setOnClickListener(this);
        findViewById(R.id.btn_getCollection).setOnClickListener(this);
        findViewById(R.id.bt_get_page2).setOnClickListener(this);
        findViewById(R.id.bt_linear_motor).setOnClickListener(this);
        findViewById(R.id.btn_ota).setOnClickListener(this);
        findViewById(R.id.bt_new_protocol).setOnClickListener(this);
        findViewById(R.id.bt_touch_setting).setOnClickListener(this);
        //获取上个页面传递过来的deviceBean对象
        //部分OPPO/华为机型上Parcelable回读可能失败返回null，直接调用getDevice()会NPE闪退
        Intent intent = getIntent();
        deviceBean = intent != null ? intent.getParcelableExtra("deviceBean") : null;
        if (deviceBean != null && deviceBean.getDevice() != null) {
            bluetoothDevice = deviceBean.getDevice();
            mac = bluetoothDevice.getAddress();
            App.getInstance().setDeviceBean(deviceBean);
            try {
                BLEUtils.connectLockByBLE(this, deviceBean.getDevice());
            } catch (SecurityException e) {
                //OPPO/华为部分机型会回收BLUETOOTH_CONNECT权限，连接时抛SecurityException
                Toast.makeText(this, "Bluetooth permission missing, grant it in system Settings", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Unknown device, choose again!", Toast.LENGTH_SHORT).show();
            finish();
        }
        READ_HISTORY_AUTO();

        LogicalApi.createToken("76d07e37bfe341b1a25c76c0e25f457a", "1204491582@qq.com", new ICreateToken() {
            @Override
            public void getTokenSuccess() {

            }

            @Override
            public void error(String msg) {

            }
        });
    }


    /**
     * 断联以后，重连
     *
     * @param mac
     */
    private void connect(String mac) {
        Logger.show(TAG, "connect=" + mac, 6);
        this.mac = mac;
        //合并
        checkPermission();
    }

    public void checkPermission() {


        Logger.show("ConnectDevice", "mac :" + mac);
        BluetoothDevice remote = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
        if (BLEService.isGetToken()) {
            Logger.show("ConnectDevice", " Bluetooth connected");

        } else if (remote != null && (mac).equalsIgnoreCase(remote.getAddress())) {
                BLEUtils.connectLockByBLE(TestActivity.this, remote);
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        BLEUtils.disconnectBLE(this);
        LmAPILite.removeWLSCmdListener(this);
        handler.removeMessages(101);
    }

    @Override
    public void lmBleConnecting(int i) {
        postView("\nConnecting..." + i);
    }

    @Override
    public void lmBleConnectionSucceeded(int i) {
        if(i==7){
            BLEUtils.setGetToken(true);
            postView("\nConnected");
        }else{
            BLEUtils.setGetToken(false);
            postView("\nConnection failed");
        }

    }

    @Override
    public void lmBleConnectionFailed(int i) {
        BLEUtils.setGetToken(false);
        postView("\nConnection failed ");

        Log.e("ConnectDevice", " Bluetooth connectionFailed");

        handler.removeMessages(101);
        handler.sendEmptyMessageDelayed(101, 5000);

    }

    @Override
    public void timeOut(String cmd) {
        postView("\nCommand timed out");
    }


    @Override
    public void saveData(String s) {
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.bt_app_bind){
            postView("\nAPP_BIND");
            LmAPILite.APP_BIND( new IBindConnectRefreshListenerLite() {
                @Override
                public void appBind(SystemControlLiteBean systemControlBean) {
                    postView("\nappBind:"+systemControlBean);
                }

                @Override
                public void appConnect(SystemControlLiteBean systemControlBean) {

                }

                @Override
                public void appRefresh(SystemControlLiteBean systemControlBean) {

                }
            });
        }


            if(view.getId()==R.id.bt_app_connect){
                postView("\nAPP_CONNECT");
                LmAPILite.APP_CONNECT(0, new IBindConnectRefreshListenerLite() {
                    @Override
                    public void appBind(SystemControlLiteBean systemControlBean) {

                    }

                    @Override
                    public void appConnect(SystemControlLiteBean systemControlBean) {
                        postView("\nappConnect:"+systemControlBean);
                    }

                    @Override
                    public void appRefresh(SystemControlLiteBean systemControlBean) {

                    }
                });
            }

        if(view.getId()==R.id.bt_app_refresh){
            postView("\nAPP_REFRESH");
            LmAPILite.APP_REFRESH(0, new IBindConnectRefreshListenerLite() {
                @Override
                public void appBind(SystemControlLiteBean systemControlBean) {

                }

                @Override
                public void appConnect(SystemControlLiteBean systemControlBean) {

                }

                @Override
                public void appRefresh(SystemControlLiteBean systemControlBean) {
                    postView("\nappRefresh:"+systemControlBean);
                }
            });
        }

        if(view.getId()==R.id.bt_clear_step){
            postView("\nClearing steps");
            LmAPILite.CLEAR_COUNTING(new IStepListenerLite() {
                @Override
                public void stepCount(int steps) {

                }

                @Override
                public void clearStepCount() {
                    postView("\nSteps cleared");
                }
            });
        }
        if(view.getId()==R.id.bt_sync_time){
            postView("\nSyncing time");
            LmAPILite.SYNC_TIME_ZONE((byte) 8, new ISyncTimeListenerLite() {
                @Override
                public void syncTime(boolean updateTime, long timeStamp, long timeZone) {
                    postView("\nTime synced");
                }
            });
        }
        if(view.getId()==R.id.bt_read_time){
            postView("\nReading time");
            LmAPILite.READ_TIME(new ISyncTimeListenerLite() {
                @Override
                public void syncTime(boolean updateTime, long timeStamp, long timeZone) {
                    postView("\nTime: "+ DateUtils.longToString(timeStamp, "yyyy-MM-dd HH:mm")+", time zone: "+timeZone);
                }
            });
        }

        if(view.getId()==R.id.bt_soft_version){
            postView("\nGetting firmware version");
            LmAPILite.GET_VERSION(true, new IVersionListenerLite() {
                @Override
                public void versionResult(String softwareVersion, String hardwareVersion) {
                    postView("\nFirmware version: " + softwareVersion);
                }
            });
        }


        if(view.getId()==R.id.bt_hardware_version){
            postView("\nGetting hardware version");
            LmAPILite.GET_VERSION(false, new IVersionListenerLite() {
                @Override
                public void versionResult(String softwareVersion, String hardwareVersion) {
                    postView("\nHardware version: " + hardwareVersion);
                }
            });
        }

        if(view.getId()==R.id.bt_battery){
            postView("\nGetting battery");
            LmAPILite.GET_BATTERY((byte) 0x00, new IBatteryListenerLite() {
                @Override
                public void battery(int type, int electricity) {
                    if(type==0){
                        postView("\nBattery: "+electricity);
                    }

                }

                @Override
                public void battery_push(int type, int electricity) {
                    postView("\nBattery push: "+electricity);
                }
            });
        }

        if(view.getId()==R.id.bt_battery_state){
            postView("\nGetting charging status");
            LmAPILite.GET_BATTERY((byte) 0x01, new IBatteryListenerLite() {
                @Override
                public void battery(int type, int electricity) {
                    if(type==1){
                        postView("\nCharging status: "+electricity);
                    }

                }

                @Override
                public void battery_push(int type, int electricity) {
                    postView("\nBattery push: "+electricity);
                }
            });
        }

        if(view.getId()==R.id.bt_step){
            postView("\nGetting steps");
            LmAPILite.STEP_COUNTING(new IStepListenerLite() {
                @Override
                public void stepCount(int steps) {
                    postView("\nSteps: "+steps);
                }

                @Override
                public void clearStepCount() {

                }
            });
        }

        if(view.getId()==R.id.bt_blood_oxygen){
            postView("\nMeasuring SpO2");
            LmAPILite.GET_HEART_Q2_WITH_TIME((byte) 1, (byte) 30, new IBloodOxygenListenerLite() {

                @Override
                public void progress(int progress) {
                    postView("\nprogress：" + progress );
                }

                @Override
                public void resultData(int bloodOxygen, int perfusion,int progress) {
                    postView("\nSpO2: " + bloodOxygen + ", perfusion: " + perfusion + "%"+ ", progress: " + progress);
                }

                @Override
                public void waveformData(int serialNumber, int numberOfData, String waveformData, List<String[]> waveDataList) {
                    postView("\nWaveform: " + waveformData );
                }

                @Override
                public void error(int code, String message) {
                    postView("\nerror：" + code );
                }

                @Override
                public void success() {
                    postView("\nsuccess");
                }

                @Override
                public void stopQ2() {
                    postView("\nMeasurement stopped");
                }


            });
        }

        if(view.getId()==R.id.bt_heart) {
            postView("\nMeasuring heart rate");
            LmAPILite.GET_HEART_ROTA((byte) 0, (byte) 30, new IHeartListenerLite() {


                @Override
                public void progress(int progress) {
                    postView("\nProgress: " + progress);
                }

                @Override
                public void resultData(int heart, int heartRota, int yaLi, int temp) {
                    postView("\nHeart rate data: " + heart);
                }

                @Override
                public void waveformData(int serialNumber, int numberOfData, String waveData, List<String[]> waveDataList) {
                    tv_result.setText(waveData);
                }



                @Override
                public void rriData(int seq, int number, String data) {
                    postView("\nData value: " + data);
                }

                @Override
                public void error(int code, String message) {
                    postView("\nHeart rate error: " + code);
                }

                @Override
                public void success() {
                    postView("\nHeart rate done");
                }

                @Override
                public void stopHeart() {
                    postView("\nHeart rate stopped");
                }


            });
        }
            if(view.getId()==R.id.bt_gomore_test) {
                postView("\nGoMore sleep");
                //postView("\n开始读取未上传数据");
                List<GoMoreSleep> gomoreSleepList=new ArrayList<>();
                LmAPILite.GET_GOMORE_SLEEP(new IGoMoreListener() {
                    @Override
                    public void overviewOfSleep(GoMoreSleep goMoreSleep) {
//                        postView("\ngomore睡眠 overviewOfSleep:" + GsonUtils.beanToJson(goMoreSleep));
//                        Log.e(TAG, "overviewOfSleep: " + GsonUtils.beanToJson(goMoreSleep));
                        Gson gson = new Gson();
                        String json = gson.toJson(goMoreSleep);
                        gomoreSleepList.add(gson.fromJson(json, GoMoreSleep.class));
                    }

                    @Override
                    public void sleepStaging(GoMoreSleep goMoreSleep) {
//                        postView("\ngomore睡眠 sleepStaging:" + GsonUtils.beanToJson(goMoreSleep));
//                        Log.e(TAG, "sleepStaging: " + GsonUtils.beanToJson(goMoreSleep));
                        Gson gson = new Gson();
                        String json = gson.toJson(goMoreSleep);
                        gomoreSleepList.add(gson.fromJson(json, GoMoreSleep.class));
                    }

                    @Override
                    public void dataUploadFinish() {
                       postView("\nGoMore sleep: " + GsonUtils.beanToJson(gomoreSleepList));
                       //Log.e(TAG, "gomore睡眠: " + GsonUtils.beanToJson(gomoreSleepList));
                        ImageSaverUtil.saveImageToInternalStorage(TestActivity.this,"GoMore sleep: " + GsonUtils.beanToJson(gomoreSleepList),"LM","gomore.txt",true);
                    }

                    @Override
                    public void noSleepData() {
                        postView("\nGoMore sleep noSleepData");
                    }
                });
            }
        if(view.getId()==R.id.bt_read_log) {
            postView("\nReading all history");

            LmAPILite.READ_HISTORY((byte) 0x1, 0, new IHistoryListenerLite() {
                @Override
                public void error(int code) {

                }

                @Override
                public void success() {
                    postView("\nHistory read complete");

                }

                @Override
                public void progress(double progress, HistoryDataBean historyDataBean) {
                    if (historyDataBean != null) {
                        postView("\nHistory progress: " + progress + "%");
                        postView("\nRecord: " + historyDataBean);
                    }

                }

                @Override
                public void clearHistory() {

                }

                @Override
                public void noNewDataAvailable() {

                }


            });
        }
        if(view.getId()==R.id.bt_test_temp) {
            postView("\nGetting temperature");
            LmAPILite.READ_TEMP(new ITempListenerLite() {


                @Override
                public void resultData(int temp) {
                    postView("\nTemperature result: " + temp);
                }

                @Override
                public void testing(int temp) {

                }

                @Override
                public void error(int code) {
                    postView("\nerror:" + code);
                }
            });
        }
        if(view.getId()==R.id.bt_setSn) {
            postView("\nUncomment the code to set the correct serial number");
//                LmAPILite.SET_SERIAL("1234567890".getBytes(), iSystemControlListenerLite);
//                LmAPILite.SET_SN("", new ISNListener() {
//                    @Override
//                    public void getSn(String sn) {
//
//                    }
//
//                    @Override
//                    public void setSn(boolean success) {
//
//                    }
//                });
        }
        if(view.getId()==R.id.bt_getSn) {
            postView("\nGetting serial number");
            LmAPILite.GET_SN(new ISNListener() {
                @Override
                public void getInfo(String sn) {

                }

                @Override
                public void setInfo(boolean success) {

                }


            });

        }
        if(view.getId()==R.id.bt_setBluttoothName) {
            String btName = et_bluetooth_name.getText().toString().trim();
            if (TextUtils.isEmpty(btName)) {
                Toast.makeText(this, "Please enter a valid value", Toast.LENGTH_SHORT).show();
                return;
            }
            postView("\nSet Bluetooth name: " + btName);
            LmAPILite.Set_BlueTooth_Name(btName, iSystemControlListenerLite);
        }
        if(view.getId()==R.id.bt_getBluttoothName) {
            postView("\nGetting Bluetooth name");
            LmAPILite.Get_BlueTooth_Name(iSystemControlListenerLite);
        }
        if(view.getId()==R.id.bt_gomore_user) {
            int age = parseGomoreInput(et_gomore_age.getText().toString());
            int sex = parseGomoreInput(et_gomore_sex.getText().toString());
            int height = parseGomoreInput(et_gomore_height.getText().toString());
            int weight = parseGomoreInput(et_gomore_weight.getText().toString());
            //年龄0-99，性别0/1，身高100-220（小于100置为100），体重10-150
            if (age < 0 || age > 99 || (sex != 0 && sex != 1)
                    || height < 0 || height > 220 || weight < 10 || weight > 150) {
                Toast.makeText(this, "Please enter a valid value", Toast.LENGTH_SHORT).show();
                return;
            }
            if (height < 100) {
                height = 100;
            }
            postView("\nSet GoMore profile: " + age + "," + sex + "," + height + "," + weight);
            LmAPILite.SET_GOMORE_USER(age, sex, height, weight, -1, -1, -1, new IGoMoreUserListener() {
                @Override
                public void setUserInfoResult(boolean success) {
                    postView("\nSet GoMore profile: " + success);
                }

                @Override
                public void getUserInfoResult(int age, int sex, int height, int weight, int maximumHeartRate, int normalHeartRate, int maximalOxygenUptake) {

                }
            });
        }
        if(view.getId()==R.id.bt_get_gomore_user) {
            postView("\nGetting GoMore profile");
            LmAPILite.GET_GOMORE_USERINFO(new IGoMoreUserListener() {
                @Override
                public void setUserInfoResult(boolean success) {

                }

                @Override
                public void getUserInfoResult(int age, int sex, int height, int weight, int maximumHeartRate, int normalHeartRate, int maximalOxygenUptake) {
                    postView("\nGoMore profile: " + age + "," + sex + "," + height + "," + weight + "," + maximumHeartRate + "," + normalHeartRate + "," + maximalOxygenUptake);
                }
            });
        }
        if(view.getId()==R.id.tv_connect) {

            BLEUtils.connectLockByBLE(TestActivity.this, bluetoothDevice);
        }


        if(view.getId()==R.id.bt_setCollection) {
            int period = parseGomoreInput(et_collection_period.getText().toString());
            if (period <= 0) {
                Toast.makeText(this, "Please enter a valid value", Toast.LENGTH_SHORT).show();
                return;
            }
            lastSetPeriod = period;
            postView("\nSet collection interval: " + period + "sec");
            LmAPILite.SET_COLLECTION(period, iSystemControlListenerLite);
        }
        if(view.getId()==R.id.btn_getCollection) {

            LmAPILite.GET_COLLECTION(iSystemControlListenerLite);
        }
        if(view.getId()==R.id.bt_get_page2) {
            Intent intent=new Intent(TestActivity.this, TestActivity2.class);
            startActivity(intent);
        }
        if(view.getId()==R.id.bt_linear_motor) {
            Intent intent=new Intent(this,LinearMotorActivity.class);
            startActivity(intent);
        }

        if(view.getId()==R.id.btn_ota) {
            LmAPILite.GET_VERSION(true, new IVersionListenerLite() {
                @Override
                public void versionResult(String softwareVersion, String hardwareVersion) {
                    postView("\nWaiting for OTA update");
                    //提供给测试人员测试的方法，如果第三方需要对接，换成 OtaApi.otaUpdateWithCheckVersion，已经通过复合指令，获取到了版本号，不需要再调用LmAPILite.GET_VERSION
                    OtaApi.otaUpdateWithCheckVersionForTester(true,softwareVersion, TestActivity.this, App.getInstance().getDeviceBean().getDevice(), App.getInstance().getDeviceBean().getRssi(), new LmOtaProgressListener() {
                        @Override
                        public void error(String message) {
                            postView("\nOTA error: " + message);
                        }

                        @Override
                        public void onProgress(int i) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    postView("\nOTA progress: "+i);
                                }
                            });

                           Logger.show("OTA", "OTA update" + i);

                        }

                        @Override
                        public void onComplete() {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    postView("\nOTA finished");
                                }
                            });
                            Logger.show("OTA", "nOTA finished");
                            OtaApi.destoryOta(TestActivity.this);
                        }

                        @Override
                        public void isLatestVersion() {
                            postView("\nAlready latest version");
                        }

                        @Override
                        public void upgradeDescribe(String msg) {
                            postView("\nRelease notes: "+msg);
                        }
                    });
                }
            });



        }
        if(view.getId()==R.id.bt_new_protocol) {
            Intent intent=new Intent(this,NewProtocolActivity.class);
            startActivity(intent);
        }
        if(view.getId()==R.id.bt_touch_setting) {
            Intent intent=new Intent(this,TouchSettingActivity.class);
            startActivity(intent);
        }
    }



    /**
     * @param value 打印的log
     */
    public void postView(String value) {
        // tv_result.setText(value);
        tv_result.setMovementMethod(ScrollingMovementMethod.getInstance());
        tv_result.setScrollbarFadingEnabled(false);//滚动条一直显示
        tv_result.append(value);
        int scrollAmount = tv_result.getLayout().getLineTop(tv_result.getLineCount()) - tv_result.getHeight();
        if (scrollAmount > 0)
            tv_result.scrollTo(0, scrollAmount);
        else
            tv_result.scrollTo(0, 0);

    }

    /**
     * 解析输入框内容，空值或非数字返回-1
     */
    private int parseGomoreInput(String text) {
        if (TextUtils.isEmpty(text)) {
            return -1;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    private void READ_HISTORY_AUTO() {

        LmAPILite.READ_HISTORY_AUTO( new IHistoryListenerLite() {
            @Override
            public void error(int code) {
                if (code == 3) {
                    postView("\nBIX problem occurred");
                }
                // setMessage(TestActivity.this,"\n出现了BIX的问题");
            }

            @Override
            public void success() {
                postView("\nHistory read complete");


            }

            @Override
            public void progress(double progress, HistoryDataBean historyDataBean) {
                postView("\nHistory progress: " + progress + "%");
                postView("\nRecord: " + historyDataBean.toString());
            }

            @Override
            public void clearHistory() {

            }

            @Override
            public void noNewDataAvailable() {

            }


        });
    }

    // Long-press the output area to share the whole log as text (added for testing)
    private void enableShareLog(final android.widget.TextView tv) {
        tv.setOnLongClickListener(v -> {
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(android.content.Intent.EXTRA_SUBJECT, "Ring test log");
            i.putExtra(android.content.Intent.EXTRA_TEXT, tv.getText().toString());
            startActivity(android.content.Intent.createChooser(i, "Share ring log"));
            return true;
        });
    }
}
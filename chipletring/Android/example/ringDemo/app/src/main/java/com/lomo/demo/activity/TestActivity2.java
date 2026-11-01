package com.lomo.demo.activity;

import static com.lomo.demo.activity.TestActivity.mac;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.lm.sdk.AdPcmTool;
import com.lm.sdk.BLEService;
import com.lm.sdk.LmAPI;
import com.lm.sdk.LmAPILite;
import com.lm.sdk.LogicalApi;
import com.lm.sdk.OtaApi;
import com.lm.sdk.inter.FileResponseCallback;
import com.lm.sdk.inter.ICommonListener;
import com.lm.sdk.inter.IFileListListener;
import com.lm.sdk.inter.IHeartListener;
import com.lm.sdk.inter.IHistoryListener;
import com.lm.sdk.inter.IKeyDownListener;
import com.lm.sdk.inter.IRealTimePPGListener;
import com.lm.sdk.inter.IResponseListener;
import com.lm.sdk.inter.ITempListener;
import com.lm.sdk.inter.IWebHistoryResult;
import com.lm.sdk.inter.IWebSleepResult;
import com.lm.sdk.inter.LmOtaProgressListener;
import com.lm.sdk.lmApiInter.IAudioListenerLite;
import com.lm.sdk.lmApiInter.IHIDListenerLite;
import com.lm.sdk.lmApiInter.IHistoryListenerLite;
import com.lm.sdk.lmApiInter.INtcAdcListenerLite;
import com.lm.sdk.lmApiInter.IResponseListenerLite;
import com.lm.sdk.lmApiInter.IVersionListenerLite;
import com.lm.sdk.mode.AlarmParamBean;
import com.lm.sdk.mode.GestureSupport;
import com.lm.sdk.mode.HistoryDataBean;
import com.lm.sdk.mode.Sleep2thBean;
import com.lm.sdk.mode.SleepBatchBean;
import com.lm.sdk.mode.SleepBean;
import com.lm.sdk.mode.SystemControlBean;
import com.lm.sdk.mode.TouchSupport;
import com.lm.sdk.utils.BLEUtils;
import com.lm.sdk.utils.CMDUtils;
import com.lm.sdk.utils.DataCovertUtils;
import com.lm.sdk.utils.FileUtil;
import com.lm.sdk.utils.GoMoreUtils;
import com.lm.sdk.utils.Logger;
import com.lm.sdk.utils.UtilSharedPreference;
import com.lomo.demo.R;
import com.lomo.demo.application.App;
import com.lomo.demo.base.BaseActivity;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TestActivity2 extends BaseActivity implements IResponseListenerLite, View.OnClickListener {

    TextView tv_result2;
//    Button bt_calculate_sleep;
    Button bt_open_audio;
    Button bt_close_audio;
    private Handler handler = new Handler();  // 创建一个 Handler 实例
    private Runnable runnable;                 // 创建一个 Runnable 来定义任务
    String outputPath = com.lomo.demo.FileUtil.getSDPath(App.getInstance(), "Save" + ".pcm");
    private byte[] fileNameByteTest=new byte[]{};//测试文件系统功能的文件名

    private IHIDListenerLite ihidListenerLite=new IHIDListenerLite() {
        @Override
        public void setHIDResut(boolean success) {
            if(!success){
                postView("\nSet HID failed");
            }else {
                postView("\nSet HID succeeded");
            }
        }

        @Override
        public void getHIDInfo(int touchMode, int gestureMode, int system) {
            postView("\nCurrent touch HID mode: " + touchMode + "\nCurrent gesture HID mode: " + gestureMode + "\nCurrent system: " + system);
        }

        @Override
        public void getHidCode(boolean HIDSupport, TouchSupport touchSupport, GestureSupport gestureSupport) {
            postView("\nHIDSupport：" + HIDSupport + "\ntouchSupport：" + touchSupport + "\ngestureSupport：" + gestureSupport);
        }
    };
    private IAudioListenerLite audiolistenerLite = new IAudioListenerLite() {



        @Override
        public void controlAudioResult(byte[] bytes, int audioType, int samplingFrequency, int audioAccuracy) {
            if (recBuf.size() == 0) {
                recStartMs = System.currentTimeMillis();
                postView("\nReceiving audio: channels=" + audioType + ", rate code=" + samplingFrequency + ", bits code=" + audioAccuracy + ", chunk=" + bytes.length + " bytes");
            }
            recChannels = audioType;
            recBuf.write(bytes, 0, bytes.length);
            recChunks++;
            if (recChunks % 20 == 0) postView("\n...audio " + (recBuf.size() / 1024) + " KB received");
        }

        @Override
        public void controlAudioRawDataResult(int seq, byte[] bytes) {

        }

        @Override
        public void getControlAudioAdpcmResult(boolean adpcm) {

        }

        @Override
        public void pushAudioInformationResult(boolean success) {

        }

        @Override
        public void TOUCH_AUDIO_FINISH_XUN_FEI() {

        }

        @Override
        public void recordingResult(boolean result) {

        }

        @Override
        public void getOfflineRecordingStatus(int status) {

        }

        @Override
        public void timedRecordingResult(boolean success) {

        }

        @Override
        public void getTimedRecordingResult(int enabled, int intervalTime, int recordTime) {

        }

        @Override
        public void setSingleRecordingDurationResult(boolean success) {

        }

        @Override
        public void getSingleRecordingDurationResult(int duration) {

        }
    };
    FileResponseCallback fileResponseCallback=new FileResponseCallback() {
        @Override
        public void onFileListReceived(byte[] data) {
            byte[] fileCountByte = new byte[4];

            System.arraycopy(data, 4, fileCountByte, 0, fileCountByte.length);
            int fileCount = DataCovertUtils.byteArray2LittleInt(fileCountByte);

            byte[] fileIndexByte = new byte[4];

            System.arraycopy(data, 8, fileIndexByte, 0, fileCountByte.length);
            int fileIndex = DataCovertUtils.byteArray2LittleInt(fileIndexByte);

            byte[] fileSizeByte = new byte[4];
            System.arraycopy(data, 12, fileSizeByte, 0, fileSizeByte.length);
            int fileSize = DataCovertUtils.byteArray2LittleInt(fileSizeByte) / 1024;

            byte[] fileNameByte = new byte[data.length-16];
            System.arraycopy(data, 16, fileNameByte, 0, fileNameByte.length);
            String fileName = new String(fileNameByte, StandardCharsets.UTF_8);
            Logger.show("fileSize", fileSize + "");
            Logger.show("result", fileName);
            if(fileIndex==1){
                fileNameByteTest=fileNameByte;//第一个文件，作为测试数据
            }
            postView("\nonFileListReceived：" + "fileCount：" + fileCount + ",fileIndex：" + fileIndex + ",fileSize：" + fileSize + ",fileName：" + fileName);
        }

        @Override
        public void onFileInfoReceived(byte[] data) {
            postView("\nonFileInfoReceived");
        }

        @Override
        public void onFileDelete(boolean success, int deleteStatus) {
            postView("\nonFileDelete,success:"+success);
        }

        @Override
        public void onFileDownloadEndReceived(byte[] data) {
            postView("onFileDownloadEndReceived");
        }

        @Override
        public void onDownloadAllFileProgress(byte[] data) {
            postView("\nonDownloadAllFileProgress");
        }

        @Override
        public void oneFileDownloadSuccess() {
            postView("\noneFileDownloadSuccess");
        }

        @Override
        public void onDownloadStatusReceived(byte[] data) {
            postView("\nonDownloadStatusReceived");
        }

        @Override
        public void onFileDataReceived(byte[] data) {
          //  postView("\nonFileDataReceived");
        }

        @Override
        public void onFileState(int data) {
            postView("\nonFileState");
        }

        @Override
        public void onFilePushFileName(byte[] data) {
            postView("\nonFilePushFileName");
        }

        @Override
        public void onFilePushFileData(byte[] data) {
            postView("\nonFilePushFileData");
        }

        @Override
        public void onFileResumeBreakpoint(int state, long startTime, long endTime) {
            postView("\nonFileResumeBreakpoint,state:"+state+",startTime:"+startTime+",endTime:"+endTime);
        }

        @Override
        public void onFileResumeBreakpointProgress(int progress) {
            postView("\nonFileResumeBreakpointProgress:"+progress);
        }

        @Override
        public void localMemoryFull(int capacitySize, int capacitySizeUsed, int capacitySizeNotUsed) {
            postView("\nlocalMemoryFull,capacitySize:"+capacitySize+",capacitySizeUsed:"+capacitySizeUsed+",capacitySizeNotUsed:"+capacitySizeNotUsed);
        }
    };
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test2);
        tv_result2 = findViewById(R.id.tv_result2); enableShareLog(tv_result2);
        findViewById(R.id.bt_unpair).setOnClickListener(this);
        findViewById(R.id.bt_set_HID).setOnClickListener(this);
        findViewById(R.id.bt_get_HID).setOnClickListener(this);
        findViewById(R.id.bt_get_HID_code).setOnClickListener(this);
        findViewById(R.id.bt_set_audio_type).setOnClickListener(this);
        findViewById(R.id.bt_stop_audio).setOnClickListener(this);
        findViewById(R.id.bt_get_audio_type).setOnClickListener(this);

        findViewById(R.id.bt_get_rssi).setOnClickListener(this);


        findViewById(R.id.bt_ecg_demo).setOnClickListener(this);
        findViewById(R.id.bt_sleep_sevice).setOnClickListener(this);



        findViewById(R.id.bt_file_list).setOnClickListener(this);
        findViewById(R.id.bt_file_content).setOnClickListener(this);
        findViewById(R.id.bt_test2).setOnClickListener(this);
        findViewById(R.id.bt_star_ppg).setOnClickListener(this);
        findViewById(R.id.bt_stop_ppg).setOnClickListener(this);
        findViewById(R.id.bt_testGomore).setOnClickListener(this);
        findViewById(R.id.btn_touch_test_open).setOnClickListener(this);
        findViewById(R.id.btn_touch_test_close).setOnClickListener(this);
        findViewById(R.id.bt_test_vibration).setOnClickListener(this);
        findViewById(R.id.bt_resume_breakpoint).setOnClickListener(this);
        findViewById(R.id.bt_get_ntc_adc).setOnClickListener(this);


        /**
         * 获取戒指主动推送的按键信息
         * @param key
         * 0x0:长按
         * 0x1:单击
         * 0x2:双击
         * 0x3:三击
         * 0x4:上滑
         * 0x5:下滑
         * 0x6:左滑
         * 0x7:右滑
         */
        LmAPILite.KEY_DOWN_LISTENER(new IKeyDownListener() {
            @Override
            public void ringPushKeyDownResult(int key) {

                postView("Touch test: "+key+"\n");
            }
        });

    }



    @Override
    public void lmBleConnecting(int code) {

    }

    @Override
    public void lmBleConnectionSucceeded(int code) {

    }

    @Override
    public void lmBleConnectionFailed(int code) {

    }


    @Override
    public void saveData(String str_data) {

    }




    public static String byteToBitString(byte b) {
        StringBuilder bitString = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            bitString.append((b >> i) & 1); // 移位并与1进行与操作，获取最低位的bit
        }
        return bitString.toString();
    }





    @Override
    public void onClick(View v) {
        if(v.getId()== R.id.bt_set_HID){
            LmAPILite.SET_HID(0,-1,TestActivity2.this,ihidListenerLite);
        }

        if(v.getId()== R.id.bt_get_HID){
            LmAPILite.GET_HID(ihidListenerLite);//获取HID现在的模式
        }

        if(v.getId()== R.id.bt_get_HID_code){
            LmAPILite.GET_HID_CODE((byte)0x00,ihidListenerLite);  //系统类型 0：安卓  1：IOS  2：windows
        }

        if(v.getId()== R.id.bt_set_audio_type){
            recBuf.reset(); recChunks = 0;
            postView("\nRecording... speak now, then tap STOP RECORDING");
            LmAPILite.CONTROL_AUDIO_ADPCM((byte)0x01,audiolistenerLite);
        }
        if(v.getId()== R.id.bt_get_audio_type){
            LmAPILite.GET_CONTROL_AUDIO_ADPCM(audiolistenerLite);
        }
        if(v.getId()== R.id.bt_stop_audio){
            LmAPILite.CONTROL_AUDIO_ADPCM((byte)0x0,audiolistenerLite);
            saveRecording();
        }


        if(v.getId()== R.id.bt_unpair){
            postView("\nUnbound\n");
            BLEUtils.setGetToken(false);
            BLEUtils.disconnectBLE(this);
            BLEUtils.removeBond(BLEService.getmBluetoothDevice());
            UtilSharedPreference.saveString(TestActivity2.this,"address","");
            Intent intent = new Intent(TestActivity2.this, MainActivity.class);

            startActivity(intent);
            finish();
        }

        if(v.getId()== R.id.bt_get_rssi){
            BLEService.readRomoteRssi();
            postView("\nrssi == "+ BLEService.RSSI);
        }


           if(v.getId()==R.id.bt_ecg_demo) {


               LogicalApi.startECGActivity(TestActivity2.this);
           }
        if(v.getId()==R.id.bt_sleep_sevice) {

            String dateTimeString = "2025-02-15 23:59:59";
            LogicalApi.getSleepDataFromService(dateTimeString, new IWebSleepResult() {
                @Override
                public void sleepDataSuccess(Sleep2thBean sleep2thBean) {
                    // 定义日期时间格式
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    // 将时间戳转换为 Date 对象
                    Date startDate = new Date(sleep2thBean.getStartTime() * 1000);
                    // 将时间戳转换为 Date 对象
                    Date endDate = new Date(sleep2thBean.getEndTime() * 1000);
                    postView("\nSleep hours: " + sleep2thBean.getHours() + "\nSleep minutes: " + sleep2thBean.getMinutes() + "\nStart and end times come from the drawing filter, see 3.5.3 Sleep chart");

                }

                @Override
                public void error(String message) {

                }

                @Override
                public void sleepDataBatchSuccess(List<SleepBatchBean> sleepBeanList) {

                }


            });
        }


        if(v.getId()==R.id.bt_file_list) {

            LmAPILite.GET_FILE_LIST(fileResponseCallback);
        }
        if(v.getId()==R.id.bt_file_content) {

            LmAPILite.DOWNLOAD_FILE(fileNameByteTest, fileResponseCallback);
        }
        if(v.getId()==R.id.bt_resume_breakpoint) {

            LmAPILite.RESUME_BREAKPOINT(20, fileNameByteTest,fileResponseCallback);
        }
        if(v.getId()==R.id.bt_test_vibration) {
            //设置参数
            LmAPILite.SET_MOTOR_LINEAR(1, 144, 5, 5, new ICommonListener() {
                @Override
                public void success() {
                    //进行震动
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LmAPILite.MOTOR_VIBRATION_TEST();
                        }
                    },200);

                }

                @Override
                public void error() {

                }
            });
        }

        if(v.getId()==R.id.bt_test2){

            Intent intent = new Intent();
                intent.setClass(TestActivity2.this,TestActivity3.class);
                startActivity(intent);

        }

        if(v.getId()==R.id.bt_star_ppg){

            postView("\nStarting raw PPG");
            //postView("\n开始读取未上传数据");
            final int[] numCount = {0};
            LmAPILite.START_REAL_TIME_PPG(40, 100, 20, 20, 20, 1, 1, new IRealTimePPGListener() {
                @Override
                public void time(long time, int zone) {
                    postView("\ntime:"+time+",zone:"+zone);
                }

                @Override
                public void waveformData(int seq, int number, List<String[]> waveData) {
                    numCount[0]=numCount[0]+waveData.size();
                    postView("\nwaveformData seq:"+seq+",number:"+number+", total samples: "+numCount[0]);

//                    for (String[] array : waveData) {
//                        postView("\nwaveData:"+Arrays.toString(array));
//                    }
                }

                @Override
                public void progress(int progress) {
                    postView("\nprogress :"+progress);
                }

                @Override
                public void RRIData(int number, byte[] rriData) {
                    postView("\nRRIData number:"+number+",rriData length:"+rriData.length);
                }

                @Override
                public void result(int result0, int heartRate, int bloodOxygen, int temperature) {
                    postView("\nresult result0:"+result0+",heartRate:"+heartRate+",bloodOxygen:"+bloodOxygen+",temperature:"+temperature);
                }
            });

        }

        if(v.getId()==R.id.bt_stop_ppg){

            postView("\nRaw PPG stopped");
            //postView("\n开始读取未上传数据");

            LmAPILite.STOP_REAL_TIME_PPG();

        }
        if(v.getId()==R.id.bt_testGomore){

            postView("\nGoMore ring licence");
            GoMoreUtils.goMoreAuthorizationKey(mac,"76d07e37bfe341b1a25c76c0e25f457a",new GoMoreUtils.IGomoreListener() {
                @Override
                public void authorization() {
                    postView("\nauthorization");

                }

                @Override
                public void unauthorization() {
                    postView("\nunauthorization");
                }

                @Override
                public void error(int code, String msg) {
                    postView("\nerror:"+msg);
                }


            });

        }

        if(v.getId()==R.id.btn_touch_test_open){

            LmAPILite.OPEN_TOUCH_TEST();

        }
        if(v.getId()==R.id.btn_touch_test_close){

            LmAPILite.CLOSE_TOUCH_TEST();

        }
        if(v.getId()==R.id.bt_get_ntc_adc){

            postView("\nRead multiple NTC ADC values");
            LmAPILite.GET_NTC_ADC(new INtcAdcListenerLite() {
                @Override
                public void onNtcAdcResult(int ntcCount, int[] ntcValues) {
                    StringBuilder sb = new StringBuilder("\nTemperature count: " + ntcCount);
                    for (int i = 0; i < ntcCount; i++) {
                        float temp = ntcValues[i] / 100.0f;
                        sb.append("\nTemperature ").append(i).append(": ").append(temp).append("℃");
                    }
                    postView(sb.toString());
                }
            });
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    /**
     * @param value 打印的log
     */
    public void postView(String value) {
        tv_result2.setMovementMethod(ScrollingMovementMethod.getInstance());
        tv_result2.setScrollbarFadingEnabled(false);//滚动条一直显示
        tv_result2.append(value);
        int scrollAmount = tv_result2.getLayout().getLineTop(tv_result2.getLineCount()) - tv_result2.getHeight();
        if (scrollAmount > 0)
            tv_result2.scrollTo(0, scrollAmount);
        else
            tv_result2.scrollTo(0, 0);

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

    // ---- Added for testing: keep the decoded audio and save it as a WAV file in Downloads ----
    private final java.io.ByteArrayOutputStream recBuf = new java.io.ByteArrayOutputStream();
    private long recStartMs = 0;
    private int recChunks = 0;
    private int recChannels = 1;

    private void saveRecording() {
        final byte[] pcm = recBuf.toByteArray();
        if (pcm.length == 0) { postView("\nNo audio received. Try START RECORDING again."); return; }
        double secs = Math.max(0.5, (System.currentTimeMillis() - recStartMs) / 1000.0);
        int ch = Math.max(1, recChannels);
        double measured = pcm.length / 2.0 / ch / secs;
        int rate = measured > 12000 ? 16000 : 8000;   // the SDK hands us 16-bit PCM
        String name = "ring-voice-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(new java.util.Date()) + ".wav";
        try {
            byte[] wav = wavBytes(pcm, rate, ch);
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name);
                cv.put(android.provider.MediaStore.Downloads.MIME_TYPE, "audio/wav");
                android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                try (java.io.OutputStream os = getContentResolver().openOutputStream(uri)) { os.write(wav); }
            } else {
                java.io.File f = new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), name);
                try (java.io.FileOutputStream os = new java.io.FileOutputStream(f)) { os.write(wav); }
            }
            postView(String.format(java.util.Locale.US,
                "\nSAVED %s in Downloads: %.1f s recorded, %d KB, %d Hz (measured %.0f), %d channel(s)",
                name, secs, pcm.length / 1024, rate, measured, ch));
        } catch (Exception e) {
            postView("\nCould not save audio: " + e);
        }
        recBuf.reset(); recChunks = 0;
    }

    private static byte[] wavBytes(byte[] pcm, int rate, int ch) {
        java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(44 + pcm.length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()).putInt(36 + pcm.length).put("WAVE".getBytes());
        b.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) ch).putInt(rate)
         .putInt(rate * ch * 2).putShort((short) (ch * 2)).putShort((short) 16);
        b.put("data".getBytes()).putInt(pcm.length).put(pcm);
        return b.array();
    }
}

package com.lomo.demo.activity;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.lm.sdk.LmAPILite;
import com.lm.sdk.lmApiInter.IAlarmClockListenerLite;
import com.lm.sdk.lmApiInter.IAlarmParamListenerLite;
import com.lm.sdk.lmApiInter.IBloodOxygenCollectionListenerLite;
import com.lm.sdk.lmApiInter.IBloodOxygenModeListenerLite;
import com.lm.sdk.lmApiInter.IElectricityMeterListenerLite;
import com.lm.sdk.lmApiInter.ILightEffectListenerLite;
import com.lm.sdk.lmApiInter.IVibrationEffectListenerLite;
import com.lm.sdk.mode.AlarmClockBean;
import com.lm.sdk.mode.AlarmParamBean;
import com.lm.sdk.mode.HolidayResult;
import com.lm.sdk.mode.LightEffectBean;
import com.lm.sdk.mode.VibrationEffectBean;
import com.lomo.demo.R;
import com.lomo.demo.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 新增协议测试Activity
 * 包含灯光特效、震动特效、戒指控制、心率压力报警参数的测试
 */
public class NewProtocolActivity extends BaseActivity implements  View.OnClickListener {
    public String TAG = getClass().getSimpleName();
    TextView tv_result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_protocol);


        tv_result = findViewById(R.id.tv_result);

        // 灯光特效
        findViewById(R.id.bt_set_light_effect).setOnClickListener(this);
        findViewById(R.id.bt_get_light_effect).setOnClickListener(this);
        // 震动特效
        findViewById(R.id.bt_set_vibration_effect).setOnClickListener(this);
        findViewById(R.id.bt_get_vibration_effect).setOnClickListener(this);
        // 戒指控制
        findViewById(R.id.bt_start_find_ring).setOnClickListener(this);
        findViewById(R.id.bt_stop_find_ring).setOnClickListener(this);
        findViewById(R.id.bt_start_dnd).setOnClickListener(this);
        findViewById(R.id.bt_stop_dnd).setOnClickListener(this);
        findViewById(R.id.bt_start_always_on).setOnClickListener(this);
        findViewById(R.id.bt_stop_always_on).setOnClickListener(this);
        findViewById(R.id.bt_start_light_vibration).setOnClickListener(this);
        findViewById(R.id.bt_stop_light_vibration).setOnClickListener(this);
        // 心率压力报警
        findViewById(R.id.bt_set_alarm_param).setOnClickListener(this);
        findViewById(R.id.bt_get_alarm_param).setOnClickListener(this);
        // 闹钟设置
        findViewById(R.id.bt_set_alarm_clock).setOnClickListener(this);
        findViewById(R.id.bt_get_alarm_clock).setOnClickListener(this);
        // 电量计信息
        findViewById(R.id.bt_get_electricity_meter).setOnClickListener(this);
        // 血氧模式
        findViewById(R.id.bt_set_blood_oxygen_mode).setOnClickListener(this);
        findViewById(R.id.bt_get_blood_oxygen_mode).setOnClickListener(this);
        // 血氧采集周期
        findViewById(R.id.bt_set_blood_oxygen_collection).setOnClickListener(this);
        findViewById(R.id.bt_get_blood_oxygen_collection).setOnClickListener(this);



    }



    @Override
    protected void onDestroy() {
        super.onDestroy();


    }


    @Override
    public void onClick(View view) {
        // ==================== 灯光特效 ====================
        if (view.getId() == R.id.bt_set_light_effect) {
            postView("\nSet light effect");
            List<LightEffectBean> lightEffects = new ArrayList<>();
            LightEffectBean effect = new LightEffectBean();
            effect.setEffectIndex(0);
            // 第一段模式（模式0：无；模式1：闪烁；模式2：呼吸）
            effect.setFirstMode(2); // 呼吸
            effect.setFirstLoopCount(0); // 无限循环
            effect.setFirstStartBrightness(50);
            effect.setFirstMiddleBrightness(200);
            effect.setFirstEndBrightness(50);
            effect.setFirstRiseStepDelay(1000); // 上升1秒
            effect.setFirstFallStepDelay(1000); // 下降1秒
            // 第二段模式
            effect.setSecondMode(1); // 闪烁
            effect.setSecondLoopCount(3); // 循环3次
            effect.setSecondStartBrightness(100);
            effect.setSecondMiddleBrightness(255);
            effect.setSecondEndBrightness(0);
            effect.setSecondRiseStepDelay(500); // 上升0.5秒
            effect.setSecondFallStepDelay(500); // 下降0.5秒
            lightEffects.add(effect);

            LmAPILite.SET_LIGHT_EFFECT(lightEffects, new ILightEffectListenerLite() {
                @Override
                public void result(List<LightEffectBean> lightEffectBeanList) {
                    if (lightEffectBeanList != null) {
                        postView("\nLight effects, count: " + lightEffectBeanList.size());
                        for (LightEffectBean e : lightEffectBeanList) {
                            postView("\n  #" + e.getEffectIndex());
                            postView("\n  Part 1 mode: " + e.getFirstMode()
                                    + ", loops: " + e.getFirstLoopCount()
                                    + ", start brightness: " + e.getFirstStartBrightness()
                                    + ", mid brightness: " + e.getFirstMiddleBrightness()
                                    + ", end brightness: " + e.getFirstEndBrightness()
                                    + ", rise delay: " + e.getFirstRiseStepDelay() + "ms"
                                    + ", fall delay: " + e.getFirstFallStepDelay() + "ms");
                            postView("\n  Part 2 mode: " + e.getSecondMode()
                                    + ", loops: " + e.getSecondLoopCount()
                                    + ", start brightness: " + e.getSecondStartBrightness()
                                    + ", mid brightness: " + e.getSecondMiddleBrightness()
                                    + ", end brightness: " + e.getSecondEndBrightness()
                                    + ", rise delay: " + e.getSecondRiseStepDelay() + "ms"
                                    + ", fall delay: " + e.getSecondFallStepDelay() + "ms");
                        }
                    } else {
                        postView("\nNo light effects");
                    }
                }

                @Override
                public void setLightEffectResult(boolean success) {
                    postView("\nSet light effect result: " + (success ? "Success" : "Failed"));
                }
            });
        }

        if (view.getId() == R.id.bt_get_light_effect) {
            postView("\nGet light effect");
            LmAPILite.GET_LIGHT_EFFECT(new ILightEffectListenerLite() {
                @Override
                public void result(List<LightEffectBean> lightEffectBeanList) {
                    if (lightEffectBeanList != null) {
                        postView("\nLight effects, count: " + lightEffectBeanList.size());
                        for (LightEffectBean e : lightEffectBeanList) {
                            postView("\n  #" + e.getEffectIndex());
                            postView("\n  Part 1 mode: " + e.getFirstMode()
                                    + ", loops: " + e.getFirstLoopCount()
                                    + ", start brightness: " + e.getFirstStartBrightness()
                                    + ", mid brightness: " + e.getFirstMiddleBrightness()
                                    + ", end brightness: " + e.getFirstEndBrightness()
                                    + ", rise delay: " + e.getFirstRiseStepDelay() + "ms"
                                    + ", fall delay: " + e.getFirstFallStepDelay() + "ms");
                            postView("\n  Part 2 mode: " + e.getSecondMode()
                                    + ", loops: " + e.getSecondLoopCount()
                                    + ", start brightness: " + e.getSecondStartBrightness()
                                    + ", mid brightness: " + e.getSecondMiddleBrightness()
                                    + ", end brightness: " + e.getSecondEndBrightness()
                                    + ", rise delay: " + e.getSecondRiseStepDelay() + "ms"
                                    + ", fall delay: " + e.getSecondFallStepDelay() + "ms");
                        }
                    } else {
                        postView("\nNo light effects");
                    }
                }

                @Override
                public void setLightEffectResult(boolean success) {
                    postView("\nSet light effect result: " + (success ? "Success" : "Failed"));
                }
            });
        }

        // ==================== 震动特效 ====================
        if (view.getId() == R.id.bt_set_vibration_effect) {
            postView("\nSet vibration effect");
            List<VibrationEffectBean> vibrationEffects = new ArrayList<>();
            VibrationEffectBean effect = new VibrationEffectBean();
            effect.setEffectIndex((byte) 0);
            effect.setLoopCount((byte) 3); // 循环3次
            effect.setIntensity((byte) 80); // 震动强度
            effect.setVibrationDelay(500); // 震动500ms
            effect.setPauseDelay(200); // 暂停200ms
            vibrationEffects.add(effect);

            LmAPILite.SET_VIBRATION_EFFECT(vibrationEffects, new IVibrationEffectListenerLite() {
                @Override
                public void result(List<VibrationEffectBean> vibrationEffectBeanList) {
                    if (vibrationEffectBeanList != null) {
                        postView("\nVibration effects, count: " + vibrationEffectBeanList.size());
                        for (VibrationEffectBean e : vibrationEffectBeanList) {
                            postView("\n  #" + e.getEffectIndex()
                                    + ", loops: " + e.getLoopCount()
                                    + ", strength: " + e.getIntensity()
                                    + ", vibrate delay: " + e.getVibrationDelay() + "ms"
                                    + ", pause delay: " + e.getPauseDelay() + "ms");
                        }
                    } else {
                        postView("\nNo vibration effects");
                    }
                }

                @Override
                public void setVibrationEffectResult(boolean success) {
                    postView("\nSet vibration effect result: " + (success ? "Success" : "Failed"));
                }
            });
        }

        if (view.getId() == R.id.bt_get_vibration_effect) {
            postView("\nGet vibration effect");
            LmAPILite.GET_VIBRATION_EFFECT(new IVibrationEffectListenerLite() {
                @Override
                public void result(List<VibrationEffectBean> vibrationEffectBeanList) {
                    if (vibrationEffectBeanList != null) {
                        postView("\nVibration effects, count: " + vibrationEffectBeanList.size());
                        for (VibrationEffectBean e : vibrationEffectBeanList) {
                            postView("\n  #" + e.getEffectIndex()
                                    + ", loops: " + e.getLoopCount()
                                    + ", strength: " + e.getIntensity()
                                    + ", vibrate delay: " + e.getVibrationDelay() + "ms"
                                    + ", pause delay: " + e.getPauseDelay() + "ms");
                        }
                    } else {
                        postView("\nNo vibration effects");
                    }
                }

                @Override
                public void setVibrationEffectResult(boolean success) {
                    postView("\nSet vibration effect result: " + (success ? "Success" : "Failed"));
                }
            });
        }

        // ==================== 戒指控制 ====================
        if (view.getId() == R.id.bt_start_find_ring) {
            postView("\nStart find-ring");
            LmAPILite.START_FIND_RING();
        }

        if (view.getId() == R.id.bt_stop_find_ring) {
            postView("\nStop find-ring");
            LmAPILite.STOP_FIND_RING();
        }

        if (view.getId() == R.id.bt_start_dnd) {
            postView("\nStart do-not-disturb");
            LmAPILite.START_DND_MODE();
        }

        if (view.getId() == R.id.bt_stop_dnd) {
            postView("\nStop do-not-disturb");
            LmAPILite.STOP_DND_MODE();
        }

        if (view.getId() == R.id.bt_start_always_on) {
            postView("\nStart always-on");
            LmAPILite.START_ALWAYS_ON();
        }

        if (view.getId() == R.id.bt_stop_always_on) {
            postView("\nStop always-on");
            LmAPILite.STOP_ALWAYS_ON();
        }

        if (view.getId() == R.id.bt_start_light_vibration) {
            postView("\nStart light+vibration effect");
            byte vibrationIndex = 0; // 震动特效序号
            byte lightIndex = 0; // 灯光特效序号
            LmAPILite.START_LIGHT_VIBRATION_EFFECT(vibrationIndex, lightIndex);
        }

        if (view.getId() == R.id.bt_stop_light_vibration) {
            postView("\nStop light+vibration effect");
            LmAPILite.STOP_LIGHT_VIBRATION_EFFECT();
        }

        // ==================== 心率压力报警参数 ====================
        if (view.getId() == R.id.bt_set_alarm_param) {
            postView("\nSet HR/stress alarm thresholds");
            AlarmParamBean alarmParam = new AlarmParamBean();
            alarmParam.setAlarmEnable((byte) 0x03); // 允许心率压力报警
            alarmParam.setHeartRateThreshold((byte) 120); // 心率阈值120
            alarmParam.setStressThreshold((byte) 80); // 压力阈值80
            alarmParam.setWalkDelayMinutes((byte) 5); // 走路后5分钟报警
            alarmParam.setRunDelayMinutes((byte) 3); // 跑步后3分钟报警

            LmAPILite.SET_ALARM_PARAM(alarmParam);
            postView("\nSent alarm-threshold command");
        }

        if (view.getId() == R.id.bt_get_alarm_param) {
            postView("\nRead HR/stress alarm thresholds");
            LmAPILite.GET_ALARM_PARAM(new IAlarmParamListenerLite() {
                @Override
                public void result(AlarmParamBean alarmParamBean) {
                    if (alarmParamBean != null) {
                        String alarmEnableStr = "";
                        switch (alarmParamBean.getAlarmEnable()) {
                            case 0x00:
                                alarmEnableStr = "Alarms off";
                                break;
                            case 0x01:
                                alarmEnableStr = "HR alarm on";
                                break;
                            case 0x02:
                                alarmEnableStr = "Stress alarm on";
                                break;
                            case 0x03:
                                alarmEnableStr = "HR + stress alarms on";
                                break;
                        }
                        postView("\nAlarm: " + alarmEnableStr);
                        postView("\nHR threshold: " + alarmParamBean.getHeartRateThreshold());
                        postView("\nStress threshold: " + alarmParamBean.getStressThreshold());
                        postView("\nWalking delay: " + alarmParamBean.getWalkDelayMinutes() + "min");
                        postView("\nRunning delay: " + alarmParamBean.getRunDelayMinutes() + "min");
                    }
                }

            });
        }

        // ==================== 闹钟设置 ====================
        if (view.getId() == R.id.bt_set_alarm_clock) {
            postView("\nSet alarm clock");
            List<AlarmClockBean> setAlarmClockData = new ArrayList<>();
         for (int i = 0; i < 5; i++) {
             AlarmClockBean alarmClockBean = new AlarmClockBean();
             if(i==0){//测试只设置一个闹铃的情况
                 alarmClockBean.setOnOrOff((byte) 1);
                 alarmClockBean.setVibrationEffect((byte) 0);
                 alarmClockBean.setTime(1788328680);
                 alarmClockBean.setRepetitiveType((byte) 0);
                 alarmClockBean.setWeekday(new int[]{});
             }else{//其他补0
                 alarmClockBean.setOnOrOff((byte) 0);
                 alarmClockBean.setVibrationEffect((byte) 0);
                 alarmClockBean.setTime(0);
                 alarmClockBean.setRepetitiveType((byte) 0);
                 alarmClockBean.setWeekday(new int[]{});
             }


                setAlarmClockData.add(alarmClockBean);
           }
            LmAPILite.ALARM_CLOCK_SETTING(setAlarmClockData, new IAlarmClockListenerLite() {
                @Override
                public void result(List<AlarmClockBean> alarmClockBeanList) {

                }

                @Override
                public void holidayResult(HolidayResult holidayResult) {

                }

                @Override
                public void setAlarmResult(boolean success) {
                    postView("\nsetAlarmResult："+success);
                }

                @Override
                public void setHolidayResult(boolean success) {

                }
            });
            postView("\nSent alarm-clock command");
        }

        if (view.getId() == R.id.bt_get_alarm_clock) {
            postView("\nGet alarm clock");
            LmAPILite.GET_ALARM_CLOCK(new IAlarmClockListenerLite() {
                @Override
                public void result(List<AlarmClockBean> alarmClockBeanList) {
                    if (alarmClockBeanList != null && !alarmClockBeanList.isEmpty()) {
                        postView("\nAlarm clocks: " + alarmClockBeanList.size());
                        for (AlarmClockBean alarmClockBean : alarmClockBeanList) {
                            if (alarmClockBean.getTime() > 0) {
                                postView("\n  Timestamp: " + alarmClockBean.getTime()
                                        + ", repeat: " + alarmClockBean.getRepetitiveType()
                                        + ", vibration: " + alarmClockBean.getVibrationEffect()
                                        + ", on/off: " + alarmClockBean.getOnOrOff()
                                        + ", light: " + alarmClockBean.getLightingEffect());
                            }
                        }
                    } else {
                        postView("\nNo alarm clocks");
                    }
                }

                @Override
                public void holidayResult(HolidayResult holidayResult) {
                }

                @Override
                public void setAlarmResult(boolean success) {
                    postView("\nSet alarm clock result: " + (success ? "Success" : "Failed"));
                }

                @Override
                public void setHolidayResult(boolean success) {
                }
            });
        }

        // ==================== 电量计信息 ====================
        if (view.getId() == R.id.bt_get_electricity_meter) {
            postView("\nRead fuel-gauge info");
            LmAPILite.GET_ELECTRICITY_METER(new IElectricityMeterListenerLite() {
                @Override
                public void battery(int power, int voltage, int current, int temperature, int cyclesNumber, int health) {
                    postView("\nBattery: " + power + "%");
                    postView("\nVoltage: " + voltage + "mV");
                    postView("\nCurrent: " + current + "mA");
                    postView("\nTemperature: " + temperature + "℃");
                    postView("\nCycles: " + cyclesNumber);
                    postView("\nHealth: " + health + "%");
                }
            });
        }

        // ==================== 血氧模式 ====================
        if (view.getId() == R.id.bt_set_blood_oxygen_mode) {
            postView("\nSet SpO2 mode: on");
            LmAPILite.SET_BLOOD_OXYGEN_MODE(1, new IBloodOxygenModeListenerLite() {
                @Override
                public void setBloodOxygenModeResult(boolean success) {
                    postView("\nSet SpO2 mode result: " + (success ? "Success" : "Failed"));
                }

                @Override
                public void getBloodOxygenModeResult(int mode) {
                }
            });
        }

        if (view.getId() == R.id.bt_get_blood_oxygen_mode) {
            postView("\nRead SpO2 mode");
            LmAPILite.GET_BLOOD_OXYGEN_MODE(new IBloodOxygenModeListenerLite() {
                @Override
                public void setBloodOxygenModeResult(boolean success) {
                }

                @Override
                public void getBloodOxygenModeResult(int mode) {
                    postView("\nCurrent SpO2 mode: " + (mode == 1 ? "On" : "Off"));
                }
            });
        }

        // ==================== 血氧采集周期 ====================
        if (view.getId() == R.id.bt_set_blood_oxygen_collection) {
            postView("\nSet SpO2 interval: awake=2, sleep=4");
            LmAPILite.SET_BLOOD_OXYGEN_COLLECTION(2, 4, new IBloodOxygenCollectionListenerLite() {
                @Override
                public void setBloodOxygenCollectionResult(boolean success) {
                    postView("\nSet interval result: " + (success ? "Success" : "Failed"));
                }

                @Override
                public void getBloodOxygenCollectionResult(int nonSleepMultiplier, int sleepMultiplier) {
                }
            });
        }

        if (view.getId() == R.id.bt_get_blood_oxygen_collection) {
            postView("\nRead SpO2 interval");
            LmAPILite.GET_BLOOD_OXYGEN_COLLECTION(new IBloodOxygenCollectionListenerLite() {
                @Override
                public void setBloodOxygenCollectionResult(boolean success) {
                }

                @Override
                public void getBloodOxygenCollectionResult(int nonSleepMultiplier, int sleepMultiplier) {
                    postView("\nAwake multiplier: " + nonSleepMultiplier);
                    postView("\nSleep multiplier: " + sleepMultiplier);
                }
            });
        }

    }

    /**
     * 打印日志到界面
     */
    public void postView(String value) {
        tv_result.setMovementMethod(ScrollingMovementMethod.getInstance());
        tv_result.setScrollbarFadingEnabled(false);
        tv_result.append(value);
        int scrollAmount = tv_result.getLayout().getLineTop(tv_result.getLineCount()) - tv_result.getHeight();
        if (scrollAmount > 0)
            tv_result.scrollTo(0, scrollAmount);
        else
            tv_result.scrollTo(0, 0);
    }
}

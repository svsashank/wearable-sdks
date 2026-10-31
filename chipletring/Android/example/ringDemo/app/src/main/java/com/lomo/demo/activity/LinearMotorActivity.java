package com.lomo.demo.activity;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lm.sdk.LmAPILite;
import com.lm.sdk.lmApiInter.IConfigVibrationControlListenerLite;
import com.lm.sdk.lmApiInter.ILinearMotorCountListenerLite;
import com.lm.sdk.lmApiInter.ILinearMotorTargetListenerLite;
import com.lm.sdk.lmApiInter.IMinCountIntervalListenerLite;
import com.lm.sdk.lmApiInter.IVibrationConfigListenerLite;
import com.lm.sdk.lmApiInter.IVibrationControlListenerLite;
import com.lomo.demo.R;
import com.lomo.demo.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 线性马达测试Activity（cmd=0x83）
 * 包含振动配置、目标值、计数值的测试
 * 光之萤火：立即振动按配置(0x0C)、停止振动(0x0D)、最小计数间隔(0x0E/0x0F)
 */
public class LinearMotorActivity extends BaseActivity implements View.OnClickListener {

    public String TAG = getClass().getSimpleName();
    TextView tv_result;
    EditText etTargetValue;
    /** 按配置振动的配置下标输入框（光之萤火） */
    EditText etVibrationConfigIndex;
    /** 最小计数间隔输入框，取值12~15（光之萤火） */
    EditText etMinCountInterval;
    /** 振动配置动态行容器 */
    LinearLayout llVibrationConfigRows;
    /** 振动配置行容器外层滚动视图 */
    ScrollView svVibrationConfigRows;
    /** 每行振动配置的两个输入框（强度、时间），与行视图一一对应 */
    private final List<EditText[]> configRowViews = new ArrayList<>();
    private static final int MAX_CONFIG_ROWS = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linear_motor);

        tv_result = findViewById(R.id.tv_result);
        etTargetValue = findViewById(R.id.et_target_value);
        etVibrationConfigIndex = findViewById(R.id.et_vibration_config_index);
        etMinCountInterval = findViewById(R.id.et_min_count_interval);
        llVibrationConfigRows = findViewById(R.id.ll_vibration_config_rows);
        svVibrationConfigRows = findViewById(R.id.sv_vibration_config_rows);

        // 振动配置
        findViewById(R.id.bt_get_vibration_config).setOnClickListener(this);
        findViewById(R.id.bt_set_vibration_config).setOnClickListener(this);
        findViewById(R.id.bt_add_vibration_config_row).setOnClickListener(this);
        // 默认添加2行
        addVibrationConfigRow(100, 20);
        addVibrationConfigRow(150, 10);
        // 立即振动和停止振动
        findViewById(R.id.bt_immediate_vibration).setOnClickListener(this);
        findViewById(R.id.bt_stop_vibration).setOnClickListener(this);
        // 目标值
        findViewById(R.id.bt_get_target).setOnClickListener(this);
        findViewById(R.id.bt_set_target).setOnClickListener(this);
        // 计数值
        findViewById(R.id.bt_get_count).setOnClickListener(this);
        findViewById(R.id.bt_clear_count).setOnClickListener(this);
        // 计数值监听
        findViewById(R.id.bt_set_count_listener).setOnClickListener(this);
        // 光之萤火：按配置振动、最小计数间隔
        findViewById(R.id.bt_immediate_vibration_by_config).setOnClickListener(this);
        findViewById(R.id.bt_stop_vibration_by_config).setOnClickListener(this);
        findViewById(R.id.bt_set_min_count_interval).setOnClickListener(this);
        findViewById(R.id.bt_get_min_count_interval).setOnClickListener(this);
    }

    /**
     * 动态添加一行振动配置输入（强度 + 时间 + 删除按钮）
     */
    private void addVibrationConfigRow(int defaultIntensity, int defaultTime) {
        if (configRowViews.size() >= MAX_CONFIG_ROWS) {
            postView("\nMax " + MAX_CONFIG_ROWS + " configs");
            return;
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp2px(2);
        row.setLayoutParams(rowLp);

        int index = configRowViews.size();

        TextView label = new TextView(this);
        label.setText("Config" + index + ":");
        label.setTextSize(12);
        row.addView(label);

        EditText etIntensity = new EditText(this);
        etIntensity.setHint("Strength 0~255");
        etIntensity.setInputType(InputType.TYPE_CLASS_NUMBER);
        etIntensity.setText(String.valueOf(defaultIntensity));
        etIntensity.setTextSize(12);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        etIntensity.setLayoutParams(etLp);
        row.addView(etIntensity);

        EditText etTime = new EditText(this);
        etTime.setHint("Time (5 ms)");
        etTime.setInputType(InputType.TYPE_CLASS_NUMBER);
        etTime.setText(String.valueOf(defaultTime));
        etTime.setTextSize(12);
        etTime.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(etTime);

        Button btDelete = new Button(this);
        btDelete.setText("－");
        btDelete.setTextSize(12);
        btDelete.setTextColor(getResources().getColor(R.color.white));
        btDelete.setBackgroundResource(R.drawable.button_bg_state);
        btDelete.setOnClickListener(v -> removeVibrationConfigRow(row, etIntensity, etTime));
        row.addView(btDelete);

        llVibrationConfigRows.addView(row);
        configRowViews.add(new EditText[]{etIntensity, etTime});

        // 等待布局完成后滚动到底部，让用户看到新增的行
        svVibrationConfigRows.post(() -> svVibrationConfigRows.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * 删除一行振动配置
     */
    private void removeVibrationConfigRow(LinearLayout row, EditText etIntensity, EditText etTime) {
        llVibrationConfigRows.removeView(row);
        for (int i = 0; i < configRowViews.size(); i++) {
            EditText[] pair = configRowViews.get(i);
            if (pair[0] == etIntensity && pair[1] == etTime) {
                configRowViews.remove(i);
                break;
            }
        }
    }

    private int dp2px(int dp) {
        return (int) (getResources().getDisplayMetrics().density * dp + 0.5f);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        // ==================== 添加振动配置输入行 ====================
        if (id == R.id.bt_add_vibration_config_row) {
            addVibrationConfigRow(100, 20);
        }

        // ==================== 读取振动配置（Subcmd=0x06）====================
        if (id == R.id.bt_get_vibration_config) {
            postView("\nRead vibration config");
            LmAPILite.GET_VIBRATION_CONFIG(new IVibrationConfigListenerLite() {
                @Override
                public void getVibrationConfigResult(int configCount, List<int[]> configs) {
                    postView("\nVibration configs: " + configCount);
                    for (int i = 0; i < configs.size(); i++) {
                        postView("\n  Config" + i + ": strength=" + configs.get(i)[0] + ", time=" + (configs.get(i)[1] * 5) + "ms");
                    }
                }

                @Override
                public void setVibrationConfigResult(boolean success) {
                    // 读取操作不会触发此回调
                }
            });
        }

        // ==================== 设置振动配置（Subcmd=0x07）====================
        if (id == R.id.bt_set_vibration_config) {
            postView("\nSet vibration config");
            if (configRowViews.isEmpty()) {
                postView("\nAdd at least 1 config first");
                return;
            }
            List<int[]> configList = new ArrayList<>();
            for (EditText[] pair : configRowViews) {
                int intensity = TextUtils.isEmpty(pair[0].getText()) ? 0 : Integer.parseInt(pair[0].getText().toString());
                int time = TextUtils.isEmpty(pair[1].getText()) ? 0 : Integer.parseInt(pair[1].getText().toString());
                configList.add(new int[]{intensity, time});
            }

            LmAPILite.SET_VIBRATION_CONFIG(configList, new IVibrationConfigListenerLite() {
                @Override
                public void getVibrationConfigResult(int configCount, List<int[]> configs) {
                    // 设置操作不会触发此回调
                }

                @Override
                public void setVibrationConfigResult(boolean success) {
                    postView("\nSet vibration config result: " + (success ? "Success" : "Failed"));
                }
            });
        }

        // ==================== 立即振动（Subcmd=0x04）====================
        if (id == R.id.bt_immediate_vibration) {
            postView("\nVibrate now (strong)");
            LmAPILite.IMMEDIATE_VIBRATION(0x01, new IVibrationControlListenerLite() {
                @Override
                public void onImmediateVibrationResult(boolean success) {
                    postView("\nVibrate now result: " + (success ? "Success" : "Failed"));
                }

                @Override
                public void onStopVibrationResult(boolean success) {
                    // 立即振动不会触发此回调
                }
            });
        }

        // ==================== 停止振动（Subcmd=0x05）====================
        if (id == R.id.bt_stop_vibration) {
            postView("\nStop vibration");
            LmAPILite.STOP_VIBRATION(new IVibrationControlListenerLite() {
                @Override
                public void onImmediateVibrationResult(boolean success) {
                    // 停止振动不会触发此回调
                }

                @Override
                public void onStopVibrationResult(boolean success) {
                    postView("\nStop vibration result: " + (success ? "Success" : "Failed"));
                }
            });
        }

        // ==================== 读取目标值（Subcmd=0x08）====================
        if (id == R.id.bt_get_target) {
            postView("\nRead target");
            LmAPILite.GET_LINEAR_MOTOR_TARGET(new ILinearMotorTargetListenerLite() {
                @Override
                public void getTargetResult(int target) {
                    postView("\nTarget: " + target);
                }

                @Override
                public void setTargetResult(boolean success) {
                    // 读取操作不会触发此回调
                }
            });
        }

        // ==================== 设置目标值（Subcmd=0x09）====================
        if (id == R.id.bt_set_target) {
            postView("\nSet target");
            int target = TextUtils.isEmpty(etTargetValue.getText()) ? 500 : Integer.parseInt(etTargetValue.getText().toString());  // uint32_t 目标值

            LmAPILite.SET_LINEAR_MOTOR_TARGET(target, new ILinearMotorTargetListenerLite() {
                @Override
                public void getTargetResult(int target) {
                    // 设置操作不会触发此回调
                }

                @Override
                public void setTargetResult(boolean success) {
                    postView("\nSet target result: " + (success ? "Success" : "Failed"));
                }
            });
        }

        // ==================== 读取计数值（Subcmd=0x0A）====================
        if (id == R.id.bt_get_count) {
            postView("\nRead count");
            LmAPILite.GET_LINEAR_MOTOR_COUNT(new ILinearMotorCountListenerLite() {
                @Override
                public void onCountResult(int count) {
                    postView("\nCount: " + count);
                }

                @Override
                public void onClearResult(boolean success) {
                    // 读取操作不会触发此回调
                }
            });
        }

        // ==================== 清空计数值（Subcmd=0x0B）====================
        if (id == R.id.bt_clear_count) {
            postView("\nClear count");
            LmAPILite.CLEAR_LINEAR_MOTOR_COUNT(new ILinearMotorCountListenerLite() {
                @Override
                public void onCountResult(int count) {
                    // 清空操作不会触发此回调
                }

                @Override
                public void onClearResult(boolean success) {
                    postView("\nClear count result: " + (success ? "Success" : "Failed"));
                }
            });
        }

        // ==================== 设置计数值监听器（接收实时推送）====================
        if (id == R.id.bt_set_count_listener) {
            postView("\nSet count listener (live push)");
            LmAPILite.SET_LINEAR_MOTOR_COUNT_LISTENER(new ILinearMotorCountListenerLite() {
                @Override
                public void onCountResult(int count) {
                    postView("\n[Live] count: " + count);
                }

                @Override
                public void onClearResult(boolean success) {
                    // 监听器不会触发此回调
                }
            });
        }

        // ==================== 立即振动（按配置，Subcmd=0x0C）-光之萤火 ====================
        if (id == R.id.bt_immediate_vibration_by_config) {
            int configIndex = TextUtils.isEmpty(etVibrationConfigIndex.getText()) ? 0 : Integer.parseInt(etVibrationConfigIndex.getText().toString());
            postView("\nVibrate now (config #" + configIndex + ")");
            LmAPILite.IMMEDIATE_VIBRATION_BY_CONFIG(configIndex, new IConfigVibrationControlListenerLite() {
                @Override
                public void onConfigImmediateVibrationResult(boolean success) {
                    postView("\nVibrate by config result: " + (success ? "Success" : "Failed"));
                }

                @Override
                public void onConfigStopVibrationResult(boolean success) {
                    // 立即振动不会触发此回调
                }
            });
        }

        // ==================== 停止振动（Subcmd=0x0D）-光之萤火 ====================
        if (id == R.id.bt_stop_vibration_by_config) {
            postView("\nStop vibration (by config)");
            LmAPILite.STOP_VIBRATION_BY_CONFIG(new IConfigVibrationControlListenerLite() {
                @Override
                public void onConfigImmediateVibrationResult(boolean success) {
                    // 停止振动不会触发此回调
                }

                @Override
                public void onConfigStopVibrationResult(boolean success) {
                    postView("\nStop vibration (by config) result: " + (success ? "Success" : "Failed"));
                }
            });
        }

        // ==================== 配置最小计数间隔（Subcmd=0x0E）-光之萤火 ====================
        if (id == R.id.bt_set_min_count_interval) {
            int interval = TextUtils.isEmpty(etMinCountInterval.getText()) ? 12 : Integer.parseInt(etMinCountInterval.getText().toString());
            if (interval < 12 || interval > 100) {
                postView("\nMin count interval range is 12~100");
                return;
            }
            postView("\nSet min count interval: " + interval);
            LmAPILite.SET_MIN_COUNT_INTERVAL(interval, new IMinCountIntervalListenerLite() {
                @Override
                public void setMinCountIntervalResult(boolean success) {
                    postView("\nSet min count interval result: " + (success ? "Success" : "Failed"));
                }

                @Override
                public void getMinCountIntervalResult(int interval) {
                    // 配置操作不会触发此回调
                }
            });
        }

        // ==================== 读取最小计数间隔（Subcmd=0x0F）-光之萤火 ====================
        if (id == R.id.bt_get_min_count_interval) {
            postView("\nRead min count interval");
            LmAPILite.GET_MIN_COUNT_INTERVAL(new IMinCountIntervalListenerLite() {
                @Override
                public void setMinCountIntervalResult(boolean success) {
                    // 读取操作不会触发此回调
                }

                @Override
                public void getMinCountIntervalResult(int interval) {
                    postView("\nMin count interval: " + interval);
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

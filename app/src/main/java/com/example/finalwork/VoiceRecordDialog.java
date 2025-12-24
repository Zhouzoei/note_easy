package com.example.finalwork;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceRecordDialog extends Dialog {

    private Context context;
    private OnAudioRecordedListener listener;

    // UI组件
    private TextView tvRecordingStatus, tvRecordingTime, tvPlayTime, tvRecordHint;
    private TextView btnRecord, btnPlay;
    private ImageButton btnClose;
    private View rippleEffect;
    private SeekBar seekBar;
    private Button btnDiscard, btnUseAudio;
    private View playbackSection;

    // 录音相关
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private String currentAudioFilePath = "";
    private long recordingStartTime = 0;
    private long recordingDuration = 0;
    private CountDownTimer recordingTimer;

    // 回调接口
    public interface OnAudioRecordedListener {
        void onAudioRecorded(String audioPath, long duration);
        void onRecordingCancelled();
    }

    public VoiceRecordDialog(Context context, OnAudioRecordedListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_voice_record);

        // 设置弹窗窗口属性
        Window window = getWindow();
        if (window != null) {
            // 设置背景透明，让圆角显示
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 设置窗口布局参数
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;

            // 设置宽度为屏幕宽度的85%
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager != null) {
                android.view.Display display = windowManager.getDefaultDisplay();
                android.graphics.Point size = new android.graphics.Point();
                display.getSize(size);
                int screenWidth = size.x;
                params.width = (int) (screenWidth * 0.85);
            }

            // 高度由内容决定
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;

            // 设置背景变暗
            params.dimAmount = 0.5f;

            // 应用参数
            window.setAttributes(params);

        }

        initViews();
        setClickListeners();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 确保窗口大小正确
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void initViews() {
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus);
        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        tvPlayTime = findViewById(R.id.tvPlayTime);
        tvRecordHint = findViewById(R.id.tvRecordHint);
        btnRecord = findViewById(R.id.btnRecord);
        btnPlay = findViewById(R.id.btnPlay);
        btnClose = findViewById(R.id.btnClose);
        rippleEffect = findViewById(R.id.rippleEffect);
        seekBar = findViewById(R.id.seekBar);
        btnDiscard = findViewById(R.id.btnDiscard);
        btnUseAudio = findViewById(R.id.btnUseAudio);
        playbackSection = findViewById(R.id.playbackSection);
    }

    private void setClickListeners() {
        // 关闭按钮
        btnClose.setOnClickListener(v -> {
            stopRecording();
            stopPlaying();
            if (listener != null) {
                listener.onRecordingCancelled();
            }
            dismiss();
        });

        // 录音按钮
        btnRecord.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        // 播放按钮
        btnPlay.setOnClickListener(v -> {
            if (currentAudioFilePath.isEmpty()) return;

            if (!isPlaying) {
                startPlaying();
            } else {
                stopPlaying();
            }
        });

        // 重录按钮
        btnDiscard.setOnClickListener(v -> {
            resetRecording();
        });

        // 使用录音按钮
        btnUseAudio.setOnClickListener(v -> {
            if (currentAudioFilePath.isEmpty()) {
                Toast.makeText(context, "请先录制音频", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listener != null) {
                listener.onAudioRecorded(currentAudioFilePath, recordingDuration);
            }
            dismiss();
        });

        // 进度条监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    int duration = mediaPlayer.getDuration();
                    int newPosition = (int) ((progress / 100.0) * duration);
                    mediaPlayer.seekTo(newPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void startRecording() {
        // 检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    (android.app.Activity) context,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    100
            );
            Toast.makeText(context, "需要录音权限", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 创建录音文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "voice_" + timeStamp + ".mp3";
            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            File audioFile = new File(storageDir, fileName);
            currentAudioFilePath = audioFile.getAbsolutePath();

            // 初始化录音器
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(currentAudioFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            recordingStartTime = System.currentTimeMillis();

            // 更新UI
            tvRecordingStatus.setText("录音中...");
            tvRecordHint.setText("点击结束录音");
            btnRecord.setText("⏹️"); // 停止图标

            // 开始波纹动画
            startRippleAnimation();

            // 开始计时
            startRecordingTimer();

            Toast.makeText(context, "录音开始", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "录音失败", Toast.LENGTH_SHORT).show();
            resetRecording();
        }
    }

    private void stopRecording() {
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                isRecording = false;
                recordingDuration = System.currentTimeMillis() - recordingStartTime;

                // 更新UI
                tvRecordingStatus.setText("录音完成");
                tvRecordHint.setText("点击重新录音");
                btnRecord.setText("🎤"); // 麦克风图标

                // 停止波纹动画
                stopRippleAnimation();

                // 停止计时器
                if (recordingTimer != null) {
                    recordingTimer.cancel();
                }

                // 显示播放区域
                playbackSection.setVisibility(View.VISIBLE);

                Toast.makeText(context, "录音完成", Toast.LENGTH_SHORT).show();

            } catch (RuntimeException e) {
                e.printStackTrace();
                Toast.makeText(context, "录音停止失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecordingTimer() {
        if (recordingTimer != null) {
            recordingTimer.cancel();
        }

        recordingTimer = new CountDownTimer(Long.MAX_VALUE, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isRecording) {
                    long elapsedTime = System.currentTimeMillis() - recordingStartTime;
                    long seconds = elapsedTime / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;
                    long millis = (elapsedTime % 1000) / 10;
                    tvRecordingTime.setText(String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, millis));
                }
            }

            @Override
            public void onFinish() {}
        };
        recordingTimer.start();
    }

    private void startRippleAnimation() {
        rippleEffect.setVisibility(View.VISIBLE);

        ValueAnimator animator = ValueAnimator.ofFloat(0.5f, 1.2f, 0.5f);
        animator.setDuration(1500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            rippleEffect.setScaleX(value);
            rippleEffect.setScaleY(value);

            // 透明度变化
            float alpha = 1.0f - (value - 0.5f) / 0.7f;
            rippleEffect.setAlpha(alpha);
        });
        animator.start();

        rippleEffect.setTag(animator);
    }

    private void stopRippleAnimation() {
        rippleEffect.setVisibility(View.INVISIBLE);
        if (rippleEffect.getTag() != null && rippleEffect.getTag() instanceof ValueAnimator) {
            ((ValueAnimator) rippleEffect.getTag()).cancel();
        }
    }

    private void startPlaying() {
        if (currentAudioFilePath.isEmpty()) return;

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(currentAudioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            isPlaying = true;
            btnPlay.setText("⏸️"); // 暂停图标

            // 设置进度条最大值
            int duration = mediaPlayer.getDuration();
            seekBar.setMax(100);

            // 更新播放进度
            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && isPlaying) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int totalDuration = mediaPlayer.getDuration();

                        // 更新播放时间
                        int seconds = (currentPosition / 1000) % 60;
                        int minutes = (currentPosition / 1000) / 60;
                        tvPlayTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

                        // 更新进度条
                        if (totalDuration > 0) {
                            int progress = (int) ((currentPosition * 100.0) / totalDuration);
                            seekBar.setProgress(progress);
                        }

                        handler.postDelayed(this, 100);
                    }
                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                stopPlaying();
                seekBar.setProgress(100);
            });

            Toast.makeText(context, "开始播放", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "播放失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        btnPlay.setText("▶️"); // 播放图标
        seekBar.setProgress(0);
        tvPlayTime.setText("00:00");
    }

    private void resetRecording() {
        // 停止录音和播放
        stopRecording();
        stopPlaying();

        // 删除录音文件
        if (currentAudioFilePath != null && !currentAudioFilePath.isEmpty()) {
            File audioFile = new File(currentAudioFilePath);
            if (audioFile.exists()) {
                audioFile.delete();
            }
        }

        // 重置状态
        currentAudioFilePath = "";
        recordingDuration = 0;

        // 重置UI
        tvRecordingStatus.setText("点击下方按钮开始录音");
        tvRecordingTime.setText("00:00");
        tvRecordHint.setText("点击录音，再次点击结束");
        playbackSection.setVisibility(View.GONE);
        seekBar.setProgress(0);
        tvPlayTime.setText("00:00");
        btnRecord.setText("🎤");

        Toast.makeText(context, "已重置录音", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void dismiss() {
        stopRecording();
        stopPlaying();
        super.dismiss();
    }
}
package com.example.courseallmobil;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;

public class Record extends AppCompatActivity {

    /*
    Объявление констант REQUEST_CODE с произвольными значениями
    Эти константы используются в интентах и запросах разрешений, чтобы отличать друг от друга пришедшие результаты(30)
     */
    private static final int RECORD_REQUEST_CODE  = 101;
    private static final int STORAGE_REQUEST_CODE = 102;
    private static final int AUDIO_REQUEST_CODE   = 103;

    /*
    Обявление перемнных класса
    MediaProjectionManager - управляет получением  определенных типов токеном MediaProjection
    MediaProjection - токен, предоставляющий приложению возможность захватить содержимое экрана или записывать аудиосистемы
    Обявление экземпляра нашего сервиса RecordService и обычной кнопки
     */
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private RecordService recordService;
    private Button startBtn;

    /*
    onCreate - получаем экземпляр MediaProjectionManager для управления сессиями отображения данных
    Создаем кнопку(поумаолчанию неактивную) и слушатель для нее
       В методе onClick по нажатию на кнопку, вызывается метод recordService.stopRecord (в случае, если запись идет)
       иначе создаем интент с projectionManager.createScreenCaptureIntent и отправляем его методом startActivityForResult
       Далее идут запросы разрешений на запись в память устройства и на запись аудио.
       Здесь создаем еще один интент для запуска сервиса и передаем его методу bindService, который выполняет привязку сервиса к приложению, а если сервис не работает, то стартует его предварительно

     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        setContentView(R.layout.activity_record);

        startBtn = (Button) findViewById(R.id.start_record);
        startBtn.setEnabled(false);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordService.isRunning()) {
                    recordService.stopRecord();
                    startBtn.setText(R.string.start_record);
                } else {
                    Intent captureIntent = projectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
                }
            }
        });

        if (ContextCompat.checkSelfPermission(Record.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(Record.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
        }

        Intent intent = new Intent(this, RecordService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    /*
    В методе onDestroy отвязываем сервис
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    /*
    Методе onActivityResult получает результат вызова метода StartActivityForResult, где отправляется капчер интент и RECORD_REQUEST_CODE
    Если запрос прошел успешно, то создадим объект mediaProjection, полученный от успешного запроса захвата экрана. Он будет иметь значение null, если результат от StartActivityForResult будет не окей.
    Вызываем метод recordService.startRecord и меняем текст кнопки на остановить запиись
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            recordService.setMediaProject(mediaProjection);
            recordService.startRecord();
            startBtn.setText(R.string.stop_record);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST_CODE || requestCode == AUDIO_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
            recordService = binder.getRecordService();
            recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
            startBtn.setEnabled(true);
            startBtn.setText(recordService.isRunning() ? R.string.stop_record : R.string.start_record);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };
}
package com.example.courseallmobil;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/*
Класс RecordService унаследован от класса Service.
Service - некая задача, которая работает в фоне и не использует пользовательский интерфейс.
Поскольку, нам нужно записывать все. что происходит на экране независимо от того какое приложение запущено, поэтому и будет использоваться Service
 */


public class RecordService extends Service {
    /*
    Объявление переменных класса:
    MediaProjection - токен, предоставляющий приложению возможность захватить содержимое экрана или записывать аудиосистемы
    MediaRecorder - класс, который используется для записи аудио и видео
    VirtualDisplay представляет собой виртуальный экран, содержимое которого рендерится в surface, который мы передаем методу СreateVirtualDisplay
    surface - компонент, на который выводится изображение
     */
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private File mCurrentFile;

    /*
    Обявление еще нескольких переменных:
    Логическую running, которой будем присваивать true в процессе записи
    Далее параметры виртуального экрана (Разрешение установлено, плотность пока не указывается)
     */
    private boolean running;
    private int width = 720;
    private int height = 1080;
    private int dpi;

    /*
    Метод onBind с типом IBinder, который позволяет приложению подключится к  сервису и взаимодействовать с ним через возвращаемый объект RecordBinder (97)
     */
    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    //Методы жизненного цикла сервиса
    /*
    Метод onStartCommand срабатывает при старте сервиса методом StartService, который вызывается к лассе RecordApplication.
    Он возвращает флаг START_STICKY - это означает, что сервис будет перезапущен, если будет убит системой
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /*
    onCreate вызывается в начале работы сервиса
    В нем создаем отдельный поток serviceThread с использованием класса HandlerThread.
    HandlerThread - вспомогательный класс ддля запуска нового  потока, который имеет лупер, который может использоваться  для создания обработчика класса
    На фход передаем проивольное имя потока и флаг THREAD_PRIORITY_BACKGROUND (стандартный приоритет фоновых потоков)
    Далее стартуем поток, сбрассываем значение переменной running и создаем MediaRecorder
     */
    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("service_thread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
        mediaRecorder = new MediaRecorder();
    }

    /*
    onDestroy - вызывается при остановке сервиса
    Просто здесь вызываем метод суперкласса
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*
    setMediaProject - будет вызываться в MainActivity и передает объект mediaProjection
     */
    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }

    /*
    Геттер для переменной running
     */
    public boolean isRunning() {
        return running;
    }

    /*
    setConfig - устанавливает параметры виртуального экрана
     */
    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    /*
    В методе startRecord проверяется:
    Если объект mediaProjection не существует и переменная running имеет значение true, то возвращаем false
    Вызываем здесь методы initRecorder и createVirtualDisplay, стартуем запись вызвав mediaRecorder.start().
    присваем переменной running значение true и возвращаем true.
     */
    public boolean startRecord() {
        if (mediaProjection == null || running) {
            return false;
        }

        initRecorder();
        createVirtualDisplay();
        mediaRecorder.start();
        running = true;
        return true;
    }

    /*
    Метод stopRecord выполняет  обратные операции
    Останавливает запись и перезапускает mediaRecorder в состояние ожидания
    Освобождаем virtualDisplay и останавливаем mediaProjection
     */
    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        running = false;
        mediaRecorder.stop();
        mediaRecorder.reset();
        virtualDisplay.release();
        mediaProjection.stop();

        return true;
    }

    /*
    Метод createVirtualDisplay - вызывается в методе startRecord
    Здесь выполняется создание виртуального экрана через метод mediaProjection.createVirtualDisplay, которому передаются произвольное имя дисплея,
    его параметры, флаг VIRTUAL_DISPLAY и флаг AUTO_MIRROR, который позволяет содержимое приватного дисплея, если его содержимое не отображается и Surface
     */
    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }


    /*
    initRecorder - вызывается в методе startRecord
    Здесь работаем  с объектом mediaRecorder
    setAudioSource - устанавливает источник звука, используемый для записи
    setVideoSource - задает источник видео, который будет использоваться для записи
    setOutputFormat - устанавливает формат получаемого файла записи
    setOutputFile - устанавливает целевое местоположение и имя файла записи
    setVideoSize - устанавливает размер видео
    setVideoEncoder - определяет кодировщик видео
    setAudioEncoder - определяет кодировщик аудио
    setVideoEncodingBitRate - устанавливает битрейт файла записи (прописано жесткое значение равное 5 Мегабит)
    setVideoFrameRate - задает частоту  кадров (30 кадров в секунду)
    prepare - подготавливает mediaRecorder для записи и  кодирования данных(выполняется в блоке try-)
    prepare - подготавливает mediaRecorder для записи и  кодирования данных(выполняется в блоке try-catch с перехватом ошибки IOException)
     */
    private void initRecorder() {
        //File outFile = new File(System.currentTimeMillis() + ".mp4");
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //mediaRecorder.setOutputFile(System.currentTimeMillis() + ".mp4");
        //mCurrentFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "test"+System.currentTimeMillis()+".mp4");
        //mediaRecorder.setOutputFile(mCurrentFile.getAbsolutePath());
        mediaRecorder.setOutputFile(getsaveDirectory() + "ScreenRecord" + System.currentTimeMillis() + ".mp4");
        //mediaRecorder.setOutputFile(getsaveDirectory() + System.currentTimeMillis() + ".mp4");
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
        mediaRecorder.setVideoFrameRate(30);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    getsaveDirectory - задаем путь для сохранения файла записи и показываем Toast об этом пользователю
     */
    public String getsaveDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+ "/";
            //String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "ScreenRecord" + "/";
            //String rootDir = getSDcardPath() + "/" + "ScreenRecord" + "/";
            //String rootDir = "ScreenRecord" + "/";
            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }

            Toast.makeText(getApplicationContext(), rootDir, Toast.LENGTH_SHORT).show();

            return rootDir;
        } else {
            return null;
        }
    }

    /*
    класс RecordBinder это Binder для связи и взаимодействия с сервисом в приложении
     */
    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }

}

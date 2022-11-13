package com.example.courseallmobil;

import android.app.Application;
import android.content.Context;
import android.content.Intent;


/*Унаследован от класса Application
  Согласно документации класс Application или его наследний инициализируется перед любым другим классом, когда создается процесс приложения.
  Его вызовов происходит в манифесте в секции application в строке android:name. Здесь запускается метод StartService, который запускает сервис RecordService при старте приложения
 */

public class RecordApplication extends Application{

    private static RecordApplication application;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        application = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Start service
        startService(new Intent(this, RecordService.class));
    }

    public static RecordApplication getInstance() {
        return application;
    }
}

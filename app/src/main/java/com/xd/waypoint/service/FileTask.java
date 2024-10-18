package com.xd.waypoint.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FileTask implements Runnable {
    private final Context context;
    private final Handler handler;
    private final String str;
    private final long interval;
    protected static final String TAG = "FileTask";

    public FileTask(Context context,  String str, long interval) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.str = str;
        this.interval = interval;
    }

    @Override
    public void run() {
        FileOutputStream output = null;
        BufferedWriter writer=null;
        try {
            output = context.openFileOutput("forlat", Context.MODE_APPEND);
            writer=new BufferedWriter(new OutputStreamWriter(output));
            writer.write(str);
            Toast.makeText(context,"文件写入成功",Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage() + "文件未找到");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage() );
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage() + "Io未正常关闭");
                }
            }
        }
        handler.postDelayed(this,interval);
    }
    public void start(){
        handler.postDelayed(this,interval);
    }
    public void stop(){
        handler.removeCallbacks(this);
    }
}

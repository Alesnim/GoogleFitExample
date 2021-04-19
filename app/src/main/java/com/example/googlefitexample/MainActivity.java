package com.example.googlefitexample;

import static java.text.DateFormat.getTimeInstance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1;
    private GoogleSignInOptionsExtension fitnessOptions;
    private TextView textView;
    private GoogleSignInAccount account;
    private float sleepHours = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
                .build();

        account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                    account,
                    fitnessOptions);
        } else {
            accessGoogleFit();
        }

        textView = findViewById(R.id.tv_text);
        /*@SuppressLint("HandlerLeak") Handler handler = new Handler() {
            @SuppressLint("DefaultLocale")
            @Override
            public void handleMessage(@NonNull Message msg) {
                long s = msg.getData().getLong("sleep");
                textView.setText(String.format("%d hour", TimeUnit.MILLISECONDS.toHours(s)));
            }
        };*/



    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) accessGoogleFit();
        } else {
            Log.d("TAGgag", "FAIL");
            Log.d("TAGgag", String.valueOf(requestCode));
        }
    }

    private void accessGoogleFit() {

        // Берем данные за прошедший день
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_WEEK, -1);
        long startTime = cal.getTimeInMillis();

        // создаем подключение в виде сессии (не знаю чем отличается от простого запроса данных)
        SessionReadRequest request = new SessionReadRequest.Builder()
                .readSessionsFromAllApps()
                // By default, only activity sessions are included, so it is necessary to explicitly
                // request sleep sessions. This will cause activity sessions to be *excluded*.
                .includeSleepSessions()
                // Sleep segment data is required for details of the fine-granularity sleep, if it is present.
                .read(DataType.TYPE_SLEEP_SEGMENT)
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        // регистрируем сессию подключения для апи
        Session session = new Session.Builder()
                .setName("sessionName")
                .setIdentifier("identifier")
                .setDescription("description")
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .build();

        // стартуем сессию
        Fitness.getSessionsClient(this, account).startSession(session).addOnFailureListener(e -> Log.i("TAG", "Session" + e.getMessage()));

        // Запрашиваем данные
        Fitness.getSessionsClient(this, account).readSession(request).addOnSuccessListener(sessionReadResponse -> {
            for (Session ses : sessionReadResponse.getSessions()) {
                long sessionStart = ses.getStartTime(TimeUnit.MILLISECONDS);
                long sessionEnd = ses.getEndTime(TimeUnit.MILLISECONDS);
                long sessionSleepTime = sessionEnd - sessionStart;
                // переводим миллисекунды в часы
                // https://stackoverflow.com/questions/625433/how-to-convert-milliseconds-to-x-mins-x-seconds-in-java
                String hours = String.format("%d hour", TimeUnit.MILLISECONDS.toHours(sessionSleepTime));
                Log.i("TAG", hours);
                textView.setText(hours);
            }
        }).addOnFailureListener(e -> Log.i("TAG", e.getMessage()));

    }





}
package com.paradox.addictsclock;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private static PowerManager.WakeLock level1Lock = null;
    private static PowerManager.WakeLock level2Lock = null;

    static MediaPlayer acceptPlayer, errorPlayer;

    EditText editText;

    int check_counter = 0;
    int error_counter = 0;

    int looping       = 0;
    int looping_now   = 1;

    List<Integer> roomIds = null;
    List<RoomItem> roomItems = new ArrayList<>();

    RoomDBHelper dbHelper;

    Handler handler = new Handler();

    StatusListAdapter statusListAdapter;


    private void setThreadPolicy() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    private void setSleepStrategy() {
        RadioButton level0Button = findViewById(R.id.level0);
        RadioButton level1Button = findViewById(R.id.level1);
        RadioButton level2Button = findViewById(R.id.level2);

        level0Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "The program will stop when the screen goes off.", Toast.LENGTH_SHORT).show();

                if (level1Lock != null)
                    level1Lock.release();

                if (level2Lock != null)
                    level2Lock.release();

                level1Lock = null;
                level2Lock = null;
            }
        });

        level1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "The program will continue to run when the screen goes off. (May Not Work)", Toast.LENGTH_SHORT).show();

                if (level1Lock != null)
                    level1Lock.release();

                if (level2Lock != null)
                    level2Lock.release();

                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                level1Lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag");
                level1Lock.acquire();

                level2Lock = null;
            }
        });

        level2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "The screen will never go off.", Toast.LENGTH_SHORT).show();

                if (level1Lock != null)
                    level1Lock.release();

                if (level2Lock != null)
                    level2Lock.release();

                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                level2Lock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MyApp::MyWakelockTag");
                level2Lock.acquire();

                level1Lock = null;
            }
        });

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        level1Lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag");
        level1Lock.acquire();
    }

    public void detectionLoop() {
        if(looping != looping_now) {
            for(RoomItem roomItem: roomItems) {
                roomItem.title   = "";
                roomItem.isLive  = false;
                roomItem.isError = RoomItem.ErrorFlag.NO_ERROR;
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    statusListAdapter.notifyDataSetChanged();
                }
            });

            return;
        }

        check_counter += 1;

        boolean error_flag = false;
        boolean live_flag = false;

        StringBuilder log = new StringBuilder("Target: ");

        for(RoomItem roomItem: roomItems) {
            roomItem.check_live();

            if(!roomItem.isCheck)
                continue;

            log.append(roomItem.uname).append('/');

            if(roomItem.isError == RoomItem.ErrorFlag.NETWORK_ERROR)
                error_flag = true;
            else if(roomItem.isLive)
                live_flag = true;
        }

        Log.d("myLog", String.valueOf(log.append(" Count: ").append(check_counter)));

        handler.post(new Runnable() {
            @Override
            public void run() {
                statusListAdapter.notifyDataSetChanged();
            }
        });

        if(error_flag) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_SHORT).show();
                }
            });
            error_counter += 1;
        }
        else error_counter = 0;

        if(live_flag) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    acceptPlayer.start();
                }
            });
        }
        else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(acceptPlayer.isPlaying())
                        acceptPlayer.pause();
                }
            });
        }

        if(error_counter >= 5) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    errorPlayer.start();
                }
            });
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setThreadPolicy();

        setSleepStrategy();

        acceptPlayer = MediaPlayer.create(getApplicationContext(), R.raw.starlight);
        acceptPlayer.setLooping(true);

        errorPlayer = MediaPlayer.create(getApplicationContext(), R.raw.thirteen);
        errorPlayer.setLooping(true);


        editText = findViewById(R.id.edit_text);
        Button addButton = findViewById(R.id.add_button);
        Button delButton = findViewById(R.id.del_button);

        dbHelper = RoomDBHelper.getInstance(this, 0);
        dbHelper.openWriteLink();

        roomIds = dbHelper.query();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = String.valueOf(editText.getText());

                if(!value.equals("")) {
                    int roomId = 0;

                    try {
                        roomId = Integer.parseInt(value);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "RoomId illegal", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    RoomItem roomItem = new RoomItem(roomId);

                    if(roomItem.isError == RoomItem.ErrorFlag.NETWORK_ERROR)
                        Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_SHORT).show();
                    else if(roomItem.isError == RoomItem.ErrorFlag.ROOM_NOT_FOUND)
                        Toast.makeText(getApplicationContext(), "ROOM " + roomId + " not found", Toast.LENGTH_SHORT).show();
                    else {
                        long result = dbHelper.insert(roomItem.roomId);

                        if(result == -1)
                            Toast.makeText(getApplicationContext(), "ROOM " + roomId + " exists", Toast.LENGTH_SHORT).show();
                        else {
                            roomIds.add(roomItem.roomId);

                            roomItems.add(roomItem);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    detectionLoop();
                                }
                            }).start();

                            editText.setText("");
                        }
                    }
                }
            }
        });

        delButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = String.valueOf(editText.getText());

                if(!value.equals("")) {
                    int roomId = Integer.parseInt(value);

                    int count = dbHelper.delete(roomId);

                    if(count == 0)
                        Toast.makeText(getApplicationContext(), "ROOM " + roomId + " does not in the list", Toast.LENGTH_SHORT).show();
                    else {
                        for(int i = 0; i < roomIds.size(); i++) {
                            if(roomIds.get(i) == roomId) {
                                roomIds.remove(i);
                                break;
                            }
                        }

                        for(int i = 0; i < roomItems.size(); i++) {
                            if(roomItems.get(i).roomId == roomId) {
                                roomItems.remove(i);
                                break;
                            }
                        }

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                detectionLoop();
                            }
                        }).start();

                        editText.setText("");
                    }
                }
            }
        });


        ToggleButton global_toggle = findViewById(R.id.global_toggle);
        ListView list_view = findViewById(R.id.listview);
        TextView textTimer = findViewById(R.id.text_timer);
        TextView textCounter = findViewById(R.id.text_counter);

        boolean connectionError = false;
        for(Integer roomId: roomIds) {
            RoomItem roomItem = new RoomItem(roomId);

            if(roomItem.isError == RoomItem.ErrorFlag.NETWORK_ERROR)
                connectionError = true;
            else if(roomItem.isError == RoomItem.ErrorFlag.ROOM_NOT_FOUND)
                Toast.makeText(getApplicationContext(), "ROOM " + roomId + " not found", Toast.LENGTH_SHORT).show();
            else
                roomItems.add(roomItem);
        }
        if(connectionError)
            Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_SHORT).show();

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date date = new Date();
        textTimer.setText(dateFormat.format(date));
        textCounter.setText("Counter: " + 0);

        Runnable check_loop = new Runnable() {
            @Override
            public void run() {
                int looping_flag = looping;

                while(looping_flag == looping_now) {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            textTimer.setText(dateFormat.format(new Date()));
                            textCounter.setText("Counter: " + check_counter);
                        }
                    });

                    detectionLoop();

                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        global_toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    Toast.makeText(getApplicationContext(), "Start detection", Toast.LENGTH_SHORT).show();

                    check_counter = 0;
                    error_counter = 0;

                    looping = (looping + 1) % 100000;

                    new Thread(check_loop).start();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Stop detection", Toast.LENGTH_SHORT).show();

                    check_counter = 0;
                    error_counter = 0;

                    looping_now = (looping_now + 1) % 100000;

                    detectionLoop();

                    if(acceptPlayer.isPlaying())
                        acceptPlayer.pause();

                    if(errorPlayer.isPlaying())
                        errorPlayer.pause();
                }

                editText.clearFocus();
            }
        });

        statusListAdapter = new StatusListAdapter(MainActivity.this, roomItems, this);
        list_view.setAdapter(statusListAdapter);

    }

    @Override
    protected void onDestroy() {
        dbHelper.closeLink();

        super.onDestroy();
    }
}
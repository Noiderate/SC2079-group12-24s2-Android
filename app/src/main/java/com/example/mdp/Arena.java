package com.example.mdp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ObjectAnimator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Arena extends AppCompatActivity {
    public static final String SHARED_PREFS = "sharedPrefs";
    private static final String TAG = "Arena->DEBUG";
    public static boolean firstStart = true;

    public void savesdata() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save car position and rotation
        if (car != null) {
            editor.putFloat("carX", car.getTranslationX());
            editor.putFloat("carY", car.getTranslationY());
            editor.putFloat("carRotation", car.getRotation());
        } else {
            Log.e(TAG, "Car is null, cannot save car data.");
            return; //Exit method to prevent crashing
        }

        // Save car coordinates and direction text
        if (car_x != null && car_y != null && car_dir != null) {
            editor.putString("x_tv", car_x.getText().toString());
            editor.putString("y_tv", car_y.getText().toString());
            editor.putString("car_dir", car_dir.getText().toString());
        }

        // Save obstacle positions and rotations
        for (Map.Entry<Integer, ImageView> entry : obstacles.entrySet()) {
            int obstacleId = entry.getKey();
            ImageView obstacle = entry.getValue();

            if (obstacle != null) {
                editor.putFloat("obs" + obstacleId + "X", obstacle.getTranslationX());
                editor.putFloat("obs" + obstacleId + "Y", obstacle.getTranslationY());
                editor.putFloat("obs" + obstacleId + "Rotation", obstacle.getRotation());
            }
        }

        editor.apply();  // Apply the changes to SharedPreferences
        Log.d(TAG, "Arena state saved successfully!");
    }


    public void loaddata() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        // Load car position and rotation
        if (car != null) {
            car.setTranslationX(sharedPreferences.getFloat("carX", 0.0f));
            car.setTranslationY(sharedPreferences.getFloat("carY", 0.0f));
            car.setRotation(sharedPreferences.getFloat("carRotation", 0.0f));
        } else {
            Log.e(TAG, "Car is null, cannot load car data.");
        }

        // Load car coordinates and direction text
        if (car_x != null && car_y != null && car_dir != null) {
            car_x.setText(sharedPreferences.getString("x_tv", ""));
            car_y.setText(sharedPreferences.getString("y_tv", ""));
            car_dir.setText(sharedPreferences.getString("car_dir", ""));
        }

        // Load obstacle positions and rotations
        for (Map.Entry<Integer, ImageView> entry : obstacles.entrySet()) {
            int obstacleId = entry.getKey();
            ImageView obstacle = entry.getValue();

            if (obstacle != null) {
                obstacle.setTranslationX(sharedPreferences.getFloat("obs" + obstacleId + "X", 0.0f));
                obstacle.setTranslationY(sharedPreferences.getFloat("obs" + obstacleId + "Y", 0.0f));
                obstacle.setRotation(sharedPreferences.getFloat("obs" + obstacleId + "Rotation", 0.0f));
            }
        }

        Log.d(TAG, "Arena state loaded successfully!");
    }


    private static final int SNAP_GRID_INTERVAL = 35;
    private static final int ANIMATOR_DURATION = 1000;

    /*
     * start from (1,1)
     * NOTE: remember to invert y
     */
    private final int INITIAL_X = 1 * SNAP_GRID_INTERVAL - SNAP_GRID_INTERVAL;
    private final int INITIAL_Y = 18 * SNAP_GRID_INTERVAL - SNAP_GRID_INTERVAL;

    private boolean cansetobstacles = false;
    private String curMode = "IDLE";

    Button IRButton, SPButton, resetButton, preset1Button, setButton, timerButton, saveButton;
    ImageView obstacle1, obstacle2, obstacle3, obstacle4, obstacle5, obstacle6, obstacle7, obstacle8, car;
    TextView statusWindow, car_x, car_y, car_dir;

    Map<Integer, ImageView> obstacles;

    // RecyclerView
    //ArrayList<String> s1 = new ArrayList<String>();
    //ArrayList<Integer> images = new ArrayList<Integer>();
    //RecyclerView recyclerView;
    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed, saving data...");
        savesdata();  // Save the arena state before exiting
        super.onBackPressed();  // Call the default back button behavior
    }


    protected void onPause() {
        super.onPause();
        Log.d("onpause", "OnPause() called");
        savesdata();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Restore saved instance state
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        setContentView(R.layout.arena);

        // start listening for incoming messages
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, new IntentFilter("incomingMessage"));

        initialiseObstacles();
        initialiseButtons();
        initialiseMovementButtons();

        loaddata();

        if (!firstStart) {
            loaddata();
        } else {
            firstStart = false;
            savesdata();
        }
    }

    /**
     * Initializes obstacles and setup listeners
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initialiseObstacles() {
        obstacle1 = findViewById(R.id.obstacle1);
        obstacle2 = findViewById(R.id.obstacle2);
        obstacle3 = findViewById(R.id.obstacle3);
        obstacle4 = findViewById(R.id.obstacle4);
        obstacle5 = findViewById(R.id.obstacle5);
        obstacle6 = findViewById(R.id.obstacle6);
        obstacle7 = findViewById(R.id.obstacle7);
        obstacle8 = findViewById(R.id.obstacle8);

        obstacles = new HashMap<Integer, ImageView>() {{
            put(1, obstacle1);
            put(2, obstacle2);
            put(3, obstacle3);
            put(4, obstacle4);
            put(5, obstacle5);
            put(6, obstacle6);
            put(7, obstacle7);
            put(8, obstacle8);
        }};

        setupObstacleRotation(obstacle1, "o1");
        setupObstacleRotation(obstacle2, "o2");
        setupObstacleRotation(obstacle3, "o3");
        setupObstacleRotation(obstacle4, "o4");
        setupObstacleRotation(obstacle5, "o5");
        setupObstacleRotation(obstacle6, "o6");
        setupObstacleRotation(obstacle7, "o7");
        setupObstacleRotation(obstacle8, "o8");

        setOnTouchListenerForObstacle(obstacle1, "obstacle1");
        setOnTouchListenerForObstacle(obstacle2, "obstacle2");
        setOnTouchListenerForObstacle(obstacle3, "obstacle3");
        setOnTouchListenerForObstacle(obstacle4, "obstacle4");
        setOnTouchListenerForObstacle(obstacle5, "obstacle5");
        setOnTouchListenerForObstacle(obstacle6, "obstacle6");
        setOnTouchListenerForObstacle(obstacle7, "obstacle7");
        setOnTouchListenerForObstacle(obstacle8, "obstacle8");
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setOnTouchListenerForObstacle(ImageView obstacle, String obstacleName) {
        obstacle.setOnTouchListener(new View.OnTouchListener() {
            int lastX = 0;
            int lastY = 0;
            float dX = 0;
            float dY = 0;
            int orientation;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!cansetobstacles) {
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        orientation = (int) obstacle.getRotation();
                        obstacle.setRotation(0);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        handleMove(obstacle, event);
                        break;

                    case MotionEvent.ACTION_UP:
                        handleSnapToGrid(obstacle, obstacleName, orientation);
                        break;

                    default:
                        break;
                }
                return true;
            }

            private void handleMove(ImageView obstacle, MotionEvent event) {
                float x = event.getRawX();
                float y = event.getRawY();
                dX = x - lastX;
                dY = y - lastY;

                float newX = obstacle.getX() + dX;
                float newY = obstacle.getY() + dY;

                int obstacleWidth = obstacle.getWidth();
                int obstacleHeight = obstacle.getHeight();

                int gridWidth = 23 * 35; // 700 pixels
                int gridHeight = 20 * 35; // 700 pixels

                newX = Math.max(0, Math.min(newX, gridWidth - obstacleWidth));
                newY = Math.max(0, Math.min(newY, gridHeight - obstacleHeight));

                obstacle.setX(newX);
                obstacle.setY(newY);

                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
            }

            private void handleSnapToGrid(ImageView obstacle, String obstacleName, int orientation) {
                int snapToX = ((int) ((obstacle.getX() + SNAP_GRID_INTERVAL / 2) / SNAP_GRID_INTERVAL)) * SNAP_GRID_INTERVAL;
                int snapToY = ((int) ((obstacle.getY() + SNAP_GRID_INTERVAL / 2) / SNAP_GRID_INTERVAL)) * SNAP_GRID_INTERVAL;
                Log.d(TAG, obstacleName + " is at " + snapToX + "," + snapToY);

                //amu: send to rpi obstacle location after snap to grid in the format
                //STM:ADD,3,16,15,N         |         STM:ADD,<obstacleID>,<x>,<y>,<direction>
                //STM:SUB 3                 |         STM:SUB <obstacleID>

                if(BluetoothService.BluetoothConnectionStatus){
                    if(getObstacleString(obstacle).isEmpty()){
                        String btRemoveObstacleMsg = "STM:SUB," + obstacleName.replace("obstacle", "");
                        BluetoothService.write(btRemoveObstacleMsg.getBytes(StandardCharsets.UTF_8));
                    }else{
                        String btSnapToGridMessage = "STM:ADD,"+obstacleName.replace("obstacle", "") + "," + getObstacleString(obstacle);
                        btSnapToGridMessage = btSnapToGridMessage.substring(0, btSnapToGridMessage.length() - 1);
                        BluetoothService.write(btSnapToGridMessage.getBytes(StandardCharsets.UTF_8));
                    }


                }else {
                    Log.d(TAG, "Bluetooth not connected");
                }

                obstacle.setX(snapToX);
                obstacle.setY(snapToY);
                obstacle.setRotation(orientation % 360);
            }
        });
    }

        /*
     * Set obstacle ID images
     */
    private void setObstacleImageID(int obstacleNumber, String image) {
        int orientation = (int) obstacles.get(obstacleNumber).getRotation();
        ImageView iv = obstacles.get(obstacleNumber);
        Log.d("Obstacle ID", String.valueOf(iv.getId()));
        try {
            if (orientation == 0) {
                obstacles.get(obstacleNumber).setImageResource(Helper.resources.get(image + "n"));
            } else if (orientation == 90) {
                obstacles.get(obstacleNumber).setImageResource(Helper.resources.get(image + "e"));
            } else if (orientation == 180) {
                obstacles.get(obstacleNumber).setImageResource(Helper.resources.get(image + "s"));
            } else if (orientation == 270) {
                obstacles.get(obstacleNumber).setImageResource(Helper.resources.get(image + "w"));
            } else {
                obstacles.get(obstacleNumber).setImageResource(Helper.resources.get(image));
                obstacles.get(obstacleNumber).setRotation(0);
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    /*
     * Initializes the arrow buttons
     */
    private void initialiseMovementButtons() {
        ImageButton forwardButton = (ImageButton) findViewById(R.id.forwardButton);
        forwardButton.setOnClickListener(v -> {
             Log.d(TAG, "forward");

            // Bluetooth message
            if (BluetoothService.BluetoothConnectionStatus) {
                // byte[] bytes = "STM:w100n".getBytes(Charset.defaultCharset());
                byte[] bytes = "STM:n".getBytes(Charset.defaultCharset());
                 BluetoothService.write(bytes);
            }

            // Animation
            forwardButton(1);
        });

        ImageButton reverseButton = (ImageButton) findViewById(R.id.reverseButton);
        reverseButton.setOnClickListener(v -> {
             Log.d(TAG, "reverse");

            // Bluetooth message
            if (BluetoothService.BluetoothConnectionStatus) {
                // byte[] bytes = "STM:s100n".getBytes(Charset.defaultCharset());
                byte[] bytes = "STM:s".getBytes(Charset.defaultCharset());
                 BluetoothService.write(bytes);
            }

            // Animation
            reverseButton(1);
        });

        ImageButton leftButton = (ImageButton) findViewById(R.id.leftButton);
        leftButton.setOnClickListener(v -> {
             Log.d(TAG, "left");

            if (BluetoothService.BluetoothConnectionStatus) {
                // byte[] bytes = "STM:ln".getBytes(Charset.defaultCharset());
                byte[] bytes = "STM:w".getBytes(Charset.defaultCharset());
                 BluetoothService.write(bytes);
            }

            leftCommand();
        });

        ImageButton rightButton = (ImageButton) findViewById(R.id.rightButton);
        rightButton.setOnClickListener(v -> {
             Log.d(TAG, "right");

            if (BluetoothService.BluetoothConnectionStatus) {
                // byte[] bytes = "STM:rn".getBytes(Charset.defaultCharset());
                byte[] bytes = "STM:e".getBytes(Charset.defaultCharset());
                 BluetoothService.write(bytes);
            }

            rightCommand();
        });
    }

    /**
     * Initalizes buttons, car and setup listeners
     */
    private void initialiseButtons() {
        // Declarations
        car = findViewById(R.id.car);
        car_x = findViewById(R.id.x_tv);
        car_y = findViewById(R.id.y_tv);
        car_dir = findViewById(R.id.dir_tv);
        IRButton = findViewById(R.id.IRButton);
        SPButton = findViewById(R.id.SPBtn);
        resetButton = findViewById(R.id.resetButton);
        preset1Button = findViewById(R.id.preset1Button);
        setButton = findViewById(R.id.setButton);
        saveButton = findViewById(R.id.saveButton);
        timerButton = findViewById(R.id.timerButton);
        statusWindow = findViewById(R.id.statusWindowText);

        // Events
        IRButton.setOnClickListener(view -> beginIRTask());
        SPButton.setOnClickListener(view -> beginSPTask());
        resetButton.setOnClickListener(view -> setResetButton());
        preset1Button.setOnClickListener(view -> setPreset1Button());
        setButton.setOnClickListener(view -> toggleSetMode());
        saveButton.setOnClickListener(view -> sendObstacles());
        timerButton.setOnClickListener(view -> stopTimerButton());

        // Initialize car to bottom left
        car.setX(INITIAL_X);
        car.setY(INITIAL_Y);
        updateXYDirText();
    }

    /*
     * Function to wait for certain amount of time
     */
    private void sleepFor(int time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
     * ======================
     * For movement commands:
     * 0 - North
     * 1 - East
     * 2 - South
     * 3 - West
     * ======================
     */

    private void forwardButton(int noOfGrids) {
        int orientation = (int) car.getRotation();
        int new_x, new_y;
        ObjectAnimator animator;
        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                new_y = (int) car.getY() - noOfGrids * SNAP_GRID_INTERVAL;
                car.setY(new_y);
                animator = ObjectAnimator.ofFloat(car, "y", new_y);
                animator.setDuration(noOfGrids * ANIMATOR_DURATION);
                animator.start();
                updateXYDirText();
                break;
            case 1:
                new_x = (int) car.getX() + noOfGrids * SNAP_GRID_INTERVAL;
                car.setX(new_x);
                animator = ObjectAnimator.ofFloat(car, "x", new_x);
                animator.setDuration(noOfGrids * ANIMATOR_DURATION);
                animator.start();
                updateXYDirText();
                break;
            case 2:
                new_y = (int) car.getY() + noOfGrids * SNAP_GRID_INTERVAL;
                car.setY(new_y);
                animator = ObjectAnimator.ofFloat(car, "y", new_y);
                animator.setDuration(noOfGrids * ANIMATOR_DURATION);
                animator.start();
                updateXYDirText();
                break;
            case 3:
                new_x = (int) car.getX() - noOfGrids * SNAP_GRID_INTERVAL;
                car.setX(new_x);
                animator = ObjectAnimator.ofFloat(car, "x", new_x);
                animator.setDuration(noOfGrids * ANIMATOR_DURATION);
                animator.start();
                updateXYDirText();
                break;
            default:
                // Shouldn't reach this case
                break;
        }
    }

    private void reverseButton(int noOfGrids) {
        int orientation = (int) car.getRotation();
        int new_x, new_y;
        ObjectAnimator animator;
        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                new_y = (int) car.getY() + noOfGrids * SNAP_GRID_INTERVAL;
                car.setY(new_y);
                animator = ObjectAnimator.ofFloat(car, "y", new_y);
                animator.setDuration(noOfGrids * ANIMATOR_DURATION);
                animator.start();
                updateXYDirText();
                break;
            case 1:
                new_x = (int) car.getX() - noOfGrids * SNAP_GRID_INTERVAL;
                car.setX(new_x);
                animator = ObjectAnimator.ofFloat(car, "x", new_x);
                animator.setDuration(noOfGrids * ANIMATOR_DURATION);
                animator.start();
                updateXYDirText();
                break;
            case 2:
                new_y = (int) car.getY() - noOfGrids * SNAP_GRID_INTERVAL;
                car.setY(new_y);
                animator = ObjectAnimator.ofFloat(car, "y", new_y);
                animator.setDuration(noOfGrids * ANIMATOR_DURATION);
                animator.start();
                updateXYDirText();
                break;
            case 3:
                new_x = (int) car.getX() + noOfGrids * SNAP_GRID_INTERVAL;
                car.setX(new_x);
                animator = ObjectAnimator.ofFloat(car, "x", new_x);
                animator.setDuration(noOfGrids * ANIMATOR_DURATION);
                animator.start();
                updateXYDirText();
                break;
            default:
                // Shouldn't reach this case
                break;
        }
    }

    public void leftCommand() {
        int orientation = (int) car.getRotation();
        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                car.setRotation(270);
                break;
            case 1:
                car.setRotation(0);
                break;
            case 2:
                car.setRotation(90);
                break;
            case 3:
                car.setRotation(180);
                break;
            default:
                // Shouldn't reach this case
                break;
        }

        updateXYDirText();
    };

    private void rightCommand() {
        int orientation = (int) car.getRotation();
        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                car.setRotation(90);
                break;
            case 1:
                car.setRotation(180);
                break;
            case 2:
                car.setRotation(270);
                break;
            case 3:
                car.setRotation(0);
                break;
            default:
                // Shouldn't reach this case
                break;
        }

        updateXYDirText();
    }

    /*
     * Slide left - forward
     */
    public void leftSlideCommand() {
        int orientation = (int) car.getRotation();
        int new_x, new_y;
        AnimatorSet animatorSet;
        ObjectAnimator moveStraightAnimator, moveStraightAgainAnimator;

        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                new_y = (int) car.getY() - 4 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 1:
                new_y = (int) car.getY() - SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 4 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 2:
                new_y = (int) car.getY() + 4 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 3:
                new_y = (int) car.getY() + SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 4 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            default:
                // Shouldn't reach this case
                break;
        }
    }

    /*
     * Slide right - forward
     */
    private void rightSlideCommand() {
        int orientation = (int) car.getRotation();
        int new_x, new_y;
        AnimatorSet animatorSet;
        ObjectAnimator moveStraightAnimator, moveStraightAgainAnimator;

        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                new_y = (int) car.getY() - 4 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 1:
                new_y = (int) car.getY() + SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 4 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 2:
                new_y = (int) car.getY() + 4 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 3:
                new_y = (int) car.getY() - SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 4 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            default:
                // Shouldn't reach this case
                break;
        }
        updateXYDirText();
    }

    /*
     * Medium turn left
     */
    public void leftMidButtonCommand() {
        int orientation = (int) car.getRotation();
        int new_x, new_y;
        AnimatorSet animatorSet;
        ObjectAnimator moveStraightAnimator, turnAnimator, moveStraightAgainAnimator;

        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                new_y = (int) car.getY() - 2 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 3 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", -90);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(270);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 1:
                new_y = (int) car.getY() - 3 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 2 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 0);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(0);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 2:
                new_y = (int) car.getY() + 2 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 3 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 90);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(90);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 3:
                new_y = (int) car.getY() + 3 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 2 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 180);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(180);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            default:
                // Shouldn't reach this case
                break;
        }
    }

    /*
     * Medium turn right
     */
    public void rightMidButtonCommand() {
        int orientation = (int) car.getRotation();
        int new_x, new_y;
        AnimatorSet animatorSet;
        ObjectAnimator moveStraightAnimator, turnAnimator, moveStraightAgainAnimator;

        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                new_y = (int) car.getY() - 2 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 3 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 90);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(90);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 1:
                new_y = (int) car.getY() + 3 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 2 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 180);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(180);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 2:
                new_y = (int) car.getY() + 2 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 3 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 270);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(270);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 3:
                new_y = (int) car.getY() - 3 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 2 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 360);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(0);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            default:
                // Shouldn't reach this case
                break;
        }
    }

    /*
     * Slide left - reverse
     */
    public void leftSlideReverseCommand() {
        int orientation = (int) car.getRotation();
        int new_x, new_y;
        AnimatorSet animatorSet;
        ObjectAnimator moveStraightAnimator, moveStraightAgainAnimator;

        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                new_y = (int) car.getY() + 4 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 1:
                new_y = (int) car.getY() - SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 4 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 2:
                new_y = (int) car.getY() - 4 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 3:
                new_y = (int) car.getY() + SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 4 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            default:
                // Shouldn't reach this case
                break;
        }
    }

    /*
     * Slide right - reverse
     */
    private void rightSlideReverseCommand() {
        int orientation = (int) car.getRotation();
        int new_x, new_y;
        AnimatorSet animatorSet;
        ObjectAnimator moveStraightAnimator, moveStraightAgainAnimator;

        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                new_y = (int) car.getY() + 4 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 1:
                new_y = (int) car.getY() + SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 4 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 2:
                new_y = (int) car.getY() - 4 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 3:
                new_y = (int) car.getY() - SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 4 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            default:
                // Shouldn't reach this case
                break;
        }
        updateXYDirText();
    }

    /*
     * reverse left
     */
    public void leftMidReverseButtonCommand() {
        int orientation = (int) car.getRotation();
        int new_x, new_y;
        AnimatorSet animatorSet;
        ObjectAnimator moveStraightAnimator, turnAnimator, moveStraightAgainAnimator;

        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                new_y = (int) car.getY() + 3 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 2 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 90);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(90);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 1:
                new_y = (int) car.getY() - 2 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 3 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 180);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(180);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 2:
                new_y = (int) car.getY() - 3 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 2 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 270);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(270);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 3:
                new_y = (int) car.getY() + 2 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 3 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 360);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(0);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            default:
                // Shouldn't reach this case
                break;
        }
    }

    /*
     * reverse right
     */
    public void rightMidReverseButtonCommand() {
        int orientation = (int) car.getRotation();
        int new_x, new_y;
        AnimatorSet animatorSet;
        ObjectAnimator moveStraightAnimator, turnAnimator, moveStraightAgainAnimator;

        switch (((orientation / 90) % 4 + 4) % 4) {
            case 0:
                new_y = (int) car.getY() + 3 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 2 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", -90);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(270);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 1:
                new_y = (int) car.getY() + 2 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 3 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 0);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(0);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 2:
                new_y = (int) car.getY() - 3 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() - 2 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 90);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(90);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            case 3:
                new_y = (int) car.getY() - 2 * SNAP_GRID_INTERVAL;
                new_x = (int) car.getX() + 3 * SNAP_GRID_INTERVAL;

                moveStraightAnimator = ObjectAnimator.ofFloat(car, "x", new_x);
                moveStraightAnimator.setDuration(ANIMATOR_DURATION);

                turnAnimator = ObjectAnimator.ofFloat(car, "rotation", 180);
                turnAnimator.setDuration(500);

                moveStraightAgainAnimator = ObjectAnimator.ofFloat(car, "y", new_y);
                moveStraightAgainAnimator.setDuration(ANIMATOR_DURATION);

                animatorSet = new AnimatorSet();
                animatorSet.playSequentially(moveStraightAnimator, turnAnimator, moveStraightAgainAnimator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        car.setY(new_y);
                        car.setX(new_x);
                        car.setRotation(180);
                        updateXYDirText();
                    }
                });
                animatorSet.start();
                break;
            default:
                // Shouldn't reach this case
                break;
        }
    }

    private void stopTimerButton() {
        Chronometer IRTimer = (Chronometer) findViewById(R.id.IRTimer);
        IRTimer.stop();
        updateStatusWindow("Ready");
    }

    private void beginIRTask() {
        String IRstart = "ALG:START";

        if (BluetoothService.BluetoothConnectionStatus) {
            // Toast.makeText(this, stringBuilder.toString(), Toast.LENGTH_SHORT).show();
//            byte[] bytes = IRstart.getBytes(Charset.defaultCharset());
//            BluetoothService.write(bytes);
            sendObstacles();
            Toast.makeText(Arena.this, "Obstacles sent", Toast.LENGTH_SHORT).show();
            updateStatusWindow("IR Started");
        } else {
            updateStatusWindow("IR Not Started");
            Toast.makeText(Arena.this, "Please connect to Bluetooth.", Toast.LENGTH_SHORT).show();
            sleepFor(ANIMATOR_DURATION);
            updateStatusWindow("Ready");
            return;
        }

        Chronometer IRTimer = (Chronometer) findViewById(R.id.IRTimer);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        IRTimer.setBase(elapsedRealtime);
        IRTimer.start();
    }

    private void beginSPTask() {
        if (BluetoothService.BluetoothConnectionStatus) {
            byte[] bytes = "STM:sp".getBytes(Charset.defaultCharset());
            BluetoothService.write(bytes);
            Toast.makeText(Arena.this, "Shortest Path Started.", Toast.LENGTH_SHORT).show();
            updateStatusWindow("SP Started");
        } else {
            updateStatusWindow("SP Not Started");
            Toast.makeText(Arena.this, "Please connect to Bluetooth.", Toast.LENGTH_SHORT).show();
            sleepFor(ANIMATOR_DURATION);
            updateStatusWindow("Ready");
            return;
        }

        Chronometer IRTimer = (Chronometer) findViewById(R.id.IRTimer);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        IRTimer.setBase(elapsedRealtime);
        IRTimer.start();
    }

    private void setResetButton() {
        // Reset Timer
        Chronometer IRTimer = (Chronometer) findViewById(R.id.IRTimer);
        IRTimer.setBase(SystemClock.elapsedRealtime());
        IRTimer.stop();
        updateStatusWindow("Ready");

        // Hard coded
        obstacle1.setTranslationX(0);
        obstacle1.setTranslationY(0);

        obstacle2.setTranslationX(0);
        obstacle2.setTranslationY(0);

        obstacle3.setTranslationX(0);
        obstacle3.setTranslationY(0);

        obstacle4.setTranslationX(0);
        obstacle4.setTranslationY(0);

        obstacle5.setTranslationX(0);
        obstacle5.setTranslationY(0);

        obstacle6.setTranslationX(0);
        obstacle6.setTranslationY(0);

        obstacle7.setTranslationX(0);
        obstacle7.setTranslationY(0);

        obstacle8.setTranslationX(0);
        obstacle8.setTranslationY(0);

        car.setX(INITIAL_X);
        car.setY(INITIAL_Y);
        car.setRotation(0);
        updateXYDirText();

        obstacle1.setImageResource(Helper.resources.get("o1n"));
        obstacle1.setTag(Helper.resources.get("o1n"));
        obstacle2.setImageResource(Helper.resources.get("o2n"));
        obstacle2.setTag(Helper.resources.get("o2n"));
        obstacle3.setImageResource(Helper.resources.get("o3n"));
        obstacle3.setTag(Helper.resources.get("o3n"));
        obstacle4.setImageResource(Helper.resources.get("o4n"));
        obstacle4.setTag(Helper.resources.get("o4n"));
        obstacle5.setImageResource(Helper.resources.get("o5n"));
        obstacle5.setTag(Helper.resources.get("o5n"));
        obstacle6.setImageResource(Helper.resources.get("o6n"));
        obstacle6.setTag(Helper.resources.get("o6n"));
        obstacle7.setImageResource(Helper.resources.get("o7n"));
        obstacle7.setTag(Helper.resources.get("o7n"));
        obstacle8.setImageResource(Helper.resources.get("o8n"));
        obstacle1.setTag(Helper.resources.get("o8n"));

        obstacle1.setRotation(0);
        obstacle2.setRotation(0);
        obstacle3.setRotation(0);
        obstacle4.setRotation(0);
        obstacle5.setRotation(0);
        obstacle6.setRotation(0);
        obstacle7.setRotation(0);
        obstacle8.setRotation(0);

        Toast.makeText(this, "Map Reset", Toast.LENGTH_SHORT).show();
    }

    private void setPreset1Button() {
        updateStatusWindow("Ready");

        obstacle1.setX(350);
        obstacle1.setY(70);
        obstacle1.setRotation(180);
        obstacle1.setImageResource(Helper.resources.get("o1s"));

        obstacle2.setX(595);
        obstacle2.setY(70);
        obstacle2.setRotation(270);
        obstacle2.setImageResource(Helper.resources.get("o2w"));

        obstacle3.setX(70);
        obstacle3.setY(105);
        obstacle3.setRotation(180);
        obstacle3.setImageResource(Helper.resources.get("o3s"));

        obstacle4.setX(560);
        obstacle4.setY(525);
        obstacle4.setRotation(180);
        obstacle4.setImageResource(Helper.resources.get("o4s"));

        obstacle5.setX(455);
        obstacle5.setY(630);
        obstacle5.setRotation(270);
        obstacle5.setImageResource(Helper.resources.get("o5w"));

        obstacle6.setX(210);
        obstacle6.setY(455);
        obstacle6.setRotation(0);
        obstacle6.setImageResource(Helper.resources.get("o6n"));

        obstacle7.setX(315);
        obstacle7.setY(280);
        obstacle7.setRotation(270);
        obstacle7.setImageResource(Helper.resources.get("o7w"));

        obstacle8.setX(105);
        obstacle8.setY(560);
        obstacle8.setRotation(90);
        obstacle8.setImageResource(Helper.resources.get("o8e"));

        Toast.makeText(Arena.this, "Preset 1 Applied", Toast.LENGTH_SHORT).show();
    }




    private void toggleSetMode() {
        cansetobstacles = !cansetobstacles;
        if (curMode.equals("IDLE")) {
            curMode = "SET";
            setButton.setText("Done");
            Toast.makeText(this, "In set mode", Toast.LENGTH_SHORT).show();
        } else if (curMode.equals("SET")) {
            curMode = "IDLE";
            setButton.setText("Set");
            Toast.makeText(this, "Obstacles set", Toast.LENGTH_SHORT).show();
        }
    }

    private void setObstacles(String[] obstaclesPreset) {
        if (obstaclesPreset.length == 0) {
            Toast.makeText(this, "No saved preset found", Toast.LENGTH_SHORT).show();
        } else {
            for (int i = 0; i < obstaclesPreset.length; i++) {
                int obstaclenum = i + 1;
                String[] obsdata = obstaclesPreset[i].split(",");
                Log.d(TAG, "Obstacle preset length is " + obstaclesPreset.length);
                Log.d(TAG, "obstacle " + obstaclenum + " data is " + Arrays.toString(obsdata));

                obstacles.get(obstaclenum).setX(Integer.parseInt(obsdata[0]) * 40);
                obstacles.get(obstaclenum).setY(Integer.parseInt(obsdata[1]) * 40);
                switch (obsdata[2]) {
                    case ("N"):
                        obstacles.get(obstaclenum).setRotation(0);
                        obstacles.get(obstaclenum).setImageResource(Helper.resources.get("o" + obstaclenum + "n"));
                        break;
                    case ("E"):
                        obstacles.get(obstaclenum).setRotation(90);
                        obstacles.get(obstaclenum).setImageResource(Helper.resources.get("o" + obstaclenum + "e"));
                        break;
                    case ("S"):
                        obstacles.get(obstaclenum).setRotation(180);
                        obstacles.get(obstaclenum).setImageResource(Helper.resources.get("o" + obstaclenum + "s"));
                        break;
                    case ("W"):
                        obstacles.get(obstaclenum).setRotation(270);
                        obstacles.get(obstaclenum).setImageResource(Helper.resources.get("o" + obstaclenum + "w"));
                        break;
                    default:
                        break;
                }
            }
        }
    }
// amu: improved code eff with this func, for the rotation of the obj
    private void setupObstacleRotation(ImageView obstacle, String obstacleKey) {
        obstacle.setOnClickListener(view -> {
            obstacle.setRotation((obstacle.getRotation() + 90) % 360);
            int orientation = (int) obstacle.getRotation();
            String direction;

            switch (((orientation / 90) % 4 + 4) % 4) {
                case 0: direction = "n"; break;
                case 1: direction = "e"; break;
                case 2: direction = "s"; break;
                case 3: direction = "w"; break;
                default: return;
            }
            //amu
            //STM:ADD,3,16,15,N         |         STM:ADD,<obstacleID>,<x>,<y>,<direction>
            obstacle.setImageResource(Helper.resources.get(obstacleKey + direction));
            Snackbar.make(view, "Object "+ obstacleKey.replace("o","")+ " Rotated to " + direction, Snackbar.LENGTH_SHORT).show();
            String btMessageRotatedObstacle = "STM:ADD,"+ obstacleKey.replace("o","") +"," + getObstacleString(obstacle);
            btMessageRotatedObstacle = btMessageRotatedObstacle.substring(0, btMessageRotatedObstacle.length() - 1);

            BluetoothService.write(btMessageRotatedObstacle.getBytes());

        });
    }


    private void sendObstacles() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("ALG:")
                .append(getObstacleString(obstacle1) + "0;")
                .append(getObstacleString(obstacle2) + "1;")
                .append(getObstacleString(obstacle3) + "2;")
                .append(getObstacleString(obstacle4) + "3;")
                .append(getObstacleString(obstacle5) + "4;")
                .append(getObstacleString(obstacle6) + "5;")
                .append(getObstacleString(obstacle7) + "6;")
                .append(getObstacleString(obstacle8) + "7;");
        Log.d(TAG, stringBuilder.toString());

        if (BluetoothService.BluetoothConnectionStatus) {
            byte[] bytes = stringBuilder.toString().getBytes(Charset.defaultCharset());
            BluetoothService.write(bytes);
            Toast.makeText(Arena.this, "Obstacles sent", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * Returns 2-D array of obstacles in [x, y, direction] format
     */
    private String[][] savedObstacles() {
        String[][] savedPreset = { getObstacleLocation(obstacle1).split(","), getObstacleLocation(obstacle2).split(","),
                getObstacleLocation(obstacle3).split(","),
                getObstacleLocation(obstacle4).split(","), getObstacleLocation(obstacle5).split(","),
                getObstacleLocation(obstacle6).split(","),
                getObstacleLocation(obstacle7).split(","), getObstacleLocation(obstacle8).split(",") };
        // {1,2,N,2,3,E}
        Log.d(TAG, "Saved obstacle data: " + savedPreset);
        return savedPreset;
    }

    /*
     * Get obstacle location in <x, y, direction> format
     */
    private String getObstacleLocation(ImageView obstacle) {
        return (int) obstacle.getX() + "," + (int) obstacle.getY() + "," + getImageOrientation(obstacle);
    }

    /*
     * Get obstacle location in <x, y, direction> format
     * NOTE: 19 because grid is only 20 x 20
     */
    private String getObstacleString(ImageView obstacle) {
        int x = (int) obstacle.getX() / SNAP_GRID_INTERVAL;
        int y = (int) obstacle.getY() / SNAP_GRID_INTERVAL;
        // (0,0) starts from top left hence invert y
        int new_y = 20 - y - 1;
        Log.d(TAG, "Obstacle at " + x + "," + new_y);

        if (x > 19 || new_y > 19) {
            return "";
        } else {
            return x + "," + new_y
                    + ","
                    + getImageOrientation(obstacle) + ",";
        }
    }

    private String getImageOrientation(ImageView obstacle) {
        switch (((int) ((obstacle.getRotation() / 90) % 4 + 4) % 4)) {
            case 0:
                return "N";
            case 1:
                return "E";
            case 2:
                return "S";
            case 3:
                return "W";
            default:
                return "X";
        }
    }

    private void updateStatusWindow(String msg) {
        statusWindow.setText(msg);
        Log.d(TAG, "Status window: " + msg);
    }

    private void updateRobotPosition(int x, int y, int direction) {
        car.setX(x * SNAP_GRID_INTERVAL - SNAP_GRID_INTERVAL);
        car.setY(y * SNAP_GRID_INTERVAL - SNAP_GRID_INTERVAL);
        switch (direction) {
            case 7: // North-west
                car.setRotation(315);
                break;
            case 0: // North
                car.setRotation(0);
                break;
            case 1: // North-east
                car.setRotation(45);
                break;
            case 2: // East
                car.setRotation(90);
                break;
            case 3: // South-east
                car.setRotation(135);
                break;
            case 4: // South
                car.setRotation(180);
                break;
            case 5: // South-west
                car.setRotation(225);
                break;
            case 6: // West
                car.setRotation(270);
                break;
            default:
                // Shouldn't reach this case
                break;
        }

        updateXYDirText();
    }

    private void updateXYDirText() {
        int x = (int) (car.getX() + SNAP_GRID_INTERVAL) / SNAP_GRID_INTERVAL;
        int y = (int) (car.getY() + SNAP_GRID_INTERVAL) / SNAP_GRID_INTERVAL;
        // (0,0) starts from top left hence invert y
        int new_y = 20 - y - 1;
        car_x.setText(String.valueOf(x));
        car_y.setText(String.valueOf(new_y));

        int direction = (int) car.getRotation();

        if (direction == 315)
            car_dir.setText("North-West");
        else if (direction == 0)
            car_dir.setText("North");
        else if (direction == 45)
            car_dir.setText("North-East");
        else if (direction == 90)
            car_dir.setText("East");
        else if (direction == 135)
            car_dir.setText("South-East");
        else if (direction == 180)
            car_dir.setText("South");
        else if (direction == 225)
            car_dir.setText("South-West");
        else if (direction == 270)
            car_dir.setText("West");
        else
            car_dir.setText("None");
    }

    // Broadcast Receiver for incoming messages
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            String command;
            Log.d(TAG, "Received message: " + message);

            try {
                command = message.substring(0, message.indexOf(','));
            } catch (IndexOutOfBoundsException e) {
                Toast.makeText(Arena.this, "Invalid message format!",
                Toast.LENGTH_SHORT).show();
                return;
            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e.getMessage());
                return;
            }

            try {
                switch (command) {
                    // move robot
                    case Helper.ROBOT:
                        int startingIndex = message.indexOf("<");
                        int endingIndex = message.indexOf(">");
                        String x = message.substring(startingIndex + 1, endingIndex);

                        startingIndex = message.indexOf("<", endingIndex + 1);
                        endingIndex = message.indexOf(">", endingIndex + 1);
                        String y = message.substring(startingIndex + 1, endingIndex);
                        int adjusted_y = 19 - Integer.parseInt(y);

                        startingIndex = message.indexOf("<", endingIndex + 1);
                        endingIndex = message.indexOf(">", endingIndex + 1);
                        String direction = message.substring(startingIndex + 1, endingIndex);

                        Log.d("ROBOT", "(x: " + x + ") (y: " + adjusted_y + ") (direction: " + direction + ")");

                        int direction_int = 0;
                        switch (direction) {
                            case "N":
                                direction_int = 0;
                                break;
                            case "NE":
                                direction_int = 1;
                                break;
                            case "E":
                                direction_int = 2;
                                break;
                            case "SE":
                                direction_int = 3;
                                break;
                            case "S":
                                direction_int = 4;
                                break;
                            case "SW":
                                direction_int = 5;
                                break;
                            case "W":
                                direction_int = 6;
                                break;
                            case "NW":
                                direction_int = 7;
                                break;
                            default:
                                break;
                        }

                        //THIS is the inverted grid, meaning x axis is inverted
                        updateRobotPosition(Integer.parseInt(x), Integer.parseInt(y), direction_int);

                        //this grid is the normal grid, as in the x and y axis makes sense
                        //updateRobotPosition(Integer.parseInt(x), adjusted_y, direction_int);
                        break;

                    // update obstacle ID (format - TARGET,obstacle_number,target_ID)
                    case Helper.TARGET:
                        int obstacleNumber = Character.getNumericValue(message.charAt(7)) - 1;
                        String solution = message.substring(9);
                        Log.d(TAG, "Solution value: " + solution);
                        if (Integer.parseInt(solution) == 0) {
                            Toast.makeText(Arena.this, "Image not recognized, trying again", Toast.LENGTH_SHORT).show();
                        } else {
                            // RMB TO PLUS 1 !!
                            setObstacleImageID(obstacleNumber + 1, solution);
                            Toast.makeText(Arena.this,
                                    "Obstacle " + (obstacleNumber + 1) + " changed to Target ID: " + solution,
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;

                    // update status window
                    case Helper.STATUS:
                        String msg;
                        if (message.contains("\n")) {
                            msg = message.substring(message.indexOf(',') + 1, message.indexOf('\n'));
                        } else {
                            msg = message.substring(message.indexOf(',') + 1);
                        }
                        if (message.contains("STOPPED")) {
                            Chronometer IRTimer = (Chronometer) findViewById(R.id.IRTimer);
                            IRTimer.stop();
                            updateStatusWindow("IR Completed");
                        } else {
                            updateStatusWindow(msg);
                        }
                        break;

                    // plot obstacles
                    case Helper.PLOT:
                        String receivedmsg = message.substring(message.indexOf(",") + 1); // string after PLOT,
                        String[] obstaclesPreset = receivedmsg.split(";"); // create 2d array for obstacle data\
                        Log.d(TAG, "Obstacle data is " + Arrays.toString(obstaclesPreset));
                        setObstacles(obstaclesPreset);
                        break;

                    // commands from RPI
                    case Helper.COMMAND:
                        String moveCommand = message.substring(message.indexOf(',') + 1); //
                        // substring after COMMANDS
                        Log.d(TAG, "Command received: " + moveCommand);

                        String prefix = moveCommand.substring(0, 2);
                        String distance = moveCommand.substring(2);
                        int convertedDistance;
                        Log.d(TAG, prefix + ";" + distance);

                        switch (prefix) {
                            // forward
                            case "SF":
                                convertedDistance = Integer.parseInt(distance) / 10;
                                Log.d(TAG, prefix + ";" + convertedDistance);
                                forwardButton(convertedDistance);
                                break;
                            // reverseA
                            case "SB":
                                convertedDistance = Integer.parseInt(distance) / 10;
                                reverseButton(convertedDistance);
                                break;
                            // right forward
                            case "RF":
                                rightMidButtonCommand();
                                break;
                            // right backward
                            case "RB":
                                rightMidReverseButtonCommand();
                                break;
                            // left forward
                            case "LF":
                                leftMidButtonCommand();
                                break;
                            // left backward
                            case "LB":
                                leftMidReverseButtonCommand();
                                break;
                            // slide right forward
                            case "JF":
                                rightSlideCommand();
                                break;
                            // slide right backward
                            case "JB":
                                rightSlideReverseCommand();
                                break;
                            // slide left forward
                            case "KF":
                                leftSlideCommand();
                                break;
                            // slide left backward
                            case "KB":
                                leftSlideReverseCommand();
                                break;
                        }
                    default:
                        // for out of "ROBOT/TARGET/STATUS/COMMAND" cases
                        break;
                }
            } catch (Exception e) {
                // TODO: handle exception
                Log.d(TAG, "Exception: " + e.getMessage());
                return;
            }
        }
    };
}

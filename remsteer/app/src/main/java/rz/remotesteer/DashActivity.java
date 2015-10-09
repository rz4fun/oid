package rz.remotesteer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.rz4fun.remotesteer.R;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import android.os.Vibrator;

public class DashActivity extends ActionBarActivity {

  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dash);
    steering_wheel_imageview_ = (ImageView)findViewById(R.id.steer_image);
    speed_seekbar_ = (VerticalSeekBar)findViewById(R.id.speed_seekbar);
    engine_button_ = (ImageButton)findViewById(R.id.engine_button);
    light_switch_ = (ImageButton)findViewById(R.id.light_switch);
    hazard_switch_ = (ImageButton)findViewById(R.id.hazard_imagebutton);
    needle_imageview_ = (ImageView)findViewById(R.id.needle_image);
    // non UI variables
    vibrator_ = (Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE);
    light_on_ = false;
    hazard_blink_on_ = false;
    speed_ = 0;
    previous_pitch_ = SENSOR_INITIAL_VALUE;
    previous_roll_ = SENSOR_INITIAL_VALUE;
    // Configuring the light switch.
    light_switch_.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (engine_on_) {
          if (!light_on_) {
            new VehicleController().execute(COMMAND_CATEGORY_LIGHT, LIGHT_ON);
            light_switch_.setImageResource(R.drawable.light_switch_on_80);
            light_on_ = true;
          } else {
            new VehicleController().execute(COMMAND_CATEGORY_LIGHT, LIGHT_OFF);
            light_switch_.setImageResource(R.drawable.light_switch_off_80);
            light_on_ = false;
          }
        }
      }
    });
    hazard_switch_.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (engine_on_) {
          if (!hazard_blink_on_) {
            new VehicleController().execute(COMMAND_CATEGORY_HAZARD, 1);
            hazard_switch_.setImageResource(R.drawable.hazard_switch_on_80);
            hazard_blink_on_ = true;
          } else {
            new VehicleController().execute(COMMAND_CATEGORY_HAZARD, 0);
            hazard_switch_.setImageResource(R.drawable.hazard_switch_off_80);
            hazard_blink_on_ = false;
          }
        }
      }
    });
    // Configuring the engine start button.
    engine_button_.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!engine_on_) {
          new EngineStarter().execute("Start");
        } else {
          if (speed_ == 0) {
            ShutdownEngine();
          }
        }
      }
    });
    // Configuring the speed controller.
    speed_seekbar_.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
        SetSpeed(arg1);
      }

      @Override
      public void onStartTrackingTouch(SeekBar arg0) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar arg0) {
        SetSpeed(SPEED_ZERO);
        arg0.setProgress(SPEED_ZERO);
      }
    });
    // The code below is commented out temporarily until I make this an selectible option from the
    // main interface.
    /*
    speed_seekbar_.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
        if (arg1 > SPEED_ZERO) {
          drive_dir_ = DRIVE_FORWARD;
        } else if (arg1 < SPEED_ZERO) {
          drive_dir_ = DRIVE_BACKWARD;
        } else {
          drive_dir_ = DRIVE_NEUTRAL;
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar arg0) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar arg0) {
        SetSpeed(SPEED_ZERO);
        arg0.setProgress(SPEED_ZERO);
      }
    });
    */
    // Configuring the steering wheel.
    InitializeControlSensor();
  }


  @Override
  public void onBackPressed() {
    if (speed_ != 0) {
      return;
    }
    sensor_manager_.unregisterListener(sensor_event_listener_);
    ShutdownEngine();
    super.onBackPressed();
  }


  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    // need to configure the initial rotation here in that this is the place where the width and height are obtainable.
    needle_imageview_.setScaleX((float)0.9);
    needle_imageview_.setScaleY((float) 0.9);
    needle_imageview_.setPivotX(needle_imageview_.getWidth() / 2);
    needle_imageview_.setPivotY(needle_imageview_.getHeight() / 2);
    needle_imageview_.setRotation(NEEDLE_ANGLE_OFFSET);
  }


  public void UISetEngineOn() {
    engine_on_ = true;
    engine_button_.setImageResource(R.drawable.b4_120_green);
    engine_button_.setEnabled(true);
    connection_checker_ = new ConnectionChecker();
    connection_checker_.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }


  public void ShutdownEngine() {
    try {
      try_turn_on_ = false;
      if (socket_ != null && socket_.isConnected()) {
        command_writer_.write(COMMAND_OFF);
        command_writer_.flush();
        command_writer_.close();
        command_writer_ = null;
        response_reader_.close();
        response_reader_ = null;
        connection_checker_.cancel(true);
        socket_.close();
      }
      socket_ = null;
      engine_on_ = false;
      // UI updates
      engine_button_.setImageResource(R.drawable.b4_120_red);
      engine_button_.setEnabled(true);
      Toast.makeText(DashActivity.this, "Engine off", Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private void SetSteer(int steer_angle) {
    if (engine_on_) {
      steer_ = steer_angle;
      steering_wheel_imageview_.setRotation(steer_ - 90);
      new VehicleController().execute(COMMAND_CATEGORY_STEER, steer_angle);
      Log.d(APPLICATION_TAG, "Steer: " + steer_angle);
    }
  }


  private int ModulateSpeed(int raw_speed) {
    if (raw_speed <= 45) {
      return (int)((float)raw_speed * 0.25);
    } else if (raw_speed < 67.5) {
      return (int)(11.25 + raw_speed - 45);
    } else {
      return (int) ((90 - 33.75) / 22.5 * (raw_speed - 67.5) + 33.75);
    }
  }


  private void SetSpeed(int speed) {
    if (engine_on_) {
      speed_ = ModulateSpeed(speed > SPEED_ZERO ? (speed - SPEED_ZERO) : (SPEED_ZERO - speed));
      Log.d(APPLICATION_TAG, " Raw: " + speed + " Modulated speed: " + speed_);
      new VehicleController().execute(COMMAND_CATEGORY_SPEED, speed_);
      float needle_angle = NEEDLE_ANGLE_OFFSET + NEEDLE_ROTATE_RATION * speed_;
      needle_imageview_.setRotation(needle_angle);
    }
  }


  private int ScaleSpeedValue(int control_value) {
    return (int)(control_value * 90 / EFFECTIVE_PITCH);
  }


  private int InitializeControlSensor() {
    steering_wheel_imageview_.setRotation(STEER_CENTER - 90);
    sensor_manager_ = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    steer_sensor_ = sensor_manager_.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    rotation_matrix_[0] = 1;
    rotation_matrix_[4] = 1;
    rotation_matrix_[8] = 1;
    rotation_matrix_[12] = 1;
    final float[] orientation_values = new float[3];
    sensor_event_listener_ = new SensorEventListener() {
      public void onAccuracyChanged(Sensor sensor, int accuracy) {}

      public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
          SensorManager.getRotationMatrixFromVector(rotation_matrix_, event.values);
          SensorManager.remapCoordinateSystem(
                  rotation_matrix_, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotation_matrix_);
          SensorManager.getOrientation(rotation_matrix_, orientation_values);
          float pitch = (float) Math.toDegrees(orientation_values[1]);
          if (pitch > 70 || pitch < -10) {
            // Disable control if phone is almost parallel to the ground plane or perpendicular to it.
            return;
          }
          // The code below is commented out temporarily until I make this an selectable option from the
          // main interface.
          /*
          if (previous_pitch_ == SENSOR_INITIAL_VALUE || Math.abs(pitch - previous_pitch_) >= SENSOR_VALUE_THRESHOLD) {
            previous_pitch_ = pitch;
            if (speed_seekbar_.getSwitchPositionChanged()) {
              start_pitch_ = pitch;
              Log.d(APPLICATION_TAG, "Start Pitch: " + pitch);
              speed_seekbar_.setSwitchPositionChanged(false);
            }
            if ((pitch > start_pitch_) && (pitch < start_pitch_ + EFFECTIVE_PITCH)) {
              Log.d(APPLICATION_TAG, "Control Pitch: " + pitch);
              if (drive_dir_ == DRIVE_FORWARD) {
                SetSpeed(90 + ScaleSpeedValue((int) (pitch - start_pitch_)));
              } else if (drive_dir_ == DRIVE_BACKWARD) {
                SetSpeed(90 - ScaleSpeedValue((int) (pitch - start_pitch_)));
              }
            }
          }
          */
          float roll = (float) Math.toDegrees(orientation_values[2]);
          if (previous_roll_ == SENSOR_INITIAL_VALUE || Math.abs(roll - previous_roll_) >= SENSOR_VALUE_THRESHOLD) {
            if (roll < -180 || roll > 0) {
              return;
            }
            previous_roll_ = roll;
            SetSteer((int) (roll + 180));
          }
        }
      }
    };

    if (sensor_manager_.registerListener(sensor_event_listener_, steer_sensor_, SensorManager.SENSOR_DELAY_UI)) {
      return 0;
    }
    SetSteer(STEER_CENTER);
    return 1;
  }
  
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.dash, menu);
    return true;
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }


  private void SetSocketTimeout(int timeout) {
    if (socket_ == null || socket_.isClosed()) {
      return;
    }
    try {
      socket_.setSoTimeout(timeout);
    } catch (SocketException ex) {}
  }

  private boolean engine_on_ = false;
  private ImageView steering_wheel_imageview_;
  private VerticalSeekBar speed_seekbar_;
  private ImageButton engine_button_;
  private ImageButton light_switch_;
  private ImageButton hazard_switch_;
  private ImageView needle_imageview_;
  private boolean try_turn_on_;
  private boolean light_on_;
  private boolean hazard_blink_on_;
  private Socket socket_;
  private PrintWriter command_writer_;
  private InputStreamReader response_reader_;
  private ConnectionChecker connection_checker_;

  private int speed_;
  private int steer_;

  // SWITCH speed-control mode variables
  private int drive_dir_;
  private float start_pitch_;
  private float previous_pitch_;
  private static final int DRIVE_FORWARD = 1;
  private static final int DRIVE_NEUTRAL = 0;
  private static final int DRIVE_BACKWARD = -1;

  private SensorManager sensor_manager_;
  private Sensor steer_sensor_;
  private SensorEventListener sensor_event_listener_;
  private float previous_roll_;
  // sensor-related data
  private final float[] rotation_matrix_ = new float[16];
  private static final float SENSOR_VALUE_THRESHOLD = 0.5f;
  private static final float SENSOR_INITIAL_VALUE = 100f;  // just an out-of-range value

  private final String APPLICATION_TAG = "Remote-Steer";
  
  public static final int COMMAND_CATEGORY_SPEED = 1;
  public static final int COMMAND_CATEGORY_STEER = 2;
  public static final int COMMAND_CATEGORY_LIGHT = 3;
  public static final int COMMAND_CATEGORY_HAZARD = 4;
  public static final String COMMAND_OFF = "0#";

  public static final int STEER_CENTER = 90;
  public static final int SPEED_ZERO = 90;
  public static final int LIGHT_ON = 1;
  public static final int LIGHT_OFF = 0;

  public static final float EFFECTIVE_PITCH = 30;

  private static final float NEEDLE_ANGLE_OFFSET = 10;
  private static final float NEEDLE_ROTATE_RATION =
      (float)((180 - NEEDLE_ANGLE_OFFSET) - NEEDLE_ANGLE_OFFSET) / (float)SPEED_ZERO;

  private static final String SECURITY_TOKEN = "308ac3d3d02a3e6c0efe8e1a3f17df3d";

  private Vibrator vibrator_;


  private class EngineStarter extends AsyncTask<String, Integer, String> {

	@Override
	protected String doInBackground(String... params) {
      if (socket_ == null) {
        socket_ = new Socket();
      }
      if (!socket_.isConnected()) {
        int count = 0;
        while (count++ < 3) {
          try {
            if (!try_turn_on_) {
              return "OFF";
            }
            publishProgress(PROGRESS_START_ENGINE, count);
            socket_.connect(new InetSocketAddress("192.168.240.1", 5678), 3000);
            if (socket_.isConnected()) {
              command_writer_ =
                  new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket_.getOutputStream())), true);
              command_writer_.write(SECURITY_TOKEN + "#");
              command_writer_.flush();
              response_reader_ = new InputStreamReader(socket_.getInputStream());
              SetSocketTimeout(5000);
              if (response_reader_.read() == 0x08) {
                SetSocketTimeout(0);
                return "ON";
              }
            }
          } catch (SocketTimeoutException e) {
            Log.d(APPLICATION_TAG, "Waiting for handshake timeout.");
          } catch (UnknownHostException e) {
            Log.d(APPLICATION_TAG, APPLICATION_TAG + e.getLocalizedMessage());
          } catch (IOException e) {
            Log.d(APPLICATION_TAG, APPLICATION_TAG + e.getLocalizedMessage());
          }
        }
      }
      return "OFF";
    }


    @Override
    protected void onProgressUpdate(Integer... progress) {
      if (progress[0] == PROGRESS_START_ENGINE) {
        engine_button_.setImageResource(R.drawable.b4_120_yellow);
      }
    }


    @Override
    protected void onPreExecute () {
      engine_button_.setEnabled(false);
      try_turn_on_ = true;
      Toast.makeText(DashActivity.this, "Try starting engine", Toast.LENGTH_SHORT).show();
      vibrator_.vibrate(200);
    }

    @Override
    protected void onPostExecute (String result) {
      if (result == "ON") {
        UISetEngineOn();
      } else if (result == "OFF") {
        ShutdownEngine();
      }
      try_turn_on_ = false;
      engine_button_.setEnabled(true);
    }

    private int PROGRESS_START_ENGINE = 1;
  }
  
  
  private class VehicleController extends AsyncTask<Integer, Void, Void> {
	@Override
	protected Void doInBackground(Integer... command) {
      if (command_writer_ == null) {
        return null;
      }
      int category = command[0];
      int value = command[1];
      if (category == COMMAND_CATEGORY_STEER) {
        command_writer_.write("S" + value + "#");
      } else if (category == COMMAND_CATEGORY_SPEED) {
        command_writer_.write("D" + value + "#");
      } else if (category == COMMAND_CATEGORY_LIGHT) {
        command_writer_.write("L" + value + "#");
      } else if (category == COMMAND_CATEGORY_HAZARD) {
        command_writer_.write("H" + value + "#");
      }
      command_writer_.flush();
      return null;
	}
  }


  private class ConnectionChecker extends AsyncTask<Integer, String, String> {
    @Override
    protected String doInBackground(Integer... command) {
      // checks for when socket is available but there is no connection
      while (true) {
        // Once the connection terminates, read() will throw the IO exception.
        Log.d("remote steer", "Testing socket connection...");
        SetSocketTimeout(5);  // short timeout to avoid blocking on read unnecessarily
        try {
          if (response_reader_ != null) {
            response_reader_.read();
          }
        } catch (SocketTimeoutException ex) {
          Log.d("Remote steer", "Connection Timeout encountered");
        } catch(IOException ex) {
          SetSocketTimeout(0);
          return "DISCONNECT";
        }
        SetSocketTimeout(0);
        Log.d("remote steer", "connection good");
        try {
          Thread.sleep(3000);
        } catch (InterruptedException ex) {
          Log.d("Remote Steer", ex.getLocalizedMessage());
        }
      }
    }

    @Override
    protected void onPostExecute (String result) {
      if (result.equals("DISCONNECT")) {
        ShutdownEngine();
      }
    }
  }
}

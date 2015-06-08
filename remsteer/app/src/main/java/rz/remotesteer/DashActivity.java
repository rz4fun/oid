package rz.remotesteer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import com.example.remotesteer.R;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class DashActivity extends ActionBarActivity {

  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dash);
    status_textview_ = (TextView)findViewById(R.id.textView1);
    speed_textview_ = (TextView)findViewById(R.id.speed_textview);
    drive_reverse_switch_ = (Switch)findViewById(R.id.drive_reverse_switch);
    steering_wheel_imageview_ = (ImageView)findViewById(R.id.steer_image);
    speed_seekbar_ = (VerticalSeekBar)findViewById(R.id.speed_seekbar);
    engine_start_button_ = (ToggleButton)findViewById(R.id.power_button);
    final TextView d_textview = (TextView)findViewById(R.id.d_textview);
    final TextView r_textview = (TextView)findViewById(R.id.r_textview);
    // non UI variables
    drive_mode_ = DRIVE_FORWARD;
    // Configuring the drive-reverse switch.
    drive_reverse_switch_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked) {
          drive_mode_ = DRIVE_BACKWARD;
          r_textview.setTextColor(Color.GREEN);
          d_textview.setTextColor(Color.GRAY);
        } else {
          drive_mode_ = DRIVE_FORWARD;
          r_textview.setTextColor(Color.GRAY);
          d_textview.setTextColor(Color.GREEN);
        }
      }
    });
    // Configuring the engine start button.
    engine_start_button_.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        if (!engine_on_) {
          ConnectToVehicle();
        } else {
          DisconnectFromVehicle();
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
        SetSpeed(0);
        arg0.setProgress(0);
      }
    });
    // Configuring the steering wheel.
    InitializeRotationMeter();
  }


  @Override
  public void onBackPressed() {
    DisconnectFromVehicle();
    super.onBackPressed();
  }


  private void ConnectToVehicle() {
    int count = 0;
    engine_start_button_.setBackgroundColor(Color.YELLOW);
    engine_start_button_.setEnabled(false);
    while (count++ < 3) {
      try {
        if (new EngineStarter().execute("Start").get().equals("OK")) {
          engine_start_button_.setBackgroundColor(Color.GREEN);
          engine_start_button_.setChecked(true);
          engine_start_button_.setEnabled(true);
          Toast.makeText(DashActivity.this, "Engine on", Toast.LENGTH_SHORT).show();
          engine_on_ = true;
          return;
        }
        Toast.makeText(DashActivity.this, "Try starting engine X" + count, Toast.LENGTH_SHORT).show();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }
    engine_start_button_.setBackgroundColor(Color.RED);
    engine_start_button_.setEnabled(true);
    engine_start_button_.setChecked(false);
    engine_on_ = false;
  }
  
  
  private void DisconnectFromVehicle() {
    Log.d("Remote Steer", "Turing off engine...");
    try {
      if (new EngineStarter().execute("Shutdown").get().equals("OK")) {
        engine_start_button_.setBackgroundColor(Color.RED);
        engine_start_button_.setChecked(false);
        engine_start_button_.setEnabled(true);
        Toast.makeText(DashActivity.this, "Engine off", Toast.LENGTH_SHORT).show();
        engine_on_ = false;
        Log.d("Remote Steer", " Engine off.");
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
  }
  
  
  private void SetSteer(int steer_angle) {
    if (engine_on_) {
      steering_wheel_imageview_.setRotation(steer_angle - 90);
      new VehicleController().execute(COMMAND_CATEGORY_STEER, steer_angle);
      Log.d("Remote Steer", "Steer: " + steer_angle);
    }
  }
  
  
  private void SetSpeed(int speed) {
    if (engine_on_) {
      speed_textview_.setText("" + speed);
      new VehicleController().execute(COMMAND_CATEGORY_SPEED, speed);
      Log.d("Remote Steer", "Speed: " + speed);
    }
  }
  
  
  private int InitializeRotationMeter() {
    orientation_event_listener_ = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_GAME) {
      @SuppressLint("NewApi")
	  @Override
      public void onOrientationChanged(int orientation) {
        if (orientation == -1) {
          return;
        }
        int tmp_rotation_value = orientation % 360;
        if (tmp_rotation_value > 270 && tmp_rotation_value < 360) {
          steer_direction_ = STEER_RIGHT;
        } else if (tmp_rotation_value < 270 && tmp_rotation_value > 180) {
          steer_direction_ = STEER_LEFT;
        }
        if (tmp_rotation_value <= 180) {
          return;
        }
        rotation_degree_ = tmp_rotation_value - 180;
        //steering_wheel_imageview_.setRotation(orientation + 90);
        SetSteer(rotation_degree_);
      }
    };
    if (orientation_event_listener_.canDetectOrientation()){
      orientation_event_listener_.enable();
      return 0;
    } else{
      SetSteer(90);
      return 1;
    }
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

  private boolean engine_on_ = false;
  private OrientationEventListener orientation_event_listener_;
  private int rotation_degree_;
  private int speed_;
  private ImageView steering_wheel_imageview_;
  private Switch drive_reverse_switch_;
  private VerticalSeekBar speed_seekbar_;
  private TextView status_textview_;
  private TextView speed_textview_;
  private ToggleButton engine_start_button_;
  
  private Socket socket_;
  private PrintWriter command_writer_;
  
  //private final int GAS_OFFSET = 30;
  
  private final String APPLICATION_TAG = "RemoteSteer";
  private int drive_mode_;
  private int steer_direction_;
  
  public static final int COMMAND_CATEGORY_SPEED = 1;
  public static final int COMMAND_CATEGORY_STEER = 2;
  public static final String COMMAND_OFF = "0#";

  public static final int DRIVE_FORWARD = 1;
  public static final int DRIVE_NEUTRAL = 0;
  public static final int DRIVE_BACKWARD = -1;

  public static final int STEER_LEFT = -1;
  public static final int STEER_NEUTRAL = 0;
  public static final int STEER_RIGHT = 1;
  
  private class EngineStarter extends AsyncTask<String, Void, String> {

	@Override
	protected String doInBackground(String... params) {
      int mode = -1;
      if (params[0].equals("Start")) {
        mode = 1;
      } else if (params[0].equals("Shutdown")) {
        mode = 0;
      }
      if (socket_ == null) {
        if (mode == 1) {
          socket_ = new Socket();
        } else if (mode == 0) {
          // no existing connection
          return "OK";
        }
      }
      if (socket_.isConnected()) {
        if (mode == 1) {
          return "";
        } else if (mode == 0) {
          try {
            command_writer_.write(COMMAND_OFF);
            command_writer_.flush();
            command_writer_.close();
            command_writer_ = null;
            socket_.close();
            socket_ = null;
            return "OK";
          } catch (IOException e) {
            e.printStackTrace();
          }
          return "";
        }
      } else {
        if (mode == 1) {
          try {
            socket_.connect(new InetSocketAddress("192.168.240.1", 5678), 3000);
            if (socket_.isConnected()) {
              command_writer_ = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket_.getOutputStream())), true);
              return "OK";
            }
            return "";
          } catch (UnknownHostException e) {
            Log.d(APPLICATION_TAG, APPLICATION_TAG + e.getLocalizedMessage());
          } catch (IOException e) {
            Log.d(APPLICATION_TAG, APPLICATION_TAG + e.getLocalizedMessage());
          }
          return "";
        } else if (mode == 0) {
          // socket_ is present but there is no connection
          socket_ = null;
          return "OK";
        }
      }
      return "";
    }
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
          if (steer_direction_ == STEER_LEFT) {
            command_writer_.write("L" + value + "#");
          } else if (steer_direction_ == STEER_RIGHT) {
            command_writer_.write("R" + value + "#");
          }
		} else if (category == COMMAND_CATEGORY_SPEED) {
          if (drive_mode_ == DRIVE_FORWARD) {
            command_writer_.write("F" + value + "#");
          } else if (drive_mode_ == DRIVE_BACKWARD) {
            command_writer_.write("B" + value + "#");
          }
		}
		command_writer_.flush();
		return null;
	}
  }
  
}

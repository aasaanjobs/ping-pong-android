package aasaanjobsemployee.employee.aasaanjobs.com.socket;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.emitter.Emitter;


public class MainActivity extends ActionBarActivity implements SensorEventListener{
    private static final float MINIMUM_ACCELERATION_CHANGE = .15f;
    private static final float MAX_ALLOWED_DEACCELERATION = .4f;

    private io.socket.client.Socket socket;
    private Emitter.Listener onConnected =   new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            JSONObject j = new JSONObject();
            try {
                j.put("message","Message from android");
                socket.emit("chat message", j);
            } catch (JSONException e) {
                e.printStackTrace();
            }


            //socket.disconnect();
        }

    };
    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject j = (JSONObject) args[0];
            try {
                updateUI(j.getString("message"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e("socket","error");
        }
    };
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] acceleration;
    private long lastUpdated =0;
    private long lastUpdatedGlobal = System.currentTimeMillis();
    private float Vx=0;
    private float Vy=0;
    private float Vz=0;
    private float accelerationThreshold= 0.15f;
    float x = 0;
    private float MAX_ALLOWED_ACCELERATION= 5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer= sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);


    }

    @Override
    protected void onResume() {
        super.onResume();
        setupSocket();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void addMessage(String message) {
        TextView tv = (TextView) findViewById(R.id.message);
        tv.setText(message);
    }



    private void setupSocket() {
        SocketApplication application = (SocketApplication) getApplication();
        socket = application.getSocket();
        socket.on(io.socket.client.Socket.EVENT_CONNECT, onConnected);
        socket.on(io.socket.client.Socket.EVENT_CONNECT_ERROR,onError);
        socket.on(io.socket.client.Socket.EVENT_ERROR,onError);
        socket.on(io.socket.client.Socket.EVENT_RECONNECT_ERROR,onError);
        socket.on("chat message",onNewMessage);
        socket.connect();
    }

    private void updateUI(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                // add the message to view
                addMessage(message);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        updateAccelerationValues(acceleration);
        if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
                acceleration = sensorEvent.values;

            //MAX ALLOWED ACCELERATION
            if(Math.abs(acceleration[0])>MAX_ALLOWED_ACCELERATION){
                acceleration[0] = MAX_ALLOWED_ACCELERATION *(Math.abs(acceleration[0])/acceleration[0]);
            }

                if((Math.abs(acceleration[0])> MAX_ALLOWED_DEACCELERATION) && (acceleration[0]*Vx<0)){
                    //acceleration[0] = MAX_ALLOWED_DEACCELERATION*((Math.abs(acceleration[0])/acceleration[0]));
                    Vx = 0;
                    acceleration[0] = 0;
                }

               else if (Math.abs( acceleration[0]) > MINIMUM_ACCELERATION_CHANGE) {
                    float dt = (System.currentTimeMillis() - lastUpdatedGlobal) / 1000f;
                    lastUpdatedGlobal = System.currentTimeMillis();
                    Vx = Vx + acceleration[0] * dt;
                    Vy = Vy + acceleration[1] * dt;
                    Vz = Vz + acceleration[2] * dt;
                    x = x+Vx*dt;


//                    double vel = Math.sqrt(Vx * Vx + Vy * Vy + Vz * Vz);
//                    if ((vel > 0.1d) && ((System.currentTimeMillis() - lastUpdated) >= 500)) {
//                        acceleration[0] = (float) vel;
//                        //updateAccelerationValues(acceleration);
//                        lastUpdated = System.currentTimeMillis();
                    if(Math.abs(Vx)>1f) {
                        publishEvent(acceleration);
                    }
//                    }
                }else {
                    Vx = 0f;
                    Vy = 0f;
                    Vz = 0f;
                    publishEvent(acceleration);

                }

            }
    }

    private void publishEvent(float[] acceleration) {
        JSONObject j = new JSONObject();
        try {
            j.put("x",Vx);
            j.put("y",acceleration[1]);
            j.put("z",acceleration[2]);
            socket.emit("acceleration",j);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private boolean changed(float[] previousValues, float[] latestValues) {
        if(previousValues !=null) {
            return ( Math.abs( latestValues[0]) > MINIMUM_ACCELERATION_CHANGE || Math.abs(  latestValues[1]) > MINIMUM_ACCELERATION_CHANGE || Math.abs( latestValues[2]) > MINIMUM_ACCELERATION_CHANGE);
            //return true;
        }
        else return true;
    }

    private float difference(float v1, float v2) {
        return Math.abs(v1-v2);
    }

    private void updateAccelerationValues(float[] acceleration) {
        if(acceleration!=null) {
            TextView textView = (TextView) findViewById(R.id.tv_acceleration);

            String display = /*"Ax: " + acceleration[0] + " Vx:" + Vx + */" x:" + x;
            textView.setText(display);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

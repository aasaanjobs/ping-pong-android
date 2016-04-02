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
    private static final float MINIMUM_ACCELERATION_CHANGE = 1f;
    private static final int MAX_VALUES_COLLECTED = 100;
    private float x = 0;
    private float Vx = 0;
    private int damperCounter = 0;
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
    private long t=0;
    private float accumulatedAcceleration=0;


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
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
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
        //if(changed(acceleration, sensorEvent.values)) {
        acceleration = sensorEvent.values;
        accumulatedAcceleration +=sensorEvent.values[0];
        damperCounter++;
        if(damperCounter>=MAX_VALUES_COLLECTED){
            acceleration[0] = accumulatedAcceleration/damperCounter;
            Vx = Vx+acceleration[0];
            if(t==0){
                t = System.currentTimeMillis();
            }
            float dt = (System.currentTimeMillis()-t)/1000f;
            t = System.currentTimeMillis();
            x = x+Vx*dt;
            if((x<=-100 && Vx<0) || (x>=100 && Vx>0)){
                Vx=0;

            }

            updateAccelerationValues(acceleration);
            publishEvent(acceleration);
            damperCounter = 0;

        }

                //}

    }

    private void publishEvent(float[] acceleration) {
        JSONObject j = new JSONObject();
        try {
            j.put("x",x);
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
        TextView textView = (TextView) findViewById(R.id.tv_acceleration);

        String display = "x: "+x+ " y:"+acceleration[1]+ " z:"+acceleration[2];
        textView.setText(display);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

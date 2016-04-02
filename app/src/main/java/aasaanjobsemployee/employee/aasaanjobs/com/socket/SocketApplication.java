package aasaanjobsemployee.employee.aasaanjobs.com.socket;

import android.app.Application;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Created by ankitamaske on 30/3/16.
 */
public class SocketApplication extends Application {

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://192.168.0.14:3000");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }
}

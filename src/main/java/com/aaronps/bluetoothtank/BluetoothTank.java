package com.aaronps.bluetoothtank;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by krom on 2016-05-16.
 */
public final class BluetoothTank implements Runnable
{
    private static final long LOOP_RESTART_MS = 5000;

    private static final String TAG = "BluetoothTank";
    private static final UUID TANK_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    public interface Listener
    {
        void onBluetoothTankConnected(BluetoothTank tank);
        void onBluetoothTankDisconnected(BluetoothTank tank);
    }


    private final String mWantedDeviceName;
    private final Listener mListener;
    private volatile Thread mThread = null;

    /**
     * Procedure to use it: use a local to get the mSocket, then work with that if it is not null
     *  BluetoothSocket so = mSocket;
     *  if ( so != null ) so.whatever();
     */
    private volatile BluetoothSocket mSocket = null;

    final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    String mSpeed = "yidang";


    public BluetoothTank(final String wantedName, Listener listener)
    {
        mWantedDeviceName = wantedName;
        mListener = listener;
    }

    public synchronized void start()
    {
        if ( mThread == null )
        {
            Log.d(TAG, "start: starting");
            mThread = new Thread(this, TAG);
            mThread.start();
        }
        else
        {
            Log.d(TAG, "start: already running");
        }
    }

    public synchronized void stop() throws InterruptedException
    {
        if ( mThread != null )
        {
            Log.d(TAG, "stop: stopping");
            final Thread t = mThread;
            mThread = null;

            final BluetoothSocket btsocket = mSocket;
            if ( btsocket != null )
                try { btsocket.close(); } catch (IOException ignored) {}

            t.interrupt();
            // @note here could leak the thread t which should be finishing
            // anyway... the leak means if t.interrupts throws...
            t.join();
        }
        else
        {
            Log.d(TAG, "stop: wasn't running");
        }
    }


    @Override
    public void run()
    {
        Log.d(TAG, "run: enter");
        FastLoopProtection loopProtection = new FastLoopProtection(LOOP_RESTART_MS);

        while ( mThread != null )
        {
            try
            {
                loopProtection.sleep();
            }
            catch (InterruptedException e)
            {
                Log.d(TAG, "run: interrupted on loop protection");
                continue; // so it checks for exit condition
            }

            final BluetoothDevice bluetoothDevice = findPairedTank();
            if ( bluetoothDevice == null )
            {
                Log.d(TAG, "run: Device '" + mWantedDeviceName + "'not paired");
                continue;
            }

            try
            {
                mSocket = bluetoothDevice.createRfcommSocketToServiceRecord(TANK_UUID);
//                Log.d(TAG, "run: Socket Created");
                mSocket.connect();
            }
            catch (IOException e)
            {
//                Log.d(TAG, "run: Exception connecting", e);
                if ( mSocket != null )
                {
                    try { mSocket.close(); } catch (IOException ignored) {}
                    mSocket = null;
                }
                continue;
            }

            Log.d(TAG, "run: Connected");
            mListener.onBluetoothTankConnected(this);

            try
            {

                while ( mThread != null )
                {
                    int c = mSocket.getInputStream().read();
                    // ignore that it might close, will give exception later
//                    if ( c == -1 )
//                    {
//                        Log.d(TAG, "run: mInputStream closed");
//                        mSocket.close();
//                    }
                    Log.d(TAG, "run: Got a byte from the tank: " + Integer.toHexString(c));
                }
            }
            catch (IOException e)
            {
                Log.d(TAG, "run: Exception reading", e);
            }
            finally
            {
                try { mSocket.close(); } catch (IOException ignored) {}
                mSocket = null;
                mListener.onBluetoothTankDisconnected(this);
            }
        }

        Log.d(TAG, "run: exit");
    }

    BluetoothDevice findPairedTank()
    {
        for (BluetoothDevice d: mBluetoothAdapter.getBondedDevices())
        {
//            Log.d(TAG, "findPairedTank: Name: " + d.getName() + " Address: " + d.getAddress());
            if ( d.getName().equals(mWantedDeviceName) )
            {
                return d;
            }
        }
        return null;
    }

    public void commandSetSpeed(int speed)
    {
        switch (speed)
        {
            case 1: mSpeed = "yidang"; break;
            case 2: mSpeed = "erdang"; break;
            case 3: mSpeed = "sandang"; break;
            default:
                mSpeed = "yidang";
        }
    }
    public void commandSetSpeedNow(int speed)
    {
        commandSetSpeed(speed);

        BluetoothSocket socket = mSocket;
        if ( socket != null )
        {
            try
            {
                socket.getOutputStream().write(mSpeed.getBytes());
            }
            catch (IOException e)
            {
                Log.d(TAG, "commandSetSpeedNow: something happened", e);
            }
        }
    }

    public void commandStop()
    {
        BluetoothSocket socket = mSocket;
        if ( socket != null )
        {
            try
            {
                socket.getOutputStream().write("tingzhi".getBytes());
            }
            catch (IOException e)
            {
                Log.d(TAG, "commandStop: something happened", e);
            }
        }
    }

    public void commandUp()
    {
        BluetoothSocket socket = mSocket;
        if ( socket != null )
        {
            try
            {
                socket.getOutputStream().write((mSpeed + "qianjin").getBytes());
            }
            catch (IOException e)
            {
                Log.d(TAG, "commandStop: something happened", e);
            }
        }
    }

    public void commandDown()
    {
        BluetoothSocket socket = mSocket;
        if ( socket != null )
        {
            try
            {
                socket.getOutputStream().write((mSpeed + "houtui").getBytes());
            }
            catch (IOException e)
            {
                Log.d(TAG, "commandDown: something happened", e);
            }
        }
    }

    public void commandLeft()
    {
        BluetoothSocket socket = mSocket;
        if ( socket != null )
        {
            try
            {
                socket.getOutputStream().write((mSpeed + "zuozhuan").getBytes());
            }
            catch (IOException e)
            {
                Log.d(TAG, "commandLeft: something happened", e);
            }
        }
    }

    public void commandRight()
    {
        BluetoothSocket socket = mSocket;
        if ( socket != null )
        {
            try
            {
                socket.getOutputStream().write((mSpeed + "youzhuan").getBytes());
            }
            catch (IOException e)
            {
                Log.d(TAG, "commandRight: something happened", e);
            }
        }
    }

}

package org.burtons.ns.pyrros;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actions.ibluz.factory.BluzDeviceFactory;
import com.actions.ibluz.factory.IBluzDevice;
import com.actions.ibluz.manager.BluzManager;
import com.actions.ibluz.manager.BluzManagerData;
import org.burtons.ns.pyrros.R;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 101;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 102;

    private static final int CUSTOM_COMMAND_NAME = 128;
    private static final int CUSTOM_COMMAND_LIGHT = 131;

    private static final int MAX_BRIGHTNESS = 255;

    public BluzManager mBluzManager;
    public IBluzDevice mBluzConnector;

    private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) setBrightness(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleControls(false);

        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(mSeekBarListener);

        ImageView imageView = (ImageView)findViewById(R.id.imageViewRefresh);
        imageView.setTag(0L);

        if (checkPermissions()) createBluzConnector();
    }

    @Override
    protected void onDestroy() {
        destroyBluzConnector();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (mBluzManager != null) mBluzManager.setForeground(false);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluzManager != null) mBluzManager.setForeground(true);
    }

    private void toggleControls(boolean enabled) {
        this.findViewById(R.id.seekBar).setEnabled(enabled);
        this.findViewById(R.id.buttonOn).setEnabled(enabled);
        this.findViewById(R.id.buttonOff).setEnabled(enabled);
        this.findViewById(R.id.buttonRename).setEnabled(enabled);
    }

    private IBluzDevice.OnConnectionListener mOnConnectionListener = new IBluzDevice.OnConnectionListener() {
        public void onConnected(BluetoothDevice device) {
            createBluzManager();
            TextView textView = (TextView) findViewById(R.id.textViewState);
            textView.setText(getApplicationContext().getString(R.string.state_connected, device.getName(), device.getAddress()));
            toggleControls(true);
        }
        public void onDisconnected(BluetoothDevice device) {
            destroyBluzManager();
            TextView textView = (TextView) findViewById(R.id.textViewState);
            textView.setText(getApplicationContext().getString(R.string.state_disconnected));
            toggleControls(false);
        }
    };

    public void createBluzManager() {
        if (mBluzConnector == null) {
            mBluzManager = null;
        } else {
            destroyBluzManager();
            mBluzManager = new BluzManager(this, mBluzConnector, new BluzManagerData.OnManagerReadyListener() {
                public void onReady() {
                    if (mBluzManager != null) {
                        mBluzManager.setSystemTime();
                        mBluzManager.setOnCustomCommandListener(mOnCustomCommandListener);
                        mBluzManager.setForeground(true);
                    }
                }
            });
        }
    }

    public void destroyBluzManager() {
        if (mBluzManager != null) {
            mBluzManager.setOnCustomCommandListener(null);
            mBluzManager.release();
            mBluzManager = null;
        }
    }

    public void createBluzConnector() {
        destroyBluzConnector();
        mBluzConnector = BluzDeviceFactory.getDevice(this);
        if (mBluzConnector != null) mBluzConnector.setOnConnectionListener(mOnConnectionListener);
    }

    public void destroyBluzConnector() {
        destroyBluzManager();
        if (mBluzConnector != null) {
            mBluzConnector.setOnConnectionListener(null);
            mBluzConnector.release();
            mBluzConnector = null;
        }
    }

    public BluzManagerData.OnCustomCommandListener mOnCustomCommandListener = new BluzManagerData.OnCustomCommandListener() {
        public void onReady(int command, int arg1, int arg2, byte[] arg3) {
            switch (command) {
                case CUSTOM_COMMAND_LIGHT:
                    int brightness = (arg2 >> 8) & 255;
                    SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
                    seekBar.setProgress(brightness);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                if ((grantResults.length == 0) || (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(getApplicationContext(), this.getString(R.string.msg_perm_location), Toast.LENGTH_LONG).show();
                }
            }
            case MY_PERMISSIONS_REQUEST_BLUETOOTH: {
                if ((grantResults.length == 0) || (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(getApplicationContext(), this.getString(R.string.msg_perm_bluetooth), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private boolean checkPermissions() {
        boolean ret = true;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ret = false;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ret = false;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }

        return ret;
    }

    public boolean setName(String name) {

        if (mBluzManager == null) {
            Toast.makeText(this, this.getString(R.string.msg_not_connected), Toast.LENGTH_SHORT).show();
            return false;
        }

        // structure of this data comes from decompiling another app
        byte[] data = new byte[57];
        data[0] = 3;

        byte[] bytes = name.getBytes();
        if (bytes.length > 56) {
            // too long
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            data[i + 1] = bytes[i];
        }
        // pad with spaces to minimum length of 7 chars
        for (int i = bytes.length; i < 7; i++) {
            data[i + 1] = ' ';
        }

        int key = BluzManager.buildKey(BluzManagerData.CommandType.SET, CUSTOM_COMMAND_NAME);
        mBluzManager.sendCustomCommand(key, 0, 0, data);
        return true;
    }

    public void setBrightness(int brightness) {
        if (mBluzManager == null) {
            Toast.makeText(this, this.getString(R.string.msg_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        if (brightness < 0) brightness = 0;
        else if (brightness > MAX_BRIGHTNESS) brightness = MAX_BRIGHTNESS;
        int key = BluzManager.buildKey(BluzManagerData.CommandType.SET, CUSTOM_COMMAND_LIGHT);
        mBluzManager.sendCustomCommand(key, 0xffffff, ((brightness << 8) | 80), new byte[0]);
    }

    public void about(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(this.getString(R.string.title_about));

        LayoutInflater inflater = this.getLayoutInflater();
        View fragment = inflater.inflate(R.layout.fragment_about, null);
        builder.setView(fragment);

        builder.setPositiveButton(this.getString(R.string.button_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.show();
    }

    public void rename(View view) {
        if (mBluzManager == null) {
            Toast.makeText(this, this.getString(R.string.msg_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(this.getString(R.string.title_rename));

        LayoutInflater inflater = this.getLayoutInflater();
        View fragment = inflater.inflate(R.layout.fragment_rename, null);
        builder.setView(fragment);

        final EditText input = fragment.findViewById(R.id.editTextName);
        BluetoothDevice device = mBluzConnector.getConnectedDevice();
        if (device != null) input.setText(device.getName());

        builder.setPositiveButton(this.getString(R.string.button_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString().trim();
                if (setName(name)) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.msg_renamed, name), Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(this.getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void turnOn(View view) {
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        int brightness = seekBar.getProgress();
        if (brightness == 0) {
            brightness = MAX_BRIGHTNESS;
            seekBar.setProgress(MAX_BRIGHTNESS);
        }
        setBrightness(brightness);
    }

    public void turnOff(View view) {
        setBrightness(0);
    }

    public void refresh(View view) {

        ImageView imageView = (ImageView)view;
        if (SystemClock.elapsedRealtime() - (long)imageView.getTag() < 1500L){
            return;
        }
        imageView.setTag(SystemClock.elapsedRealtime());


        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(1000);
        rotate.setRepeatCount(0);
        imageView.startAnimation(rotate);

        if (checkPermissions()) createBluzConnector();
    }

}

package com.haubutz.kiss2go;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private UsbSerialPort port;
    private TextView statusText;
    private EditText rollP, rollI, rollD;
    private EditText pitchP, pitchI, pitchD;
    private EditText yawP, yawI, yawD;
    private EditText rateRoll, ratePitch, rateYaw;
    private Button readButton, writeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(layout);

        statusText = new TextView(this);
        layout.addView(statusText);

        rollP = createField(layout, "Roll P");
        rollI = createField(layout, "Roll I");
        rollD = createField(layout, "Roll D");
        pitchP = createField(layout, "Pitch P");
        pitchI = createField(layout, "Pitch I");
        pitchD = createField(layout, "Pitch D");
        yawP = createField(layout, "Yaw P");
        yawI = createField(layout, "Yaw I");
        yawD = createField(layout, "Yaw D");

        rateRoll = createField(layout, "Rate Roll");
        ratePitch = createField(layout, "Rate Pitch");
        rateYaw = createField(layout, "Rate Yaw");

        readButton = new Button(this);
        readButton.setText("Read from FC");
        layout.addView(readButton);

        writeButton = new Button(this);
        writeButton.setText("Write to FC");
        layout.addView(writeButton);

        setContentView(scroll);

        UsbManager manager = (UsbManager) getSystemService(USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            statusText.setText("No USB device found");
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();
        if (!manager.hasPermission(device)) {
            statusText.setText("No permission for USB device");
            return;
        }

        port = driver.getPorts().get(0);
        try {
            port.open(manager.openDevice(device));
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            statusText.setText("Error opening port: " + e.getMessage());
            return;
        }

        SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] data) {
                runOnUiThread(() -> parseData(new String(data)));
            }

            @Override
            public void onRunError(Exception e) {
                runOnUiThread(() -> statusText.setText("Error: " + e.getMessage()));
            }
        });

        Executors.newSingleThreadExecutor().submit(usbIoManager);

        readButton.setOnClickListener(v -> {
            try {
                port.write("get pid\n".getBytes(), 1000);
                port.write("get rates\n".getBytes(), 1000);
            } catch (IOException e) {
                statusText.setText("Read error: " + e.getMessage());
            }
        });

        writeButton.setOnClickListener(v -> {
            StringBuilder cmd = new StringBuilder();
            cmd.append(String.format("set pid %s %s %s %s %s %s %s %s %s\n",
                    rollP.getText(), rollI.getText(), rollD.getText(),
                    pitchP.getText(), pitchI.getText(), pitchD.getText(),
                    yawP.getText(), yawI.getText(), yawD.getText()));
            cmd.append(String.format("set rates %s %s %s\n",
                    rateRoll.getText(), ratePitch.getText(), rateYaw.getText()));
            try {
                port.write(cmd.toString().getBytes(), 1000);
                statusText.setText("Written to FC\n" + cmd);
            } catch (IOException e) {
                statusText.setText("Write error: " + e.getMessage());
            }
        });
    }

    private EditText createField(LinearLayout layout, String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        layout.addView(field);
        return field;
    }

    private void parseData(String data) {
        statusText.setText("Received:\n" + data);

        if (data.contains("PID:")) {
            String[] parts = data.split("[\\r\\n]+");
            for (String line : parts) {
                if (line.startsWith("PID:")) {
                    String[] tokens = line.split("[ :/]+");
                    if (tokens.length >= 13) {
                        rollP.setText(tokens[2]); rollI.setText(tokens[3]); rollD.setText(tokens[4]);
                        pitchP.setText(tokens[6]); pitchI.setText(tokens[7]); pitchD.setText(tokens[8]);
                        yawP.setText(tokens[10]); yawI.setText(tokens[11]); yawD.setText(tokens[12]);
                    }
                }
            }
        }

        if (data.contains("Rates:")) {
            String[] parts = data.split("[\\r\\n]+");
            for (String line : parts) {
                if (line.startsWith("Rates:")) {
                    String[] tokens = line.split("[ :]+");
                    if (tokens.length >= 7) {
                        rateRoll.setText(tokens[2]);
                        ratePitch.setText(tokens[4]);
                        rateYaw.setText(tokens[6]);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (port != null) {
                port.close();
            }
        } catch (IOException ignored) {}
    }
}

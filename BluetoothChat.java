package com.example.lilea.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
public class BluetoothChat extends AppCompatActivity {
    Toolbar mToolbar;
    TextView mTextView;
    private PopupMenu mOverflowMenu;
    private ImageView toolbarMoreIcon;
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private TextView mTitle;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    private String mConnectedDeviceName = null;

    private ArrayAdapter<String> mConversationArrayAdapter;

    private StringBuffer mOutStringBuffer;

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothChatService mChatService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view=getWindow().getDecorView().findViewById(android.R.id.content);
        Log.v("lilea","count==="+view.getC);
//        ViewGroup viewGroup=(ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content);
//        Log.v("lilea","count==="+viewGroup.getChildCount());

        setContentView(com.example.yuehuli.bluetoothdemo.R.layout.activity_bluetooth_chat);
        mTitle = (TextView) findViewById(com.example.yuehuli.bluetoothdemo.R.id.toolbar_title);
        mToolbar = (Toolbar) findViewById(com.example.yuehuli.bluetoothdemo.R.id.toolbar);
        toolbarMoreIcon = (ImageView) findViewById(com.example.yuehuli.bluetoothdemo.R.id.toolbar_more);
        setSupportActionBar(mToolbar);
        mTitle.setText(com.example.yuehuli.bluetoothdemo.R.string.toolbar_title);
        mOverflowMenu = buildOptionsMenu(toolbarMoreIcon);
        toolbarMoreIcon.setOnTouchListener(mOverflowMenu.getDragToOpenListener());
        toolbarMoreIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOverflowMenu.show();
            }
        });
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    private OptionsPopupMenu buildOptionsMenu(View invoker) {
        final OptionsPopupMenu popupMenu = new OptionsPopupMenu(this, invoker) {
            @Override
            public void show() {
                super.show();
            }
        };
        popupMenu.inflate(com.example.yuehuli.bluetoothdemo.R.menu.option_menu);
        popupMenu.setOnMenuItemClickListener(menuItemClickListener);
        return popupMenu;
    }

    protected class OptionsPopupMenu extends PopupMenu {
        public OptionsPopupMenu(Context context, View anchor) {
            super(context, anchor, Gravity.END);
        }

        @Override
        public void show() {
            super.show();
        }
    }

    PopupMenu.OnMenuItemClickListener menuItemClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case com.example.yuehuli.bluetoothdemo.R.id.scan:
                    Intent serverIntent = new Intent(BluetoothChat.this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                    break;
                case com.example.yuehuli.bluetoothdemo.R.id.discoverable:
                    ensureDiscoverable();
                    break;
                case com.example.yuehuli.bluetoothdemo.R.id.about_app:
                    Toast.makeText(getApplicationContext(), "Formate date", Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        } else {
            if (mChatService == null)
                setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    private void setupChat() {
        mConversationArrayAdapter = new ArrayAdapter<String>(this,
                com.example.yuehuli.bluetoothdemo.R.layout.message);
        mConversationView = (ListView) findViewById(com.example.yuehuli.bluetoothdemo.R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);
        mOutEditText = (EditText) findViewById(com.example.yuehuli.bluetoothdemo.R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);
        mSendButton = (Button) findViewById(com.example.yuehuli.bluetoothdemo.R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mOutEditText.getText().toString();
                sendMessage(message);
            }
        });

        mChatService = new BluetoothChatService(this, mHandler);
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mChatService != null)
            mChatService.stop();
    }

    private void ensureDiscoverable() {
        Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void sendMessage(String message) {

        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, com.example.yuehuli.bluetoothdemo.R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            byte[] temp={50};
            byte[] temp1={49};
            if(message.equals("关灯")){
                send=temp;
            }
            else if(message.equals("开灯")){
                send=temp1;
            }
            Log.v("lilealyh","send=="+send[0]);
            mChatService.write(send);
            mOutStringBuffer.setLength(0);//清空发送区
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId,
                                      KeyEvent event) {
            // If the action is a key-up event on the return key, send the
            // message
            if (actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D)
                        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            mTitle.setText(com.example.yuehuli.bluetoothdemo.R.string.title_connected_to);
                            mTitle.append(mConnectedDeviceName);
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            mTitle.setText(com.example.yuehuli.bluetoothdemo.R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            mTitle.setText(com.example.yuehuli.bluetoothdemo.R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.v("lilea","writeMessage==="+writeMessage);
                    if(writeMessage.equals("1")){
                        writeMessage="开灯";
                    }
                    else if(writeMessage.equals("2")){
                        writeMessage="关灯";
                    }
                    mConversationArrayAdapter.add("Send:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.v("lilea","readMessage==="+readMessage);
                    if(readMessage.equals("1")){
                        readMessage="请稍等...灯已打开";
                    }
                    else if(readMessage.equals("2")){
                        readMessage="请稍等...关闭中...";
                    }
                    mConversationArrayAdapter.add("Receive" + ":  "
                            + readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(),
                            "Connected to " + mConnectedDeviceName,
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D)
            Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(
                            DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter
                            .getRemoteDevice(address);
                    // Attempt to connect to the device
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, com.example.yuehuli.bluetoothdemo.R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

}

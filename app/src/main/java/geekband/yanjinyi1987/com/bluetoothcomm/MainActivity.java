package geekband.yanjinyi1987.com.bluetoothcomm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import geekband.yanjinyi1987.com.bluetoothcomm.fragment.BluetoothConnection;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int DEVICE_CONNECTED = 1;
    public static final int MESSAGE_READ=0;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean noBluetooth;
    private boolean bluetoothDisable;
    private static final int REQUEST_ENABLE_BT=1;
    private ArrayList<String> mConnectedBTDevices = new ArrayList<>();
    private boolean rwReady=false;
    public static final String[] array_growth_time = {
            "      0月","   0.5月","      1月","   1.5月",
            "      2月","   2.5月","      3月","   3.5月",
            "      4月","   4.5月","      5月","   5.5月",
            "      6月","   6.5月","      7月","   7.5月",
            "      8月","   8.5月","      9月","   9.5月",
            "    10月" ,"10.5月"  ,"    11月" ,"11.5月"
    };
    public int growth_time_index = 0;

    public static final String[] array_plants = {
            "<--未设置-->",
            "土豆",
            "棉花",
            "小麦"
    };

    public @IdRes int[] images_plants = {
            0,
            R.drawable.tudou,
            R.drawable.mianhua,
            R.drawable.xiaomai
    };

    public int plants_index = 0;

    // 初始值，温度系数，温度offset，湿度系数，光强系数
    public String[] controlData = {"0","0","30","0.3","20","0.01","0.05"};
    public String[] controlData_mianhua = {"0","0","10","0.6","90","0.01","0.05"};

    boolean isOver=false;

    //与fragmentDialog通信的Handler
    private Handler mMainActivityHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mBluetoothDevice = null;
            BluetoothSocket mBluetoothSocket=null;
            switch(msg.what) {
                case DEVICE_CONNECTED:
                    mBluetoothDevice = (BluetoothDevice) (((ArrayList<Object>) msg.obj).get(0));
                    mBluetoothSocket = (BluetoothSocket) (((ArrayList<Object>) msg.obj).get(1));

                    //建立读写通道哦！UI主线程与读写线程的交互
                    mSSPRWThread = new SSPRWThread(mBluetoothSocket);
                    mSSPRWThread.start();
                    //将功能区使能
                    rwReady=true;
                    enableViews();
                    //检测当前植物类型与参数

                    //循环获取当前测量参数
                    getControlData(mSSPRWThread);

                    //getSensorData(mSSPRWThread);
                    break;
                default:
                    break;
            }
        }
    };
    //循环读取
    Handler getSensorDataHandler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Log.i("Loop thread","A");
            isOver=false;
            mSSPRWThread.write("s".getBytes()); //发送读取命令
            getSensorDataHandler.postDelayed(this,10*1000); //10s
        }
    };

    private TextView mHumidityValue;
    private TextView mSoilHumidityValue;
    private TextView mTemperatureValue;
    private TextView mSunlightValue;
    private String strInitialValue;
    private String strTempCoefficient;
    private String strTempOffset;
    private String strHumidityCoefficient;
    private String strSunlightIntensity;
    private BluetoothDevice mBluetoothDevice;
    private Button mParametersSettingButton;

    void getSensorData(SSPRWThread mSSPRWThread) {
        Log.i("MainActivity","send command");
        mSSPRWThread.write("s".getBytes());
    }
    void getControlData(SSPRWThread mSSPRWThread) {
        mSSPRWThread.write("c".getBytes());
    }
    //与读写线程通信的Handler
    private Handler mReadSSPHandler = new Handler() {
        StringBuilder stringBuilder = new StringBuilder();
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MESSAGE_READ:
                    if(isOver) {
                        break;
                    }
                    int length = msg.arg1;
                    String strInfo =(String)(msg.obj); //问题在于这里直接引用了buffer，而buffer的数据会被read覆盖，这样造成了问题。
                    Log.i("MainActivity READ",strInfo);
                    stringBuilder.append(strInfo);
                    if(strInfo.indexOf('x')!=-1) {
                        isOver=true;
                        String resultInfo = stringBuilder.toString();
                        Log.i("Show result",resultInfo);
                        if(resultInfo.indexOf('s')!=-1) {
                            setUISensorData(resultInfo);
                            stringBuilder.delete(0,stringBuilder.length());//清除StringBuilder
                        }
                        else if(resultInfo.indexOf('c')!=-1) {
                            setUIControlData(resultInfo);
                            Log.i("get control data",resultInfo);
                            getSensorDataHandler.postDelayed(runnable,10*1000); //10s后isOver设为false
                            stringBuilder.delete(0,stringBuilder.length());//清除StringBuilder
                        }
                    }
                    //mReceivedSPPDataText.setText(mReceivedSPPDataText.getText()+strInfo);
                    //读取远程数据
                    break;
                default:
                    break;
            }
        }
    };

    void setUISensorData(String strInfo) {
        String[] sensorData = strInfo.split(",");
        mSoilHumidityValue.setText(sensorData[0].substring(1)+"%");
        mSunlightValue.setText(sensorData[1].substring(1)+"%");
        mTemperatureValue.setText(sensorData[2].substring(1,3)+"℃");
        mHumidityValue.setText(sensorData[3].substring(1,3)+"%");
    }

    void setUIControlData(String strInfo) {
        String[] controlData = strInfo.split(",");
        // 初始值，温度系数，温度offset，湿度系数，光强系数
        String plant_type = controlData[0].substring(1);
        plants_index = Float.valueOf(plant_type).intValue();
        mSpinnerPlant.setSelection(plants_index);
        String growth_time = controlData[1].substring(1);
        growth_time_index = Float.valueOf(growth_time).intValue();
        mSpinnerGrowthTime.setSelection(growth_time_index);
        strInitialValue = controlData[2].substring(1);
        strTempCoefficient = controlData[3].substring(1);
        strTempOffset = controlData[4].substring(1);
        strHumidityCoefficient = controlData[5].substring(1);
        strSunlightIntensity = controlData[6].substring(1);
    }

    //处理蓝牙接收器的状态变化
    /**
     * https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html#STATE_TURNING_ON
     * int STATE_OFF : Indicates the local Bluetooth adapter is off.
     * int STATE_ON  : Indicates the local Bluetooth adapter is on, and ready for use.
     * int STATE_TURNING_OFF : Indicates the local Bluetooth adapter is turning off.
     *                         Local clients should immediately attempt graceful disconnection of any remote links.
     *int STATE_TURNING_ON: Indicates the local Bluetooth adapter is turning on.
     *                      However local clients should wait for STATE_ON before attempting to use the adapter.
     */
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
                int state_previous = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE,-1);
                switch (state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        mConnectedBTDevices.clear(); //清除连接列表
                        //关闭读写通道
                        if(rwReady==true) {
                            mSSPRWThread.cancel();
                        }
                        //disable功能区
                        rwReady=false;
                        break;
                    case BluetoothAdapter.STATE_ON:

                        break;
                    default:
                        break;
                }
            }
        }
    };
    private Button mBTConnectionButton;
    private SSPRWThread mSSPRWThread;
    private TextView mReadRemoteDataButton;
    private Button mSetRemoteDataButton;
    //private ListView mParameterListView;
    //private ParameterArrayAdapter parameterArrayAdapter;
    private TextView mConnectionStatus;
    private Spinner mSpinnerGrowthTime,mSpinnerPlant;
    public ImageView mPlantImage;
    public TextView mPlantText;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode==RESULT_OK) {
                    //蓝牙设备已经被使能了，then do the job，paired or discovery and then connecting，看sample我们
                    //需要做一个listview来实现这一点。
                    /*
                    mBTConnectionButton.setEnabled(false);
                    //先打开系统自带的蓝牙设置界面来配对和连接蓝牙，有时间再自己写一个DialogFragment的例子
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    //但是打开的这个Activity好像只有显示配对和查找配对设备的功能，没有连接的功能哦。
                    startActivity(settingsIntent);
                    */
                    if(noBluetooth==false) {
                        callBtConnectionDialog();
                    }
                    else {
                        try {
                            throw(new Exception("程序不可能运行到这里"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                else if(resultCode == RESULT_CANCELED) {
                    //蓝牙设备没有被使能
                }
                else {
                    //不可能到这里来
                    Toast.makeText(this,"Error！",Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    void callBtConnectionDialog() {
        BluetoothConnection btDialog = BluetoothConnection.newInstance(mBluetoothAdapter,mConnectedBTDevices,mMainActivityHandler);
        btDialog.show(getFragmentManager(), "蓝牙设置");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        //添加IntentFilter来监听Bluetooth的状态变化
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //注册接受器
        registerReceiver(mBroadcastReceiver,intentFilter);
    }

    @Override
    protected void onDestroy() {
        Log.i("MainActivity","onDestroy");
        super.onDestroy();
        getSensorDataHandler.removeCallbacks(runnable);
        unregisterReceiver(mBroadcastReceiver);
        if(rwReady==true) {
            mConnectedBTDevices.clear();
            mSSPRWThread.cancel();
        }
    }

    /*
        Bluetooth init
         */
    void initBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            //设备不支持蓝牙
            Toast.makeText(this,"您的设备不支持蓝牙",Toast.LENGTH_LONG).show();
            noBluetooth = true;
            return;
        }
        //蓝牙设备是存在的
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT); //对应onActivityResult
        }
        else {
            callBtConnectionDialog();
        }
    }
    /*
    关于白天黑夜 ，春夏秋冬什么的，好像是可以通过手机上的时间或者网络获取的
    需要选择的只是植物的种类
    在传递较多数据时，应该对这些数据做校验以确保传输正确。
     */
    void initViews()
    {
        mBTConnectionButton = (Button) findViewById(R.id.connect_bt_device);
        mConnectionStatus = (TextView) findViewById(R.id.connection_status);
        mReadRemoteDataButton = (TextView) findViewById(R.id.read_remote_data);
        mSetRemoteDataButton = (Button) findViewById(R.id.set_remote_data);
        mParametersSettingButton = (Button) findViewById(R.id.parameters_setting);

        //空气湿度
        mHumidityValue = (TextView) findViewById(R.id.humidity_value);
        //土壤湿度
        mSoilHumidityValue = (TextView) findViewById(R.id.soil_humidity_value);
        //空气温度
        mTemperatureValue = (TextView) findViewById(R.id.temperature_value);
        //光照强度
        mSunlightValue = (TextView) findViewById(R.id.sunlight_value);


        mBTConnectionButton.setOnClickListener(this);
        mReadRemoteDataButton.setOnClickListener(this);
        mSetRemoteDataButton.setOnClickListener(this);
        mParametersSettingButton.setOnClickListener(this);

        //growth time spinner
        mSpinnerGrowthTime = (Spinner) findViewById(R.id.growth_time_spinner);
        ArrayAdapter<String> spinnerAdapterForGrowthTime = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                array_growth_time);
        spinnerAdapterForGrowthTime.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerGrowthTime.setAdapter(spinnerAdapterForGrowthTime);
        mSpinnerGrowthTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                growth_time_index = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSpinnerGrowthTime.setVisibility(View.VISIBLE);

        //plant spinner
        mSpinnerPlant = (Spinner) findViewById(R.id.plant_spinner);
        ArrayAdapter<String> spinnerAdapterForPlant = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                array_plants);
        spinnerAdapterForPlant.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerPlant.setAdapter(spinnerAdapterForPlant);
        mSpinnerPlant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                plants_index = position;
                mPlantImage.setImageResource(images_plants[plants_index]);
                if(plants_index==0) {
                    mPlantText.setText("");
                }
                else {
                    mPlantText.setText(array_plants[plants_index]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSpinnerPlant.setVisibility(View.VISIBLE);

        //获取Plant ImageView and TextView
        mPlantImage = (ImageView) findViewById(R.id.image_plant);
        mPlantText = (TextView) findViewById(R.id.text_name_plant);

        mPlantImage.setImageResource(images_plants[plants_index]);
        mPlantText.setText(array_plants[plants_index]);
        disableViews();
    }

    void enableViews() {
        Log.i("MainActivity:","enableViews");
        mReadRemoteDataButton.setEnabled(true);
        mSetRemoteDataButton.setEnabled(true);
        //mParameterListView.setEnabled(true);
        mConnectionStatus.setText("远程设备"+mBluetoothDevice.getName()+"已经连接！");
        mConnectionStatus.setTextColor(Color.rgb(0,0,0)); //设置为黑色
    }

    void disableViews() {
        //mReadRemoteDataButton.setEnabled(false);
        mSetRemoteDataButton.setEnabled(false);
        //mParameterListView.setEnabled(false);
    }
    //接受传回的结果肯定是异步的哦！
    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.connect_bt_device:
                initBluetooth();
                break;
//            case R.id.send_AT_command:
//                String at_command = mAtCommandText.getText().toString();
//                if(at_command!=null && at_command.length()>0) {
//                    //发送命令
//                    if(rwReady==true) {
//                        mSSPRWThread.write(at_command.getBytes());
//                    }
//                    else {
//                        Toast.makeText(this,"没有蓝牙连接",Toast.LENGTH_LONG).show();
//                    }
//                }
//                break;
            case R.id.read_remote_data:
                //读取远程数据
                //更新list
                //parameterArrayAdapter.notifyDataSetChanged();
                break;
            case R.id.set_remote_data:
                //设置远程数据
                getSensorDataHandler.removeCallbacks(runnable); //停止读取蓝牙sensor data的线程
                mSSPRWThread.write(buildControlCommand().getBytes());
                getSensorDataHandler.postDelayed(runnable,10*1000);//10s。再次启动，延时10s

            case R.id.parameters_setting:
                Log.i("MainActivity","Goto next Activity");
                startActivity(new Intent(MainActivity.this,ParameterActivity.class)); //打开参数设置界面
                break;
            default:
                break;
        }

    }

    String buildControlCommand() {
        if(plants_index==2) {
            strInitialValue = controlData_mianhua[2];
            strTempCoefficient = controlData_mianhua[3];
            strTempOffset = controlData_mianhua[4];
            strHumidityCoefficient = controlData_mianhua[5];
            strSunlightIntensity = controlData_mianhua[6];
        }
        else {
            strInitialValue = controlData[2];
            strTempCoefficient = controlData[3];
            strTempOffset = controlData[4];
            strHumidityCoefficient = controlData[5];
            strSunlightIntensity = controlData[6];
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("w");
        stringBuilder.append(strInitialValue+",");
        stringBuilder.append(strTempCoefficient+",");
        stringBuilder.append(strTempOffset+",");
        stringBuilder.append(strHumidityCoefficient+",");
        stringBuilder.append(strSunlightIntensity+",");
        stringBuilder.append(String.valueOf(plants_index)+",");
        stringBuilder.append(String.valueOf(growth_time_index));
        Log.i("controlCommand",stringBuilder.toString());
        return stringBuilder.toString();
    }

    private class SSPRWThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public SSPRWThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer); //这种问题应该怎么处理呢？
                    Log.i("MainActivitylength",Integer.toString(bytes));
                    buffer[bytes]=0;
                    // Send the obtained bytes to the UI activity
                    Log.i("MainActivity Thread",new String(buffer,0,bytes));
                    mReadSSPHandler.obtainMessage(MESSAGE_READ, bytes, -1, new String(buffer,0,bytes))
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
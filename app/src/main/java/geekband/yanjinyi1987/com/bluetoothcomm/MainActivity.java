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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
            "土豆",
            "棉花",
            "小麦"
    };

    public @IdRes int[] images_plants = {
            R.drawable.tudou,
            R.drawable.mianhua,
            R.drawable.xiaomai
    };

    public int plants_index = 0;

    // 初始值，温度系数，温度offset，湿度系数，光强系数
    public String[] controlData = {"30","0.3","20","0.01","0.05"};
    private ArrayList<String> parameter_name = new ArrayList<>();
    private ArrayList<String> parameter_value = new ArrayList<>();
    private ArrayList<String> parameter_default_value = new ArrayList<>();

    private ArrayList<ParameterData> parameterDatas = new ArrayList<>();

    //与fragmentDialog通信的Handler
    private Handler mMainActivityHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothDevice mBluetoothDevice = null;
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
                    break;
                default:
                    break;
            }
        }
    };

    //与读写线程通信的Handler
    private Handler mReadSSPHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MESSAGE_READ:
                    int length = msg.arg1;
                    byte[] info =(byte[])(msg.obj); //问题在于这里直接引用了buffer，而buffer的数据会被read覆盖，这样造成了问题。
                    StringBuilder stringBuilder = new StringBuilder();
                    for(int i=0;i<length;i++) {
                        Log.i("byte in handler",String.valueOf((char)info[i]));
                        stringBuilder.append((char)info[i]);
                    }
                    String strInfo = stringBuilder.toString();
                    Log.i("MainActivity",strInfo);
                    //mReceivedSPPDataText.setText(mReceivedSPPDataText.getText()+strInfo);
                    //读取远程数据
                    break;
                default:
                    break;
            }
        }
    };

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
    private Button mReadRemoteDataButton;
    private Button mSetRemoteDataButton;
    private ListView mParameterListView;
    private ParameterArrayAdapter parameterArrayAdapter;
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
        mReadRemoteDataButton = (Button) findViewById(R.id.read_remote_data);
        mSetRemoteDataButton = (Button) findViewById(R.id.set_remote_data);

        mBTConnectionButton.setOnClickListener(this);
        mReadRemoteDataButton.setOnClickListener(this);
        mSetRemoteDataButton.setOnClickListener(this);

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
                mPlantText.setText(array_plants[plants_index]);
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
//        mParameterListView = (ListView) findViewById(R.id.parameter_list);
//        //debug 手动设置parameter数据
//        parameterDatas.add(new ParameterData("A:","100","100",1));
//        parameterDatas.add(new ParameterData("B:","100","100",2));
//        parameterDatas.add(new ParameterData("C:","100","100",3));
//        parameterDatas.add(new ParameterData("D:","100","100",4));
//        parameterDatas.add(new ParameterData("A:","100","100",1));
//        parameterDatas.add(new ParameterData("B:","100","100",2));
//        parameterDatas.add(new ParameterData("C:","100","100",3));
//        parameterDatas.add(new ParameterData("D:","100","100",4));
//        parameterDatas.add(new ParameterData("A:","100","100",1));
//        parameterDatas.add(new ParameterData("B:","100","100",2));
//        parameterDatas.add(new ParameterData("C:","100","100",3));
//        parameterDatas.add(new ParameterData("D:","100","100",4));
//        parameterDatas.add(new ParameterData("A:","100","100",1));
//        parameterDatas.add(new ParameterData("B:","100","100",2));
//        parameterDatas.add(new ParameterData("C:","100","100",3));
//        parameterDatas.add(new ParameterData("D:","100","100",4));
//        parameterDatas.add(new ParameterData("A:","100","100",1));
//        parameterDatas.add(new ParameterData("B:","100","100",2));
//        parameterDatas.add(new ParameterData("C:","100","100",3));
//        parameterDatas.add(new ParameterData("D:","100","100",4));
//        parameterDatas.add(new ParameterData("A:","100","100",1));
//        parameterDatas.add(new ParameterData("B:","100","100",2));
//        parameterDatas.add(new ParameterData("C:","100","100",3));
//        parameterDatas.add(new ParameterData("D:","100","100",4));
//        parameterDatas.add(new ParameterData("A:","100","100",1));
//        parameterDatas.add(new ParameterData("B:","100","100",2));
//        parameterDatas.add(new ParameterData("C:","100","100",3));
//        parameterDatas.add(new ParameterData("D:","100","100",4));
//        parameterDatas.add(new ParameterData("A:","100","100",1));
//        parameterDatas.add(new ParameterData("B:","100","100",2));
//        parameterDatas.add(new ParameterData("C:","100","100",3));
//        parameterDatas.add(new ParameterData("D:","100","100",4));
//
//        parameterArrayAdapter = new ParameterArrayAdapter(this,
//                R.layout.parameter_item,
//                parameterDatas);
//        mParameterListView.setAdapter(parameterArrayAdapter);
        //disableViews();
    }

    void enableViews() {
        Log.i("MainActivity:","enableViews");
        mReadRemoteDataButton.setEnabled(true);
        mSetRemoteDataButton.setEnabled(true);
        mParameterListView.setEnabled(true);
        mConnectionStatus.setText("远程设备已经连接！");
        mConnectionStatus.setTextColor(Color.rgb(0,0,0));
    }

    void disableViews() {
        mReadRemoteDataButton.setEnabled(false);
        mSetRemoteDataButton.setEnabled(false);
        mParameterListView.setEnabled(false);
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
                parameterArrayAdapter.notifyDataSetChanged();
                break;
            case R.id.set_remote_data:
                //获取远程数据
                for (int i = 0; i < 10; i++) {
                    Log.i("MainActivity","data "+i+" is "+parameterDatas.get(i).value);
                }

                //设置远程数据
                break;
            default:
                break;
        }

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
            StringBuilder stringBuilder = new StringBuilder();
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer); //这种问题应该怎么处理呢？
                    // Send the obtained bytes to the UI activity
                    mReadSSPHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
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

class ParameterData{
    public String name;
    public String value;
    public String defaultValue;
    public int index;

    public ParameterData(String name, String value, String defaultValue, int index) {
        this.name = name;
        this.value = value;
        this.defaultValue = defaultValue;
        this.index = index;
    }
}
//http://www.webplusandroid.com/creating-listview-with-edittext-and-textwatcher-in-android/
class ParameterArrayAdapter extends ArrayAdapter<ParameterData> {
    int resourceId;
    Context context;
    List<ParameterData> parameterDatas;
    public ParameterArrayAdapter(Context context, int resource, List<ParameterData> objects) {
        super(context, resource, objects);
        resourceId = resource;
        this.context = context;
        parameterDatas = objects;
    }
    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ParameterData parameterData = getItem(position);
        View view;
        final ParentViewHolder parentViewHolder;
        if(convertView==null) {
            view = LayoutInflater.from(context).inflate(resourceId,null);
            parentViewHolder = new ParentViewHolder();
            parentViewHolder.parameter_name = (TextView) view.findViewById(R.id.parameter_text);
            parentViewHolder.parameter_value = (EditText) view.findViewById(R.id.parameter_value);
            parentViewHolder.parameter_default_value = (EditText) view.findViewById(R.id.parameter_default_value);
            parentViewHolder.sequence_number = (TextView)view.findViewById(R.id.sequence_number);
            parentViewHolder.parameter_default_value.setKeyListener(null);
            parentViewHolder.parameter_value.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    //由于View的重用，我们会发现text会发生变化，比如我改了第一个，但是重画时，text的值会被
                    //setText改变，但是此时我们应该改变的是position上的value
                    Log.i("MainActivity","text changed "+position+" "+s.toString());
                    parameterDatas.get(parentViewHolder.ref).value = s.toString();
//                    parameterDatas.get(parentViewHolder.ref).defaultValue = s.toString();
//                    parentViewHolder.parameter_default_value.setText(s);
                }
            });
            view.setTag(parentViewHolder);
        }
        else {
            view = convertView;
            parentViewHolder = (ParentViewHolder) view.getTag();
            Log.i("MainActivity","position is "+position+" "+parentViewHolder.ref);
        }
        parentViewHolder.ref = position;
        parentViewHolder.parameter_name.setText(parameterDatas.get(position).name);
        parentViewHolder.parameter_value.setText(parameterDatas.get(position).value);
        parentViewHolder.parameter_default_value.setText(parameterDatas.get(position).defaultValue);
        parentViewHolder.sequence_number.setText(String.valueOf(position));
        return view;
    }

    class ParentViewHolder {
        TextView parameter_name;
        EditText parameter_value;
        EditText parameter_default_value;
        TextView sequence_number;
        int ref;
    }
}
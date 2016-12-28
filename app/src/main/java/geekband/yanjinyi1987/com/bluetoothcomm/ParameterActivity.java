package geekband.yanjinyi1987.com.bluetoothcomm;

import android.content.Context;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ParameterActivity extends AppCompatActivity implements View.OnClickListener{
    private ListView mParameterListView;
    private ParameterArrayAdapter parameterArrayAdapter;

    private ArrayList<String> parameter_value = new ArrayList<>();
    private ArrayList<String> parameter_default_value = new ArrayList<>();
    private ArrayList<ParameterData> parameterDatas = new ArrayList<>();
    // 初始值，温度系数，温度offset，湿度系数，光强系数
//    String plant_type = controlData[0].substring(1);
//    plants_index = Float.valueOf(plant_type).intValue();
//    mSpinnerPlant.setSelection(plants_index);
//    String growth_time = controlData[1].substring(1);
//    growth_time_index = Float.valueOf(growth_time).intValue();
//    mSpinnerGrowthTime.setSelection(growth_time_index);
//    strInitialValue = controlData[2].substring(1);
//    strTempCoefficient = controlData[3].substring(1);
//    strTempOffset = controlData[4].substring(1);
//    strHumidityCoefficient = controlData[5].substring(1);
//    strSunlightIntensity = controlData[6].substring(1);

    public static final String[] parameter_name = {
            "初始值\t\t\t\t\t",
            "温度系数\t\t",
            "温度offset\t",
            "湿度系数\t\t",
            "光强系数\t\t"
    };
    int parameter_count = 5;

    public static final String[] array_growth_time = {
            "      0月","   0.5月","      1月","   1.5月",
            "      2月","   2.5月","      3月","   3.5月",
            "      4月","   4.5月","      5月","   5.5月",
            "      6月","   6.5月","      7月","   7.5月",
            "      8月","   8.5月","      9月","   9.5月",
            "    10月" ,"10.5月"  ,"    11月" ,"11.5月"
    };

    private ArrayList<String> array_plants = new ArrayList<>();
    private Spinner mSpinnerGrowthTime;
    private Spinner mSpinnerPlant;

    private int growth_time_index=0;
    private int plants_index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("ParameterActivity","OnCreate");
        setContentView(R.layout.activity_parameter);
        initViews();
    }

    void initViews() {
        //Buttons

        //ListView
        mParameterListView = (ListView) findViewById(R.id.parameter_list);
        for (int i = 0; i < parameter_count; i++) {
            parameterDatas.add(new ParameterData(parameter_name[i],"",""));
        }
        parameterArrayAdapter = new ParameterArrayAdapter(this,
                R.layout.parameter_item,
                parameterDatas);
        mParameterListView.setAdapter(parameterArrayAdapter);

        //Spinner
        //growth time spinner
        mSpinnerGrowthTime = (Spinner) findViewById(R.id.growth_time_spinner_for_parameters);
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
        array_plants.add("<新建植物>");
        mSpinnerPlant = (Spinner) findViewById(R.id.plant_spinner_for_parameters);
        ArrayAdapter<String> spinnerAdapterForPlant = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                array_plants);
        spinnerAdapterForPlant.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerPlant.setAdapter(spinnerAdapterForPlant);
        mSpinnerPlant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                plants_index = position;
                //如果选择新建植物，那么弹出对话框输入名称并检测数据库是否重名，如果重名，直接跳转到现有的名字
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSpinnerPlant.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {

    }
}


class ParameterData{
    public String name;
    public String value;
    public String defaultValue;
    //public int index;

    public ParameterData(String name, String value, String defaultValue) {
        this.name = name;
        this.value = value;
        this.defaultValue = defaultValue;
        //this.index = index;
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
            //parentViewHolder.sequence_number = (TextView)view.findViewById(R.id.sequence_number);
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
        //parentViewHolder.sequence_number.setText(String.valueOf(position));
        return view;
    }

    class ParentViewHolder {
        TextView parameter_name;
        EditText parameter_value;
        EditText parameter_default_value;
        //TextView sequence_number;
        int ref;
    }
}

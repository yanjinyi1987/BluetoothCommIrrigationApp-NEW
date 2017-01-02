package geekband.yanjinyi1987.com.bluetoothcomm.fragment;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import geekband.yanjinyi1987.com.bluetoothcomm.Database.DBService;
import geekband.yanjinyi1987.com.bluetoothcomm.ParameterActivity;
import geekband.yanjinyi1987.com.bluetoothcomm.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NewPlantFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NewPlantFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewPlantFragment extends DialogFragment implements View.OnClickListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private Button mExitButton;
    private Button mOKButton;
    private EditText mNewPlantName;

    private static Handler mHandleSpinner;
    private static DBService mDBService;

    public NewPlantFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param handleSpinner Parameter 1.
     * @return A new instance of fragment NewPlantFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NewPlantFragment newInstance(Handler handleSpinner, DBService dbService) {
        NewPlantFragment fragment = new NewPlantFragment();
        mHandleSpinner = handleSpinner;
        mDBService = dbService;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setCancelable(false); //保持在最前
        int style = DialogFragment.STYLE_NORMAL;
        int theme = android.R.style.Theme_DeviceDefault_Light_Dialog;
        setStyle(style,theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getDialog().setTitle("请输入新植物的名称");
        View v = inflater.inflate(R.layout.fragment_new_plant, container, false);

        initViews(v);
        return v;
    }

    void initViews(View v) {
        mExitButton = (Button) v.findViewById(R.id.new_plant_cancle_button);
        mOKButton = (Button) v.findViewById(R.id.new_plant_ok_button);

        mNewPlantName = (EditText) v.findViewById(R.id.new_plant_name_input);

        mExitButton.setOnClickListener(this);
        mOKButton.setOnClickListener(this);



    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.new_plant_cancle_button:
                dismiss();
                break;
            case R.id.new_plant_ok_button:
                String plantName = mNewPlantName.getText().toString();
                if(plantName.isEmpty()) {
                    Toast.makeText(getActivity(),"您没有输入字符",Toast.LENGTH_LONG).show();
                }
                else {
                    //check the String and perform database operation
                    if(mDBService.checkPlantExist(plantName)) {
                        //植物存在
                        Toast.makeText(getActivity(),"植物\""+plantName+"\"已经存在!",Toast.LENGTH_LONG).show();
                    }
                    else {
                        //更新数据库
                        if(mDBService.createNewPlant(plantName)) {
                            //update activity UI
                            Message msg = new Message();
                            msg.what = ParameterActivity.UPDATE_UI;
                            msg.obj = plantName;
                            mHandleSpinner.sendMessage(msg);
                            //成功退出
                            dismiss();
                        }
                        else {
                            Toast.makeText(getActivity(),"创建植物失败，请重新输入",Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(String name);
    }
}

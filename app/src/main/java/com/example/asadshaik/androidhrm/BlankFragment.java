package com.example.asadshaik.androidhrm;



        import android.app.Activity;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.content.Intent;
        import android.os.Bundle;
        import android.os.Handler;
        import android.os.Message;
        import android.support.annotation.Nullable;
        import android.support.v4.app.Fragment;
        import android.support.v4.app.FragmentActivity;
        import android.util.Log;
        import android.view.LayoutInflater;
        import android.view.Menu;
        import android.view.MenuInflater;
        import android.view.MenuItem;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.AdapterView;
        import android.widget.ArrayAdapter;
        import android.widget.Button;
        import android.widget.ListView;
        import android.widget.TextView;
        import android.widget.Toast;
        import android.app.ActionBar;

        import com.github.clans.fab.FloatingActionButton;
        import com.github.clans.fab.FloatingActionMenu;
        import com.google.firebase.auth.FirebaseAuth;
        import com.google.firebase.auth.FirebaseUser;
        import com.google.firebase.database.ChildEventListener;
        import com.google.firebase.database.DataSnapshot;
        import com.google.firebase.database.DatabaseError;
        import com.google.firebase.database.DatabaseReference;
        import com.google.firebase.database.FirebaseDatabase;

        import java.util.ArrayList;
        import java.util.Arrays;


/**
 * A simple {@link Fragment} subclass.
 */

//This Fragment is used for displaying heart readings in
// textual manner by populating the readings in a listview



public class BlankFragment extends Fragment {
    private static final String TAG = "BlankFragment";

    private static final int REQUEST_CONNECT_DEVICE_SECURE=1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE=2;
    private static final int REQUEST_ENABLE_BT=3;

    private ListView in;

    private String mConnectedDeviceName = null;

    private ArrayAdapter<String> readingArrayAdapter;

    private StringBuffer mOutSringBuffer;

    private BluetoothAdapter btAdapter = null;

    private DataExchange dataExchange = null;

    private String userId;

    //Floating Action buttons
    FloatingActionMenu materialDesignFAM;
    FloatingActionButton floatingActionButton1, floatingActionButton2, floatingActionButton3;

    //private ArrayAdapter<HeartReadings> readingArrayAdapter;
    public ArrayList<HeartReadings> items=new ArrayList<>();

    public ArrayList<HeartReadings> heartReadings=new ArrayList<>();
    Button send_button;
    TextView textReading;
    String realMessage = new String();
    String readings;
    DatabaseReference db;

    //Firebase
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mfirebaseUser;
    private FirebaseAuth.AuthStateListener mAuthStateListener;


    public BlankFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(btAdapter == null){
            FragmentActivity activity = getActivity();
            Toast.makeText(activity,"No Bluetooth Available",Toast.LENGTH_LONG).show();
            activity.finish();
        }



    }

    @Override
    public void onStart() {
        super.onStart();

        if (!btAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_ENABLE_BT);
        }
        else if (dataExchange == null){
            setupchat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dataExchange!=null){
            dataExchange.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();


        if (dataExchange!=null){
            if (dataExchange.getState()==DataExchange.STATE_NONE){
                dataExchange.start();
            }
        }
    }

    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what){
                case DataConstants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1){
                        case DataExchange.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to,mConnectedDeviceName));
                            readingArrayAdapter.clear();
                            break;
                        case DataExchange.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case DataExchange.STATE_LISTEN:
                        case DataExchange.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case DataConstants.MESSAGE_READ:
                    byte[] readbuf = (byte[]) msg.obj;
                    final String readmessage = new String(readbuf,0,msg.arg1);

                    String result[] = {"70","71","72","73","74","75","76","77","78","79","80","81","82","83","84","85","86","87","88","89","90","91","93","94","95","96","97","98","99","100","101","102","103","104","105","106","107","108","109","110"};


                    for (int i = 0;i<result.length;i++){
                       if (result[i].equals(readmessage)){
                           //readingArrayAdapter.add(" "+readmessage);
                           textReading.setText(readmessage);
                           realMessage=readmessage;
                       }

                    //realMessage = readmessage;
                    }
                    //}

                    //make next two comments as statements if the above doesn't work
                    //if(Arrays.asList(codes).contains(userCode))
                    //readingArrayAdapter.add(mConnectedDeviceName+" "+readmessage);

                    //readingArrayAdapter.add(" "+realMessage);


                    floatingActionButton1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            HeartReadings heartReadings = new HeartReadings();
                            heartReadings.setUserid(userId);
                            heartReadings.setReading(realMessage+" bpm");
                            save(heartReadings);

                        }
                    });
                    /*send_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            HeartReadings heartReadings = new HeartReadings();
                            heartReadings.setReading(realMessage+"bpm");
                            save(heartReadings);
                        }
                    });*/

                    break;

                case DataConstants.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(DataConstants.device_name);
                    if (null!=activity){
                        Toast.makeText(activity,"Connected to" + mConnectedDeviceName,Toast.LENGTH_LONG).show();
                    }
                    break;
                case DataConstants.MESSAGE_TOAST:
                    if (null!=activity){
                        Toast.makeText(activity,msg.getData().getString(DataConstants.toast),Toast.LENGTH_LONG).show();
                    }
                    break;


            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_blank, container, false);

        //Firebase
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mfirebaseUser = mFirebaseAuth.getCurrentUser();

        //mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("readings");


        readings=realMessage;

        db=FirebaseDatabase.getInstance().getReference();

        textReading = (TextView) view.findViewById(R.id.textReading);
        /*retrieve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retrieve();
            }
        });*/


        userId = mFirebaseAuth.getCurrentUser().getUid();




        materialDesignFAM = (FloatingActionMenu) view.findViewById(R.id.material_design_android_floating_action_menu);
        floatingActionButton1 = (FloatingActionButton) view.findViewById(R.id.material_design_floating_action_menu_item1);
        floatingActionButton2 = (FloatingActionButton) view.findViewById(R.id.material_design_floating_action_menu_item2);

        floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(),Records.class);
                startActivity(intent);
            }
        });

        return view;
    }

    public boolean save(HeartReadings heartReadings){
        db.child("HeartReadings").push().setValue(heartReadings);
        return true;
    }

    public void fetchData(DataSnapshot dataSnapshot){
        heartReadings.clear();

        for(DataSnapshot ds:dataSnapshot.getChildren()){
            HeartReadings readings=ds.getValue(HeartReadings.class);
            heartReadings.add(readings);

        }

    }

    public ArrayList<HeartReadings> retrieve() {

        db.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                fetchData(dataSnapshot);
                readingArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                fetchData(dataSnapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                fetchData(dataSnapshot);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                fetchData(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return heartReadings;
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        in = (ListView) view.findViewById(R.id.in);


    }

    private void setupchat(){
        Log.d(TAG,"setupchat()");

        readingArrayAdapter = new ArrayAdapter<String>(getActivity(),R.layout.message);
        in.setAdapter(readingArrayAdapter);

        dataExchange = new DataExchange(getActivity(),mHandler);

        //mOutSringBuffer = new StringBuffer(" ");
    }


    private ChildEventListener mChildEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Toast.makeText(getActivity(),"readings stored!",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Toast.makeText(getActivity(),"Error Occured",Toast.LENGTH_LONG).show();

        }
    };




    private void ensureDiscoverable(){
        if (btAdapter.getScanMode()!= BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(intent);
        }
    }

    private void setStatus(int resId){
        FragmentActivity activity = getActivity();
        if(null == activity){
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar){
            return;
        }
        actionBar.setSubtitle(resId);
    }

    private void setStatus(CharSequence subtitle){
        FragmentActivity activity = getActivity();
        if (null == activity){
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar){
            return;
        }
        actionBar.setSubtitle(subtitle);

    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK){
                    connectDevice(data,true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK){
                    connectDevice(data,false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK){
                    setupchat();
                }else {
                    Log.d(TAG,"BT was not enabled");

                    Toast.makeText(getActivity(),"BT was not enabled",Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }

        }
    }


    private void connectDevice(Intent data,boolean secure){
        String address = data.getExtras().getString(DeviceList.EXTRA_DEVICE_ADDRESS);

        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        dataExchange.connect(device,secure);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_connect,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.device_secure:
                Intent intent = new Intent(getActivity(),DeviceList.class);
                startActivityForResult(intent,REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.device_insecure:
                Intent i = new Intent(getActivity(),DeviceList.class);
                startActivityForResult(i,REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            case R.id.make_discoverable:
                ensureDiscoverable();
                return true;


        }
        return false;
    }
}
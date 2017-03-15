package com.example.asadshaik.androidhrm;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class Readings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readings);


        if (savedInstanceState == null){
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            BlankFragment fragment = new BlankFragment();
            transaction.replace(R.id.sample_content_fragment,fragment);
            transaction.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.options,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.precautions:
                Toast.makeText(getApplicationContext(), "Precautions", Toast.LENGTH_LONG).show();
                return true;

            case R.id.symptoms:
                Toast.makeText(getApplicationContext(), "Symptoms", Toast.LENGTH_LONG).show();
                return true;

            case R.id.about:
                Toast.makeText(getApplicationContext(), "About", Toast.LENGTH_LONG).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }
}

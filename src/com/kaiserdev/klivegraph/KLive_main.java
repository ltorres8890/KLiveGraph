package com.kaiserdev.klivegraph;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class KLive_main extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_klive_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_klive_main, menu);
        return true;
    }
}

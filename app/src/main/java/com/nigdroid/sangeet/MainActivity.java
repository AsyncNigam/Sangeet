package com.nigdroid.sangeet;


import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        String []arr={
                "Samjho na Samajh__",
                "Chahun  Main Ya Naa__ ",
                "Guli mata___  ",
                "Sun Saathiya___ ",
                "Samayama Hi_Nanna__",
                "Hasi Ban Gaye___",
                "illahi_Mera Ji aye___",
                "Jhol___",
                "Jhoome Jo Pathaan___ ",
                "Dhadak Dhadak___",
                "kaun tujhe___",
                "Lo maan liya hum ne___",
                "Kasturi___",
                "Na_Roja_Nuvve__",
                "Pal___",
                "Paro__",
                "Sanam_Teri_Kasam",
                "Tofa_Chandini_re___",
                "Chiring_Chiring___",
                "Megha_ru_tu_jharilu_Na..",
                "Lal_Taha_Taha__",
                "Akhiyaan_Gulaab__",
                "dulhan banami___",
                "Nigam_tune"


        };

//        toolbar=findViewById(R.id.toolbar);
        listView=findViewById(R.id.listView);

        Nigam ad=new Nigam(this,R.layout.my_nigam,arr);

        listView.setAdapter(ad);

    }


}
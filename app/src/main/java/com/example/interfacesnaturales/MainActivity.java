package com.example.interfacesnaturales;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.interfacesnaturales.comprarvoz.MainComprarVoz;
import com.example.interfacesnaturales.llamarmalla.MainMallaFacial;
import com.example.interfacesnaturales.lenguajesenas.MainSenas;
import com.example.interfacesnaturales.leertexto.MainLeerTexto;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Encontrar los botones por su ID
        Button btnComprarVoz = findViewById(R.id.btnComprarVoz);
        Button btnLenguajeSenas = findViewById(R.id.btnLenguajeSenas);
        Button btnLeerTexto = findViewById(R.id.btnLeerTexto);
        Button btnReconocimientoFacial = findViewById(R.id.btnReconocimientoFacial);

        // Asignar el listener de clics a cada botón
        btnComprarVoz.setOnClickListener(this);
        btnLenguajeSenas.setOnClickListener(this);
        btnLeerTexto.setOnClickListener(this);
        btnReconocimientoFacial.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        int id = v.getId();
        if (id == R.id.btnComprarVoz) {
            intent = new Intent(this, MainComprarVoz.class);
            startActivity(intent);
        } else if (id == R.id.btnLenguajeSenas) {
            intent = new Intent(this, MainSenas.class);
            startActivity(intent);
        } else if (id == R.id.btnLeerTexto) {
            intent = new Intent(this, MainLeerTexto.class);
            startActivity(intent);
        } else if (id == R.id.btnReconocimientoFacial) {
            intent = new Intent(this, MainMallaFacial.class);
            startActivity(intent);
        }
    }
}

package com.convertor.pdf;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.convertor.pdf.ui.ImageEditActivity;
import com.convertor.pdf.ui.OfficeConvertActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UpdateChecker.check(this);

        CardView cardImages = findViewById(R.id.card_images);
        CardView cardWord = findViewById(R.id.card_word);
        CardView cardExcel = findViewById(R.id.card_excel);
        CardView cardPpt = findViewById(R.id.card_ppt);

        View.OnClickListener listener = v -> {
            Intent intent = new Intent(MainActivity.this, ImageEditActivity.class);
            if (v == cardWord) intent = new Intent(MainActivity.this, OfficeConvertActivity.class)
                .putExtra("type", "word");
            else if (v == cardExcel) intent = new Intent(MainActivity.this, OfficeConvertActivity.class)
                .putExtra("type", "excel");
            else if (v == cardPpt) intent = new Intent(MainActivity.this, OfficeConvertActivity.class)
                .putExtra("type", "ppt");
            startActivity(intent);
        };

        cardImages.setOnClickListener(listener);
        cardWord.setOnClickListener(listener);
        cardExcel.setOnClickListener(listener);
        cardPpt.setOnClickListener(listener);
    }
}

package com.example.marianapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.opencv.android.OpenCVLoader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button EstimateChlorophyll;
    BottomNavigationView bottomNavigationView;
    Bitmap capturedImageBitmap;


    @SuppressLint({"WrongViewCast", "NonConstantResourceId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        EstimateChlorophyll = findViewById(R.id.EstimateChlorophyll);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_item_camera:
                    openCamera();
                    return true;
                case R.id.nav_item_library:
                    openGallery();
                    return true;
            }
            return false;
        });

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed!");
        } else {
            Log.d("OpenCV", "OpenCV initialization succeeded!");
        }

        EstimateChlorophyll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (capturedImageBitmap != null) {
                    calculateChlorophyllContent(capturedImageBitmap);
                } else {
                    Toast.makeText(MainActivity.this, "No image captured or selected", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void calculateChlorophyllContent(Bitmap image) {
        HorizontalBarChart redChart = findViewById(R.id.redDistributionChart);
        HorizontalBarChart greenChart = findViewById(R.id.greenDistributionChart);
        HorizontalBarChart blueChart = findViewById(R.id.blueDistributionChart);

        int width = image.getWidth();
        int height = image.getHeight();

        // Initialize arrays for color distribution
        int[] redDistribution = new int[256];
        int[] greenDistribution = new int[256];
        int[] blueDistribution = new int[256];

        // Loop through each pixel to calculate color distribution
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getPixel(x, y);
                int redValue = Color.red(pixel);
                int greenValue = Color.green(pixel);
                int blueValue = Color.blue(pixel);

                redDistribution[redValue]++;
                greenDistribution[greenValue]++;
                blueDistribution[blueValue]++;
            }
        }

        // Create a BarEntry list for the red, green, and blue color distributions
        List<BarEntry> redEntries = new ArrayList<>();
        List<BarEntry> greenEntries = new ArrayList<>();
        List<BarEntry> blueEntries = new ArrayList<>();

        for (int i = 0; i < 256; i++) {
            redEntries.add(new BarEntry(i, redDistribution[i]));
            greenEntries.add(new BarEntry(i, greenDistribution[i]));
            blueEntries.add(new BarEntry(i, blueDistribution[i]));
        }

        // Create BarDataSet objects for the color distributions
        BarDataSet redDataSet = new BarDataSet(redEntries, "Red");
        redDataSet.setColor(Color.RED);

        BarDataSet greenDataSet = new BarDataSet(greenEntries, "Green");
        greenDataSet.setColor(Color.GREEN);

        BarDataSet blueDataSet = new BarDataSet(blueEntries, "Blue");
        blueDataSet.setColor(Color.BLUE);

        // Create BarData objects from the datasets
        BarData barData = new BarData(redDataSet, greenDataSet, blueDataSet);

        redChart.setData(barData);
        customizeChart(redChart);

        greenChart.setData(barData);
        customizeChart(greenChart);

        blueChart.setData(barData);
        customizeChart(blueChart);
    }

    // A separate method to customize the appearance of the charts
    private void customizeChart(HorizontalBarChart chart) {
        Description description = new Description();
        description.setEnabled(false);
        chart.setDescription(description);
        chart.getXAxis().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.invalidate();
    }



    private void openCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openGallery() {
        try {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // Get the captured image data from the extras or directly from the data intent
            Bundle extras = data.getExtras();
            capturedImageBitmap = (Bitmap) extras.get("data");

            // If capturedImageBitmap is still null, handle the error
            if (capturedImageBitmap == null) {
                Toast.makeText(this, "Failed to get image data", Toast.LENGTH_SHORT).show();
                return;
            }
            // Save the captured image to the gallery
            saveImageToGallery(capturedImageBitmap);

        } else if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            // Get the captured image data from the extras or directly from the data intent
            Bitmap galleryImage = null;

            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                galleryImage = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // If galleryImage is still null, handle the error
            if (galleryImage == null) {
                Toast.makeText(this, "Failed to get image data", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save the selected image to the gallery
            saveImageToGallery(galleryImage);
        }
    }

    private void saveImageToGallery(Bitmap bitmap) {
        // Save the image to the gallery
        String fileName = "CapturedImage_" + System.currentTimeMillis() + ".jpg";
        String fileDescription = "Captured Image";
        String savedImagePath = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                fileName,
                fileDescription
        );
        // If the image is saved successfully, show a toast message
        if (savedImagePath != null) {
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save image to gallery", Toast.LENGTH_SHORT).show();
        }
    }
}

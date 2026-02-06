package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase FIRST
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("Cities");

        // Initialize views and data structures SECOND
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // Create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // Set up snapshot listener THIRD (after ArrayList is initialized)
        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Snapshot listener error: " + error.toString());
                return;
            }

            if (value != null) {
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");

                    Log.d("Firestore", "Loaded city: " + name + ", " + province);
                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
                Log.d("Firestore", "Total cities loaded: " + cityArrayList.size());
            }
        });

        // Set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        Log.d("Firestore", "Updating city: " + city.getName() + " -> " + title);

        String oldName = city.getName();
        city.setName(title);
        city.setProvince(year);

        if (!oldName.equals(title)) {
            // Delete old document
            citiesRef.document(oldName).delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "Old city deleted: " + oldName);
                    });
            // Create new document
            citiesRef.document(title).set(city)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "New city created: " + title);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error creating new city", e);
                    });
        } else {
            // Just update the existing document
            citiesRef.document(oldName).set(city)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "City updated: " + oldName);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error updating city", e);
                    });
        }
    }

    @Override
    public void addCity(City city) {
        Log.d("Firestore", "Adding city: " + city.getName() + ", " + city.getProvince());

        // Don't add to ArrayList manually - let the snapshot listener handle it
        citiesRef.document(city.getName()).set(city)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "City successfully added to Firestore: " + city.getName());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error adding city to Firestore", e);
                });
    }

    @Override
    public void deleteCity(City city) {
        Log.d("Firestore", "Deleting city: " + city.getName());

        // Don't remove from ArrayList manually - let the snapshot listener handle it
        citiesRef.document(city.getName()).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "City successfully deleted from Firestore: " + city.getName());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error deleting city from Firestore", e);
                });
    }
}
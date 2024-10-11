package com.axldev.conversordivisas;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private EditText editTextValor;
    private Spinner spinnerOrigen, spinnerDestino;
    private TextView textViewResultado, textViewMonedaOrigen, textViewMonedaDestino;
    private Button buttonConvertir;

    private HashMap<String, Float> exchangeRates;
    private String[] currencies = {"EUR", "USD", "JPY", "BGN", "CZK", "DKK", "GBP", "HUF", "PLN", "RON", "SEK", "CHF", "ISK", "NOK", "HRK", "RUB", "TRY", "AUD", "BRL", "CAD", "CNY", "HKD", "IDR", "ILS", "INR", "KRW", "MXN", "MYR", "NZD", "PHP", "SGD", "THB", "ZAR"};
    private String[] currencyNames = {"Euro", "Dólar Estadounidense", "Yen Japonés", "Lev Búlgaro", "Corona Checa", "Corona Danesa", "Libra Esterlina", "Florín Húngaro", "Złoty Polaco", "Leu Rumano", "Corona Sueca", "Franco Suizo", "Corona Islandesa", "Corona Noruega", "Kuna Croata", "Rublo Ruso", "Lira Turca", "Dólar Australiano", "Real Brasileño", "Dólar Canadiense", "Yuan Chino", "Dólar de Hong Kong", "Rupia Indonesia", "Shekel Israelí", "Rupia India", "Won Surcoreano", "Peso Mexicano", "Ringgit Malasio", "Dólar Neozelandés", "Peso Filipino", "Dólar Singapurense", "Baht Tailandés", "Rand Sudafricano"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextValor = findViewById(R.id.editTextValor);
        spinnerOrigen = findViewById(R.id.spinnerOrigen);
        spinnerDestino = findViewById(R.id.spinnerDestino);
        textViewResultado = findViewById(R.id.textViewResultado);
        textViewMonedaOrigen = findViewById(R.id.textViewMonedaOrigen);
        textViewMonedaDestino = findViewById(R.id.textViewMonedaDestino);
        buttonConvertir = findViewById(R.id.btnConvertir);

        // Fill spinners with currency codes
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrigen.setAdapter(adapter);
        spinnerDestino.setAdapter(adapter);

        // Set listeners for spinners
        spinnerOrigen.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                textViewMonedaOrigen.setText(currencyNames[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerDestino.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                textViewMonedaDestino.setText(currencyNames[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Load exchange rates
        loadExchangeRates();

        buttonConvertir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertir();
            }
        });
    }

    private void convertir() {
        String valorTexto = editTextValor.getText().toString();
        if (valorTexto.isEmpty()) {
            textViewResultado.setText("Introduce un valor a convertir.");
            return;
        }

        float valor = Float.parseFloat(valorTexto);
        String origen = spinnerOrigen.getSelectedItem().toString();
        String destino = spinnerDestino.getSelectedItem().toString();

        if (!exchangeRates.containsKey(origen) || !exchangeRates.containsKey(destino)) {
            textViewResultado.setText("Error: No se encontraron tasas de cambio.");
            return;
        }

        // Convertir de MXN a la moneda origen y luego a destino
        float tasaOrigen = exchangeRates.get(origen);
        float tasaDestino = exchangeRates.get(destino);

        // Convertir a destino directamente
        float resultado = valor * tasaDestino / tasaOrigen;

        textViewResultado.setText(String.format("%.2f %s", resultado, destino));
    }

    private void loadExchangeRates() {
        exchangeRates = new HashMap<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://www.floatrates.com/daily/mxn.xml");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    InputStream inputStream = connection.getInputStream();
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(inputStream, null);

                    int eventType = parser.getEventType();
                    String currentCurrency = null;
                    float currentRate = 0;

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            String tagName = parser.getName();
                            if (tagName.equals("item")) {
                                currentCurrency = null;
                                currentRate = 0;
                            } else if (tagName.equals("targetCurrency")) {
                                currentCurrency = parser.nextText();
                            } else if (tagName.equals("exchangeRate")) {
                                currentRate = Float.parseFloat(parser.nextText());
                            }
                        } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals("item")) {
                            if (currentCurrency != null) {
                                exchangeRates.put(currentCurrency.toUpperCase(), currentRate);
                            }
                        }
                        eventType = parser.next();
                    }


                    exchangeRates.put("MXN", 1.0f);
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

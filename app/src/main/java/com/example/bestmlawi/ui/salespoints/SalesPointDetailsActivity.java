package com.example.bestmlawi.ui.salespoints;

import android.os.Bundle;
import android.widget.TextView;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.FragmentActivity;

import com.example.bestmlawi.R;


public class SalesPointDetailsActivity extends FragmentActivity {

    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_ADDRESS = "extra_address";
    public static final String EXTRA_CITY = "extra_city";
    public static final String EXTRA_PHONE = "extra_phone";
    public static final String EXTRA_OPENING = "extra_opening";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";

    private WebView webSpDetailMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_point_details);

        TextView txtName = findViewById(R.id.txtSpDetailName);
        TextView txtAddress = findViewById(R.id.txtSpDetailAddress);
        TextView txtCity = findViewById(R.id.txtSpDetailCity);
        TextView txtPhone = findViewById(R.id.txtSpDetailPhone);
        TextView txtOpening = findViewById(R.id.txtSpDetailOpening);

        webSpDetailMap = findViewById(R.id.webSpDetailMap);
        if (webSpDetailMap != null) {
            WebSettings settings = webSpDetailMap.getSettings();
            settings.setJavaScriptEnabled(true);
            webSpDetailMap.setWebViewClient(new WebViewClient());
            webSpDetailMap.loadUrl("file:///android_asset/salespoint_detail.html");
        }

        String name = getIntent().getStringExtra(EXTRA_NAME);
        String address = getIntent().getStringExtra(EXTRA_ADDRESS);
        String city = getIntent().getStringExtra(EXTRA_CITY);
        String phone = getIntent().getStringExtra(EXTRA_PHONE);
        String opening = getIntent().getStringExtra(EXTRA_OPENING);
        double lat = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0.0);
        double lng = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0.0);

        txtName.setText(name != null ? name : "");
        txtAddress.setText(address != null ? address : "");
        txtCity.setText(city != null ? city : "");
        txtPhone.setText(phone != null ? phone : "");
        txtOpening.setText(opening != null ? opening : "");

        if (lat != 0.0 || lng != 0.0 && webSpDetailMap != null) {
            String safeName = name != null ? name.replace("'", "\\'") : "";
            String js = "javascript:showSinglePoint(" + lat + "," + lng + ", '" + safeName + "')";
            webSpDetailMap.post(() -> webSpDetailMap.evaluateJavascript(js, null));
        }
    }
}

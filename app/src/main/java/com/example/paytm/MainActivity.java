package com.example.paytm;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;
import com.paytm.pgsdk.TransactionManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private String  TAG ="MainActivity";
    private ProgressBar progressBar;
    private String orderIdString="", customerIdString="",txnTokenString="",midString="wRkmOe59237351879270",txnAmountString="10.00";
    //FOR TESTING
    private String HOST_NAME = "https://securegw-stage.paytm.in";
    //FOR PROD
    //private String HOST_NAME = "https://securegw.paytm.in";
    private int ActivityRequestCode = 05;
    private Button btnPayNow;
    private Text result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnPayNow = findViewById(R.id.btn);
        orderIdString = idsGenerator();
        customerIdString= idsGenerator();
        Log.d("orderIdString",orderIdString);
        Log.d("customerIdString",customerIdString);
        btnPayNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getToken();
            }
        });

    }
    private void getToken(){
        Log.d("tokenMethod","token is started");
        progressBar.setVisibility(View.VISIBLE);
        RequestQueue queue = Volley.newRequestQueue(this);

//      For online use
     String url = "https://us-central1-paytm-9423c.cloudfunctions.net/getCheckSum?oId="+orderIdString+"&"+"custId="+customerIdString+"&"+"amount="+txnAmountString;

//      For Localhost
     //   String url = "http://10.0.2.2:5001/paytm-9423c/us-central1/getCheckSum?oId="+orderIdString+"&"+"custId="+customerIdString+"&"+"amount="+txnAmountString;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        try {
                            String token =  response.getJSONObject("body").getString("txnToken");
                            startPaytmPayment(token);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("Error","Token error");
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        Log.d("Error", "error : " + error.toString());
                    }});
        queue.add(jsonObjectRequest);
    }

    public void startPaytmPayment(String token){
        txnTokenString = token;
        String callBackUrl = "https://securegw-stage.paytm.in/theia/paytmCallback?ORDER_ID="+orderIdString;
        PaytmOrder paytmOrder = new PaytmOrder(orderIdString, midString, txnTokenString, txnAmountString, callBackUrl);
        TransactionManager transactionManager = new TransactionManager(paytmOrder, new PaytmPaymentTransactionCallback(){
            @Override
            public void onTransactionResponse(Bundle bundle) {
                Toast.makeText(getApplicationContext(), "Payment Transaction response " + bundle.toString(), Toast.LENGTH_LONG).show();
            }
            @Override
            public void networkNotAvailable() {
                Log.e(TAG, "network not available ");
            }
            @Override
            public void onErrorProceed(String s) {
                Log.e(TAG, " onErrorProcess "+s.toString());
            }
            @Override
            public void clientAuthenticationFailed(String s) {
                Log.e(TAG, "Clientauth "+s);
            }
            @Override
            public void someUIErrorOccurred(String s) {
                Log.e(TAG, " UI error "+s);
            }
            @Override
            public void onErrorLoadingWebPage(int i, String s, String s1) {
                Log.e(TAG, " error loading web "+s+"--"+s1);
            }
            @Override
            public void onBackPressedCancelTransaction() {
                Log.e(TAG, "backPress ");
            }
            @Override
            public void onTransactionCancel(String s, Bundle bundle) {
                Log.e(TAG, " transaction cancel "+s);
            }
        });
        transactionManager.setShowPaymentUrl(HOST_NAME + "/theia/api/v1/showPaymentPage");
        transactionManager.startTransaction(this, ActivityRequestCode);
    }

    // starting paytm app
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
/*
            Use the result code for further operation
*/

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ActivityRequestCode && data != null) {
            Log.e(TAG, " data "+  data.getStringExtra("nativeSdkForMerchantMessage"));
            Log.e(TAG, " data response - "+data.getStringExtra("response"));
            Toast.makeText(this, data.getStringExtra("nativeSdkForMerchantMessage")
                    + data.getStringExtra("response"), Toast.LENGTH_SHORT).show();
            // globally


        }
    }

    // for generating customer id and oder id
    private String  idsGenerator(){
        Calendar cal  = Calendar.getInstance();
        SimpleDateFormat df =  new SimpleDateFormat("ddMMyyyy");
        String date =  df.format(cal.getTime());
        Random  random = new Random();
        int min = 1000, max = 9999;

        int randomNum = random.nextInt((max-min)+1)+min;
        String idString = date+String.valueOf(randomNum);
        return idString;
    }
}
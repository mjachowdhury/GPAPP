package com.mohammed.transport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;

public class PaymentActivity extends AppCompatActivity {

    final int DROP_IN_REQUEST = 145;
    final String mClientToken = "sandbox_vf4f346q_n8w5dnnq4ds5kwgr";
    final String send_payment_details = "AbsHQFDncI_cAdy1dTtBXSB2znBJi3Y-BxXoLhDrtuQiiKju_6j_Lk4W6wHjSZ1syn1wy8Rwsh8w_IkZ";
    private BraintreeFragment mBraintreeFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        DropInRequest dropInRequest = new DropInRequest()
                .clientToken(mClientToken);
        startActivityForResult(dropInRequest.getIntent(getApplicationContext()), DROP_IN_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DROP_IN_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                result.describeContents();
                result.getPaymentMethodType();
                result.getPaymentMethodType();
                String paymentMethodNonce = result.getPaymentMethodNonce().getNonce();
                Log.d("Payment",paymentMethodNonce);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(),"cancelled",Toast.LENGTH_SHORT).show();
            } else {
                // an error occurred, checked the returned exception
                Exception exception = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);

                Toast.makeText(getApplicationContext(),"error",Toast.LENGTH_SHORT).show();
            }
        }
    }


}
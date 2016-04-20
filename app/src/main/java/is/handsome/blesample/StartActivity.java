package is.handsome.blesample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Button centralClientButton = (Button) findViewById(R.id.start_central_client_button);
        assert centralClientButton != null;
        centralClientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StartActivity.this, CentralClientActivity.class));
            }
        });

        Button peripheralServerButton = (Button) findViewById(R.id.start_peripheral_server_button);
        assert peripheralServerButton != null;
        peripheralServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StartActivity.this, PeripheralServerActivity.class));
            }
        });
    }
}

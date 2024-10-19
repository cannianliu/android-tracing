package com.demo.tracing;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button enter = findViewById(R.id.enter_button);
        enter.setOnClickListener(view -> new OtelButton().clickEvent());

        Button grpc = findViewById(R.id.button_grpc);
        grpc.setOnClickListener(view -> new GrpcClient("localhost:50051").greet("world"));

        new Thread(() -> {
            GrpcServer grpcServer = new GrpcServer();
            grpcServer.start(50051);
            try {
                grpcServer.blockUntilShutdown();
            } catch (InterruptedException ignored) {
            }
        }).start();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
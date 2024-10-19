package com.demo.tracing;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;

public class GrpcClient {

    private static final String TAG = "GRPC-demo";
    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    public GrpcClient(String endpoint) {
        channel = Grpc.newChannelBuilder(endpoint, InsecureChannelCredentials.create())
                .intercept(OtelApplication.grpcTelemetry().newClientInterceptor())
                .build();
        this.blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    @WithSpan(value = "client-greet", kind = SpanKind.CLIENT)
    public void greet(String name) {
        Tracer tracer = OtelApplication.tracer("grpc-client");
        if (tracer == null) {
            Log.e(TAG, "RUM not init!!!");
            return;
        }
        Span span = tracer.spanBuilder("client/outer")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        Log.i(TAG, "try to greet: " + name);
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply reply;
        Span spanInner = tracer.spanBuilder("client/inner")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        reply = blockingStub.sayHello(request);
        spanInner.end();
        Log.i(TAG, "Greet receive: " + reply.getMessage());
        span.end();
        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        Log.i(TAG, "grpc client shutdown");
    }
}

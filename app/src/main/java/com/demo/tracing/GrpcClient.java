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
import io.opentelemetry.context.Scope;

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

    public void greet(String name) {
        Tracer tracer = OtelApplication.tracer("client-greet");
        if (tracer == null) {
            Log.e(TAG, "RUM not init!!!");
            return;
        }
        Span span = tracer.spanBuilder("client/outer")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            Log.i(TAG, "try to greet: " + name);
            HelloRequest request = HelloRequest.newBuilder().setName(name).build();
            HelloReply reply;
            Span spanInner = tracer.spanBuilder("client/inner")
                    .startSpan();
            try (Scope inner = spanInner.makeCurrent()) {
                reply = blockingStub.sayHello(request);
                spanInner.end();
                Log.i(TAG, "Greet receive: " + reply.getMessage());
            } finally {
                spanInner.end();
            }
        } finally {
            try {
                channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            Log.i(TAG, "grpc client shutdown");
            span.end();
        }
    }
}

package com.demo.tracing;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;

public class GrpcServer {

    private static final String TAG = "GRPC-demo";
    private Server server;

    public void start(int port) {
        try {
            server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                    .addService(configureServerInterceptor(new GreetImpl()))
                    .build()
                    .start();
        } catch (IOException e) {
            Log.e(TAG, "grpc server start error!!" + e.getMessage());
        }

        Log.i(TAG, "grpc server listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    GrpcServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private ServerServiceDefinition configureServerInterceptor(BindableService bindableService) {
        return ServerInterceptors.intercept(bindableService, OtelApplication.grpcTelemetry().newServerInterceptor());
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    static class GreetImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        @WithSpan(value = "server-hello", kind = SpanKind.SERVER)
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}

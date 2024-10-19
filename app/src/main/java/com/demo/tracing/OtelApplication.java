package com.demo.tracing;

import android.app.Application;
import android.util.Log;

import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.android.OpenTelemetryRumBuilder;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.events.EventBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider;

public class OtelApplication extends Application {

    private static final String TAG = "otel.demo";
    public static OpenTelemetryRum rum;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Initializing the opentelemetry-android-agent");

        OtelRumConfig otelRumConfig = new OtelRumConfig()
                .setGlobalAttributes(Attributes.of(AttributeKey.stringKey("ServerName"), "OTelDemo"));

        OtlpHttpSpanExporter httpSpanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint("http://10.0.2.2:4318/v1/traces")
                .build();

        OtlpGrpcSpanExporter grpcSpanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://10.0.2.2:4317")
                .build();

        OpenTelemetryRumBuilder rumBuilder = OpenTelemetryRum.builder(this, otelRumConfig)
                .addSpanExporterCustomizer(x -> grpcSpanExporter)
                .addPropagatorCustomizer(p ->
                        TextMapPropagator.composite(
                                W3CTraceContextPropagator.getInstance(),
                                W3CBaggagePropagator.getInstance()));

        rum = rumBuilder.build();

        Log.i(TAG, "RUM session started: " + rum.getRumSessionId());
    }

    public static Tracer tracer(String name) {
        if (rum == null) {
            Log.e(TAG, "RUM not init yet");
            return null;
        }
        return rum.getOpenTelemetry().getTracerProvider().get(name);
    }

    public static EventBuilder eventBuilder(String scopeName, String eventName) {
        return SdkEventLoggerProvider.create(rum.getOpenTelemetry().getLogsBridge())
                .get(scopeName)
                .builder(eventName);
    }

    public static GrpcTelemetry grpcTelemetry() {
        while(rum == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        return GrpcTelemetry.create(rum.getOpenTelemetry());
    }
}

package com.demo.tracing;

import android.util.Log;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;

public class OtelButton {

    private static final String TAG = "otel.demo";

    public void clickEvent() {
        OtelApplication.eventBuilder("otel.demo", "enter click").emit();
        Tracer tracer = OtelApplication.tracer("otel demo");
        if (tracer == null) {
            Log.e(TAG, "RUM not init!!!");
            return;
        }
        Span span = tracer.spanBuilder("enter click")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        Log.i(TAG, "enter is clicked");

        span.end();
    }
}

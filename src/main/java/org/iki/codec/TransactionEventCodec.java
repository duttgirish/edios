package org.iki.codec;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;
import org.iki.model.TransactionEvent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Codec for serializing/deserializing TransactionEvent on the Vert.x Event Bus.
 */
public class TransactionEventCodec implements MessageCodec<TransactionEvent, TransactionEvent> {

    @Override
    public void encodeToWire(Buffer buffer, TransactionEvent event) {
        JsonObject json = new JsonObject()
                .put("debitAccount", event.debitAccount())
                .put("creditAccount", event.creditAccount())
                .put("cin", event.cin())
                .put("amount", event.amount().toString())
                .put("transactedTime", event.transactedTime().toString());

        String encoded = json.encode();
        buffer.appendInt(encoded.length());
        buffer.appendString(encoded);
    }

    @Override
    public TransactionEvent decodeFromWire(int pos, Buffer buffer) {
        int length = buffer.getInt(pos);
        String jsonStr = buffer.getString(pos + 4, pos + 4 + length);
        JsonObject json = new JsonObject(jsonStr);

        return new TransactionEvent(
                json.getString("debitAccount"),
                json.getString("creditAccount"),
                json.getString("cin"),
                new BigDecimal(json.getString("amount")),
                Instant.parse(json.getString("transactedTime"))
        );
    }

    @Override
    public TransactionEvent transform(TransactionEvent event) {
        return event;
    }

    @Override
    public String name() {
        return TransactionEventCodec.class.getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}

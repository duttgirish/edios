package org.iki.config;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.iki.codec.TransactionEventCodec;
import org.iki.model.TransactionEvent;
import org.jboss.logging.Logger;

/**
 * Configuration for the Vert.x Event Bus.
 * Registers custom codecs for message serialization.
 */
@ApplicationScoped
public class EventBusConfiguration {

    private static final Logger LOG = Logger.getLogger(EventBusConfiguration.class);

    @Inject
    EventBus eventBus;

    void onStart(@Observes StartupEvent event) {
        try {
            LOG.info("Registering TransactionEvent codec on Event Bus");
            eventBus.registerDefaultCodec(TransactionEvent.class, new TransactionEventCodec());
        } catch (IllegalStateException e) {
            // Codec already registered (happens during hot reload in dev mode)
            LOG.debug("TransactionEvent codec already registered");
        }
    }
}

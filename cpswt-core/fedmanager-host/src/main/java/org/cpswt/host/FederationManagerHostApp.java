package org.cpswt.host;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cpswt.host.api.FederationManagerControlRequest;
import org.cpswt.host.api.StateResponse;
import org.cpswt.config.FederateConfigParser;
import org.cpswt.hla.FederateState;
import org.cpswt.hla.FederationManager;
import org.cpswt.hla.FederationManagerConfig;
import org.cpswt.host.api.StateChangeResponse;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

/**
 * Federation Manager hosting through Akka-HTTP
 */
public class FederationManagerHostApp extends AllDirectives {
    private static final Logger logger = LogManager.getLogger();
    
    private ObjectMapper objectMapper;

    private FederationManagerConfig federationManagerConfig;
    
    private FederationManager federationManager;

    private String bindingAddress;

    private int port;
    
    public FederationManagerHostApp(String[] args) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JodaModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false);
        
        initializeFederationManager(args);
    }

    private void initializeFederationManager(String[] args) {
        try {
            this.federationManagerConfig = this.parseConfigurationFile(args);
            this.federationManager = new FederationManager(this.federationManagerConfig);
        } catch (IOException e) {
            throw new RuntimeException("failed to initialize federation manager", e);
        }
        this.bindingAddress = this.federationManagerConfig.bindHost;
        this.port = this.federationManagerConfig.port;
    }
    
    private FederationManagerConfig parseConfigurationFile(String[] args) {
        FederateConfigParser federateConfigParser = new FederateConfigParser();
        return federateConfigParser.parseArgs(args, FederationManagerConfig.class);
    }
    
    private void startFederationManager() {
        this.federationManager.start();
    }

    Route createRoute() {
        return route(
                get(() ->
                        path("fedmgr", () -> 
                                completeOK(new StateResponse(federationManager.getFederateState()), Jackson.marshaller(this.objectMapper))
                        )
                ),
                get(() ->
                        path("federates", () ->
                                completeOK(federationManager.getFederatesStatus(), Jackson.marshaller(this.objectMapper))
                        )
                ),
                post(() ->
                        path("fedmgr", () ->
                                entity(Jackson.unmarshaller(FederationManagerControlRequest.class), controlRequest -> {
                                    final FederateState currentState = federationManager.getFederateState();
                                    final FederateState targetState = controlRequest.action.getTargetState();

                                    if (federationManager.setFederateState(targetState)) {
                                        return completeOK(new StateChangeResponse(currentState, targetState), Jackson.marshaller(this.objectMapper));
                                    } else {
                                        return complete(StatusCodes.BAD_REQUEST);
                                    }
                                })
                        )
                )
        );
    }
    
    public static void main(String[] args) throws Exception {
        // from the Routing DSL example at https://doc.akka.io/docs/akka-http/current/routing-dsl/index.html
        ActorSystem system = ActorSystem.create("routes");

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        
        FederationManagerHostApp app = new FederationManagerHostApp(args);
        app.startFederationManager();
        
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(app.bindingAddress, app.port), materializer);

        logger.info("Server online at {}:{}", app.bindingAddress, app.port);
        System.in.read(); // check fedmanager instead . . .

        binding.thenCompose(ServerBinding::unbind).thenAccept(unbound -> system.terminate());
    }
}

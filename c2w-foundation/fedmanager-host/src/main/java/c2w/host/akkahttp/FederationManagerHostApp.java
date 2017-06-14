package c2w.host.akkahttp;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import c2w.hla.FederationManager;
import c2w.hla.FederationManagerParameter;
import c2w.host.FederationManagerConsoleHost;
import c2w.host.api.StateResponse;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletionStage;

/**
 * Federation Manager hosting through Akka-HTTP
 */
public class FederationManagerHostApp extends AllDirectives {

    static final Logger logger = Logger.getLogger(FederationManagerConsoleHost.class);
    private FederationManager federationManager;

    String bindingAddress;
    public String getBindingAddress() {
        return bindingAddress;
    }
    int port;
    public int getPort() {
        return port;
    }

    FederationManagerParameter getFederationManagerParameter(String[] args) {
        CommandLineParser parser  = new DefaultParser();
        Options cliOptions = FederationManagerParameter.GetCLIOptions();
        FederationManagerParameter currentParameter = null;

        try {
            CommandLine commandLine = parser.parse(cliOptions, args);
            String mConfigFilePath;

            if(args.length == 1) {
                mConfigFilePath = args[0];
            }
            else {
                mConfigFilePath = commandLine.getOptionValue("configFile");
            }

            File configFile = new File(mConfigFilePath);
            ObjectMapper mapper = new ObjectMapper(new JsonFactory());
            FederationManagerParameter federationManagerParameter = mapper.readValue(configFile, FederationManagerParameter.class);

            return federationManagerParameter;
        }
        catch (ParseException parseExp) {
            logger.error("Parsing CLI arguments failed. Reason: " + parseExp.getMessage(), parseExp);
            System.exit(-1);
        }
        catch(IOException ioExp) {
            logger.error("Parsing input configuration file failed. Reason: " + ioExp.getMessage(), ioExp);
            System.exit(-1);
        }
        catch (Exception fedMgrExp) {
            logger.error("There was an error starting the federation manager. Reason: " + fedMgrExp.getMessage(), fedMgrExp);
            System.exit(-1);
        }

        return null;
    }

    Route createRoute() {
        return route(
                get(() ->
//                        path("fedmgr", () ->
//                            completeOK(new StateResponse(federationManager.getFederateState(), Jackson.marshaller()))
//                        )
                ),

                post(() ->
                    path("fedmgr", () -> {

                    })
                )
        );
    }




    public static void main(String[] args) {

        ActorSystem actorSystem = ActorSystem.create("routes");

        final Http http = Http.get(actorSystem);
        final ActorMaterializer materializer = ActorMaterializer.create(actorSystem);

        FederationManagerHostApp app = new FederationManagerHostApp();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(actorSystem, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(app.getBindingAddress(), app.getPort()), materializer);

    }
}

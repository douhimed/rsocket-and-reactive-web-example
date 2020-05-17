package org.adex.clientrsocketapp;

import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class ClientRSocketAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientRSocketAppApplication.class, args);
    }

    @Bean
    public RSocketRequester rSocketRequester(RSocketRequester.Builder b) {
        return b.dataMimeType(MimeTypeUtils.APPLICATION_JSON)
                .connectTcp(Constants.HOST.getUrl(), 8000)
                .block();
    }

    @Bean
    public RouterFunction<ServerResponse> routes(RequestHandler handler) {
        return route()
                .GET(Constants.STREAM.getUrl() + Constants.NAME.getUrl(), handler::handleStream)
                .GET(Constants.MONO.getUrl() + Constants.NAME.getUrl(), handler::handleMono)
                .GET(Constants.EMPTY.getUrl() + Constants.NAME.getUrl(), handler::handleEmpty)
                .build();
    }

}

@Component
@RequiredArgsConstructor
class RequestHandler {

    private final RSocketRequester rSocketRequester;

    public Mono<ServerResponse> handleStream(ServerRequest serverRequest) {
        return ok().contentType(MediaType.TEXT_EVENT_STREAM)
                .body(this.generateRouteToServer(serverRequest, Constants.STREAM_SERVER).retrieveFlux(Response.class), Response.class);
    }


    public Mono<ServerResponse> handleMono(ServerRequest serverRequest) {
        return ok().body(this.generateRouteToServer(serverRequest, Constants.MONO_SERVER).retrieveMono(Response.class), Response.class);
    }

    public Mono<ServerResponse> handleEmpty(ServerRequest serverRequest) {
        return ok().body(this.rSocketRequester.route(Constants.EMPTY_SERVER.getUrl() + "/" + serverRequest.pathVariable("name")).send(), Response.class);
    }

    private RSocketRequester.RetrieveSpec generateRouteToServer(ServerRequest serverRequest, Constants url) {
        return this.rSocketRequester.route(url.getUrl()).data(new Request(serverRequest.pathVariable("name")));
    }
}

@RestController
@RequiredArgsConstructor
class ClientController {

    private final RSocketRequester rSocketRequester;

//    @GetMapping("/mono/{name}")
//    public Publisher<Response> getMonoFromServer(@PathVariable String name) {
//        return this.generateRouteToServer(name, "mono-server").retrieveMono(Response.class);
//    }

//    @GetMapping(value = "/stream/{name}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Publisher<Response> getStreamFromServer(@PathVariable String name) {
//        return this.generateRouteToServer(name, "stream-server").retrieveFlux(Response.class);
//    }

//    @GetMapping("/empty/{name}")
//    public Publisher<Void> fireAndForgetServer(@PathVariable String name) {
//        return this.rSocketRequester.route("fire-and-forget-server/" + name).send();
//    }

//    private RSocketRequester.RetrieveSpec generateRouteToServer(String name, String host) {
//        return this.rSocketRequester.route(host).data(new Request(name));
//    }

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Response {
    private String message;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Request {
    private String name;
}

enum Constants {
    MONO_SERVER("mono-server"), STREAM_SERVER("stream-server"), EMPTY_SERVER("fire-and-forget-server"),
    MONO("/mono"), STREAM("/stream"), EMPTY("/empty"), HOST("localhost"), NAME("/{name}");

    private String url;

    private Constants(String value) {
        this.url = value;
    }

    public String getUrl() {
        return url;
    }
}
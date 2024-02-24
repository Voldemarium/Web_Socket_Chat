package ru.synergy.webSocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.UnicastProcessor;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class ReactiveChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveChatApplication.class, args);
    }

    @Bean
    public UnicastProcessor<Event> eventPublisher() {
        return UnicastProcessor.create();
    }

    @Bean
    public Flux<Event> events(UnicastProcessor<Event> eventPublisher) {
        return eventPublisher
                .replay(25)
                .autoConnect();
    }

    @Bean // класс для маппинга Вебсокета
    public HandlerMapping webSocketMapping(UnicastProcessor<Event> eventPublisher, Flux<Event> events) {
        Map<String, Object> map = new HashMap<>();
        map.put("/websocket/chat", new ChatSocketHandler(eventPublisher, events));

        SimpleUrlHandlerMapping simpleUrlHandlerMapping = new SimpleUrlHandlerMapping();
        simpleUrlHandlerMapping.setUrlMap(map);

        //Without the order things break :
        simpleUrlHandlerMapping.setOrder(10);
        return simpleUrlHandlerMapping;
    }

    @Bean //класс-адаптер из библиотеки Spring для поддержки Вебсокета
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
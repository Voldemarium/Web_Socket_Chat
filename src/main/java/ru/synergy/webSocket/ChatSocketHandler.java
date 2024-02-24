package ru.synergy.webSocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.util.Optional;

import static ru.synergy.webSocket.Event.Type.USER_LEFT;

//Основной класс (для обработки всех сообщений от пользователей)
public class ChatSocketHandler implements WebSocketHandler {
    //UnicastProcessor - обработчик для Event — это два в одном: он одновременно и Subscriber, и Publisher.
    // Он принимает какие-то значения и куда-то их кладет
    private UnicastProcessor<Event> eventPublisher;
    private Flux<String> outputEvents; // поток сообщений
    private ObjectMapper mapper; //ObjectMapper используется для маппинга из JSON в объекты и обратно

    public ChatSocketHandler(UnicastProcessor<Event> eventPublisher, Flux<Event> events) {
        this.eventPublisher = eventPublisher;
        this.mapper = new ObjectMapper();
        this.outputEvents = Flux.from(events).map(this::toJSON);
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        WebSocketMessageSubscriber subscriber = new WebSocketMessageSubscriber(eventPublisher);
        return session.receive()                         // принимаем сессию
                .map(WebSocketMessage::getPayloadAsText) // каждое сообщение в чате получаем как текст
                .map(this::toEvent)                      // перевод текста в Event
                                                //применение к Event дополнительных функций WebSocketMessageSubscriber:
                .doOnNext(subscriber::onNext)            // публикация нового Event
                .doOnError(subscriber::onError)          // в случае ошибки - пишем в консоль ошибку
                .doOnComplete(subscriber::onComplete)    // публикация нового Event при завершении сокет-соединения
                                                         // (когда происходит выход пользователя User)
                .zipWith(session.send(outputEvents.map(session::textMessage)))  // совмещение потоков
                                                 // (входящих и исходящих сообщений) из этого чата в один общий поток
                .then();                        //возвращение общего потока Mono<Void>
    }

    //Возвращение Event из JSON (с помощью mapper)
    private Event toEvent(String json) {
        try {
            return mapper.readValue(json, Event.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Invalid JSON:" + json, e);
        }
    }

    //Возвращение JSON из Event (с помощью mapper)
    private String toJSON(Event event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    /*
   WebSocketMessageSubscriber — подписчик. Он определяет, что делать, если:
    Появилось очередное значение в потоке (onNext);
    Появилось ошибочное значение (onError);
    Поток завершился (onComplete).
     */
    private static class WebSocketMessageSubscriber {
        //UnicastProcessor - обработчик — это два в одном: он одновременно и Subscriber, и Publisher.
        // Он принимает какие-то значения и куда-то их кладет
        private UnicastProcessor<Event> eventPublisher;
        private Optional<Event> lastReceivedEvent = Optional.empty();

        public WebSocketMessageSubscriber(UnicastProcessor<Event> eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        public void onNext(Event event) {            //если появилось очередное значение в потоке;
            lastReceivedEvent = Optional.of(event);
            eventPublisher.onNext(event);           // публикация нового события
        }

        public void onError(Throwable error) {       // если появилось ошибочное значение
            //TODO log error
            error.printStackTrace();
        }

        public void onComplete() {                   // Появилось поток завершился
            lastReceivedEvent.ifPresent(event -> eventPublisher.onNext(
                    Event.type(USER_LEFT)
                            .withPayload()
                            .user(event.getUser())
                            .build()
            ));
        }
    }
}
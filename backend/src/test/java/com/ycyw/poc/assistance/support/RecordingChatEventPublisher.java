package com.ycyw.poc.assistance.support;

import com.ycyw.poc.assistance.domain.event.ChatEvent;
import com.ycyw.poc.assistance.domain.port.ChatEventPublisher;
import java.util.ArrayList;
import java.util.List;

/** Diffuseur d'evenements qui se contente de les retenir, pour que le test les examine. */
public class RecordingChatEventPublisher implements ChatEventPublisher {

    private final List<ChatEvent> published = new ArrayList<>();

    @Override
    public void publish(ChatEvent event) {
        published.add(event);
    }

    public List<ChatEvent> published() {
        return List.copyOf(published);
    }

    @SuppressWarnings("unchecked")
    public <T extends ChatEvent> List<T> publishedOfType(Class<T> type) {
        return published.stream().filter(type::isInstance).map(event -> (T) event).toList();
    }
}

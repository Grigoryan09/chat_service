package am.chat_service.messaging;

import am.chat_service.dto.request.ChangeChatStatusRequest;
import am.chat_service.exception.ChatNotFoundException;
import am.chat_service.messaging.event.ChatStatusUpdateEvent;
import am.chat_service.messaging.event.OrderDocumentRequestEvent;
import am.chat_service.model.enums.ChatStatus;
import am.chat_service.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChatStatusUpdateConsumerTest {

    @Mock
    private ChatService chatService;
    @Mock
    private ChatKafkaProducer chatKafkaProducer;

    @InjectMocks
    private ChatStatusUpdateConsumer consumer;

    @Test
    void onChatStatusUpdate_completed_changesStatusAndRequestsDocument() {
        consumer.onChatStatusUpdate(new ChatStatusUpdateEvent(5L, "COMPLETED"));

        ArgumentCaptor<ChangeChatStatusRequest> statusCaptor =
                ArgumentCaptor.forClass(ChangeChatStatusRequest.class);
        verify(chatService).changeChatStatus(statusCaptor.capture());
        assertThat(statusCaptor.getValue().chatId()).isEqualTo(5L);
        assertThat(statusCaptor.getValue().chatStatus()).isEqualTo(ChatStatus.COMPLETED);

        ArgumentCaptor<OrderDocumentRequestEvent> docCaptor =
                ArgumentCaptor.forClass(OrderDocumentRequestEvent.class);
        verify(chatKafkaProducer).sendOrderDocumentRequest(docCaptor.capture());
        assertThat(docCaptor.getValue().chatId()).isEqualTo(5L);
    }

    @Test
    void onChatStatusUpdate_statusChangeFails_stillRequestsDocument() {
        doThrow(new ChatNotFoundException("Chat with id 5 not found"))
                .when(chatService).changeChatStatus(any());

        assertThatCode(() -> consumer.onChatStatusUpdate(new ChatStatusUpdateEvent(5L, "COMPLETED")))
                .doesNotThrowAnyException();

        verify(chatKafkaProducer).sendOrderDocumentRequest(any());
    }

    @Test
    void onChatStatusUpdate_zeroChatId_isIgnoredEntirely() {
        consumer.onChatStatusUpdate(new ChatStatusUpdateEvent(0L, "COMPLETED"));

        verifyNoInteractions(chatService);
        verifyNoInteractions(chatKafkaProducer);
    }

    @Test
    void onChatStatusUpdate_unknownStatus_isIgnoredWithoutThrowing() {
        assertThatCode(() -> consumer.onChatStatusUpdate(new ChatStatusUpdateEvent(5L, "NOT_A_STATUS")))
                .doesNotThrowAnyException();

        verifyNoInteractions(chatService);
        verifyNoInteractions(chatKafkaProducer);
    }

    @Test
    void onChatStatusUpdate_nonCompletedStatus_doesNotRequestDocument() {
        consumer.onChatStatusUpdate(new ChatStatusUpdateEvent(5L, "ARCHIVED"));

        verify(chatService).changeChatStatus(any());
        verify(chatKafkaProducer, never()).sendOrderDocumentRequest(any());
    }
}

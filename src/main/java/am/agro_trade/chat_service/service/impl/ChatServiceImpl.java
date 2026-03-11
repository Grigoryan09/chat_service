package am.agro_trade.chat_service.service.impl;
import am.agro_trade.chat_service.dto.request.ChangeChatStatusRequest;
import am.agro_trade.chat_service.dto.request.CreateChatRequest;
import am.agro_trade.chat_service.dto.request.UpdateMessageRequest;
import am.agro_trade.chat_service.dto.response.ChatDto;
import am.agro_trade.chat_service.model.Chat;
import am.agro_trade.chat_service.repository.ChatRepository;
import am.agro_trade.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;


    @Override
    public long saveChat(CreateChatRequest createChatRequest) {
        return 0;
    }

    @Override
    public void changeChatStatus(ChangeChatStatusRequest updateChatStatusRequest) {

    }

    @Override
    public ChatDto findByChatId(long chatId) {
        return null;
    }
}

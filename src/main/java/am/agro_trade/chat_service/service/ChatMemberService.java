package am.agro_trade.chat_service.service;

import java.util.List;

public interface ChatMemberService {


    void save(List<Long> userIds, long chatId);

}

package am.chat_service.model;

import am.chat_service.model.enums.ChatStatus;
import am.chat_service.model.enums.ChatType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "chat")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private LocalDateTime createdDateTime;

    private LocalDateTime lastActivity;

    @Enumerated(EnumType.STRING)
    private ChatType chatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ChatStatus chatStatus;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMember> chatMembers;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages;
}

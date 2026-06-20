package com.skillnet.service.notification;

import com.skillnet.persistence.entity.core.Notification;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.NotificationRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void publish(User user, String type, String title, String message, String link) {
        if (user == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(type != null ? type : "general");
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
    }
}

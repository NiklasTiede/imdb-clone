package com.thecodinglab.imdbclone.notification.internal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationRecovery {
  private final JdbcTemplate jdbc;
  private final NotificationDelivery delivery;

  public NotificationRecovery(JdbcTemplate jdbc, NotificationDelivery delivery) {
    this.jdbc = jdbc;
    this.delivery = delivery;
  }

  public void deliverPending() {
    var ids =
        jdbc.queryForList(
            """
        select id from notification_delivery where state = 'PENDING'
        and available_at <= current_timestamp order by available_at, id
        limit 100 for update skip locked
        """,
            String.class);
    ids.forEach(delivery::process);
  }
}

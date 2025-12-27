-- Пример получения доступных мест на сеанс
SELECT s.seat_id
FROM seat s
LEFT JOIN ticket t
  ON s.seat_id = t.seat_id
  AND t.showtime_id = ?
  AND t.status_id NOT IN (?, ?) -- отменён, возврат
WHERE s.hall_id = ?
  AND t.ticket_id IS NULL;

-- Просроченные брони (старше 30 минут)
SELECT ticket_id, showtime_id, seat_id
FROM ticket
WHERE status_id = ? -- забронирован
  AND created_at < (NOW() - INTERVAL 30 MINUTE);

-- Создание брони
INSERT INTO ticket(showtime_id, seat_id, customer_id, user_id, status_id, created_at)
VALUES (?, ?, ?, ?, ?, NOW());

-- Подтверждение брони (оплата)
UPDATE ticket SET status_id = ? WHERE ticket_id = ?;
INSERT INTO payment(ticket_id, user_id, amount, method_id, payment_time)
VALUES (?, ?, ?, ?, NOW());

-- Отчёт за период
SELECT COUNT(*) AS payments, SUM(amount) AS revenue
FROM payment
WHERE payment_time BETWEEN ? AND ?;
